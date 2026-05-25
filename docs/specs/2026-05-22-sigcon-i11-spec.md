# Spec Tecnica — SIGCON I11
## Ajustes de Formato PDF 11-IF-023 V1

> Fecha: 2026-05-22  
> Metodologia: SDD Spec-Anchored  
> Estado: APROBADO para ejecucion  
> Alcance: ajustes visuales y de validacion de firmas del PDF institucional

---

## 1. Objetivo

Alinear el PDF institucional de informe de actividades con el formato 11-IF-023 V1 observado en las referencias visuales revisadas durante I11.

El incremento corrige cuatro grupos de GAP:

1. Header: orden y texto de paginacion/periodo.
2. Seccion 1: fila de fechas de inicio y terminacion.
3. Footer: orden, posicion y texto de codigo, paginacion y direccion SED.
4. Firmas: validacion obligatoria y tamano visual controlado.

---

## 2. Stack Y Archivos

| Capa | Decision |
|------|----------|
| Backend | Java 8, Spring Boot 2.7.18 |
| Render PDF | XHTML generado en Java + Flying Saucer + OpenPDF |
| Servicio plantilla | `sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/application/service/InformePdfTemplateService.java` |
| Servicio generacion | `sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/application/service/PdfInformeService.java` |
| Tests | `sigcon-backend/src/test/java/co/gov/bogota/sed/sigcon/application/PdfInformeServiceTest.java` |

No se introducen nuevas dependencias, tablas, endpoints ni cambios de frontend.

---

## 3. Reglas Funcionales

### 3.1 Header

La celda derecha del encabezado debe quedar en este orden:

1. `Pagina X de Y`.
2. `PERIODO DEL INFORME`.
3. `Desde (DD/MM/AAAA)`.
4. `Hasta (DD/MM/AAAA)`.

Reglas:

- No usar el formato abreviado `Pag. X / Y`.
- La numeracion debe usar counters de CSS paged media.
- El periodo conserva las fechas reales del informe.

### 3.2 Seccion 1 — Datos Del Contrato

La fila de fechas debe ocupar el ancho completo de la tabla y mantener cuatro celdas visibles:

```text
Fecha de Inicio: | DD/MM/AAAA | Fecha de Terminacion: | DD/MM/AAAA
```

Reglas:

- La fecha de terminacion no debe partirse en varias lineas.
- La fila no debe colapsar hacia el borde derecho.
- Las demas filas de dos columnas no deben cambiar.

### 3.3 Footer

El footer debe separarse en tres zonas:

1. Codigo de formato arriba a la derecha:

```text
11-IF-023
V1
```

2. Paginacion centrada:

```text
Pagina: X de Y
```

3. Direccion institucional centrada abajo:

```text
Avenida El Dorado N° 66-63    PBX: 3241000    www.educacionbogota.edu.co    Linea 195
```

Reglas:

- El codigo no va en la misma linea de la direccion.
- La paginacion no va a la derecha.
- No usar `Pag.` en el footer; usar `Pagina:`.
- El running footer no debe aparecer en el flujo normal inmediatamente debajo del header; solo debe renderizarse como pie de pagina.

### 3.4 Firmas

La generacion del PDF solo puede continuar si las firmas obligatorias existen.

Reglas:

- Contratista: obligatorio y debe tener firma.
- Supervisor: obligatorio y debe tener firma.
- Revisor: si el contrato tiene revisor asignado, debe tener firma.
- Revisor no asignado: no se exige firma de revisor y no se renderiza bloque de revisor.
- Si falta una firma obligatoria, `PdfInformeService` debe lanzar `FIRMA_REQUERIDA` y no debe generar ni almacenar el PDF.
- Las imagenes de firma deben renderizarse dentro de una caja visual controlada para evitar firmas gigantes o desbalanceadas.
- Si existe revisor asignado y pasa validacion, el bloque `Reviso:` debe renderizarse.

---

## 4. Criterios De Aceptacion

| ID | Criterio | Verificacion |
|----|----------|--------------|
| AC-1 | Header muestra `Pagina X de Y` arriba de `PERIODO DEL INFORME` | Revision XHTML/PDF |
| AC-2 | Header no usa `Pag. X / Y` | Revision de codigo |
| AC-3 | Fechas de inicio y terminacion quedan en una fila estable de 4 celdas | Revision XHTML/PDF |
| AC-4 | Footer muestra codigo `11-IF-023` y `V1` arriba a la derecha | Revision XHTML/PDF |
| AC-5 | Footer muestra `Pagina: X de Y` centrado | Revision XHTML/PDF |
| AC-6 | Footer muestra direccion SED centrada abajo | Revision XHTML/PDF |
| AC-7 | Si hay revisor asignado sin firma, PDF falla con `FIRMA_REQUERIDA` | Test unitario |
| AC-8 | Si hay revisor asignado con firma, su firma se entrega a la plantilla | Test unitario |
| AC-9 | Firmas usan caja/tamano controlado | Revision CSS |
| AC-10 | Tests PDF enfocados pasan | `mvn test -Dtest=PdfInformeServiceTest` |
| AC-11 | Footer no se imprime entre header y Seccion 1 en el XHTML base | Test estructural de plantilla |

---

## 5. Limites

- No modificar el flujo de estados de informes.
- No modificar generacion de datos de secciones 2 a 5 salvo firmas.
- No modificar frontend.
- No cambiar dependencias de PDF.
- No relajar las validaciones existentes de contratista y supervisor.
