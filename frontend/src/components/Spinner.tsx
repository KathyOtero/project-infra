interface SpinnerProps {
  texto?: string;
}

/** Indicador de carga reutilizable, reemplaza los "Cargando..." de texto plano. */
export function Spinner({ texto = 'Cargando...' }: SpinnerProps) {
  return (
    <div className="flex flex-col items-center justify-center gap-3 py-12 text-slate-400">
      <div
        className="h-8 w-8 animate-spin rounded-full border-4 border-slate-200 border-t-caribe-blue"
        role="status"
        aria-label="cargando"
      />
      <span className="text-sm">{texto}</span>
    </div>
  );
}
