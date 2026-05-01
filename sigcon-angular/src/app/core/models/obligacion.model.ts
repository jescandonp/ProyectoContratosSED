export interface Obligacion {
  id: number;
  descripcion: string;
  orden: number;
}

export interface ObligacionRequest {
  descripcion: string;
  orden: number;
}
