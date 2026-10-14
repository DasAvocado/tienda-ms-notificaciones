package com.tienda.ms_notificaciones.service;

import com.tienda.ms_notificaciones.dto.NotificacionEnviadaEvent;
import com.tienda.ms_notificaciones.dto.NotificacionResponse;
import com.tienda.ms_notificaciones.dto.OrdenCreadaEvent;
import com.tienda.ms_notificaciones.messaging.NotificacionPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Correo de confirmacion de compra: se envia al correo con el que el usuario inicio sesion
 * (viaja en el evento orden.creada desde el JWT de Entra ID), salvo que el usuario lo haya
 * desactivado en "Notificaciones".
 */
@Service
public class ConfirmacionCompraService {

    private static final Logger log = LoggerFactory.getLogger(ConfirmacionCompraService.class);
    public static final String OMITIDO = "OMITIDO_POR_PREFERENCIA";

    private final NotificacionService correo;
    private final PreferenciaService preferencias;
    private final NotificacionPublisher publisher;
    private final String overrideTo;

    public ConfirmacionCompraService(NotificacionService correo,
                                     PreferenciaService preferencias,
                                     NotificacionPublisher publisher,
                                     @Value("${app.mail.override-to:}") String overrideTo) {
        this.correo = correo;
        this.preferencias = preferencias;
        this.publisher = publisher;
        this.overrideTo = overrideTo;
    }

    /** Destino real del correo: el correo de la sesion (o la casilla de override si esta configurada). */
    public String destinoPara(String emailSesion) {
        return (overrideTo != null && !overrideTo.isBlank()) ? overrideTo : emailSesion;
    }

    /**
     * @throws IllegalStateException si el SMTP falla (error recuperable: el consumidor reintenta y luego va a DLQ)
     */
    public NotificacionResponse notificarCompra(OrdenCreadaEvent evento) {
        String asunto = "Pedidos360 - Confirmacion de tu compra #" + evento.ordenId();
        String destino = destinoPara(evento.email());

        if (!preferencias.recibeCorreoCompra(evento.usuarioId())) {
            log.info("Orden {}: el usuario {} desactivo el correo de compra; no se envia.", evento.ordenId(), evento.email());
            publisher.notificacionEnviada(new NotificacionEnviadaEvent(evento.ordenId(), destino, asunto, OMITIDO));
            return new NotificacionResponse(OMITIDO, destino, asunto, "Desactivado por el usuario");
        }

        NotificacionResponse resp = correo.enviar(destino, asunto, textoPlano(evento), html(evento));
        if (NotificacionService.ERROR.equals(resp.estado())) {
            throw new IllegalStateException("No se pudo enviar el correo de la orden " + evento.ordenId() + ": " + resp.detalle());
        }
        publisher.notificacionEnviada(new NotificacionEnviadaEvent(evento.ordenId(), destino, asunto, resp.estado()));
        return resp;
    }

    /** Correo de prueba al usuario conectado (boton "Enviar correo de prueba" del frontend). */
    public NotificacionResponse enviarPrueba(String emailSesion, String nombre) {
        String destino = destinoPara(emailSesion);
        String saludo = nombre == null || nombre.isBlank() ? "Hola" : "Hola " + nombre;
        String texto = saludo + ",\n\nEste es un correo de prueba de Pedidos360. Si lo recibes, "
                + "las confirmaciones de compra llegaran a esta casilla (" + destino + ").";
        String html = "<p>" + HtmlUtils.htmlEscape(saludo) + ",</p><p>Este es un correo de prueba de <b>Pedidos360</b>. "
                + "Si lo recibes, las confirmaciones de compra llegaran a <b>" + HtmlUtils.htmlEscape(destino) + "</b>.</p>";
        return correo.enviar(destino, "Pedidos360 - Correo de prueba", texto, html);
    }

    // ------------------------------------------------------------------ plantillas

    private static String clp(double monto) {
        NumberFormat f = NumberFormat.getIntegerInstance(Locale.forLanguageTag("es-CL"));
        return "$" + f.format(Math.round(monto));
    }

    String textoPlano(OrdenCreadaEvent e) {
        StringBuilder sb = new StringBuilder();
        sb.append("Hola ").append(e.nombre()).append(",\n\n")
          .append("Recibimos tu compra. Detalle de la orden #").append(e.ordenId()).append(":\n\n");
        for (OrdenCreadaEvent.Item i : e.items()) {
            sb.append(" - ").append(i.nombre()).append(" x").append(i.cantidad())
              .append("  ").append(clp(i.precio() * i.cantidad())).append('\n');
        }
        sb.append("\nTotal: ").append(clp(e.total())).append("\n\nGracias por comprar en Pedidos360.");
        return sb.toString();
    }

    String html(OrdenCreadaEvent e) {
        StringBuilder filas = new StringBuilder();
        for (OrdenCreadaEvent.Item i : e.items()) {
            filas.append("<tr><td style=\"padding:6px 0\">").append(HtmlUtils.htmlEscape(i.nombre()))
                 .append(" &times; ").append(i.cantidad())
                 .append("</td><td style=\"padding:6px 0;text-align:right\">").append(clp(i.precio() * i.cantidad()))
                 .append("</td></tr>");
        }
        return "<div style=\"font-family:Arial,sans-serif;max-width:520px;color:#1d1d1f\">"
                + "<h2 style=\"color:#d92f41\">Gracias por tu compra</h2>"
                + "<p>Hola " + HtmlUtils.htmlEscape(e.nombre() == null ? "" : e.nombre()) + ",</p>"
                + "<p>Recibimos tu orden <b>#" + e.ordenId() + "</b>. Este es el detalle:</p>"
                + "<table style=\"width:100%;border-collapse:collapse;border-top:1px solid #ddd\">" + filas
                + "<tr><td style=\"padding:8px 0;border-top:1px solid #ddd\"><b>Total</b></td>"
                + "<td style=\"padding:8px 0;border-top:1px solid #ddd;text-align:right\"><b>" + clp(e.total()) + "</b></td></tr>"
                + "</table><p style=\"color:#777;font-size:12px\">Recibes este correo porque tienes activadas las "
                + "notificaciones de compra en Pedidos360. Puedes desactivarlas en la seccion Notificaciones.</p></div>";
    }
}
