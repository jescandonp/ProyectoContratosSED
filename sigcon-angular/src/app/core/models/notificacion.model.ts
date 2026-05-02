export type TipoEventoNotificacion =
  | 'INFORME_ENVIADO'
  | 'REVISION_APROBADA'
  | 'REVISION_DEVUELTA'
  | 'INFORME_APROBADO'
  | 'INFORME_DEVUELTO';

export interface Notificacion {
  id: number;
  titulo: string;
  descripcion: string;
  tipoEvento: TipoEventoNotificacion;
  idInforme: number | null;
  leida: boolean;
  fecha: string;
}

export interface NotificacionesCount {
  count: number;
}
