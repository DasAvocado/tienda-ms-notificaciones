package com.tienda.ms_notificaciones.service;

import com.tienda.ms_notificaciones.dto.NotificacionResponse;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Envia correos por SMTP. Si no hay SMTP configurado (SPRING_MAIL_HOST), SIMULA el envio
 * escribiendo el correo completo en el log (valido para la evidencia de la demo).
 */
@Service
public class NotificacionService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionService.class);

    public static final String ENVIADO = "ENVIADO";
    public static final String SIMULADO = "SIMULADO";
    public static final String ERROR = "ERROR";

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

    /** true = hay un servidor SMTP real configurado (los correos llegan a la bandeja). */
    public boolean smtpConfigurado() {
        return smtpHost != null && !smtpHost.isBlank() && mailSender.getIfAvailable() != null;
    }

    /** Correo de texto plano (usado por POST /api/notificaciones/enviar). */
    public NotificacionResponse enviar(String to, String subject, String message) {
        return enviar(to, subject, message, null);
    }

    /**
     * Envia un correo. Si html != null se envia como multipart (texto + HTML).
     * Nunca lanza excepciones: el resultado viene en NotificacionResponse.estado (ENVIADO | SIMULADO | ERROR).
     */
    public NotificacionResponse enviar(String to, String subject, String texto, String html) {
        if (!smtpConfigurado()) {
            log.info("\n===== CORREO SIMULADO =====\nPara:   {}\nAsunto: {}\n\n{}\n===========================",
                    to, subject, texto);
            return new NotificacionResponse(SIMULADO, to, subject, "SMTP no configurado: correo registrado en el log");
        }

        try {
            JavaMailSender sender = mailSender.getObject();
            MimeMessage mime = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, html != null, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            if (html != null) {
                helper.setText(texto, html);
            } else {
                helper.setText(texto);
            }
            sender.send(mime);
            log.info("Correo enviado a {} (asunto: {})", to, subject);
            return new NotificacionResponse(ENVIADO, to, subject, "Correo enviado por SMTP");
        } catch (Exception e) {
            log.error("Error enviando correo a {}: {}", to, e.getMessage());
            return new NotificacionResponse(ERROR, to, subject, e.getMessage());
        }
    }
}
