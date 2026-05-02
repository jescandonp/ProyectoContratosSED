import type { ActividadInforme } from './actividad-informe.model';
import type { DocumentoAdicional } from './documento-adicional.model';
import type { Observacion } from './observacion.model';
import type { Usuario } from './usuario.model';

export type EstadoInforme = 'BORRADOR' | 'ENVIADO' | 'EN_REVISION' | 'DEVUELTO' | 'APROBADO';

export interface InformeResumen {
  id: number;
  numero: number;
  contratoId: number | null;
  contratoNumero: string | null;
  fechaInicio: string;
  fechaFin: string;
  estado: EstadoInforme;
  fechaUltimoEnvio: string | null;
  fechaAprobacion: string | null;
  pdfRuta?: string | null;
  pdfGeneradoAt?: string | null;
  pdfHash?: string | null;
}

export interface InformeDetalle extends InformeResumen {
  contratista: Usuario | null;
  revisor: Usuario | null;
  supervisor: Usuario | null;
  actividades: ActividadInforme[];
  documentosAdicionales: DocumentoAdicional[];
  observaciones: Observacion[];
}

export interface InformeRequest {
  idContrato: number;
  fechaInicio: string;
  fechaFin: string;
}
