-- ============================================================
-- WARNING: LOCAL-DEV ONLY
-- Repara la coherencia entre DevSecurityConfig/DevSessionService
-- y SGCN_USUARIOS sin borrar datos existentes.
-- ============================================================

MERGE INTO SGCN_USUARIOS u
USING (
  SELECT 'admin@educacionbogota.edu.co' EMAIL, 'Administrador SIGCON' NOMBRE, 'Jefe de Sistemas' CARGO, 'ADMIN' ROL FROM dual
) src
ON (u.EMAIL = src.EMAIL)
WHEN MATCHED THEN UPDATE SET u.NOMBRE = src.NOMBRE, u.CARGO = src.CARGO, u.ROL = src.ROL, u.ACTIVO = 1
WHEN NOT MATCHED THEN INSERT (EMAIL, NOMBRE, CARGO, ROL, ACTIVO)
VALUES (src.EMAIL, src.NOMBRE, src.CARGO, src.ROL, 1);

MERGE INTO SGCN_USUARIOS u
USING (
  SELECT 'juan.escandon@educacionbogota.edu.co' EMAIL, 'Juan Escandon Perez' NOMBRE, 'Contratista OPS' CARGO, 'CONTRATISTA' ROL FROM dual
) src
ON (u.EMAIL = src.EMAIL)
WHEN MATCHED THEN UPDATE SET u.NOMBRE = src.NOMBRE, u.CARGO = src.CARGO, u.ROL = src.ROL, u.ACTIVO = 1
WHEN NOT MATCHED THEN INSERT (EMAIL, NOMBRE, CARGO, ROL, ACTIVO)
VALUES (src.EMAIL, src.NOMBRE, src.CARGO, src.ROL, 1);

MERGE INTO SGCN_USUARIOS u
USING (
  SELECT 'revisor1@educacionbogota.edu.co' EMAIL, 'Revisor SIGCON' NOMBRE, 'Apoyo Supervision' CARGO, 'REVISOR' ROL FROM dual
) src
ON (u.EMAIL = src.EMAIL)
WHEN MATCHED THEN UPDATE SET u.NOMBRE = src.NOMBRE, u.CARGO = src.CARGO, u.ROL = src.ROL, u.ACTIVO = 1
WHEN NOT MATCHED THEN INSERT (EMAIL, NOMBRE, CARGO, ROL, ACTIVO)
VALUES (src.EMAIL, src.NOMBRE, src.CARGO, src.ROL, 1);

MERGE INTO SGCN_USUARIOS u
USING (
  SELECT 'supervisor1@educacionbogota.edu.co' EMAIL, 'Supervisor SIGCON' NOMBRE, 'Supervisor Contractual' CARGO, 'SUPERVISOR' ROL FROM dual
) src
ON (u.EMAIL = src.EMAIL)
WHEN MATCHED THEN UPDATE SET u.NOMBRE = src.NOMBRE, u.CARGO = src.CARGO, u.ROL = src.ROL, u.ACTIVO = 1
WHEN NOT MATCHED THEN INSERT (EMAIL, NOMBRE, CARGO, ROL, ACTIVO)
VALUES (src.EMAIL, src.NOMBRE, src.CARGO, src.ROL, 1);

COMMIT;
