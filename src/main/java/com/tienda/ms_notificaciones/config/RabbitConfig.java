package com.tienda.ms_notificaciones.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** CONSUME "orden.creada"; PUBLICA "notificacion.enviada". */
@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "pedidos360.events";
    public static final String RK_ORDEN_CREADA = "orden.creada";
    public static final String RK_NOTIFICACION_ENVIADA = "notificacion.enviada";
    public static final String QUEUE_ORDEN_CREADA = "notificaciones.orden-creada";

    @Bean
    public TopicExchange eventosExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue colaOrdenCreada() {
        return QueueBuilder.durable(QUEUE_ORDEN_CREADA).build();
    }

    @Bean
    public Binding bindingOrdenCreada() {
        return BindingBuilder.bind(colaOrdenCreada()).to(eventosExchange()).with(RK_ORDEN_CREADA);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
