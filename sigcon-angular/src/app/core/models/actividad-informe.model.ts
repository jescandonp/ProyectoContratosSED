import type { SoporteAdjunto } from './soporte-adjunto.model';

export interface ActividadInforme {
  id: number;
  idObligacion: number | null;
  ordenObligacion: number | null;
  descripcionObligacion: string | null;
  descripcion: string;
  porcentaje: number;
  soportes: SoporteAdjunto[];
}

export interface ActividadInformeRequest {
  idObligacion: number;
  descripcion: string;
  porcentaje: number;
}
