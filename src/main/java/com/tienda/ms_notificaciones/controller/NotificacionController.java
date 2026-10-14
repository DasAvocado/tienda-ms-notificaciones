package com.tienda.ms_notificaciones.controller;

import com.tienda.ms_notificaciones.dto.EnviarRequest;
import com.tienda.ms_notificaciones.dto.NotificacionResponse;
import com.tienda.ms_notificaciones.service.NotificacionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * POST /api/notificaciones/enviar   {"to":"...","subject":"...","message":"..."}
 * Permite disparar un correo directamente desde Postman (requisito del enunciado). Solo ADMIN (ver SecurityConfig).
 */
@RestController
@RequestMapping("/api/notificaciones")
public class NotificacionController {

    private final NotificacionService service;

    public NotificacionController(NotificacionService service) {
        this.service = service;
    }

    @PostMapping("/enviar")
    public ResponseEntity<NotificacionResponse> enviar(@Valid @RequestBody EnviarRequest req) {
        String asunto = req.subject() == null || req.subject().isBlank() ? "Notificacion Pedidos360" : req.subject();
        String mensaje = req.message() == null ? "" : req.message();
        return ResponseEntity.ok(service.enviar(req.to(), asunto, mensaje));
    }
}
