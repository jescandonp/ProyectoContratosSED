-- SIGCON I12 - R1: bandera de contratista con funciones administrativas.
-- Ejecutar una vez antes de habilitar usuarios duales en ambientes existentes.

ALTER TABLE SGCN_USUARIOS ADD ES_ADMIN NUMBER(1) DEFAULT 0 NOT NULL;

ALTER TABLE SGCN_USUARIOS ADD CONSTRAINT CHK_USUARIOS_ES_ADMIN
CHECK (ES_ADMIN IN (0, 1));

UPDATE SGCN_USUARIOS
   SET ES_ADMIN = 0
 WHERE ES_ADMIN IS NULL;

COMMIT;
