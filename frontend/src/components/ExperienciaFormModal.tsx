import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useEffect, useState } from 'react';
import { z } from 'zod';
import { experienciasApi } from '@/api/experienciasApi';
import { FormInput } from '@/components/FormInput';
import { SubmitButton } from '@/components/SubmitButton';
import { AlertaError } from '@/components/AlertaError';
import { useCatalogos } from '@/lib/useCatalogos';
import { experienciaSchema, type ExperienciaFormData } from '@/lib/schemas';
import { extraerMensajeError } from '@/lib/errors';
import type { ExperienciaResponse } from '@/types/api';

interface ExperienciaFormModalProps {
  /** Si viene una experiencia, el formulario opera en modo edicion (PUT); si no, en modo creacion (POST). */
  experienciaExistente?: ExperienciaResponse | null;
  onGuardado: () => void;
  onCancelar: () => void;
}

/**
 * Modal simple (overlay) para crear o editar una experiencia. Se reutiliza
 * el mismo esquema Zod y la misma peticion HTTP para ambos casos; solo
 * cambia si se llama a experienciasApi.crear o .actualizar.
 */
export function ExperienciaFormModal({
  experienciaExistente,
  onGuardado,
  onCancelar,
}: ExperienciaFormModalProps) {
  const { ciudades, categorias } = useCatalogos();
  const [errorServidor, setErrorServidor] = useState<string | null>(null);
  const esEdicion = Boolean(experienciaExistente);

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<z.input<typeof experienciaSchema>, unknown, ExperienciaFormData>({
    resolver: zodResolver(experienciaSchema),
  });

  useEffect(() => {
    if (experienciaExistente) {
      reset({
        titulo: experienciaExistente.titulo,
        descripcion: experienciaExistente.descripcion,
        precio: experienciaExistente.precio,
        cupoMax: experienciaExistente.cupoMax,
        fecha: experienciaExistente.fecha,
        hora: experienciaExistente.hora,
        fotoUrl: experienciaExistente.fotoUrl ?? '',
        ciudadId: experienciaExistente.ciudadId,
        categoriaId: experienciaExistente.categoriaId,
      });
    }
    // Solo se debe re-poblar el formulario cuando cambia la experiencia a
    // editar (su id), no cuando cambian las referencias de ciudades/
    // categorias (useCatalogos crea un nuevo array al resolver su fetch).
    // Incluirlas en las dependencias causaba un reset() tardio que
    // sobreescribia lo que el usuario ya habia escrito en el formulario.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [experienciaExistente?.id, reset]);

  const onSubmit = async (data: ExperienciaFormData) => {
    setErrorServidor(null);
    const payload = { ...data, fotoUrl: data.fotoUrl || null };
    try {
      if (esEdicion && experienciaExistente) {
        await experienciasApi.actualizar(experienciaExistente.id, payload);
      } else {
        await experienciasApi.crear(payload);
      }
      onGuardado();
    } catch (error) {
      setErrorServidor(extraerMensajeError(error, 'No se pudo guardar la experiencia.'));
    }
  };

  return (
    <div className="fixed inset-0 z-10 flex items-center justify-center bg-black/40 p-4">
      <div className="max-h-[90vh] w-full max-w-lg overflow-y-auto rounded-lg bg-white p-6 shadow-lg">
        <h2 className="text-xl font-semibold text-slate-800">
          {esEdicion ? 'Editar experiencia' : 'Nueva experiencia'}
        </h2>

        <form onSubmit={handleSubmit(onSubmit)} className="mt-4 flex flex-col gap-3">
          <FormInput label="Titulo" error={errors.titulo?.message} {...register('titulo')} />

          <div className="flex flex-col gap-1">
            <label className="text-sm font-medium text-slate-700">Descripcion</label>
            <textarea
              id="descripcion"
              rows={3}
              className="rounded-md border border-slate-300 px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-caribe-blue"
              {...register('descripcion')}
            />
            {errors.descripcion && (
              <span className="text-xs text-red-500">{errors.descripcion.message}</span>
            )}
          </div>

          <div className="flex gap-3">
            <FormInput
              label="Precio (COP)"
              type="number"
              step="0.01"
              error={errors.precio?.message}
              {...register('precio')}
            />
            <FormInput
              label="Cupo maximo"
              type="number"
              error={errors.cupoMax?.message}
              {...register('cupoMax')}
            />
          </div>

          <div className="flex gap-3">
            <FormInput label="Fecha" type="date" error={errors.fecha?.message} {...register('fecha')} />
            <FormInput label="Hora" type="time" error={errors.hora?.message} {...register('hora')} />
          </div>

          <FormInput
            label="URL de foto (opcional)"
            error={errors.fotoUrl?.message}
            {...register('fotoUrl')}
          />

          <div className="flex gap-3">
            <div className="flex flex-1 flex-col gap-1">
              <label className="text-sm font-medium text-slate-700">Ciudad</label>
              <select
                className="rounded-md border border-slate-300 px-3 py-2 text-sm"
                {...register('ciudadId')}
              >
                <option value="">Selecciona...</option>
                {ciudades.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.nombre}
                  </option>
                ))}
              </select>
              {errors.ciudadId && (
                <span className="text-xs text-red-500">{errors.ciudadId.message}</span>
              )}
            </div>

            <div className="flex flex-1 flex-col gap-1">
              <label className="text-sm font-medium text-slate-700">Categoria</label>
              <select
                className="rounded-md border border-slate-300 px-3 py-2 text-sm"
                {...register('categoriaId')}
              >
                <option value="">Selecciona...</option>
                {categorias.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.nombre}
                  </option>
                ))}
              </select>
              {errors.categoriaId && (
                <span className="text-xs text-red-500">{errors.categoriaId.message}</span>
              )}
            </div>
          </div>

          <AlertaError mensaje={errorServidor} />

          <div className="mt-2 flex justify-end gap-2">
            <button
              type="button"
              onClick={onCancelar}
              className="rounded-md border border-slate-300 px-4 py-2 text-sm"
            >
              Cancelar
            </button>
            <SubmitButton cargando={isSubmitting}>{esEdicion ? 'Guardar cambios' : 'Crear'}</SubmitButton>
          </div>
        </form>
      </div>
    </div>
  );
}
