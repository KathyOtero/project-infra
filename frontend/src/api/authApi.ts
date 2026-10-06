import { httpClient } from '@/api/httpClient';
import type { LoginRequest, LoginResponse, UsuarioRegistroRequest, UsuarioResponse } from '@/types/api';

export const authApi = {
  login: (data: LoginRequest) =>
    httpClient.post<LoginResponse>('/auth/login', data).then((res) => res.data),
};

export const usuariosApi = {
  registrar: (data: UsuarioRegistroRequest) =>
    httpClient.post<UsuarioResponse>('/usuarios/registro', data).then((res) => res.data),

  obtenerPorId: (id: number) =>
    httpClient.get<UsuarioResponse>(`/usuarios/${id}`).then((res) => res.data),
};
