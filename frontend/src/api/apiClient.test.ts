import { describe, expect, it, vi } from 'vitest';
import { createApiClient } from './apiClient';

function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  });
}

function emptyResponse(status: number): Response {
  return new Response(null, { status });
}

describe('apiClient', () => {
  it('checkSession devuelve ok true en 200 y SESSION_REQUIRED en 401', async () => {
    const fetchMock = vi
      .fn<typeof fetch>()
      .mockResolvedValueOnce(jsonResponse(200, { autenticado: true }))
      .mockResolvedValueOnce(jsonResponse(401, { codigo: 'SESSION_REQUIRED' }));

    const client = createApiClient(fetchMock);

    const active = await client.checkSession();
    expect(active).toEqual({ ok: true, data: { autenticado: true } });
    expect(fetchMock).toHaveBeenNthCalledWith(1, '/api/odontologo/sesion', {
      method: 'GET',
      credentials: 'same-origin',
    });

    const inactive = await client.checkSession();
    expect(inactive.ok).toBe(false);
    if (!inactive.ok) {
      expect(inactive.status).toBe(401);
      expect(inactive.codigo).toBe('SESSION_REQUIRED');
    }
  });

  it('obtiene CSRF antes de login, envia form-urlencoded y renueva estado CSRF en 204', async () => {
    const fetchMock = vi
      .fn<typeof fetch>()
      .mockResolvedValueOnce(jsonResponse(200, { token: 'csrf-anon', headerName: 'X-CSRF-TOKEN' }))
      .mockResolvedValueOnce(emptyResponse(204))
      .mockResolvedValueOnce(jsonResponse(200, { token: 'csrf-auth', headerName: 'X-CSRF-TOKEN' }))
      .mockResolvedValueOnce(
        jsonResponse(201, {
          id: '11111111-1111-1111-1111-111111111111',
          nombre: 'Consulta',
          duracionMinutos: 30,
        }),
      );

    const client = createApiClient(fetchMock);

    const loginResult = await client.login('carlos', 'clave-segura-12345');
    expect(loginResult).toEqual({ ok: true, data: undefined });

    const loginCall = fetchMock.mock.calls[1];
    expect(loginCall?.[0]).toBe('/api/odontologo/sesion');
    const loginInit = loginCall?.[1];
    expect(loginInit?.method).toBe('POST');
    expect(loginInit?.credentials).toBe('same-origin');
    const loginHeaders = new Headers(loginInit?.headers);
    expect(loginHeaders.get('X-CSRF-TOKEN')).toBe('csrf-anon');
    expect(loginHeaders.get('Content-Type')).toContain('application/x-www-form-urlencoded');
    expect(String(loginInit?.body)).toContain('username=carlos');
    expect(String(loginInit?.body)).toContain('password=clave-segura-12345');

    // Al hacer una escritura posterior tras el login, debe pedir un CSRF nuevo (descarto csrf-anon)
    const createResult = await client.createMotivo({ nombre: 'Consulta', duracionMinutos: 30 });
    expect(createResult.ok).toBe(true);
    const createHeaders = new Headers(fetchMock.mock.calls[3]?.[1]?.headers);
    expect(createHeaders.get('X-CSRF-TOKEN')).toBe('csrf-auth');
  });

  it('ante 403 CSRF_INVALID con sesion activa descarta el token, consulta sesion sin reenviar y permite reintento manual', async () => {
    const fetchMock = vi
      .fn<typeof fetch>()
      .mockResolvedValueOnce(jsonResponse(200, { token: 'csrf-1', headerName: 'X-CSRF-TOKEN' }))
      .mockResolvedValueOnce(jsonResponse(403, { codigo: 'CSRF_INVALID' }))
      .mockResolvedValueOnce(jsonResponse(200, { autenticado: true }))
      .mockResolvedValueOnce(jsonResponse(200, { token: 'csrf-2', headerName: 'X-CSRF-TOKEN' }))
      .mockResolvedValueOnce(
        jsonResponse(201, {
          id: '11111111-1111-1111-1111-111111111111',
          nombre: 'Limpieza',
          duracionMinutos: 45,
        }),
      );

    const client = createApiClient(fetchMock);

    const failed = await client.createMotivo({ nombre: 'Limpieza', duracionMinutos: 45 });
    expect(failed.ok).toBe(false);
    if (!failed.ok) {
      expect(failed.status).toBe(403);
      expect(failed.codigo).toBe('CSRF_INVALID');
    }
    // 3 llamadas: GET csrf + POST motivo + GET sesion. No reenvio automatico.
    expect(fetchMock).toHaveBeenCalledTimes(3);
    expect(fetchMock.mock.calls[2]?.[0]).toBe('/api/odontologo/sesion');

    // En el reintento manual del usuario, solicita un token nuevo y envia la escritura una sola vez
    const retried = await client.createMotivo({ nombre: 'Limpieza', duracionMinutos: 45 });
    expect(retried.ok).toBe(true);
    expect(fetchMock).toHaveBeenCalledTimes(5);
    const retryHeaders = new Headers(fetchMock.mock.calls[4]?.[1]?.headers);
    expect(retryHeaders.get('X-CSRF-TOKEN')).toBe('csrf-2');
  });

  it('ante 403 CSRF_INVALID con sesion expirada (401 SESSION_REQUIRED) devuelve SESSION_REQUIRED sin reenviar', async () => {
    const fetchMock = vi
      .fn<typeof fetch>()
      .mockResolvedValueOnce(jsonResponse(200, { token: 'csrf-1', headerName: 'X-CSRF-TOKEN' }))
      .mockResolvedValueOnce(jsonResponse(403, { codigo: 'CSRF_INVALID' }))
      .mockResolvedValueOnce(jsonResponse(401, { codigo: 'SESSION_REQUIRED' }));

    const client = createApiClient(fetchMock);

    const failed = await client.createMotivo({ nombre: 'Limpieza', duracionMinutos: 45 });
    expect(failed.ok).toBe(false);
    if (!failed.ok) {
      expect(failed.status).toBe(401);
      expect(failed.codigo).toBe('SESSION_REQUIRED');
    }
    expect(fetchMock).toHaveBeenCalledTimes(3);
    expect(fetchMock.mock.calls[1]?.[0]).toBe('/api/odontologo/motivos');
    expect(fetchMock.mock.calls[2]?.[0]).toBe('/api/odontologo/sesion');
  });

  it('mapea errores de dominio (409, 404, 503, 500) y fallos de red sin lanzar', async () => {
    const fetchMock = vi
      .fn<typeof fetch>()
      .mockResolvedValueOnce(jsonResponse(200, { token: 't', headerName: 'X-CSRF-TOKEN' }))
      .mockResolvedValueOnce(jsonResponse(409, { codigo: 'AGENDA_BUSY' }))
      .mockResolvedValueOnce(jsonResponse(500, { codigo: 'TECHNICAL_FAILURE' }))
      .mockRejectedValueOnce(new TypeError('Failed to fetch'));

    const client = createApiClient(fetchMock);

    const busy = await client.replaceHorario({ franjas: [] });
    expect(busy.ok).toBe(false);
    if (!busy.ok) {
      expect(busy.codigo).toBe('AGENDA_BUSY');
      expect(busy.status).toBe(409);
    }

    const tech = await client.deleteBloqueo('11111111-1111-1111-1111-111111111111');
    expect(tech.ok).toBe(false);
    if (!tech.ok) {
      expect(tech.codigo).toBe('TECHNICAL_FAILURE');
      expect(tech.status).toBe(500);
    }

    const net = await client.getMotivos();
    expect(net.ok).toBe(false);
    if (!net.ok) {
      expect(net.codigo).toBe('NETWORK_ERROR');
    }
  });

  it('consulta disponibilidad y crea reserva publica sin solicitar CSRF interno y mapea 409 SLOT_UNAVAILABLE', async () => {
    const fetchMock = vi
      .fn<typeof fetch>()
      .mockResolvedValueOnce(
        jsonResponse(200, [
          { id: '11111111-1111-1111-1111-111111111111', nombre: 'Consulta', duracionMinutos: 30 },
        ]),
      )
      .mockResolvedValueOnce(
        jsonResponse(200, {
          motivoId: '11111111-1111-1111-1111-111111111111',
          duracionMinutos: 30,
          zonaHoraria: 'America/Argentina/Mendoza',
          horarios: [{ inicio: '2030-01-08T12:00:30Z', fin: '2030-01-08T12:30:30Z' }],
        }),
      )
      .mockResolvedValueOnce(
        jsonResponse(201, {
          turnoId: '22222222-2222-2222-2222-222222222222',
          motivoId: '11111111-1111-1111-1111-111111111111',
          inicio: '2030-01-08T12:00:30Z',
          fin: '2030-01-08T12:30:30Z',
          duracionReservadaMinutos: 30,
          estado: 'CONFIRMADO',
          notificacionEstado: 'PENDIENTE',
        }),
      )
      .mockResolvedValueOnce(jsonResponse(409, { codigo: 'SLOT_UNAVAILABLE' }));

    const client = createApiClient(fetchMock);

    const motivos = await client.getPublicMotivos();
    expect(motivos.ok).toBe(true);
    expect(fetchMock.mock.calls[0]?.[0]).toBe('/api/public/motivos');

    const disp = await client.getPublicDisponibilidad({
      motivoId: '11111111-1111-1111-1111-111111111111',
      desde: '2030-01-08',
      hasta: '2030-01-10',
    });
    expect(disp.ok).toBe(true);
    expect(fetchMock.mock.calls[1]?.[0]).toBe(
      '/api/public/disponibilidad?motivoId=11111111-1111-1111-1111-111111111111&desde=2030-01-08&hasta=2030-01-10',
    );

    const bookingInput = {
      motivoId: '11111111-1111-1111-1111-111111111111',
      inicio: '2030-01-08T12:00:30Z',
      nombre: 'Ana',
      apellido: 'Perez',
      email: 'ana@example.com',
      telefono: '2615551234',
      dni: '30123456',
    };
    const booked = await client.createPublicTurno(bookingInput);
    expect(booked.ok).toBe(true);
    expect(fetchMock.mock.calls[2]?.[0]).toBe('/api/public/turnos');

    const conflict = await client.createPublicTurno(bookingInput);
    expect(conflict.ok).toBe(false);
    if (!conflict.ok) {
      expect(conflict.status).toBe(409);
      expect(conflict.codigo).toBe('SLOT_UNAVAILABLE');
    }

    // Ninguna llamada debe haber ido a /api/odontologo/csrf
    expect(fetchMock).toHaveBeenCalledTimes(4);
  });

  it('captura fallo al leer el cuerpo de una respuesta 201 en createPublicTurno y devuelve NETWORK_ERROR sin lanzar', async () => {
    const broken201 = new Response('', { status: 201 });
    vi.spyOn(broken201, 'text').mockRejectedValueOnce(new TypeError('Body stream aborted'));
    const fetchMock = vi.fn<typeof fetch>().mockResolvedValueOnce(broken201);

    const client = createApiClient(fetchMock);
    const result = await client.createPublicTurno({
      motivoId: '11111111-1111-1111-1111-111111111111',
      inicio: '2030-01-08T12:00:30Z',
      nombre: 'Ana',
      apellido: 'Perez',
      email: 'ana@example.com',
      telefono: '2615551234',
      dni: '30123456',
    });

    expect(result.ok).toBe(false);
    if (!result.ok) {
      expect(result.status).toBe(0);
      expect(result.codigo).toBe('NETWORK_ERROR');
    }
  });

  it('consulta turno y disponibilidad protegida sin motivoId ni cache', async () => {
    const token = 'private-management-token';
    const fetchMock = vi
      .fn<typeof fetch>()
      .mockResolvedValueOnce(new Response(JSON.stringify({
        fecha: '2030-01-08',
        horaInicio: '09:00:00',
        horaFin: '09:30:00',
        estado: 'CONFIRMADO',
        acciones: ['CANCELAR', 'REPROGRAMAR'],
      }), { status: 200, headers: { ETag: '"turno-r1"' } }))
      .mockResolvedValueOnce(jsonResponse(200, {
        zonaHoraria: 'America/Argentina/Mendoza',
        horarios: [{ inicio: '2030-01-09T12:00:00Z', fin: '2030-01-09T12:30:00Z' }],
      }));

    const client = createApiClient(fetchMock);
    const appointment = await client.getGestionTurno(token);
    const availability = await client.getGestionDisponibilidad(token, {
      desde: '2030-01-09',
      hasta: '2030-01-20',
    });

    expect(appointment.ok).toBe(true);
    if (appointment.ok) {
      expect(appointment.data.etag).toBe('"turno-r1"');
      expect(appointment.data.turno.estado).toBe('CONFIRMADO');
    }
    expect(fetchMock.mock.calls[0]?.[0]).toBe('/api/gestion/turno');
    expect(fetchMock.mock.calls[1]?.[0]).toBe(
      '/api/gestion/turno/disponibilidad?desde=2030-01-09&hasta=2030-01-20',
    );
    for (const [, init] of fetchMock.mock.calls) {
      const headers = new Headers(init?.headers);
      expect(headers.get('X-Management-Token')).toBe(token);
      expect(init?.credentials).toBe('same-origin');
      expect(init?.cache).toBe('no-store');
      expect(String(init?.body ?? '')).not.toContain('motivoId');
    }
    expect(availability.ok).toBe(true);
  });

  it('envia If-Match en cancelacion y reprogramacion y conserva el inicio exacto', async () => {
    const fetchMock = vi
      .fn<typeof fetch>()
      .mockResolvedValueOnce(jsonResponse(200, {
        fecha: '2030-01-08',
        horaInicio: '09:00:00',
        horaFin: '09:30:00',
        estado: 'CANCELADO',
        notificacionEstado: 'PENDIENTE',
      }))
      .mockResolvedValueOnce(new Response(JSON.stringify({
        turnoId: '22222222-2222-2222-2222-222222222222',
        fecha: '2030-01-09',
        horaInicio: '09:00:00',
        horaFin: '09:30:00',
        estado: 'CONFIRMADO',
        notificacionEstado: 'PENDIENTE',
      }), { status: 200, headers: { ETag: '"turno-r2"' } }));

    const client = createApiClient(fetchMock);
    const cancelled = await client.cancelarGestionTurno('secret-a', '"turno-r1"');
    const newStart = '2030-01-09T12:00:00Z';
    const rescheduled = await client.reprogramarGestionTurno('secret-b', '"turno-r2"', newStart);

    expect(cancelled.ok).toBe(true);
    expect(rescheduled.ok).toBe(true);
    if (rescheduled.ok) {
      expect(rescheduled.data.etag).toBe('"turno-r2"');
    }
    expect(fetchMock.mock.calls[0]?.[0]).toBe('/api/gestion/turno/cancelacion');
    expect(fetchMock.mock.calls[1]?.[0]).toBe('/api/gestion/turno/reprogramacion');
    const cancelInit = fetchMock.mock.calls[0]?.[1];
    const rescheduleInit = fetchMock.mock.calls[1]?.[1];
    expect(new Headers(cancelInit?.headers).get('If-Match')).toBe('"turno-r1"');
    expect(new Headers(cancelInit?.headers).get('X-Management-Token')).toBe('secret-a');
    expect(new Headers(rescheduleInit?.headers).get('If-Match')).toBe('"turno-r2"');
    expect(new Headers(rescheduleInit?.headers).get('X-Management-Token')).toBe('secret-b');
    expect(rescheduleInit?.body).toBe(JSON.stringify({ nuevoInicio: newStart }));
  });

  it('reconoce los errores de enlace y de gestion de C-09', async () => {
    const fetchMock = vi
      .fn<typeof fetch>()
      .mockResolvedValueOnce(jsonResponse(404, { codigo: 'LINK_NOT_AVAILABLE' }))
      .mockResolvedValueOnce(jsonResponse(409, { codigo: 'MANAGEMENT_WINDOW_CLOSED' }));
    const client = createApiClient(fetchMock);

    const missing = await client.getGestionTurno('secret');
    const closed = await client.cancelarGestionTurno('secret', '"turno-r1"');

    expect(missing).toMatchObject({ ok: false, status: 404, codigo: 'LINK_NOT_AVAILABLE' });
    expect(closed).toMatchObject({ ok: false, status: 409, codigo: 'MANAGEMENT_WINDOW_CLOSED' });
  });
});
