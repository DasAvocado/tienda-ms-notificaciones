package com.tienda.ms_notificaciones.dto;

/** Cuerpo de POST /api/notificaciones/enviar */
public record EnviarRequest(String to, String subject, String message) {}
