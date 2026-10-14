package com.tienda.ms_notificaciones.messaging;

import com.rabbitmq.client.Channel;
import com.tienda.ms_notificaciones.dto.OrdenCreadaEvent;
import com.tienda.ms_notificaciones.service.ConfirmacionCompraService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Dominio NOTIFICACIONES - consumidor de "orden.creada".
 * Solo valida el mensaje y delega: el correo lo arma ConfirmacionCompraService y el ACK ConfirmacionMensajes.
 */
@Component
public class OrdenCreadaConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrdenCreadaConsumer.class);

    private final ConfirmacionCompraService confirmacionCompra;
    private final ConfirmacionMensajes confirmacion;

    public OrdenCreadaConsumer(ConfirmacionCompraService confirmacionCompra, ConfirmacionMensajes confirmacion) {
        this.confirmacionCompra = confirmacionCompra;
        this.confirmacion = confirmacion;
    }

    @RabbitListener(queues = "${app.rabbit.queues.orden-creada}", ackMode = "MANUAL")
    public void onOrdenCreada(@Payload OrdenCreadaEvent evento, Message mensaje, Channel canal) throws IOException {
        log.info("[RabbitMQ] orden.creada recibida: ordenId={}", evento.ordenId());
        confirmacion.procesar(mensaje, canal, () -> {
            if (evento.ordenId() == null || evento.items() == null || evento.items().isEmpty()) {
                throw new MensajeInvalidoException("orden.creada sin ordenId o sin items");
            }
            if (evento.email() == null || !evento.email().contains("@")) {
                throw new MensajeInvalidoException("orden.creada sin correo valido del usuario");
            }
            confirmacionCompra.notificarCompra(evento);
        });
    }
}
