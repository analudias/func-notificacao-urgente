package com.pos.tech;

import com.microsoft.azure.functions.annotation.*;
import com.microsoft.azure.functions.*;
import com.fasterxml.jackson.databind.*;

public class EvaluationEventFunction {

    private static final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @FunctionName("EvaluationEventHandler")
    public void handle(
        @EventGridTrigger(name = "event") String event,
        final ExecutionContext context
    ) {
        try {
            JsonNode node = mapper.readTree(event);
            
            JsonNode dataNode = node.isArray() ? node.get(0).get("data") : node.get("data");

            if (dataNode == null) {
                context.getLogger().warning("Campo 'data' não encontrado no evento.");
                return;
            }

            Feedback fb = mapper.treeToValue(dataNode, Feedback.class);

            if (fb != null && fb.getNota() < 3) {
                
                String mensagemFormatada = String.format(
                    "🚨 ALERTA DE FEEDBACK CRÍTICO 🚨\n\n" +
                    "Um novo feedback de baixa pontuação foi recebido e requer atenção imediata:\n\n" +
                    "------------------------------------------\n" +
                    "Nota: %d / 10\n" +
                    "Data de Envio: %s\n" +
                    "Descrição: %s\n" +
                    "------------------------------------------\n\n",
                    fb.getNota(),
                    fb.getDataEnvio() != null ? fb.getDataEnvio() : "Não informada",
                    fb.getDescricao()
                );

                NotificacaoService.enviarEmailUrgencia(mensagemFormatada, context);
                context.getLogger().info("E-mail de urgência enviado com sucesso");
            }

        } catch (Exception e) {
            context.getLogger().severe("Erro ao processar evento e enviar e-mail: " + e.getMessage());
        }
    }
}