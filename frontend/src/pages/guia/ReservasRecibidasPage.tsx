import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { reservasApi } from '@/api/reservasApi';
import { extraerMensajeError } from '@/lib/errors';
import { Paginador } from '@/components/Paginador';
import { Spinner } from '@/components/Spinner';
import type { ReservaResponse } from '@/types/api';

const formatoFecha = new Intl.DateTimeFormat('es-CO', {
  dateStyle: 'medium',
  timeStyle: 'short',
});

function EstadoBadge({ estado }: { estado: string }) {
  const estilos: Record<string, string> = {
    PENDIENTE: 'bg-yellow-50 text-yellow-700 border-yellow-200',
    CONFIRMADA: 'bg-green-50 text-green-700 border-green-200',
    CANCELADA: 'bg-slate-100 text-slate-500 border-slate-200',
  };
  return (
    <span className={`rounded-full border px-2 py-0.5 text-xs font-medium ${estilos[estado] ?? ''}`}>
      {estado}
    </span>
  );
}

/**
 * Panel de solo lectura del GUIA: reservas recibidas en todas sus
 * experiencias. El guia no cancela reservas de sus viajeros (esa accion es
 * exclusiva del viajero, ver ReservaServiceImpl.cancelar), asi que aqui no
 * hay botones de accion.
 */
export function ReservasRecibidasPage() {
  const [reservas, setReservas] = useState<ReservaResponse[]>([]);
  const [pagina, setPagina] = useState(0);
  const [totalPaginas, setTotalPaginas] = useState(0);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setCargando(true);
    reservasApi
      .reservasRecibidas(pagina)
      .then((res) => {
        setReservas(res.contenido);
        setTotalPaginas(res.totalPaginas);
      })
      .catch((err) => setError(extraerMensajeError(err)))
      .finally(() => setCargando(false));
  }, [pagina]);

  return (
    <section>
      <h1 className="text-2xl font-semibold text-slate-800">Reservas recibidas</h1>
      <p className="mt-1 text-slate-500">Reservas que los viajeros han hecho en tus experiencias.</p>

      {error && (
        <p className="mt-4 rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-600">
          {error}
        </p>
      )}

      {cargando ? (
        <Spinner />
      ) : reservas.length === 0 ? (
        <p className="mt-6 text-center text-slate-400">Aun no has recibido reservas.</p>
      ) : (
        <div className="mt-6 space-y-3">
          {reservas.map((r) => (
            <div
              key={r.id}
              className="flex items-center justify-between rounded-md border border-slate-200 p-4"
            >
              <div>
                <Link to={`/experiencias/${r.experienciaId}`} className="font-medium text-slate-800 hover:underline">
                  {r.experienciaTitulo}
                </Link>
                <p className="mt-1 text-sm text-slate-500">
                  Reservado por <span className="font-medium">{r.viajeroNombre}</span> · {r.cantidadPersonas}{' '}
                  {r.cantidadPersonas === 1 ? 'persona' : 'personas'} · {formatoFecha.format(new Date(r.fechaReserva))}
                </p>
              </div>
              <EstadoBadge estado={r.estado} />
            </div>
          ))}
        </div>
      )}

      <Paginador paginaActual={pagina} totalPaginas={totalPaginas} onCambiarPagina={setPagina} />
    </section>
  );
}
