export interface DocumentoAdicional {
  id: number;
  idCatalogo: number | null;
  nombreCatalogo: string | null;
  obligatorio: boolean | null;
  referencia: string;
}

export interface DocumentoAdicionalRequest {
  idCatalogo: number;
  referencia: string;
}
