# SIGCON I10 — Identidad Visual Prensa SED — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Aplicar la nueva identidad visual de Prensa SED al frontend Angular de SIGCON — fuentes Montserrat/Work Sans, paleta naranja SED, logo y set de iconos personalizados.

**Architecture:** Cambios puramente visuales en tres capas: (1) tokens CSS globales, (2) assets estáticos copiados al proyecto Angular, (3) templates de componentes compartidos. Sin modificaciones a lógica de negocio ni modelos.

**Tech Stack:** Angular 17 standalone, Tailwind CSS, SCSS, PrimeNG/PrimeIcons (a reemplazar en sidebar), `ng build` para validación.

---

## Mapa de archivos

| Archivo | Acción |
|---|---|
| `sigcon-angular/src/app/shared/design-tokens.scss` | Modificar tokens de color y font-family |
| `sigcon-angular/src/styles.scss` | Agregar declaraciones `@font-face` |
| `sigcon-angular/src/assets/fonts/montserrat/` | Crear — copiar 6 variantes .woff + .ttf |
| `sigcon-angular/src/assets/fonts/work-sans/` | Crear — copiar variante variable .ttf |
| `sigcon-angular/src/assets/icons/` | Crear — copiar 15 PNGs del set Prensa SED |
| `sigcon-angular/src/assets/images/logo-sigcon.png` | Crear — copiar logo oficial |
| `shared/components/sidebar/sidebar.component.ts` | Modificar template — logo, íconos, colores |
| `shared/components/topbar/topbar.component.ts` | Modificar template — logo pequeño |
| `shared/components/status-chip/status-chip.component.ts` | Modificar colores de tones |

---

### Task 1: Copiar assets (fuentes, iconos, logo)

**Files:**
- Create: `sigcon-angular/src/assets/fonts/montserrat/`
- Create: `sigcon-angular/src/assets/fonts/work-sans/`
- Create: `sigcon-angular/src/assets/icons/`
- Create: `sigcon-angular/src/assets/images/`

- [ ] **Step 1.1: Crear directorios de assets**

```powershell
New-Item -ItemType Directory -Force "sigcon-angular\src\assets\fonts\montserrat"
New-Item -ItemType Directory -Force "sigcon-angular\src\assets\fonts\work-sans"
New-Item -ItemType Directory -Force "sigcon-angular\src\assets\icons"
New-Item -ItemType Directory -Force "sigcon-angular\src\assets\images"
```

- [ ] **Step 1.2: Copiar fuentes Montserrat (woff para web)**

```powershell
Copy-Item "Prototipo\SIGCON_\font_\Montserrat\woff_\Montserrat-Regular.woff"  "sigcon-angular\src\assets\fonts\montserrat\"
Copy-Item "Prototipo\SIGCON_\font_\Montserrat\woff_\Montserrat-Medium.woff"   "sigcon-angular\src\assets\fonts\montserrat\"
Copy-Item "Prototipo\SIGCON_\font_\Montserrat\woff_\Montserrat-SemiBold.woff" "sigcon-angular\src\assets\fonts\montserrat\"
Copy-Item "Prototipo\SIGCON_\font_\Montserrat\woff_\Montserrat-Bold.woff"     "sigcon-angular\src\assets\fonts\montserrat\"
Copy-Item "Prototipo\SIGCON_\font_\Montserrat\woff_\Montserrat-Light.woff"    "sigcon-angular\src\assets\fonts\montserrat\"
Copy-Item "Prototipo\SIGCON_\font_\Montserrat\woff_\Montserrat-Italic.woff"   "sigcon-angular\src\assets\fonts\montserrat\"
```

- [ ] **Step 1.3: Copiar fuente Work Sans (variable TTF)**

```powershell
Copy-Item "Prototipo\SIGCON_\font_\Work_Sans\WorkSans-VariableFont_wght.ttf" "sigcon-angular\src\assets\fonts\work-sans\"
Copy-Item "Prototipo\SIGCON_\font_\Work_Sans\WorkSans-Italic-VariableFont_wght.ttf" "sigcon-angular\src\assets\fonts\work-sans\"
```

- [ ] **Step 1.4: Copiar los 15 iconos PNG**

```powershell
$icons = @(
  "ico-inicio_.png","ico-contratos_.png","ico-contratos-admin_.png",
  "ico-admin_.png","ico-catalogo_.png","ico-usuarios_.png",
  "ico-perfil_.png","ico-filtro_.png","ico-contratos-admin_.png",
  "icon-flecha-der_.png","icon-flecha-iz_.png","icon-mail_.png",
  "icon-check_.png","icon-buscar_.png","icon-alerta_.png","icon-salir_.png"
)
foreach ($ico in $icons) {
  Copy-Item "Prototipo\SIGCON_\iconos_\$ico" "sigcon-angular\src\assets\icons\" -ErrorAction SilentlyContinue
}
```

- [ ] **Step 1.5: Copiar logo**

```powershell
Copy-Item "Prototipo\SIGCON_\logo-head-SIGCON_.png" "sigcon-angular\src\assets\images\logo-sigcon.png"
```

- [ ] **Step 1.6: Verificar que los archivos existen**

```powershell
ls sigcon-angular\src\assets\fonts\montserrat\
ls sigcon-angular\src\assets\fonts\work-sans\
ls sigcon-angular\src\assets\icons\
ls sigcon-angular\src\assets\images\
```

Esperado: ver los archivos .woff, .ttf y .png listados.

- [ ] **Step 1.7: Commit**

```powershell
git add sigcon-angular/src/assets/
git commit -m "assets(i10): copiar fuentes Montserrat+WorkSans, iconos y logo Prensa SED"
```

---

### Task 2: Actualizar tokens CSS

**Files:**
- Modify: `sigcon-angular/src/app/shared/design-tokens.scss`

- [ ] **Step 2.1: Reemplazar el contenido de design-tokens.scss**

Reemplazar todo el contenido del archivo con:

```scss
:root {
  /* Superficies */
  --color-surface: #f5f7ff;
  --color-surface-dim: #c9d6f0;
  --color-surface-bright: #f5f7ff;
  --color-surface-container-lowest: #ffffff;
  --color-surface-container-low: #eef2fb;
  --color-surface-container: #e4ebf8;
  --color-surface-container-high: #dae3f5;
  --color-surface-container-highest: #d0dbf2;
  --color-on-surface: #0b1c30;
  --color-on-surface-variant: #434652;
  --color-inverse-surface: #1a2d44;
  --color-inverse-on-surface: #eaf1ff;
  --color-outline: #747783;
  --color-outline-variant: #c4c6d3;

  /* Primario — Azul marino SED */
  --color-primary: #0a0e5a;
  --color-on-primary: #ffffff;
  --color-primary-container: #1a2080;
  --color-on-primary-container: #9eaaff;
  --color-inverse-primary: #b8c0ff;
  --color-surface-tint: #0a0e5a;

  /* Acento — Naranja SED */
  --color-accent: #e8401c;
  --color-on-accent: #ffffff;
  --color-accent-container: #ff6d45;
  --color-accent-dim: #ff8a6a;

  /* Secundario — Azul institucional */
  --color-secondary: #002869;
  --color-on-secondary: #ffffff;
  --color-secondary-container: #0b3d91;
  --color-on-secondary-container: #8dadff;

  /* Estados semánticos */
  --color-error: #ba1a1a;
  --color-on-error: #ffffff;
  --color-error-container: #ffdad6;
  --color-on-error-container: #93000a;
  --color-success: #1b6b3a;
  --color-on-success: #ffffff;
  --color-success-container: #d4f5e0;
  --color-warning: #7e5700;
  --color-on-warning: #ffffff;
  --color-warning-container: #ffdeac;

  /* Fondo */
  --color-background: #f5f7ff;
  --color-on-background: #0b1c30;
  --color-surface-variant: #d0dbf2;

  /* Tipografía */
  --font-family-heading: 'Montserrat', sans-serif;
  --font-family: 'Work Sans', sans-serif;
  --font-size-body: 16px;
  --font-size-body-sm: 14px;
  --font-size-table-data: 13px;
  --line-height-body: 24px;

  /* Espaciado */
  --radius-sm: 0.125rem;
  --radius-default: 0.25rem;
  --radius-md: 0.375rem;
  --radius-lg: 0.5rem;
  --radius-xl: 0.75rem;
  --space-xs: 4px;
  --space-sm: 8px;
  --space-md: 16px;
  --space-lg: 24px;
  --space-xl: 40px;
  --grid-gutter: 16px;
  --page-margin: 32px;

  /* PrimeNG overrides */
  --p-primary-color: var(--color-accent);
  --p-primary-contrast-color: var(--color-on-accent);
  --p-primary-hover-color: #c93518;
  --p-primary-active-color: #c93518;
  --p-content-border-radius: var(--radius-default);
  --p-surface-0: var(--color-surface-container-lowest);
  --p-surface-50: var(--color-surface);
  --p-surface-100: var(--color-surface-container-low);
  --p-surface-200: var(--color-surface-container);
  --p-surface-300: var(--color-surface-container-high);
  --p-surface-400: var(--color-surface-container-highest);
  --p-text-color: var(--color-on-surface);
  --p-text-muted-color: var(--color-on-surface-variant);
}
```

- [ ] **Step 2.2: Verificar que el build no rompe**

```powershell
cd sigcon-angular
npx ng build --configuration development 2>&1 | Select-String -Pattern "ERROR|error TS" | Select-Object -First 20
```

Esperado: sin líneas de ERROR. Warnings de SCSS son aceptables.

- [ ] **Step 2.3: Commit**

```powershell
git add sigcon-angular/src/app/shared/design-tokens.scss
git commit -m "style(i10): actualizar tokens CSS — paleta Naranja SED y Azul Marino SED"
```

---

### Task 3: Declarar @font-face en styles.scss

**Files:**
- Modify: `sigcon-angular/src/styles.scss`

- [ ] **Step 3.1: Agregar declaraciones @font-face al inicio de styles.scss**

Insertar al inicio del archivo, antes del `@use`:

```scss
/* ── Montserrat ─────────────────────────────────────────── */
@font-face {
  font-family: 'Montserrat';
  src: url('/assets/fonts/montserrat/Montserrat-Regular.woff') format('woff');
  font-weight: 400;
  font-style: normal;
  font-display: swap;
}
@font-face {
  font-family: 'Montserrat';
  src: url('/assets/fonts/montserrat/Montserrat-Medium.woff') format('woff');
  font-weight: 500;
  font-style: normal;
  font-display: swap;
}
@font-face {
  font-family: 'Montserrat';
  src: url('/assets/fonts/montserrat/Montserrat-SemiBold.woff') format('woff');
  font-weight: 600;
  font-style: normal;
  font-display: swap;
}
@font-face {
  font-family: 'Montserrat';
  src: url('/assets/fonts/montserrat/Montserrat-Bold.woff') format('woff');
  font-weight: 700;
  font-style: normal;
  font-display: swap;
}
@font-face {
  font-family: 'Montserrat';
  src: url('/assets/fonts/montserrat/Montserrat-Light.woff') format('woff');
  font-weight: 300;
  font-style: normal;
  font-display: swap;
}
@font-face {
  font-family: 'Montserrat';
  src: url('/assets/fonts/montserrat/Montserrat-Italic.woff') format('woff');
  font-weight: 400;
  font-style: italic;
  font-display: swap;
}

/* ── Work Sans (variable font) ──────────────────────────── */
@font-face {
  font-family: 'Work Sans';
  src: url('/assets/fonts/work-sans/WorkSans-VariableFont_wght.ttf') format('truetype');
  font-weight: 100 900;
  font-style: normal;
  font-display: swap;
}
@font-face {
  font-family: 'Work Sans';
  src: url('/assets/fonts/work-sans/WorkSans-Italic-VariableFont_wght.ttf') format('truetype');
  font-weight: 100 900;
  font-style: italic;
  font-display: swap;
}

```

El resultado final de styles.scss debe quedar:

```scss
/* ── Montserrat ─────────────────────────────────────────── */
@font-face { /* ... (las 6 declaraciones de arriba) */ }

/* ── Work Sans ──────────────────────────────────────────── */
@font-face { /* ... (las 2 declaraciones de arriba) */ }

@use "./app/shared/design-tokens.scss";

@tailwind base;
@tailwind components;
@tailwind utilities;

@import "primeicons/primeicons.css";

html, body { min-height: 100%; }

body {
  margin: 0;
  background: var(--color-surface);
  color: var(--color-on-surface);
  font-family: var(--font-family);
  font-size: 16px;
  line-height: 24px;
}

* { box-sizing: border-box; }
```

- [ ] **Step 3.2: Verificar build**

```powershell
npx ng build --configuration development 2>&1 | Select-String -Pattern "ERROR" | Select-Object -First 10
```

Esperado: sin errores.

- [ ] **Step 3.3: Commit**

```powershell
git add sigcon-angular/src/styles.scss
git commit -m "style(i10): declarar @font-face Montserrat y Work Sans desde assets locales"
```

---

### Task 4: Rediseñar Sidebar

**Files:**
- Modify: `sigcon-angular/src/app/shared/components/sidebar/sidebar.component.ts`

- [ ] **Step 4.1: Mapear los iconos del set Prensa SED para cada ruta**

La nueva interfaz `NavItem` agrega `iconAsset` con el path al PNG:

```typescript
interface NavItem {
  label: string;
  iconAsset: string;   // path relativo a assets/icons/
  route: string;
}
```

Mapping de rutas a iconos:
- `/perfil` → `ico-perfil_.png`
- `/contratos` → `ico-contratos_.png`
- `/revision/informes` → `ico-contratos-admin_.png`
- `/aprobacion/informes` → `ico-contratos-admin_.png`
- `/visto-bueno` → `ico-contratos-admin_.png`
- `/admin` → `ico-admin_.png`
- `/admin/contratos` → `ico-contratos_.png`
- `/admin/usuarios` → `ico-usuarios_.png`
- `/admin/documentos-catalogo` → `ico-catalogo_.png`

- [ ] **Step 4.2: Reemplazar sidebar.component.ts completo**

```typescript
import { Component, computed } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

import { AuthService } from '../../../core/auth/auth.service';

interface NavItem {
  label: string;
  iconAsset: string;
  route: string;
}

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  template: `
    <aside class="flex h-full w-56 flex-col bg-[#0a0e5a] text-white">

      <!-- Franja logo: fondo blanco -->
      <div class="flex h-[72px] items-center justify-center bg-white px-md">
        <img
          src="assets/images/logo-sigcon.png"
          alt="SIGCON — Una educación que te responde"
          class="h-11 w-auto object-contain"
        />
      </div>

      <!-- Navegación -->
      <nav class="flex flex-1 flex-col gap-xs p-sm" aria-label="Navegacion principal">
        @for (item of navItems(); track item.route) {
          <a
            class="flex h-10 items-center gap-sm rounded px-sm text-sm font-medium text-white/75 no-underline hover:bg-white/10 hover:text-white"
            routerLinkActive="!bg-[#1a2080] !text-white border-l-[3px] border-[#e8401c] pl-[calc(0.5rem-3px)]"
            [routerLink]="item.route"
          >
            <img
              [src]="'assets/icons/' + item.iconAsset"
              [alt]="item.label"
              class="h-5 w-5 object-contain brightness-0 invert"
              aria-hidden="true"
            />
            <span>{{ item.label }}</span>
          </a>
        }
      </nav>

      <!-- Sección inferior -->
      <div class="border-t border-white/15 p-sm">
        <a
          class="flex h-10 items-center gap-sm rounded px-sm text-sm font-medium text-white/75 no-underline hover:bg-white/10 hover:text-white cursor-pointer"
          routerLinkActive="!bg-[#1a2080] !text-white"
          [routerLink]="'/perfil'"
        >
          <img src="assets/icons/ico-perfil_.png" alt="Perfil" class="h-5 w-5 object-contain brightness-0 invert" aria-hidden="true" />
          <span>Mi Perfil</span>
        </a>
      </div>
    </aside>
  `
})
export class SidebarComponent {
  readonly navItems = computed<NavItem[]>(() => {
    const items: NavItem[] = [
      { label: 'Contratos', iconAsset: 'ico-contratos_.png', route: '/contratos' }
    ];

    if (this.authService.hasRole('REVISOR')) {
      items.push({ label: 'Revision', iconAsset: 'ico-contratos-admin_.png', route: '/revision/informes' });
    }

    if (this.authService.hasRole('SUPERVISOR')) {
      items.push({ label: 'Aprobacion', iconAsset: 'ico-contratos-admin_.png', route: '/aprobacion/informes' });
    }

    if (this.authService.hasRole('ADMINISTRATIVO')) {
      items.push({ label: 'Visto Bueno', iconAsset: 'ico-contratos-admin_.png', route: '/visto-bueno' });
    }

    if (this.authService.hasRole('ADMIN')) {
      items.push(
        { label: 'Administracion', iconAsset: 'ico-admin_.png', route: '/admin' },
        { label: 'Contratos Admin', iconAsset: 'ico-contratos_.png', route: '/admin/contratos' },
        { label: 'Usuarios', iconAsset: 'ico-usuarios_.png', route: '/admin/usuarios' },
        { label: 'Catalogo', iconAsset: 'ico-catalogo_.png', route: '/admin/documentos-catalogo' }
      );
    }

    return items;
  });

  constructor(private readonly authService: AuthService) {}
}
```

- [ ] **Step 4.3: Verificar build sin errores de TypeScript**

```powershell
npx ng build --configuration development 2>&1 | Select-String -Pattern "ERROR|error TS" | Select-Object -First 20
```

Esperado: sin errores de compilación.

- [ ] **Step 4.4: Commit**

```powershell
git add sigcon-angular/src/app/shared/components/sidebar/sidebar.component.ts
git commit -m "feat(i10): rediseñar sidebar — logo Prensa SED, iconos custom navy, ítem activo naranja"
```

---

### Task 5: Actualizar Topbar

**Files:**
- Modify: `sigcon-angular/src/app/shared/components/topbar/topbar.component.ts`

- [ ] **Step 5.1: Agregar logo pequeño al topbar**

Reemplazar el bloque `<div>` izquierdo del header que contiene el texto "Sistema de Gestion de Contratos" por un logo pequeño a la izquierda más el texto de sección:

```typescript
@Component({
  selector: 'app-topbar',
  standalone: true,
  imports: [StatusChipComponent, NotificacionesMenuComponent],
  template: `
    <header class="flex min-h-14 items-center justify-between border-b border-[var(--color-outline-variant)] bg-white px-lg">
      <div class="flex items-center gap-md">
        <img
          src="assets/images/logo-sigcon.png"
          alt="SIGCON"
          class="h-8 w-auto object-contain"
        />
        <div class="h-6 w-px bg-[var(--color-outline-variant)]"></div>
        <p class="m-0 text-sm font-medium text-[var(--color-on-surface-variant)]">
          Sistema de Gestión de Contratos — SED
        </p>
      </div>

      @if (authService.currentUser(); as user) {
        <div class="flex items-center gap-sm">
          <app-notificaciones-menu />
          <app-status-chip [value]="user.rol" [label]="user.rol" tone="success" />
          <div class="text-right">
            <p class="m-0 text-sm font-semibold text-[var(--color-on-surface)]">{{ user.nombre }}</p>
            <p class="m-0 text-xs text-[var(--color-on-surface-variant)]">{{ user.email }}</p>
          </div>
          <button
            class="h-9 rounded border border-[var(--color-outline-variant)] bg-white px-sm text-sm font-semibold text-[var(--color-on-surface)] hover:bg-[var(--color-surface-container-low)]"
            type="button"
            (click)="authService.logout()"
          >
            Salir
          </button>
        </div>
      }
    </header>
  `
})
export class TopbarComponent {
  constructor(readonly authService: AuthService) {}
}
```

- [ ] **Step 5.2: Verificar build**

```powershell
npx ng build --configuration development 2>&1 | Select-String -Pattern "ERROR" | Select-Object -First 10
```

- [ ] **Step 5.3: Commit**

```powershell
git add sigcon-angular/src/app/shared/components/topbar/topbar.component.ts
git commit -m "feat(i10): agregar logo Prensa SED al topbar"
```

---

### Task 6: Actualizar Status Chip

**Files:**
- Modify: `sigcon-angular/src/app/shared/components/status-chip/status-chip.component.ts`

- [ ] **Step 6.1: Actualizar los colores de cada tone usando los nuevos tokens**

```typescript
import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-status-chip',
  standalone: true,
  template: `
    <span class="inline-flex h-7 items-center rounded-full border px-[10px] text-xs font-semibold" [class]="toneClass">
      {{ label || value }}
    </span>
  `
})
export class StatusChipComponent {
  @Input({ required: true }) value = '';
  @Input() label = '';
  @Input() tone: 'neutral' | 'success' | 'warning' | 'danger' | 'vb' = 'neutral';

  get toneClass() {
    const tones = {
      neutral:  'border-[var(--color-outline-variant)] bg-[var(--color-surface-container-low)] text-[var(--color-on-surface)]',
      success:  'border-[var(--color-success-container)] bg-[var(--color-success-container)] text-[var(--color-success)]',
      warning:  'border-[var(--color-warning-container)] bg-[var(--color-warning-container)] text-[var(--color-warning)]',
      danger:   'border-[var(--color-error-container)] bg-[var(--color-error-container)] text-[var(--color-error)]',
      vb:       'border-[#ffe5dc] bg-[#ffe5dc] text-[#c93518]'
    };
    return tones[this.tone];
  }
}
```

Nota: `border-radius` cambia de `rounded-lg` a `rounded-full` para seguir el DESIGN.md (chips pill-shape).

- [ ] **Step 6.2: Verificar build**

```powershell
npx ng build --configuration development 2>&1 | Select-String -Pattern "ERROR" | Select-Object -First 10
```

- [ ] **Step 6.3: Commit**

```powershell
git add sigcon-angular/src/app/shared/components/status-chip/status-chip.component.ts
git commit -m "style(i10): actualizar status-chip — colores nuevos tokens, chip vb naranja SED"
```

---

### Task 7: Build final y validación

- [ ] **Step 7.1: Build de producción completo**

```powershell
cd sigcon-angular
npx ng build 2>&1 | tail -20
```

Esperado: `✔ Building...` con `0 errors` y bundle sizes listados.

- [ ] **Step 7.2: Verificar assets incluidos en el build**

```powershell
ls sigcon-angular\dist\sigcon-angular\browser\assets\fonts\montserrat\ | Select-Object Name
ls sigcon-angular\dist\sigcon-angular\browser\assets\icons\ | Select-Object Name
ls sigcon-angular\dist\sigcon-angular\browser\assets\images\ | Select-Object Name
```

Esperado: los archivos .woff, .ttf, .png copiados al dist.

- [ ] **Step 7.3: Verificar en angular.json que assets están incluidos**

Abrir `sigcon-angular/angular.json` y confirmar que existe la entrada:

```json
"assets": [
  { "glob": "**/*", "input": "src/assets", "output": "assets" },
  ...
]
```

Si no existe la entrada de `src/assets`, agregarla al array `assets` de la configuración `build`.

- [ ] **Step 7.4: Actualizar README con nota de I10**

En `README.md`, en la tabla de incrementos, agregar:

```markdown
| I10 | Identidad Visual Prensa SED | ✅ Cerrado | Fuentes Montserrat/Work Sans, paleta naranja SED, logo y set de iconos personalizados |
```

- [ ] **Step 7.5: Commit de cierre**

```powershell
git add README.md
git commit -m "docs(i10): registrar incremento I10 — identidad visual Prensa SED"
```

- [ ] **Step 7.6: Push**

```powershell
git push origin main
```

---

## Self-Review

**Spec coverage:**
- R1 Tipografía → Tasks 3 (font-face) y 2 (token `--font-family`) ✅
- R2 Tokens color → Task 2 ✅
- R3 Logo → Tasks 1 (copia asset), 4 (sidebar), 5 (topbar) ✅
- R4 Iconografía → Tasks 1 (copia PNGs), 4 (sidebar usa `<img>` + `brightness-0 invert`) ✅
- R5 Sidebar rediseñado → Task 4 ✅
- R6 Status Chip → Task 6 ✅
- R7 Topbar → Task 5 ✅

**Criterio angular.json:** Task 7.3 cubre la verificación de que los assets estén declarados.

**Placeholder scan:** ninguno encontrado — todas las tareas tienen código concreto y comandos exactos.

**Tipo consistency:** `NavItem` con campo `iconAsset: string` definido en Task 4.1 y usado en el template de Task 4.2 ✅
