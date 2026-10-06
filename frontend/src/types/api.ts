// Tipos que reflejan exactamente los DTOs expuestos por el backend Spring
// Boot (web/dto/**). Mantener sincronizados manualmente con el backend.

export type Rol = 'GUIA' | 'VIAJERO';

export interface CatalogoItem {
  id: number;
  nombre: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  tipo: string;
  usuarioId: number;
  nombre: string;
  email: string;
  rol: Rol;
}

export interface UsuarioRegistroRequest {
  nombre: string;
  apellido: string;
  email: string;
  password: string;
  rol: Rol;
}

export interface UsuarioResponse {
  id: number;
  nombre: string;
  apellido: string;
  email: string;
  rol: Rol;
  activo: boolean;
  createdAt: string;
}

export interface ExperienciaRequest {
  titulo: string;
  descripcion: string;
  precio: number;
  cupoMax: number;
  fecha: string; // ISO yyyy-MM-dd
  hora: string; // HH:mm
  fotoUrl?: string | null;
  ciudadId: number;
  categoriaId: number;
}

export interface ExperienciaResponse {
  id: number;
  guiaId: number;
  guiaNombre: string;
  ciudadId: number;
  ciudad: string;
  categoriaId: number;
  categoria: string;
  estado: string;
  titulo: string;
  descripcion: string;
  precio: number;
  cupoMax: number;
  cupoDisponible: number;
  fecha: string;
  hora: string;
  fotoUrl: string | null;
  createdAt: string;
}

export interface ExperienciaFiltro {
  ciudadId?: number;
  categoriaId?: number;
  fechaDesde?: string;
  fechaHasta?: string;
}

export interface ReservaRequest {
  experienciaId: number;
  cantidadPersonas: number;
}

export interface ReservaResponse {
  id: number;
  experienciaId: number;
  experienciaTitulo: string;
  viajeroId: number;
  viajeroNombre: string;
  cantidadPersonas: number;
  estado: string;
  fechaReserva: string;
}

export interface PaginaResponse<T> {
  contenido: T[];
  paginaActual: number;
  totalPaginas: number;
  totalElementos: number;
  esUltimaPagina: boolean;
}

/** Forma del cuerpo de error que arma GlobalExceptionHandler en el backend. */
export interface ApiErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  errores?: Record<string, string> | null;
}
