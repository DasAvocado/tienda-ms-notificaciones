package com.tienda.ms_notificaciones.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topologia RabbitMQ de ms-notificaciones. Los NOMBRES se definen solo en application.yml (app.rabbit.*).
 *
 *   [pedidos360.events] --orden.creada--> notificaciones.orden-creada   (consume OrdenCreadaConsumer)
 *   notificaciones.orden-creada --(rechazo/reintentos agotados)--> [pedidos360.dlx] --> notificaciones.orden-creada.dlq
 *   ms-notificaciones PUBLICA notificacion.enviada (NotificacionPublisher)
 */
@Configuration
public class RabbitConfig {

    private static final Logger log = LoggerFactory.getLogger(RabbitConfig.class);

    private final String exchange;
    private final String dlx;
    private final String rkOrdenCreada;
    private final String colaOrdenCreada;
    private final String colaOrdenCreadaDlq;

    public RabbitConfig(@Value("${app.rabbit.exchange}") String exchange,
                        @Value("${app.rabbit.dlx}") String dlx,
                        @Value("${app.rabbit.routing-keys.orden-creada}") String rkOrdenCreada,
                        @Value("${app.rabbit.queues.orden-creada}") String colaOrdenCreada,
                        @Value("${app.rabbit.queues.orden-creada-dlq}") String colaOrdenCreadaDlq) {
        this.exchange = exchange;
        this.dlx = dlx;
        this.rkOrdenCreada = rkOrdenCreada;
        this.colaOrdenCreada = colaOrdenCreada;
        this.colaOrdenCreadaDlq = colaOrdenCreadaDlq;
    }

    @Bean
    public TopicExchange eventosExchange() {
        return new TopicExchange(exchange, true, false);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(dlx, true, false);
    }

    // ---------- Caso de uso: correo de confirmacion de compra ----------
    @Bean
    public Queue colaOrdenCreada() {
        return QueueBuilder.durable(colaOrdenCreada)
                .deadLetterExchange(dlx)
                .deadLetterRoutingKey(colaOrdenCreadaDlq)
                .build();
    }

    @Bean
    public Binding bindingOrdenCreada() {
        return BindingBuilder.bind(colaOrdenCreada()).to(eventosExchange()).with(rkOrdenCreada);
    }

    @Bean
    public Queue colaOrdenCreadaDlq() {
        return QueueBuilder.durable(colaOrdenCreadaDlq).build();
    }

    @Bean
    public Binding bindingOrdenCreadaDlq() {
        return BindingBuilder.bind(colaOrdenCreadaDlq()).to(deadLetterExchange()).with(colaOrdenCreadaDlq);
    }

    // ---------- Infraestructura comun ----------
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    /** RabbitTemplate con confirmaciones del broker: registra en log los mensajes NO entregados. */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        template.setMandatory(true);
        template.setConfirmCallback((correlacion, ack, causa) -> {
            if (!ack) log.error("[RabbitMQ] El broker NO confirmo el mensaje: {}", causa);
        });
        template.setReturnsCallback(r -> log.error(
                "[RabbitMQ] Mensaje no enrutable (sin cola destino): exchange={} routingKey={} motivo={}",
                r.getExchange(), r.getRoutingKey(), r.getReplyText()));
        return template;
    }
}
