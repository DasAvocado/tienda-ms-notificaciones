package com.tienda.ms_notificaciones.messaging;

import com.tienda.ms_notificaciones.dto.NotificacionEnviadaEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Productor del evento "notificacion.enviada" (lo registra ms-auditoria). */
@Component
public class NotificacionPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final String exchange;
    private final String rkNotificacionEnviada;

    public NotificacionPublisher(RabbitTemplate rabbitTemplate,
                                 @Value("${app.rabbit.exchange}") String exchange,
                                 @Value("${app.rabbit.routing-keys.notificacion-enviada}") String rkNotificacionEnviada) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
        this.rkNotificacionEnviada = rkNotificacionEnviada;
    }

    public void notificacionEnviada(NotificacionEnviadaEvent evento) {
        rabbitTemplate.convertAndSend(exchange, rkNotificacionEnviada, evento);
    }
}
