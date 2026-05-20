-- ============================================================
-- SIGCON I9 — Accion en observaciones de informe
-- Schema: SED_SIGCON · Oracle 19c+
-- ============================================================
-- PREREQUISITES:
--   - Connected as SED_SIGCON schema owner
--   - Oracle 19c+
-- IDEMPOTENT: Safe to re-run; adds ACCION only if missing.
-- Valores esperados por aplicacion:
--   VISTO_BUENO | DEVOLUCION | ESCALACION
-- NULL conserva retrocompatibilidad para observaciones previas.
-- DO NOT execute in production without DBA review.
-- ============================================================

SET SERVEROUTPUT ON
WHENEVER SQLERROR EXIT ROLLBACK

DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*)
    INTO v_count
    FROM USER_TAB_COLUMNS
    WHERE TABLE_NAME = 'SGCN_OBSERVACIONES'
      AND COLUMN_NAME = 'ACCION';

    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE SGCN_OBSERVACIONES ADD ACCION VARCHAR2(20) DEFAULT NULL';
        DBMS_OUTPUT.PUT_LINE('Columna SGCN_OBSERVACIONES.ACCION creada.');
    ELSE
        DBMS_OUTPUT.PUT_LINE('Columna SGCN_OBSERVACIONES.ACCION ya existe; se conserva.');
    END IF;
END;
/

COMMIT;

PROMPT SIGCON I9: columna ACCION en SGCN_OBSERVACIONES lista.
