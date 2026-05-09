# Plan de Implementación — SIGCON Incremento 6
## Rediseño PDF Institucional + Datos Complementarios del Informe

> **Metodología:** Spec-Driven Development (SDD) — Spec-Anchored  
> **Versión:** 1.0 — **Fecha:** 2026-05-09  
> **Spec de referencia:** `docs/specs/2026-05-09-sigcon-i6-spec.md`  
> **Rama:** `feat/sigcon-i6` (base: `feat/sigcon-i5`)  
> **Estado:** Listo para ejecución

---

## Resumen Ejecutivo

Incremento de **12 tareas** que abarca DDL, backend y frontend. El orden es lineal con dependencias marcadas.

| Tarea | Scope | Descripción |
|-------|-------|-------------|
| T1 | Infraestructura | Crear rama `feat/sigcon-i6` + actualizar DDL |
| T2 | Backend — Dominio | Nuevo enum `ItemSgssi`, nueva entidad `AporteSgssi`, extensión de `Contrato`, `Informe`, `Usuario` + nuevo `AporteSgssiRepository` |
| T3 | Backend — Utilidad | `NumeroPesosConverter` (BigDecimal → texto en letras pesos colombianos) |
| T4 | Backend — DTOs | `AporteSgssiRequest`, `AporteSgssiDto`; extensión de `ContratoRequest/DetalleDto`, `InformeRequest/UpdateDto/DetalleDto`, `PerfilUpdateRequest/UsuarioDto` |
| T5 | Backend — Servicios | `AporteSgssiService` (nuevo); extensión de `InformeService`, `ContratoService`, `UsuarioService` |
| T6 | Backend — Controlador | `AporteSgssiController` (GET + PUT `/api/informes/{id}/aportes-sgssi`) |
| T7 | Backend — Tests | 23 tests nuevos: `NumeroPesosConverterTest`, `AporteSgssiServiceTest`, extensiones de `InformeServiceTest`, `ContratoServiceTest`, `SecurityIntegrationTest` |
| T8 | Backend — PDF | Extensión de `PdfInformeService` (carga firma revisor); rediseño completo de `InformePdfTemplateService` |
| T9 | Frontend — Perfil + Contrato | Campos SGSSI en `PerfilComponent`; Dependencia (dropdown SED), FormaPago, Modificaciones en `AdminContratoFormComponent`; `sed-dependencias.constants.ts` |
| T10 | Frontend — Servicio Angular | `AporteSgssiService` Angular (`core/services/aporte-sgssi.service.ts`) |
| T11 | Frontend — Formulario informe | Campos nuevos (desembolso, ejecución, correspondencia) + sección SGSSI en `InformeFormComponent` |
| T12 | Frontend — Detalle + Tests | Sección SGSSI editable + eliminación porcentaje por actividad en `InformeDetalleComponent` + 16 nuevos specs + guía de pruebas |

---

## T1 — Infraestructura: Rama + DDL

**Archivos a modificar:**
- `db/00_setup.sql`

**Acciones:**

### 1.1 Crear rama

```bash
git checkout feat/sigcon-i5
git checkout -b feat/sigcon-i6
```

### 1.2 Agregar DDL al final de `db/00_setup.sql`

Agregar bloque con separador de comentario:

```sql
-- ============================================================
-- SIGCON I6 — Rediseño PDF + Datos Complementarios
-- ============================================================

-- Ajustes SGCN_CONTRATOS
ALTER TABLE SGCN_CONTRATOS ADD (
    DEPENDENCIA     VARCHAR2(500),
    FORMA_PAGO      CLOB,
    MODIFICACIONES  VARCHAR2(2000)  DEFAULT 'No se han presentado'
);

-- Ajustes SGCN_INFORMES
ALTER TABLE SGCN_INFORMES ADD (
    NUMERO_DESEMBOLSO         NUMBER(5),
    VALOR_DESEMBOLSO          NUMBER(14,2),
    PORCENTAJE_EJECUCION      NUMBER(5,2),
    CORRESPONDENCIA_PENDIENTE NUMBER(1) DEFAULT 0 NOT NULL
);

-- Ajustes SGCN_USUARIOS
ALTER TABLE SGCN_USUARIOS ADD (
    SGSSI_SALUD_ENTIDAD    VARCHAR2(200),
    SGSSI_PENSION_ENTIDAD  VARCHAR2(200),
    SGSSI_ARL_ENTIDAD      VARCHAR2(200)
);

-- Nueva secuencia + tabla SGCN_APORTES_SGSSI
CREATE SEQUENCE SGCN_APORTES_SGSSI_SEQ START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE TABLE SGCN_APORTES_SGSSI (
    ID              NUMBER          DEFAULT SGCN_APORTES_SGSSI_SEQ.NEXTVAL PRIMARY KEY,
    ID_INFORME      NUMBER          NOT NULL,
    ITEM            VARCHAR2(20)    NOT NULL,
    FECHA_PAGO      DATE            NOT NULL,
    VALOR_APORTADO  NUMBER(12,2)    NOT NULL,
    ENTIDAD         VARCHAR2(200)   NOT NULL,
    ACTIVO          NUMBER(1)       DEFAULT 1 NOT NULL,
    CREATED_AT      TIMESTAMP       DEFAULT SYSTIMESTAMP NOT NULL,
    CREATED_BY      VARCHAR2(200),
    UPDATED_AT      TIMESTAMP,
    CONSTRAINT FK_APORTES_SGSSI_INFORME
        FOREIGN KEY (ID_INFORME) REFERENCES SGCN_INFORMES(ID)
);

CREATE INDEX IDX_APORTES_SGSSI_INFORME ON SGCN_APORTES_SGSSI(ID_INFORME);

CREATE OR REPLACE TRIGGER TRG_APORTES_SGSSI_AUDIT
BEFORE UPDATE ON SGCN_APORTES_SGSSI
FOR EACH ROW
BEGIN
    :NEW.UPDATED_AT := SYSTIMESTAMP;
END;
/
```

**Commit:** `chore(i6): bootstrap branch and DDL for pdf-rediseno-datos-complementarios`

**Validación:** `git log --oneline -1` muestra el commit; `db/00_setup.sql` incluye el bloque I6 al final.

---

## T2 — Backend: Dominio (Entidades + Repository)

**Spec:** secciones 4.1

**Archivos a crear/modificar:**
- `sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/domain/enums/ItemSgssi.java` ← nuevo
- `sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/domain/entity/AporteSgssi.java` ← nuevo
- `sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/domain/repository/AporteSgssiRepository.java` ← nuevo
- `sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/domain/entity/Contrato.java` ← extender
- `sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/domain/entity/Informe.java` ← extender
- `sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/domain/entity/Usuario.java` ← extender

### 2.1 Enum `ItemSgssi`

```java
package co.gov.bogota.sed.sigcon.domain.enums;

public enum ItemSgssi {
    SALUD, PENSION, ARL
}
```

### 2.2 Entidad `AporteSgssi`

Usar exactamente los campos de la spec 4.1. Restricciones Java 8: sin `var`, sin `List.of()`.
Lombok: `@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor`.
`activo` inicializado a `1` en la declaración del campo.

### 2.3 Repository `AporteSgssiRepository`

```java
public interface AporteSgssiRepository extends JpaRepository<AporteSgssi, Long> {
    List<AporteSgssi> findByInformeIdAndActivoTrue(Long informeId);
}
```

> **Nota:** No declarar `deleteByInformeId` — el borrado lógico se hace actualizando `activo = 0` en el servicio, no con DELETE físico.

### 2.4 Extender `Contrato`

Agregar al final de los campos existentes (antes de relaciones):

```java
@Column(name = "DEPENDENCIA", length = 500)
private String dependencia;

@Lob
@Column(name = "FORMA_PAGO")
private String formaPago;

@Column(name = "MODIFICACIONES", length = 2000)
private String modificaciones;
```

### 2.5 Ajuste a `ActividadInforme`

El campo `porcentaje` **permanece mapeado** en la entidad (evita error Hibernate) pero se elimina de los DTOs:

```java
// Mantener el campo mapeado SIN CAMBIO:
@Column(name = "PORCENTAJE")
private Integer porcentaje;
```

No agregar getter/setter nuevos. El campo dejará de poblarse desde I6 en adelante.

### 2.6 Extender `Informe`

```java
@Column(name = "NUMERO_DESEMBOLSO")
private Integer numeroDesembolso;

@Column(name = "VALOR_DESEMBOLSO", precision = 14, scale = 2)
private java.math.BigDecimal valorDesembolso;

@Column(name = "PORCENTAJE_EJECUCION", precision = 5, scale = 2)
private java.math.BigDecimal porcentajeEjecucion;

@Column(name = "CORRESPONDENCIA_PENDIENTE", nullable = false)
private Integer correspondenciaPendiente = 0;
```

### 2.7 Extender `Usuario`

```java
@Column(name = "SGSSI_SALUD_ENTIDAD", length = 200)
private String sgssiSaludEntidad;

@Column(name = "SGSSI_PENSION_ENTIDAD", length = 200)
private String sgssiPensionEntidad;

@Column(name = "SGSSI_ARL_ENTIDAD", length = 200)
private String sgssiArlEntidad;
```

**Commit:** `feat(i6): add AporteSgssi entity, ItemSgssi enum, and domain extensions`

**Validación:** `mvn compile -pl sigcon-backend -q` sin errores.

---

## T3 — Backend: `NumeroPesosConverter`

**Spec:** sección 4.3 (NumeroPesosConverter)

**Archivo a crear:**
- `sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/application/service/NumeroPesosConverter.java`

### 3.1 Estructura del converter

Clase utilitaria con método estático `convertir(BigDecimal monto)`. Compatible con Java 8.

Algoritmo:
1. Tomar la parte entera del monto (`monto.toBigInteger().longValue()`).
2. Si es 0 → retornar `"CERO PESOS M/CTE"`.
3. Descomponer en grupos: millones (`/ 1_000_000`), miles (`% 1_000_000 / 1_000`), unidades (`% 1_000`).
4. Convertir cada grupo de 3 dígitos a texto usando arreglos de unidades, decenas y centenas en español.
5. Concatenar con "MILLONES", "MIL", y las unidades, manejando el artículo "UN MILLÓN" vs "MILLONES".
6. Añadir sufijo `" PESOS M/CTE"`.

Arreglos de referencia (Java 8, sin `List.of()`):

```java
private static final String[] UNIDADES = {
    "", "UN", "DOS", "TRES", "CUATRO", "CINCO",
    "SEIS", "SIETE", "OCHO", "NUEVE", "DIEZ",
    "ONCE", "DOCE", "TRECE", "CATORCE", "QUINCE",
    "DIECISÉIS", "DIECISIETE", "DIECIOCHO", "DIECINUEVE"
};

private static final String[] DECENAS = {
    "", "DIEZ", "VEINTE", "TREINTA", "CUARENTA", "CINCUENTA",
    "SESENTA", "SETENTA", "OCHENTA", "NOVENTA"
};

private static final String[] CENTENAS = {
    "", "CIEN", "DOSCIENTOS", "TRESCIENTOS", "CUATROCIENTOS", "QUINIENTOS",
    "SEISCIENTOS", "SETECIENTOS", "OCHOCIENTOS", "NOVECIENTOS"
};
```

Casos especiales:
- 100 → "CIEN" (no "CIENTO")
- 101–199 → "CIENTO ..."
- 1.000.000 → "UN MILLÓN"
- 1.000.000.000 → "MIL MILLONES"
- Veinte + 1–9 → "VEINTIUNO", "VEINTIDÓS", etc.

**Commit:** `feat(i6): add NumeroPesosConverter utility for peso amounts in text`

**Validación:** `mvn compile -pl sigcon-backend -q` sin errores.

---

## T4 — Backend: DTOs

**Spec:** sección 4.2

**Archivos a crear/modificar:**
- `sigcon-backend/src/main/java/.../web/dto/AporteSgssiRequest.java` ← nuevo
- `sigcon-backend/src/main/java/.../web/dto/AporteSgssiDto.java` ← nuevo
- `sigcon-backend/src/main/java/.../web/dto/ContratoRequest.java` ← extender
- `sigcon-backend/src/main/java/.../web/dto/ContratoDetalleDto.java` ← extender
- `sigcon-backend/src/main/java/.../web/dto/InformeRequest.java` ← extender
- `sigcon-backend/src/main/java/.../web/dto/InformeUpdateDto.java` ← extender
- `sigcon-backend/src/main/java/.../web/dto/InformeDetalleDto.java` ← extender
- `sigcon-backend/src/main/java/.../web/dto/PerfilUpdateRequest.java` ← extender
- `sigcon-backend/src/main/java/.../web/dto/UsuarioDto.java` ← extender
- `sigcon-backend/src/main/java/.../web/dto/ActividadInformeRequest.java` ← eliminar campo `porcentaje`
- `sigcon-backend/src/main/java/.../web/dto/ActividadInformeDto.java` ← eliminar campo `porcentaje`

### 4.1 `AporteSgssiRequest`

```java
@Getter @Setter
public class AporteSgssiRequest {
    @NotNull
    private ItemSgssi item;

    @NotNull
    private LocalDate fechaPago;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal valorAportado;

    @NotBlank
    @Size(max = 200)
    private String entidad;
}
```

### 4.2 `AporteSgssiDto`

```java
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class AporteSgssiDto {
    private Long id;
    private ItemSgssi item;
    private String periodoSgssi;   // "mm/aaaa" derivado
    private LocalDate fechaPago;
    private BigDecimal valorAportado;
    private String entidad;
}
```

### 4.3 Extensiones en DTOs existentes

Todos los campos nuevos son opcionales (`nullable`/sin anotación `@NotNull`) salvo donde la spec indica restricción.

En `ContratoRequest` / `ContratoDetalleDto` agregar: `dependencia` (`@Size(max=500)`), `formaPago`, `modificaciones` (`@Size(max=2000)`).

En `InformeRequest` agregar: `numeroDesembolso`, `valorDesembolso` (`@DecimalMin("0.00")`), `porcentajeEjecucion` (`@DecimalMin("0.00") @DecimalMax("100.00")`), `correspondenciaPendiente` (Boolean), `aportesSgssi` (List\<AporteSgssiRequest\>).

En `InformeUpdateDto` agregar los mismos cinco campos que en `InformeRequest`.

En `InformeDetalleDto` agregar: `numeroDesembolso`, `valorDesembolso`, `porcentajeEjecucion`, `correspondenciaPendiente` (Boolean), `aportesSgssi` (List\<AporteSgssiDto\>).

En `PerfilUpdateRequest` agregar: `sgssiSaludEntidad`, `sgssiPensionEntidad`, `sgssiArlEntidad` (todos `@Size(max=200)`).

En `UsuarioDto` agregar: `sgssiSaludEntidad`, `sgssiPensionEntidad`, `sgssiArlEntidad`.

**Commit:** `feat(i6): add AporteSgssi DTOs and extend contract/informe/perfil DTOs`

**Validación:** `mvn compile -pl sigcon-backend -q` sin errores.

---

## T5 — Backend: Servicios

**Spec:** sección 4.3

**Archivos a crear/modificar:**
- `sigcon-backend/src/main/java/.../application/service/AporteSgssiService.java` ← nuevo
- `sigcon-backend/src/main/java/.../application/service/InformeService.java` ← extender `crear()` y `actualizar()`
- `sigcon-backend/src/main/java/.../application/service/ContratoService.java` ← extender `crear()` y `actualizar()`
- `sigcon-backend/src/main/java/.../application/service/UsuarioService.java` ← extender `actualizarPerfil()`

### 5.1 `AporteSgssiService` (nuevo)

```java
@Service
@Transactional
public class AporteSgssiService {

    private final AporteSgssiRepository repository;
    private final InformeRepository informeRepository;

    public AporteSgssiService(AporteSgssiRepository repository,
                               InformeRepository informeRepository) {
        this.repository = repository;
        this.informeRepository = informeRepository;
    }

    public List<AporteSgssiDto> guardarAportes(Long informeId,
                                                List<AporteSgssiRequest> requests,
                                                String contratistaEmail) {
        Informe informe = informeRepository.findById(informeId)
            .orElseThrow(() -> new SigconBusinessException(
                ErrorCode.INFORME_NO_ENCONTRADO, "Informe no encontrado", HttpStatus.NOT_FOUND));

        EstadoInforme estado = informe.getEstado();
        if (estado != EstadoInforme.BORRADOR && estado != EstadoInforme.DEVUELTO) {
            throw new SigconBusinessException(
                ErrorCode.INFORME_NO_EDITABLE,
                "El informe debe estar en estado BORRADOR o DEVUELTO para modificar los aportes.",
                HttpStatus.CONFLICT);
        }

        String propietario = informe.getContrato().getContratista().getEmail();
        if (!propietario.equalsIgnoreCase(contratistaEmail)) {
            throw new SigconBusinessException(
                ErrorCode.ACCESO_DENEGADO, "No autorizado", HttpStatus.FORBIDDEN);
        }

        // Validar unicidad de ítems
        long distintos = requests.stream()
            .map(AporteSgssiRequest::getItem)
            .distinct()
            .count();
        if (distintos < requests.size()) {
            throw new SigconBusinessException(
                ErrorCode.VALIDACION_FALLIDA,
                "No puede haber dos aportes del mismo ítem SGSSI en el mismo informe.",
                HttpStatus.BAD_REQUEST);
        }

        // Borrado lógico de aportes previos
        List<AporteSgssi> previos = repository.findByInformeIdAndActivoTrue(informeId);
        for (AporteSgssi a : previos) {
            a.setActivo(0);
            repository.save(a);
        }

        // Insertar nuevos
        String periodo = calcularPeriodo(informe);
        List<AporteSgssiDto> resultado = new ArrayList<AporteSgssiDto>();
        for (AporteSgssiRequest req : requests) {
            AporteSgssi nuevo = new AporteSgssi();
            nuevo.setInforme(informe);
            nuevo.setItem(req.getItem());
            nuevo.setFechaPago(req.getFechaPago());
            nuevo.setValorAportado(req.getValorAportado());
            nuevo.setEntidad(req.getEntidad());
            nuevo.setActivo(1);
            AporteSgssi guardado = repository.save(nuevo);
            resultado.add(toDto(guardado, periodo));
        }
        return resultado;
    }

    @Transactional(readOnly = true)
    public List<AporteSgssiDto> listarAportes(Long informeId) {
        Informe informe = informeRepository.findById(informeId)
            .orElseThrow(() -> new SigconBusinessException(
                ErrorCode.INFORME_NO_ENCONTRADO, "Informe no encontrado", HttpStatus.NOT_FOUND));
        String periodo = calcularPeriodo(informe);
        List<AporteSgssi> aportes = repository.findByInformeIdAndActivoTrue(informeId);
        List<AporteSgssiDto> dtos = new ArrayList<AporteSgssiDto>();
        for (AporteSgssi a : aportes) {
            dtos.add(toDto(a, periodo));
        }
        return dtos;
    }

    private String calcularPeriodo(Informe informe) {
        java.time.YearMonth ym = java.time.YearMonth.from(informe.getFechaInicio()).minusMonths(1);
        return String.format("%02d/%d", ym.getMonthValue(), ym.getYear());
    }

    private AporteSgssiDto toDto(AporteSgssi a, String periodo) {
        return AporteSgssiDto.builder()
            .id(a.getId())
            .item(a.getItem())
            .periodoSgssi(periodo)
            .fechaPago(a.getFechaPago())
            .valorAportado(a.getValorAportado())
            .entidad(a.getEntidad())
            .build();
    }
}
```

### 5.2 Extender `InformeService.crear()`

Después del `informeRepository.save(informe)` inicial, aplicar los nuevos campos si el DTO los trae:

```java
if (dto.getNumeroDesembolso() != null) informe.setNumeroDesembolso(dto.getNumeroDesembolso());
if (dto.getValorDesembolso() != null)  informe.setValorDesembolso(dto.getValorDesembolso());
if (dto.getPorcentajeEjecucion() != null) informe.setPorcentajeEjecucion(dto.getPorcentajeEjecucion());
informe.setCorrespondenciaPendiente(
    Boolean.TRUE.equals(dto.getCorrespondenciaPendiente()) ? 1 : 0);
informeRepository.save(informe);

if (dto.getAportesSgssi() != null && !dto.getAportesSgssi().isEmpty()) {
    aporteSgssiService.guardarAportes(informe.getId(), dto.getAportesSgssi(), contratistaEmail);
}
```

### 5.3 Extender `InformeService.actualizar()` (PATCH)

Después de las asignaciones existentes de `fechaInicio`/`fechaFin`:

```java
if (dto.getNumeroDesembolso() != null)    informe.setNumeroDesembolso(dto.getNumeroDesembolso());
if (dto.getValorDesembolso() != null)     informe.setValorDesembolso(dto.getValorDesembolso());
if (dto.getPorcentajeEjecucion() != null) informe.setPorcentajeEjecucion(dto.getPorcentajeEjecucion());
if (dto.getCorrespondenciaPendiente() != null) {
    informe.setCorrespondenciaPendiente(Boolean.TRUE.equals(dto.getCorrespondenciaPendiente()) ? 1 : 0);
}
if (dto.getAportesSgssi() != null) {
    aporteSgssiService.guardarAportes(informe.getId(), dto.getAportesSgssi(), contratistaEmail);
}
```

### 5.4 Extender `ContratoService`

En `crear(dto)` y `actualizar(id, dto)`:

```java
contrato.setDependencia(dto.getDependencia());
contrato.setFormaPago(dto.getFormaPago());
if (dto.getModificaciones() != null && !dto.getModificaciones().trim().isEmpty()) {
    contrato.setModificaciones(dto.getModificaciones());
} else if (contrato.getModificaciones() == null) {
    contrato.setModificaciones("No se han presentado");
}
```

### 5.5 Extender `UsuarioService.actualizarPerfil()`

```java
if (dto.getSgssiSaludEntidad() != null)   usuario.setSgssiSaludEntidad(dto.getSgssiSaludEntidad());
if (dto.getSgssiPensionEntidad() != null) usuario.setSgssiPensionEntidad(dto.getSgssiPensionEntidad());
if (dto.getSgssiArlEntidad() != null)     usuario.setSgssiArlEntidad(dto.getSgssiArlEntidad());
```

Extender el mapper que construye `UsuarioDto` para incluir los tres nuevos campos.

Extender el mapper de `InformeDetalleDto` para incluir los campos nuevos del informe y la lista `aportesSgssi` (llamar `aporteSgssiService.listarAportes(informe.getId())` al construir el DTO de detalle).

**Commit:** `feat(i6): add AporteSgssiService and extend InformeService, ContratoService, UsuarioService`

**Validación:** `mvn compile -pl sigcon-backend -q` sin errores.

---

## T6 — Backend: Controlador `AporteSgssiController`

**Spec:** sección 4.4

**Archivo a crear:**
- `sigcon-backend/src/main/java/.../web/controller/AporteSgssiController.java`

```java
@RestController
@RequestMapping("/api/informes/{informeId}/aportes-sgssi")
@Tag(name = "Aportes SGSSI", description = "Aportes de seguridad social por informe")
public class AporteSgssiController {

    private final AporteSgssiService service;

    public AporteSgssiController(AporteSgssiService service) {
        this.service = service;
    }

    @Operation(summary = "Listar aportes SGSSI del informe")
    @GetMapping
    @PreAuthorize("hasAnyRole('CONTRATISTA','REVISOR','SUPERVISOR','ADMIN')")
    public ResponseEntity<List<AporteSgssiDto>> listar(@PathVariable Long informeId) {
        return ResponseEntity.ok(service.listarAportes(informeId));
    }

    @Operation(summary = "Guardar aportes SGSSI (reemplaza todos)")
    @PutMapping
    @PreAuthorize("hasRole('CONTRATISTA')")
    public ResponseEntity<List<AporteSgssiDto>> guardar(
            @PathVariable Long informeId,
            @Valid @RequestBody List<AporteSgssiRequest> request,
            Authentication auth) {
        return ResponseEntity.ok(service.guardarAportes(informeId, request, auth.getName()));
    }
}
```

**Commit:** `feat(i6): add AporteSgssiController with GET and PUT endpoints`

**Validación:** `mvn compile -pl sigcon-backend -q` sin errores.

---

## T7 — Backend: Tests

**Spec:** sección 7 (backend)

**Archivos a crear/modificar:**
- `sigcon-backend/src/test/.../service/NumeroPesosConverterTest.java` ← nuevo
- `sigcon-backend/src/test/.../service/AporteSgssiServiceTest.java` ← nuevo
- `sigcon-backend/src/test/.../service/InformeServiceTest.java` ← extender
- `sigcon-backend/src/test/.../service/ContratoServiceTest.java` ← extender
- `sigcon-backend/src/test/.../integration/SecurityIntegrationTest.java` ← extender

### 7.1 `NumeroPesosConverterTest` (5 tests)

```java
@Test void convertirCero()       { assertEquals("CERO PESOS M/CTE", NumeroPesosConverter.convertir(BigDecimal.ZERO)); }
@Test void convertirMilUnidades(){ assertEquals("MIL PESOS M/CTE", NumeroPesosConverter.convertir(new BigDecimal("1000"))); }
@Test void convertirMillones()   { /* 118666667 → texto exacto */ }
@Test void convertirUnMillon()   { /* 1000000 → "UN MILLÓN PESOS M/CTE" */ }
@Test void convertirMilMillones(){ /* 1000000000 → "MIL MILLONES PESOS M/CTE" */ }
```

### 7.2 `AporteSgssiServiceTest` (6 tests)

| Test | Escenario |
|------|-----------|
| `guardarAportesExitoso` | 3 ítems en BORRADOR → lista con `periodoSgssi` correcto |
| `guardarAportesSinPension` | 2 ítems (SALUD + ARL) → exitoso |
| `guardarAportesInformeEnviadoFalla` | ENVIADO → `INFORME_NO_EDITABLE` 409 |
| `guardarAportesContratistaIncorrectoFalla` | Email diferente → `ACCESO_DENEGADO` 403 |
| `guardarAportesItemDuplicadoFalla` | Dos ítems SALUD → `VALIDACION_FALLIDA` 400 |
| `periodoSgssiEsCorrecto` | `fechaInicio = 2026-04-05` → `periodoSgssi = "03/2026"` |

### 7.3 `InformeServiceTest` (2 tests nuevos)

| Test | Escenario |
|------|-----------|
| `crearInformeConCamposNuevosExitoso` | DTO con `numeroDesembolso`, `valorDesembolso`, `porcentajeEjecucion` → persistidos en entidad |
| `actualizarInformeCamposNuevosExitoso` | PATCH con campos nuevos en BORRADOR → actualizados |

### 7.4 `ContratoServiceTest` (2 tests nuevos)

| Test | Escenario |
|------|-----------|
| `crearContratoConDependenciaYFormaPago` | Contrato creado con nuevos campos → retornados en DTO |
| `actualizarContratoNuevosCampos` | PUT actualiza `dependencia`, `formaPago`, `modificaciones` |

### 7.5 `SecurityIntegrationTest` (3 tests nuevos)

| Test | Escenario |
|------|-----------|
| `contratistaCanPutAportesSgssi` | PUT con rol CONTRATISTA → 2xx o 4xx de negocio (no 403) |
| `supervisorCannotPutAportesSgssi` | PUT con rol SUPERVISOR → 403 |
| `unauthenticatedCannotPutAportesSgssi` | PUT sin auth → 401 |

**Commit:** `test(i6): add NumeroPesosConverterTest, AporteSgssiServiceTest and backend regression tests`

**Validación:** `mvn test -pl sigcon-backend -q` → >= 146 tests, 0 fallos.

---

## T8 — Backend: Rediseño PDF

**Spec:** secciones 2 (coherencia), 4.6

**Archivos a modificar:**
- `sigcon-backend/src/main/java/.../application/service/PdfInformeService.java`
- `sigcon-backend/src/main/java/.../application/service/InformePdfTemplateService.java`

### 8.1 Extender `PdfInformeService.generarYPersistir()`

Después de leer `firmaContratista` y `firmaSupervisor`, agregar:

```java
byte[] firmaRevisor = null;
Usuario revisor = informe.getContrato().getRevisor();
if (revisor != null && revisor.getFirmaImagen() != null && !revisor.getFirmaImagen().isEmpty()) {
    firmaRevisor = readSignatureBytes(revisor);
}
```

Cambiar la llamada a `templateService`:

```java
byte[] pdfBytes = templateService.generarPdf(informe, firmaContratista, firmaSupervisor, firmaRevisor);
```

Inyectar también `AporteSgssiRepository` (o `AporteSgssiService`) para que `InformePdfTemplateService` pueda recibir los aportes. Alternativa más limpia: pasar los aportes ya cargados al método:

```java
List<AporteSgssi> aportes = aporteSgssiRepository.findByInformeIdAndActivoTrue(informe.getId());
byte[] pdfBytes = templateService.generarPdf(informe, actividades, aportes,
                                              firmaContratista, firmaSupervisor, firmaRevisor);
```

> **Nota:** El campo `actividades` ya se carga en `InformePdfTemplateService.generarPdf()`. Para evitar doble consulta, se puede mantener la carga dentro del servicio. Lo importante es que `generarPdf` reciba `firmaRevisor` como cuarto parámetro nullable.

### 8.2 Reescribir `InformePdfTemplateService`

El método `generarPdf()` pasa a tener la firma:

```java
public byte[] generarPdf(Informe informe,
                          byte[] firmaContratista,
                          byte[] firmaSupervisor,
                          byte[] firmaRevisor)    // null si no hay revisor
    throws IOException, DocumentException, Exception
```

Internamente carga actividades, soportes, documentos adicionales y aportes SGSSI (inyectar `AporteSgssiRepository`).

Reemplazar completamente `buildHtml()` con el layout de 8 secciones definido en la spec 4.6:

**Sección 1 — Encabezado institucional**
- `background-color: #002869`, texto blanco
- Entidad, subdirección, título "INFORME DE ACTIVIDADES", Informe No. / Contrato No.

**Sección 2 — Datos del Contrato**
- `background-color: #C0C0C0` para el título de sección
- Tabla vertical: Contratista, Objeto, Valor (letras + número via `NumeroPesosConverter`), Forma de Pago (omitir si null), Plazo (fechas inicio/fin), Modificaciones, Fecha de Inicio, Fecha de Terminación, Dependencia (omitir si null), Supervisor — Cargo

**Sección 3 — Ejecución de Actividades**
- Período (fechaInicio a fechaFin del informe)
- Tabla 3 columnas: Obligación Contractual | Actividades realizadas | Evidencia Verificable
- Evidencia: nombres de soportes separados por salto de línea (`<br/>`)
- Si no hay actividades: párrafo "Sin actividades registradas."

**Sección 4 — Aportes SGSSI**
- Tabla 5 columnas: ÍTEM | PERÍODO PAGO | FECHA DE PAGO | VALOR APORTADO | ENTIDAD
- Período derivado con `calcularPeriodo()` (mes anterior a `informe.getFechaInicio()`)
- Si lista vacía: celda colspan "Sin aportes registrados."

**Sección 5 — Estado de Radicación de Correspondencia**
- Texto con marcadores `[X]` según `informe.getCorrespondenciaPendiente() == 1`
- Formato: "SI [X] / NO [ ]" o "SI [ ] / NO [X]"

**Sección 6 — Declaración Especial**
- Texto estático completo de la spec

**Sección 7 — Cierre de Aprobación**
- Párrafo con `numeroDesembolso`, `valorDesembolso`, `porcentajeEjecucion`
- Fecha de elaboración: `informe.getFechaAprobacion()` formateada `dd/MM/yyyy`
- Frase "Para constancia se firma..." con fecha en palabras

**Sección 8 — Firmas**
- Tabla 2 o 3 columnas según `firmaRevisor != null` / `contrato.getRevisor() != null`
- Columnas: imagen (si hay), nombre en negrita, cargo, rol (Contratista / Supervisor(a) / Apoyo a la Supervisión)

**Paleta CSS:**

```css
body { font-family: Arial, sans-serif; font-size: 10pt; color: #0b1c30; margin: 30pt; }
.header-institucional { background-color: #002869; color: #ffffff; padding: 8pt; text-align: center; }
.section-title { background-color: #C0C0C0; color: #000000; font-weight: bold; padding: 4pt 6pt; }
th { background-color: #002869; color: #ffffff; padding: 4pt 6pt; text-align: left; font-size: 10pt; }
td { padding: 4pt 6pt; border: 0.5pt solid #ccc; font-size: 10pt; vertical-align: top; }
.highlight { background-color: #eff4ff; }
.firma-img { max-height: 60pt; max-width: 150pt; }
table { width: 100%; border-collapse: collapse; margin-bottom: 8pt; }
```

**Commit:** `feat(i6): redesign InformePdfTemplateService with institutional SED layout`

**Validación:**
- `mvn test -pl sigcon-backend -q` → >= 146 tests, 0 fallos
- Generación manual de un PDF de prueba: verificar que las 8 secciones están presentes y el diseño se corresponde con la referencia visual

---

## T9 — Frontend: Perfil SGSSI + Formulario de Contrato Admin

**Spec:** secciones 5.1, 5.2

**Archivos a crear/modificar:**
- `sigcon-angular/src/app/features/perfil/perfil.component.ts` (y su template)
- `sigcon-angular/src/app/features/admin/contratos/admin-contrato-form.component.ts` (y su template)
- `sigcon-angular/src/app/core/models/usuario.model.ts` ← extender `UsuarioDto`
- `sigcon-angular/src/app/core/models/contrato.model.ts` ← extender `ContratoDetalleDto`
- `sigcon-angular/src/app/core/constants/sed-dependencias.constants.ts` ← nuevo

### 9.1 `PerfilComponent` — campos SGSSI

En el formulario de perfil, agregar grupo "Seguridad Social" con tres `p-inputText`:
- Entidad de salud (EPS) — `sgssiSaludEntidad`
- Fondo de pensión — `sgssiPensionEntidad` (label "(opcional)")
- ARL — `sgssiArlEntidad`

Inicializar desde el usuario actual. El PATCH existente al perfil recoge los nuevos campos porque el modelo ya los incluye.

### 9.2 `sed-dependencias.constants.ts` — catálogo organizacional SED

Crear el archivo con las 44 unidades organizacionales de la SED (árbol completo del spec sección 5.2). Cada entrada: `{ label: string; value: string }` con `label === value`.

```typescript
export const SED_DEPENDENCIAS: { label: string; value: string }[] = [
  { label: 'Despacho de la Secretaría de Educación', value: 'Despacho de la Secretaría de Educación' },
  { label: 'Oficina Asesora de Planeación', value: 'Oficina Asesora de Planeación' },
  // ... (ver spec sección 5.2 para la lista completa de 44 entradas)
];
```

### 9.3 `AdminContratoFormComponent` — campos nuevos

Agregar al final del formulario, antes del bloque de submit:

- **Dependencia** — `p-dropdown` con `[options]="sedDependencias"` `optionLabel="label"` `optionValue="value"` `[filter]="true"` `filterBy="label"` `placeholder="Seleccione la dependencia"` (opcional)
- **Forma de pago** — `p-textarea` (rows: 5, opcional)
- **Modificaciones** — `p-inputText`, valor inicial "No se han presentado"

En el componente TypeScript:

```typescript
readonly sedDependencias = SED_DEPENDENCIAS;
```

Inicializar `modificaciones` con el valor del contrato existente (en modo edición) o con "No se han presentado" (en modo creación si está vacío).

**Commit:** `feat(i6): add SGSSI profile fields, SED dependencias constant, and admin contract form extensions`

**Validación:** `ng build --configuration=development` sin errores.

---

## T10 — Frontend: `AporteSgssiService` Angular

**Spec:** sección 5.4 (servicio Angular)

**Archivo a crear:**
- `sigcon-angular/src/app/core/services/aporte-sgssi.service.ts`

```typescript
@Injectable({ providedIn: 'root' })
export class AporteSgssiService {

  private readonly http = inject(HttpClient);

  listarAportes(informeId: number): Observable<AporteSgssiDto[]> {
    return this.http.get<AporteSgssiDto[]>(`/api/informes/${informeId}/aportes-sgssi`);
  }

  guardarAportes(informeId: number, aportes: AporteSgssiRequest[]): Observable<AporteSgssiDto[]> {
    return this.http.put<AporteSgssiDto[]>(`/api/informes/${informeId}/aportes-sgssi`, aportes);
  }
}
```

**Archivos a crear:**
- `sigcon-angular/src/app/core/models/aporte-sgssi.model.ts`

```typescript
export type ItemSgssi = 'SALUD' | 'PENSION' | 'ARL';

export interface AporteSgssiDto {
  id: number;
  item: ItemSgssi;
  periodoSgssi: string;
  fechaPago: string;       // ISO date "yyyy-MM-dd"
  valorAportado: number;
  entidad: string;
}

export interface AporteSgssiRequest {
  item: ItemSgssi;
  fechaPago: string;
  valorAportado: number;
  entidad: string;
}
```

**Commit:** `feat(i6): add AporteSgssiService and models to Angular core`

**Validación:** `ng build --configuration=development` sin errores.

---

## T11 — Frontend: `InformeFormComponent` (creación)

**Spec:** sección 5.3, 5.4

**Archivos a modificar:**
- `sigcon-angular/src/app/features/informes/form/informe-form.component.ts`
- `sigcon-angular/src/app/core/models/informe.model.ts` ← extender `InformeRequest` y `InformeDetalleDto`

### 11.1 Extender el modelo `InformeRequest`

```typescript
numeroDesembolso?: number;
valorDesembolso?: number;
porcentajeEjecucion?: number;
correspondenciaPendiente?: boolean;
aportesSgssi?: AporteSgssiRequest[];
```

### 11.2 Campos del encabezado

Debajo del selector de período, agregar:
- Número de desembolso — `p-inputNumber` (mode: 'decimal', minFractionDigits: 0, maxFractionDigits: 0)
- Valor del desembolso — `p-inputNumber` (mode: 'currency', currency: 'COP', locale: 'es-CO')
- Porcentaje de ejecución — `p-inputNumber` (suffix: ' %', min: 0, max: 100, maxFractionDigits: 2)
- ¿Hay correspondencia pendiente? — `p-checkbox`

### 11.3 Sección SGSSI

Agregar sección "Aportes al Sistema de Seguridad Social" con una tabla reactiva:

```typescript
interface AporteSgssiRow {
  item: ItemSgssi;
  fechaPago: Date | null;
  valorAportado: number | null;
  entidad: string;
  editable: boolean;  // siempre true en el form de creación
}
```

Signal `aportesSgssiRows = signal<AporteSgssiRow[]>([...])`.

Inicializar con 2 filas fijas (SALUD, ARL) + 0 o 1 fila PENSIÓN. Pre-rellenar `entidad` desde `usuarioActual.sgssiSaludEntidad`, `sgssiArlEntidad`.

Botón "Agregar PENSIÓN" visible solo cuando no hay fila PENSIÓN. Botón "×" en la fila PENSIÓN para quitarla.

La columna PERÍODO PAGO se calcula del `fechaInicio` del formulario (reactivamente) y se muestra como texto estático.

Al enviar el formulario, mapear `aportesSgssiRows` a `AporteSgssiRequest[]` y incluirlos en `InformeRequest`.

**Commit:** `feat(i6): extend InformeFormComponent with desembolso fields and SGSSI section`

**Validación:** `ng build --configuration=development` sin errores; revisión visual del formulario de creación.

---

## T12 — Frontend: `InformeDetalleComponent` + Tests + Docs

**Spec:** secciones 5.5, 7 (frontend), 8

**Archivos a modificar:**
- `sigcon-angular/src/app/features/informes/detalle/informe-detalle.component.ts`
- `sigcon-angular/src/app/features/informes/detalle/informe-detalle.component.spec.ts`
- `docs/GUIA_PRUEBAS_FUNCIONALES.md`

### 12.1 Campos nuevos del encabezado en detalle

Cuando `estado === 'BORRADOR' || estado === 'DEVUELTO'`:

```
Número de desembolso:    [p-inputNumber]
Valor del desembolso:    [p-inputNumber]
Porcentaje de ejecución: [p-inputNumber]
¿Hay correspondencia pendiente? [p-checkbox]
[Guardar datos del informe]
```

El botón llama `PATCH /api/informes/{id}` con los cuatro campos nuevos.

Cuando el informe está en otro estado: mostrar en solo lectura.

### 12.2 Eliminar porcentaje por actividad (regresión I5)

En `InformeDetalleComponent`, eliminar el campo `porcentaje` del estado local de edición:

```typescript
// Eliminar de ActividadEditState:
//   porcentaje: number;

// Eliminar del template (tarjeta editable):
//   <p-inputNumber [ngModel]="state.porcentaje" ...>
//   <label>Avance %</label>

// Eliminar de guardarActividad():
//   validación if (state.porcentaje < 0 || state.porcentaje > 100)
//   del objeto request: porcentaje: state.porcentaje
```

El request a `ActividadInformeService.actualizar()` pasa a enviar solo `{ idObligacion, descripcion }` (sin `porcentaje`).

### 12.3 Sección SGSSI en detalle

Cuando `estado === 'BORRADOR' || estado === 'DEVUELTO'`:
- Mostrar la misma tabla editable que en `InformeFormComponent` (SALUD + ARL fijas, PENSIÓN opcional).
- Cargar aportes existentes con `AporteSgssiService.listarAportes(informeId)` al inicializar.
- Botón "Guardar aportes SGSSI" llama `PUT /api/informes/{id}/aportes-sgssi`.

Cuando el informe está en otro estado: tabla solo lectura con los valores del `InformeDetalleDto.aportesSgssi`.

### 12.4 Tests a agregar en `informe-detalle.component.spec.ts`

```
describe('I6 - Datos complementarios', () => {
  it('InformeFormComponent muestra sección SGSSI con 3 filas')
  it('InformeFormComponent pre-rellena entidades desde perfil')
  it('InformeFormComponent permite quitar PENSIÓN')
  it('InformeDetalleComponent muestra SGSSI editable en BORRADOR')
  it('InformeDetalleComponent muestra SGSSI solo lectura en ENVIADO')
  it('guardarAportesSgssi llama PUT correctamente')
  it('PerfilComponent muestra campos SGSSI')
  it('AdminContratoFormComponent muestra dependencia como dropdown SED')
  it('AdminContratoFormComponent muestra formaPago como textarea')
})

describe('I6 - Regresión porcentaje actividades', () => {
  it('InformeDetalleComponent NO muestra input de porcentaje en tarjeta de actividad en BORRADOR')
  it('InformeDetalleComponent guarda actividad sin campo porcentaje en el request')
  // Actualizar: reemplazar 'muestra error inline si porcentaje es inválido' por test de solo descripcion
})
```

Mocks adicionales requeridos:

```typescript
aporteSgssiService = jasmine.createSpyObj<AporteSgssiService>(
    'AporteSgssiService', ['listarAportes', 'guardarAportes']);
```

### 12.4 Guía de pruebas funcionales

Agregar sección 14 en `docs/GUIA_PRUEBAS_FUNCIONALES.md`:

```markdown
## Sección 14 — I6: PDF Institucional + Datos Complementarios

### Escenario 14.1 — Configurar entidades SGSSI en perfil
1. Autenticarse como CONTRATISTA.
2. Ir a Perfil → completar EPS, Fondo de Pensión (opcional), ARL.
3. Guardar. Verificar que se persisten.

### Escenario 14.2 — Crear informe con aportes SGSSI
1. Crear nuevo informe. Verificar que la sección SGSSI aparece.
2. Las entidades de SALUD y ARL deben venir pre-rellenas del perfil.
3. Ingresar fecha de pago y valor para cada ítem.
4. Agregar PENSIÓN si aplica.
5. Enviar el informe. Verificar creación exitosa.

### Escenario 14.3 — Editar datos complementarios en BORRADOR
1. Ir a detalle de informe en BORRADOR.
2. Ingresar número de desembolso, valor, porcentaje de ejecución.
3. Marcar / desmarcar correspondencia pendiente.
4. Guardar. Verificar persistencia.

### Escenario 14.4 — Aprobar informe y verificar PDF
1. Como SUPERVISOR, aprobar el informe.
2. Descargar el PDF generado.
3. Verificar que el PDF contiene:
   - Encabezado azul SED (#002869) con nombre de entidad, contrato e informe
   - Sección DATOS DEL CONTRATO con valor en letras
   - Tabla EJECUCIÓN DE ACTIVIDADES con evidencia verificable
   - Tabla APORTES SGSSI con período, fecha, valor y entidad
   - Sección ESTADO DE RADICACIÓN con SI/NO correcto
   - DECLARACIÓN ESPECIAL (texto estático)
   - CIERRE DE APROBACIÓN con número de desembolso, valor y porcentaje
   - FIRMAS: contratista, supervisor y (si aplica) apoyo a la supervisión

### Diagnóstico de errores comunes I6

| Síntoma | Causa probable | Solución |
|---------|---------------|----------|
| 409 al guardar aportes SGSSI | Informe no en BORRADOR/DEVUELTO | Recargar la página |
| 400 al guardar aportes SGSSI | Dos ítems con el mismo tipo | Verificar que no hay duplicados en la tabla |
| PDF sin sección SGSSI | No se registraron aportes | Ir al informe y agregar aportes SGSSI |
| Valor en letras incorrecto | Monto con decimales inusuales | El converter usa la parte entera; centavos se ignoran |
| Firma de revisor ausente en PDF | Revisor sin imagen de firma registrada | El revisor debe cargar su firma en el perfil |
```

**Commit:** `feat(i6): informe detalle SGSSI section, remove activity porcentaje, 16 new frontend specs and functional guide`

**Validación final:**
- `ng test --include="**/informe-detalle*" --include="**/informe-form*" --include="**/perfil*" --include="**/admin-contrato*"` → >= 106 specs, 0 fallos
- `ng build` → sin errores
- Revisión manual del flujo completo: perfil → contrato → informe → aprobación → PDF

---

## Orden de Ejecución y Dependencias

```
T1 (rama + DDL)
  └─ T2 (dominio: entidades + repository)
       └─ T3 (NumeroPesosConverter)
            └─ T4 (DTOs)
                 └─ T5 (servicios)
                      └─ T6 (controlador)
                           └─ T7 (tests backend)
                                └─ T8 (PDF redesign)
                                     └─ T9 (frontend perfil + admin contrato)
                                          └─ T10 (Angular AporteSgssiService)
                                               └─ T11 (InformeFormComponent)
                                                    └─ T12 (InformeDetalleComponent + tests + docs)
```

Las tareas T2, T3, T4 pueden ejecutarse en paralelo una vez T1 complete. T5 requiere T2 + T4. T6 requiere T5. T7 requiere T6. T8 requiere T7 (para que los tests previos validen el backend antes de reescribir el template). T9–T12 pueden iniciarse en paralelo a T7–T8, pero T11 y T12 requieren T10.

---

## Métricas de Cierre

| Métrica | Meta |
|---------|------|
| Backend tests | >= 148 (123 base + >= 25 nuevos), 0 fallos |
| Frontend specs | >= 106 (90 base + >= 16 nuevos), 0 fallos |
| Regresión PDF (`PdfInformeServiceTest`) | Adaptado al nuevo contrato de `generarPdf`, 0 fallos |
| Regresión I5 actividades | Test porcentaje por actividad eliminado/actualizado, 0 fallos |
| DDL scripts | `db/00_setup.sql` contiene bloque I6 completo; columna `PORCENTAJE` sin DROP |
| Endpoints nuevos | 2 (`GET` y `PUT /api/informes/{id}/aportes-sgssi`) |
| Endpoints modificados | 1 (`PUT actividades/{actId}` — sin campo `porcentaje`) |
| Catálogo dependencias | `sed-dependencias.constants.ts` con 44 unidades organizacionales SED |
| Secciones PDF | 8 secciones institucionales presentes en el PDF generado |
| Commits | 12 commits (uno por tarea T1–T12) |
