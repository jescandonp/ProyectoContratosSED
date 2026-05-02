import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { AdminDashboardComponent } from './admin-dashboard.component';

describe('AdminDashboardComponent', () => {
  let fixture: ComponentFixture<AdminDashboardComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminDashboardComponent],
      providers: [provideRouter([])]
    }).compileComponents();

    fixture = TestBed.createComponent(AdminDashboardComponent);
    fixture.detectChanges();
  });

  it('renders the admin dashboard with all four shortcut cards', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Contratos');
    expect(compiled.textContent).toContain('Usuarios');
    expect(compiled.textContent).toContain('Catálogo Docs');
    expect(compiled.textContent).toContain('Informes');
  });

  it('Informes card is active (no opacity-60 placeholder)', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    // The card should now be an <a> link, not a disabled <div>
    const informesCard = Array.from(compiled.querySelectorAll('a')).find(
      (el) => el.textContent?.includes('Informes')
    );
    expect(informesCard).toBeTruthy();
    expect(informesCard?.classList.contains('opacity-60')).toBeFalse();
  });

  it('Informes card links to /contratos for read-only admin consultation', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    const informesCard = Array.from(compiled.querySelectorAll('a')).find(
      (el) => el.textContent?.includes('Informes')
    );
    expect(informesCard?.getAttribute('href')).toBe('/contratos');
  });
});
