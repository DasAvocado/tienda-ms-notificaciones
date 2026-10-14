package com.tienda.ms_notificaciones.service;

import com.tienda.ms_notificaciones.dto.NotificacionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Envia correos por SMTP. Si no hay SMTP configurado (SPRING_MAIL_HOST), SIMULA el envio
 * escribiendo el correo completo en el log (valido para la evidencia de la demo).
 */
@Service
public class NotificacionService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionService.class);

    private final ObjectProvider<JavaMailSender> mailSender;
    private final String from;
    private final String smtpHost;

    public NotificacionService(ObjectProvider<JavaMailSender> mailSender,
                               @Value("${app.mail.from}") String from,
                               @Value("${spring.mail.host:}") String smtpHost) {
        this.mailSender = mailSender;
        this.from = from;
        this.smtpHost = smtpHost;
    }

    public NotificacionResponse enviar(String to, String subject, String message) {
        JavaMailSender sender = (smtpHost == null || smtpHost.isBlank()) ? null : mailSender.getIfAvailable();

        if (sender == null) {
            log.info("\n===== CORREO SIMULADO =====\nPara:   {}\nAsunto: {}\n\n{}\n===========================",
                    to, subject, message);
            return new NotificacionResponse("SIMULADO", to, subject, "SMTP no configurado: correo registrado en el log");
        }

        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setFrom(from);
            mail.setTo(to);
            mail.setSubject(subject);
            mail.setText(message);
            sender.send(mail);
            log.info("Correo enviado a {} (asunto: {})", to, subject);
            return new NotificacionResponse("ENVIADO", to, subject, "Correo enviado por SMTP");
        } catch (Exception e) {
            log.error("Error enviando correo a {}: {}", to, e.getMessage());
            return new NotificacionResponse("ERROR", to, subject, e.getMessage());
        }
    }
}
