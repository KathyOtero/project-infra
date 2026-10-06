import { useEffect, useState } from 'react';
import { experienciasApi } from '@/api/experienciasApi';
import { ExperienciaCard } from '@/components/ExperienciaCard';
import { Paginador } from '@/components/Paginador';
import { Spinner } from '@/components/Spinner';
import { useCatalogos } from '@/lib/useCatalogos';
import type { ExperienciaFiltro, ExperienciaResponse } from '@/types/api';

const FILTRO_INICIAL: ExperienciaFiltro = {};

/** Listado publico de experiencias, con filtros por ciudad y categoria. */
export function HomePage() {
  const { ciudades, categorias } = useCatalogos();
  const [filtro, setFiltro] = useState<ExperienciaFiltro>(FILTRO_INICIAL);
  const [pagina, setPagina] = useState(0);
  const [experiencias, setExperiencias] = useState<ExperienciaResponse[]>([]);
  const [totalPaginas, setTotalPaginas] = useState(0);
  const [cargando, setCargando] = useState(true);

  useEffect(() => {
    let activo = true;
    setCargando(true);
    experienciasApi
      .buscar(filtro, pagina)
      .then((data) => {
        if (!activo) return;
        setExperiencias(data.contenido);
        setTotalPaginas(data.totalPaginas);
      })
      .finally(() => {
        if (activo) setCargando(false);
      });
    return () => {
      activo = false;
    };
  }, [filtro, pagina]);

  const actualizarFiltro = (cambios: Partial<ExperienciaFiltro>) => {
    setPagina(0);
    setFiltro((actual) => ({ ...actual, ...cambios }));
  };

  return (
    <section>
      <h1 className="text-3xl font-semibold text-slate-800">Explora experiencias en el Caribe</h1>
      <p className="mt-2 text-slate-500">
        Tours, gastronomia, aventura y cultura ofrecidos por guias locales.
      </p>

      <div className="mt-6 flex flex-wrap gap-3">
        <select
          className="rounded-md border border-slate-300 px-3 py-2 text-sm"
          value={filtro.ciudadId ?? ''}
          onChange={(e) =>
            actualizarFiltro({ ciudadId: e.target.value ? Number(e.target.value) : undefined })
          }
        >
          <option value="">Todas las ciudades</option>
          {ciudades.map((c) => (
            <option key={c.id} value={c.id}>
              {c.nombre}
            </option>
          ))}
        </select>

        <select
          className="rounded-md border border-slate-300 px-3 py-2 text-sm"
          value={filtro.categoriaId ?? ''}
          onChange={(e) =>
            actualizarFiltro({ categoriaId: e.target.value ? Number(e.target.value) : undefined })
          }
        >
          <option value="">Todas las categorias</option>
          {categorias.map((c) => (
            <option key={c.id} value={c.id}>
              {c.nombre}
            </option>
          ))}
        </select>

        <input
          type="date"
          className="rounded-md border border-slate-300 px-3 py-2 text-sm"
          value={filtro.fechaDesde ?? ''}
          onChange={(e) => actualizarFiltro({ fechaDesde: e.target.value || undefined })}
        />
      </div>

      {cargando ? (
        <Spinner texto="Cargando experiencias..." />
      ) : experiencias.length === 0 ? (
        <p className="mt-8 text-center text-slate-400">
          No se encontraron experiencias con esos filtros.
        </p>
      ) : (
        <div className="mt-6 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {experiencias.map((exp) => (
            <ExperienciaCard key={exp.id} experiencia={exp} />
          ))}
        </div>
      )}

      <Paginador paginaActual={pagina} totalPaginas={totalPaginas} onCambiarPagina={setPagina} />
    </section>
  );
}
