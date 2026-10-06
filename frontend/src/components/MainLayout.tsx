import { useState } from 'react';
import { Link, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '@/auth/AuthContext';

/** Enlaces de navegacion segun el estado de autenticacion/rol, para no duplicar el JSX entre desktop y movil. */
function EnlacesNav({ onNavegar }: { onNavegar?: () => void }) {
  const { sesion, estaAutenticado, logout } = useAuth();

  return (
    <>
      <Link to="/" className="hover:underline" onClick={onNavegar}>
        Explorar
      </Link>

      {estaAutenticado && sesion?.rol === 'GUIA' && (
        <>
          <Link to="/guia/experiencias" className="hover:underline" onClick={onNavegar}>
            Mis experiencias
          </Link>
          <Link to="/guia/reservas" className="hover:underline" onClick={onNavegar}>
            Reservas recibidas
          </Link>
        </>
      )}

      {estaAutenticado && sesion?.rol === 'VIAJERO' && (
        <Link to="/viajero/reservas" className="hover:underline" onClick={onNavegar}>
          Mis reservas
        </Link>
      )}

      {estaAutenticado ? (
        <button
          onClick={() => {
            logout();
            onNavegar?.();
          }}
          className="rounded bg-white/10 px-3 py-1 text-left hover:bg-white/20 transition"
        >
          Salir ({sesion?.nombre})
        </button>
      ) : (
        <>
          <Link
            to="/login"
            onClick={onNavegar}
            className="rounded bg-white/10 px-3 py-1 hover:bg-white/20 transition"
          >
            Iniciar sesion
          </Link>
          <Link
            to="/registro"
            onClick={onNavegar}
            className="rounded bg-caribe-coral px-3 py-1 hover:opacity-90 transition"
          >
            Registrarme
          </Link>
        </>
      )}
    </>
  );
}

/**
 * Layout principal: barra de navegacion + contenido de la ruta activa
 * (via <Outlet />). Se muestra en todas las paginas. La navegacion colapsa
 * a un menu hamburguesa por debajo del breakpoint `md` (pantallas de
 * celular), y se cierra automaticamente al navegar a una nueva ruta.
 */
export function MainLayout() {
  const [menuAbierto, setMenuAbierto] = useState(false);
  const location = useLocation();

  return (
    <div className="min-h-screen flex flex-col">
      <header className="bg-caribe-blue text-white shadow-sm">
        <nav className="max-w-6xl mx-auto flex items-center justify-between px-4 py-3">
          <Link to="/" className="text-xl font-semibold tracking-tight" onClick={() => setMenuAbierto(false)}>
            CaribeXperience
          </Link>

          <div className="hidden md:flex items-center gap-4 text-sm">
            <EnlacesNav />
          </div>

          <button
            onClick={() => setMenuAbierto((v) => !v)}
            aria-label="Abrir menu"
            aria-expanded={menuAbierto}
            className="md:hidden rounded p-2 hover:bg-white/10"
          >
            <svg className="h-6 w-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              {menuAbierto ? (
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
              ) : (
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" />
              )}
            </svg>
          </button>
        </nav>

        {menuAbierto && (
          <div
            key={location.pathname}
            className="md:hidden flex flex-col gap-3 px-4 pb-4 text-sm"
          >
            <EnlacesNav onNavegar={() => setMenuAbierto(false)} />
          </div>
        )}
      </header>

      <main className="flex-1 max-w-6xl w-full mx-auto px-4 py-6">
        <Outlet />
      </main>

      <footer className="text-center text-xs text-slate-400 py-4">
        Proyecto academico UTB - Ingenieria de Sistemas - LOS PARRILLEROS
      </footer>
    </div>
  );
}
