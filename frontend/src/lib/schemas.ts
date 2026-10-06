import { z } from 'zod';

/**
 * Esquemas de validacion Zod, espejo de las anotaciones de Bean Validation
 * del backend (jakarta.validation en los DTO de web/dto/**). Validar en el
 * cliente da feedback instantaneo al usuario; el backend sigue siendo la
 * fuente de verdad (vuelve a validar todo, nunca se confia solo en el
 * frontend).
 */

export const loginSchema = z.object({
  email: z.string().min(1, 'El email es obligatorio').email('El email no tiene un formato valido'),
  password: z.string().min(1, 'La contrasena es obligatoria'),
});

export type LoginFormData = z.infer<typeof loginSchema>;

export const registroSchema = z.object({
  nombre: z.string().min(1, 'El nombre es obligatorio'),
  apellido: z.string().min(1, 'El apellido es obligatorio'),
  email: z.string().min(1, 'El email es obligatorio').email('El email no tiene un formato valido'),
  password: z.string().min(8, 'La contrasena debe tener minimo 8 caracteres'),
  rol: z.enum(['GUIA', 'VIAJERO'], { message: 'Selecciona un rol' }),
});

export type RegistroFormData = z.infer<typeof registroSchema>;

export const experienciaSchema = z.object({
  titulo: z
    .string()
    .min(1, 'El titulo es obligatorio')
    .max(150, 'El titulo no puede superar 150 caracteres'),
  descripcion: z.string().min(1, 'La descripcion es obligatoria'),
  precio: z.coerce.number().min(0, 'El precio no puede ser negativo'),
  cupoMax: z.coerce.number().int().positive('El cupo maximo debe ser mayor a 0'),
  fecha: z.string().min(1, 'La fecha es obligatoria'),
  hora: z.string().min(1, 'La hora es obligatoria'),
  fotoUrl: z.string().optional().or(z.literal('')),
  ciudadId: z.coerce.number({ message: 'La ciudad es obligatoria' }).positive('Selecciona una ciudad'),
  categoriaId: z.coerce
    .number({ message: 'La categoria es obligatoria' })
    .positive('Selecciona una categoria'),
});

export type ExperienciaFormData = z.infer<typeof experienciaSchema>;
