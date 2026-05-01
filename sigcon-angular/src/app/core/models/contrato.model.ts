import type { DocumentoCatalogo } from './documento-catalogo.model';
import type { Obligacion } from './obligacion.model';
import type { Usuario } from './usuario.model';

export type TipoContrato = 'OPS';
export type EstadoContrato = 'EN_EJECUCION' | 'LIQUIDADO' | 'CERRADO';

export interface ContratoResumen {
  id: number;
  numero: string;
  objeto: string;
  tipo: TipoContrato;
  estado: EstadoContrato;
  fechaInicio: string;
  fechaFin: string;
  valorTotal: number;
  contratistaNombre: string | null;
  supervisorNombre: string | null;
}

export interface ContratoDetalle extends ContratoResumen {
  contratista: Usuario;
  revisor: Usuario | null;
  supervisor: Usuario | null;
  obligaciones: Obligacion[];
  docsAplicables: DocumentoCatalogo[];
}

export interface ContratoRequest {
  numero: string;
  objeto: string;
  tipo: TipoContrato;
  valorTotal: number;
  fechaInicio: string;
  fechaFin: string;
  idContratista: number;
  idRevisor: number | null;
  idSupervisor: number | null;
}

export interface EstadoContratoRequest {
  estado: EstadoContrato;
}
