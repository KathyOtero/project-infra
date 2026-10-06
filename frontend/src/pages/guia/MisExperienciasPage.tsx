import { useEffect, useState } from 'react';
import { experienciasApi } from '@/api/experienciasApi';
import { ExperienciaCard } from '@/components/ExperienciaCard';
import { ExperienciaFormModal } from '@/components/ExperienciaFormModal';
import { Spinner } from '@/components/Spinner';
import { Paginador } from '@/components/Paginador';
import { extraerMensajeError } from '@/lib/errors';
import type { ExperienciaResponse } from '@/types/api';

/** Panel de gestion del Guia: crear, editar y cancelar sus propias experiencias. */
export function MisExperienciasPage() {
  const [experiencias, setExperiencias] = useState<ExperienciaResponse[]>([]);
  const [pagina, setPagina] = useState(0);
  const [totalPaginas, setTotalPaginas] = useState(0);
  const [cargando, setCargando] = useState(true);
  const [modalAbierto, setModalAbierto] = useState(false);
  const [experienciaEnEdicion, setExperienciaEnEdicion] = useState<ExperienciaResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  const cargar = () => {
    setCargando(true);
    experienciasApi
      .misExperiencias(pagina)
      .then((data) => {
        setExperiencias(data.contenido);
        setTotalPaginas(data.totalPaginas);
      })
      .finally(() => setCargando(false));
  };

  useEffect(cargar, [pagina]);

  const abrirCrear = () => {
    setExperienciaEnEdicion(null);
    setModalAbierto(true);
  };

  const abrirEditar = (exp: ExperienciaResponse) => {
    setExperienciaEnEdicion(exp);
    setModalAbierto(true);
  };

  const alGuardar = () => {
    setModalAbierto(false);
    cargar();
  };

  const cancelarExperiencia = async (id: number) => {
    if (!confirm('Seguro que deseas cancelar esta experiencia? Esta accion no se puede deshacer.')) {
      return;
    }
    setError(null);
    try {
      await experienciasApi.cancelar(id);
      cargar();
    } catch (e) {
      setError(extraerMensajeError(e, 'No se pudo cancelar la experiencia.'));
    }
  };

  return (
    <section>
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold text-slate-800">Mis experiencias</h1>
        <button
          onClick={abrirCrear}
          className="rounded-md bg-caribe-coral px-4 py-2 text-sm font-medium text-white hover:opacity-90"
        >
          + Nueva experiencia
        </button>
      </div>

      {error && <p className="mt-3 text-sm text-red-500">{error}</p>}

      {cargando ? (
        <Spinner texto="Cargando tus experiencias..." />
      ) : experiencias.length === 0 ? (
        <p className="mt-8 text-center text-slate-400">
          Aun no has publicado experiencias. Crea la primera con el boton de arriba.
        </p>
      ) : (
        <div className="mt-6 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {experiencias.map((exp) => (
            <ExperienciaCard
              key={exp.id}
              experiencia={exp}
              acciones={
                <div className="mt-2 flex gap-2">
                  <button
                    onClick={() => abrirEditar(exp)}
                    className="flex-1 rounded-md border border-slate-300 px-2 py-1 text-xs hover:bg-slate-50"
                  >
                    Editar
                  </button>
                  <button
                    onClick={() => cancelarExperiencia(exp.id)}
                    disabled={exp.estado === 'CANCELADA'}
                    className="flex-1 rounded-md border border-red-300 px-2 py-1 text-xs text-red-500 hover:bg-red-50 disabled:opacity-40"
                  >
                    {exp.estado === 'CANCELADA' ? 'Cancelada' : 'Cancelar'}
                  </button>
                </div>
              }
            />
          ))}
        </div>
      )}

      <Paginador paginaActual={pagina} totalPaginas={totalPaginas} onCambiarPagina={setPagina} />

      {modalAbierto && (
        <ExperienciaFormModal
          experienciaExistente={experienciaEnEdicion}
          onGuardado={alGuardar}
          onCancelar={() => setModalAbierto(false)}
        />
      )}
    </section>
  );
}
