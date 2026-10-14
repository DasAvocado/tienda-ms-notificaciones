package com.tienda.ms_notificaciones.controller;

import com.tienda.ms_notificaciones.dto.NotificacionResponse;
import com.tienda.ms_notificaciones.dto.PreferenciaRequest;
import com.tienda.ms_notificaciones.dto.PreferenciaResponse;
import com.tienda.ms_notificaciones.security.Seguridad;
import com.tienda.ms_notificaciones.security.UsuarioActual;
import com.tienda.ms_notificaciones.service.ConfirmacionCompraService;
import com.tienda.ms_notificaciones.service.NotificacionService;
import com.tienda.ms_notificaciones.service.PreferenciaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/**
 * Opcion "Notificaciones" del usuario conectado (scope Menu-compra + rol CLIENTE/ADMIN):
 *
 *   GET  /api/notificaciones/preferencias          -> {email, destino, correoCompra, modoEnvio}
 *   PUT  /api/notificaciones/preferencias          -> {"correoCompra": true|false}
 *   POST /api/notificaciones/preferencias/prueba   -> envia un correo de prueba al correo de la sesion
 *
 * El correo SIEMPRE se toma del JWT (no del cuerpo): un usuario no puede pedir que se envien correos a terceros.
 */
@RestController
@RequestMapping("/api/notificaciones/preferencias")
@PreAuthorize(Seguridad.CLIENTE)
public class PreferenciaController {

    private final PreferenciaService preferencias;
    private final ConfirmacionCompraService confirmacionCompra;
    private final NotificacionService correo;

    public PreferenciaController(PreferenciaService preferencias,
                                 ConfirmacionCompraService confirmacionCompra,
                                 NotificacionService correo) {
        this.preferencias = preferencias;
        this.confirmacionCompra = confirmacionCompra;
        this.correo = correo;
    }

    @GetMapping
    public PreferenciaResponse obtener(@AuthenticationPrincipal Jwt jwt) {
        return respuesta(jwt, preferencias.recibeCorreoCompra(UsuarioActual.id(jwt)));
    }

    @PutMapping
    public PreferenciaResponse guardar(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody PreferenciaRequest req) {
        preferencias.guardar(UsuarioActual.id(jwt), UsuarioActual.email(jwt), req.correoCompra());
        return respuesta(jwt, req.correoCompra());
    }

    @PostMapping("/prueba")
    public NotificacionResponse prueba(@AuthenticationPrincipal Jwt jwt) {
        String email = UsuarioActual.email(jwt);
        if (email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Tu token no trae un correo (claims email / preferred_username)");
        }
        return confirmacionCompra.enviarPrueba(email, UsuarioActual.nombre(jwt));
    }

    private PreferenciaResponse respuesta(Jwt jwt, boolean correoCompra) {
        String email = UsuarioActual.email(jwt);
        return new PreferenciaResponse(email, confirmacionCompra.destinoPara(email), correoCompra,
                correo.smtpConfigurado() ? "SMTP" : NotificacionService.SIMULADO);
    }
}
