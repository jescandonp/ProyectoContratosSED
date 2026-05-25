-- ============================================================
-- SIGCON - Reset integral de datos de prueba
-- Schema: SED_SIGCON - Oracle 19c+
-- ============================================================
-- USO PREVISTO:
--   Ambientes locales, QA controlado o schemas clonados para pruebas.
--   NO ejecutar en produccion sin autorizacion DBA y backup.
--
-- OBJETIVO:
--   1. Eliminar datos operativos en orden seguro de dependencias.
--   2. Reiniciar secuencias principales para evitar colisiones.
--   3. Dejar usuarios base alineados con DevSecurityConfig local-dev.
--   4. Dejar contrato, obligaciones, catalogos y parametros base para
--      iniciar ciclos de prueba limpios.
--
-- MAPA DE DEPENDENCIAS:
--   SGCN_USUARIOS
--     -> SGCN_CONTRATOS
--       -> SGCN_OBLIGACIONES
--       -> SGCN_INFORMES
--          -> SGCN_ACTIVIDADES
--             -> SGCN_SOPORTES
--          -> SGCN_APORTES_SGSSI
--          -> SGCN_DOCS_ADICIONALES
--          -> SGCN_DOCS_REQUERIDOS
--          -> SGCN_OBSERVACIONES
--          -> SGCN_NOTIFICACIONES
--     -> SGCN_NOTIFICACIONES
--   SGCN_DOCS_CATALOGO
--     -> SGCN_DOCS_ADICIONALES
--
-- EJECUCION:
--   1. Revisar que el schema objetivo sea correcto.
--   2. Cambiar:
--        DEFINE SIGCON_RESET_DATOS_CONFIRM = 'RESET_DATOS_SIGCON'
--   3. Ejecutar con SQL*Plus/SQLcl conectado como propietario del schema.
-- ============================================================

DEFINE SIGCON_RESET_DATOS_CONFIRM = 'RESET_DATOS_SIGCON'

SET SERVEROUTPUT ON
WHENEVER SQLERROR EXIT ROLLBACK

DECLARE
    v_confirmacion VARCHAR2(50) := UPPER(TRIM('&SIGCON_RESET_DATOS_CONFIRM'));
BEGIN
    IF v_confirmacion <> 'RESET_DATOS_SIGCON' THEN
        RAISE_APPLICATION_ERROR(
            -20000,
            'Reset cancelado. Cambie SIGCON_RESET_DATOS_CONFIRM a RESET_DATOS_SIGCON para ejecutar.'
        );
    END IF;
END;
/

PROMPT ===== Usuario/schema objetivo =====
SELECT USER AS SCHEMA_ACTUAL FROM DUAL;

PROMPT ===== Conteos antes del reset =====
SELECT 'SGCN_SOPORTES' tabla, COUNT(*) total FROM SGCN_SOPORTES
UNION ALL SELECT 'SGCN_ACTIVIDADES', COUNT(*) FROM SGCN_ACTIVIDADES
UNION ALL SELECT 'SGCN_APORTES_SGSSI', COUNT(*) FROM SGCN_APORTES_SGSSI
UNION ALL SELECT 'SGCN_DOCS_ADICIONALES', COUNT(*) FROM SGCN_DOCS_ADICIONALES
UNION ALL SELECT 'SGCN_DOCS_REQUERIDOS', COUNT(*) FROM SGCN_DOCS_REQUERIDOS
UNION ALL SELECT 'SGCN_OBSERVACIONES', COUNT(*) FROM SGCN_OBSERVACIONES
UNION ALL SELECT 'SGCN_NOTIFICACIONES', COUNT(*) FROM SGCN_NOTIFICACIONES
UNION ALL SELECT 'SGCN_INFORMES', COUNT(*) FROM SGCN_INFORMES
UNION ALL SELECT 'SGCN_OBLIGACIONES', COUNT(*) FROM SGCN_OBLIGACIONES
UNION ALL SELECT 'SGCN_CONTRATOS', COUNT(*) FROM SGCN_CONTRATOS
UNION ALL SELECT 'SGCN_DOCS_CATALOGO', COUNT(*) FROM SGCN_DOCS_CATALOGO
UNION ALL SELECT 'SGCN_USUARIOS', COUNT(*) FROM SGCN_USUARIOS;

SAVEPOINT reset_datos_sigcon;

-- 1) Borrado operativo en orden de hojas a raices.
DELETE FROM SGCN_SOPORTES;
DELETE FROM SGCN_ACTIVIDADES;
DELETE FROM SGCN_APORTES_SGSSI;
DELETE FROM SGCN_DOCS_ADICIONALES;
DELETE FROM SGCN_DOCS_REQUERIDOS;
DELETE FROM SGCN_OBSERVACIONES;
DELETE FROM SGCN_NOTIFICACIONES;
DELETE FROM SGCN_INFORMES;
DELETE FROM SGCN_OBLIGACIONES;
DELETE FROM SGCN_CONTRATOS;
DELETE FROM SGCN_DOCS_CATALOGO;
DELETE FROM SGCN_USUARIOS;

-- 2) Compatibilidad I9: roles y estados nuevos deben existir en checks.
DECLARE
    PROCEDURE replace_check(
        p_table_name VARCHAR2,
        p_constraint_name VARCHAR2,
        p_sql VARCHAR2
    ) IS
        v_count NUMBER;
    BEGIN
        SELECT COUNT(*)
          INTO v_count
          FROM USER_CONSTRAINTS
         WHERE TABLE_NAME = UPPER(p_table_name)
           AND CONSTRAINT_NAME = UPPER(p_constraint_name);

        IF v_count > 0 THEN
            EXECUTE IMMEDIATE 'ALTER TABLE ' || p_table_name || ' DROP CONSTRAINT ' || p_constraint_name;
        END IF;

        EXECUTE IMMEDIATE p_sql;
        DBMS_OUTPUT.PUT_LINE('OK: constraint ' || p_constraint_name || ' normalizada.');
    END;
BEGIN
    replace_check(
        'SGCN_USUARIOS',
        'CHK_USUARIOS_ROL',
        q'[
            ALTER TABLE SGCN_USUARIOS ADD CONSTRAINT CHK_USUARIOS_ROL
            CHECK (ROL IN ('CONTRATISTA','REVISOR','SUPERVISOR','ADMINISTRATIVO','ADMIN'))
        ]'
    );

    replace_check(
        'SGCN_INFORMES',
        'CHK_INFORMES_ESTADO',
        q'[
            ALTER TABLE SGCN_INFORMES ADD CONSTRAINT CHK_INFORMES_ESTADO
            CHECK (ESTADO IN ('BORRADOR','ENVIADO','EN_VISTO_BUENO','EN_REVISION','DEVUELTO','APROBADO'))
        ]'
    );
END;
/

-- 3) Reinicio de secuencias. Se dejan desde 100 para reservar IDs 1..99
--    a datos base estables de pruebas.
DECLARE
    PROCEDURE reset_sequence(p_name VARCHAR2) IS
        v_count NUMBER;
    BEGIN
        SELECT COUNT(*)
          INTO v_count
          FROM USER_SEQUENCES
         WHERE SEQUENCE_NAME = UPPER(p_name);

        IF v_count > 0 THEN
            EXECUTE IMMEDIATE 'DROP SEQUENCE ' || p_name;
        END IF;

        EXECUTE IMMEDIATE 'CREATE SEQUENCE ' || p_name || ' START WITH 100 INCREMENT BY 1 NOCACHE';
        DBMS_OUTPUT.PUT_LINE('OK: secuencia ' || p_name || ' reiniciada en 100.');
    END;
BEGIN
    reset_sequence('SGCN_USUARIOS_SEQ');
    reset_sequence('SGCN_CONTRATOS_SEQ');
    reset_sequence('SGCN_OBLIGACIONES_SEQ');
    reset_sequence('SGCN_DOCS_CATALOGO_SEQ');
    reset_sequence('SGCN_INFORMES_SEQ');
    reset_sequence('SGCN_ACTIVIDADES_SEQ');
    reset_sequence('SGCN_SOPORTES_SEQ');
    reset_sequence('SGCN_DOCS_ADICIONALES_SEQ');
    reset_sequence('SGCN_OBSERVACIONES_SEQ');
    reset_sequence('SGCN_NOTIFICACIONES_SEQ');
    reset_sequence('SGCN_APORTES_SGSSI_SEQ');
    reset_sequence('SGCN_DOCS_REQUERIDOS_SEQ');
END;
/

-- 4) Usuarios base. Las credenciales HTTP Basic viven en DevSecurityConfig:
--    admin@educacionbogota.edu.co / admin123
--    juan.escandon@educacionbogota.edu.co / contratista123
--    aecheverry@educacionbogota.gov.co / contratista123
--    revisor1@educacionbogota.edu.co / revisor123
--    supervisor1@educacionbogota.edu.co / supervisor123
--    administrativo@educacionbogota.edu.co / admin123
INSERT INTO SGCN_USUARIOS
    (ID, EMAIL, NOMBRE, CARGO, ROL, ACTIVO, SGSSI_SALUD_ENTIDAD,
     SGSSI_PENSION_ENTIDAD, SGSSI_ARL_ENTIDAD, RESPONSABLE_IVA, CREATED_BY)
VALUES
    (1, 'admin@educacionbogota.edu.co', 'Administrador SIGCON', 'Administrador funcional',
     'ADMIN', 1, NULL, NULL, NULL, 0, 'reset-datos');

INSERT INTO SGCN_USUARIOS
    (ID, EMAIL, NOMBRE, CARGO, ROL, ACTIVO, SGSSI_SALUD_ENTIDAD,
     SGSSI_PENSION_ENTIDAD, SGSSI_ARL_ENTIDAD, RESPONSABLE_IVA, CREATED_BY)
VALUES
    (2, 'juan.escandon@educacionbogota.edu.co', 'Juan Escandon Perez', 'Contratista OPS',
     'CONTRATISTA', 1, 'EPS Sanitas', 'Porvenir', 'ARL Sura', 0, 'reset-datos');

INSERT INTO SGCN_USUARIOS
    (ID, EMAIL, NOMBRE, CARGO, ROL, ACTIVO, SGSSI_SALUD_ENTIDAD,
     SGSSI_PENSION_ENTIDAD, SGSSI_ARL_ENTIDAD, RESPONSABLE_IVA, CREATED_BY)
VALUES
    (3, 'aecheverry@educacionbogota.gov.co', 'Andres Echeverry', 'Contratista OPS IVA',
     'CONTRATISTA', 1, 'Compensar EPS', 'Proteccion', 'ARL Positiva', 1, 'reset-datos');

INSERT INTO SGCN_USUARIOS
    (ID, EMAIL, NOMBRE, CARGO, ROL, ACTIVO, RESPONSABLE_IVA, CREATED_BY)
VALUES
    (4, 'revisor1@educacionbogota.edu.co', 'Revisor SIGCON', 'Apoyo supervision',
     'REVISOR', 1, 0, 'reset-datos');

INSERT INTO SGCN_USUARIOS
    (ID, EMAIL, NOMBRE, CARGO, ROL, ACTIVO, RESPONSABLE_IVA, CREATED_BY)
VALUES
    (5, 'supervisor1@educacionbogota.edu.co', 'Supervisor SIGCON', 'Supervisor contractual',
     'SUPERVISOR', 1, 0, 'reset-datos');

INSERT INTO SGCN_USUARIOS
    (ID, EMAIL, NOMBRE, CARGO, ROL, ACTIVO, RESPONSABLE_IVA, CREATED_BY)
VALUES
    (6, 'administrativo@educacionbogota.edu.co', 'Administrativo SIGCON', 'Visto bueno administrativo',
     'ADMINISTRATIVO', 1, 0, 'reset-datos');

-- 5) Contratos base para pruebas.
INSERT INTO SGCN_CONTRATOS
    (ID, NUMERO, OBJETO, TIPO, VALOR_TOTAL, FECHA_INICIO, FECHA_FIN, ESTADO,
     ID_CONTRATISTA, ID_REVISOR, ID_SUPERVISOR, DEPENDENCIA, FORMA_PAGO,
     MODIFICACIONES, ACTIVO, CREATED_BY)
VALUES
    (1, 'CO1.PCCNTR 8504408 - 2025',
     'Prestacion de servicios profesionales de apoyo a la gestion contractual y administrativa de la SED.',
     'OPS', 48000000, DATE '2025-01-15', DATE '2025-12-31', 'EN_EJECUCION',
     2, 4, 5, 'Direccion de Contratacion',
     'Pagos mensuales previa presentacion y aprobacion del informe de actividades y soportes requeridos.',
     'No se han presentado', 1, 'reset-datos');

INSERT INTO SGCN_CONTRATOS
    (ID, NUMERO, OBJETO, TIPO, VALOR_TOTAL, FECHA_INICIO, FECHA_FIN, ESTADO,
     ID_CONTRATISTA, ID_REVISOR, ID_SUPERVISOR, DEPENDENCIA, FORMA_PAGO,
     MODIFICACIONES, ACTIVO, CREATED_BY)
VALUES
    (2, 'OPS-2026-IVA-001',
     'Prestacion de servicios profesionales para validar flujo de documentos requeridos con responsable IVA.',
     'OPS', 36000000, DATE '2026-01-01', DATE '2026-12-31', 'EN_EJECUCION',
     3, 4, 5, 'Oficina de Tecnologia',
     'Pagos mensuales contra informe, aportes SGSSI y documentos tributarios aplicables.',
     'No se han presentado', 1, 'reset-datos');

-- 6) Obligaciones base.
INSERT INTO SGCN_OBLIGACIONES (ID, ID_CONTRATO, DESCRIPCION, ORDEN, ACTIVO, CREATED_BY)
VALUES (1, 1, 'Apoyar la organizacion, seguimiento y control de actividades contractuales asignadas.', 1, 1, 'reset-datos');
INSERT INTO SGCN_OBLIGACIONES (ID, ID_CONTRATO, DESCRIPCION, ORDEN, ACTIVO, CREATED_BY)
VALUES (2, 1, 'Elaborar informes, actas y documentos de seguimiento solicitados por la supervision.', 2, 1, 'reset-datos');
INSERT INTO SGCN_OBLIGACIONES (ID, ID_CONTRATO, DESCRIPCION, ORDEN, ACTIVO, CREATED_BY)
VALUES (3, 1, 'Atender reuniones de seguimiento y registrar compromisos derivados del contrato.', 3, 1, 'reset-datos');

INSERT INTO SGCN_OBLIGACIONES (ID, ID_CONTRATO, DESCRIPCION, ORDEN, ACTIVO, CREATED_BY)
VALUES (4, 2, 'Ejecutar actividades tecnicas asignadas y reportar avances verificables.', 1, 1, 'reset-datos');
INSERT INTO SGCN_OBLIGACIONES (ID, ID_CONTRATO, DESCRIPCION, ORDEN, ACTIVO, CREATED_BY)
VALUES (5, 2, 'Entregar soportes documentales, factura o documento equivalente cuando aplique.', 2, 1, 'reset-datos');
INSERT INTO SGCN_OBLIGACIONES (ID, ID_CONTRATO, DESCRIPCION, ORDEN, ACTIVO, CREATED_BY)
VALUES (6, 2, 'Mantener actualizada la informacion de aportes al sistema de seguridad social.', 3, 1, 'reset-datos');

-- 7) Catalogo base. Los documentos requeridos por informe se crean en el flujo,
--    este catalogo conserva documentos adicionales administrables.
INSERT INTO SGCN_DOCS_CATALOGO
    (ID, NOMBRE, DESCRIPCION, OBLIGATORIO, TIPO_CONTRATO, ACTIVO, CREATED_BY)
VALUES
    (1, 'Planilla de aportes seguridad social',
     'Comprobante de pago al sistema de seguridad social', 1, 'OPS', 1, 'reset-datos');

INSERT INTO SGCN_DOCS_CATALOGO
    (ID, NOMBRE, DESCRIPCION, OBLIGATORIO, TIPO_CONTRATO, ACTIVO, CREATED_BY)
VALUES
    (2, 'Constancia de afiliacion EPS',
     'Certificado vigente de afiliacion a EPS', 0, 'OPS', 1, 'reset-datos');

INSERT INTO SGCN_DOCS_CATALOGO
    (ID, NOMBRE, DESCRIPCION, OBLIGATORIO, TIPO_CONTRATO, ACTIVO, CREATED_BY)
VALUES
    (3, 'Certificacion bancaria',
     'Soporte bancario opcional para validaciones administrativas', 0, 'OPS', 1, 'reset-datos');

-- 8) Parametros de sistema. Se conserva la tabla y se fuerza la llave base
--    para probar el flujo de visto bueno administrativo.
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
        DBMS_OUTPUT.PUT_LINE('OK: tabla SGCN_PARAMETROS creada.');
    ELSE
        DBMS_OUTPUT.PUT_LINE('SKIP: tabla SGCN_PARAMETROS ya existe.');
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
    UPDATE SET p.VALOR = src.VALOR, p.DESCRIPCION = src.DESCRIPCION
WHEN NOT MATCHED THEN
    INSERT (CLAVE, VALOR, DESCRIPCION)
    VALUES (src.CLAVE, src.VALOR, src.DESCRIPCION);

-- 9) Validaciones finales de integridad minima.
DECLARE
    v_usuarios NUMBER;
    v_contratos NUMBER;
    v_obligaciones NUMBER;
    v_catalogo NUMBER;
    v_parametros NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_usuarios FROM SGCN_USUARIOS WHERE ACTIVO = 1;
    SELECT COUNT(*) INTO v_contratos FROM SGCN_CONTRATOS WHERE ACTIVO = 1;
    SELECT COUNT(*) INTO v_obligaciones FROM SGCN_OBLIGACIONES WHERE ACTIVO = 1;
    SELECT COUNT(*) INTO v_catalogo FROM SGCN_DOCS_CATALOGO WHERE ACTIVO = 1;
    SELECT COUNT(*) INTO v_parametros FROM SGCN_PARAMETROS WHERE CLAVE = 'VB_ACTIVO' AND VALOR = 'S';

    IF v_usuarios <> 6 THEN
        RAISE_APPLICATION_ERROR(-20001, 'Reset incompleto: usuarios base esperados = 6.');
    END IF;
    IF v_contratos <> 2 THEN
        RAISE_APPLICATION_ERROR(-20002, 'Reset incompleto: contratos base esperados = 2.');
    END IF;
    IF v_obligaciones <> 6 THEN
        RAISE_APPLICATION_ERROR(-20003, 'Reset incompleto: obligaciones base esperadas = 6.');
    END IF;
    IF v_catalogo <> 3 THEN
        RAISE_APPLICATION_ERROR(-20004, 'Reset incompleto: catalogo base esperado = 3.');
    END IF;
    IF v_parametros <> 1 THEN
        RAISE_APPLICATION_ERROR(-20005, 'Reset incompleto: parametro VB_ACTIVO no quedo activo.');
    END IF;
END;
/

COMMIT;

PROMPT ===== Conteos despues del reset =====
SELECT 'SGCN_SOPORTES' tabla, COUNT(*) total FROM SGCN_SOPORTES
UNION ALL SELECT 'SGCN_ACTIVIDADES', COUNT(*) FROM SGCN_ACTIVIDADES
UNION ALL SELECT 'SGCN_APORTES_SGSSI', COUNT(*) FROM SGCN_APORTES_SGSSI
UNION ALL SELECT 'SGCN_DOCS_ADICIONALES', COUNT(*) FROM SGCN_DOCS_ADICIONALES
UNION ALL SELECT 'SGCN_DOCS_REQUERIDOS', COUNT(*) FROM SGCN_DOCS_REQUERIDOS
UNION ALL SELECT 'SGCN_OBSERVACIONES', COUNT(*) FROM SGCN_OBSERVACIONES
UNION ALL SELECT 'SGCN_NOTIFICACIONES', COUNT(*) FROM SGCN_NOTIFICACIONES
UNION ALL SELECT 'SGCN_INFORMES', COUNT(*) FROM SGCN_INFORMES
UNION ALL SELECT 'SGCN_OBLIGACIONES', COUNT(*) FROM SGCN_OBLIGACIONES
UNION ALL SELECT 'SGCN_CONTRATOS', COUNT(*) FROM SGCN_CONTRATOS
UNION ALL SELECT 'SGCN_DOCS_CATALOGO', COUNT(*) FROM SGCN_DOCS_CATALOGO
UNION ALL SELECT 'SGCN_USUARIOS', COUNT(*) FROM SGCN_USUARIOS;

PROMPT ===== Usuarios disponibles para pruebas local-dev =====
SELECT EMAIL, ROL, ACTIVO
  FROM SGCN_USUARIOS
 ORDER BY ID;

PROMPT Reset integral de datos de prueba finalizado.
