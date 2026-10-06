import { Link } from 'react-router-dom';
import type { ExperienciaResponse } from '@/types/api';

const formatoMoneda = new Intl.NumberFormat('es-CO', {
  style: 'currency',
  currency: 'COP',
  maximumFractionDigits: 0,
});

interface ExperienciaCardProps {
  experiencia: ExperienciaResponse;
  /** Acciones extra (editar/cancelar) que solo aplican en el panel del Guia. */
  acciones?: React.ReactNode;
}

export function ExperienciaCard({ experiencia, acciones }: ExperienciaCardProps) {
  const sinCupo = experiencia.cupoDisponible <= 0;

  return (
    <div className="flex flex-col overflow-hidden rounded-lg border border-slate-200 bg-white shadow-sm transition hover:shadow-md">
      <div className="h-36 w-full bg-slate-100">
        {experiencia.fotoUrl ? (
          <img src={experiencia.fotoUrl} alt={experiencia.titulo} className="h-full w-full object-cover" />
        ) : (
          <div className="flex h-full items-center justify-center text-3xl">🏖️</div>
        )}
      </div>

      <div className="flex flex-1 flex-col gap-2 p-4">
        <div className="flex items-center justify-between text-xs text-slate-500">
          <span>{experiencia.ciudad}</span>
          <span className="rounded-full bg-caribe-blue/10 px-2 py-0.5 text-caribe-blue">
            {experiencia.categoria}
          </span>
        </div>

        <h3 className="font-semibold text-slate-800 line-clamp-1">{experiencia.titulo}</h3>
        <p className="text-sm text-slate-500 line-clamp-2">{experiencia.descripcion}</p>

        <div className="mt-auto flex items-center justify-between pt-2">
          <span className="font-semibold text-slate-800">{formatoMoneda.format(experiencia.precio)}</span>
          <span className={`text-xs ${sinCupo ? 'text-red-500' : 'text-slate-500'}`}>
            {sinCupo ? 'Sin cupo' : `${experiencia.cupoDisponible} cupos`}
          </span>
        </div>

        <p className="text-xs text-slate-400">
          {experiencia.fecha} · {experiencia.hora}
        </p>

        <Link
          to={`/experiencias/${experiencia.id}`}
          className="mt-2 rounded-md bg-caribe-blue px-3 py-1.5 text-center text-sm font-medium text-white transition hover:bg-caribe-blue-dark"
        >
          Ver detalle
        </Link>

        {acciones}
      </div>
    </div>
  );
}
