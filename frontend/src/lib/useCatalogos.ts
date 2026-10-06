import { useEffect, useState } from 'react';
import { catalogosApi } from '@/api/catalogosApi';
import type { CatalogoItem } from '@/types/api';

/**
 * Carga ciudades y categorias una sola vez (son catalogos estaticos, poco
 * cambiantes). Se reutiliza tanto en los filtros del listado publico como
 * en el formulario de creacion/edicion de experiencias del Guia.
 */
export function useCatalogos() {
  const [ciudades, setCiudades] = useState<CatalogoItem[]>([]);
  const [categorias, setCategorias] = useState<CatalogoItem[]>([]);
  const [cargando, setCargando] = useState(true);

  useEffect(() => {
    let activo = true;
    Promise.all([catalogosApi.ciudades(), catalogosApi.categorias()])
      .then(([ciudadesData, categoriasData]) => {
        if (!activo) return;
        setCiudades(ciudadesData);
        setCategorias(categoriasData);
      })
      .finally(() => {
        if (activo) setCargando(false);
      });
    return () => {
      activo = false;
    };
  }, []);

  return { ciudades, categorias, cargando };
}
