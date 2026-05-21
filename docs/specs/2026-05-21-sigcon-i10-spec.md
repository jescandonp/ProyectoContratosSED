# SIGCON I10 — Identidad Visual Prensa SED

**Incremento:** I10  
**Fecha:** 2026-05-21  
**Tipo:** Visual-only (sin cambios de lógica de negocio)  
**Estado:** APROBADO — pendiente implementación

---

## Objetivo

Aplicar los lineamientos visuales entregados por la Oficina de Prensa de la SED al frontend Angular de SIGCON, alineando tipografía, paleta de colores, logo, iconografía y componentes compartidos con la nueva identidad de marca definida en `Prototipo/DESIGN.md`.

---

## Contexto

La Oficina de Prensa de la SED entregó:
- **Logo:** `Prototipo/SIGCON_/logo-head-SIGCON_.png` — lockup "Una educación que te responde" + burbuja naranja SIGCON
- **Fuentes:** `Prototipo/SIGCON_/font_/Montserrat/` y `Prototipo/SIGCON_/font_/Work_Sans/`
- **Iconos:** `Prototipo/SIGCON_/iconos_/` — 15 PNGs de línea en navy y gris

El diseño anterior usaba Public Sans, azul `#0B3D91`, dorado `#FFB300` y rojo Bogotá `#92032E`. El nuevo sistema usa Montserrat + Work Sans, azul marino `#0A0E5A` y naranja SED `#E8401C`.

---

## Alcance — Solo visual

Este incremento **no modifica** ninguna lógica de negocio, rutas, guards, servicios ni modelos. Solo cambia:

1. Variables CSS (`design-tokens.scss`)
2. Declaraciones `@font-face` (`styles.scss`)
3. Assets (fuentes, iconos, logo en `src/assets/`)
4. Templates de componentes compartidos: sidebar, topbar, status-chip

---

## Requerimientos

### R1 — Tipografía
- Montserrat (700, 600) para headings `h1`–`h4`
- Work Sans (400, 700) para body, labels, tablas y UI
- Las fuentes se sirven localmente desde `src/assets/fonts/` (no Google Fonts — entorno institucional sin acceso externo garantizado)

### R2 — Tokens de color
Los tokens CSS en `design-tokens.scss` deben reflejar la nueva paleta:
- `--color-primary`: `#0a0e5a` (azul marino SED)
- `--color-accent`: `#e8401c` (naranja SED — token nuevo)
- `--color-secondary`: `#002869`
- `--color-secondary-container`: `#0b3d91`
- Se eliminan referencias a dorado (`#feb300`, `#ffb300`) y rojo Bogotá (`#89002a`, `#5f001b`) como colores de marca
- Tokens semánticos (error, success, warning) se preservan sin cambio

### R3 — Logo
- Reemplazar el encabezado textual del sidebar ("SED Bogota / SIGCON") por el logo PNG oficial
- El logo ocupa una franja blanca de 72px de alto en la parte superior del sidebar
- Tamaño del logo: `auto × 44px` manteniendo aspect ratio

### R4 — Iconografía en sidebar
- Reemplazar íconos PrimeIcons (`pi-*`) por los PNGs del set Prensa SED
- Íconos sobre fondo navy usan filtro CSS `brightness(0) invert(1)` para aparecer en blanco
- Ítem activo mantiene borde izquierdo 3px naranja SED `#E8401C`

### R5 — Sidebar rediseñado
- Fondo: `#0a0e5a`
- Ítem inactivo: texto `rgba(255,255,255,0.75)`
- Ítem activo: fondo `#1a2080`, texto blanco, borde izquierdo 3px `#E8401C`

### R6 — Status Chip actualizado
- Tone `vb`: reemplazar dorado `#FFB300` por naranja SED `#FFE5DC` / `#C93518`
- Tone `success`: usar tokens `success-container` / `on-success` nuevos
- Tone `warning`: usar tokens `warning-container` / `on-warning`
- Tone `danger`: usar tokens `error-container` / `on-error-container`

### R7 — Topbar
- Agregar logo pequeño (32px alto) en zona izquierda del topbar
- Mantener funcionalidad actual (notificaciones, usuario, logout)

---

## Archivos afectados

| Archivo | Acción |
|---|---|
| `sigcon-angular/src/app/shared/design-tokens.scss` | Modificar — nuevos tokens |
| `sigcon-angular/src/styles.scss` | Modificar — `@font-face` declarations |
| `sigcon-angular/src/index.html` | Modificar — `font-display` hint |
| `sigcon-angular/src/assets/fonts/` | Crear — copiar Montserrat + Work Sans |
| `sigcon-angular/src/assets/icons/` | Crear — copiar 15 PNGs |
| `sigcon-angular/src/assets/images/` | Crear — copiar logo |
| `shared/components/sidebar/sidebar.component.ts` | Modificar — logo, íconos, colores |
| `shared/components/topbar/topbar.component.ts` | Modificar — logo |
| `shared/components/status-chip/status-chip.component.ts` | Modificar — nuevos tokens de color |

---

## Criterios de aceptación

- [ ] `ng build` sin errores ni warnings de tipado
- [ ] El sidebar muestra el logo PNG en la franja superior blanca
- [ ] Los íconos de navegación son los PNGs del set Prensa SED (en blanco sobre navy)
- [ ] El ítem activo del sidebar tiene borde izquierdo naranja
- [ ] Los botones primarios usan naranja SED `#E8401C`
- [ ] Las fuentes Montserrat y Work Sans se cargan correctamente (sin fallback a sans-serif genérico)
- [ ] El chip `vb` (Visto Bueno) usa tonos naranja suave en lugar de dorado
- [ ] No hay cambios en rutas, guards, servicios ni modelos de datos

---

## Fuera de alcance

- Responsive / mobile — se aplica solo a la vista desktop
- Dark mode
- Animaciones o transiciones
- Cambios en el backend
- Cambios en lógica de negocio o flujos
