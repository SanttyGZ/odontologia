import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import type { ApiClient } from './api/apiClient';
import { createApiClient } from './api/apiClient';
import { App } from './App';

function makeClient(overrides: Partial<ApiClient> = {}): ApiClient {
  return {
    checkSession: vi.fn().mockResolvedValue({ ok: true, data: { autenticado: true } }),
    login: vi.fn().mockResolvedValue({ ok: true, data: undefined }),
    logout: vi.fn().mockResolvedValue({ ok: true, data: undefined }),
    getMotivos: vi.fn().mockResolvedValue({ ok: true, data: [] }),
    createMotivo: vi.fn(),
    updateMotivo: vi.fn(),
    getHorario: vi.fn().mockResolvedValue({ ok: true, data: { franjas: [] } }),
    replaceHorario: vi.fn(),
    getBloqueos: vi.fn().mockResolvedValue({ ok: true, data: [] }),
    createBloqueo: vi.fn(),
    deleteBloqueo: vi.fn(),
    clearCsrfToken: vi.fn(),
    ...overrides,
  };
}

describe('App - flujo de sesion', () => {
  it('verifica sesion una sola vez y muestra login si responde 401 SESSION_REQUIRED', async () => {
    const checkSession = vi.fn().mockResolvedValue({
      ok: false,
      status: 401,
      codigo: 'SESSION_REQUIRED',
      message: 'Tu sesión expiró o no está activa. Volvé a ingresar.',
    });
    const getMotivos = vi.fn();

    render(<App client={makeClient({ checkSession, getMotivos })} />);

    expect(await screen.findByRole('button', { name: /ingresar/i })).toBeTruthy();
    expect(checkSession).toHaveBeenCalledTimes(1);
    expect(getMotivos).not.toHaveBeenCalled();
  });

  it('muestra configuracion cuando la sesion esta activa y permite cerrar sesion', async () => {
    const user = userEvent.setup();
    const logout = vi.fn().mockResolvedValue({ ok: true, data: undefined });
    const client = makeClient({ logout });

    render(<App client={client} />);

    expect(await screen.findByRole('button', { name: /cerrar sesión/i })).toBeTruthy();
    expect(screen.getByRole('tab', { name: /motivos/i })).toBeTruthy();
    expect(screen.getByRole('tab', { name: /horario/i })).toBeTruthy();
    expect(screen.getByRole('tab', { name: /bloqueos/i })).toBeTruthy();

    await user.click(screen.getByRole('button', { name: /cerrar sesión/i }));

    expect(logout).toHaveBeenCalledTimes(1);
    expect(await screen.findByRole('button', { name: /ingresar/i })).toBeTruthy();
  });

  it('vuelve al ingreso si una operacion interna responde 401 SESSION_REQUIRED', async () => {
    const getMotivos = vi.fn().mockResolvedValue({
      ok: false,
      status: 401,
      codigo: 'SESSION_REQUIRED',
      message: 'Tu sesión expiró o no está activa. Volvé a ingresar.',
    });
    const client = makeClient({ getMotivos });

    render(<App client={client} />);

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /ingresar/i })).toBeTruthy();
    });
    expect(screen.getByRole('alert').textContent).toMatch(/expiró/i);
  });

  it('informa 409 AGENDA_BUSY y 500 TECHNICAL_FAILURE sin asumir guardado ni reintentar automaticamente', async () => {
    const user = userEvent.setup();
    const createMotivo = vi
      .fn()
      .mockResolvedValueOnce({
        ok: false,
        status: 409,
        codigo: 'AGENDA_BUSY',
        message:
          'La agenda está ocupada procesando otra operación. Reintentá manualmente en unos segundos.',
      })
      .mockResolvedValueOnce({
        ok: false,
        status: 500,
        codigo: 'TECHNICAL_FAILURE',
        message: 'Ocurrió un error técnico en el servidor. No se guardaron cambios.',
      });

    const client = makeClient({ createMotivo });
    render(<App client={client} />);

    await screen.findByText(/no hay motivos cargados/i);
    await user.type(screen.getByLabelText(/nombre del motivo/i), 'Limpieza');
    await user.click(screen.getByRole('button', { name: /crear motivo/i }));

    expect((await screen.findByRole('alert')).textContent).toMatch(/reintentá manualmente/i);
    expect(createMotivo).toHaveBeenCalledTimes(1);

    await user.click(screen.getByRole('button', { name: /crear motivo/i }));
    expect((await screen.findByRole('alert')).textContent).toMatch(/no se guardaron cambios/i);
    expect(createMotivo).toHaveBeenCalledTimes(2);
  });

  it('ante 403 CSRF_INVALID con sesion expirada vuelve al ingreso enviando la escritura una sola vez', async () => {
    const user = userEvent.setup();
    const fetchMock = vi
      .fn<typeof fetch>()
      .mockResolvedValueOnce(
        new Response(JSON.stringify({ autenticado: true }), { status: 200 }),
      )
      .mockResolvedValueOnce(new Response(JSON.stringify([]), { status: 200 }))
      .mockResolvedValueOnce(
        new Response(JSON.stringify({ token: 'csrf-1', headerName: 'X-CSRF-TOKEN' }), { status: 200 }),
      )
      .mockResolvedValueOnce(
        new Response(JSON.stringify({ codigo: 'CSRF_INVALID' }), { status: 403 }),
      )
      .mockResolvedValueOnce(
        new Response(JSON.stringify({ codigo: 'SESSION_REQUIRED' }), { status: 401 }),
      );

    render(<App client={createApiClient(fetchMock)} />);

    await screen.findByText(/no hay motivos cargados/i);
    await user.type(screen.getByLabelText(/nombre del motivo/i), 'Limpieza');
    await user.click(screen.getByRole('button', { name: /crear motivo/i }));

    expect(await screen.findByRole('button', { name: /ingresar/i })).toBeTruthy();
    expect(screen.getByRole('alert').textContent).toMatch(/expiró/i);

    const postMotivos = fetchMock.mock.calls.filter(
      ([url, init]) => url === '/api/odontologo/motivos' && init?.method === 'POST',
    );
    expect(postMotivos).toHaveLength(1);
  });

  it('ante 403 CSRF_INVALID con sesion activa muestra error y permite reintento manual con token nuevo', async () => {
    const user = userEvent.setup();
    const fetchMock = vi
      .fn<typeof fetch>()
      .mockResolvedValueOnce(
        new Response(JSON.stringify({ autenticado: true }), { status: 200 }),
      )
      .mockResolvedValueOnce(new Response(JSON.stringify([]), { status: 200 }))
      .mockResolvedValueOnce(
        new Response(JSON.stringify({ token: 'csrf-1', headerName: 'X-CSRF-TOKEN' }), { status: 200 }),
      )
      .mockResolvedValueOnce(
        new Response(JSON.stringify({ codigo: 'CSRF_INVALID' }), { status: 403 }),
      )
      .mockResolvedValueOnce(
        new Response(JSON.stringify({ autenticado: true }), { status: 200 }),
      )
      .mockResolvedValueOnce(
        new Response(JSON.stringify({ token: 'csrf-2', headerName: 'X-CSRF-TOKEN' }), { status: 200 }),
      )
      .mockResolvedValueOnce(
        new Response(
          JSON.stringify({
            id: '11111111-1111-1111-1111-111111111111',
            nombre: 'Limpieza',
            duracionMinutos: 30,
          }),
          { status: 201 },
        ),
      );

    render(<App client={createApiClient(fetchMock)} />);

    await screen.findByText(/no hay motivos cargados/i);
    await user.type(screen.getByLabelText(/nombre del motivo/i), 'Limpieza');
    await user.click(screen.getByRole('button', { name: /crear motivo/i }));

    expect((await screen.findByRole('alert')).textContent).toMatch(/token de seguridad/i);
    expect(screen.getByRole('button', { name: /cerrar sesión/i })).toBeTruthy();

    await user.click(screen.getByRole('button', { name: /crear motivo/i }));
    expect(await screen.findByText('Limpieza')).toBeTruthy();

    const postMotivos = fetchMock.mock.calls.filter(
      ([url, init]) => url === '/api/odontologo/motivos' && init?.method === 'POST',
    );
    expect(postMotivos).toHaveLength(2);
    expect(new Headers(postMotivos[1]?.[1]?.headers).get('X-CSRF-TOKEN')).toBe('csrf-2');
  });

  it('muestra error de cierre de sesion si logout falla con sesion activa', async () => {
    const user = userEvent.setup();
    const logout = vi.fn().mockResolvedValue({
      ok: false,
      status: 403,
      codigo: 'CSRF_INVALID',
      message: 'El token de seguridad venció o es inválido. Intentá la operación nuevamente.',
    });

    render(<App client={makeClient({ logout })} />);

    await screen.findByRole('button', { name: /cerrar sesión/i });
    await user.click(screen.getByRole('button', { name: /cerrar sesión/i }));

    expect((await screen.findByRole('alert')).textContent).toMatch(/token de seguridad/i);
    expect(screen.getByRole('button', { name: /cerrar sesión/i })).toBeTruthy();
  });
});
