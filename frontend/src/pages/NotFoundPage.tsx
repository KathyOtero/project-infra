import { Link } from 'react-router-dom';

/** Pagina 404: se muestra cuando ninguna ruta del router coincide. */
export function NotFoundPage() {
  return (
    <section className="flex flex-col items-center justify-center gap-4 py-20 text-center">
      <span className="text-6xl">🏝️</span>
      <h1 className="text-3xl font-semibold text-slate-800">Página no encontrada</h1>
      <p className="max-w-md text-slate-500">
        Esta ruta no existe o la experiencia que buscas ya no está disponible.
      </p>
      <Link
        to="/"
        className="mt-2 rounded-md bg-caribe-blue px-4 py-2 text-sm font-medium text-white hover:bg-caribe-blue-dark transition"
      >
        Volver al inicio
      </Link>
    </section>
  );
}
