# SIGCON I1 Execution Log

> Documento de handoff compartido para trabajo alternado entre Codex, Claude y otros modelos.
> Fuente rectora: `docs/plans/2026-05-01-sigcon-i1-implementation-plan.md`.

## Estado De Rama

- Rama activa: `feat/sigcon-i1`
- Remoto: `origin/feat/sigcon-i1`
- Ultimo commit funcional de Task 5: `70e28a7 feat: add SIGCON I1 application services`
- Ultimo commit funcional de Task 6: `dd76343 feat: add SIGCON I1 backend APIs and security`
- Ultimo commit funcional de Task 7: `799388e feat: bootstrap SIGCON Angular app`
- Cambio local no versionado conocido: `.claude/` queda fuera de Git por ser configuracion local de Claude.

## Tareas I1 Completadas

| Task | Estado | Commit principal | Evidencia |
|---|---|---|---|
| Task 2 - Oracle schema scripts | Completa | `89c3a50` + fixes `1fb4f06`, `503c2ad` | Scripts `db/00_setup.sql`, `db/01_datos_prueba.sql`; sin tablas I2/I3 |
| Task 3 - Backend WAR bootstrap | Completa | `e17c986` + fixes `71dcd53`, `e5819ed` | `mvn test -Dtest=SigconBackendApplicationTests` pasa |
| Task 4 - Domain model/repositories | Completa | `f137d14` | `DomainModelMappingTest`; entidades y repositorios I1 |
| Task 5 - DTOs/services/error contract | Completa | `70e28a7` | `mvn test -Dtest=*ServiceTest`; `mvn test`; `mvn test -DskipTests` pasan |
| Task 6 - Backend security/controllers | Completa | `dd76343` | `mvn test -Dtest=*SecurityTest`; `mvn test`; `mvn test -DskipTests` pasan |
| Task 7 - Frontend bootstrap/design system | Completa | `799388e` | `npm install`; `npm run verify:bootstrap`; `npm run build` pasan |

## Task 5 Implementado

Archivos principales:

- DTOs en `sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/application/dto/`
- Mappers en `sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/application/mapper/`
- Servicios en `sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/application/service/`
- Contrato de error en `sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/web/exception/`
- Pruebas en `sigcon-backend/src/test/java/co/gov/bogota/sed/sigcon/application/`

Reglas cubiertas por pruebas:

- ADMIN lista todos los contratos activos.
- CONTRATISTA lista solo sus contratos activos.
- SUPERVISOR lista contratos supervisados.
- REVISOR lista contratos asignados.
- Numero de contrato duplicado lanza `NUMERO_CONTRATO_DUPLICADO`.
- CONTRATISTA no accede a contrato ajeno y lanza `ACCESO_DENEGADO`.
- Borrado logico de contrato marca `activo=false`.
- `LocalDocumentStorageService` guarda firma PNG/JPG bajo ruta local configurable.
- Firma con formato no valido lanza `FORMATO_IMAGEN_INVALIDO`.

Validaciones ejecutadas:

```powershell
cd sigcon-backend
mvn test -Dtest=*ServiceTest
mvn test
mvn test -DskipTests
```

Resultados observados:

- `mvn test -Dtest=*ServiceTest`: 9 tests, 0 fallas.
- `mvn test`: 13 tests, 0 fallas.
- `mvn test -DskipTests`: build success.

## Restricciones De Alcance I1

No se implemento:

- `Informe`
- `Soporte`
- `Notificacion`
- PDF
- endpoints `/api/informes`
- endpoints `/api/notificaciones`
- tablas `SGCN_INFORMES`, `SGCN_SOPORTES`, `SGCN_NOTIFICACIONES`

Busqueda de alcance realizada:

```powershell
Select-String -Path sigcon-backend\src\main\java\co\gov\bogota\sed\sigcon\**\*.java -Pattern 'Informe|Soporte|Notificacion|Notificación|Pdf|PDF|SGCN_INFORMES|SGCN_SOPORTES|SGCN_NOTIFICACIONES'
```

Resultado: sin coincidencias.

## Task 6 Implementado

Archivos principales:

- Seguridad local-dev en `sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/config/DevSecurityConfig.java`.
- Seguridad WebLogic/Azure JWT en `sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/config/SecurityConfig.java`.
- Controladores REST I1 en `sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/web/controller/`.
- Pruebas MockMvc en `sigcon-backend/src/test/java/co/gov/bogota/sed/sigcon/web/SigconBackendSecurityTest.java`.
- Spec I1 ajustada para que `UsuarioRequest` incluya `email`, coherente con `POST /api/usuarios` y `EMAIL_DUPLICADO`.

Reglas cubiertas por pruebas:

- CONTRATISTA lista solo sus contratos.
- ADMIN lista todos los contratos.
- CONTRATISTA no puede hacer `POST /api/contratos`.
- Numero de contrato duplicado retorna 409 con `NUMERO_CONTRATO_DUPLICADO`.
- CONTRATISTA no puede abrir contrato ajeno.
- `POST /api/usuarios/me/firma` acepta PNG y rechaza PDF con `FORMATO_IMAGEN_INVALIDO`.
- `/actuator/health`, `/api-docs` y `/swagger-ui.html` son publicos.
- `/api/informes` y `/api/notificaciones` no estan expuestos en I1.

Validaciones ejecutadas:

```powershell
cd sigcon-backend
mvn test -Dtest=*SecurityTest
mvn test
mvn test -DskipTests
Get-ChildItem -Path sigcon-backend\src\main\java -Recurse -File | Select-String -Pattern "Informe|Soporte|Notificacion|Notificación|Pdf|PDF|SGCN_INFORMES|SGCN_SOPORTES|SGCN_NOTIFICACIONES|/api/informes|/api/notificaciones"
```

Resultados observados:

- `mvn test -Dtest=*SecurityTest`: 8 tests, 0 fallas.
- `mvn test`: 21 tests, 0 fallas.
- `mvn test -DskipTests`: build success.
- Busqueda de alcance en `src/main/java`: sin coincidencias.

## Task 7 Implementado

Archivos principales:

- Workspace Angular en `sigcon-angular/`.
- Configuracion Angular standalone/routing/SCSS/test en `angular.json`, `tsconfig*.json`, `src/main.ts`, `src/app/app.config.ts`, `src/app/app.routes.ts`, `src/app/app.component.ts`.
- Dependencias frontend en `sigcon-angular/package.json` y `sigcon-angular/package-lock.json`.
- Proxy local-dev en `sigcon-angular/proxy.conf.json` para `/api`, `/api-docs`, `/swagger-ui.html` y `/actuator`.
- Tailwind/PostCSS en `sigcon-angular/tailwind.config.js` y `sigcon-angular/postcss.config.js`.
- Tokens SED en `sigcon-angular/src/app/shared/design-tokens.scss` importados desde `sigcon-angular/src/styles.scss`.
- Verificador estructural en `sigcon-angular/scripts/verify-bootstrap.mjs`.

Correccion de coherencia SDD:

- El plan original indicaba Angular 20 + PrimeNG 21.
- `npm view primeng@21.0.0 peerDependencies --json` confirma que PrimeNG 21 exige Angular 21.
- `npm view primeng@20.0.0 peerDependencies --json` confirma compatibilidad con Angular 20.
- Para preservar la decision arquitectonica Angular 20, se ajustaron plan/spec/docs a PrimeNG 20 y `@primeng/themes` 20, agregando `@angular/cdk` 20 como peer requerido.

Validaciones ejecutadas:

```powershell
cd sigcon-angular
node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" install
node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" run verify:bootstrap
node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" run build
```

Resultados observados:

- `npm install`: 602 paquetes instalados, 0 vulnerabilidades.
- `npm run verify:bootstrap`: passed.
- `npm run build`: production build success.
- Warning conocido: Tailwind reporta que no detecta utility classes porque Task 7 aun no crea pantallas/componentes con clases; se espera resolver naturalmente en Task 8/9.
- Warning conocido: `@primeng/themes@20.4.0` aparece como deprecated en npm, pero es la linea compatible con Angular 20 indicada por peers de PrimeNG 20.

## Decisiones Y Notas Para El Siguiente Modelo

- La inconsistencia de `UsuarioRequest` quedo resuelta en Task 6: ahora incluye `email`, y `UsuarioService` valida duplicados con `EMAIL_DUPLICADO` antes de crear o actualizar usuarios.
- `CurrentUserService` usa `Authentication.getName()` como email/UPN, coherente con la regla de Task 6 sobre usuarios HTTP Basic con email completo.
- `LocalDocumentStorageService` implementa solo almacenamiento local de firma. No debe extenderse a soportes, informes ni PDF en I1.
- `DevSecurityConfig` usa usuarios HTTP Basic con emails completos: `admin@educacionbogota.edu.co`, `juan.escandon@educacionbogota.edu.co`, `revisor1@educacionbogota.edu.co`, `supervisor1@educacionbogota.edu.co`.
- `SigconBackendApplicationTests` excluye `application.service.*` y `web.controller.*` porque el smoke test no levanta repositorios ni datasource Oracle. Las reglas de servicio/controlador quedan cubiertas por unit tests y MockMvc.
- Frontend queda en Angular 20 + PrimeNG 20, no PrimeNG 21, por incompatibilidad formal de peer dependencies entre PrimeNG 21 y Angular 20.
- El wrapper global `npm` de la maquina falla porque resuelve `C:\Users\jmep2\AppData\Roaming\npm\node_modules\npm\bin\npm-cli.js`; usar temporalmente `node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" ...` hasta reparar npm global.
- Maven corre actualmente con Java 21 en esta maquina, aunque `pom.xml` compila con source/target `1.8`. Falta validar con Oracle JDK 8 real antes del cierre backend.

## Proximo Punto De Retoma

Continuar con **Task 8: Frontend Core Auth, API Models, And Shell**.

Antes de avanzar:

1. Leer `docs/plans/2026-05-01-sigcon-i1-implementation-plan.md`, Task 8.
2. Crear primero pruebas unitarias para modelos/servicios/guards cuando aplique.
3. Usar solo modelos y servicios I1: usuario, contrato, obligacion y documento-catalogo.
4. No crear `informe.service.ts`, `pdf.service.ts`, `notificacion.service.ts` ni modelos I2/I3.
5. Mantener `Prototipo/DESIGN.md` como autoridad visual para shell/topbar/sidebar.
6. Registrar en este log los archivos tocados, validaciones, errores y commit resultante.
