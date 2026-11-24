import { describe, expect, it } from 'vitest';
import {
  isBloqueo,
  isBloqueoList,
  isCsrfResponse,
  isErrorResponse,
  isFranjaSemanal,
  isHorarioResponse,
  isMotivo,
  isMotivoList,
  isSessionResponse,
  isValidDuration,
} from './odontologia';

describe('type guards de odontologia', () => {
  it('valida CsrfResponse', () => {
    expect(isCsrfResponse({ token: 'abc', headerName: 'X-CSRF-TOKEN' })).toBe(true);
    expect(isCsrfResponse({ token: '', headerName: 'X-CSRF-TOKEN' })).toBe(false);
    expect(isCsrfResponse({ token: 123, headerName: 'X-CSRF-TOKEN' })).toBe(false);
    expect(isCsrfResponse(null)).toBe(false);
  });

  it('valida SessionResponse', () => {
    expect(isSessionResponse({ autenticado: true })).toBe(true);
    expect(isSessionResponse({ autenticado: false })).toBe(true);
    expect(isSessionResponse({ autenticado: 'true' })).toBe(false);
    expect(isSessionResponse(undefined)).toBe(false);
  });

  it('valida duraciones enteras admitidas (15..240, múltiplo de 15)', () => {
    expect(isValidDuration(15)).toBe(true);
    expect(isValidDuration(30)).toBe(true);
    expect(isValidDuration(240)).toBe(true);
    expect(isValidDuration(0)).toBe(false);
    expect(isValidDuration(10)).toBe(false);
    expect(isValidDuration(255)).toBe(false);
    expect(isValidDuration(30.5)).toBe(false);
    expect(isValidDuration('45')).toBe(false);
    expect(isValidDuration(Number.NaN)).toBe(false);
  });

  it('valida Motivo y MotivoList', () => {
    const valid = { id: '11111111-1111-1111-1111-111111111111', nombre: 'Limpieza', duracionMinutos: 30 };
    expect(isMotivo(valid)).toBe(true);
    expect(isMotivoList([valid])).toBe(true);
    expect(isMotivo({ ...valid, duracionMinutos: 30.5 })).toBe(false);
    expect(isMotivo({ ...valid, nombre: '' })).toBe(false);
    expect(isMotivoList([valid, { id: 'x' }])).toBe(false);
  });

  it('valida FranjaSemanal y HorarioResponse', () => {
    const franja = {
      id: '22222222-2222-2222-2222-222222222222',
      diaSemana: 1,
      horaInicio: '09:00:00',
      horaFin: '12:00:00',
      tipo: 'ATENCION',
    };
    expect(isFranjaSemanal(franja)).toBe(true);
    expect(isHorarioResponse({ franjas: [franja] })).toBe(true);
    expect(isFranjaSemanal({ ...franja, diaSemana: 0 })).toBe(false);
    expect(isFranjaSemanal({ ...franja, diaSemana: 8 })).toBe(false);
    expect(isFranjaSemanal({ ...franja, tipo: 'OTRO' })).toBe(false);
    expect(isHorarioResponse({ franjas: 'no-array' })).toBe(false);
  });

  it('valida Bloqueo y BloqueoList con descripcion opcional o null', () => {
    const b1 = {
      id: '33333333-3333-3333-3333-333333333333',
      inicio: '2026-12-01T12:00:00Z',
      fin: '2026-12-01T14:00:00Z',
      descripcion: 'Congreso',
    };
    const b2 = { ...b1, descripcion: null };
    expect(isBloqueo(b1)).toBe(true);
    expect(isBloqueo(b2)).toBe(true);
    expect(isBloqueoList([b1, b2])).toBe(true);
    expect(isBloqueo({ ...b1, inicio: 123 })).toBe(false);
  });

  it('valida ErrorResponse', () => {
    expect(isErrorResponse({ codigo: 'VALIDATION_ERROR' })).toBe(true);
    expect(isErrorResponse({ codigo: '' })).toBe(false);
    expect(isErrorResponse({})).toBe(false);
  });
});
