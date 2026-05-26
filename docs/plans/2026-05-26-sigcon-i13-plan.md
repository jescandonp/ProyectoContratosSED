# SIGCON I13 — Identidad Visual Institucional — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Alinear la UI de SIGCON con el mockup institucional de comunicaciones SED: franja gov.co, footer institucional, logo sin duplicar, login rediseñado con foto de fondo, paleta #f95000/#00005f, sin Public Sans.

**Architecture:** Dos nuevos componentes compartidos (`GovcoBarComponent`, `FooterInstitucionalComponent`) se añaden a `app/shared/components/` y se consumen en `LoginComponent` y `AppShellComponent`. Los cambios de tokens y assets no afectan lógica de negocio. No hay cambios de backend.

**Tech Stack:** Angular 20, standalone components, Tailwind CSS, SCSS tokens, PrimeNG 20. Tests con Jest vía `ng test --watch=false`. Assets PNG copiados manualmente antes de codificar.

**Spec:** `docs/specs/2026-05-26-sigcon-i13-spec.md`

---

## File Map

| Acción | Archivo |
|---|---|
| Copiar | `Prototipo/SIGCON_/ima-fondo_.png` → `src/assets/images/ima-fondo.png` |
| Copiar | `Prototipo/SIGCON_/logo-head-SIGCON_.png` → `src/assets/images/logo-head-sigcon.png` |
| Copiar | `Prototipo/SIGCON_/logos_/logo-gov.co-BLANCO_.png` → `src/assets/images/logo-govco-blanco.png` |
| Copiar | `Prototipo/SIGCON_/logos_/logo-gov.co-COLOR_.png` → `src/assets/images/logo-govco-color.png` |
| Modificar | `src/app/shared/design-tokens.scss` |
| Crear | `src/app/shared/components/govco-bar/govco-bar.component.ts` |
| Crear | `src/app/shared/components/govco-bar/govco-bar.component.spec.ts` |
| Crear | `src/app/shared/components/footer/footer-institucional.component.ts` |
| Crear | `src/app/shared/components/footer/footer-institucional.component.spec.ts` |
| Modificar | `src/app/shared/app-shell.component.ts` |
| Modificar | `src/app/shared/components/sidebar/sidebar.component.ts` |
| Modificar | `src/app/shared/components/topbar/topbar.component.ts` |
| Modificar | `src/app/features/auth/login.component.ts` |

> `src/styles.scss` **no se toca** — ya declara `@font-face` para Montserrat y Work Sans, y `body { font-family: var(--font-family) }`. Sin Public Sans presente.

---

## Task 1: Copiar assets institucionales

**Files:**
- Crear: `src/assets/images/ima-fondo.png`
- Crear: `src/assets/images/logo-head-sigcon.png`
- Crear: `src/assets/images/logo-govco-blanco.png`
- Crear: `src/assets/images/logo-govco-color.png`

- [ ] **Step 1: Copiar los 4 archivos**

Ejecutar desde la raíz del workspace:

```bash
cp "Prototipo/SIGCON_/ima-fondo_.png"               "sigcon-angular/src/assets/images/ima-fondo.png"
cp "Prototipo/SIGCON_/logo-head-SIGCON_.png"         "sigcon-angular/src/assets/images/logo-head-sigcon.png"
cp "Prototipo/SIGCON_/logos_/logo-gov.co-BLANCO_.png" "sigcon-angular/src/assets/images/logo-govco-blanco.png"
cp "Prototipo/SIGCON_/logos_/logo-gov.co-COLOR_.png"  "sigcon-angular/src/assets/images/logo-govco-color.png"
```

- [ ] **Step 2: Verificar que los archivos existen**

```bash
ls sigcon-angular/src/assets/images/
```

Resultado esperado (entre otros): `ima-fondo.png`, `logo-head-sigcon.png`, `logo-govco-blanco.png`, `logo-govco-color.png`

- [ ] **Step 3: Commit**

```bash
cd sigcon-angular
git add src/assets/images/ima-fondo.png src/assets/images/logo-head-sigcon.png \
        src/assets/images/logo-govco-blanco.png src/assets/images/logo-govco-color.png
git commit -m "assets(i13): agregar imagen fondo login y logos institucionales gov.co"
```

---

## Task 2: Corregir design tokens (paleta de colores)

**Files:**
- Modificar: `sigcon-angular/src/app/shared/design-tokens.scss`

- [ ] **Step 1: Abrir el archivo y aplicar los cambios**

Archivo: `sigcon-angular/src/app/shared/design-tokens.scss`

Cambios puntuales (buscar y reemplazar los valores exactos):

```scss
/* ANTES → DESPUÉS */

/* Acento naranja SED: corregir de #e8401c a #f95000 */
--color-accent: #f95000;
--color-accent-container: #ff803d;
--color-accent-dim: #ffac87;

/* Navy SED: corregir de #0a0e5a a #00005f */
--color-primary: #00005f;
--color-primary-container: #1a2085;
--color-surface-tint: #00005f;

/* PrimeNG overrides: actualizar al nuevo naranja */
--p-primary-color: #f95000;
--p-primary-hover-color: #d94700;
--p-primary-active-color: #d94700;
```

Agregar al final del archivo, antes del cierre de `:root { }`, la sección de paleta ampliada:

```scss
  /* Paleta ampliada brand SED */
  --color-orange-mid:   #ff803d;
  --color-orange-light: #ffac87;
  --color-neutral-mid:  #dddddd;
  --color-alt-blue:     #54a2e6;
  --color-alt-yellow:   #f9e04b;
  --color-alt-cyan:     #64d9d5;
```

- [ ] **Step 2: Ejecutar los tests Angular para confirmar que nada se rompe**

```bash
cd sigcon-angular
node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" test -- --watch=false
```

Resultado esperado: `166 SUCCESS` (o más si hay nuevos tests). Sin errores de compilación.

- [ ] **Step 3: Commit**

```bash
git add src/app/shared/design-tokens.scss
git commit -m "style(i13): corregir paleta institucional #f95000 naranja SED y #00005f navy SED"
```

---

## Task 3: Crear `GovcoBarComponent`

**Files:**
- Crear: `sigcon-angular/src/app/shared/components/govco-bar/govco-bar.component.ts`
- Crear: `sigcon-angular/src/app/shared/components/govco-bar/govco-bar.component.spec.ts`

- [ ] **Step 1: Crear el spec (test primero)**

Crear `sigcon-angular/src/app/shared/components/govco-bar/govco-bar.component.spec.ts`:

```typescript
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { GovcoBarComponent } from './govco-bar.component';

describe('GovcoBarComponent', () => {
  let fixture: ComponentFixture<GovcoBarComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GovcoBarComponent],
    }).compileComponents();
    fixture = TestBed.createComponent(GovcoBarComponent);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should render GOV.CO text', () => {
    const el: HTMLElement = fixture.nativeElement;
    expect(el.textContent).toContain('GOV.CO');
  });

  it('should render Colombia text', () => {
    const el: HTMLElement = fixture.nativeElement;
    expect(el.textContent).toContain('Colombia');
  });
});
```

- [ ] **Step 2: Ejecutar el test para confirmar que falla**

```bash
cd sigcon-angular
node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" test -- --watch=false --include="**/govco-bar*"
```

Resultado esperado: error de compilación porque `GovcoBarComponent` no existe aún.

- [ ] **Step 3: Crear el componente**

Crear `sigcon-angular/src/app/shared/components/govco-bar/govco-bar.component.ts`:

```typescript
import { Component } from '@angular/core';

@Component({
  selector: 'app-govco-bar',
  standalone: true,
  template: `
    <div
      class="flex items-center justify-between px-4"
      style="background:#00005f; height:28px; position:relative; z-index:100;"
    >
      <div class="flex items-center gap-2">
        <img
          src="assets/images/logo-govco-blanco.png"
          alt="GOV.CO"
          class="h-4 w-auto object-contain"
          (error)="onLogoError($event)"
        />
        <span
          class="text-white font-semibold tracking-widest"
          style="font-family: var(--font-family); font-size:11px; letter-spacing:1.5px;"
        >GOV.CO</span>
      </div>
      <span
        class="text-white"
        style="font-family: var(--font-family); font-size:10px; opacity:0.85;"
      >Colombia</span>
    </div>
  `
})
export class GovcoBarComponent {
  onLogoError(event: Event): void {
    (event.target as HTMLImageElement).style.display = 'none';
  }
}
```

- [ ] **Step 4: Ejecutar el test para confirmar que pasa**

```bash
node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" test -- --watch=false --include="**/govco-bar*"
```

Resultado esperado: `3 SUCCESS`.

- [ ] **Step 5: Commit**

```bash
git add src/app/shared/components/govco-bar/
git commit -m "feat(i13): agregar GovcoBarComponent franja institucional"
```

---

## Task 4: Crear `FooterInstitucionalComponent`

**Files:**
- Crear: `sigcon-angular/src/app/shared/components/footer/footer-institucional.component.ts`
- Crear: `sigcon-angular/src/app/shared/components/footer/footer-institucional.component.spec.ts`

- [ ] **Step 1: Crear el spec (test primero)**

Crear `sigcon-angular/src/app/shared/components/footer/footer-institucional.component.spec.ts`:

```typescript
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FooterInstitucionalComponent } from './footer-institucional.component';

describe('FooterInstitucionalComponent', () => {
  let fixture: ComponentFixture<FooterInstitucionalComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FooterInstitucionalComponent],
    }).compileComponents();
    fixture = TestBed.createComponent(FooterInstitucionalComponent);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should show contact phone', () => {
    const el: HTMLElement = fixture.nativeElement;
    expect(el.textContent).toContain('(601) 324 1000');
  });

  it('should show contact email', () => {
    const el: HTMLElement = fixture.nativeElement;
    expect(el.textContent).toContain('contactenos@educacionbogota.edu.co');
  });

  it('should show address', () => {
    const el: HTMLElement = fixture.nativeElement;
    expect(el.textContent).toContain('Av. El Dorado');
  });

  it('should show SED institution name', () => {
    const el: HTMLElement = fixture.nativeElement;
    expect(el.textContent).toContain('Secretaría de Educación del Distrito');
  });
});
```

- [ ] **Step 2: Ejecutar para confirmar que falla**

```bash
node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" test -- --watch=false --include="**/footer-institucional*"
```

Resultado esperado: error de compilación por componente inexistente.

- [ ] **Step 3: Crear el componente**

Crear `sigcon-angular/src/app/shared/components/footer/footer-institucional.component.ts`:

```typescript
import { Component } from '@angular/core';

@Component({
  selector: 'app-footer-institucional',
  standalone: true,
  template: `
    <footer
      class="w-full"
      style="background:#00005f;"
    >
      <div
        class="mx-auto flex flex-col items-start justify-between gap-4 px-8 py-4 md:flex-row md:items-center"
        style="max-width:1440px;"
      >
        <!-- Columna izquierda: datos de contacto -->
        <div class="flex flex-col gap-1">
          <span
            class="text-white font-semibold"
            style="font-family:var(--font-family); font-size:12px;"
          >Contáctenos</span>
          <span
            class="text-white opacity-80"
            style="font-family:var(--font-family); font-size:11px; line-height:1.8;"
          >
            Tel: {{ phone }}<br/>
            {{ email }}<br/>
            {{ address }}<br/>
            {{ city }}
          </span>
        </div>

        <!-- Columna derecha: logos + nombre institución -->
        <div class="flex flex-col items-end gap-2">
          <div class="flex items-center gap-3">
            <img
              src="assets/images/logo-govco-blanco.png"
              alt="GOV.CO"
              class="h-6 w-auto object-contain"
              (error)="onLogoError($event)"
            />
          </div>
          <span
            class="text-white text-right opacity-80"
            style="font-family:var(--font-family); font-size:10px; line-height:1.5;"
          >{{ institution }}</span>
        </div>
      </div>
    </footer>
  `
})
export class FooterInstitucionalComponent {
  readonly phone      = '(601) 324 1000';
  readonly email      = 'contactenos@educacionbogota.edu.co';
  readonly address    = 'Av. El Dorado No 66-63, Bogotá - Colombia';
  readonly city       = 'Bogotá D.C.';
  readonly institution = 'Secretaría de Educación del Distrito';

  onLogoError(event: Event): void {
    (event.target as HTMLImageElement).style.display = 'none';
  }
}
```

- [ ] **Step 4: Ejecutar para confirmar que pasa**

```bash
node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" test -- --watch=false --include="**/footer-institucional*"
```

Resultado esperado: `5 SUCCESS`.

- [ ] **Step 5: Commit**

```bash
git add src/app/shared/components/footer/
git commit -m "feat(i13): agregar FooterInstitucionalComponent con datos de contacto SED"
```

---

## Task 5: Corregir `SidebarComponent` — eliminar logo duplicado

**Files:**
- Modificar: `sigcon-angular/src/app/shared/components/sidebar/sidebar.component.ts`
- Modificar: `sigcon-angular/src/app/shared/components/sidebar/sidebar.component.spec.ts`

- [ ] **Step 1: Abrir el spec existente y agregar test**

Archivo: `sigcon-angular/src/app/shared/components/sidebar/sidebar.component.spec.ts`

Agregar este test al `describe` existente:

```typescript
it('should NOT render logo-sigcon img inside sidebar', () => {
  const el: HTMLElement = fixture.nativeElement;
  const logoImgs = el.querySelectorAll('img[alt*="SIGCON"]');
  expect(logoImgs.length).toBe(0);
});
```

> Nota: si el spec existente no tiene `fixture` configurado con `TestBed`, revisarlo antes de agregar. El patrón existente de ese spec ya tiene `ComponentFixture` — solo añadir el `it` al bloque `describe`.

- [ ] **Step 2: Ejecutar para confirmar que el test falla**

```bash
node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" test -- --watch=false --include="**/sidebar*"
```

Resultado esperado: el test nuevo falla porque el img con alt "SIGCON" existe en el sidebar.

- [ ] **Step 3: Eliminar el bloque de logo del sidebar**

En `sigcon-angular/src/app/shared/components/sidebar/sidebar.component.ts`, eliminar las líneas del bloque `<!-- Franja logo -->` y reemplazar por un espaciador:

```html
<!-- ELIMINAR este bloque completo (líneas ~20-26): -->
<!--
<div class="flex h-[72px] items-center justify-center bg-white px-4">
  <img
    src="assets/images/logo-sigcon.png"
    alt="SIGCON — Una educación que te responde"
    class="h-11 w-auto object-contain"
  />
</div>
-->

<!-- REEMPLAZAR POR: -->
<div class="pt-4"></div>
```

El template del componente queda así en su parte inicial:

```typescript
template: `
  <aside class="flex h-full w-56 flex-col bg-[#00005f] text-white">

    <div class="pt-4"></div>

    <!-- Navegación principal -->
    <nav class="flex flex-1 flex-col gap-1 p-2" aria-label="Navegacion principal">
      ...
    </nav>
    ...
  </aside>
`
```

Nota: también actualizar el color `bg-[#0a0e5a]` a `bg-[#00005f]` en la clase del `<aside>` para que coincida con el nuevo token.

- [ ] **Step 4: Ejecutar para confirmar que pasa**

```bash
node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" test -- --watch=false --include="**/sidebar*"
```

Resultado esperado: todos los tests del sidebar en `SUCCESS`.

- [ ] **Step 5: Commit**

```bash
git add src/app/shared/components/sidebar/sidebar.component.ts \
        src/app/shared/components/sidebar/sidebar.component.spec.ts
git commit -m "fix(i13): eliminar logo duplicado del sidebar"
```

---

## Task 6: Actualizar `TopbarComponent` — nuevo logo y placeholder institucional

**Files:**
- Modificar: `sigcon-angular/src/app/shared/components/topbar/topbar.component.ts`

- [ ] **Step 1: Editar el topbar**

En `sigcon-angular/src/app/shared/components/topbar/topbar.component.ts`, reemplazar el bloque del logo y título por el siguiente (dentro del template, en la sección izquierda del `<header>`):

```html
<div class="flex items-center gap-md">
  <img
    src="assets/images/logo-head-sigcon.png"
    alt="SIGCON — Una educación que te responde"
    class="h-8 w-auto object-contain"
  />
  <div class="h-6 w-px bg-[var(--color-outline-variant)]"></div>
  <p class="m-0 text-sm font-medium text-[var(--color-on-surface-variant)]">
    Sistema de Gestión de Contratos - SED
  </p>
  <!-- Logos institucionales: pendiente entrega comunicaciones -->
  <div class="inst-logos-placeholder pointer-events-none opacity-0" aria-hidden="true">
  </div>
</div>
```

- [ ] **Step 2: Ejecutar los tests Angular**

```bash
node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" test -- --watch=false
```

Resultado esperado: `166+ SUCCESS` (igual que antes, sin nuevas fallas).

- [ ] **Step 3: Commit**

```bash
git add src/app/shared/components/topbar/topbar.component.ts
git commit -m "fix(i13): actualizar logo topbar a logo-head-sigcon con placeholder institucional"
```

---

## Task 7: Actualizar `AppShellComponent` — govco bar, footer, grid

**Files:**
- Modificar: `sigcon-angular/src/app/shared/app-shell.component.ts`

- [ ] **Step 1: Editar el componente**

Reemplazar el contenido completo de `sigcon-angular/src/app/shared/app-shell.component.ts`:

```typescript
import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

import { FooterInstitucionalComponent } from './components/footer/footer-institucional.component';
import { GovcoBarComponent } from './components/govco-bar/govco-bar.component';
import { SidebarComponent } from './components/sidebar/sidebar.component';
import { TopbarComponent } from './components/topbar/topbar.component';

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [RouterOutlet, SidebarComponent, TopbarComponent, GovcoBarComponent, FooterInstitucionalComponent],
  template: `
    <div class="flex min-h-screen flex-col bg-[var(--color-surface)]">
      <app-govco-bar />
      <div class="grid flex-1 grid-cols-[16rem_1fr]">
        <app-sidebar />
        <div class="flex min-w-0 flex-col">
          <app-topbar />
          <main class="mx-auto w-full max-w-[1440px] flex-1 p-lg">
            <router-outlet />
          </main>
        </div>
      </div>
      <app-footer-institucional />
    </div>
  `
})
export class AppShellComponent {}
```

- [ ] **Step 2: Ejecutar los tests Angular**

```bash
node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" test -- --watch=false
```

Resultado esperado: `166+ SUCCESS`.

- [ ] **Step 3: Commit**

```bash
git add src/app/shared/app-shell.component.ts
git commit -m "feat(i13): agregar govco bar y footer institucional al app shell"
```

---

## Task 8: Rediseñar `LoginComponent`

**Files:**
- Modificar: `sigcon-angular/src/app/features/auth/login.component.ts`

- [ ] **Step 1: Reemplazar el componente completo**

Reemplazar el contenido completo de `sigcon-angular/src/app/features/auth/login.component.ts`:

```typescript
import { Component } from '@angular/core';
import { Router } from '@angular/router';

import { AuthService } from '../../core/auth/auth.service';
import { RolUsuario } from '../../core/models/usuario.model';
import { FooterInstitucionalComponent } from '../../shared/components/footer/footer-institucional.component';
import { GovcoBarComponent } from '../../shared/components/govco-bar/govco-bar.component';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FooterInstitucionalComponent, GovcoBarComponent],
  template: `
    <div class="flex min-h-screen flex-col" style="font-family: var(--font-family);">

      <!-- Franja gov.co -->
      <app-govco-bar />

      <!-- Header institucional -->
      <header
        class="flex items-center justify-between px-8 py-3"
        style="background: #00005f;"
      >
        <img
          src="assets/images/logo-head-sigcon.png"
          alt="SIGCON — Una educación que te responde"
          class="h-11 w-auto object-contain"
        />
        <!-- Logos SED / Alcaldía / Bogotá — pendiente entrega comunicaciones -->
        <div class="inst-logos-placeholder pointer-events-none opacity-0" aria-hidden="true"></div>
      </header>

      <!-- Área principal con imagen de fondo -->
      <main
        class="relative flex flex-1 items-center justify-center p-8"
        style="
          background-image: url('assets/images/ima-fondo.png');
          background-size: cover;
          background-position: center;
        "
      >
        <!-- Overlay oscuro sobre la foto -->
        <div
          class="pointer-events-none absolute inset-0"
          style="background: rgba(0,0,95,0.55);"
        ></div>

        <!-- Tarjeta de login -->
        <div
          class="relative z-10 w-full overflow-hidden rounded-lg bg-white shadow-xl"
          style="max-width: 420px;"
        >
          <!-- Encabezado tarjeta -->
          <div
            class="border-b px-6 py-4 text-center"
            style="border-color: #eeeeee;"
          >
            <h2
              class="m-0 text-xl"
              style="font-family: var(--font-family-heading); font-weight: 700; color: #00005f;"
            >BIENVENIDO</h2>
            <p
              class="m-0 mt-1 text-sm"
              style="color: #666666;"
            >
              Acceda a la plataforma utilizando sus credenciales institucionales de la SED.
            </p>
          </div>

          <!-- Cuerpo tarjeta -->
          <div class="relative px-6 py-5">

            <!-- Botón SSO principal -->
            <button
              class="mb-4 flex w-full items-center justify-center gap-3 rounded-md py-3 text-sm font-semibold text-white shadow-md transition-opacity hover:opacity-90 active:opacity-80"
              style="background: #f95000;"
              type="button"
            >
              <svg class="h-5 w-5 shrink-0" viewBox="0 0 23 23" fill="none" xmlns="http://www.w3.org/2000/svg">
                <rect x="1" y="1" width="10" height="10" fill="#f25022"/>
                <rect x="12" y="1" width="10" height="10" fill="#7fba00"/>
                <rect x="1" y="12" width="10" height="10" fill="#00a4ef"/>
                <rect x="12" y="12" width="10" height="10" fill="#ffb900"/>
              </svg>
              <span>Iniciar con office 365</span>
            </button>

            <!-- Separador desarrollo -->
            <div class="relative mb-3 flex items-center">
              <div class="flex-grow border-t" style="border-color:#dddddd;"></div>
              <span class="mx-3 flex-shrink text-xs uppercase tracking-wider" style="color:#999;">Acceso local (desarrollo)</span>
              <div class="flex-grow border-t" style="border-color:#dddddd;"></div>
            </div>

            <!-- Botones dev (grid 2 col) -->
            <div class="mb-3 grid grid-cols-2 gap-2">
              @for (user of devUsers; track user.email) {
                <button
                  class="flex flex-col items-start rounded border px-3 py-2 text-left transition-colors hover:border-[#f95000]"
                  style="border-color: #dddddd; background: #fafafa;"
                  type="button"
                  (click)="loginDev(user)"
                >
                  <span class="text-xs font-semibold" style="color:#00005f;">{{ user.label }}</span>
                  <span class="truncate text-[10px]" style="color:#999; max-width:140px;">{{ user.email }}</span>
                </button>
              }
            </div>

            <!-- Link olvidé contraseña -->
            <div class="text-center">
              <a
                href="#"
                class="text-xs"
                style="color: #f95000;"
                (click)="onForgotPassword($event)"
              >Olvide la contraseña</a>
            </div>

            <!-- Badge versión -->
            <span
              class="absolute bottom-2 right-3 text-[9px] font-semibold"
              style="color: #f95000;"
            >VS {{ version }}</span>
          </div>
        </div>
      </main>

      <!-- Footer institucional -->
      <app-footer-institucional />
    </div>
  `
})
export class LoginComponent {
  readonly version = '1.0.3';

  readonly devUsers: { rol: RolUsuario; label: string; email: string; useEmail?: boolean }[] = [
    { rol: 'ADMIN',       label: 'Admin',          email: 'admin@educacionbogota.edu.co' },
    { rol: 'CONTRATISTA', label: 'Contratista',    email: 'juan.escandon@...' },
    { rol: 'CONTRATISTA', label: 'Contratista IVA', email: 'aecheverry@educacionbogota.gov.co', useEmail: true },
    { rol: 'REVISOR',     label: 'Revisor',        email: 'revisor1@...' },
    { rol: 'SUPERVISOR',  label: 'Supervisor',     email: 'supervisor1@...' }
  ];

  constructor(
    private readonly authService: AuthService,
    private readonly router: Router
  ) {}

  loginDev(user: { rol: RolUsuario; email: string; useEmail?: boolean }): void {
    if (user.useEmail) {
      this.authService.loginDevEmail(user.email);
    } else {
      this.authService.loginDev(user.rol);
    }
    if (user.rol === 'ADMIN') {
      void this.router.navigate(['/admin']);
    } else {
      void this.router.navigate(['/contratos']);
    }
  }

  onForgotPassword(event: Event): void {
    event.preventDefault();
    // Flujo de recuperación es Azure AD — mostrar instrucción
    alert('Para recuperar su contraseña, contacte a Mesa de Ayuda: (601) 324 1000');
  }
}
```

- [ ] **Step 2: Ejecutar los tests Angular completos**

```bash
node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" test -- --watch=false
```

Resultado esperado: `166+ SUCCESS`. Sin errores de compilación TypeScript.

- [ ] **Step 3: Commit**

```bash
git add src/app/features/auth/login.component.ts
git commit -m "feat(i13): rediseño login institucional — fondo ima-fondo, govco bar, footer, #f95000"
```

---

## Task 9: Verificación final

- [ ] **Step 1: Ejecutar suite completa Angular**

```bash
cd sigcon-angular
node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" test -- --watch=false
```

Resultado esperado: todos los tests en `SUCCESS`. Anotar el total (debe ser ≥ 166).

- [ ] **Step 2: Build de producción Angular**

```bash
node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" run build
```

Resultado esperado: `Build at: ... - Time: ...ms` sin errores ni warnings de tipo.

- [ ] **Step 3: Verificar criterios de aceptación visuales**

Iniciar el servidor local y recorrer visualmente:

```bash
node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" run start
```

Checklist AC visual:

| AC | Verificación |
|---|---|
| AC-1 | Franja azul `#00005f` + texto "GOV.CO" visible en login Y en pantallas internas |
| AC-2 | Footer azul `#00005f` con datos de contacto visible en login Y en pantallas internas |
| AC-3 | Sidebar NO muestra logo — solo ítems de navegación |
| AC-4 | Login muestra foto de fondo con overlay oscuro |
| AC-5 | Botón SSO es naranja `#f95000` |
| AC-6 | Badge "VS 1.0.3" visible en esquina inferior derecha del card |
| AC-7 | Click en "Olvide la contraseña" muestra alert con Mesa de Ayuda |
| AC-8 | Inspeccionar elemento: ningún `font-family` usa Public Sans |
| AC-9 | Botones y elementos activos PrimeNG usan `#f95000` |
| AC-10 | Sidebar y navy usan `#00005f` |

- [ ] **Step 4: Commit de cierre**

```bash
cd ..
git add sigcon-angular/
git commit -m "docs(i13): cerrar iteración identidad visual institucional — $(date +%F)"
```

---

## Ejecución log

| Tarea | Estado | Commit | Tests |
|---|---|---|---|
| T1 Assets | ⬜ pendiente | — | — |
| T2 Tokens | ⬜ pendiente | — | — |
| T3 GovcoBar | ⬜ pendiente | — | — |
| T4 FooterInstitucional | ⬜ pendiente | — | — |
| T5 Sidebar dedup logo | ⬜ pendiente | — | — |
| T6 Topbar logo | ⬜ pendiente | — | — |
| T7 AppShell | ⬜ pendiente | — | — |
| T8 Login | ⬜ pendiente | — | — |
| T9 Verificación final | ⬜ pendiente | — | — |
