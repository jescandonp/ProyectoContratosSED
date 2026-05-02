export type RolObservacion = 'REVISOR' | 'SUPERVISOR';

export interface Observacion {
  id: number;
  texto: string;
  autorRol: RolObservacion;
  fecha: string | null;
}

export interface ObservacionRequest {
  texto: string;
}
