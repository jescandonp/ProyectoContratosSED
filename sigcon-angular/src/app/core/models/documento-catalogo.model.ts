import type { TipoContrato } from './contrato.model';

export interface DocumentoCatalogo {
  id: number;
  nombre: string;
  descripcion: string | null;
  obligatorio: boolean;
  tipoContrato: TipoContrato;
}

export interface DocumentoCatalogoRequest {
  nombre: string;
  descripcion: string | null;
  obligatorio: boolean;
  tipoContrato: TipoContrato;
}
