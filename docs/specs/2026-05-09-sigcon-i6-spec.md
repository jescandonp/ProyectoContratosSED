# Spec Técnica — SIGCON Incremento 6
## Rediseño PDF Institucional + Datos Complementarios del Informe

> **Metodología:** Spec-Driven Development (SDD) — Spec-Anchored
> **Versión:** 1.0 — **Fecha:** 2026-05-09
> **Constitución:** `docs/CONSTITUTION.md`
> **Arquitectura:** `docs/ARCHITECTURE.md`
> **PRD de referencia:** `docs/specs/2026-04-30-sigcon-prd.md`
> **Specs base:** I1–I5 completados (HEAD `feat/sigcon-i5`)
> **Formato institucional de referencia:** `Notas_ProyectoContratos/06_Informe_actividades_06_Abril_2026_Juan_Escandon - Formato.docx`
> **Feature name:** `pdf-rediseno-datos-complementarios`
> **Estado:** Listo para implementación

---

## 1. Alcance del Incremento

### 1.1 Problema que resuelve

El PDF generado actualmente (I3) no se corresponde con el formato institucional oficial de la SED. Faltan secciones enteras requeridas por el formato de referencia (`06_Informe_actividades_06_Abril_2026_Juan_Escandon - Formato.docx`):

- **DATOS DEL CONTRATO** incompleto: falta Dependencia, Forma de Pago, Modificaciones y Valor en letras.
- **APORTES AL SISTEMA DE SEGURIDAD SOCIAL INTEGRAL** (SGSSI): sección completa ausente. El contratista debe declarar sus aportes de SALUD, PENSIÓN y ARL por el período del informe.
- **ESTADO DE RADICACIÓN DE CORRESPONDENCIA**: sección ausente.
- **DECLARACIÓN ESPECIAL**: sección ausente.
- **Cierre de aprobación**: faltan número de desembolso, valor del desembolso y porcentaje de ejecución del contrato.
- **Firma del Revisor** (Apoyo a la Supervisión): ausente en el layout de firmas.
- **Diseño visual**: no corresponde al design system SED (`Prototipo/DESIGN.md`).

### 1.2 Módulos incluidos

| Módulo | Descripción | Rol principal |
|--------|-------------|---------------|
| DDL — `SGCN_CONTRATOS` | Nuevos campos: `DEPENDENCIA`, `FORMA_PAGO`, `MODIFICACIONES` | ADMIN |
| DDL — `SGCN_INFORMES` | Nuevos campos: `NUMERO_DESEMBOLSO`, `VALOR_DESEMBOLSO`, `PORCENTAJE_EJECUCION`, `CORRESPONDENCIA_PENDIENTE` | CONTRATISTA |
| DDL — `SGCN_USUARIOS` | Nuevos campos: entidades SGSSI del perfil del contratista | CONTRATISTA |
| DDL — `SGCN_APORTES_SGSSI` | Nueva tabla para aportes de seguridad social por informe | CONTRATISTA |
| Backend — Dominio/Aplicación | Entidades, DTOs, servicios y mappers para los nuevos datos | Sistema |
| Backend — `NumeroPesosConverter` | Utilidad Java 8 para convertir BigDecimal a texto en letras (pesos colombianos) | Sistema |
| Backend — `InformePdfTemplateService` | Rediseño completo del template XHTML con el layout institucional SED | Sistema |
| Backend — Actividades: eliminar `porcentaje` por actividad | El campo `PORCENTAJE` de `SGCN_ACTIVIDADES_INFORME` deja de exponerse; el % de ejecución se registra a nivel de informe (`PORCENTAJE_EJECUCION`) | Sistema |
| Frontend — Perfil contratista | Campos de entidades SGSSI en la pantalla de perfil | CONTRATISTA |
| Frontend — Admin contrato | Dependencia (selector árbol SED), Forma de Pago, Modificaciones en formulario de contrato | ADMIN |
| Frontend — Formulario de informe | Nuevos campos: número desembolso, valor desembolso, porcentaje ejecución (informe vs. contrato), correspondencia pendiente, aportes SGSSI | CONTRATISTA |
| Frontend — Detalle informe (I5 regresión) | Eliminar campo `porcentaje` del estado de edición por actividad; quitar input y validación | CONTRATISTA |

### 1.3 Fuera de este incremento

- Cambios en la máquina de estados del informe.
- Nuevos roles de usuario.
- Cambios en el sistema de notificaciones.
- Integración SECOP2.
- Firma digital criptográfica avanzada.
- Validación de que el contratista haya rellenado todos los campos obligatorios antes de enviar (fuera de alcance; el PDF tolera valores nulos mostrando celda vacía).
- Gestión de modificaciones contractuales como entidad propia (el campo `modificaciones` es texto libre).
- Motor de pagos.

### 1.4 Entregable de cierre

El PDF generado al aprobar un informe reproduce fielmente el formato institucional SED: incluye todos los datos del contrato (con valor en letras autogenerado), la tabla de actividades con evidencia verificable, la sección de aportes SGSSI, el estado de radicación de correspondencia, la declaración especial, el cierre de aprobación con número de desembolso / valor / porcentaje de ejecución, y el bloque de firmas con contratista, supervisor y (si aplica) apoyo a la supervisión.

---

## 2. Coherencia con Incrementos Anteriores

- El flujo de estados del informe **no cambia**. El PDF sigue generándose al momento de la aprobación final del supervisor.
- `PdfInformeService.generarYPersistir()` **no cambia** su lógica de control (inmutabilidad, validación de firmas, hash SHA-256, almacenamiento). Solo cambia el template que produce.
- El perfil de usuario ya soporta `firmaImagen`. I6 agrega campos SGSSI sin modificar la lógica existente de firma.
- `PATCH /api/informes/{id}` (I4) sigue funcionando para `fechaInicio`/`fechaFin`. Se **extiende** con los nuevos campos del informe.
- El flujo sin revisor (I4) aplica al PDF: cuando `contrato.revisor == null`, se omite el bloque de Apoyo a la Supervisión.
- La edición inline de actividades (I5) **se ajusta**: se elimina el campo `porcentaje` de la tarjeta de edición por actividad. El campo `SGCN_ACTIVIDADES_INFORME.PORCENTAJE` permanece en DDL (sin DROP) pero deja de exponerse en la API ni en la interfaz. La columna puede eliminarse en un migration de limpieza posterior.
- El porcentaje de ejecución del contrato se captura ahora a nivel de informe (`PORCENTAJE_EJECUCION` en `SGCN_INFORMES`) y representa el avance global del contrato a la fecha, no por actividad individual.
- Los tests existentes no se rompen: `InformePdfTemplateService` es un servicio de aplicación llamado solo desde `PdfInformeService`; sus tests actuales en `PdfInformeServiceTest` se adaptan al nuevo contrato del método `generarPdf`. Los tests de I5 que validan el campo porcentaje por actividad se actualizan en I6.

---

## 3. Base de Datos — DDL Incremento 6

### 3.1 Ajustes a `SGCN_CONTRATOS`

```sql
ALTER TABLE SGCN_CONTRATOS ADD (
    DEPENDENCIA     VARCHAR2(500),
    FORMA_PAGO      CLOB,
    MODIFICACIONES  VARCHAR2(2000)  DEFAULT 'No se han presentado'
);
```

- `DEPENDENCIA`: unidad organizacional de la SED (`VARCHAR2(500)`). Se selecciona desde un catálogo fijo de 44 dependencias (árbol organizacional SED). El valor almacenado es el nombre textual de la dependencia. Ver sección 5.2 para el catálogo completo.
- `FORMA_PAGO`: texto libre con la cláusula de pago del contrato. Puede ser NULL; si es NULL el PDF omite la fila.
- `MODIFICACIONES`: texto libre con modificaciones al contrato. Default "No se han presentado". El admin puede actualizarlo.

### 3.2 Ajustes a `SGCN_INFORMES`

```sql
ALTER TABLE SGCN_INFORMES ADD (
    NUMERO_DESEMBOLSO       NUMBER(5),
    VALOR_DESEMBOLSO        NUMBER(14,2),
    PORCENTAJE_EJECUCION    NUMBER(5,2),
    CORRESPONDENCIA_PENDIENTE NUMBER(1) DEFAULT 0 NOT NULL
);
```

- `NUMERO_DESEMBOLSO`: número secuencial del pago/desembolso para este informe (ej. 6). Lo ingresa el contratista.
- `VALOR_DESEMBOLSO`: valor en pesos del desembolso (ej. 10000000). Lo ingresa el contratista.
- `PORCENTAJE_EJECUCION`: porcentaje de ejecución del contrato a la fecha del informe (ej. 49.44). Lo ingresa el contratista.
- `CORRESPONDENCIA_PENDIENTE`: 1 = hay correspondencia pendiente, 0 = no hay. Default 0.

### 3.3 Ajustes a `SGCN_USUARIOS`

```sql
ALTER TABLE SGCN_USUARIOS ADD (
    SGSSI_SALUD_ENTIDAD     VARCHAR2(200),
    SGSSI_PENSION_ENTIDAD   VARCHAR2(200),
    SGSSI_ARL_ENTIDAD       VARCHAR2(200)
);
```

Estos campos persisten la EPS, fondo de pensión y ARL del contratista, configurados en su perfil una sola vez y reutilizados como valores por defecto al capturar aportes en cada informe.

### 3.4 Nueva tabla `SGCN_APORTES_SGSSI`

```sql
CREATE SEQUENCE SGCN_APORTES_SGSSI_SEQ START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE TABLE SGCN_APORTES_SGSSI (
    ID              NUMBER          DEFAULT SGCN_APORTES_SGSSI_SEQ.NEXTVAL PRIMARY KEY,
    ID_INFORME      NUMBER          NOT NULL,
    ITEM            VARCHAR2(20)    NOT NULL,   -- SALUD | PENSION | ARL
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

**Regla de negocio:** El período de pago (`mm/aaaa`) **no se almacena**. Se deriva en tiempo de ejecución como el mes anterior a `informe.fechaInicio`:

```
periodoSgssi = YearMonth.from(informe.getFechaInicio()).minusMonths(1)
             → formato: String.format("%02d/%d", mes, año)
```

**Ítems:** El contratista registra SALUD y ARL siempre; PENSIÓN es opcional (contratistas exentos de aportes a pensión). La tabla puede tener 2 o 3 filas por informe.

### 3.5 Depreciación de `PORCENTAJE` en `SGCN_ACTIVIDADES_INFORME`

**Sin cambio de DDL** en este incremento. La columna `PORCENTAJE` de `SGCN_ACTIVIDADES_INFORME` (agregada en I2) permanece en el schema pero **deja de utilizarse**:

- El backend deja de leer o escribir ese campo en la entidad `ActividadInforme` expuesta por la API.
- El frontend elimina el input de porcentaje de las tarjetas de edición de actividades.
- El PDF no muestra "Avance (%)" por actividad (el nuevo layout de 3 columnas ya no lo incluía).
- El porcentaje de ejecución del contrato pasa a registrarse en `SGCN_INFORMES.PORCENTAJE_EJECUCION` (sección 3.2), que representa el avance global del contrato a la fecha del informe. Es el contratista quien lo ingresa.

> La columna `PORCENTAJE` puede eliminarse mediante un migration de limpieza en un incremento posterior cuando se confirme que ningún entorno tiene dependencia sobre ella.

---

## 4. Backend

### 4.1 Dominio — Nuevas entidades y ajustes

#### Nuevo enum `ItemSgssi`

```java
// domain/enums/ItemSgssi.java
public enum ItemSgssi {
    SALUD, PENSION, ARL
}
```

#### Nueva entidad `AporteSgssi`

```java
// domain/entity/AporteSgssi.java
@Entity
@Table(name = "SGCN_APORTES_SGSSI")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class AporteSgssi {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SGCN_APORTES_SGSSI_SEQ")
    @SequenceGenerator(name = "SGCN_APORTES_SGSSI_SEQ",
                       sequenceName = "SGCN_APORTES_SGSSI_SEQ", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_INFORME", nullable = false)
    private Informe informe;

    @Enumerated(EnumType.STRING)
    @Column(name = "ITEM", nullable = false, length = 20)
    private ItemSgssi item;

    @Column(name = "FECHA_PAGO", nullable = false)
    private LocalDate fechaPago;

    @Column(name = "VALOR_APORTADO", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorAportado;

    @Column(name = "ENTIDAD", nullable = false, length = 200)
    private String entidad;

    @Column(name = "ACTIVO", nullable = false)
    private Integer activo = 1;

    @CreatedDate
    @Column(name = "CREATED_AT", updatable = false)
    private LocalDateTime createdAt;

    @CreatedBy
    @Column(name = "CREATED_BY", updatable = false)
    private String createdBy;

    @LastModifiedDate
    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;
}
```

#### Ajustes a `Contrato`

Agregar campos mapeados a las nuevas columnas:

```java
@Column(name = "DEPENDENCIA", length = 500)
private String dependencia;

@Column(name = "FORMA_PAGO", columnDefinition = "CLOB")
private String formaPago;

@Column(name = "MODIFICACIONES", length = 2000)
private String modificaciones;
```

#### Ajustes a `Informe`

```java
@Column(name = "NUMERO_DESEMBOLSO")
private Integer numeroDesembolso;

@Column(name = "VALOR_DESEMBOLSO", precision = 14, scale = 2)
private BigDecimal valorDesembolso;

@Column(name = "PORCENTAJE_EJECUCION", precision = 5, scale = 2)
private BigDecimal porcentajeEjecucion;

@Column(name = "CORRESPONDENCIA_PENDIENTE", nullable = false)
private Integer correspondenciaPendiente = 0;
```

#### Ajustes a `Usuario`

```java
@Column(name = "SGSSI_SALUD_ENTIDAD", length = 200)
private String sgssiSaludEntidad;

@Column(name = "SGSSI_PENSION_ENTIDAD", length = 200)
private String sgssiPensionEntidad;

@Column(name = "SGSSI_ARL_ENTIDAD", length = 200)
private String sgssiArlEntidad;
```

#### Nuevo repository `AporteSgssiRepository`

```java
// domain/repository/AporteSgssiRepository.java
public interface AporteSgssiRepository extends JpaRepository<AporteSgssi, Long> {
    List<AporteSgssi> findByInformeIdAndActivoTrue(Long informeId);
    void deleteByInformeId(Long informeId);
}
```

#### Ajuste a `ActividadInforme` — eliminar exposición del campo `porcentaje`

El campo `porcentaje` de la entidad `ActividadInforme` **permanece mapeado** (`@Column(name = "PORCENTAJE")`) para evitar errores de Hibernate, pero deja de incluirse en los DTOs de respuesta (`ActividadInformeDto`) y en el request de actualización (`ActividadInformeRequest`). El valor almacenado en la columna no se lee ni se escribe a partir de I6.

### 4.2 DTOs nuevos y ajustados

#### `AporteSgssiRequest`

```java
public class AporteSgssiRequest {
    @NotNull
    private ItemSgssi item;          // SALUD | PENSION | ARL

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

#### `AporteSgssiDto`

```java
public class AporteSgssiDto {
    private Long id;
    private ItemSgssi item;
    private String periodoSgssi;  // derivado: "mm/aaaa" del mes anterior al informe
    private LocalDate fechaPago;
    private BigDecimal valorAportado;
    private String entidad;
}
```

#### Extensión de `ContratoRequest` / `ContratoDetalleDto`

Agregar campos en `ContratoRequest` (todos opcionales):

```java
@Size(max = 500)
private String dependencia;

private String formaPago;          // texto libre, CLOB

@Size(max = 2000)
private String modificaciones;
```

Agregar los mismos campos en `ContratoDetalleDto` para su lectura.

#### Extensión de `InformeRequest`

```java
private Integer numeroDesembolso;

@DecimalMin(value = "0.00")
private BigDecimal valorDesembolso;

@DecimalMin(value = "0.00")
@DecimalMax(value = "100.00")
private BigDecimal porcentajeEjecucion;

private Boolean correspondenciaPendiente;   // default false si null

private List<AporteSgssiRequest> aportesSgssi;
```

#### Extensión de `InformeUpdateDto`

Mismos campos nuevos que `InformeRequest` (todos opcionales en el PATCH):

```java
private Integer numeroDesembolso;
private BigDecimal valorDesembolso;
private BigDecimal porcentajeEjecucion;
private Boolean correspondenciaPendiente;
private List<AporteSgssiRequest> aportesSgssi;
```

#### Extensión de `InformeDetalleDto`

```java
private Integer numeroDesembolso;
private BigDecimal valorDesembolso;
private BigDecimal porcentajeEjecucion;
private Boolean correspondenciaPendiente;
private List<AporteSgssiDto> aportesSgssi;
```

#### Extensión de `PerfilUpdateRequest`

```java
@Size(max = 200)
private String sgssiSaludEntidad;

@Size(max = 200)
private String sgssiPensionEntidad;

@Size(max = 200)
private String sgssiArlEntidad;
```

#### Extensión de `UsuarioDto`

```java
private String sgssiSaludEntidad;
private String sgssiPensionEntidad;
private String sgssiArlEntidad;
```

#### Ajuste a `ActividadInformeRequest` y `ActividadInformeDto`

Eliminar el campo `porcentaje` de ambas clases. El request de `PUT /api/informes/{id}/actividades/{actId}` ya no acepta ni retorna ese campo. El backend en `ActividadInformeService.actualizar()` deja de asignar el valor de porcentaje.

### 4.3 Servicios

#### `AporteSgssiService` (nuevo)

```
Reglas:
  - Solo se ejecuta cuando informe.estado == BORRADOR o DEVUELTO.
  - Si no se cumple la condición: SigconBusinessException(INFORME_NO_EDITABLE, 409).
  - Validar que contratistaEmail == informe.contrato.contratista.email; si no: ACCESO_DENEGADO 403.
  - La operación reemplaza todos los aportes activos del informe (delete lógico + insert).
  - Ítems aceptados: SALUD, ARL (obligatorios) y PENSION (opcional).
  - No puede haber dos ítems con el mismo ItemSgssi en la misma llamada: validar unicidad.

guardarAportes(informeId, List<AporteSgssiRequest>, contratistaEmail):
  1. Cargar informe; si no existe → 404.
  2. Validar estado BORRADOR o DEVUELTO.
  3. Validar propiedad (contratistaEmail).
  4. Validar unicidad de ítems en la lista.
  5. Marcar como inactivos (activo = 0) todos los aportes previos del informe.
  6. Persistir los nuevos aportes con activo = 1.
  7. Retornar List<AporteSgssiDto> con el periodoSgssi derivado.

listarAportes(informeId):
  1. Retornar findByInformeIdAndActivoTrue(informeId) mapeados a AporteSgssiDto.
  2. periodoSgssi = YearMonth.from(informe.getFechaInicio()).minusMonths(1),
     formateado como String.format("%02d/%d", mes, año).
```

#### Extensión de `InformeService.crear()`

Cuando `dto.getAportesSgssi()` no es null y no está vacío:

```
1. Crear el informe normalmente (código existente).
2. Si dto.getAportesSgssi() != null: llamar aporteSgssiService.guardarAportes(
       informe.getId(), dto.getAportesSgssi(), contratistaEmail).
3. Si dto.getNumeroDesembolso() != null: informe.setNumeroDesembolso(...)
4. Si dto.getValorDesembolso() != null: informe.setValorDesembolso(...)
5. Si dto.getPorcentajeEjecucion() != null: informe.setPorcentajeEjecucion(...)
6. correspondenciaPendiente: si null → false; si no null → usar el valor.
7. informeRepository.save(informe) — ya se hace en el flujo actual.
```

#### Extensión de `InformeService.actualizar()` (PATCH /api/informes/{id})

Aplica los mismos campos nuevos cuando estén presentes (no-null) en `InformeUpdateDto`:

```
Si dto.getNumeroDesembolso() != null → informe.setNumeroDesembolso(...)
Si dto.getValorDesembolso() != null  → informe.setValorDesembolso(...)
Si dto.getPorcentajeEjecucion() != null → informe.setPorcentajeEjecucion(...)
Si dto.getCorrespondenciaPendiente() != null → informe.setCorrespondenciaPendiente(...)
Si dto.getAportesSgssi() != null → aporteSgssiService.guardarAportes(...)
```

La validación de estado (BORRADOR o DEVUELTO) y propiedad ya está implementada.

#### Extensión de `ContratoService`

`crear(dto)` y `actualizar(id, dto)` copian los nuevos campos opcionales:

```
contrato.setDependencia(dto.getDependencia());
contrato.setFormaPago(dto.getFormaPago());
contrato.setModificaciones(dto.getModificaciones() != null
    ? dto.getModificaciones() : "No se han presentado");
```

#### Extensión de `UsuarioService` (perfil)

`actualizarPerfil(email, dto)` persiste los campos SGSSI cuando están presentes en el request.

#### Nuevo `NumeroPesosConverter` (utilidad)

```java
// application/service/NumeroPesosConverter.java
public class NumeroPesosConverter {

    /**
     * Convierte un BigDecimal de pesos colombianos a texto en mayúsculas.
     * Ejemplo: 118666667 → "CIENTO DIECIOCHO MILLONES SEISCIENTOS SESENTA Y SEIS
     *                        MIL SEISCIENTOS SESENTA Y SIETE PESOS M/CTE"
     * Compatible con Java 8. Soporta hasta 999.999.999.999.
     */
    public static String convertir(BigDecimal monto) { ... }
}
```

El método trabaja con la parte entera del monto (sin centavos), usando arreglos de unidades, decenas y grupos convencionales en español colombiano. Se añade el sufijo " PESOS M/CTE" al resultado.

**Casos especiales:**
- `0` → "CERO PESOS M/CTE"
- `1.000.000` → "UN MILLÓN PESOS M/CTE"
- `1.000.000.000` → "MIL MILLONES PESOS M/CTE"

### 4.4 Controladores y endpoints

#### Nuevo `AporteSgssiController`

```java
@RestController
@RequestMapping("/api/informes/{informeId}/aportes-sgssi")
@Tag(name = "Aportes SGSSI", description = "Aportes de seguridad social por informe")
public class AporteSgssiController {

    @Operation(summary = "Listar aportes SGSSI del informe")
    @GetMapping
    @PreAuthorize("hasAnyRole('CONTRATISTA','REVISOR','SUPERVISOR','ADMIN')")
    public ResponseEntity<List<AporteSgssiDto>> listar(@PathVariable Long informeId) { ... }

    @Operation(summary = "Guardar aportes SGSSI (reemplaza todos)")
    @PutMapping
    @PreAuthorize("hasRole('CONTRATISTA')")
    public ResponseEntity<List<AporteSgssiDto>> guardar(
            @PathVariable Long informeId,
            @Valid @RequestBody List<AporteSgssiRequest> request,
            Authentication auth) { ... }
}
```

#### Endpoint de perfil SGSSI

El endpoint existente `PATCH /api/usuarios/perfil` recibe el `PerfilUpdateRequest` extendido. No se requiere nuevo controlador.

#### Ajustes en `ContratoController` / `AdminContratoController`

Los DTOs de creación y actualización de contrato ya se usan en los controladores existentes. Con los campos extendidos en `ContratoRequest`/`ContratoDetalleDto` los controladores no necesitan cambios de firma.

### 4.5 Seguridad

| Endpoint | Método | Autorización |
|----------|--------|--------------|
| `/api/informes/{id}/aportes-sgssi` | GET | `CONTRATISTA`, `REVISOR`, `SUPERVISOR`, `ADMIN` |
| `/api/informes/{id}/aportes-sgssi` | PUT | `CONTRATISTA` (propietario del informe) |

La regla `.antMatchers("/api/informes/**")` ya existe en `SecurityConfig` y `DevSecurityConfig`. No se requieren cambios de configuración.

### 4.6 Rediseño de `InformePdfTemplateService`

Se reemplaza **completamente** el método `buildHtml()`. El contrato público `generarPdf(informe, firmaContratista, firmaSupervisor, firmaRevisor)` añade el parámetro `firmaRevisor` (puede ser `null` si no hay revisor).

`PdfInformeService.generarYPersistir()` carga los bytes de la firma del revisor si `informe.getContrato().getRevisor() != null` y tiene `firmaImagen`. Si el revisor no tiene firma registrada, pasa `null` y la sección "Apoyo a la Supervisión" se imprime sin imagen.

#### Estructura del nuevo HTML institucional

```
SECCIÓN 1: ENCABEZADO
─────────────────────────────────────────────────────────────────
SECRETARÍA DE EDUCACIÓN DEL DISTRITO
Subdirección de Gestión Contractual
INFORME DE ACTIVIDADES
Informe No. [informe.numero] — Contrato No. [contrato.numero]
─────────────────────────────────────────────────────────────────

SECCIÓN 2: DATOS DEL CONTRATO  (encabezado gris #C0C0C0, texto negro negrita)
  Contratista:          [contratista.nombre]
  Objeto:               [contrato.objeto]
  Valor del Contrato:   El valor del contrato es de [MONTO EN LETRAS]
                        ([$ monto numérico])
  Forma de Pago:        [contrato.formaPago]            (omitir fila si null)
  Plazo:                Desde el [contrato.fechaInicio] hasta el [contrato.fechaFin]
  Modificaciones:       [contrato.modificaciones]
  Fecha de Inicio:      [contrato.fechaInicio]
  Fecha de Terminación: [contrato.fechaFin]
  Dependencia:          [contrato.dependencia]          (omitir fila si null)
  Supervisor — Cargo:   [supervisor.nombre] – [supervisor.cargo]

SECCIÓN 3: EJECUCIÓN DE ACTIVIDADES FRENTE A LAS OBLIGACIONES
           DURANTE EL PERÍODO REPORTADO
  Periodo: Desde [informe.fechaInicio] hasta [informe.fechaFin]
  Tabla (3 columnas):
    | Obligación Contractual | Actividades realizadas | Evidencia Verificable |
    | [obligacion.descripcion] | [actividad.descripcion] | [lista soporte.nombre] |
    → una fila por actividad; columna Evidencia = nombres de soportes adjuntos

SECCIÓN 4: RELACIÓN DEL PAGO DE APORTES AL SISTEMA DE SEGURIDAD SOCIAL INTEGRAL
  Tabla (5 columnas):
    | ITEM | PERÍODO PAGO | FECHA DE PAGO | VALOR APORTADO | ENTIDAD |
    | SALUD | [mm/aaaa] | [dd/mm/aaaa] | [valor] | [entidad] |
    | PENSIÓN | [mm/aaaa] | [dd/mm/aaaa] | [valor] | [entidad] | (omitir si no existe)
    | ARL | [mm/aaaa] | [dd/mm/aaaa] | [valor] | [entidad] |
  * Período = mes anterior a informe.fechaInicio, formato mm/aaaa
  * Si no hay aportes registrados: mostrar fila "Sin aportes registrados"

SECCIÓN 5: ESTADO RADICACIÓN DE LA CORRESPONDENCIA
  Texto (estático con variable SI/NO):
  "Una vez revisado el aplicativo de seguimiento de la correspondencia a cargo del
  contratista, se identificó que SI_[X si pendiente]_ NO_[X si no pendiente]_
  se encuentran radicados pendientes a la fecha, para el período objeto del
  presente informe."
  Párrafo 2 (estático):
  "La anterior información corresponde a la verificación realizada por el
  responsable del manejo de la correspondencia en el área."

SECCIÓN 6: DECLARACIÓN ESPECIAL
  Texto estático:
  "El contratista declara que toda la información relacionada en el presente
  informe corresponde fidedignamente a todas las actividades ejecutadas dentro
  del respectivo período, así como los pagos efectuados en el marco del Sistema
  General de Seguridad Social Integral – SGSSI. Esta declaración se realiza bajo
  la responsabilidad del contratista."

SECCIÓN 7: CIERRE DE APROBACIÓN
  "La supervisión verificó el cumplimiento de las actividades a cargo del
  contratista, en virtud de lo cual se establece la procedencia de la autorización
  del pago/desembolso No. [numeroDesembolso], el cual corresponde a
  $ [valorDesembolso] m/cte. A la fecha el porcentaje de ejecución es:
  [porcentajeEjecucion]%"

  Fecha de elaboración: [informe.fechaAprobacion formateada dd/MM/yyyy]

  "Para constancia se firma por quienes en ella intervinieron al [día] día ([dd])
  del mes de [nombreMes] de [año]"

SECCIÓN 8: FIRMAS
  [img contratista]        [img supervisor]        [img revisor si existe]
  [nombre contratista]     [nombre supervisor]     [nombre revisor]
  [cargo contratista]      [cargo supervisor]      [cargo revisor]
  Contratista              Supervisor(a)           Apoyo a la Supervisión
                                                   (omitir si no hay revisor)
```

#### Paleta CSS del nuevo template

```css
/* Colores institucionales SED (del DESIGN.md) */
.header-institucional {
    background-color: #002869;   /* primary SED */
    color: #ffffff;
}
.section-title {
    background-color: #C0C0C0;   /* gris del DOCX de referencia */
    color: #000000;
    font-weight: bold;
}
th {
    background-color: #002869;
    color: #ffffff;
}
.badge-aprobado {
    background-color: #89002a;   /* tertiary-container SED */
    color: #ffffff;
    padding: 2pt 6pt;
}
.highlight { background-color: #eff4ff; }   /* surface-container-low SED */
body { font-family: Arial, sans-serif; font-size: 10pt; color: #0b1c30; }
```

> **Nota sobre tipografía:** Flying Saucer renderiza XHTML con fuentes del sistema. Arial es la fuente segura disponible en los entornos de despliegue Oracle WebLogic. Public Sans no se incrusta en I6; queda registrada como deuda técnica.

---

## 5. Frontend

### 5.1 Perfil del contratista — campos SGSSI

En `PerfilComponent` (o el componente equivalente de edición de perfil), agregar tres campos de texto:

| Label | Campo | Placeholder |
|-------|-------|-------------|
| Entidad de salud (EPS) | `sgssiSaludEntidad` | Ej. SANITAS |
| Fondo de pensión | `sgssiPensionEntidad` | Ej. COLFONDOS (opcional) |
| ARL | `sgssiArlEntidad` | Ej. SURA |

Los tres campos son `p-inputText` sin asterisco de obligatorio (el campo de pensión lleva label "(opcional)"). El `PATCH /api/usuarios/perfil` existente recibe los valores.

### 5.2 Admin — Formulario de contrato

En `AdminContratoFormComponent`, agregar tres campos nuevos (todos opcionales):

| Label | Campo | Componente |
|-------|-------|------------|
| Dependencia | `dependencia` | `p-dropdown` con catálogo SED (ver abajo) |
| Forma de pago | `formaPago` | `p-textarea` (filas: 5) |
| Modificaciones | `modificaciones` | `p-inputText`, valor default "No se han presentado" |

El campo `modificaciones` se inicializa con "No se han presentado" si está vacío al cargar el formulario de creación.

#### Catálogo de dependencias SED

Definir como constante TypeScript en `core/constants/sed-dependencias.constants.ts`. Cada entrada es un objeto `{ label: string; value: string }` donde `label` y `value` son iguales (nombre de la dependencia). El `p-dropdown` usa `optionLabel="label"` y `optionValue="value"`.

```typescript
// core/constants/sed-dependencias.constants.ts
export const SED_DEPENDENCIAS: { label: string; value: string }[] = [
  { label: 'Despacho de la Secretaría de Educación', value: 'Despacho de la Secretaría de Educación' },
  { label: 'Oficina Asesora de Planeación', value: 'Oficina Asesora de Planeación' },
  { label: 'Oficina de Control Interno', value: 'Oficina de Control Interno' },
  { label: 'Oficina Asesora Jurídica', value: 'Oficina Asesora Jurídica' },
  { label: 'Oficina de Control Disciplinario de Instrucción', value: 'Oficina de Control Disciplinario de Instrucción' },
  { label: 'Oficina de Control Disciplinario de Juzgamiento', value: 'Oficina de Control Disciplinario de Juzgamiento' },
  { label: 'Oficina Asesora de Comunicación y Prensa', value: 'Oficina Asesora de Comunicación y Prensa' },
  { label: 'Oficina de las Tecnologías de la Información y las Comunicaciones', value: 'Oficina de las Tecnologías de la Información y las Comunicaciones' },
  { label: 'Oficina para la Convivencia Escolar', value: 'Oficina para la Convivencia Escolar' },
  // Subsecretaría de Integración Interinstitucional
  { label: 'Subsecretaría de Integración Interinstitucional', value: 'Subsecretaría de Integración Interinstitucional' },
  { label: 'Dirección General de Educación y Colegios Distritales', value: 'Dirección General de Educación y Colegios Distritales' },
  { label: 'Dirección de Participación y Relaciones Interinstitucionales', value: 'Dirección de Participación y Relaciones Interinstitucionales' },
  { label: 'Dirección de Relaciones con el Sector Educativo Privado', value: 'Dirección de Relaciones con el Sector Educativo Privado' },
  { label: 'Dirección de Inspección y Vigilancia', value: 'Dirección de Inspección y Vigilancia' },
  { label: 'Dirección de Relaciones con los Sectores de Educación Superior y Educación para el Trabajo', value: 'Dirección de Relaciones con los Sectores de Educación Superior y Educación para el Trabajo' },
  // Subsecretaría de Calidad y Pertinencia
  { label: 'Subsecretaría de Calidad y Pertinencia', value: 'Subsecretaría de Calidad y Pertinencia' },
  { label: 'Dirección de Educación Preescolar y Básica', value: 'Dirección de Educación Preescolar y Básica' },
  { label: 'Dirección de Educación Media', value: 'Dirección de Educación Media' },
  { label: 'Dirección de Ciencias, Tecnologías y Medios Educativos', value: 'Dirección de Ciencias, Tecnologías y Medios Educativos' },
  { label: 'Dirección de Inclusión e Integración de Poblaciones', value: 'Dirección de Inclusión e Integración de Poblaciones' },
  { label: 'Dirección de Formación de Docentes e Innovaciones Pedagógicas', value: 'Dirección de Formación de Docentes e Innovaciones Pedagógicas' },
  { label: 'Dirección de Evaluación de la Educación', value: 'Dirección de Evaluación de la Educación' },
  // Subsecretaría de Acceso y Permanencia
  { label: 'Subsecretaría de Acceso y Permanencia', value: 'Subsecretaría de Acceso y Permanencia' },
  { label: 'Dirección de Cobertura', value: 'Dirección de Cobertura' },
  { label: 'Dirección de Bienestar Estudiantil', value: 'Dirección de Bienestar Estudiantil' },
  { label: 'Dirección de Construcción y Conservación de Establecimientos Educativos', value: 'Dirección de Construcción y Conservación de Establecimientos Educativos' },
  { label: 'Dirección de Dotaciones Escolares', value: 'Dirección de Dotaciones Escolares' },
  // Subsecretaría de Gestión Institucional
  { label: 'Subsecretaría de Gestión Institucional', value: 'Subsecretaría de Gestión Institucional' },
  { label: 'Dirección de Talento Humano', value: 'Dirección de Talento Humano' },
  { label: 'Dirección de Talento Humano – Prestaciones', value: 'Dirección de Talento Humano – Prestaciones' },
  { label: 'Oficina de Personal', value: 'Oficina de Personal' },
  { label: 'Grupo de Certificaciones Laborales', value: 'Grupo de Certificaciones Laborales' },
  { label: 'Oficina de Escalafón Docente', value: 'Oficina de Escalafón Docente' },
  { label: 'Oficina de Nómina', value: 'Oficina de Nómina' },
  { label: 'Dirección de Contratación', value: 'Dirección de Contratación' },
  { label: 'Oficina de Apoyo Precontractual', value: 'Oficina de Apoyo Precontractual' },
  { label: 'Oficina de Contratos', value: 'Oficina de Contratos' },
  { label: 'Dirección de Servicios Administrativos', value: 'Dirección de Servicios Administrativos' },
  { label: 'Oficina de Servicio al Ciudadano', value: 'Oficina de Servicio al Ciudadano' },
  { label: 'Dirección Financiera', value: 'Dirección Financiera' },
  { label: 'Oficina de Presupuesto', value: 'Oficina de Presupuesto' },
  { label: 'Oficina de Tesorería y Contabilidad', value: 'Oficina de Tesorería y Contabilidad' },
];
```

El `p-dropdown` debe tener `[filter]="true"` para permitir búsqueda por texto (el catálogo tiene 44 entradas). No se usa `[editable]="true"` — el campo acepta solo valores del catálogo.

### 5.3 Formulario de informe — campos nuevos del encabezado

En `InformeFormComponent` (creación) y en `InformeDetalleComponent` (edición en BORRADOR/DEVUELTO), agregar los siguientes campos al encabezado del informe, debajo del selector de período:

| Label | Campo | Componente | Validación |
|-------|-------|------------|------------|
| Número de desembolso | `numeroDesembolso` | `p-inputNumber` (entero positivo) | Opcional al crear |
| Valor del desembolso ($) | `valorDesembolso` | `p-inputNumber` (decimal, 2 dec.) | Opcional al crear |
| Porcentaje de ejecución (%) | `porcentajeEjecucion` | `p-inputNumber` (0–100, 2 dec.) | Opcional al crear |
| ¿Hay correspondencia pendiente? | `correspondenciaPendiente` | `p-checkbox` | No obligatorio |

### 5.4 Formulario de informe — sección SGSSI

En `InformeFormComponent`, agregar una sección "Aportes al Sistema de Seguridad Social" con una tabla editable de 2 o 3 filas:

```
┌──────────┬──────────────┬────────────────────┬────────────────┬─────────────────┐
│ ÍTEM     │ PERÍODO PAGO │ FECHA DE PAGO       │ VALOR APORTADO │ ENTIDAD         │
│          │ (automático) │                     │                │                 │
├──────────┼──────────────┼────────────────────┼────────────────┼─────────────────┤
│ SALUD    │ [calculado]  │ [p-calendar]        │ [p-inputNumber]│ [p-inputText]   │
│ PENSIÓN  │ [calculado]  │ [p-calendar]        │ [p-inputNumber]│ [p-inputText]   │
│ ARL      │ [calculado]  │ [p-calendar]        │ [p-inputNumber]│ [p-inputText]   │
└──────────┴──────────────┴────────────────────┴────────────────┴─────────────────┘
  [+ Agregar PENSIÓN]  ← botón visible solo cuando PENSIÓN no está en la tabla
```

**Reglas de UX:**
- La columna PERÍODO PAGO se calcula del `fechaInicio` del informe y se muestra como texto de solo lectura (ej. "03/2026").
- SALUD y ARL siempre están presentes. PENSIÓN es opcional: hay un botón "Agregar PENSIÓN" que la muestra, y un botón "×" para quitarla.
- Al cargar el formulario, los campos ENTIDAD se pre-rellenan desde el perfil del usuario autenticado (`usuario.sgssiSaludEntidad`, etc.).
- Si el contratista no tiene entidades configuradas en el perfil, los campos ENTIDAD quedan vacíos.

**Servicio Angular requerido:**

```typescript
// core/services/aporte-sgssi.service.ts
guardarAportes(informeId: number, aportes: AporteSgssiRequest[]): Observable<AporteSgssiDto[]>
listarAportes(informeId: number): Observable<AporteSgssiDto[]>
```

### 5.5 Vista de detalle del informe (BORRADOR/DEVUELTO)

En `InformeDetalleComponent`, cuando `estado === 'BORRADOR' || estado === 'DEVUELTO'`:
- Mostrar la sección SGSSI con la misma tabla editable descrita en 5.4.
- Botón "Guardar aportes SGSSI" que llama `PUT /api/informes/{id}/aportes-sgssi`.
- Los campos nuevos del encabezado (número desembolso, valor, porcentaje de ejecución del contrato, correspondencia) se muestran como campos editables.

Cuando el informe está en otro estado: todos los campos se muestran en solo lectura.

#### Ajuste al modo de edición de actividades (I5 — regresión controlada)

Eliminar el campo `porcentaje` del estado de edición por actividad (`ActividadEditState`):

```typescript
// ANTES (I5):
interface ActividadEditState {
  descripcion: string;
  porcentaje: number;   // ← ELIMINAR
  guardando: boolean;
  error: string;
  soporteNombre: string;
  soporteUrl: string;
  soporteArchivo: File | null;
}

// DESPUÉS (I6):
interface ActividadEditState {
  descripcion: string;
  guardando: boolean;
  error: string;
  soporteNombre: string;
  soporteUrl: string;
  soporteArchivo: File | null;
}
```

Cambios adicionales en `InformeDetalleComponent`:
- Eliminar el `<input type="number">` / `p-inputNumber` de porcentaje de la tarjeta de actividad editable.
- Eliminar la validación `if (state.porcentaje < 0 || state.porcentaje > 100)`.
- En `guardarActividad()`, el request a `ActividadInformeService.actualizar()` ya no incluye `porcentaje`.
- Eliminar el test `'muestra error inline si porcentaje es inválido'` (I5) o actualizarlo para reflejar que el campo ya no existe.

---

## 6. Seguridad

No se requieren cambios en `SecurityConfig` ni `DevSecurityConfig`. La regla `antMatchers("/api/informes/**")` cubre el nuevo endpoint de aportes SGSSI. La propiedad del informe se valida en `AporteSgssiService`, no en el controlador (consistente con el patrón de `InformeService`).

---

## 7. Tests Requeridos

### Backend — `NumeroPesosConverter`

| Test | Descripción |
|------|-------------|
| `convertirCero` | 0 → "CERO PESOS M/CTE" |
| `convertirMilUnidades` | 1000 → "MIL PESOS M/CTE" |
| `convertirMillones` | 118666667 → texto correcto en mayúsculas |
| `convertirUnMillon` | 1000000 → "UN MILLÓN PESOS M/CTE" |
| `convertirMilMillones` | 1000000000 → "MIL MILLONES PESOS M/CTE" |

### Backend — `AporteSgssiService`

| Test | Descripción |
|------|-------------|
| `guardarAportesExitoso` | 3 ítems guardados en BORRADOR → lista retornada con periodoSgssi correcto |
| `guardarAportesSinPension` | 2 ítems (SALUD + ARL) → guardado exitoso |
| `guardarAportesInformeEnviadoFalla` | Estado ENVIADO → INFORME_NO_EDITABLE 409 |
| `guardarAportesContratistaIncorrectoFalla` | Email diferente → ACCESO_DENEGADO 403 |
| `guardarAportesItemDuplicadoFalla` | Dos ítems SALUD → error de validación |
| `periodoSgssiEsCorrecto` | fechaInicio = 2026-04-05 → periodoSgssi = "03/2026" |

### Backend — `InformeService` (extensión)

| Test | Descripción |
|------|-------------|
| `crearInformeConCamposNuevosExitoso` | Informe con numeroDesembolso, valorDesembolso, porcentajeEjecucion → persistidos |
| `actualizarInformeCamposNuevosExitoso` | PATCH con campos nuevos en BORRADOR → actualizados |

### Backend — `ContratoService` (extensión)

| Test | Descripción |
|------|-------------|
| `crearContratoConDependenciaYFormaPago` | Contrato creado con nuevos campos → retornados en DTO |
| `actualizarContratoNuevosCampos` | PUT actualiza dependencia, formaPago, modificaciones |

### Backend — Seguridad

| Test | Descripción |
|------|-------------|
| `contratistaCanPutAportesSgssi` | PUT aportes con rol CONTRATISTA → 2xx o 4xx negocio |
| `supervisorCannotPutAportesSgssi` | PUT aportes con rol SUPERVISOR → 403 |
| `unauthenticatedCannotPutAportesSgssi` | PUT sin auth → 401 |

### Frontend

| Test | Descripción |
|------|-------------|
| `InformeFormComponent muestra sección SGSSI con 3 filas` | SALUD, PENSIÓN, ARL visibles |
| `InformeFormComponent pre-rellena entidades desde perfil` | Entidades del servicio de usuario aplicadas |
| `InformeFormComponent permite quitar PENSIÓN` | Fila PENSIÓN se oculta al eliminar |
| `InformeDetalleComponent muestra SGSSI editable en BORRADOR` | Campos editables visibles |
| `InformeDetalleComponent muestra SGSSI solo lectura en ENVIADO` | Sin controles de edición |
| `guardarAportesSgssi llama PUT correctamente` | Servicio invocado con payload correcto |
| `PerfilComponent muestra campos SGSSI` | Tres campos de entidad visibles |
| `AdminContratoFormComponent muestra dependencia como dropdown` | Selector con opciones SED visibles y filtrables |
| `AdminContratoFormComponent muestra formaPago como textarea` | Campo de texto largo presente |
| `InformeDetalleComponent NO muestra porcentaje por actividad en BORRADOR` | I5 regresión: campo porcentaje eliminado de tarjeta de actividad |
| `InformeDetalleComponent guarda actividad sin campo porcentaje` | Request a PUT actividad sin porcentaje → éxito |

---

## 8. Criterios de Aceptación

### Datos del contrato en el PDF

- [ ] El PDF muestra Dependencia cuando el campo está diligenciado en el contrato.
- [ ] El PDF muestra Forma de Pago cuando el campo está diligenciado en el contrato.
- [ ] El PDF muestra Modificaciones con el texto configurado (default "No se han presentado").
- [ ] El Valor del Contrato aparece en letras generadas automáticamente y en número.
- [ ] El campo Plazo se muestra con las fechas de inicio y fin del contrato.
- [ ] Supervisor y cargo aparecen en la fila "Supervisor — Cargo".

### Sección SGSSI

- [ ] Al crear o editar un informe en BORRADOR, el contratista puede ingresar aportes SALUD, PENSIÓN (opcional) y ARL.
- [ ] Los campos ENTIDAD se pre-rellenan desde el perfil del contratista.
- [ ] El PERÍODO PAGO se calcula automáticamente como el mes anterior al inicio del informe.
- [ ] El PDF muestra la tabla de aportes SGSSI con período, fecha, valor y entidad.
- [ ] Si no hay aportes registrados, el PDF muestra "Sin aportes registrados" en la sección SGSSI.

### Estado de radicación y declaración

- [ ] El PDF muestra la sección de radicación de correspondencia con el marcador SI/NO correcto según el campo `correspondenciaPendiente`.
- [ ] La Declaración Especial aparece siempre con texto estático.

### Datos de cierre

- [ ] El PDF muestra el número de desembolso ingresado por el contratista.
- [ ] El PDF muestra el valor del desembolso ingresado por el contratista.
- [ ] El PDF muestra el porcentaje de ejecución ingresado por el contratista.
- [ ] La fecha de elaboración es la fecha de aprobación del informe (automática).
- [ ] El texto del para constancia de firmas usa la fecha de aprobación expresada en palabras.

### Firmas

- [ ] El PDF muestra la firma, nombre y cargo del contratista.
- [ ] El PDF muestra la firma, nombre y cargo del supervisor.
- [ ] Cuando el contrato tiene revisor, el PDF muestra nombre, cargo e imagen de firma del revisor bajo "Apoyo a la Supervisión".
- [ ] Cuando no hay revisor asignado, el bloque "Apoyo a la Supervisión" se omite del PDF.
- [ ] Si el revisor no tiene imagen de firma registrada, su sección muestra nombre y cargo sin imagen.

### Campos de perfil y contrato

- [ ] El contratista puede configurar sus entidades SGSSI (salud, pensión, ARL) desde su perfil.
- [ ] El administrador puede ingresar Dependencia (seleccionada del catálogo SED), Forma de Pago y Modificaciones al crear o editar un contrato.
- [ ] El campo Dependencia muestra un dropdown filtrable con las 44 unidades organizacionales de la SED.
- [ ] El campo Modificaciones del contrato tiene valor por defecto "No se han presentado".

### Eliminación de % avance por actividad

- [ ] Las tarjetas de actividad en modo BORRADOR no muestran el campo "Avance (%)" por actividad.
- [ ] El endpoint `PUT /api/informes/{id}/actividades/{actId}` ya no recibe ni retorna el campo `porcentaje`.
- [ ] El porcentaje de ejecución del contrato se ingresa exclusivamente a nivel de informe (campo "Porcentaje de ejecución del contrato").

### Diseño visual del PDF

- [ ] El encabezado usa el color primario SED (`#002869`) con texto blanco.
- [ ] Los títulos de sección usan fondo gris `#C0C0C0` con texto negro en negrita.
- [ ] Los headers de tabla usan fondo `#002869` con texto blanco.
- [ ] Las filas alternas usan fondo `#eff4ff`.

---

## 9. Rama y Entorno

- **Rama base:** `feat/sigcon-i5`
- **Rama nueva:** `feat/sigcon-i6`
- **Schema Oracle:** `SED_SIGCON` (prefijo tablas `SGCN_`)
- **Migraciones DDL:** Sí — `db/00_setup.sql` se actualiza con los ALTER TABLE y CREATE TABLE/SEQUENCE/TRIGGER.
- **Stack:** Java 8, Spring Boot 2.7.18 WAR, Angular 20 + PrimeNG 20, TypeScript strict.
- **Restricción Java 8:** Sin `var`, sin `Map.of()`, sin `List.of()`, sin `InputStream.readAllBytes()`, sin `YearMonth.of()` implícito (usar `YearMonth.from()`).

---

## 10. Métricas de Cierre

| Métrica | Meta |
|---------|------|
| Backend tests | >= 148 (123 base + >= 25 nuevos), 0 fallos |
| Frontend specs | >= 106 (90 base + >= 16 nuevos), 0 fallos |
| Regresión PDF existente | `PdfInformeServiceTest` adaptado, 0 fallos |
| Regresión I5 actividades | Tests de porcentaje por actividad eliminados/actualizados, 0 fallos |
| DDL scripts actualizados | `db/00_setup.sql` refleja todos los cambios; `SGCN_ACTIVIDADES_INFORME.PORCENTAJE` sin DROP |
| Endpoints nuevos | 2 (`GET` y `PUT /api/informes/{id}/aportes-sgssi`) |
| Endpoints modificados | 1 (`PUT /api/informes/{id}/actividades/{actId}` sin porcentaje) |
| Catálogo dependencias SED | `sed-dependencias.constants.ts` con 44 entradas |
| Campos nuevos en contratos | 3 (dependencia/dropdown, formaPago, modificaciones) |
| Campos nuevos en informes | 4 (numeroDesembolso, valorDesembolso, porcentajeEjecucion, correspondenciaPendiente) |
| Campos nuevos en perfil usuario | 3 (sgssiSaludEntidad, sgssiPensionEntidad, sgssiArlEntidad) |
| Nueva tabla | `SGCN_APORTES_SGSSI` |

---

*Spec generada bajo SDD Spec-Anchored — SIGCON I6 — 2026-05-09*
