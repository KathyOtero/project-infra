import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { Link, useNavigate } from 'react-router-dom';
import { usuariosApi } from '@/api/authApi';
import { useAuth } from '@/auth/AuthContext';
import { FormInput } from '@/components/FormInput';
import { SubmitButton } from '@/components/SubmitButton';
import { AlertaError } from '@/components/AlertaError';
import { registroSchema, type RegistroFormData } from '@/lib/schemas';
import { extraerMensajeError } from '@/lib/errors';

/**
 * El endpoint POST /api/usuarios/registro solo crea el usuario (no devuelve
 * token). Por eso, tras un registro exitoso, se llama automaticamente a
 * login() con las mismas credenciales para dejar al usuario ya autenticado
 * (mejor UX que mandarlo de vuelta al formulario de login).
 */
export function RegistroPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [errorServidor, setErrorServidor] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<RegistroFormData>({
    resolver: zodResolver(registroSchema),
    defaultValues: { rol: 'VIAJERO' },
  });

  const onSubmit = async (data: RegistroFormData) => {
    setErrorServidor(null);
    try {
      await usuariosApi.registrar(data);
      const sesion = await login({ email: data.email, password: data.password });
      navigate(sesion.rol === 'GUIA' ? '/guia/experiencias' : '/', { replace: true });
    } catch (error) {
      setErrorServidor(extraerMensajeError(error, 'No se pudo completar el registro.'));
    }
  };

  return (
    <section className="max-w-sm mx-auto">
      <h1 className="text-2xl font-semibold text-slate-800">Crear cuenta</h1>
      <p className="mt-1 text-sm text-slate-500">
        Unete a CaribeXperience como Guia (ofreces experiencias) o Viajero (las reservas).
      </p>

      <form onSubmit={handleSubmit(onSubmit)} className="mt-6 flex flex-col gap-4">
        <div className="flex gap-2">
          <FormInput
            label="Nombre"
            error={errors.nombre?.message}
            {...register('nombre')}
          />
          <FormInput
            label="Apellido"
            error={errors.apellido?.message}
            {...register('apellido')}
          />
        </div>

        <FormInput
          label="Email"
          type="email"
          autoComplete="email"
          error={errors.email?.message}
          {...register('email')}
        />
        <FormInput
          label="Contrasena"
          type="password"
          autoComplete="new-password"
          error={errors.password?.message}
          {...register('password')}
        />

        <div className="flex flex-col gap-1">
          <label className="text-sm font-medium text-slate-700">Quiero registrarme como</label>
          <select
            className="rounded-md border border-slate-300 px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-caribe-blue"
            {...register('rol')}
          >
            <option value="VIAJERO">Viajero (quiero reservar experiencias)</option>
            <option value="GUIA">Guia (quiero ofrecer experiencias)</option>
          </select>
          {errors.rol && <span className="text-xs text-red-500">{errors.rol.message}</span>}
        </div>

        <AlertaError mensaje={errorServidor} />

        <SubmitButton cargando={isSubmitting}>Crear cuenta</SubmitButton>
      </form>

      <p className="mt-4 text-sm text-slate-500">
        Ya tienes cuenta?{' '}
        <Link to="/login" className="text-caribe-blue hover:underline">
          Inicia sesion
        </Link>
      </p>
    </section>
  );
}
