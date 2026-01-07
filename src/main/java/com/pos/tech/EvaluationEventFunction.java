package com.pos.tech;

import com.microsoft.azure.functions.annotation.*;
import com.microsoft.azure.functions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import jakarta.validation.*;
import java.util.*;

import com.sendgrid.*;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.*;

public class EvaluationEventFunction {

    private static final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    private static final Validator validator = factory.getValidator();

    @FunctionName("EvaluationEventHandler")
    public void handle(
        @EventGridTrigger(name = "event") String event,
        final ExecutionContext context
    ) {
        context.getLogger().info("Evento recebido:");
        context.getLogger().info(event);

        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            JsonNode root = mapper.readTree(event);

            Feedback fb = null;

            if (root.isArray()) {
                for (JsonNode el : root) {
                    JsonNode candidate = el.has("data") ? el.get("data") : el;
                    try {
                        fb = mapper.treeToValue(candidate, Feedback.class);
                        if (fb != null) break;
                    } catch (Exception ex) {
                        // continue trying other elements
                    }
                }
            } else {
                // try direct mapping (root or root.data)
                JsonNode candidate = root.has("data") ? root.get("data") : root;
                try {
                    fb = mapper.treeToValue(candidate, Feedback.class);
                } catch (Exception ex) {
                    // try to find nested object that looks like feedback
                    for (Iterator<Map.Entry<String, JsonNode>> it = root.fields(); it.hasNext() && fb == null; ) {
                        Map.Entry<String, JsonNode> e = it.next();
                        JsonNode v = e.getValue();
                        if (v != null && v.isObject()) {
                            try {
                                fb = mapper.treeToValue(v, Feedback.class);
                            } catch (Exception ex2) {
                                // ignore and continue
                            }
                        }
                    }
                }
            }

            if (fb == null) {
                context.getLogger().warning("Evento não contém dados de feedback válidos.");
                return;
            }

            Set<ConstraintViolation<Feedback>> violations = validator.validate(fb);
            if (!violations.isEmpty()) {
                StringBuilder sb = new StringBuilder("Erro de validação:");
                for (ConstraintViolation<Feedback> v : violations) {
                    sb.append("\n- ").append(v.getPropertyPath()).append(": ").append(v.getMessage());
                }
                context.getLogger().warning(sb.toString());
                return;
            }

            if (fb.getDataEnvio() == null || fb.getDataEnvio().isBlank()) {
                fb.setDataEnvio(java.time.OffsetDateTime.now().toString());
            }

            if (fb.getNota() < 3) {
                String mensagemUrgencia = String.format(
                        "AVISO DE URGÊNCIA\n" +
                        "Descrição: %s\n" +
                        "Urgência: ALTA (Nota: %d)\n" +
                        "Data de Envio: %s",
                        fb.getDescricao(), fb.getNota(), fb.getDataEnvio()
                );

                try {
                    enviarEmailUrgencia(mensagemUrgencia, context);
                    context.getLogger().info("E-mail de urgência enviado com sucesso.");
                } catch (Exception e) {
                    context.getLogger().severe("Falha ao enviar e-mail: " + e.getMessage());
                }

            } else {
                context.getLogger().info("Feedback processado (Normal).");
            }

        } catch (Exception e) {
            context.getLogger().severe("Erro ao processar evento: " + e.getMessage());
        }
    }

    private void enviarEmailUrgencia(String corpo, ExecutionContext context) throws Exception {
        String apiKey = System.getenv("SENDGRID_API_KEY");
        String fromEmail = System.getenv("FROM_EMAIL");
        String toEmailsRaw = System.getenv("TO_EMAILS");

        Mail mail = new Mail();
        mail.setFrom(new Email(fromEmail));
        mail.setSubject("[URGENTE] Feedback crítico recebido");
        mail.addContent(new Content("text/plain", corpo));

        Personalization personalization = new Personalization();
        String[] emailList = toEmailsRaw.split(",");
        for (String emailStr : emailList) {
            personalization.addTo(new Email(emailStr.trim()));
        }
        mail.addPersonalization(personalization);

        SendGrid sg = new SendGrid(apiKey);
        Request req = new Request();
        req.setMethod(Method.POST);
        req.setEndpoint("mail/send");
        req.setBody(mail.build());

        Response resp = sg.api(req);
        context.getLogger().info("SendGrid status: " + resp.getStatusCode());

        if (resp.getStatusCode() != 202) {
            throw new RuntimeException("Falha no SendGrid: " + resp.getStatusCode());
        }
    }
}
