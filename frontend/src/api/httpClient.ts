import axios from 'axios';
import { getToken, clearSession } from '@/auth/tokenStorage';

/**
 * Cliente Axios centralizado.
 *
 * baseURL = "/api": en desarrollo, el proxy configurado en vite.config.ts
 * reenvia estas peticiones al backend Spring Boot (localhost:8080), evitando
 * problemas de CORS. En produccion (build estatico servido en S3/CloudFront),
 * se configura via VITE_API_BASE_URL para apuntar directo al backend/ALB.
 */
export const httpClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '/api',
});

// Adjunta el JWT (si existe) a cada peticion saliente.
httpClient.interceptors.request.use((config) => {
  const token = getToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Si el backend responde 401 (token vencido/invalido), limpiamos la sesion
// local para forzar el redirect a /login en la siguiente navegacion.
httpClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      clearSession();
    }
    return Promise.reject(error);
  },
);
