import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-empty-state',
  standalone: true,
  template: `
    <section class="flex min-h-64 flex-col items-center justify-center gap-sm rounded border border-[var(--color-outline-variant)] bg-white p-lg text-center">
      <i class="pi pi-folder-open text-3xl text-[var(--color-primary-container)]" aria-hidden="true"></i>
      <h1 class="m-0 text-xl font-semibold text-[var(--color-on-surface)]">{{ title }}</h1>
      <p class="m-0 max-w-xl text-sm text-[var(--color-on-surface-variant)]">{{ description }}</p>
    </section>
  `
})
export class EmptyStateComponent {
  @Input() title = 'Modulo pendiente';
  @Input() description = 'Este espacio queda reservado para las pantallas del Incremento 1.';
}
