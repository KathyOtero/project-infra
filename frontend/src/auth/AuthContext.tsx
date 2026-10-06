import { createContext, useContext, useMemo, useState, type ReactNode } from 'react';
import { authApi } from '@/api/authApi';
import { clearSession, getSession, saveSession, type SesionUsuario } from '@/auth/tokenStorage';
import type { LoginRequest } from '@/types/api';

interface AuthContextValue {
  sesion: SesionUsuario | null;
  estaAutenticado: boolean;
  login: (credenciales: LoginRequest) => Promise<SesionUsuario>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

/**
 * Contexto global de autenticacion. Se inicializa leyendo localStorage para
 * que un refresh de pagina no cierre la sesion del usuario (persistencia
 * simple, suficiente para el alcance del proyecto; en un caso real se
 * validaria ademas la expiracion del JWT contra la fecha actual).
 */
export function AuthProvider({ children }: { children: ReactNode }) {
  const [sesion, setSesion] = useState<SesionUsuario | null>(() => getSession());

  const login = async (credenciales: LoginRequest) => {
    const response = await authApi.login(credenciales);
    const nuevaSesion = saveSession(response);
    setSesion(nuevaSesion);
    return nuevaSesion;
  };

  const logout = () => {
    clearSession();
    setSesion(null);
  };

  const value = useMemo<AuthContextValue>(
    () => ({ sesion, estaAutenticado: sesion !== null, login, logout }),
    [sesion],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth debe usarse dentro de un <AuthProvider>');
  }
  return context;
}
