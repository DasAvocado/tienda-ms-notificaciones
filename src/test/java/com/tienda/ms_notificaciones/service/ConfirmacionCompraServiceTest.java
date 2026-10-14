package com.tienda.ms_notificaciones.service;

import com.tienda.ms_notificaciones.dto.NotificacionEnviadaEvent;
import com.tienda.ms_notificaciones.dto.NotificacionResponse;
import com.tienda.ms_notificaciones.dto.OrdenCreadaEvent;
import com.tienda.ms_notificaciones.messaging.NotificacionPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ConfirmacionCompraServiceTest {

    private NotificacionService correo;
    private PreferenciaService preferencias;
    private NotificacionPublisher publisher;

    private final OrdenCreadaEvent evento = new OrdenCreadaEvent(42L, "u1", "cliente@correo.cl", "Ana", 30000,
            "2026-01-01T00:00:00Z", List.of(new OrdenCreadaEvent.Item(1L, "Filtro", 15000, 2)));

    @BeforeEach
    void setUp() {
        correo = mock(NotificacionService.class);
        preferencias = mock(PreferenciaService.class);
        publisher = mock(NotificacionPublisher.class);
    }

    @Test
    void enviaAlCorreoDeLaSesion() {
        when(preferencias.recibeCorreoCompra("u1")).thenReturn(true);
        when(correo.enviar(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new NotificacionResponse("SIMULADO", "cliente@correo.cl", "x", ""));

        new ConfirmacionCompraService(correo, preferencias, publisher, "").notificarCompra(evento);

        verify(correo).enviar(eq("cliente@correo.cl"), contains("#42"), contains("Filtro"), contains("Filtro"));
        verify(publisher).notificacionEnviada(any(NotificacionEnviadaEvent.class));
    }

    @Test
    void noEnviaSiElUsuarioLoDesactivo() {
        when(preferencias.recibeCorreoCompra("u1")).thenReturn(false);

        var resp = new ConfirmacionCompraService(correo, preferencias, publisher, "").notificarCompra(evento);

        assertEquals(ConfirmacionCompraService.OMITIDO, resp.estado());
        verify(correo, never()).enviar(anyString(), anyString(), anyString(), anyString());
        ArgumentCaptor<NotificacionEnviadaEvent> ev = ArgumentCaptor.forClass(NotificacionEnviadaEvent.class);
        verify(publisher).notificacionEnviada(ev.capture());
        assertEquals(ConfirmacionCompraService.OMITIDO, ev.getValue().estado());
    }

    @Test
    void errorSmtpLanzaExcepcionParaReintentar() {
        when(preferencias.recibeCorreoCompra("u1")).thenReturn(true);
        when(correo.enviar(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new NotificacionResponse(NotificacionService.ERROR, "cliente@correo.cl", "x", "timeout"));

        var service = new ConfirmacionCompraService(correo, preferencias, publisher, "");
        assertThrows(IllegalStateException.class, () -> service.notificarCompra(evento));
        verify(publisher, never()).notificacionEnviada(any());
    }

    @Test
    void overrideRedirigeElCorreo() {
        var service = new ConfirmacionCompraService(correo, preferencias, publisher, "pruebas@gmail.com");
        assertEquals("pruebas@gmail.com", service.destinoPara("cliente@correo.cl"));
        assertEquals("cliente@correo.cl",
                new ConfirmacionCompraService(correo, preferencias, publisher, "").destinoPara("cliente@correo.cl"));
    }
}
