package com.tienda.ms_notificaciones.security;

import org.springframework.security.oauth2.jwt.Jwt;

/** Datos del usuario extraidos del JWT emitido por Microsoft Entra ID. */
public final class UsuarioActual {

    private UsuarioActual() {}

    /** Identificador estable del usuario en el tenant (claim "oid"; si no existe, "sub"). */
    public static String id(Jwt jwt) {
        String oid = jwt.getClaimAsString("oid");
        return oid != null ? oid : jwt.getSubject();
    }

    /** Correo / UPN del usuario. */
    public static String email(Jwt jwt) {
        String email = jwt.getClaimAsString("preferred_username");
        if (email == null) email = jwt.getClaimAsString("email");
        return email != null ? email : "";
    }

    public static String nombre(Jwt jwt) {
        String nombre = jwt.getClaimAsString("name");
        return nombre != null ? nombre : email(jwt);
    }
}
