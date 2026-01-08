package com.pos.tech;

import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;
import jakarta.validation.*;
import java.util.*;

public class Function {

    private static final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @FunctionName("NotificarUrgencia")
    public HttpResponseMessage run(
            @HttpTrigger(name = "req", methods = {HttpMethod.POST}, authLevel = AuthorizationLevel.ANONYMOUS) 
            HttpRequestMessage<Optional<Feedback>> request,
            final ExecutionContext context) {

        Feedback fb = request.getBody().orElse(null);

        if (fb == null) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body("JSON inválido.").build();
        }

        // Validação
        Set<ConstraintViolation<Feedback>> violations = validator.validate(fb);
        if (!violations.isEmpty()) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body("Erro de validação").build();
        }

        if (fb.getNota() < 3) {
            String mensagem = String.format("URGÊNCIA ALTA: %s (Nota: %d)", fb.getDescricao(), fb.getNota());
            try {
                NotificacaoService.enviarEmailUrgencia(mensagem, context);
            } catch (Exception e) {
                return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        }

        return request.createResponseBuilder(HttpStatus.OK).body("Processado").build();
    }
}