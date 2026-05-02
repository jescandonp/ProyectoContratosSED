package co.gov.bogota.sed.sigcon.domain.enums;

/**
 * Tipos de evento que generan notificaciones in-app y correo electronico
 * en el flujo de informes de I3.
 *
 * <p>Los valores deben coincidir exactamente con el CHECK constraint
 * CHK_NOTIFICACIONES_EVENTO en SGCN_NOTIFICACIONES.</p>
 */
public enum TipoEvento {

    /** Contratista envia el informe al revisor. */
    INFORME_ENVIADO,

    /** Revisor aprueba la revision; informe pasa a supervision. */
    REVISION_APROBADA,

    /** Revisor devuelve el informe al contratista para correccion. */
    REVISION_DEVUELTA,

    /** Supervisor aprueba el informe definitivamente; PDF generado. */
    INFORME_APROBADO,

    /** Supervisor devuelve el informe al contratista sin aprobarlo. */
    INFORME_DEVUELTO
}
