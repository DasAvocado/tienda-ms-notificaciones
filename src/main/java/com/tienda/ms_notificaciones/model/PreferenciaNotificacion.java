package com.tienda.ms_notificaciones.model;

import jakarta.persistence.*;

import java.time.Instant;

/** Preferencias de notificacion de cada usuario (clave = "oid" del token de Entra ID). */
@Entity
@Table(name = "preferencias_notificacion")
public class PreferenciaNotificacion {

    @Id
    @Column(name = "usuario_id", length = 64)
    private String usuarioId;

    /** Correo con el que inicio sesion la ultima vez que guardo sus preferencias. */
    private String email;

    /** true = enviarme un correo de confirmacion cada vez que compro. */
    @Column(name = "correo_compra", nullable = false)
    private boolean correoCompra = true;

    private Instant actualizado;

    public PreferenciaNotificacion() {}

    public PreferenciaNotificacion(String usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getUsuarioId() { return usuarioId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public boolean isCorreoCompra() { return correoCompra; }
    public void setCorreoCompra(boolean correoCompra) { this.correoCompra = correoCompra; }

    public Instant getActualizado() { return actualizado; }
    public void setActualizado(Instant actualizado) { this.actualizado = actualizado; }
}
