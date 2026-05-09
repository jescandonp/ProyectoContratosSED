export type ItemSgssi = 'SALUD' | 'PENSION' | 'ARL';

export interface AporteSgssiDto {
  id: number;
  item: ItemSgssi;
  fechaPago: string;
  valorAportado: number;
  entidad: string;
}

export interface AporteSgssiRequest {
  item: ItemSgssi;
  fechaPago: string;
  valorAportado: number;
  entidad: string;
}

export const ITEM_SGSSI_LABELS: Record<ItemSgssi, string> = {
  SALUD:   'Salud',
  PENSION: 'Pensión',
  ARL:     'A.R.L.',
};
