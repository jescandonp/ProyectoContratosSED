# SIGCON SDD Constitution

> Estado: Activa para todos los incrementos SIGCON.
> Metodologia: Spec-Driven Development (SDD), nivel Spec-Anchored.
> Fecha base: 2026-05-01.

## 1. Autoridad De Artefactos

Cuando exista tension entre documentos, decisiones o codigo, aplicar este orden:

1. `docs/CONSTITUTION.md`
2. `docs/ARCHITECTURE.md`
3. `docs/specs/2026-04-30-sigcon-prd.md`
4. Spec tecnica del incremento activo
5. Plan de implementacion aprobado en `docs/plans/`
6. Codigo fuente

El codigo nunca es la fuente primaria de verdad del proyecto. Si el codigo contradice la spec, se corrige el codigo o se actualiza primero la spec y luego el plan.

Cuando `docs/ARCHITECTURE.md` conserve ejemplos o plantillas reutilizables del ecosistema SED, las coordenadas canonicas SIGCON prevalecen para este proyecto: `SGCN_`, `SED_SIGCON`, `/sigcon`, `sigcon-backend.war`, `sigcon-angular` y `co.gov.bogota.sed.sigcon`.

## 2. Reglas SDD

- Todo incremento debe tener spec tecnica escrita, revisada y aprobada antes de implementarse.
- Todo incremento debe tener plan de implementacion en `docs/plans/` antes de ejecutar tareas.
- Los cambios de alcance entran primero por PRD o spec tecnica, no por codigo.
- Los cambios de arquitectura entran primero por `docs/ARCHITECTURE.md` o por esta constitucion.
- Los cambios visuales entran primero por `Prototipo/DESIGN.md` o por la spec activa; `docs/ARCHITECTURE.md` solo define la integracion tecnica UI.
- Cada tarea de implementacion debe tener salida verificable y trazabilidad a criterios de aceptacion.
- No se implementa funcionalidad fuera del incremento activo aunque parezca conveniente.

## 3. Stack No Negociable

Backend:

- Java runtime: Oracle JDK 8.
- Spring Boot: 2.7.x, version canonica 2.7.18.
- Empaquetado: WAR.
- Servidor objetivo: Oracle WebLogic 12.2.1.4.0.
- Swagger/OpenAPI: SpringDoc 1.7.0, siempre activo.
- Base de datos: Oracle 19c+.
- Driver: ojdbc8.

Frontend:

- Angular 20.
- TypeScript strict mode.
- PrimeNG 21.
- Tailwind CSS 3.4.
- MSAL Angular/Browser 3.x para Office 365/Azure AD.

Identidad y seguridad:

- Perfil `local-dev`: HTTP Basic con usuarios de prueba.
- Perfil `weblogic`: Azure AD JWT / Office 365.
- Toda autorizacion por rol se valida en backend.
- Un contratista nunca puede acceder a contratos o informes de otro contratista.

## 4. Reglas De Arquitectura SED

- El backend mantiene la estructura `domain/`, `application/`, `web/`, `config/`.
- Las entidades JPA no se exponen desde controladores; se usan DTOs.
- Todos los listados deben ser paginados cuando sean datos operativos.
- Las tablas Oracle usan prefijo del sistema; para SIGCON el prefijo es `SGCN_`.
- El esquema Oracle del MVP es `SED_SIGCON`.
- El contexto WebLogic del sistema es `/sigcon`.
- El artefacto backend es `sigcon-backend.war`.
- `weblogic.xml` debe declarar `prefer-web-inf-classes`.
- `spring.jpa.open-in-view` debe estar deshabilitado.
- La auditoria debe conservar usuario y timestamps cuando aplique.

## 5. Reglas De UX/UI

- `Prototipo/DESIGN.md` gobierna colores, tipografia, densidad y componentes.
- Las pantallas de `Prototipo/*/screen.png` son referencia visual obligatoria.
- Usar componentes PrimeNG 21 antes de construir controles propios.
- Mantener interfaz institucional, densa, clara y orientada a gestion operativa.
- No introducir pantallas tipo landing page para flujos administrativos.
- No introducir estilos visuales que contradigan `Prototipo/DESIGN.md`, aunque aparezcan como ejemplos genericos en documentos de arquitectura reutilizable.

## 6. Fronteras Por Incremento

Incremento 1 incluye solamente:

- Auth y perfiles.
- Imagen de firma en perfil.
- Administracion de contratos, obligaciones, usuarios y catalogo de documentos.
- Vista de contratos y detalle para contratista.

Incremento 1 excluye:

- Creacion y gestion de informes.
- Flujo de revision/aprobacion.
- Generacion de PDF.
- Notificaciones email o in-app.
- Integracion SECOP2.
- Contratos de personas juridicas/proveedores.

## 7. Gates De Calidad

Antes de cerrar una tarea:

- Ejecutar la verificacion definida en el plan.
- Confirmar que no se invadio alcance de otro incremento.
- Confirmar que el cambio respeta `docs/ARCHITECTURE.md`.
- Confirmar que los criterios de aceptacion afectados estan cubiertos.

Antes de cerrar Incremento 1:

- Backend: pruebas de servicios, controladores y seguridad.
- Frontend: pruebas unitarias y build.
- Integracion local-dev: flujo Admin y Contratista.
- WAR: `sigcon-backend.war` generado.
- Swagger y health accesibles.
- `docs/ARRANQUE.md` actualizado.

## 8. Politica De Evolucion

Esta constitucion es un documento vivo, pero estable. Cualquier cambio debe:

- Explicar que regla cambia.
- Indicar que specs o planes quedan afectados.
- Actualizar los artefactos derivados antes de implementar codigo.
