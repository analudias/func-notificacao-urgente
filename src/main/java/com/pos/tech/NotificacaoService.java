package com.pos.tech;

import com.microsoft.azure.functions.ExecutionContext;
import com.sendgrid.*;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.*;

public class NotificacaoService {
    
    private static final String API_KEY = System.getenv("SENDGRID_API_KEY");
    private static final SendGrid sg = new SendGrid(API_KEY);

    public static void enviarEmailUrgencia(String corpo, ExecutionContext context) throws Exception {
        String fromEmail = System.getenv("FROM_EMAIL");
        String toEmailsRaw = System.getenv("TO_EMAILS");

        if (toEmailsRaw == null || fromEmail == null) {
            throw new IllegalStateException("Configurações de e-mail (FROM_EMAIL/TO_EMAILS) ausentes.");
        }

        Mail mail = new Mail();
        mail.setFrom(new Email(fromEmail));
        mail.setSubject("[URGENTE] Feedback crítico recebido");
        mail.addContent(new Content("text/plain", corpo));

        Personalization personalization = new Personalization();
        for (String emailStr : toEmailsRaw.split(",")) {
            personalization.addTo(new Email(emailStr.trim()));
        }
        mail.addPersonalization(personalization);

        Request req = new Request();
        req.setMethod(Method.POST);
        req.setEndpoint("mail/send");
        req.setBody(mail.build());

        Response resp = sg.api(req);
        if (resp.getStatusCode() != 202) {
            throw new RuntimeException("SendGrid erro: " + resp.getStatusCode());
        }
    }
}
