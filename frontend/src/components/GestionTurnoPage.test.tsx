import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import type { ApiClient } from '../api/apiClient';
import { GestionTurnoPage } from './GestionTurnoPage';

const currentTurn = {
  turno: {
    fecha: '2030-01-08',
    horaInicio: '09:00:00',
    horaFin: '09:30:00',
    estado: 'CONFIRMADO' as const,
    acciones: ['CANCELAR', 'REPROGRAMAR'],
  },
  etag: '"turno-r1"',
};

function setManagementUrl(fragment = '#private-token') {
  window.history.replaceState(null, '', `/gestionar-turno${fragment}`);
}

function makeClient(overrides: Partial<ApiClient> = {}): ApiClient {
  return {
    checkSession: vi.fn(),
    login: vi.fn(),
    logout: vi.fn(),
    getMotivos: vi.fn(),
    createMotivo: vi.fn(),
    updateMotivo: vi.fn(),
    getHorario: vi.fn(),
    replaceHorario: vi.fn(),
    getBloqueos: vi.fn(),
    createBloqueo: vi.fn(),
    deleteBloqueo: vi.fn(),
    getPublicMotivos: vi.fn(),
    getPublicDisponibilidad: vi.fn(),
    createPublicTurno: vi.fn(),
    getGestionTurno: vi.fn().mockResolvedValue({ ok: true, data: currentTurn }),
    getGestionDisponibilidad: vi.fn(),
    cancelarGestionTurno: vi.fn(),
    reprogramarGestionTurno: vi.fn(),
    clearCsrfToken: vi.fn(),
    ...overrides,
  };
}

afterEach(() => {
  window.history.replaceState(null, '', '/');
  window.localStorage.clear();
  window.sessionStorage.clear();
});

describe('GestionTurnoPage', () => {
  it('quita el fragmento, consulta solo con la credencial en memoria y no ofrece datos de contacto', async () => {
    setManagementUrl();
    const client = makeClient();

    const { unmount } = render(<GestionTurnoPage apiClient={client} />);

    expect(await screen.findByRole('heading', { name: 'Tu turno' })).toBeTruthy();
    expect(window.location.hash).toBe('');
    expect(client.getGestionTurno).toHaveBeenCalledWith('private-token');
    expect(window.localStorage.length).toBe(0);
    expect(window.sessionStorage.length).toBe(0);
    expect(screen.queryByText(/@/)).toBeNull();

    unmount();
    const reloadedClient = makeClient();
    render(<GestionTurnoPage apiClient={reloadedClient} />);
    expect((await screen.findByRole('status')).textContent).toMatch(/no incluye una credencial/i);
    expect(reloadedClient.getGestionTurno).not.toHaveBeenCalled();
  });

  it('muestra el error generico cuando C-09 no encuentra un enlace vigente', async () => {
    setManagementUrl();
    const client = makeClient({
      getGestionTurno: vi.fn().mockResolvedValue({
        ok: false,
        status: 404,
        codigo: 'LINK_NOT_AVAILABLE',
        message: 'Este enlace de autogestión no está disponible. Volvé a abrir el enlace original.',
      }),
    });

    render(<GestionTurnoPage apiClient={client} />);

    expect((await screen.findByRole('alert')).textContent).toMatch(/enlace de autogestión no está disponible/i);
    expect(screen.queryByRole('heading', { name: 'Tu turno' })).toBeNull();
  });

  it('requiere confirmacion expresa antes de cancelar y no promete recepcion del correo', async () => {
    setManagementUrl();
    const user = userEvent.setup();
    const cancel = vi.fn().mockResolvedValue({
      ok: true,
      data: {
        fecha: '2030-01-08',
        horaInicio: '09:00:00',
        horaFin: '09:30:00',
        estado: 'CANCELADO',
        notificacionEstado: 'PENDIENTE',
      },
    });
    const client = makeClient({ cancelarGestionTurno: cancel });

    render(<GestionTurnoPage apiClient={client} />);
    await screen.findByRole('heading', { name: 'Tu turno' });
    await user.click(screen.getByRole('button', { name: 'Cancelar turno' }));
    expect(cancel).not.toHaveBeenCalled();

    await user.click(screen.getByRole('button', { name: 'Confirmar cancelación' }));
    expect(cancel).toHaveBeenCalledWith('private-token', '"turno-r1"');
    expect(await screen.findByText(/notificación está PENDIENTE/i)).toBeTruthy();
    expect(screen.getByText(/no confirma la recepción del correo/i)).toBeTruthy();
  });

  it('muestra horarios en la zona devuelta y confirma el inicio exacto seleccionado', async () => {
    setManagementUrl();
    const user = userEvent.setup();
    const availability = vi.fn().mockResolvedValue({
      ok: true,
      data: {
        zonaHoraria: 'America/Argentina/Mendoza',
        horarios: [{ inicio: '2030-01-09T12:00:00Z', fin: '2030-01-09T12:30:00Z' }],
      },
    });
    const reschedule = vi.fn().mockResolvedValue({
      ok: true,
      data: {
        turnoId: '22222222-2222-2222-2222-222222222222',
        fecha: '2030-01-09',
        horaInicio: '09:00:00',
        horaFin: '09:30:00',
        estado: 'CONFIRMADO',
        notificacionEstado: 'PENDIENTE',
        etag: '"turno-r2"',
      },
    });
    const client = makeClient({
      getGestionDisponibilidad: availability,
      reprogramarGestionTurno: reschedule,
    });

    render(<GestionTurnoPage apiClient={client} />);
    await screen.findByRole('heading', { name: 'Tu turno' });
    await user.click(screen.getByRole('button', { name: 'Ver horarios disponibles' }));
    await user.click(screen.getByRole('button', { name: 'Consultar horarios' }));
    const slot = await screen.findByRole('radio', { name: /09:00 a 09:30/i });
    await user.click(slot);
    expect(screen.getByText(/America\/Argentina\/Mendoza/)).toBeTruthy();

    await user.click(screen.getByRole('button', { name: 'Confirmar reprogramación' }));
    expect(availability).toHaveBeenCalledWith('private-token', {});
    expect(reschedule).toHaveBeenCalledWith(
      'private-token',
      '"turno-r1"',
      '2030-01-09T12:00:00Z',
    );
    expect(await screen.findByText(/El turno fue reprogramado/i)).toBeTruthy();
  });

  it('mantiene el turno visible y orienta al contacto directo ante MANAGEMENT_WINDOW_CLOSED', async () => {
    setManagementUrl();
    const user = userEvent.setup();
    const cancel = vi.fn().mockResolvedValue({
      ok: false,
      status: 409,
      codigo: 'MANAGEMENT_WINDOW_CLOSED',
      message: 'El plazo para gestionar este turno en línea está cerrado. Para solicitar un cambio, contactá directamente al odontólogo por el canal habitual del consultorio.',
    });
    const client = makeClient({ cancelarGestionTurno: cancel });

    render(<GestionTurnoPage apiClient={client} />);
    await screen.findByRole('heading', { name: 'Tu turno' });
    await user.click(screen.getByRole('button', { name: 'Cancelar turno' }));
    await user.click(screen.getByRole('button', { name: 'Confirmar cancelación' }));

    expect((await screen.findByRole('alert')).textContent).toContain(
      'Para solicitar un cambio, contactá directamente al odontólogo por el canal habitual del consultorio.',
    );
    expect(screen.getByText('Confirmado')).toBeTruthy();
    expect(screen.queryByText(/alta manual|excepción|teléfono|email/i)).toBeNull();
  });

  it('conserva el turno original y orienta al contacto directo si la reprogramación queda fuera de plazo', async () => {
    setManagementUrl();
    const user = userEvent.setup();
    const availability = vi.fn().mockResolvedValue({
      ok: true,
      data: {
        zonaHoraria: 'America/Argentina/Mendoza',
        horarios: [{ inicio: '2030-01-09T12:00:00Z', fin: '2030-01-09T12:30:00Z' }],
      },
    });
    const reprogram = vi.fn().mockResolvedValue({
      ok: false,
      status: 409,
      codigo: 'MANAGEMENT_WINDOW_CLOSED',
      message: 'El plazo para gestionar este turno en línea está cerrado. Para solicitar un cambio, contactá directamente al odontólogo por el canal habitual del consultorio.',
    });
    const client = makeClient({
      getGestionDisponibilidad: availability,
      reprogramarGestionTurno: reprogram,
    });

    render(<GestionTurnoPage apiClient={client} />);
    await screen.findByRole('heading', { name: 'Tu turno' });
    await user.click(screen.getByRole('button', { name: 'Ver horarios disponibles' }));
    await user.click(screen.getByRole('button', { name: 'Consultar horarios' }));
    await user.click(await screen.findByRole('radio', { name: /09:00 a 09:30/i }));
    await user.click(screen.getByRole('button', { name: 'Confirmar reprogramación' }));

    expect(reprogram).toHaveBeenCalledWith(
      'private-token',
      '"turno-r1"',
      '2030-01-09T12:00:00Z',
    );
    expect((await screen.findByRole('alert')).textContent).toContain(
      'Para solicitar un cambio, contactá directamente al odontólogo por el canal habitual del consultorio.',
    );
    expect(screen.getByText('Confirmado')).toBeTruthy();
    expect(screen.queryByText(/alta manual|excepción|teléfono|email/i)).toBeNull();
  });

  it('ante una escritura de resultado incierto vuelve a consultar antes de afirmar el estado', async () => {
    setManagementUrl();
    const user = userEvent.setup();
    const reload = vi.fn()
      .mockResolvedValueOnce({ ok: true, data: currentTurn })
      .mockResolvedValueOnce({
        ok: true,
        data: {
          ...currentTurn,
          turno: { ...currentTurn.turno, fecha: '2030-01-10' },
          etag: '"turno-r2"',
        },
      });
    const cancel = vi.fn().mockResolvedValue({
      ok: false,
      status: 0,
      codigo: 'NETWORK_ERROR',
      message: 'No se pudo conectar con el servidor.',
    });
    const client = makeClient({ getGestionTurno: reload, cancelarGestionTurno: cancel });

    render(<GestionTurnoPage apiClient={client} />);
    await screen.findByRole('heading', { name: 'Tu turno' });
    await user.click(screen.getByRole('button', { name: 'Cancelar turno' }));
    await user.click(screen.getByRole('button', { name: 'Confirmar cancelación' }));

    expect(await screen.findByText(/No pudimos confirmar la respuesta/i)).toBeTruthy();
    expect(screen.getByText('10/01/2030')).toBeTruthy();
    expect(reload).toHaveBeenCalledTimes(2);
    expect(screen.queryByText(/La cancelación quedó registrada/i)).toBeNull();
  });

  it('permite llegar y abrir las acciones con teclado', async () => {
    setManagementUrl();
    const user = userEvent.setup();
    const client = makeClient();

    render(<GestionTurnoPage apiClient={client} />);
    await screen.findByRole('heading', { name: 'Tu turno' });
    await user.tab();
    expect(document.activeElement).toBe(screen.getByRole('button', { name: 'Actualizar estado del turno' }));
    await user.tab();
    expect(document.activeElement).toBe(screen.getByRole('button', { name: 'Cancelar turno' }));
    await user.keyboard('{Enter}');
    expect(screen.getByRole('group', { name: 'Confirmación de cancelación' })).toBeTruthy();
  });

  it.each([
    ['SLOT_UNAVAILABLE', 409, /horario elegido ya no está disponible/i],
    ['SAME_INTERVAL', 409, /coincide con el turno actual/i],
    ['TURN_VERSION_MISMATCH', 412, /turno cambió desde la última consulta/i],
  ] as const)(
    'mantiene el turno y permite actualizarlo tras %s',
    async (codigo, status, mensaje) => {
      setManagementUrl();
      const user = userEvent.setup();
      const reschedule = vi.fn().mockResolvedValue({
        ok: false,
        status,
        codigo,
        message: {
          SLOT_UNAVAILABLE: 'El horario elegido ya no está disponible. Seleccioná otra opción libre.',
          SAME_INTERVAL: 'El horario elegido coincide con el turno actual. Seleccioná otro horario.',
          TURN_VERSION_MISMATCH: 'El turno cambió desde la última consulta. Actualizá el estado antes de reintentar.',
        }[codigo],
      });
      const client = makeClient({
        getGestionDisponibilidad: vi.fn().mockResolvedValue({
          ok: true,
          data: {
            zonaHoraria: 'America/Argentina/Mendoza',
            horarios: [{ inicio: '2030-01-09T12:00:00Z', fin: '2030-01-09T12:30:00Z' }],
          },
        }),
        reprogramarGestionTurno: reschedule,
      });

      render(<GestionTurnoPage apiClient={client} />);
      await screen.findByRole('heading', { name: 'Tu turno' });
      await user.click(screen.getByRole('button', { name: 'Ver horarios disponibles' }));
      await user.click(screen.getByRole('button', { name: 'Consultar horarios' }));
      await user.click(await screen.findByRole('radio', { name: /09:00 a 09:30/i }));
      await user.click(screen.getByRole('button', { name: 'Confirmar reprogramación' }));

      expect((await screen.findByRole('alert')).textContent).toMatch(mensaje);
      expect(screen.getByText('Confirmado')).toBeTruthy();
      expect(screen.getByRole('button', { name: 'Actualizar estado del turno' })).toBeTruthy();
      expect(reschedule).toHaveBeenCalledTimes(1);
    },
  );
});
