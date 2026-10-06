import { httpClient } from '@/api/httpClient';
import type {
  ExperienciaFiltro,
  ExperienciaRequest,
  ExperienciaResponse,
  PaginaResponse,
} from '@/types/api';

export const experienciasApi = {
  buscar: (filtro: ExperienciaFiltro, page = 0, size = 12) =>
    httpClient
      .get<PaginaResponse<ExperienciaResponse>>('/experiencias', {
        params: { ...filtro, page, size },
      })
      .then((res) => res.data),

  obtenerPorId: (id: number) =>
    httpClient.get<ExperienciaResponse>(`/experiencias/${id}`).then((res) => res.data),

  crear: (data: ExperienciaRequest) =>
    httpClient.post<ExperienciaResponse>('/experiencias', data).then((res) => res.data),

  actualizar: (id: number, data: ExperienciaRequest) =>
    httpClient.put<ExperienciaResponse>(`/experiencias/${id}`, data).then((res) => res.data),

  cancelar: (id: number) => httpClient.delete<void>(`/experiencias/${id}`).then((res) => res.data),

  misExperiencias: (page = 0, size = 12) =>
    httpClient
      .get<PaginaResponse<ExperienciaResponse>>('/experiencias/mias', { params: { page, size } })
      .then((res) => res.data),
};
