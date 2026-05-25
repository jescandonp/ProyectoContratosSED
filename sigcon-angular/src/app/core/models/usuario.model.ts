export type RolUsuario = 'CONTRATISTA' | 'REVISOR' | 'SUPERVISOR' | 'ADMIN' | 'ADMINISTRATIVO';

export interface Usuario {
  id: number;
  email: string;
  nombre: string;
  cargo: string | null;
  rol: RolUsuario;
  firmaImagen: string | null;
  activo: boolean;
  sgssiSaludEntidad?: string | null;
  sgssiPensionEntidad?: string | null;
  sgssiArlEntidad?: string | null;
  responsableIva?: boolean | null;
  esAdmin?: boolean | null;
}

export interface UsuarioRequest {
  email: string;
  nombre: string;
  cargo: string | null;
  rol: RolUsuario;
  responsableIva?: boolean;
  esAdmin?: boolean;
}

export interface PerfilUpdateRequest {
  nombre: string;
  cargo: string | null;
  sgssiSaludEntidad?: string | null;
  sgssiPensionEntidad?: string | null;
  sgssiArlEntidad?: string | null;
}

export interface EstadoUsuarioRequest {
  activo: boolean;
}
