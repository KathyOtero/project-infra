import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { experienciasApi } from '@/api/experienciasApi';
import { useAuth } from '@/auth/AuthContext';
import { ReservaModal } from '@/components/ReservaModal';
import { Spinner } from '@/components/Spinner';
import type { ExperienciaResponse } from '@/types/api';

const formatoMoneda = new Intl.NumberFormat('es-CO', {
  style: 'currency',
  currency: 'COP',
  maximumFractionDigits: 0,
});

/**
 * Detalle publico de una experiencia. Si el usuario autenticado es VIAJERO y
 * hay cupo disponible, puede abrir el ReservaModal para confirmar su
 * reserva; al confirmar se recarga la experiencia (el cupo cambia).
 */
export function ExperienciaDetallePage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { sesion, estaAutenticado } = useAuth();
  const [experiencia, setExperiencia] = useState<ExperienciaResponse | null>(null);
  const [cargando, setCargando] = useState(true);
  const [noEncontrada, setNoEncontrada] = useState(false);
  const [modalAbierto, setModalAbierto] = useState(false);
  const [reservaOk, setReservaOk] = useState(false);

  function cargar() {
    if (!id) return;
    experienciasApi
      .obtenerPorId(Number(id))
      .then(setExperiencia)
      .catch(() => setNoEncontrada(true))
      .finally(() => setCargando(false));
  }

  useEffect(cargar, [id]);

  if (cargando) {
    return <Spinner />;
  }

  if (noEncontrada || !experiencia) {
    return (
      <section className="text-center">
        <h1 className="text-2xl font-semibold text-slate-800">Experiencia no encontrada</h1>
        <button onClick={() => navigate('/')} className="mt-4 text-caribe-blue hover:underline">
          Volver al listado
        </button>
      </section>
    );
  }

  const sinCupo = experiencia.cupoDisponible <= 0;
  const puedeReservar = estaAutenticado && sesion?.rol === 'VIAJERO' && !sinCupo;

  return (
    <section className="mx-auto max-w-2xl">
      <div className="h-56 w-full overflow-hidden rounded-lg bg-slate-100">
        {experiencia.fotoUrl ? (
          <img src={experiencia.fotoUrl} alt={experiencia.titulo} className="h-full w-full object-cover" />
        ) : (
          <div className="flex h-full items-center justify-center text-5xl">🏖️</div>
        )}
      </div>

      <div className="mt-4 flex items-center justify-between text-sm text-slate-500">
        <span>
          {experiencia.ciudad} · {experiencia.categoria}
        </span>
        <span>
          {experiencia.fecha} · {experiencia.hora}
        </span>
      </div>

      <h1 className="mt-2 text-2xl font-semibold text-slate-800">{experiencia.titulo}</h1>
      <p className="mt-2 text-slate-600">{experiencia.descripcion}</p>

      <div className="mt-4 flex items-center justify-between rounded-md border border-slate-200 p-4">
        <div>
          <p className="text-xs text-slate-400">Guiado por</p>
          <p className="font-medium text-slate-700">{experiencia.guiaNombre}</p>
        </div>
        <div className="text-right">
          <p className="text-xl font-semibold text-slate-800">
            {formatoMoneda.format(experiencia.precio)}
          </p>
          <p className={`text-xs ${sinCupo ? 'text-red-500' : 'text-slate-500'}`}>
            {sinCupo ? 'Sin cupo disponible' : `${experiencia.cupoDisponible} cupos disponibles`}
          </p>
        </div>
      </div>

      {puedeReservar ? (
        <button
          onClick={() => setModalAbierto(true)}
          className="mt-6 w-full rounded-md bg-caribe-coral px-4 py-2 font-medium text-white hover:opacity-90 transition"
        >
          Reservar
        </button>
      ) : !estaAutenticado ? (
        <p className="mt-6 text-center text-sm text-slate-500">
          <Link to="/login" className="text-caribe-blue hover:underline">
            Inicia sesion
          </Link>{' '}
          como Viajero para reservar esta experiencia.
        </p>
      ) : sinCupo ? (
        <p className="mt-6 text-center text-sm text-red-500">Esta experiencia ya no tiene cupo.</p>
      ) : null}

      {reservaOk && (
        <p className="mt-4 rounded-md border border-green-200 bg-green-50 px-3 py-2 text-center text-sm text-green-700">
          ¡Reserva confirmada! Puedes verla en{' '}
          <Link to="/viajero/reservas" className="underline">
            Mis reservas
          </Link>
          .
        </p>
      )}

      {modalAbierto && experiencia && (
        <ReservaModal
          experiencia={experiencia}
          onCerrar={() => setModalAbierto(false)}
          onReservada={() => {
            setModalAbierto(false);
            setReservaOk(true);
            cargar();
          }}
        />
      )}
    </section>
  );
}
