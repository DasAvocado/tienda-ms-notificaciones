package com.tienda.ms_notificaciones.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Cuerpo de POST /api/notificaciones/enviar */
public record EnviarRequest(
        @NotBlank(message = "El campo 'to' es obligatorio")
        @Email(message = "El campo 'to' debe ser un correo valido")
        String to,
        @Size(max = 200, message = "El asunto no puede superar 200 caracteres")
        String subject,
        @Size(max = 5000, message = "El mensaje no puede superar 5000 caracteres")
        String message) {}
