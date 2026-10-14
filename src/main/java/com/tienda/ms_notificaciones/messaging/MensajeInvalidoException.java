package com.tienda.ms_notificaciones.messaging;

/** Error NO recuperable: el mensaje tiene datos invalidos y reintentar no sirve -> va directo a la DLQ. */
public class MensajeInvalidoException extends RuntimeException {
    public MensajeInvalidoException(String mensaje) {
        super(mensaje);
    }
}
