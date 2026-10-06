import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '@/auth/AuthContext';
import type { Rol } from '@/types/api';

interface RutaProtegidaProps {
  /** Si se indica, ademas de estar autenticado, el usuario debe tener este rol. */
  rolRequerido?: Rol;
}

/**
 * Guard de rutas: usado en las rutas del panel de Guia (crear/editar
 * experiencias) y del Viajero (mis reservas). Redirige a /login si no hay
 * sesion, o a la home si el rol no coincide (ej. un Viajero intentando
 * entrar al panel de Guia).
 */
export function RutaProtegida({ rolRequerido }: RutaProtegidaProps) {
  const { sesion, estaAutenticado } = useAuth();
  const location = useLocation();

  if (!estaAutenticado) {
    return <Navigate to="/login" state={{ from: location.pathname }} replace />;
  }

  if (rolRequerido && sesion?.rol !== rolRequerido) {
    return <Navigate to="/" replace />;
  }

  return <Outlet />;
}
