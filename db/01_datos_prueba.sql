-- ============================================================
-- SIGCON I1 — Datos de prueba (local-dev only)
-- ============================================================

-- USUARIOS
-- IMPORTANT: usernames match DevSecurityConfig HTTP Basic credentials
-- admin@educacionbogota.edu.co / admin123
-- juan.escandon@educacionbogota.edu.co / contratista123
-- revisor1@educacionbogota.edu.co / revisor123
-- supervisor1@educacionbogota.edu.co / supervisor123
INSERT INTO SGCN_USUARIOS (EMAIL, NOMBRE, CARGO, ROL) VALUES
  ('admin@educacionbogota.edu.co', 'Administrador SIGCON', 'Jefe de Sistemas', 'ADMIN');
INSERT INTO SGCN_USUARIOS (EMAIL, NOMBRE, CARGO, ROL) VALUES
  ('juan.escandon@educacionbogota.edu.co', 'Juan Escandón Pérez', 'Contratista OPS', 'CONTRATISTA');
INSERT INTO SGCN_USUARIOS (EMAIL, NOMBRE, CARGO, ROL) VALUES
  ('revisor1@educacionbogota.edu.co', 'María Revisora', 'Apoyo Supervisión', 'REVISOR');
INSERT INTO SGCN_USUARIOS (EMAIL, NOMBRE, CARGO, ROL) VALUES
  ('supervisor1@educacionbogota.edu.co', 'Carlos Supervisor', 'Supervisor Contractual', 'SUPERVISOR');

-- CONTRATO
INSERT INTO SGCN_CONTRATOS (NUMERO, OBJETO, TIPO, VALOR_TOTAL, FECHA_INICIO, FECHA_FIN,
                             ESTADO, ID_CONTRATISTA, ID_REVISOR, ID_SUPERVISOR)
VALUES ('OPS-2026-001',
        'Prestación de servicios de apoyo en gestión documental y administrativa',
        'OPS', 18000000, DATE '2026-01-15', DATE '2026-12-31',
        'EN_EJECUCION', 2, 3, 4);

-- OBLIGACIONES
INSERT INTO SGCN_OBLIGACIONES (ID_CONTRATO, DESCRIPCION, ORDEN) VALUES
  (1, 'Apoyar la organización y clasificación del archivo físico y digital de la dependencia', 1);
INSERT INTO SGCN_OBLIGACIONES (ID_CONTRATO, DESCRIPCION, ORDEN) VALUES
  (1, 'Elaborar y radicar comunicaciones oficiales según instrucciones del supervisor', 2);
INSERT INTO SGCN_OBLIGACIONES (ID_CONTRATO, DESCRIPCION, ORDEN) VALUES
  (1, 'Asistir a reuniones de seguimiento y elaborar actas de las mismas', 3);

-- CATÁLOGO DE DOCUMENTOS
INSERT INTO SGCN_DOCS_CATALOGO (NOMBRE, DESCRIPCION, OBLIGATORIO, TIPO_CONTRATO) VALUES
  ('Planilla de aportes seguridad social', 'Comprobante de pago al sistema de seguridad social', 1, 'OPS');
INSERT INTO SGCN_DOCS_CATALOGO (NOMBRE, DESCRIPCION, OBLIGATORIO, TIPO_CONTRATO) VALUES
  ('Constancia de afiliación EPS', 'Certificado vigente de afiliación a EPS', 0, 'OPS');

COMMIT;
