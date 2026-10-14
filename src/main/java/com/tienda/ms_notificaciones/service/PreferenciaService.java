package com.tienda.ms_notificaciones.service;

import com.tienda.ms_notificaciones.model.PreferenciaNotificacion;
import com.tienda.ms_notificaciones.repository.PreferenciaNotificacionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/** Preferencias de notificacion por usuario. Por defecto el correo de compra esta ACTIVADO. */
@Service
public class PreferenciaService {

    private final PreferenciaNotificacionRepository repo;

    public PreferenciaService(PreferenciaNotificacionRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public boolean recibeCorreoCompra(String usuarioId) {
        return repo.findById(usuarioId).map(PreferenciaNotificacion::isCorreoCompra).orElse(true);
    }

    @Transactional
    public PreferenciaNotificacion guardar(String usuarioId, String email, boolean correoCompra) {
        PreferenciaNotificacion p = repo.findById(usuarioId).orElseGet(() -> new PreferenciaNotificacion(usuarioId));
        p.setEmail(email);
        p.setCorreoCompra(correoCompra);
        p.setActualizado(Instant.now());
        return repo.save(p);
    }
}
