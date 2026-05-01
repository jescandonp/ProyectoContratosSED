export type RolUsuario = 'CONTRATISTA' | 'REVISOR' | 'SUPERVISOR' | 'ADMIN';

export interface Usuario {
  id: number;
  email: string;
  nombre: string;
  cargo: string | null;
  rol: RolUsuario;
  firmaImagen: string | null;
  activo: boolean;
}

export interface UsuarioRequest {
  email: string;
  nombre: string;
  cargo: string | null;
  rol: RolUsuario;
}

export interface PerfilUpdateRequest {
  nombre: string;
  cargo: string | null;
}

export interface EstadoUsuarioRequest {
  activo: boolean;
}
