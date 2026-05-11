/** I7: Documento requerido por informe (PDF / EML). */
export interface DocumentoRequerido {
  id: number | null;
  claveLogica: string;
  nombreDisplay: string;
  cargado: boolean;
  nombreArchivo: string | null;
  contentType: string | null;
  extension: string | null;
  tamanoBytes: number | null;
  /** true si este requerido es dinámico por IVA (FACTURA). */
  porIva: boolean;
}

/** I7: Preview básico de un archivo .eml. */
export interface EmlPreview {
  asunto: string | null;
  remitente: string | null;
  destinatarios: string | null;
  fecha: string | null;
  cuerpoTexto: string | null;
  previewParcial: boolean;
}

/** Extensiones permitidas para documentos requeridos. */
export const EXTENSIONES_PERMITIDAS_REQUERIDOS = ['.pdf', '.eml'];
