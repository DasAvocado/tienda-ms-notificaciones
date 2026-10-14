package com.tienda.ms_notificaciones.dto;

/** Evento "notificacion.enviada" (lo registra ms-auditoria). */
public record NotificacionEnviadaEvent(Long ordenId, String destinatario, String asunto, String estado) {}
