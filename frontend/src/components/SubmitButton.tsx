import type { ButtonHTMLAttributes } from 'react';

interface SubmitButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  cargando?: boolean;
}

/** Boton de submit con estado de "cargando" (deshabilitado + texto alternativo). */
export function SubmitButton({ cargando, children, disabled, ...props }: SubmitButtonProps) {
  return (
    <button
      type="submit"
      disabled={cargando || disabled}
      className="rounded-md bg-caribe-blue px-4 py-2 text-sm font-medium text-white transition hover:bg-caribe-blue-dark disabled:cursor-not-allowed disabled:opacity-60"
      {...props}
    >
      {cargando ? 'Procesando...' : children}
    </button>
  );
}
