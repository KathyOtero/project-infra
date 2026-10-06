import { createBrowserRouter } from 'react-router-dom';
import { MainLayout } from '@/components/MainLayout';
import { RutaProtegida } from '@/routes/RutaProtegida';
import { HomePage } from '@/pages/HomePage';
import { LoginPage } from '@/pages/LoginPage';
import { RegistroPage } from '@/pages/RegistroPage';
import { ExperienciaDetallePage } from '@/pages/ExperienciaDetallePage';
import { MisExperienciasPage } from '@/pages/guia/MisExperienciasPage';
import { ReservasRecibidasPage } from '@/pages/guia/ReservasRecibidasPage';
import { MisReservasPage } from '@/pages/viajero/MisReservasPage';
import { NotFoundPage } from '@/pages/NotFoundPage';

export const router = createBrowserRouter([
  {
    path: '/',
    element: <MainLayout />,
    children: [
      { index: true, element: <HomePage /> },
      { path: 'login', element: <LoginPage /> },
      { path: 'registro', element: <RegistroPage /> },
      { path: 'experiencias/:id', element: <ExperienciaDetallePage /> },

      {
        element: <RutaProtegida rolRequerido="GUIA" />,
        children: [
          { path: 'guia/experiencias', element: <MisExperienciasPage /> },
          { path: 'guia/reservas', element: <ReservasRecibidasPage /> },
        ],
      },
      {
        element: <RutaProtegida rolRequerido="VIAJERO" />,
        children: [{ path: 'viajero/reservas', element: <MisReservasPage /> }],
      },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
]);
