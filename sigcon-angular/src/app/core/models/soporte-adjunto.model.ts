export type TipoSoporte = 'ARCHIVO' | 'URL';

export interface SoporteAdjunto {
  id: number;
  tipo: TipoSoporte;
  nombre: string;
  referencia: string;
}

export interface SoporteUrlRequest {
  nombre: string;
  url: string;
}
