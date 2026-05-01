import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-status-chip',
  standalone: true,
  template: `
    <span class="inline-flex h-7 items-center rounded-lg border px-sm text-xs font-semibold" [class]="toneClass">
      {{ label || value }}
    </span>
  `
})
export class StatusChipComponent {
  @Input({ required: true }) value = '';
  @Input() label = '';
  @Input() tone: 'neutral' | 'success' | 'warning' | 'danger' = 'neutral';

  get toneClass() {
    const tones = {
      neutral: 'border-[var(--color-outline-variant)] bg-[var(--color-surface-container-low)] text-[var(--color-on-surface)]',
      success: 'border-[var(--color-primary-container)] bg-[var(--color-primary-fixed)] text-[var(--color-primary)]',
      warning: 'border-[var(--color-secondary-container)] bg-[var(--color-secondary-fixed)] text-[var(--color-secondary)]',
      danger: 'border-[var(--color-tertiary-container)] bg-[var(--color-tertiary-fixed)] text-[var(--color-tertiary)]'
    };
    return tones[this.tone];
  }
}
