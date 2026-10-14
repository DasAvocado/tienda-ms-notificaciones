package com.tienda.ms_notificaciones.dto;

/**
 * @param email        correo con el que el usuario inicio sesion
 * @param destino      casilla a la que realmente se enviara (igual a email salvo override)
 * @param correoCompra true = recibir correo al comprar
 * @param modoEnvio    SMTP (correo real) | SIMULADO (solo en el log del servicio)
 */
public record PreferenciaResponse(String email, String destino, boolean correoCompra, String modoEnvio) {}
