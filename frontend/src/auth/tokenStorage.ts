import type { LoginResponse, Rol } from '@/types/api';

/**
 * Persistencia de la sesion en localStorage.
 *
 * Se guarda el token y los datos basicos del usuario (id, nombre, rol) para
 * poder renderizar la UI (ej. "Hola, Juan" o mostrar el panel de Guia) sin
 * tener que decodificar el JWT en el cliente ni volver a llamar al backend
 * en cada carga de pagina.
 */
const STORAGE_KEY = 'caribexperience_session';

export interface SesionUsuario {
  token: string;
  usuarioId: number;
  nombre: string;
  email: string;
  rol: Rol;
}

export function saveSession(response: LoginResponse): SesionUsuario {
  const sesion: SesionUsuario = {
    token: response.token,
    usuarioId: response.usuarioId,
    nombre: response.nombre,
    email: response.email,
    rol: response.rol,
  };
  localStorage.setItem(STORAGE_KEY, JSON.stringify(sesion));
  return sesion;
}

export function getSession(): SesionUsuario | null {
  const raw = localStorage.getItem(STORAGE_KEY);
  if (!raw) return null;
  try {
    return JSON.parse(raw) as SesionUsuario;
  } catch {
    return null;
  }
}

export function getToken(): string | null {
  return getSession()?.token ?? null;
}

export function clearSession(): void {
  localStorage.removeItem(STORAGE_KEY);
}
