import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export interface ContratistaResultado {
  id: number;
  nombre: string;
  email: string;
  cargo: string | null;
}

export interface ContratoResultado {
  id: number;
  numero: string;
  objeto: string;
  estado: string;
  contratistaNombre: string | null;
  contratistaId?: number | null;
  /** T11: informes anidados del contrato que cumplen los filtros. */
  informes?: InformeResultado[];
}

export interface InformeResultado {
  id: number;
  numero: number;
  estado: string;
  fechaInicio: string;
  fechaFin: string;
  contratoId?: number | null;
  contratoNumero: string | null;
  contratistaNombre: string | null;
  revisorNombre?: string | null;
}

export interface BusquedaAdminResponse {
  contratistas: ContratistaResultado[];
  contratos: ContratoResultado[];
  informes: InformeResultado[];
}

/** T11: Respuesta paginada de la búsqueda avanzada. */
export interface BusquedaAdminPageResponse {
  contratos: ContratoResultado[];
  totalElementos: number;
  paginaActual: number;
  totalPaginas: number;
  tamano: number;
}

/** T11: Filtros para la búsqueda avanzada. */
export interface BusquedaAdminFiltros {
  q?: string;
  estadoContrato?: string;
  fechaInicio?: string;
  fechaFin?: string;
  contratistaId?: number;
  revisorId?: number;
  estadoInforme?: string;
  pagina?: number;
  tamano?: number;
}

@Injectable({ providedIn: 'root' })
export class BusquedaAdminService {
  constructor(private readonly http: HttpClient) {}

  /** T8 legacy: búsqueda simple por texto libre + rango de fechas. */
  buscar(q: string, fechaInicio?: string, fechaFin?: string): Observable<BusquedaAdminResponse> {
    let params = new HttpParams().set('q', q);
    if (fechaInicio) params = params.set('fechaInicio', fechaInicio);
    if (fechaFin) params = params.set('fechaFin', fechaFin);
    return this.http.get<BusquedaAdminResponse>('/api/admin/busqueda', { params });
  }

  /** T11: búsqueda avanzada con filtros combinados y paginación. */
  buscarAvanzado(filtros: BusquedaAdminFiltros): Observable<BusquedaAdminPageResponse> {
    let params = new HttpParams();
    if (filtros.q !== undefined && filtros.q !== null) params = params.set('q', filtros.q);
    if (filtros.estadoContrato) params = params.set('estadoContrato', filtros.estadoContrato);
    if (filtros.fechaInicio) params = params.set('fechaInicio', filtros.fechaInicio);
    if (filtros.fechaFin) params = params.set('fechaFin', filtros.fechaFin);
    if (filtros.contratistaId != null) params = params.set('contratistaId', String(filtros.contratistaId));
    if (filtros.revisorId != null) params = params.set('revisorId', String(filtros.revisorId));
    if (filtros.estadoInforme) params = params.set('estadoInforme', filtros.estadoInforme);
    if (filtros.pagina != null) params = params.set('pagina', String(filtros.pagina));
    if (filtros.tamano != null) params = params.set('tamano', String(filtros.tamano));
    return this.http.get<BusquedaAdminPageResponse>('/api/admin/busqueda/avanzada', { params });
  }
}
