import { describe, expect, it } from 'vitest';
import {
  isAgendaResponse,
  isAgendaTurnoResumen,
  isBloqueo,
  isBloqueoList,
  isCsrfResponse,
  isDisponibilidadPublicaResponse,
  isErrorResponse,
  isFranjaSemanal,
  isHorarioDisponible,
  isHorarioResponse,
  isMotivo,
  isMotivoList,
  isPacienteBasico,
  isReservaPublicaResponse,
  isSessionResponse,
  isTurnoDetalleResponse,
  isValidDuration,
  toApiErrorCode,
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

  it('reconoce SLOT_UNAVAILABLE y valida DisponibilidadPublicaResponse y ReservaPublicaResponse', () => {
    expect(toApiErrorCode('SLOT_UNAVAILABLE')).toBe('SLOT_UNAVAILABLE');

    const slot = { inicio: '2030-01-08T12:00:30Z', fin: '2030-01-08T12:30:30Z' };
    expect(isHorarioDisponible(slot)).toBe(true);
    expect(isHorarioDisponible({ inicio: '', fin: '2030-01-08T12:30:30Z' })).toBe(false);

    const disp = {
      motivoId: '11111111-1111-1111-1111-111111111111',
      duracionMinutos: 30,
      zonaHoraria: 'America/Argentina/Mendoza',
      horarios: [slot],
    };
    expect(isDisponibilidadPublicaResponse(disp)).toBe(true);
    expect(isDisponibilidadPublicaResponse({ ...disp, horarios: [{}] })).toBe(false);

    const reserva = {
      turnoId: '99999999-9999-9999-9999-999999999999',
      motivoId: '11111111-1111-1111-1111-111111111111',
      inicio: '2030-01-08T12:00:30Z',
      fin: '2030-01-08T12:30:30Z',
      duracionReservadaMinutos: 30,
      estado: 'CONFIRMADO',
      notificacionEstado: 'PENDIENTE',
    };
    expect(isReservaPublicaResponse(reserva)).toBe(true);
    expect(isReservaPublicaResponse({ ...reserva, estado: 'OTRO' })).toBe(false);
    expect(isReservaPublicaResponse({ ...reserva, notificacionEstado: 'ACEPTADA_SMTP' })).toBe(false);
  });

  it('reconoce TURNO_NOT_FOUND y ACCESS_DENIED y valida AgendaTurnoResumen y AgendaResponse', () => {
    expect(toApiErrorCode('TURNO_NOT_FOUND')).toBe('TURNO_NOT_FOUND');
    expect(toApiErrorCode('ACCESS_DENIED')).toBe('ACCESS_DENIED');

    const turnoConfirmado = {
      id: '22222222-2222-2222-2222-222222222222',
      motivoId: '11111111-1111-1111-1111-111111111111',
      motivoNombre: 'Limpieza',
      inicio: '2026-11-25T12:00:00Z',
      fin: '2026-11-25T12:30:00Z',
      duracionReservadaMinutos: 30,
      estado: 'CONFIRMADO',
      pacienteNombre: 'Ana',
      pacienteApellido: 'Perez',
    };
    const turnoCancelado = {
      ...turnoConfirmado,
      id: '33333333-3333-3333-3333-333333333333',
      estado: 'CANCELADO',
    };

    expect(isAgendaTurnoResumen(turnoConfirmado)).toBe(true);
    expect(isAgendaTurnoResumen(turnoCancelado)).toBe(true);
    expect(isAgendaTurnoResumen({ ...turnoConfirmado, duracionReservadaMinutos: 0 })).toBe(false);
    expect(isAgendaTurnoResumen({ ...turnoConfirmado, duracionReservadaMinutos: 30.5 })).toBe(false);
    const { pacienteApellido: _omit, ...sinApellido } = turnoConfirmado;
    expect(isAgendaTurnoResumen(sinApellido)).toBe(false);
    expect(isAgendaTurnoResumen({ ...turnoConfirmado, estado: 'REPROGRAMADO' })).toBe(false);

    expect(
      isAgendaResponse({
        vista: 'DIA',
        desde: '2026-11-25',
        hasta: '2026-11-25',
        zonaHoraria: 'America/Argentina/Mendoza',
        turnos: [],
      }),
    ).toBe(true);
    expect(
      isAgendaResponse({
        vista: 'SEMANA',
        desde: '2026-11-23',
        hasta: '2026-11-29',
        zonaHoraria: 'America/Argentina/Mendoza',
        turnos: [turnoConfirmado, turnoCancelado],
      }),
    ).toBe(true);
    expect(
      isAgendaResponse({
        vista: 'MES',
        desde: '2026-11-01',
        hasta: '2026-11-30',
        zonaHoraria: 'America/Argentina/Mendoza',
        turnos: [],
      }),
    ).toBe(false);
    expect(
      isAgendaResponse({
        vista: 'DIA',
        desde: '2026-02-30',
        hasta: '2026-02-30',
        zonaHoraria: 'America/Argentina/Mendoza',
        turnos: [],
      }),
    ).toBe(false);
  });

  it('valida PacienteBasico y TurnoDetalleResponse', () => {
    const paciente = {
      nombre: 'Ana',
      apellido: 'Perez',
      email: 'ana@example.com',
      telefono: '2615551234',
      dni: '30123456',
    };
    expect(isPacienteBasico(paciente)).toBe(true);
    const { dni: _omitDni, ...sinDni } = paciente;
    expect(isPacienteBasico(sinDni)).toBe(false);
    expect(isPacienteBasico({ ...paciente, nombre: '' })).toBe(false);

    const detalleConfirmadoRev0 = {
      id: '22222222-2222-2222-2222-222222222222',
      motivoId: '11111111-1111-1111-1111-111111111111',
      motivoNombre: 'Limpieza',
      inicio: '2030-01-08T12:00:00Z',
      fin: '2030-01-08T12:30:00Z',
      duracionReservadaMinutos: 30,
      estado: 'CONFIRMADO',
      revision: 0,
      creadoEn: '2030-01-01T10:00:00Z',
      canceladoEn: null,
      paciente,
    };
    expect(isTurnoDetalleResponse(detalleConfirmadoRev0)).toBe(true);
    expect(isTurnoDetalleResponse({ ...detalleConfirmadoRev0, revision: 2 })).toBe(true);
    expect(
      isTurnoDetalleResponse({
        ...detalleConfirmadoRev0,
        estado: 'CANCELADO',
        canceladoEn: '2030-01-08T10:00:00Z',
      }),
    ).toBe(true);

    expect(isTurnoDetalleResponse({ ...detalleConfirmadoRev0, revision: -1 })).toBe(false);
    expect(isTurnoDetalleResponse({ ...detalleConfirmadoRev0, revision: 1.5 })).toBe(false);
    expect(isTurnoDetalleResponse({ ...detalleConfirmadoRev0, canceladoEn: 123 })).toBe(false);
    expect(isTurnoDetalleResponse({ ...detalleConfirmadoRev0, canceladoEn: '' })).toBe(false);
    expect(isTurnoDetalleResponse({ ...detalleConfirmadoRev0, paciente: sinDni })).toBe(false);
    expect(isTurnoDetalleResponse({ ...detalleConfirmadoRev0, estado: 'REPROGRAMADO' })).toBe(false);
  });
});
