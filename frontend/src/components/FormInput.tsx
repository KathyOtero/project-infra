import { forwardRef, type InputHTMLAttributes } from 'react';

interface FormInputProps extends InputHTMLAttributes<HTMLInputElement> {
  label: string;
  error?: string;
}

/**
 * Input de formulario reutilizable: label + mensaje de error debajo.
 * forwardRef es necesario para que funcione con react-hook-form (register).
 */
export const FormInput = forwardRef<HTMLInputElement, FormInputProps>(
  ({ label, error, id, ...props }, ref) => {
    const inputId = id ?? props.name;
    return (
      <div className="flex flex-col gap-1">
        <label htmlFor={inputId} className="text-sm font-medium text-slate-700">
          {label}
        </label>
        <input
          id={inputId}
          ref={ref}
          className={`rounded-md border px-3 py-2 text-sm outline-none transition focus:ring-2 focus:ring-caribe-blue ${
            error ? 'border-red-400' : 'border-slate-300'
          }`}
          {...props}
        />
        {error && <span className="text-xs text-red-500">{error}</span>}
      </div>
    );
  },
);
FormInput.displayName = 'FormInput';
