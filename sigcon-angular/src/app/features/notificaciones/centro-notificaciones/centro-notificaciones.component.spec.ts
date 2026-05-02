import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of } from 'rxjs';

import { Notificacion } from '../../../core/models/notificacion.model';
import { NotificacionService } from '../../../core/services/notificacion.service';
import { CentroNotificacionesComponent } from './centro-notificaciones.component';

describe('CentroNotificacionesComponent', () => {
  let fixture: ComponentFixture<CentroNotificacionesComponent>;
  let service: jasmine.SpyObj<NotificacionService>;
  let router: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    service = jasmine.createSpyObj<NotificacionService>('NotificacionService', [
      'listar',
      'marcarLeida',
      'marcarTodasLeidas'
    ]);
    router = jasmine.createSpyObj<Router>('Router', ['navigate']);

    service.listar.and.returnValue(of(page([notificacion()])));
    service.marcarLeida.and.returnValue(of({ ...notificacion(), leida: true }));
    service.marcarTodasLeidas.and.returnValue(of(undefined));

    await TestBed.configureTestingModule({
      imports: [CentroNotificacionesComponent],
      providers: [
        { provide: NotificacionService, useValue: service },
        { provide: Router, useValue: router }
      ]
    }).compileComponents();
  });

  it('navigates to the informe after marking an unread notification as read', () => {
    fixture = TestBed.createComponent(CentroNotificacionesComponent);
    fixture.detectChanges();

    fixture.nativeElement.querySelector('[data-testid="notificacion-row"]').click();

    expect(service.marcarLeida).toHaveBeenCalledWith(10);
    expect(router.navigate).toHaveBeenCalledWith(['/informes', 50]);
  });

  it('marks all notifications as read', () => {
    fixture = TestBed.createComponent(CentroNotificacionesComponent);
    fixture.detectChanges();

    fixture.nativeElement.querySelector('[data-testid="marcar-todas"]').click();

    expect(service.marcarTodasLeidas).toHaveBeenCalled();
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
      size: 10,
      number: 0,
      first: true,
      last: true
    };
  }
});
