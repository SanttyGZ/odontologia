export type ApiErrorCode =
  | 'VALIDATION_ERROR'
  | 'INVALID_LOGIN_REQUEST'
  | 'INVALID_CREDENTIALS'
  | 'SESSION_REQUIRED'
  | 'CSRF_INVALID'
  | 'ACCESS_NOT_CONFIGURED'
  | 'ACCESS_UNAVAILABLE'
  | 'MOTIVO_NOT_FOUND'
  | 'BLOQUEO_NOT_FOUND'
  | 'CONFIGURATION_CONFLICT'
  | 'AGENDA_BUSY'
  | 'TECHNICAL_FAILURE'
  | 'NETWORK_ERROR'
  | 'INVALID_RESPONSE';

export interface CsrfResponse {
  token: string;
  headerName: string;
}

export interface SessionResponse {
  autenticado: boolean;
}

export interface Motivo {
  id: string;
  nombre: string;
  duracionMinutos: number;
}

export interface MotivoInput {
  nombre: string;
  duracionMinutos: number;
}

export type FranjaTipo = 'ATENCION' | 'PAUSA';

export interface FranjaSemanal {
  id: string;
  diaSemana: number;
  horaInicio: string;
  horaFin: string;
  tipo: FranjaTipo;
}

export interface HorarioResponse {
  franjas: FranjaSemanal[];
}

export interface Bloqueo {
  id: string;
  inicio: string;
  fin: string;
  descripcion: string | null;
}

export interface BloqueoInput {
  inicio: string;
  fin: string;
  descripcion: string | null;
}

export interface ErrorResponse {
  codigo: string;
}

export type ApiResult<T> =
  | { ok: true; data: T }
  | { ok: false; status: number; codigo: ApiErrorCode; message: string };

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}

function isNonEmptyString(value: unknown): value is string {
  return typeof value === 'string' && value.trim().length > 0;
}

export function isValidDuration(value: unknown): value is number {
  return (
    typeof value === 'number' &&
    Number.isInteger(value) &&
    value >= 15 &&
    value <= 240 &&
    value % 15 === 0
  );
}

export function isCsrfResponse(value: unknown): value is CsrfResponse {
  if (!isRecord(value)) {
    return false;
  }
  return isNonEmptyString(value.token) && isNonEmptyString(value.headerName);
}

export function isSessionResponse(value: unknown): value is SessionResponse {
  if (!isRecord(value)) {
    return false;
  }
  return typeof value.autenticado === 'boolean';
}

export function isMotivo(value: unknown): value is Motivo {
  if (!isRecord(value)) {
    return false;
  }
  return (
    isNonEmptyString(value.id) &&
    isNonEmptyString(value.nombre) &&
    isValidDuration(value.duracionMinutos)
  );
}

export function isMotivoList(value: unknown): value is Motivo[] {
  return Array.isArray(value) && value.every(isMotivo);
}

export function isFranjaSemanal(value: unknown): value is FranjaSemanal {
  if (!isRecord(value)) {
    return false;
  }
  const dia = value.diaSemana;
  const tipo = value.tipo;
  return (
    isNonEmptyString(value.id) &&
    typeof dia === 'number' &&
    Number.isInteger(dia) &&
    dia >= 1 &&
    dia <= 7 &&
    isNonEmptyString(value.horaInicio) &&
    isNonEmptyString(value.horaFin) &&
    (tipo === 'ATENCION' || tipo === 'PAUSA')
  );
}

export function isHorarioResponse(value: unknown): value is HorarioResponse {
  if (!isRecord(value)) {
    return false;
  }
  return Array.isArray(value.franjas) && value.franjas.every(isFranjaSemanal);
}

export function isBloqueo(value: unknown): value is Bloqueo {
  if (!isRecord(value)) {
    return false;
  }
  const desc = value.descripcion;
  return (
    isNonEmptyString(value.id) &&
    isNonEmptyString(value.inicio) &&
    isNonEmptyString(value.fin) &&
    (desc === null || typeof desc === 'string')
  );
}

export function isBloqueoList(value: unknown): value is Bloqueo[] {
  return Array.isArray(value) && value.every(isBloqueo);
}

export function isErrorResponse(value: unknown): value is ErrorResponse {
  if (!isRecord(value)) {
    return false;
  }
  return isNonEmptyString(value.codigo);
}

const KNOWN_ERROR_CODES: ReadonlySet<string> = new Set<ApiErrorCode>([
  'VALIDATION_ERROR',
  'INVALID_LOGIN_REQUEST',
  'INVALID_CREDENTIALS',
  'SESSION_REQUIRED',
  'CSRF_INVALID',
  'ACCESS_NOT_CONFIGURED',
  'ACCESS_UNAVAILABLE',
  'MOTIVO_NOT_FOUND',
  'BLOQUEO_NOT_FOUND',
  'CONFIGURATION_CONFLICT',
  'AGENDA_BUSY',
  'TECHNICAL_FAILURE',
  'NETWORK_ERROR',
  'INVALID_RESPONSE',
]);

export function toApiErrorCode(raw: string): ApiErrorCode {
  if (KNOWN_ERROR_CODES.has(raw)) {
    return raw as ApiErrorCode;
  }
  return 'TECHNICAL_FAILURE';
}
