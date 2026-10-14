package com.tienda.ms_notificaciones.messaging;

import com.tienda.ms_notificaciones.config.RabbitConfig;
import com.tienda.ms_notificaciones.dto.NotificacionEnviadaEvent;
import com.tienda.ms_notificaciones.dto.NotificacionResponse;
import com.tienda.ms_notificaciones.dto.OrdenCreadaEvent;
import com.tienda.ms_notificaciones.service.NotificacionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Locale;

/** orden.creada -> correo de confirmacion al cliente -> publica notificacion.enviada */
@Component
public class OrdenCreadaConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrdenCreadaConsumer.class);

    private final NotificacionService notificaciones;
    private final RabbitTemplate rabbitTemplate;
    private final String overrideTo;

    public OrdenCreadaConsumer(NotificacionService notificaciones,
                               RabbitTemplate rabbitTemplate,
                               @Value("${app.mail.override-to:}") String overrideTo) {
        this.notificaciones = notificaciones;
        this.rabbitTemplate = rabbitTemplate;
        this.overrideTo = overrideTo;
    }

    @RabbitListener(queues = RabbitConfig.QUEUE_ORDEN_CREADA)
    public void onOrdenCreada(OrdenCreadaEvent evento) {
        log.info("[RabbitMQ] orden.creada recibida: ordenId={}", evento.ordenId());

        String destino = (overrideTo != null && !overrideTo.isBlank()) ? overrideTo : evento.email();
        String asunto = "Pedidos360 - Confirmacion de tu orden #" + evento.ordenId();

        StringBuilder cuerpo = new StringBuilder();
        cuerpo.append("Hola ").append(evento.nombre()).append(",\n\n")
              .append("Recibimos tu compra. Detalle de la orden #").append(evento.ordenId()).append(":\n\n");
        for (OrdenCreadaEvent.Item i : evento.items()) {
            cuerpo.append(String.format(Locale.US, " - %s x%d  $%.0f%n", i.nombre(), i.cantidad(), i.precio() * i.cantidad()));
        }
        cuerpo.append(String.format(Locale.US, "%nTotal: $%.0f%n%nGracias por comprar en Pedidos360.", evento.total()));

        NotificacionResponse resp = notificaciones.enviar(destino, asunto, cuerpo.toString());

        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.RK_NOTIFICACION_ENVIADA,
                new NotificacionEnviadaEvent(evento.ordenId(), destino, asunto, resp.estado()));
    }
}
