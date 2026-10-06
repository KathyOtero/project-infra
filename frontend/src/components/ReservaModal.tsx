import { useState } from 'react';
import { reservasApi } from '@/api/reservasApi';
import { extraerMensajeError } from '@/lib/errors';
import { AlertaError } from '@/components/AlertaError';
import { SubmitButton } from '@/components/SubmitButton';
import type { ExperienciaResponse } from '@/types/api';

interface ReservaModalProps {
  experiencia: ExperienciaResponse;
  onCerrar: () => void;
  onReservada: () => void;
}

/**
 * Modal simple (sin react-hook-form: un solo campo numerico) para confirmar
 * la cantidad de personas antes de reservar. El backend es la fuente de
 * verdad del cupo (bloqueo pesimista en ReservaServiceImpl.reservar), asi que
 * el cliente solo valida que el numero sea positivo y no supere el cupo que
 * ya conoce del detalle de la experiencia.
 */
export function ReservaModal({ experiencia, onCerrar, onReservada }: ReservaModalProps) {
  const [cantidadPersonas, setCantidadPersonas] = useState(1);
  const [enviando, setEnviando] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const total = cantidadPersonas * experiencia.precio;

  async function confirmar() {
    setError(null);

    if (cantidadPersonas < 1) {
      setError('La cantidad de personas debe ser mayor a 0.');
      return;
    }
    if (cantidadPersonas > experiencia.cupoDisponible) {
      setError(`Solo hay ${experiencia.cupoDisponible} cupos disponibles.`);
      return;
    }

    setEnviando(true);
    try {
      await reservasApi.reservar({ experienciaId: experiencia.id, cantidadPersonas });
      onReservada();
    } catch (err) {
      setError(extraerMensajeError(err));
    } finally {
      setEnviando(false);
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 px-4">
      <div className="w-full max-w-sm rounded-lg bg-white p-6 shadow-xl">
        <h2 className="text-lg font-semibold text-slate-800">Reservar experiencia</h2>
        <p className="mt-1 text-sm text-slate-500">{experiencia.titulo}</p>

        <label htmlFor="cantidadPersonas" className="mt-4 block text-sm font-medium text-slate-700">
          Cantidad de personas
        </label>
        <input
          id="cantidadPersonas"
          type="number"
          min={1}
          max={experiencia.cupoDisponible}
          value={cantidadPersonas}
          onChange={(e) => setCantidadPersonas(Number(e.target.value))}
          className="mt-1 w-full rounded-md border border-slate-300 px-3 py-2"
        />
        <p className="mt-1 text-xs text-slate-400">
          {experiencia.cupoDisponible} cupos disponibles
        </p>

        <div className="mt-3 flex items-center justify-between rounded-md bg-slate-50 px-3 py-2 text-sm">
          <span className="text-slate-500">Total</span>
          <span className="font-semibold text-slate-800">
            {new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 }).format(
              total,
            )}
          </span>
        </div>

        <AlertaError mensaje={error} />

        <div className="mt-5 flex justify-end gap-3">
          <button
            type="button"
            onClick={onCerrar}
            disabled={enviando}
            className="rounded-md border border-slate-300 px-4 py-2 text-sm text-slate-600 hover:bg-slate-50"
          >
            Cancelar
          </button>
          <SubmitButton type="button" cargando={enviando} onClick={confirmar}>
            Confirmar reserva
          </SubmitButton>
        </div>
      </div>
    </div>
  );
}
