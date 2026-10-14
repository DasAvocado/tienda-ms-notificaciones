package com.tienda.ms_notificaciones.dto;

import java.util.List;

public record OrdenCreadaEvent(
        Long ordenId,
        String usuarioId,
        String email,
        String nombre,
        double total,
        String fecha,
        List<Item> items) {

    public record Item(Long productoId, String nombre, double precio, int cantidad) {}
}
