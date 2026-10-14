package com.tienda.ms_notificaciones.dto;

import jakarta.validation.constraints.NotNull;

/** Cuerpo de PUT /api/notificaciones/preferencias */
public record PreferenciaRequest(@NotNull(message = "correoCompra es obligatorio (true/false)") Boolean correoCompra) {}
