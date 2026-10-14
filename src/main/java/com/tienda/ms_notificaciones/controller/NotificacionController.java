package com.tienda.ms_notificaciones.controller;

import com.tienda.ms_notificaciones.dto.EnviarRequest;
import com.tienda.ms_notificaciones.dto.NotificacionResponse;
import com.tienda.ms_notificaciones.service.NotificacionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * POST /api/notificaciones/enviar   {"to":"...","subject":"...","message":"..."}
 * Permite disparar un correo directamente desde Postman (requisito del enunciado).
 */
@RestController
@RequestMapping("/api/notificaciones")
public class NotificacionController {

    private final NotificacionService service;

    public NotificacionController(NotificacionService service) {
        this.service = service;
    }

    @PostMapping("/enviar")
    public ResponseEntity<NotificacionResponse> enviar(@RequestBody EnviarRequest req) {
        if (req.to() == null || req.to().isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new NotificacionResponse("ERROR", null, req.subject(), "El campo 'to' es obligatorio"));
        }
        String asunto = req.subject() == null ? "Notificacion Pedidos360" : req.subject();
        String mensaje = req.message() == null ? "" : req.message();
        return ResponseEntity.ok(service.enviar(req.to(), asunto, mensaje));
    }
}
