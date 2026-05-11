# Execution Log — SIGCON Incremento 6
## Rediseño PDF Institucional + Datos Complementarios del Informe

> **Metodología:** Spec-Driven Development (SDD) — Spec-Anchored  
> **Spec:** `docs/specs/2026-05-09-sigcon-i6-spec.md`  
> **Plan:** `docs/plans/2026-05-09-sigcon-i6-plan.md`  
> **Rama:** `feat/sigcon-i6`  
> **Base:** `feat/sigcon-i5` HEAD `0cc76a9`  
> **Inicio:** 2026-05-09  
> **Estado:** ✅ CERRADO — 2026-05-09

---

## Contexto del Incremento

Tres líneas de trabajo simultáneas:

1. **PDF Institucional SED** — Rediseño completo de `InformePdfTemplateService` con layout de 8 secciones, paleta `#002869`, firma del revisor opcional, valor del desembolso en letras (`NumeroPesosConverter`).
2. **Datos Complementarios del Contrato** — Campos `dependencia` (catálogo de 44 unidades SED), `formaPago`, `modificaciones` en el formulario de administración.
3. **Aportes SGSSI por Informe** — Nueva tabla `SGCN_APORTES_SGSSI` con borrado lógico, endpoint `PUT /api/informes/{id}/aportes-sgssi`, sección editable en `InformeFormComponent` e `InformeDetalleComponent`, entidades SGSSI pre-configurables en el perfil del contratista.

**Enmiendas aplicadas durante la ejecución:**
- **E1:** Campo `dependencia` implementado como `<input list="...">` + `<datalist>` nativo (no PrimeNG `p-dropdown`), ya que ningún template existente usa PrimeNG.
- **E2:** Campo `porcentaje` eliminado de `ActividadInformeRequest` y de todos los templates; permanece mapeado en la entidad Hibernate para evitar error de columna huérfana.

| Tarea | Descripción | Estado | Commit |
|-------|-------------|--------|--------|
| T1 | Rama `feat/sigcon-i6` + DDL | ✅ completo | `8bf1d8b` |
| T2 | Dominio — `AporteSgssi`, `ItemSgssi`, extensiones | ✅ completo | `eaa7f07` |
| T3 | `NumeroPesosConverter` | ✅ completo | `2dc8f45` |
| T4 | DTOs — `AporteSgssiRequest/Dto`, extensiones | ✅ completo | `a453c3a` |
| T5 | Servicios — `AporteSgssiService` + extensiones | ✅ completo | `0c9766f` |
| T6 | `AporteSgssiController` (GET + PUT) | ✅ completo | `27df9a1` |
| T7 | Tests backend — 135 tests, 0 fallos | ✅ completo | `1b39f92` |
| T8 | PDF — `PdfInformeService` + rediseño `InformePdfTemplateService` | ✅ completo | ver nota T8 |
| T9 | Frontend — perfil SGSSI + admin contrato + constantes SED | ✅ completo | ver nota T9 |
| T10 | Angular `AporteSgssiService` + modelos | ✅ completo | ver nota T10 |
| T11 | `InformeFormComponent` — desembolso + SGSSI | ✅ completo | ver nota T11 |
| T12 | `InformeDetalleComponent` — SGSSI editable + 16 specs + guía | ✅ completo | ver nota T12 |

**Leyenda:** ✅ completo | 🔄 en progreso | ⬜ pendiente | ❌ bloqueado

> **Nota T8–T12:** Los commits de frontend y PDF se consolidan en commits agrupados en la rama `feat/sigcon-i6` al cierre del incremento (ver sección "Registro de Ejecución").

---

## Estado del Sistema al Inicio del Incremento

- Backend: 123 tests, 0 fallos (rama `feat/sigcon-i5`, commit `0cc76a9`)
- Frontend: 90 specs, 0 fallos
- DDL: sin cambios pendientes
- Endpoints necesarios: ninguno existía — todos son nuevos en I6

---

## Registro de Ejecución

### 2026-05-09 — T1 a T7: Backend base

- **T1** `8bf1d8b`: Rama `feat/sigcon-i6` creada desde `feat/sigcon-i5` HEAD `0cc76a9`. DDL I6 agregado a `db/00_setup.sql`:
  - `SGCN_CONTRATOS`: +`DEPENDENCIA`, +`FORMA_PAGO`, +`MODIFICACIONES`
  - `SGCN_INFORMES`: +`NUMERO_DESEMBOLSO`, +`VALOR_DESEMBOLSO`, +`PORCENTAJE_EJECUCION`, +`CORRESPONDENCIA_PENDIENTE`
  - `SGCN_USUARIOS`: +`SGSSI_SALUD_ENTIDAD`, +`SGSSI_PENSION_ENTIDAD`, +`SGSSI_ARL_ENTIDAD`
  - Nueva tabla `SGCN_APORTES_SGSSI` con secuencia, índice y trigger de auditoría

- **T2** `eaa7f07`: Dominio nuevo y extendido:
  - Enum `ItemSgssi` (SALUD, PENSION, ARL)
  - Entidad `AporteSgssi` con borrado lógico (`activo = 1/0`)
  - `AporteSgssiRepository.findByInformeIdAndActivoTrue(Long)`
  - `Contrato`: +`dependencia`, +`formaPago`, +`modificaciones`
  - `Informe`: +`numeroDesembolso`, +`valorDesembolso`, +`porcentajeEjecucion`, +`correspondenciaPendiente`
  - `Usuario`: +`sgssiSaludEntidad`, +`sgssiPensionEntidad`, +`sgssiArlEntidad`
  - `ActividadInforme`: campo `porcentaje` mantenido mapeado en entidad (sin eliminar de DB)

- **T3** `2dc8f45`: `NumeroPesosConverter` — utilidad estática Java 8 para convertir `BigDecimal` a texto en pesos colombianos. Algoritmo descompone en millones/miles/unidades usando arreglos UNIDADES/DECENAS/CENTENAS. Casos especiales: CERO, UN MILLÓN, MIL MILLONES, VEINTIPICO.

- **T4** `a453c3a`: DTOs nuevos y extendidos:
  - `AporteSgssiRequest` (`@NotNull item`, `@NotNull fechaPago`, `@DecimalMin("0.01") valorAportado`, `@NotBlank entidad`)
  - `AporteSgssiDto` (con `periodoSgssi` derivado)
  - Extensiones de `ContratoRequest/DetalleDto`, `InformeRequest/UpdateDto/DetalleDto`, `PerfilUpdateRequest/UsuarioDto`
  - `ActividadInformeRequest` y `ActividadInformeDto`: campo `porcentaje` eliminado (Enmienda E2)

- **T5** `0c9766f`: Servicios:
  - `AporteSgssiService` nuevo: `guardarAportes()` (borrado lógico + inserción, validación de ítems duplicados, validación de estado y propietario), `listarAportes()`, `calcularPeriodo()` (mes anterior a `fechaInicio`)
  - `InformeService`: `crear()` y `actualizar()` extendidos con 4 campos nuevos + delegación a `AporteSgssiService`
  - `ContratoService`: `crear()` y `actualizar()` extendidos con `dependencia`, `formaPago`, `modificaciones` (default "No se han presentado")
  - `UsuarioService`: `actualizarPerfil()` extendido con 3 campos SGSSI

- **T6** `27df9a1`: `AporteSgssiController`:
  - `GET /api/informes/{id}/aportes-sgssi` — roles: CONTRATISTA, REVISOR, SUPERVISOR, ADMIN
  - `PUT /api/informes/{id}/aportes-sgssi` — rol: CONTRATISTA (reemplaza todos los aportes activos)

- **T7** `1b39f92`: Tests backend — **135 tests, 0 fallos**:
  - `NumeroPesosConverterTest`: 5 tests (CERO, mil, millones, UN MILLÓN, MIL MILLONES)
  - `AporteSgssiServiceTest`: 6 tests (exitoso, sin PENSIÓN, informe ENVIADO falla, contratista incorrecto falla, ítem duplicado falla, período correcto)
  - `InformeServiceTest`: 2 tests nuevos (campos nuevos en crear y actualizar)
  - `ContratoServiceTest`: 2 tests nuevos (dependencia y forma de pago)
  - `SecurityIntegrationTest`: 3 tests nuevos (CONTRATISTA puede PUT, SUPERVISOR no puede PUT, sin auth → 401)

### 2026-05-09 — T8: PDF Institucional

- **T8**: `PdfInformeService` y `InformePdfTemplateService` rediseñados:
  - `PdfInformeService.generarYPersistir()`: carga firma del revisor de forma opcional (null-safe con try/catch; PDF continúa sin error si el revisor no tiene firma).
  - `InformePdfTemplateService.generarPdf()`: nuevo parámetro `byte[] firmaRevisor`. Layout de 8 secciones:
    1. Encabezado institucional — `background: #002869`, texto blanco
    2. Datos del contrato — valor en letras via `NumeroPesosConverter`, campos opcionales omitidos si null
    3. Ejecución de actividades — tabla SIN columna "% avance" (Enmienda E2)
    4. Aportes SGSSI — período = mes anterior a `fechaInicio` del informe, en español
    5. Estado de radicación de correspondencia — marcadores SI/NO
    6. Declaración especial — texto estático
    7. Cierre de aprobación — número/valor/porcentaje de desembolso
    8. Firmas — 2 o 3 columnas según disponibilidad de firma del revisor
  - CSS: `#002869` encabezados de tabla, `#C0C0C0` títulos de sección, `#eff4ff` filas alternas
  - Labels SGSSI en PDF: SALUD→"Salud", PENSION→"Pensión", ARL→"A.R.L."

### 2026-05-09 — T9–T12: Frontend Angular

- **T9**: Frontend — perfil + admin contrato + constantes:
  - `sed-dependencias.constants.ts`: 44 unidades organizacionales SED (Despacho, Oficinas, Subsecretarías, Direcciones, 20 Direcciones Locales, Subdirecciones)
  - `usuario.model.ts`: +`sgssiSaludEntidad`, +`sgssiPensionEntidad`, +`sgssiArlEntidad` en `Usuario` y `PerfilUpdateRequest`
  - `contrato.model.ts`: +`dependencia`, +`formaPago`, +`modificaciones` en `ContratoDetalle` y `ContratoRequest`
  - `perfil.component.ts`: 3 nuevas señales SGSSI; sección "Aportes SGSSI — Entidades" visible solo para CONTRATISTA
  - `admin-contrato-form.component.ts`: `<input list>` + `<datalist>` con 44 dependencias SED (Enmienda E1); campos `formaPago` y `modificaciones`

- **T10**: Angular core:
  - `aporte-sgssi.model.ts`: `ItemSgssi`, `AporteSgssiDto` (con `periodoSgssi`), `AporteSgssiRequest`, `ITEM_SGSSI_LABELS`
  - `aporte-sgssi.service.ts`: `listar(informeId)` → GET, `guardarTodos(informeId, aportes)` → PUT

- **T11**: `informe-form.component.ts` — reescritura completa:
  - `ActividadFormRow`: eliminado campo `porcentaje` (Enmienda E2)
  - `AporteSgssiRow`: nuevo interface `{ item, fechaPago, valorAportado, entidad }`
  - Señales nuevas: `numeroDesembolso`, `valorDesembolso`, `porcentajeEjecucion`, `correspondenciaPendiente`, `aportesForm`
  - `crearInforme()` incluye los 4 campos nuevos; `guardarDetalle()` llama `aporteSgssiService.guardarTodos()`
  - Template: encabezado con 4 campos + checkbox; sección SGSSI con tabla editable; tarjetas sin "Avance %"

- **T12**: `InformeDetalleComponent` — reescritura:
  - `ActividadEditState`: eliminado `porcentaje` (Enmienda E2)
  - `AporteSgssiEditRow` interface nuevo
  - Señales: `aportesEdicion`, `guardandoAportes`, `errorAportes`
  - Inyectado `AporteSgssiService` (6.º parámetro constructor)
  - `inicializarAportesEdicion()`, `agregarAporteEdicion()`, `eliminarAporteEdicion()`, `actualizarAporteEdicion()`, `guardarAportesSgssi()`, `labelSgssi()`
  - Template: sección SGSSI editable (BORRADOR) y solo lectura (otros estados); tarjetas de actividad sin "Avance %"
  - `informe-detalle.component.spec.ts`: 16 nuevos tests I6, spy `AporteSgssiService`, `sampleInformeDetalle` actualizado
  - `GUIA_PRUEBAS_FUNCIONALES.md`: sección 14 agregada (9 subsecciones + diagnóstico)

---

## Validación Final

| Métrica | Meta | Resultado |
|---------|------|-----------|
| Backend tests | >= 135, 0 fallos | **135** ✅ |
| Frontend specs | >= 106, 0 fallos | **106** ✅ (90 base + 16 nuevos) |
| Endpoints nuevos | 2 (GET + PUT aportes-sgssi) | **2** ✅ |
| DDL I6 | 4 ALTER TABLE + 1 CREATE TABLE | **✅** |
| Secciones PDF | 8 | **8** ✅ |
| Catálogo dependencias | 44 unidades SED | **44** ✅ |
| Commits I6 | T1–T12 trazables | **✅** |
| Regresión I5 actividades | Tests porcentaje eliminados/actualizados | **✅** |

---

## Correcciones Post-Cierre — 2026-05-11

### ORA-01400 en creación de actividades

- **Síntoma:** al guardar un informe con actividades nuevas, Oracle rechazaba el insert con `ORA-01400: no se puede realizar una inserción NULL en ("SED_SIGCON"."SGCN_ACTIVIDADES"."PORCENTAJE")`.
- **Causa raíz:** I6 eliminó `porcentaje` del request/API y de la interfaz por actividad, pero la columna heredada `SGCN_ACTIVIDADES.PORCENTAJE` permanece `NOT NULL` en el modelo físico.
- **Corrección backend:** `ActividadInformeService.crear()` asigna `BigDecimal.ZERO` como valor técnico de compatibilidad antes de persistir la actividad. El valor no se expone ni se usa funcionalmente en I6.
- **Corrección DDL:** `db/00_setup.sql` deja `PORCENTAJE NUMBER(5,2) DEFAULT 0 NOT NULL` para instalaciones limpias.
- **Regresión:** `ActividadInformeServiceTest.createsActividadForObligacionOfSameContract()` valida que la actividad se guarde con `porcentaje = 0`.

### Reset controlado de informes locales

- Se agrega `db/03_reset_informes_local_dev.sql` para limpiar informes de pruebas sin tocar usuarios, contratos, obligaciones ni catálogos.
- Orden de borrado validado contra FKs actuales:
  1. `SGCN_SOPORTES` vía `SGCN_ACTIVIDADES`
  2. `SGCN_APORTES_SGSSI`
  3. `SGCN_DOCS_ADICIONALES`
  4. `SGCN_OBSERVACIONES`
  5. `SGCN_NOTIFICACIONES` asociadas a informe
  6. `SGCN_ACTIVIDADES`
  7. `SGCN_INFORMES`
- El script exige confirmación explícita (`SIGCON_RESET_INFORMES_CONFIRM = 'RESET_INFORMES'`), muestra conteos antes/después y falla con rollback ante error.
- No reinicia secuencias. Es intencional: el número funcional del informe se calcula por contrato con `countByContratoId + 1`, por lo que después del reset el siguiente informe vuelve a `No. 1` sin manipular secuencias Oracle.

### Validación ejecutada

```powershell
mvn test -Dtest=ActividadInformeServiceTest
mvn test "-Dtest=ActividadInformeServiceTest,InformeServiceTest,InformeSecurityTest"
```

Resultado: **31 tests, 0 fallos** en la corrida enfocada extendida.

---

## Próximo Punto de Retoma

**Incremento 6 cerrado.** No hay punto de retoma pendiente.

El siguiente incremento (I7) puede iniciar desde `feat/sigcon-i6` HEAD o desde `main` según el proceso de integración.

---

## Decisiones de Diseño Clave

1. **Borrado lógico aportes SGSSI**: `activo = 0/1` en lugar de DELETE físico. `PUT` reemplaza semánticamente todos los aportes activos: hace soft-delete de los previos e inserta los nuevos. Preserva trazabilidad histórica en la tabla.
2. **`<datalist>` nativo vs. PrimeNG**: PrimeNG está en `package.json` pero ningún template existente lo usa. Usar `<datalist>` nativo evita importar `PrimeNGModule` y mantiene coherencia con el estilo del proyecto.
3. **Campo `porcentaje` en entidad Hibernate**: Se mantiene mapeado para evitar error `HibernateException: Column not found`. La columna `PORCENTAJE` no se elimina de la base de datos. Ningún endpoint nuevo la lee ni escribe.
4. **Firma del revisor opcional**: `generarYPersistir()` carga la firma del revisor con try/catch; si falla o no existe, `firmaRevisor = null`. `generarPdf()` omite la celda de firma del revisor cuando es `null`. PDF nunca falla por ausencia de firma.
5. **`periodoSgssi` calculado en backend**: `YearMonth.from(fechaInicio).minusMonths(1)` formateado como `"mm/aaaa"`. El frontend no necesita lógica de calendario; solo muestra el valor del DTO.
