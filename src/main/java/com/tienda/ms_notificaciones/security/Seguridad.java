package com.tienda.ms_notificaciones.security;

/**
 * Expresiones de autorizacion reutilizables con @PreAuthorize.
 *
 * - El SCOPE viene en el claim "scp" del token (se define en Azure > Exponer una API).
 * - Los ROLES vienen en el claim "roles" (se definen en Azure > Roles de aplicacion).
 */
public final class Seguridad {

    private Seguridad() {}

    /** Scope delegado que expone la API en Azure (api://CLIENT_ID/Menu-compra). */
    public static final String SCOPE = "hasAuthority('SCOPE_Menu-compra')";

    /** Usuario autenticado con scope y rol CLIENTE o ADMIN. */
    public static final String CLIENTE = SCOPE + " and hasAnyRole('CLIENTE','ADMIN')";

    /** Usuario autenticado con scope y rol ADMIN. */
    public static final String ADMIN = SCOPE + " and hasRole('ADMIN')";
}
