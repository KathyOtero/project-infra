import axios from 'axios';
import type { ApiErrorResponse } from '@/types/api';

/**
 * Extrae un mensaje de error legible desde una respuesta de error de Axios.
 *
 * El backend (GlobalExceptionHandler) siempre responde con la forma
 * ApiErrorResponse. Si el error trae "errores" (violaciones de Bean
 * Validation por campo), se concatenan; si no, se usa el "message" general
 * (ej. "Email o contrasena incorrectos").
 */
export function extraerMensajeError(error: unknown, fallback = 'Ocurrio un error inesperado.'): string {
  if (axios.isAxiosError<ApiErrorResponse>(error)) {
    const data = error.response?.data;
    if (data?.errores && Object.keys(data.errores).length > 0) {
      return Object.values(data.errores).join(' ');
    }
    if (data?.message) {
      return data.message;
    }
  }
  return fallback;
}
