# SIGCON I13 — Identidad Visual Institucional

**Fecha:** 2026-05-26  
**Estado:** APROBADO — listo para implementación  
**Rama:** main  
**Precedente:** I12 cerrado (241 tests backend, 166 Angular)

---

## Contexto

El equipo de comunicaciones de la SED entregó un mockup de diseño institucional que difiere
de la implementación Angular actual en cuatro áreas: pantalla de login, estructura del app
shell (logo duplicado + footer faltante), paleta de colores y tipografía global. Esta
iteración cierra esos GAPs sin tocar lógica de negocio.

---

## Assets disponibles

Ruta origen: `Prototipo/SIGCON_/`

| Archivo origen | Destino en assets | Uso |
|---|---|---|
| `ima-fondo_.png` | `assets/images/ima-fondo.png` | Background login |
| `logo-head-SIGCON_.png` | `assets/images/logo-head-sigcon.png` | Logo header login + topbar |
| `logos_/logo-gov.co-BLANCO_.png` | `assets/images/logo-govco-blanco.png` | Franja gov.co y footer |
| `logos_/logo-gov.co-COLOR_.png` | `assets/images/logo-govco-color.png` | Variante color (reserva) |

Los íconos PNG, fuentes Montserrat WOFF y Work Sans TTF ya están en `src/assets/` desde
iteraciones anteriores. No se copian de nuevo.

Los logos institucionales SED/Alcaldía/Bogotá del header derecho del login están pendientes
de entrega por comunicaciones. Se reserva el slot en HTML con clase `inst-logos-placeholder`
hasta recibirlos.

---

## R1 — Tokens de diseño y tipografía global

### Archivo: `src/app/shared/design-tokens.scss`

Correcciones de valor (2 tokens):

| Token | Valor actual | Valor correcto | Motivo |
|---|---|---|---|
| `--color-accent` | `#e8401c` | `#f95000` | Naranja SED institucional según manual de marca |
| `--color-primary` | `#0a0e5a` | `#00005f` | Navy SED institucional según manual de marca |
| `--p-primary-color` | `#e8401c` | `#f95000` | Override PrimeNG debe seguir al acento |
| `--p-primary-hover-color` | `#c93518` | `#d94700` | Hover proporcional al nuevo primario |
| `--p-primary-active-color` | `#c93518` | `#d94700` | Active proporcional |

Tokens nuevos a agregar (sección "Paleta ampliada brand SED"):

```scss
/* Paleta ampliada brand SED */
--color-orange-mid:   #ff803d;
--color-orange-light: #ffac87;
--color-neutral-mid:  #dddddd;
--color-alt-blue:     #54a2e6;
--color-alt-yellow:   #f9e04b;
--color-alt-cyan:     #64d9d5;
```

### Archivo: `src/styles.scss`

- Eliminar cualquier referencia a `Public_Sans` o `public-sans`.
- Agregar regla base: `body { font-family: var(--font-family); }` si no existe.
- Verificar que `@font-face` para Montserrat (Bold mínimo) y Work Sans variable
  apunten a `assets/fonts/montserrat/` y `assets/fonts/work-sans/` respectivamente.
  Los archivos ya existen; solo falta la declaración si está ausente.

---

## R2 — Componente `GovcoBarComponent`

**Ruta:** `src/app/shared/components/govco-bar/govco-bar.component.ts`

Componente standalone sin dependencias externas.

### Especificación visual

```
┌──────────────────────────────────────────────────────────────┐
│ ● GOV.CO                                             Colombia │  ← h: 28px, bg: #00005f
└──────────────────────────────────────────────────────────────┘
```

- Fondo: `#00005f`
- Texto "GOV.CO": blanco, Work Sans SemiBold, 11px, letra-spacing 1.5px
- Punto naranja: `#f95000`, 7px, border-radius 50%
- Texto "Colombia": blanco, Work Sans Regular, 10px, alineado a la derecha
- Logo `logo-govco-blanco.png` junto al punto si la imagen carga; si falla, solo texto
- Altura fija: 28px, padding horizontal 16px
- `position: relative; z-index: 100` para estar siempre sobre el contenido

### Uso

Se importa en `LoginComponent` y `AppShellComponent`. No tiene inputs ni outputs.

---

## R3 — Componente `FooterInstitucionalComponent`

**Ruta:** `src/app/shared/components/footer/footer-institucional.component.ts`

Componente standalone. Datos de contacto hardcodeados como constantes privadas del componente
(no requieren servicio ni API).

### Especificación visual

```
┌──────────────────────────────────────────────────────────────┐
│ Contáctenos                          [logo-govco-blanco.png] │
│ Tel: (601) 324 1000                  [logo-bogota-blanco]    │
│ contactenos@educacionbogota.edu.co                           │
│ Av. El Dorado No 66-63, Bogotá - Colombia  Secretaría de    │
│                                            Educación del     │
│                                            Distrito          │
└──────────────────────────────────────────────────────────────┘
```

- Fondo: `#00005f`
- Texto: blanco, Work Sans Regular, 11px, line-height 1.8
- Label "Contáctenos": Work Sans SemiBold, 12px
- Layout: `flex justify-between` en md+; columna en móvil
- Logos: columna derecha, alineados verticalmente al centro
  - `logo-govco-blanco.png` (de assets)
  - Texto "gov.co" en blanco como fallback si la imagen no carga
- Padding: 16px vertical, 32px horizontal

### Uso

Se importa en `LoginComponent` y `AppShellComponent`. No tiene inputs ni outputs.

---

## R4 — Rediseño `LoginComponent`

**Archivo:** `src/app/features/auth/login.component.ts`

Reemplaza el diseño actual completo. La lógica TypeScript (devUsers, loginDev) no cambia.

### Estructura HTML

```
<div.login-page>                         ← flex-col, min-h-screen
  <app-govco-bar />                      ← R2
  <header.login-header>                  ← posición: sobre la imagen de fondo
    <div.brand-left>
      <img logo-head-sigcon.png />        ← h: 44px
    </div>
    <div.inst-logos-placeholder>         ← reservado para logos SED/Alcaldía
      <!-- pendiente comunicaciones -->
    </div>
  </header>
  <main.login-main>                      ← flex-1, background-image: ima-fondo.png
                                            overlay: linear-gradient(#00005f 55% opacity)
    <div.login-card>                     ← bg white, border-radius 8px, shadow-lg, max-w 420px
      <div.card-header>
        <h2>BIENVENIDO</h2>
        <p.subtitle>Acceda a la plataforma...</p>
      </div>
      <div.card-body>
        <button.btn-sso>Iniciar con office 365</button>   ← bg #f95000
        <div.dev-grid>                   ← grid 2-col, solo en desarrollo
          @for dev buttons ...
        </div>
        <a.forgot-link>Olvide la contraseña</a>
      </div>
      <span.version-badge>VS {{ version }}</span>        ← position absolute bottom-right
    </div>
  </main>
  <app-footer-institucional />           ← R3
</div>
```

### Tipografía

- Eliminar clase `font-[Public_Sans,sans-serif]` del contenedor raíz
- Reemplazar por `font-family: var(--font-family)` (Work Sans)
- `<h2>BIENVENIDO</h2>`: `font-family: var(--font-family-heading)` (Montserrat Bold)

### Versión

Constante `version = '1.0.3'` declarada en el componente. Se muestra como badge
`position: absolute; bottom: 8px; right: 8px` dentro de la tarjeta.

### Link "Olvidé la contraseña"

Elemento `<a>` con `href="#"` y `(click).preventDefault()`. En esta iteración no tiene
funcionalidad (flujo de recuperación es Azure AD). Muestra toast informativo:
"Para recuperar su contraseña, contacte a Mesa de Ayuda."

---

## R5 — `AppShellComponent` — gov.co bar, footer, grid corregido

**Archivo:** `src/app/shared/app-shell.component.ts`

### Cambios

1. Importar `GovcoBarComponent` y `FooterInstitucionalComponent`.
2. Nuevo layout:

```html
<div class="shell-root">           ← display:flex; flex-direction:column; min-height:100vh
  <app-govco-bar />
  <div class="shell-body">         ← flex:1; display:grid; grid-template-columns: 16rem 1fr
    <app-sidebar />
    <div class="shell-content">    ← display:flex; flex-direction:column
      <app-topbar />
      <main ...>
        <router-outlet />
      </main>
    </div>
  </div>
  <app-footer-institucional />
</div>
```

El grid actual `grid-cols-[16rem_1fr]` se mantiene para el body pero ya no es el
contenedor raíz — ahora el raíz es flex-col para que el footer quede siempre al pie.

---

## R6 — `SidebarComponent` — eliminar logo duplicado

**Archivo:** `src/app/shared/components/sidebar/sidebar.component.ts`

Eliminar las líneas 20–26 (franja blanca con `logo-sigcon.png`):

```html
<!-- ELIMINAR este bloque: -->
<div class="flex h-[72px] items-center justify-center bg-white px-4">
  <img src="assets/images/logo-sigcon.png" ... />
</div>
```

Reemplazar por un espaciador de 16px (`<div class="pt-4">`) para mantener la
separación visual entre el tope del sidebar y el primer ítem de navegación.

El sidebar queda: fondo `#00005f` continuo desde arriba, nav items empiezan con
padding-top de 16px.

---

## R7 — `TopbarComponent` — logos institucionales placeholder

**Archivo:** `src/app/shared/components/topbar/topbar.component.ts`

El logo `logo-sigcon.png` ya existe en el topbar — **no se toca**.

Cambio: reemplazar el `<img src="assets/images/logo-sigcon.png">` actual por
`<img src="assets/images/logo-head-sigcon.png">` (el asset del prototipo de comunicaciones,
más completo que el actual). Si el archivo es idéntico al existente tras revisión visual,
se mantiene el nombre actual.

Agregar a la derecha del título, antes del bloque de usuario:

```html
<div class="inst-logos-placeholder flex items-center gap-2 opacity-0 pointer-events-none">
  <!-- logos SED/Alcaldía/Bogotá — pendiente entrega comunicaciones -->
</div>
```

El placeholder tiene `opacity-0` hasta recibir los assets. Esto reserva el espacio en DOM
sin mostrar nada roto.

---

## Criterios de aceptación

| ID | Criterio |
|---|---|
| AC-1 | La franja gov.co (azul `#00005f`, texto "GOV.CO") aparece en la cima del login y de todas las páginas internas |
| AC-2 | El footer institucional (azul `#00005f`, datos de contacto + logo gov.co) aparece al pie del login y de todas las páginas internas |
| AC-3 | El logo **no** aparece en el sidebar; solo aparece en el topbar |
| AC-4 | El login muestra la imagen `ima-fondo_.png` como fondo con overlay oscuro |
| AC-5 | El botón SSO del login es color `#f95000` |
| AC-6 | El card del login muestra el badge de versión "VS 1.0.3" en bottom-right |
| AC-7 | El link "Olvide la contraseña" aparece debajo de los botones dev y muestra toast informativo al hacer clic |
| AC-8 | Ninguna pantalla usa la fuente Public Sans |
| AC-9 | El color primario en botones, active states y PrimeNG overrides usa `#f95000` |
| AC-10 | El color del sidebar y navy usa `#00005f` |
| AC-11 | `mvn test` sigue en BUILD SUCCESS (los cambios son solo frontend) |
| AC-12 | `ng test --watch=false` sigue en 166 SUCCESS |

---

## Fuera de alcance I13

- Logos reales de Alcaldía Mayor / Secretaría de Educación / Bogotá (pendiente comunicaciones)
- Flujo real de recuperación de contraseña (es Azure AD)
- Cambios en lógica de negocio, servicios o backend
- Responsive / mobile (el sistema es desktop-first)
- Pantalla de perfil, formularios de informes u otras vistas internas
