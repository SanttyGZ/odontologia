import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import type { ApiClient } from '../api/apiClient';
import type { HorarioResponse } from '../types/odontologia';
import { HorarioSection } from './HorarioSection';

function makeClient(overrides: Partial<ApiClient> = {}): ApiClient {
  return {
    checkSession: vi.fn(),
    login: vi.fn(),
    logout: vi.fn(),
    getMotivos: vi.fn(),
    createMotivo: vi.fn(),
    updateMotivo: vi.fn(),
    getHorario: vi.fn().mockResolvedValue({ ok: true, data: { franjas: [] } }),
    replaceHorario: vi.fn(),
    getBloqueos: vi.fn(),
    createBloqueo: vi.fn(),
    deleteBloqueo: vi.fn(),
    clearCsrfToken: vi.fn(),
    ...overrides,
  };
}

const initialHorario: HorarioResponse = {
  franjas: [
    {
      id: '11111111-1111-1111-1111-111111111111',
      diaSemana: 1,
      horaInicio: '09:00:00',
      horaFin: '13:00:00',
      tipo: 'ATENCION',
    },
  ],
};

describe('HorarioSection', () => {
  it('carga franjas y guarda reemplazo completo con UUID no vacio en cada nueva franja', async () => {
    const user = userEvent.setup();
    const replaceHorario = vi.fn().mockImplementation(async (payload: HorarioResponse) => ({
      ok: true as const,
      data: payload,
    }));
    const client = makeClient({
      getHorario: vi.fn().mockResolvedValue({ ok: true, data: initialHorario }),
      replaceHorario,
    });

    render(<HorarioSection apiClient={client} onSessionExpired={vi.fn()} />);

    await screen.findByDisplayValue('09:00');
    await user.click(screen.getByRole('button', { name: /agregar franja/i }));
    await user.click(screen.getByRole('button', { name: /guardar horario/i }));

    expect(replaceHorario).toHaveBeenCalledTimes(1);
    const sent = replaceHorario.mock.calls[0]?.[0] as HorarioResponse;
    expect(sent.franjas).toHaveLength(2);
    expect(sent.franjas[1]?.id.length).toBeGreaterThan(0);
    expect(sent.franjas[1]?.id).not.toBe(sent.franjas[0]?.id);
    expect((await screen.findByRole('status')).textContent).toMatch(/guardado/i);
  });

  it('conserva la edicion pendiente ante 409 CONFIGURATION_CONFLICT', async () => {
    const user = userEvent.setup();
    const replaceHorario = vi.fn().mockResolvedValue({
      ok: false,
      status: 409,
      codigo: 'CONFIGURATION_CONFLICT',
      message: 'El cambio entra en conflicto con turnos confirmados vigentes.',
    });
    const client = makeClient({
      getHorario: vi.fn().mockResolvedValue({ ok: true, data: initialHorario }),
      replaceHorario,
    });

    render(<HorarioSection apiClient={client} onSessionExpired={vi.fn()} />);
    await screen.findByDisplayValue('09:00');

    await user.click(screen.getByRole('button', { name: /agregar franja/i }));
    await user.click(screen.getByRole('button', { name: /guardar horario/i }));

    expect((await screen.findByRole('alert')).textContent).toMatch(/conflicto/i);
    // Ambas filas siguen en pantalla para que el odontologo corrija sin perder su edicion
    expect(screen.getAllByRole('button', { name: /quitar/i })).toHaveLength(2);
  });

  it('valida horaInicio < horaFin antes de enviar', async () => {
    const user = userEvent.setup();
    const replaceHorario = vi.fn();
    const client = makeClient({
      getHorario: vi.fn().mockResolvedValue({
        ok: true,
        data: {
          franjas: [
            {
              id: '11111111-1111-1111-1111-111111111111',
              diaSemana: 2,
              horaInicio: '14:00:00',
              horaFin: '12:00:00',
              tipo: 'ATENCION',
            },
          ],
        },
      }),
      replaceHorario,
    });

    render(<HorarioSection apiClient={client} onSessionExpired={vi.fn()} />);
    await screen.findByDisplayValue('14:00');

    await user.click(screen.getByRole('button', { name: /guardar horario/i }));
    expect(replaceHorario).not.toHaveBeenCalled();
    expect(screen.getByRole('alert').textContent).toMatch(/anterior a la hora de fin/i);
  });
});
