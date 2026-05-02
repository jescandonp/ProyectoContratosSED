import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of } from 'rxjs';

import { Notificacion } from '../../../core/models/notificacion.model';
import { NotificacionService } from '../../../core/services/notificacion.service';
import { NotificacionesMenuComponent } from './notificaciones-menu.component';

describe('NotificacionesMenuComponent', () => {
  let fixture: ComponentFixture<NotificacionesMenuComponent>;
  let service: jasmine.SpyObj<NotificacionService>;
  let router: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    service = jasmine.createSpyObj<NotificacionService>('NotificacionService', [
      'pollNoLeidas',
      'listar',
      'marcarLeida'
    ]);
    router = jasmine.createSpyObj<Router>('Router', ['navigate']);

    service.pollNoLeidas.and.returnValue(of({ count: 3 }));
    service.listar.and.returnValue(of(page([notificacion()])));
    service.marcarLeida.and.returnValue(of({ ...notificacion(), leida: true }));

    await TestBed.configureTestingModule({
      imports: [NotificacionesMenuComponent],
      providers: [
        { provide: NotificacionService, useValue: service },
        { provide: Router, useValue: router }
      ]
    }).compileComponents();
  });

  it('shows unread badge when count is greater than zero', () => {
    fixture = TestBed.createComponent(NotificacionesMenuComponent);
    fixture.detectChanges();

    const badge = fixture.nativeElement.querySelector('[data-testid="notificaciones-badge"]');
    expect(badge?.textContent.trim()).toBe('3');
  });

  it('hides unread badge when count is zero', () => {
    service.pollNoLeidas.and.returnValue(of({ count: 0 }));
    fixture = TestBed.createComponent(NotificacionesMenuComponent);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-testid="notificaciones-badge"]')).toBeNull();
  });

  it('marks a notification as read when clicked from the menu', () => {
    fixture = TestBed.createComponent(NotificacionesMenuComponent);
    fixture.detectChanges();

    fixture.nativeElement.querySelector('[data-testid="notificaciones-toggle"]').click();
    fixture.detectChanges();
    fixture.nativeElement.querySelector('[data-testid="notificacion-menu-item"]').click();

    expect(service.marcarLeida).toHaveBeenCalledWith(10);
  });

  function notificacion(): Notificacion {
    return {
      id: 10,
      titulo: 'Informe aprobado',
      descripcion: 'El informe fue aprobado',
      tipoEvento: 'INFORME_APROBADO',
      idInforme: 50,
      leida: false,
      fecha: '2026-05-02T16:00:00'
    };
  }

  function page(content: Notificacion[]) {
    return {
      content,
      totalElements: content.length,
      totalPages: 1,
      size: 5,
      number: 0,
      first: true,
      last: true
    };
  }
});
