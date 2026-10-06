import { httpClient } from '@/api/httpClient';
import type { PaginaResponse, ReservaRequest, ReservaResponse } from '@/types/api';

export const reservasApi = {
  reservar: (data: ReservaRequest) =>
    httpClient.post<ReservaResponse>('/reservas', data).then((res) => res.data),

  cancelar: (id: number) => httpClient.delete<void>(`/reservas/${id}`).then((res) => res.data),

  misReservas: (page = 0, size = 12) =>
    httpClient
      .get<PaginaResponse<ReservaResponse>>('/reservas/mias', { params: { page, size } })
      .then((res) => res.data),

  reservasRecibidas: (page = 0, size = 12) =>
    httpClient
      .get<PaginaResponse<ReservaResponse>>('/reservas/recibidas', { params: { page, size } })
      .then((res) => res.data),
};
