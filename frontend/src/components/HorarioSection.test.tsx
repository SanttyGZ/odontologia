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

    await screen.findByDisplayValue('09:00:00');
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
    await screen.findByDisplayValue('09:00:00');

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
    await screen.findByDisplayValue('14:00:00');

    await user.click(screen.getByRole('button', { name: /guardar horario/i }));
    expect(replaceHorario).not.toHaveBeenCalled();
    expect(screen.getByRole('alert').textContent).toMatch(/anterior a la hora de fin/i);
  });

  it('si falla GET /api/odontologo/horario bloquea edicion y guardado, permite reintentar y habilita crear si devuelve []', async () => {
    const user = userEvent.setup();
    const getHorario = vi
      .fn()
      .mockResolvedValueOnce({
        ok: false,
        status: 500,
        codigo: 'TECHNICAL_FAILURE',
        message: 'Ocurrió un error técnico en el servidor. No se guardaron cambios.',
      })
      .mockResolvedValueOnce({
        ok: true,
        data: { franjas: [] },
      });
    const replaceHorario = vi.fn();

    render(
      <HorarioSection
        apiClient={makeClient({ getHorario, replaceHorario })}
        onSessionExpired={vi.fn()}
      />,
    );

    expect((await screen.findByRole('alert')).textContent).toMatch(/error técnico/i);
    // Jamás debe habilitar edición ni un PUT con lista vacía tras fallo de GET
    expect(screen.queryByRole('button', { name: /agregar franja/i })).toBeNull();
    expect(screen.queryByRole('button', { name: /guardar horario/i })).toBeNull();
    expect(replaceHorario).not.toHaveBeenCalled();

    await user.click(screen.getByRole('button', { name: /reintentar/i }));
    expect(getHorario).toHaveBeenCalledTimes(2);
    expect(await screen.findByText(/no hay franjas configuradas/i)).toBeTruthy();
    expect(screen.getByRole('button', { name: /agregar franja/i })).toBeTruthy();
    expect(screen.getByRole('button', { name: /guardar horario/i })).toBeTruthy();
  });

  it('conserva 09:00:30 y 12:00:45 en PUT al guardar sin editar y al editar otra franja', async () => {
    const user = userEvent.setup();
    const preciseHorario: HorarioResponse = {
      franjas: [
        {
          id: '11111111-1111-1111-1111-111111111111',
          diaSemana: 1,
          horaInicio: '09:00:30',
          horaFin: '12:00:45',
          tipo: 'ATENCION',
        },
      ],
    };
    const replaceHorario = vi.fn().mockImplementation(async (payload: HorarioResponse) => ({
      ok: true as const,
      data: payload,
    }));

    render(
      <HorarioSection
        apiClient={makeClient({
          getHorario: vi.fn().mockResolvedValue({ ok: true, data: preciseHorario }),
          replaceHorario,
        })}
        onSessionExpired={vi.fn()}
      />,
    );

    await screen.findByDisplayValue('09:00:30');
    await user.click(screen.getByRole('button', { name: /guardar horario/i }));

    expect(replaceHorario).toHaveBeenCalledTimes(1);
    expect(replaceHorario.mock.calls[0]?.[0]).toEqual(preciseHorario);

    await user.click(screen.getByRole('button', { name: /agregar franja/i }));
    await user.click(screen.getByRole('button', { name: /guardar horario/i }));

    expect(replaceHorario).toHaveBeenCalledTimes(2);
    const secondPayload = replaceHorario.mock.calls[1]?.[0] as HorarioResponse;
    expect(secondPayload.franjas[0]).toEqual(preciseHorario.franjas[0]);
    expect(secondPayload.franjas).toHaveLength(2);
  });
});
