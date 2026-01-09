package com.pos.tech;

import com.microsoft.azure.functions.ExecutionContext;
import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.util.Properties;

public class NotificacaoService {

    public static void enviarEmailUrgencia(String corpo, ExecutionContext context) throws Exception {

        String host = System.getenv("EMAIL_HOST");
        String port = System.getenv("EMAIL_PORT");
        String user = System.getenv("EMAIL_USER");
        String pass = System.getenv("EMAIL_PASS");
        String destinatarios = System.getenv("TO_EMAILS");

        Properties props = new Properties();
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", port);
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(user, pass);
            }
        });

        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(user));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinatarios));
        message.setSubject("[URGENTE] Feedback crítico recebido", "UTF-8");

        MimeBodyPart texto = new MimeBodyPart();
        texto.setText(corpo, "UTF-8");

        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(texto);

        message.setContent(multipart);

        Transport.send(message);
        context.getLogger().info("E-mail de urgência enviado via SMTP com sucesso.");
    }
}