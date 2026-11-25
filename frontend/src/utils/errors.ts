import type { ApiErrorCode } from '../types/odontologia';

const ERROR_MESSAGES: Record<ApiErrorCode, string> = {
  VALIDATION_ERROR: 'Los datos ingresados no son válidos. Revisá los campos e intentá nuevamente.',
  INVALID_LOGIN_REQUEST: 'El formato de la solicitud de ingreso no es válido.',
  INVALID_CREDENTIALS: 'Usuario o contraseña incorrectos.',
  SESSION_REQUIRED: 'Tu sesión expiró o no está activa. Volvé a ingresar.',
  CSRF_INVALID: 'El token de seguridad venció o es inválido. Intentá la operación nuevamente.',
  ACCESS_DENIED: 'No tenés permisos para realizar esta operación.',
  ACCESS_NOT_CONFIGURED: 'El acceso interno del odontólogo no está configurado en este entorno.',
  ACCESS_UNAVAILABLE: 'El servicio de acceso interno no está disponible en este momento.',
  MOTIVO_NOT_FOUND: 'El motivo de consulta seleccionado ya no existe.',
  BLOQUEO_NOT_FOUND: 'El bloqueo seleccionado ya no existe en la agenda.',
  TURNO_NOT_FOUND: 'El turno seleccionado no existe en la agenda.',
  CONFIGURATION_CONFLICT: 'El cambio entra en conflicto con turnos confirmados vigentes.',
  SLOT_UNAVAILABLE: 'El horario elegido ya no está disponible. Seleccioná otra opción libre.',
  AGENDA_BUSY: 'La agenda está ocupada procesando otra operación. Reintentá manualmente en unos segundos.',
  TECHNICAL_FAILURE: 'Ocurrió un error técnico en el servidor. No se guardaron cambios.',
  NETWORK_ERROR: 'No se pudo conectar con el servidor. Verificá la conexión e intentá nuevamente.',
  INVALID_RESPONSE: 'La respuesta del servidor no tiene el formato esperado.',
};

export function getErrorMessage(codigo: ApiErrorCode): string {
  return ERROR_MESSAGES[codigo];
}
