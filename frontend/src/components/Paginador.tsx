interface PaginadorProps {
  paginaActual: number;
  totalPaginas: number;
  onCambiarPagina: (nuevaPagina: number) => void;
}

/** Paginador simple: anterior / numero de pagina actual / siguiente. */
export function Paginador({ paginaActual, totalPaginas, onCambiarPagina }: PaginadorProps) {
  if (totalPaginas <= 1) return null;

  return (
    <div className="mt-6 flex items-center justify-center gap-3 text-sm">
      <button
        onClick={() => onCambiarPagina(paginaActual - 1)}
        disabled={paginaActual === 0}
        className="rounded-md border border-slate-300 px-3 py-1 disabled:opacity-40"
      >
        Anterior
      </button>
      <span className="text-slate-500">
        Pagina {paginaActual + 1} de {totalPaginas}
      </span>
      <button
        onClick={() => onCambiarPagina(paginaActual + 1)}
        disabled={paginaActual >= totalPaginas - 1}
        className="rounded-md border border-slate-300 px-3 py-1 disabled:opacity-40"
      >
        Siguiente
      </button>
    </div>
  );
}
