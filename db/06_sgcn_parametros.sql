-- ============================================================
-- SIGCON I9 — Parametros de sistema
-- Schema: SED_SIGCON · Oracle 19c+
-- ============================================================
-- PREREQUISITES:
--   - Connected as SED_SIGCON schema owner
--   - Oracle 19c+
-- IDEMPOTENT: Safe to re-run; creates table only if missing and
--             inserts VB_ACTIVO=S only if the key does not exist.
--             Existing runtime values are preserved.
-- DO NOT execute in production without DBA review.
-- ============================================================

SET SERVEROUTPUT ON
WHENEVER SQLERROR EXIT ROLLBACK

DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*)
    INTO v_count
    FROM USER_TABLES
    WHERE TABLE_NAME = 'SGCN_PARAMETROS';

    IF v_count = 0 THEN
        EXECUTE IMMEDIATE '
            CREATE TABLE SGCN_PARAMETROS (
                CLAVE        VARCHAR2(50)  NOT NULL,
                VALOR        VARCHAR2(200) NOT NULL,
                DESCRIPCION  VARCHAR2(500),
                CONSTRAINT PK_SGCN_PARAMETROS PRIMARY KEY (CLAVE)
            )';

        DBMS_OUTPUT.PUT_LINE('Tabla SGCN_PARAMETROS creada.');
    ELSE
        DBMS_OUTPUT.PUT_LINE('Tabla SGCN_PARAMETROS ya existe; se conserva.');
    END IF;
END;
/

MERGE INTO SGCN_PARAMETROS p
USING (
    SELECT
        'VB_ACTIVO' AS CLAVE,
        'S' AS VALOR,
        'Visto Bueno Administrativo activo en el flujo de informes' AS DESCRIPCION
    FROM DUAL
) src
ON (p.CLAVE = src.CLAVE)
WHEN MATCHED THEN
    UPDATE SET p.DESCRIPCION = src.DESCRIPCION
WHEN NOT MATCHED THEN
    INSERT (CLAVE, VALOR, DESCRIPCION)
    VALUES (src.CLAVE, src.VALOR, src.DESCRIPCION);

COMMIT;

PROMPT SIGCON I9: SGCN_PARAMETROS listo.
