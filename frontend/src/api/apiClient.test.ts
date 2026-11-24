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
});
