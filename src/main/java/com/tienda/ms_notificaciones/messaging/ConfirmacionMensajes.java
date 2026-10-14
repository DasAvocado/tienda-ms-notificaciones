package com.tienda.ms_notificaciones.messaging;

import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Manejo EXPLICITO de la confirmacion de mensajes (ACK manual) para todos los consumidores del servicio.
 *
 * Reglas (iguales en todos los microservicios):
 *  1. Procesado OK                       -> basicAck.
 *  2. MensajeInvalidoException           -> error NO recuperable (datos malos): basicNack(requeue=false)
 *                                           -> RabbitMQ lo envia a la DLQ de la cola. Se registra en log.
 *  3. Cualquier otra excepcion           -> error recuperable (BD caida, SMTP caido...):
 *       - si quedan reintentos           -> se re-publica una copia SOLO en la misma cola (exchange por defecto)
 *                                           con el header x-reintentos+1 y se hace ACK del original.
 *       - si se agotaron                 -> basicNack(requeue=false) -> DLQ. Se registra en log.
 *  4. JSON que no se puede convertir     -> Spring lo rechaza sin reencolar (ConditionalRejectingErrorHandler)
 *                                           -> DLQ (ver spring.rabbitmq.listener.simple.default-requeue-rejected).
 *
 * No se usa requeue=true para evitar bucles infinitos de reproceso.
 */
@Component
public class ConfirmacionMensajes {

    public static final String HEADER_REINTENTOS = "x-reintentos";
    /** Routing key con la que llego el mensaje por primera vez (los reintentos llegan por el exchange por defecto). */
    public static final String HEADER_RK_ORIGINAL = "x-routing-key-original";

    private static final Logger log = LoggerFactory.getLogger(ConfirmacionMensajes.class);

    private final RabbitTemplate rabbitTemplate;
    private final int maxReintentos;

    public ConfirmacionMensajes(RabbitTemplate rabbitTemplate,
                                @Value("${app.rabbit.max-reintentos:3}") int maxReintentos) {
        this.rabbitTemplate = rabbitTemplate;
        this.maxReintentos = maxReintentos;
    }

    /** Accion de negocio que puede lanzar excepciones. */
    @FunctionalInterface
    public interface Accion {
        void ejecutar() throws Exception;
    }

    public void procesar(Message mensaje, Channel canal, Accion accion) throws IOException {
        MessageProperties props = mensaje.getMessageProperties();
        long tag = props.getDeliveryTag();
        String cola = props.getConsumerQueue();
        int intento = reintentos(props);

        try {
            accion.ejecutar();
            canal.basicAck(tag, false);
        } catch (MensajeInvalidoException e) {
            log.error("[DLQ] Mensaje invalido en '{}' -> enviado a DLQ sin reintentar. Motivo: {} | payload={}",
                    cola, e.getMessage(), cuerpo(mensaje));
            canal.basicNack(tag, false, false);
        } catch (Exception e) {
            if (intento < maxReintentos) {
                log.warn("[REINTENTO {}/{}] Error procesando mensaje de '{}': {}",
                        intento + 1, maxReintentos, cola, e.getMessage());
                Message copia = MessageBuilder.fromClonedMessage(mensaje)
                        .setHeader(HEADER_REINTENTOS, intento + 1)
                        .setHeaderIfAbsent(HEADER_RK_ORIGINAL, props.getReceivedRoutingKey())
                        .build();
                // Exchange por defecto ("") + nombre de cola: la copia llega SOLO a esta cola, no a los demas suscriptores.
                rabbitTemplate.send("", cola, copia);
                canal.basicAck(tag, false);
            } else {
                log.error("[DLQ] Mensaje de '{}' agoto {} reintentos -> enviado a DLQ. Causa: {} | payload={}",
                        cola, maxReintentos, e.getMessage(), cuerpo(mensaje));
                canal.basicNack(tag, false, false);
            }
        }
    }

    private static int reintentos(MessageProperties props) {
        Object valor = props.getHeader(HEADER_REINTENTOS);
        return valor instanceof Number n ? n.intValue() : 0;
    }

    private static String cuerpo(Message mensaje) {
        String texto = new String(mensaje.getBody(), StandardCharsets.UTF_8);
        return texto.length() > 500 ? texto.substring(0, 500) + "..." : texto;
    }
}
