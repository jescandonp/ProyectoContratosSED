# SIGCON I1 Execution Log

> Documento de handoff compartido para trabajo alternado entre Codex, Claude y otros modelos.
> Fuente rectora: `docs/plans/2026-05-01-sigcon-i1-implementation-plan.md`.

## Estado De Rama

- Rama activa: `feat/sigcon-i1`
- Remoto: `origin/feat/sigcon-i1`
- Ultimo commit funcional de Task 5: `70e28a7 feat: add SIGCON I1 application services`
- Cambio local no versionado conocido: `.claude/` queda fuera de Git por ser configuracion local de Claude.

## Tareas I1 Completadas

| Task | Estado | Commit principal | Evidencia |
|---|---|---|---|
| Task 2 - Oracle schema scripts | Completa | `89c3a50` + fixes `1fb4f06`, `503c2ad` | Scripts `db/00_setup.sql`, `db/01_datos_prueba.sql`; sin tablas I2/I3 |
| Task 3 - Backend WAR bootstrap | Completa | `e17c986` + fixes `71dcd53`, `e5819ed` | `mvn test -Dtest=SigconBackendApplicationTests` pasa |
| Task 4 - Domain model/repositories | Completa | `f137d14` | `DomainModelMappingTest`; entidades y repositorios I1 |
| Task 5 - DTOs/services/error contract | Completa | `70e28a7` | `mvn test -Dtest=*ServiceTest`; `mvn test`; `mvn test -DskipTests` pasan |

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

## Decisiones Y Notas Para El Siguiente Modelo

- `UsuarioRequest` conserva los campos definidos por la spec I1: `nombre`, `cargo`, `rol`. No se agrego `email`, aunque el texto de endpoints menciona `POST /api/usuarios` y `EMAIL_DUPLICADO`; antes de implementar alta manual de usuarios en controladores conviene resolver esta inconsistencia en spec/plan.
- `CurrentUserService` usa `Authentication.getName()` como email/UPN, coherente con la regla de Task 6 sobre usuarios HTTP Basic con email completo.
- `LocalDocumentStorageService` implementa solo almacenamiento local de firma. No debe extenderse a soportes, informes ni PDF en I1.
- `SigconBackendApplicationTests` excluye `application.service.*` porque el smoke test no levanta repositorios ni datasource Oracle. Las reglas de servicio quedan cubiertas por unit tests.
- Maven corre actualmente con Java 21 en esta maquina, aunque `pom.xml` compila con source/target `1.8`. Falta validar con Oracle JDK 8 real antes del cierre backend.

## Proximo Punto De Retoma

Continuar con **Task 6: Backend Security And Controllers**.

Antes de escribir controladores:

1. Leer `docs/plans/2026-05-01-sigcon-i1-implementation-plan.md`, Task 6.
2. Confirmar que `DevSecurityConfig` use emails completos:
   - `admin@educacionbogota.edu.co`
   - `juan.escandon@educacionbogota.edu.co`
   - `revisor1@educacionbogota.edu.co`
   - `supervisor1@educacionbogota.edu.co`
3. Crear tests MockMvc primero.
4. No crear rutas ni servicios para informes/notificaciones/PDF.
5. Resolver o documentar la inconsistencia de `UsuarioRequest` antes de exponer `POST /api/usuarios`.
