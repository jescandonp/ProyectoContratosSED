# SIGCON I16 — Implementation Plan
## Bloqueo Individual de Carga de Informes + Campo Plazo en Contrato

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Spec:** `docs/specs/2026-06-10-sigcon-i16-spec.md`
**Rama:** `main`
**Fecha:** 2026-06-10

**Goal:** Agregar bloqueo por contrato de carga de informes (gestionado por ADMIN) y campo `plazo` libre en contrato que reemplaza el texto hardcodeado en el PDF institucional.

**Architecture:** Dos features independientes sobre la entidad `Contrato`. F1 añade un flag booleano + endpoint PATCH + validación en `crearInforme`. F2 añade un campo CLOB + lo expone en form/detalle Angular + reemplaza texto hardcodeado en `InformePdfTemplateService`.

**Tech Stack:** Java 8 · Spring Boot 2.7.18 · JPA/Hibernate · Oracle 19c · Angular 20 · JUnit 5 · Mockito · AssertJ

---

## Mapa de archivos

| Archivo | Acción |
|---------|--------|
| `sigcon-backend/src/main/java/.../domain/entity/Contrato.java` | Modificar — +2 campos |
| `sigcon-backend/src/main/java/.../dto/contrato/ContratoDetalleDto.java` | Modificar — +2 campos |
| `sigcon-backend/src/main/java/.../dto/contrato/ContratoRequest.java` | Modificar — +1 campo `plazo` |
| `sigcon-backend/src/main/java/.../dto/contrato/BloqueoInformeRequest.java` | **Crear nuevo** |
| `sigcon-backend/src/main/java/.../mapper/ContratoMapper.java` | Modificar — mapear 2 campos nuevos |
| `sigcon-backend/src/main/java/.../service/ContratoService.java` | Modificar — +`applyRequest` plazo, +`actualizarBloqueoInforme()` |
| `sigcon-backend/src/main/java/.../service/InformeService.java` | Modificar — +validación bloqueo en `crearInforme()` |
| `sigcon-backend/src/main/java/.../service/InformePdfTemplateService.java` | Modificar — reemplazar texto hardcodeado de plazo |
| `sigcon-backend/src/main/java/.../controller/AdminContratoController.java` | Modificar — +endpoint PATCH bloqueo |
| `sigcon-backend/src/main/java/.../exception/ErrorCode.java` | Modificar — +`INFORME_CARGA_BLOQUEADA` |
| `sigcon-backend/src/test/.../application/InformeServiceTest.java` | Modificar — +1 test bloqueo |
| `sigcon-backend/src/test/.../application/ContratoServiceTest.java` | Modificar — +1 test bloqueo |
| `sigcon-backend/src/test/.../application/InformePdfTemplateServiceTest.java` | Modificar — +2 tests plazo |
| `sigcon-angular/src/app/core/models/contrato.model.ts` | Modificar — +2 campos |
| `sigcon-angular/src/app/core/services/contrato.service.ts` | Modificar — +método bloqueo |
| `sigcon-angular/src/app/features/contratos/detalle/contrato-detalle.component.ts` | Modificar — ocultar botón + mostrar plazo |
| `sigcon-angular/src/app/features/admin/contratos/admin-contrato-form.component.ts` | Modificar — +textarea Plazo |
| `sigcon-angular/src/app/features/admin/contratos/admin-contratos.component.ts` | Modificar — +toggle bloqueo |

---

## Task 1 — DDL: migración de esquema Oracle

**Files:**
- Create: `sigcon-backend/src/main/resources/db/migration/V16__bloqueo_informe_y_plazo_contrato.sql`

- [ ] **Step 1: Crear script de migración**

Crear el archivo con el siguiente contenido exacto:

```sql
-- I16: bloqueo carga informes y plazo libre por contrato
ALTER TABLE SGCN_CONTRATOS
  ADD BLOQUEADO_CARGA_INFORME NUMBER(1) DEFAULT 0 NOT NULL;

ALTER TABLE SGCN_CONTRATOS
  ADD PLAZO CLOB;
```

- [ ] **Step 2: Verificar que el archivo existe**

```powershell
ls "C:\Users\jmep2\Downloads\AgenIALab\ProyectoContratosSED\sigcon-backend\src\main\resources\db\migration\"
```

Esperado: aparece `V16__bloqueo_informe_y_plazo_contrato.sql` en el listado.

- [ ] **Step 3: Commit DDL**

```powershell
cd "C:\Users\jmep2\Downloads\AgenIALab\ProyectoContratosSED"
git add sigcon-backend/src/main/resources/db/migration/V16__bloqueo_informe_y_plazo_contrato.sql
git commit -m "feat(i16): DDL — columnas BLOQUEADO_CARGA_INFORME y PLAZO en SGCN_CONTRATOS"
```

---

## Task 2 — Backend dominio: entidad, DTOs, ErrorCode, DTO nuevo

**Files:**
- Modify: `sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/domain/entity/Contrato.java`
- Modify: `sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/application/dto/contrato/ContratoDetalleDto.java`
- Modify: `sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/application/dto/contrato/ContratoRequest.java`
- Create: `sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/application/dto/contrato/BloqueoInformeRequest.java`
- Modify: `sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/web/exception/ErrorCode.java`

- [ ] **Step 1: Agregar campos a `Contrato.java`**

En `Contrato.java`, antes del campo `@CreatedDate`, agregar los dos campos nuevos:

```java
@Column(name = "BLOQUEADO_CARGA_INFORME", nullable = false)
private Boolean bloqueadoCargaInforme = false;

@Column(name = "PLAZO", columnDefinition = "CLOB")
private String plazo;
```

Y al final de la clase, antes del último `}`, agregar los getters/setters:

```java
public Boolean getBloqueadoCargaInforme() { return bloqueadoCargaInforme; }
public void setBloqueadoCargaInforme(Boolean bloqueadoCargaInforme) { this.bloqueadoCargaInforme = bloqueadoCargaInforme; }
public String getPlazo() { return plazo; }
public void setPlazo(String plazo) { this.plazo = plazo; }
```

- [ ] **Step 2: Agregar campos a `ContratoDetalleDto.java`**

En `ContratoDetalleDto.java`, después del campo `modificaciones`, agregar:

```java
private Boolean bloqueadoCargaInforme = false;
private String plazo;
```

Y sus getters/setters al final de la clase:

```java
public Boolean getBloqueadoCargaInforme() { return bloqueadoCargaInforme; }
public void setBloqueadoCargaInforme(Boolean bloqueadoCargaInforme) { this.bloqueadoCargaInforme = bloqueadoCargaInforme; }
public String getPlazo() { return plazo; }
public void setPlazo(String plazo) { this.plazo = plazo; }
```

- [ ] **Step 3: Agregar campo `plazo` a `ContratoRequest.java`**

En `ContratoRequest.java`, después del campo `modificaciones`, agregar:

```java
private String plazo;
```

Y sus getter/setter:

```java
public String getPlazo() { return plazo; }
public void setPlazo(String plazo) { this.plazo = plazo; }
```

- [ ] **Step 4: Crear `BloqueoInformeRequest.java`**

Crear el archivo en `sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/application/dto/contrato/`:

```java
package co.gov.bogota.sed.sigcon.application.dto.contrato;

import javax.validation.constraints.NotNull;

public class BloqueoInformeRequest {
    @NotNull
    private Boolean bloqueado;

    public Boolean getBloqueado() { return bloqueado; }
    public void setBloqueado(Boolean bloqueado) { this.bloqueado = bloqueado; }
}
```

- [ ] **Step 5: Agregar `INFORME_CARGA_BLOQUEADA` a `ErrorCode.java`**

En `ErrorCode.java`, al final de la lista de enums (después de `SOPORTE_TAMANIO_EXCEDIDO`), agregar:

```java
// I16
INFORME_CARGA_BLOQUEADA
```

- [ ] **Step 6: Verificar compilación**

```powershell
cd "C:\Users\jmep2\Downloads\AgenIALab\ProyectoContratosSED\sigcon-backend"
mvn compile -q
```

Esperado: BUILD SUCCESS sin errores.

- [ ] **Step 7: Commit dominio**

```powershell
cd "C:\Users\jmep2\Downloads\AgenIALab\ProyectoContratosSED"
git add sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/domain/entity/Contrato.java
git add sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/application/dto/contrato/ContratoDetalleDto.java
git add sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/application/dto/contrato/ContratoRequest.java
git add sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/application/dto/contrato/BloqueoInformeRequest.java
git add sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/web/exception/ErrorCode.java
git commit -m "feat(i16): dominio — campos bloqueadoCargaInforme/plazo en Contrato, BloqueoInformeRequest, ErrorCode"
```

---

## Task 3 — Backend mapper y servicio Contrato

**Files:**
- Modify: `sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/application/mapper/ContratoMapper.java`
- Modify: `sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/application/service/ContratoService.java`

- [ ] **Step 1: (TDD RED) Escribir test `actualizarBloqueoInforme` en `ContratoServiceTest.java`**

En `ContratoServiceTest.java`, añadir al final de la clase (antes del cierre `}`):

```java
@Test
void actualizarBloqueoInformeActivaFlag() {
    Contrato contrato = contratoExistente(10L);
    contrato.setBloqueadoCargaInforme(false);
    when(contratoRepository.findByIdAndActivoTrue(10L)).thenReturn(Optional.of(contrato));
    when(contratoRepository.save(any(Contrato.class))).thenAnswer(inv -> inv.getArgument(0));
    when(obligacionRepository.findByContratoIdAndActivoTrueOrderByOrdenAsc(10L))
        .thenReturn(Collections.emptyList());
    when(documentoCatalogoRepository.findByTipoContratoAndActivoTrue(any()))
        .thenReturn(Collections.emptyList());

    ContratoDetalleDto result = contratoService.actualizarBloqueoInforme(10L, true);

    assertThat(result.getBloqueadoCargaInforme()).isTrue();
    verify(contratoRepository).save(contrato);
}

@Test
void actualizarBloqueoInformeDesactivaFlag() {
    Contrato contrato = contratoExistente(10L);
    contrato.setBloqueadoCargaInforme(true);
    when(contratoRepository.findByIdAndActivoTrue(10L)).thenReturn(Optional.of(contrato));
    when(contratoRepository.save(any(Contrato.class))).thenAnswer(inv -> inv.getArgument(0));
    when(obligacionRepository.findByContratoIdAndActivoTrueOrderByOrdenAsc(10L))
        .thenReturn(Collections.emptyList());
    when(documentoCatalogoRepository.findByTipoContratoAndActivoTrue(any()))
        .thenReturn(Collections.emptyList());

    ContratoDetalleDto result = contratoService.actualizarBloqueoInforme(10L, false);

    assertThat(result.getBloqueadoCargaInforme()).isFalse();
    verify(contratoRepository).save(contrato);
}
```

Necesitarás el helper `contratoExistente` — busca en el test si ya existe un helper similar. Si no, agrégalo:

```java
private static Contrato contratoExistente(Long id) {
    Contrato c = new Contrato();
    c.setId(id);
    c.setNumero("OPS-2026-" + id);
    c.setObjeto("Objeto de prueba");
    c.setTipo(co.gov.bogota.sed.sigcon.domain.enums.TipoContrato.OPS);
    c.setValorTotal(java.math.BigDecimal.valueOf(1000));
    c.setFechaInicio(java.time.LocalDate.of(2026, 1, 1));
    c.setFechaFin(java.time.LocalDate.of(2026, 12, 31));
    c.setEstado(EstadoContrato.EN_EJECUCION);
    c.setActivo(true);
    return c;
}
```

- [ ] **Step 2: Ejecutar test RED**

```powershell
cd "C:\Users\jmep2\Downloads\AgenIALab\ProyectoContratosSED\sigcon-backend"
mvn test "-Dtest=ContratoServiceTest" -q
```

Esperado: FAIL — `actualizarBloqueoInforme` no existe aún.

- [ ] **Step 3: Actualizar `ContratoMapper.java` — mapear los 2 campos nuevos**

En el método `toDetalleDto`, después de `dto.setModificaciones(contrato.getModificaciones());`, añadir:

```java
dto.setBloqueadoCargaInforme(contrato.getBloqueadoCargaInforme() != null && contrato.getBloqueadoCargaInforme());
dto.setPlazo(contrato.getPlazo());
```

- [ ] **Step 4: Actualizar `ContratoService.java` — `applyRequest` + nuevo método**

En `applyRequest`, después de `contrato.setModificaciones(request.getModificaciones());`, añadir:

```java
contrato.setPlazo(request.getPlazo());
```

Después del método `eliminarContrato`, añadir el nuevo método:

```java
public ContratoDetalleDto actualizarBloqueoInforme(Long id, Boolean bloqueado) {
    Contrato contrato = findActiveContrato(id);
    contrato.setBloqueadoCargaInforme(bloqueado);
    Contrato saved = contratoRepository.save(contrato);
    return contratoMapper.toDetalleDto(
        saved,
        obligacionRepository.findByContratoIdAndActivoTrueOrderByOrdenAsc(saved.getId()),
        java.util.Collections.emptyList()
    );
}
```

- [ ] **Step 5: Ejecutar test GREEN**

```powershell
cd "C:\Users\jmep2\Downloads\AgenIALab\ProyectoContratosSED\sigcon-backend"
mvn test "-Dtest=ContratoServiceTest" -q
```

Esperado: BUILD SUCCESS, todos los tests de `ContratoServiceTest` pasan.

- [ ] **Step 6: Suite completa backend**

```powershell
cd "C:\Users\jmep2\Downloads\AgenIALab\ProyectoContratosSED\sigcon-backend"
mvn test -q
```

Esperado: BUILD SUCCESS, 0 failures.

- [ ] **Step 7: Commit mapper + servicio**

```powershell
cd "C:\Users\jmep2\Downloads\AgenIALab\ProyectoContratosSED"
git add sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/application/mapper/ContratoMapper.java
git add sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/application/service/ContratoService.java
git add sigcon-backend/src/test/java/co/gov/bogota/sed/sigcon/application/ContratoServiceTest.java
git commit -m "feat(i16): ContratoMapper/Service — mapear plazo, actualizarBloqueoInforme()"
```

---

## Task 4 — Backend InformeService: validación de bloqueo en crearInforme

**Files:**
- Modify: `sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/application/service/InformeService.java`
- Modify: `sigcon-backend/src/test/java/co/gov/bogota/sed/sigcon/application/InformeServiceTest.java`

- [ ] **Step 1: (TDD RED) Escribir test en `InformeServiceTest.java`**

En `InformeServiceTest.java`, añadir al final de la clase (antes del cierre `}`):

```java
@Test
void contractorCannotCreateInformeOnBlockedContract() {
    Usuario contratista = usuario(2L, RolUsuario.CONTRATISTA);
    Contrato contrato = contrato(10L, contratista, EstadoContrato.EN_EJECUCION);
    contrato.setBloqueadoCargaInforme(true);
    when(currentUserService.getCurrentUser()).thenReturn(contratista);
    when(parametroService.isCargaInformesActiva()).thenReturn(true);
    when(contratoRepository.findByIdAndActivoTrue(10L)).thenReturn(Optional.of(contrato));

    assertThatThrownBy(() -> informeService.crearInforme(informeRequest(10L)))
        .isInstanceOfSatisfying(SigconBusinessException.class, ex -> {
            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INFORME_CARGA_BLOQUEADA);
            assertThat(ex.getStatus().value()).isEqualTo(403);
        });
}
```

- [ ] **Step 2: Ejecutar test RED**

```powershell
cd "C:\Users\jmep2\Downloads\AgenIALab\ProyectoContratosSED\sigcon-backend"
mvn test "-Dtest=InformeServiceTest#contractorCannotCreateInformeOnBlockedContract" -q
```

Esperado: FAIL — la validación no existe aún.

- [ ] **Step 3: Agregar validación en `InformeService.crearInforme()`**

En `InformeService.java`, en el método `crearInforme`, después de la línea:

```java
if (!isAssigned(contrato.getContratista(), usuario.getId())) {
    throw accessDenied();
}
```

Añadir:

```java
if (Boolean.TRUE.equals(contrato.getBloqueadoCargaInforme())) {
    throw new SigconBusinessException(
        ErrorCode.INFORME_CARGA_BLOQUEADA,
        "La carga de informes para este contrato está bloqueada por el administrador.",
        HttpStatus.FORBIDDEN
    );
}
```

- [ ] **Step 4: Ejecutar test GREEN**

```powershell
cd "C:\Users\jmep2\Downloads\AgenIALab\ProyectoContratosSED\sigcon-backend"
mvn test "-Dtest=InformeServiceTest" -q
```

Esperado: BUILD SUCCESS, todos los tests de `InformeServiceTest` pasan.

- [ ] **Step 5: Suite completa backend**

```powershell
cd "C:\Users\jmep2\Downloads\AgenIALab\ProyectoContratosSED\sigcon-backend"
mvn test -q
```

Esperado: BUILD SUCCESS, 0 failures.

- [ ] **Step 6: Commit validación bloqueo**

```powershell
cd "C:\Users\jmep2\Downloads\AgenIALab\ProyectoContratosSED"
git add sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/application/service/InformeService.java
git add sigcon-backend/src/test/java/co/gov/bogota/sed/sigcon/application/InformeServiceTest.java
git commit -m "feat(i16): InformeService — bloquear crearInforme si contrato.bloqueadoCargaInforme=true"
```

---

## Task 5 — Backend controller: endpoint PATCH bloqueo

**Files:**
- Modify: `sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/web/controller/AdminContratoController.java`

- [ ] **Step 1: Añadir endpoint en `AdminContratoController.java`**

Agregar el import necesario al inicio del archivo:

```java
import co.gov.bogota.sed.sigcon.application.dto.contrato.BloqueoInformeRequest;
import org.springframework.web.bind.annotation.PatchMapping;
```

Añadir el nuevo método después del método `actualizarContrato`:

```java
@Operation(summary = "Actualizar bloqueo de carga de informes",
           description = "Permite al ADMIN bloquear o desbloquear la carga de nuevos informes en un contrato específico.")
@PatchMapping("/{id}/bloqueo-informe")
public ResponseEntity<ContratoDetalleDto> actualizarBloqueoInforme(
        @PathVariable Long id,
        @Valid @RequestBody BloqueoInformeRequest request) {
    return ResponseEntity.ok(contratoService.actualizarBloqueoInforme(id, request.getBloqueado()));
}
```

- [ ] **Step 2: Verificar compilación**

```powershell
cd "C:\Users\jmep2\Downloads\AgenIALab\ProyectoContratosSED\sigcon-backend"
mvn compile -q
```

Esperado: BUILD SUCCESS.

- [ ] **Step 3: Suite completa backend**

```powershell
cd "C:\Users\jmep2\Downloads\AgenIALab\ProyectoContratosSED\sigcon-backend"
mvn test -q
```

Esperado: BUILD SUCCESS, 0 failures.

- [ ] **Step 4: Commit controller**

```powershell
cd "C:\Users\jmep2\Downloads\AgenIALab\ProyectoContratosSED"
git add sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/web/controller/AdminContratoController.java
git commit -m "feat(i16): AdminContratoController — PATCH /{id}/bloqueo-informe"
```

---

## Task 6 — Backend PDF: campo plazo en InformePdfTemplateService

**Files:**
- Modify: `sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/application/service/InformePdfTemplateService.java`
- Modify: `sigcon-backend/src/test/java/co/gov/bogota/sed/sigcon/application/InformePdfTemplateServiceTest.java`

- [ ] **Step 1: (TDD RED) Escribir 2 tests en `InformePdfTemplateServiceTest.java`**

En `InformePdfTemplateServiceTest.java`, añadir al final de la clase:

```java
@Test
void seccion1UsaPlazoCustomSiExiste() throws Exception {
    Informe informe = informe();
    informe.getContrato().setPlazo("El plazo del contrato es de seis (6) meses contados desde la suscripción del acta de inicio.");

    InformePdfTemplateService service = new InformePdfTemplateService(
        mock(ActividadInformeRepository.class),
        mock(SoporteAdjuntoRepository.class),
        mock(DocumentoAdicionalRepository.class),
        mock(AporteSgssiRepository.class)
    );

    java.lang.reflect.Method buildHtml = InformePdfTemplateService.class.getDeclaredMethod(
        "buildHtml",
        Informe.class, java.util.List.class, java.util.List.class,
        java.util.List.class, byte[].class, byte[].class, byte[].class
    );
    buildHtml.setAccessible(true);

    String html = (String) buildHtml.invoke(
        service, informe,
        Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
        new byte[]{1}, new byte[]{1}, new byte[]{1}
    );

    assertThat(html).contains("El plazo del contrato es de seis (6) meses");
    // No debe aparecer el texto hardcodeado cuando hay plazo personalizado
    assertThat(html).doesNotContain("ser&#225; hasta el");
}

@Test
void seccion1UsaFallbackSiPlazoNulo() throws Exception {
    Informe informe = informe();
    informe.getContrato().setPlazo(null);

    InformePdfTemplateService service = new InformePdfTemplateService(
        mock(ActividadInformeRepository.class),
        mock(SoporteAdjuntoRepository.class),
        mock(DocumentoAdicionalRepository.class),
        mock(AporteSgssiRepository.class)
    );

    java.lang.reflect.Method buildHtml = InformePdfTemplateService.class.getDeclaredMethod(
        "buildHtml",
        Informe.class, java.util.List.class, java.util.List.class,
        java.util.List.class, byte[].class, byte[].class, byte[].class
    );
    buildHtml.setAccessible(true);

    String html = (String) buildHtml.invoke(
        service, informe,
        Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
        new byte[]{1}, new byte[]{1}, new byte[]{1}
    );

    // Texto hardcodeado aparece como fallback
    assertThat(html).contains("ser&#225; hasta el");
}
```

- [ ] **Step 2: Ejecutar tests RED**

```powershell
cd "C:\Users\jmep2\Downloads\AgenIALab\ProyectoContratosSED\sigcon-backend"
mvn test "-Dtest=InformePdfTemplateServiceTest#seccion1UsaPlazoCustomSiExiste+seccion1UsaFallbackSiPlazoNulo" -q
```

Esperado: FAIL — el método aún usa texto hardcodeado sin leer `c.getPlazo()`.

- [ ] **Step 3: Modificar `appendSeccion1` en `InformePdfTemplateService.java`**

Localizar en `appendSeccion1` el bloque que construye el texto del plazo (líneas ~322–326):

```java
String plazo = "El plazo del contrato ser&#225; hasta el " + c.getFechaFin().format(DATE_FMT)
    + " y a partir de la suscripci&#243;n del acta de inicio, previo cumplimiento de los"
    + " requisitos de perfeccionamiento y ejecuci&#243;n. En todo caso, la fecha de Inicio"
    + " no podr&#225; ser anterior al " + c.getFechaInicio().format(DATE_FMT) + ".";
fila2(sb, "Plazo:", plazo, false);
```

Reemplazar **todo ese bloque** por:

```java
String plazoTexto;
if (notEmpty(c.getPlazo())) {
    plazoTexto = esc(c.getPlazo());
} else {
    plazoTexto = "El plazo del contrato ser&#225; hasta el " + c.getFechaFin().format(DATE_FMT)
        + " y a partir de la suscripci&#243;n del acta de inicio, previo cumplimiento de los"
        + " requisitos de perfeccionamiento y ejecuci&#243;n. En todo caso, la fecha de Inicio"
        + " no podr&#225; ser anterior al " + c.getFechaInicio().format(DATE_FMT) + ".";
}
fila2(sb, "Plazo:", plazoTexto, false);
```

> Nota: `notEmpty()` ya existe en el servicio. Verifica que recibe `String` y devuelve `boolean`.

- [ ] **Step 4: Ejecutar tests GREEN**

```powershell
cd "C:\Users\jmep2\Downloads\AgenIALab\ProyectoContratosSED\sigcon-backend"
mvn test "-Dtest=InformePdfTemplateServiceTest" -q
```

Esperado: BUILD SUCCESS, todos los tests de `InformePdfTemplateServiceTest` pasan.

- [ ] **Step 5: Suite completa backend**

```powershell
cd "C:\Users\jmep2\Downloads\AgenIALab\ProyectoContratosSED\sigcon-backend"
mvn test -q
```

Esperado: BUILD SUCCESS, 0 failures.

- [ ] **Step 6: Commit PDF plazo**

```powershell
cd "C:\Users\jmep2\Downloads\AgenIALab\ProyectoContratosSED"
git add sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/application/service/InformePdfTemplateService.java
git add sigcon-backend/src/test/java/co/gov/bogota/sed/sigcon/application/InformePdfTemplateServiceTest.java
git commit -m "feat(i16): InformePdfTemplateService — usar plazo libre del contrato en Seccion1, fallback a texto calculado"
```

---

## Task 7 — Frontend: modelos y servicio Angular

**Files:**
- Modify: `sigcon-angular/src/app/core/models/contrato.model.ts`
- Modify: `sigcon-angular/src/app/core/services/contrato.service.ts`

- [ ] **Step 1: Actualizar `contrato.model.ts`**

En la interface `ContratoDetalle`, después de `modificaciones?: string | null;`, añadir:

```typescript
bloqueadoCargaInforme: boolean;
plazo?: string | null;
```

En la interface `ContratoRequest`, después de `modificaciones?: string | null;`, añadir:

```typescript
plazo?: string | null;
```

- [ ] **Step 2: Agregar método en `contrato.service.ts`**

Primero abre el archivo para ver la estructura de los métodos existentes. Luego añade el nuevo método al final de la clase, antes del cierre `}`:

```typescript
actualizarBloqueoInforme(id: number, bloqueado: boolean): Observable<ContratoDetalle> {
  return this.http.patch<ContratoDetalle>(
    `${this.apiUrl}/${id}/bloqueo-informe`,
    { bloqueado }
  );
}
```

> Nota: `this.apiUrl` debe apuntar a `/api/admin/contratos`. Verifica cómo están construidas las URLs en los otros métodos del servicio y adapta si usa una base diferente.

- [ ] **Step 3: Verificar compilación Angular**

```powershell
cd "C:\Users\jmep2\Downloads\AgenIALab\ProyectoContratosSED\sigcon-angular"
npx ng build --configuration development 2>&1 | Select-String -Pattern "ERROR|error TS" | Select-Object -First 20
```

Esperado: sin errores de compilación TypeScript.

- [ ] **Step 4: Commit modelos y servicio**

```powershell
cd "C:\Users\jmep2\Downloads\AgenIALab\ProyectoContratosSED"
git add sigcon-angular/src/app/core/models/contrato.model.ts
git add sigcon-angular/src/app/core/services/contrato.service.ts
git commit -m "feat(i16): Angular models/service — bloqueadoCargaInforme, plazo, actualizarBloqueoInforme()"
```

---

## Task 8 — Frontend: contrato-detalle (contratista)

**Files:**
- Modify: `sigcon-angular/src/app/features/contratos/detalle/contrato-detalle.component.ts`

- [ ] **Step 1: Ocultar botón "Nuevo Informe" si contrato está bloqueado**

En `contrato-detalle.component.ts`, localiza el botón o enlace que permite crear un nuevo informe. Tendrá algo como `routerLink` hacia `/informes/nuevo` o un botón con `(click)` para navegar. Envuélvelo en una condición:

```html
@if (!contrato().bloqueadoCargaInforme) {
  <!-- botón o enlace "Nuevo Informe" existente aquí -->
}
```

- [ ] **Step 2: Mostrar campo Plazo en la sección de datos generales**

En la sección `<dl>` de datos generales, añadir junto a `formaPago` y `modificaciones`:

```html
@if (c.plazo) {
  <div class="lg:col-span-2">
    <dt class="text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">Plazo</dt>
    <dd class="mt-xs text-[var(--color-on-surface)]">{{ c.plazo }}</dd>
  </div>
}
```

- [ ] **Step 3: Verificar compilación**

```powershell
cd "C:\Users\jmep2\Downloads\AgenIALab\ProyectoContratosSED\sigcon-angular"
npx ng build --configuration development 2>&1 | Select-String -Pattern "ERROR|error TS" | Select-Object -First 20
```

Esperado: sin errores.

- [ ] **Step 4: Commit detalle contratista**

```powershell
cd "C:\Users\jmep2\Downloads\AgenIALab\ProyectoContratosSED"
git add sigcon-angular/src/app/features/contratos/detalle/contrato-detalle.component.ts
git commit -m "feat(i16): contrato-detalle — ocultar 'Nuevo Informe' si bloqueado, mostrar plazo"
```

---

## Task 9 — Frontend: formulario admin (crear/editar contrato)

**Files:**
- Modify: `sigcon-angular/src/app/features/admin/contratos/admin-contrato-form.component.ts`

- [ ] **Step 1: Añadir campo `plazo` al objeto `form`**

En `admin-contrato-form.component.ts`, localizar la definición del objeto `form` (tendrá campos como `numero`, `objeto`, `formaPago`, `modificaciones`). Añadir `plazo`:

```typescript
plazo: null as string | null,
```

- [ ] **Step 2: Añadir textarea "Plazo" al template**

En el template, localizar el bloque del campo "Modificaciones" (tiene un `<textarea>` con `name="modificaciones"`). Añadir **antes** de ese bloque:

```html
<div class="space-y-xs lg:col-span-2">
  <label class="text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">Plazo</label>
  <textarea class="input-field" rows="3" name="plazo" [(ngModel)]="form.plazo"></textarea>
  <p class="text-xs text-[var(--color-on-surface-variant)]">
    Si se deja vacío, se usará el texto estándar basado en las fechas del contrato.
  </p>
</div>
```

- [ ] **Step 3: Incluir `plazo` en el objeto de request al guardar**

Localizar el método `guardar()` donde se construye el objeto `ContratoRequest`. Añadir `plazo: this.form.plazo` al objeto enviado.

- [ ] **Step 4: Precargar `plazo` al editar**

En el bloque que carga datos al editar (donde se asigna `this.form.numero = c.numero`, etc.), añadir:

```typescript
this.form.plazo = c.plazo ?? null;
```

- [ ] **Step 5: Verificar compilación**

```powershell
cd "C:\Users\jmep2\Downloads\AgenIALab\ProyectoContratosSED\sigcon-angular"
npx ng build --configuration development 2>&1 | Select-String -Pattern "ERROR|error TS" | Select-Object -First 20
```

Esperado: sin errores.

- [ ] **Step 6: Commit form admin**

```powershell
cd "C:\Users\jmep2\Downloads\AgenIALab\ProyectoContratosSED"
git add sigcon-angular/src/app/features/admin/contratos/admin-contrato-form.component.ts
git commit -m "feat(i16): admin-contrato-form — textarea Plazo con hint retrocompatibilidad"
```

---

## Task 10 — Frontend: panel admin toggle bloqueo

**Files:**
- Modify: `sigcon-angular/src/app/features/admin/contratos/admin-contratos.component.ts`

- [ ] **Step 1: Añadir método `toggleBloqueo` al componente**

En `admin-contratos.component.ts`, inyectar `ContratoService` si no lo tiene ya. Añadir el método:

```typescript
toggleBloqueo(contrato: ContratoDetalle): void {
  const nuevoEstado = !contrato.bloqueadoCargaInforme;
  this.contratoService.actualizarBloqueoInforme(contrato.id, nuevoEstado).subscribe({
    next: (actualizado) => {
      contrato.bloqueadoCargaInforme = actualizado.bloqueadoCargaInforme;
    },
    error: () => {
      // el estado queda sin cambiar; el usuario ve el toggle en su posición original
    }
  });
}
```

- [ ] **Step 2: Añadir toggle en el template**

En el template, en la fila o card de cada contrato (donde ya aparecen acciones como editar/eliminar), añadir:

```html
<button
  type="button"
  class="text-xs px-sm py-xs rounded border"
  [class.border-red-400]="c.bloqueadoCargaInforme"
  [class.text-red-600]="c.bloqueadoCargaInforme"
  [class.border-[var(--color-outline)]]="!c.bloqueadoCargaInforme"
  [class.text-[var(--color-on-surface-variant)]]="!c.bloqueadoCargaInforme"
  (click)="toggleBloqueo(c)"
  [title]="c.bloqueadoCargaInforme ? 'Desbloquear carga de informes' : 'Bloquear carga de informes'"
>
  {{ c.bloqueadoCargaInforme ? 'Desbloquear informes' : 'Bloquear informes' }}
</button>
```

> Nota: revisa qué tipo usa el loop (`@for (c of contratos; ...)`) para asegurarte de que `c` es `ContratoDetalle` y no `ContratoResumen`. Si es `ContratoResumen`, deberás cambiar el tipo del listado o cargar el detalle antes de llamar al toggle.

- [ ] **Step 3: Verificar compilación**

```powershell
cd "C:\Users\jmep2\Downloads\AgenIALab\ProyectoContratosSED\sigcon-angular"
npx ng build --configuration development 2>&1 | Select-String -Pattern "ERROR|error TS" | Select-Object -First 20
```

Esperado: sin errores.

- [ ] **Step 4: Commit toggle admin**

```powershell
cd "C:\Users\jmep2\Downloads\AgenIALab\ProyectoContratosSED"
git add sigcon-angular/src/app/features/admin/contratos/admin-contratos.component.ts
git commit -m "feat(i16): admin-contratos — toggle bloqueo/desbloqueo de carga de informes por contrato"
```

---

## Task 11 — Verificación final y documentación

- [ ] **Step 1: Suite completa backend**

```powershell
cd "C:\Users\jmep2\Downloads\AgenIALab\ProyectoContratosSED\sigcon-backend"
mvn test
```

Esperado: BUILD SUCCESS, 0 failures. Anotar el número total de tests.

- [ ] **Step 2: Build completo Angular**

```powershell
cd "C:\Users\jmep2\Downloads\AgenIALab\ProyectoContratosSED\sigcon-angular"
npx ng build --configuration development 2>&1 | tail -5
```

Esperado: `Build at:` sin errores.

- [ ] **Step 3: Crear execution log**

Crear `docs/plans/2026-06-10-sigcon-i16-execution-log.md` con el resumen de lo ejecutado, tests pasados y observaciones.

- [ ] **Step 4: Actualizar README.md**

En `README.md`, en la tabla de incrementos, añadir la fila de I16:

```markdown
| I16 | 2026-06-10 | Bloqueo individual carga informes + campo Plazo libre en contrato | Cerrado |
```

- [ ] **Step 5: Commit de cierre y push**

```powershell
cd "C:\Users\jmep2\Downloads\AgenIALab\ProyectoContratosSED"
git add docs/plans/2026-06-10-sigcon-i16-plan.md
git add docs/plans/2026-06-10-sigcon-i16-execution-log.md
git add README.md
git commit -m "docs(i16): plan, execution log y README — I16 cerrado"
git push origin main
```
