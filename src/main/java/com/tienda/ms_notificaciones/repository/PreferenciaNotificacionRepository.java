package com.tienda.ms_notificaciones.repository;

import com.tienda.ms_notificaciones.model.PreferenciaNotificacion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PreferenciaNotificacionRepository extends JpaRepository<PreferenciaNotificacion, String> {
}
