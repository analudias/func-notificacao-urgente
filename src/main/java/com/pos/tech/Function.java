package com.pos.tech;

import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;

import jakarta.validation.*;
import java.util.*;

import com.sendgrid.*;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.*;

public class Function {

    private static final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    private static final Validator validator = factory.getValidator();

    @FunctionName("NotificarUrgencia")
    public HttpResponseMessage run(
            @HttpTrigger(
                    name = "req",
                    methods = {HttpMethod.POST},
                    authLevel = AuthorizationLevel.ANONYMOUS
            )
            HttpRequestMessage<Optional<Feedback>> request,
            final ExecutionContext context) {

        Feedback fb = request.getBody().orElse(null);

        if (fb == null) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("Body inválido. Envie JSON com {descricao, nota, dataEnvio}.")
                    .build();
        }

        // Validação via Jakarta Validation (se houver anotações @NotNull/@Min/@Max no Feedback)
        Set<ConstraintViolation<Feedback>> violations = validator.validate(fb);
        if (!violations.isEmpty()) {
            StringBuilder sb = new StringBuilder("Erro de validação:\n");
            for (ConstraintViolation<Feedback> v : violations) {
                sb.append("- ").append(v.getPropertyPath()).append(": ").append(v.getMessage()).append("\n");
            }
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body(sb.toString()).build();
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
                context.getLogger().info("E-mail de urgência enviado (ou aceito) com sucesso.");
            } catch (Exception e) {
                context.getLogger().severe("Falha ao enviar e-mail: " + e.getMessage());
                return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Falha ao enviar notificação por e-mail: " + e.getMessage())
                        .build();
            }

            return request.createResponseBuilder(HttpStatus.OK).body(mensagemUrgencia).build();
        }

        return request.createResponseBuilder(HttpStatus.OK).body("Feedback processado (Normal).").build();
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
