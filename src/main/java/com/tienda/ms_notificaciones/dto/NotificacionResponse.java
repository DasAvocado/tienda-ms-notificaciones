package com.tienda.ms_notificaciones.dto;

/** estado: ENVIADO (SMTP real) | SIMULADO (sin SMTP configurado) | ERROR */
public record NotificacionResponse(String estado, String destinatario, String asunto, String detalle) {}
