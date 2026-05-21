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
      neutral: 'border-[var(--color-outline-variant)] bg-[var(--color-surface-container-low)] text-[var(--color-on-surface)]',
      success: 'border-[var(--color-success-container)] bg-[var(--color-success-container)] text-[var(--color-success)]',
      warning: 'border-[var(--color-warning-container)] bg-[var(--color-warning-container)] text-[var(--color-warning)]',
      danger: 'border-[var(--color-error-container)] bg-[var(--color-error-container)] text-[var(--color-error)]',
      vb: 'border-[#ffe5dc] bg-[#ffe5dc] text-[#c93518]'
    };
    return tones[this.tone];
  }
}
