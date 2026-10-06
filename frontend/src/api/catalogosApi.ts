import { httpClient } from '@/api/httpClient';
import type { CatalogoItem } from '@/types/api';

export const catalogosApi = {
  ciudades: () => httpClient.get<CatalogoItem[]>('/catalogos/ciudades').then((res) => res.data),
  categorias: () => httpClient.get<CatalogoItem[]>('/catalogos/categorias').then((res) => res.data),
};
