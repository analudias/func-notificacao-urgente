package com.pos.tech;

import com.microsoft.azure.functions.annotation.*;
import com.microsoft.azure.functions.*;
import com.fasterxml.jackson.databind.*;
import java.util.*;

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
            
            // Event Grid pode enviar um array ou objeto único. 
            // O dado da sua API está sempre dentro do campo "data".
            JsonNode dataNode = node.isArray() ? node.get(0).get("data") : node.get("data");

            if (dataNode == null) {
                context.getLogger().warning("Campo 'data' não encontrado no evento.");
                return;
            }

            Feedback fb = mapper.treeToValue(dataNode, Feedback.class);

            if (fb != null && fb.getNota() < 3) {
                String msg = "Urgência via Evento: " + fb.getDescricao();
                NotificacaoService.enviarEmailUrgencia(msg, context);
            }

        } catch (Exception e) {
            context.getLogger().severe("Erro: " + e.getMessage());
        }
    }
}