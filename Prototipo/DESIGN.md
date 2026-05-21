---
name: SED Bogotá Design System
colors:
  # Superficies — azul frío institucional
  surface: '#f5f7ff'
  surface-dim: '#c9d6f0'
  surface-bright: '#f5f7ff'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#eef2fb'
  surface-container: '#e4ebf8'
  surface-container-high: '#dae3f5'
  surface-container-highest: '#d0dbf2'
  on-surface: '#0b1c30'
  on-surface-variant: '#434652'
  inverse-surface: '#1a2d44'
  inverse-on-surface: '#eaf1ff'
  outline: '#747783'
  outline-variant: '#c4c6d3'
  surface-tint: '#0a0e5a'
  # Primario — Azul marino SED (navegación, sidebar, iconografía)
  primary: '#0a0e5a'
  on-primary: '#ffffff'
  primary-container: '#1a2080'
  on-primary-container: '#9eaaff'
  inverse-primary: '#b8c0ff'
  # Acento — Naranja SED (CTAs, highlights, logo bubble)
  accent: '#e8401c'
  on-accent: '#ffffff'
  accent-container: '#ff6d45'
  on-accent-container: '#3d0e00'
  accent-dim: '#ff8a6a'
  # Secundario — Azul medio (estados activos, badges)
  secondary: '#002869'
  on-secondary: '#ffffff'
  secondary-container: '#0b3d91'
  on-secondary-container: '#8dadff'
  # Estados semánticos
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  success: '#1b6b3a'
  on-success: '#ffffff'
  success-container: '#d4f5e0'
  warning: '#7e5700'
  on-warning: '#ffffff'
  warning-container: '#ffdeac'
  # Fondo
  background: '#f5f7ff'
  on-background: '#0b1c30'
  surface-variant: '#d0dbf2'
typography:
  # Montserrat — headings y display
  h1:
    fontFamily: Montserrat
    fontSize: 40px
    fontWeight: '700'
    lineHeight: 48px
    letterSpacing: -0.02em
  h2:
    fontFamily: Montserrat
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 40px
    letterSpacing: -0.01em
  h3:
    fontFamily: Montserrat
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
  h4:
    fontFamily: Montserrat
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 28px
  # Work Sans — body, UI, datos
  body-lg:
    fontFamily: Work Sans
    fontSize: 18px
    fontWeight: '400'
    lineHeight: 28px
  body-md:
    fontFamily: Work Sans
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-sm:
    fontFamily: Work Sans
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  label-bold:
    fontFamily: Work Sans
    fontSize: 12px
    fontWeight: '700'
    lineHeight: 16px
    letterSpacing: 0.04em
  table-data:
    fontFamily: Work Sans
    fontSize: 13px
    fontWeight: '400'
    lineHeight: 18px
rounded:
  sm: 0.125rem
  DEFAULT: 0.25rem
  md: 0.375rem
  lg: 0.5rem
  xl: 0.75rem
  full: 9999px
spacing:
  unit: 4px
  xs: 4px
  sm: 8px
  md: 16px
  lg: 24px
  xl: 40px
  gutter: 16px
  margin: 32px
---

## Brand & Style

This design system reflects the identity defined by the Oficina de Prensa de la Secretaría de Educación del Distrito for SIGCON. The brand personality is **Cercana, Eficiente y Confiable** — a system that speaks to the citizen ("Una educación que te responde") while maintaining the institutional authority of the district government.

The chosen aesthetic is **Institucional Moderno**. The interface uses a strong navy-blue structural foundation (sidebar, iconography, navigation) anchored by the SED Orange as the primary action and brand color. This combination reflects the official visual identity delivered by Prensa SED and moves away from the previous cold-blue/gold palette toward a warmer, more energetic identity that communicates proximity and responsiveness.

The logo is the combined lockup: the "Una educación que te responde" logotype alongside the SIGCON speech-bubble mark in SED Orange (`#E8401C`). Both assets are in `Prototipo/SIGCON_/logo-head-SIGCON_.png`.

## Colors

The palette is derived from the visual assets provided by Prensa SED.

- **Azul Marino SED (`#0A0E5A`):** Primary structural color. Used for sidebar background, navigation, iconography, and headings. Communicates authority and depth.
- **Naranja SED (`#E8401C`):** Primary accent and action color. Used for primary CTA buttons, the SIGCON logo bubble, active navigation indicators, and key highlights. Replaces the previous Gold and Bogotá Red accents.
- **Azul Institucional (`#002869`):** Secondary brand blue. Used for secondary actions, active state backgrounds, and link colors.
- **Neutral Slate (`#747783`):** Secondary text, borders, and UI scaffolding.

The default mode is **Light**. High-contrast ratios are maintained to meet accessibility standards for public government platforms. The Naranja SED on white (`#E8401C` on `#FFFFFF`) achieves a contrast ratio of ~4.5:1, meeting WCAG AA for large text and UI components.

Semantic colors (error, success, warning) are independent of the brand palette and follow standard accessibility conventions.

## Typography

The typography system uses a **dual-family pairing** provided by Prensa SED:

- **Montserrat** — Display and headings (h1–h4). A geometric sans-serif that projects confidence and modernity. Used at Bold (700) and SemiBold (600) weights. Font files are in `Prototipo/SIGCON_/font_/Montserrat/`.
- **Work Sans** — Body, UI labels, form fields, and data tables. Optimized for on-screen legibility at small sizes. Used at Regular (400) and Bold (700). Font files are in `Prototipo/SIGCON_/font_/Work_Sans/`.

Usage rules:
- **Headings:** Montserrat Bold with negative letter-spacing (`-0.02em` for h1, `-0.01em` for h2) to maintain strong visual presence.
- **Body Text:** Work Sans Regular at 16px for content, 14px for form fields and dense administrative data.
- **Data Tables:** Work Sans Regular 13px with 18px line-height for high-density grids.
- **Labels:** Work Sans Bold 12px with slight positive tracking (`0.04em`) for uppercase form labels.

## Logo

SIGCON usa un lockup de dos piezas provisto por Prensa SED (`Prototipo/SIGCON_/logo-head-SIGCON_.png`):

1. **Logotipo institucional izquierdo:** "Una educación que te responde" con el ícono de la *e* en Naranja SED sobre fondo blanco. Tipografía en gris oscuro (`#3d3d3d`).
2. **Marca SIGCON derecha:** Texto "SIGCON" en blanco, sobre una burbuja de diálogo en Naranja SED (`#E8401C`). Comunica que el sistema "responde" al usuario.

**Reglas de uso del logo:**
- Siempre usar el lockup completo en el header de la aplicación.
- No separar las dos piezas.
- Sobre fondos oscuros (navbar navy) usar versión con texto blanco.
- Tamaño mínimo: 200px de ancho para preservar legibilidad del logotipo institucional.
- Fondo del header: blanco (`#FFFFFF`) o navy (`#0A0E5A`). No usar sobre grises medios.

## Iconografía

El set de iconos personalizados de Prensa SED reemplaza los íconos genéricos de Material Icons. Son **15 PNGs de línea**, trazo grueso, sin relleno. Se encuentran en `Prototipo/SIGCON_/iconos_/`.

### Dos grupos por color

**Grupo A — Iconos de Navegación (navy `#0A0E5A`)**  
Usados en sidebar, menú lateral y secciones principales.

| Archivo | Uso en la app |
|---|---|
| `ico-inicio_.png` | Dashboard / Inicio |
| `ico-contratos_.png` | Mis Contratos |
| `ico-contratos-admin_.png` | Gestión de Contratos (admin) |
| `ico-admin_.png` | Administración / Archivos |
| `ico-catalogo_.png` | Catálogo / Reportes |
| `ico-usuarios_.png` | Gestión de Usuarios |
| `ico-perfil_.png` | Mi Perfil |
| `ico-filtro_.png` | Filtrar (tablas) |
| `icon-flecha-der_.png` | Paginación / siguiente |
| `icon-flecha-iz_.png` | Paginación / anterior |
| `icon-mail_.png` | Notificaciones email |
| `icon-check_.png` | Estado aprobado / confirmación |

**Grupo B — Iconos Utilitarios (gris `#333333`)**  
Usados en barras de búsqueda, alertas y acciones secundarias sobre fondo claro.

| Archivo | Uso en la app |
|---|---|
| `icon-buscar_.png` | Barra de búsqueda global |
| `icon-alerta_.png` | Alertas y advertencias |
| `icon-salir_.png` | Cerrar sesión |

### Reglas de uso
- **Tamaño base:** 24×24px en UI. Los PNG fuente son ~257px, escalar con `width/height` fijo.
- **No recolorear** los PNGs — ya tienen su color de marca embebido.
- En el sidebar sobre fondo navy, los iconos del Grupo A aparecen en blanco (usar filtro CSS `brightness(0) invert(1)` o versión SVG cuando esté disponible).
- Los iconos del Grupo B **no se usan** sobre fondo navy.
- Espaciado entre ícono y label: `8px` (`spacing.sm`).

## Layout & Spacing

This design system employs a **Fixed Grid** model for large desktop resolutions (max-width: 1440px) to ensure consistency in complex forms, transitioning to a fluid model for smaller screens. 

The layout relies on a **12-column grid** with 16px gutters. This tight gutter width supports the "high-density" requirement, allowing side-by-side form inputs and multi-column data tables to exist without excessive whitespace. Spacing follows a 4px base unit, where administrative panels typically use 8px (sm) or 16px (md) internal padding to maximize the amount of visible data on the "above the fold" area.

## Elevation & Depth

To maintain a clean and professional appearance, this design system minimizes the use of heavy shadows. Depth is primarily communicated through **Tonal Layers** and **Low-contrast outlines**.

- **Level 0 (Background):** Solid #FFFFFF or a very light gray (#F8FAFC) for the canvas.
- **Level 1 (Cards/Panels):** Defined by a 1px border in a neutral-light shade (#E2E8F0). No shadow.
- **Level 2 (Interactive/Floating):** Used for dropdowns and tooltips, utilizing a soft, subtle ambient shadow (0px 4px 12px rgba(0, 0, 0, 0.05)) to separate them from the base administrative layers.
- **Active State:** Elements like focused inputs use a 2px outer glow in the Primary Blue color at 20% opacity.

## Shapes

The shape language is **Soft**, striking a balance between the rigid tradition of government (sharp) and modern user friendliness (rounded). 

- **Standard Elements:** Buttons, input fields, and checkboxes use a 4px (0.25rem) corner radius.
- **Containers:** Large data cards or dashboard sections also follow the 4px rule to maintain a consistent geometric rhythm.
- **Tags/Chips:** May use a slightly higher radius (8px) to differentiate them from functional buttons.

## Components

### Buttons

| Variante | Fondo | Texto | Uso |
|---|---|---|---|
| **Primary** | Naranja SED `#E8401C` | Blanco | CTA principal: "Crear Informe", "Nuevo Contrato", "Aprobar" |
| **Secondary** | Transparente + borde `#0A0E5A` | Navy `#0A0E5A` | Acciones secundarias: "Filtrar", "Exportar" |
| **Tertiary** | Transparente | Navy `#0A0E5A` | Acciones de tabla: "Ver detalle", "Editar" |
| **Danger** | Transparente + borde `#BA1A1A` | Rojo `#BA1A1A` | Acciones destructivas: "Eliminar", "Rechazar" |

Estados:
- **Hover Primary:** `#C93518` (naranja 15% más oscuro)
- **Disabled:** fondo `#E0E0E0`, texto `#9E9E9E`
- **Focus ring:** 2px solid `#E8401C` con 2px offset

### Sidebar / Navegación

El sidebar usa Azul Marino SED (`#0A0E5A`) como fondo con texto e íconos blancos.

- **Ancho:** 220px (expandido) / 64px (colapsado con solo íconos)
- **Logo:** Lockup completo de Prensa SED en la parte superior sobre fondo blanco (`#FFFFFF`) — franja de ~72px de alto
- **Ítem inactivo:** ícono blanco + label Work Sans 14px Regular, blanco `rgba(255,255,255,0.75)`
- **Ítem activo:** fondo `#1a2080` (primary-container) + ícono blanco + label blanco `#FFFFFF` + borde izquierdo 3px Naranja SED `#E8401C`
- **Sección inferior** (Configuración / Cerrar Sesión): separador `rgba(255,255,255,0.15)`, misma jerarquía visual

### Input Fields & Forms

Forms deben ser compactos para alta densidad administrativa.

- **Label:** Work Sans Bold 12px, `#434652`, uppercase con tracking `0.04em`, 4px sobre el input
- **Input height:** 36px
- **Border:** 1px solid `#C4C6D3` (outline-variant)
- **Border focus:** 2px solid `#E8401C` (Naranja SED) — señal de acción activa
- **Placeholder:** Work Sans Regular 14px, `#747783`
- **Error state:** borde `#BA1A1A` + mensaje 12px rojo debajo

### Data Tables

Las tablas son el núcleo del sistema administrativo.

- **Header:** fondo `#EEF2FB` (surface-container-low), Montserrat SemiBold 12px uppercase, color `#0A0E5A`
- **Rows:** alternancia blanco / `#F5F7FF` (surface)
- **Padding de celda:** 8px vertical, 12px horizontal
- **Acción de fila hover:** fondo `#E4EBF8` (surface-container)
- **Columna de acciones:** íconos terciarios alineados a la derecha, 24px

### Chips & Status Indicators

Usados para estados del flujo de contratos e informes. Forma: `border-radius: 9999px` (full), padding `4px 10px`.

| Estado | Fondo | Texto | Token |
|---|---|---|---|
| **APROBADO / VIGENTE** | `#D4F5E0` | `#1B6B3A` | success-container / on-success |
| **PENDIENTE / EN REVISIÓN** | `#E4EBF8` | `#0A0E5A` | surface-container / primary |
| **DEVUELTO / ALERTA** | `#FFDEAC` | `#7E5700` | warning-container / warning |
| **VENCIDO / ERROR** | `#FFDAD6` | `#BA1A1A` | error-container / error |
| **EN VISTO BUENO** | `#FFE5DC` | `#C93518` | accent-dim tint / accent dark |
| **LIQUIDADO** | `#D0DBF2` | `#0A0E5A` | surface-container-highest / primary |

### Cards

Fondo blanco `#FFFFFF`, borde `1px solid #C4C6D3` (outline-variant), `border-radius: 8px` (lg).

- **Header de card:** Montserrat SemiBold 16px color `#0A0E5A`, borde inferior `1px solid #E4EBF8`
- **Padding interno:** 16px (md)
- **Card de KPI / métrica:** borde superior 4px Naranja SED `#E8401C` para destacar el dato principal
- **Card de alerta:** borde izquierdo 4px según color semántico del estado

### Header de Aplicación

Barra superior sobre el contenido principal (a la derecha del sidebar).

- **Fondo:** Blanco `#FFFFFF`
- **Borde inferior:** `1px solid #C4C6D3`
- **Alto:** 56px
- **Contenido:** título de sección (Montserrat Bold 20px, `#0A0E5A`) a la izquierda; ícono de notificaciones + avatar de usuario a la derecha
- **Ícono de búsqueda global:** `icon-buscar_.png` (Grupo B, gris `#333333`), 20px