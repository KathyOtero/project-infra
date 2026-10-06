import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '@/auth/AuthContext';
import { FormInput } from '@/components/FormInput';
import { SubmitButton } from '@/components/SubmitButton';
import { AlertaError } from '@/components/AlertaError';
import { loginSchema, type LoginFormData } from '@/lib/schemas';
import { extraerMensajeError } from '@/lib/errors';
import type { Rol } from '@/types/api';

/** A donde redirigir segun el rol, despues de un login exitoso. */
function rutaInicioPorRol(rol: Rol): string {
  return rol === 'GUIA' ? '/guia/experiencias' : '/';
}

export function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [errorServidor, setErrorServidor] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginFormData>({ resolver: zodResolver(loginSchema) });

  // Si el usuario venia de una ruta protegida (ej. quiso reservar sin sesion),
  // lo regresamos ahi despues de loguearse. Si no, usamos la home por rol.
  const destinoOriginal = (location.state as { from?: string } | null)?.from;

  const onSubmit = async (data: LoginFormData) => {
    setErrorServidor(null);
    try {
      const sesion = await login(data);
      navigate(destinoOriginal ?? rutaInicioPorRol(sesion.rol), { replace: true });
    } catch (error) {
      setErrorServidor(extraerMensajeError(error, 'Email o contrasena incorrectos.'));
    }
  };

  return (
    <section className="max-w-sm mx-auto">
      <h1 className="text-2xl font-semibold text-slate-800">Iniciar sesion</h1>
      <p className="mt-1 text-sm text-slate-500">
        Ingresa a tu cuenta de CaribeXperience como Guia o Viajero.
      </p>

      <form onSubmit={handleSubmit(onSubmit)} className="mt-6 flex flex-col gap-4">
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
          autoComplete="current-password"
          error={errors.password?.message}
          {...register('password')}
        />

        <AlertaError mensaje={errorServidor} />

        <SubmitButton cargando={isSubmitting}>Iniciar sesion</SubmitButton>
      </form>

      <p className="mt-4 text-sm text-slate-500">
        No tienes cuenta?{' '}
        <Link to="/registro" className="text-caribe-blue hover:underline">
          Registrate aqui
        </Link>
      </p>
    </section>
  );
}
