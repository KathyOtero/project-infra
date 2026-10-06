interface AlertaErrorProps {
  mensaje: string | null;
}

/** Caja de error simple, usada en formularios para mostrar errores del backend. */
export function AlertaError({ mensaje }: AlertaErrorProps) {
  if (!mensaje) return null;
  return (
    <div className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-600">
      {mensaje}
    </div>
  );
}
