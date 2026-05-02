import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';

import { SidebarComponent } from './sidebar.component';
import { DevSessionService } from '../../../core/auth/dev-session.service';

describe('SidebarComponent', () => {
  let fixture: ComponentFixture<SidebarComponent>;
  let devSession: DevSessionService;

  beforeEach(async () => {
    localStorage.clear();
    await TestBed.configureTestingModule({
      imports: [SidebarComponent],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()]
    }).compileComponents();
    devSession = TestBed.inject(DevSessionService);
  });

  afterEach(() => localStorage.clear());

  it('hides admin navigation for non-admin users', () => {
    devSession.loginAs('CONTRATISTA');
    fixture = TestBed.createComponent(SidebarComponent);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).not.toContain('Administracion');
    expect(fixture.nativeElement.textContent).toContain('Contratos');
  });

  it('shows admin navigation for admin users', () => {
    devSession.loginAs('ADMIN');
    fixture = TestBed.createComponent(SidebarComponent);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Administracion');
  });

  it('shows Revision entry only for REVISOR', () => {
    devSession.loginAs('REVISOR');
    fixture = TestBed.createComponent(SidebarComponent);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Revision');
    expect(fixture.nativeElement.textContent).not.toContain('Aprobacion');
    expect(fixture.nativeElement.textContent).not.toContain('Administracion');
  });

  it('shows Aprobacion entry only for SUPERVISOR', () => {
    devSession.loginAs('SUPERVISOR');
    fixture = TestBed.createComponent(SidebarComponent);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Aprobacion');
    expect(fixture.nativeElement.textContent).not.toContain('Revision');
    expect(fixture.nativeElement.textContent).not.toContain('Administracion');
  });
});
