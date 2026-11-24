import { useCallback, useEffect, useRef, useState } from 'react';
import type { FormEvent } from 'react';
import type { ApiClient } from '../api/apiClient';
import type { FranjaSemanal, FranjaTipo } from '../types/odontologia';

const DIAS_SEMANA: ReadonlyArray<{ value: number; label: string }> = [
  { value: 1, label: 'Lunes' },
  { value: 2, label: 'Martes' },
  { value: 3, label: 'Miércoles' },
  { value: 4, label: 'Jueves' },
  { value: 5, label: 'Viernes' },
  { value: 6, label: 'Sábado' },
  { value: 7, label: 'Domingo' },
];

type LoadState = 'loading' | 'loaded' | 'error';

function toTimeInputValue(raw: string): string {
  const dotIndex = raw.indexOf('.');
  return dotIndex === -1 ? raw : raw.slice(0, dotIndex);
}

function extractSubsecondFraction(raw: string): string | null {
  const match = /(\.\d+)$/.exec(raw.trim());
  return match?.[1] ?? null;
}

function normalizeTimeForCompare(raw: string): string {
  const trimmed = raw.trim();
  const [base = '', fraction = ''] = trimmed.split('.');
  const hms = /^\d{2}:\d{2}$/.test(base) ? `${base}:00` : base;
  return `${hms}.${fraction.padEnd(6, '0').slice(0, 6)}`;
}

function createEmptyFranja(): FranjaSemanal {
  return {
    id: crypto.randomUUID(),
    diaSemana: 1,
    horaInicio: '09:00',
    horaFin: '12:00',
    tipo: 'ATENCION',
  };
}

interface HorarioSectionProps {
  apiClient: ApiClient;
  onSessionExpired: (message: string) => void;
  onBusyChange?: (busy: boolean) => void;
}

export function HorarioSection({ apiClient, onSessionExpired, onBusyChange }: HorarioSectionProps) {
  const [franjas, setFranjas] = useState<FranjaSemanal[]>([]);
  const [loadState, setLoadState] = useState<LoadState>('loading');
  const [saving, setSaving] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const latestRequestIdRef = useRef(0);

  useEffect(() => {
    onBusyChange?.(saving);
    return () => {
      onBusyChange?.(false);
    };
  }, [onBusyChange, saving]);

  const loadHorario = useCallback(async (): Promise<void> => {
    const requestId = ++latestRequestIdRef.current;
    setLoadState('loading');
    setErrorMessage(null);
    const result = await apiClient.getHorario();
    if (requestId !== latestRequestIdRef.current) {
      return;
    }
    if (!result.ok) {
      if (result.codigo === 'SESSION_REQUIRED') {
        onSessionExpired(result.message);
        return;
      }
      setLoadState('error');
      setErrorMessage(result.message);
      return;
    }
    setFranjas(result.data.franjas);
    setLoadState('loaded');
  }, [apiClient, onSessionExpired]);

  useEffect(() => {
    void loadHorario();
    return () => {
      latestRequestIdRef.current += 1;
    };
  }, [loadHorario]);

  function updateFranja(id: string, patch: Partial<FranjaSemanal>): void {
    setFranjas((prev) => prev.map((item) => (item.id === id ? { ...item, ...patch } : item)));
    setSuccessMessage(null);
  }

  function removeFranja(id: string): void {
    setFranjas((prev) => prev.filter((item) => item.id !== id));
    setSuccessMessage(null);
  }

  function addFranja(): void {
    setFranjas((prev) => [...prev, createEmptyFranja()]);
    setErrorMessage(null);
    setSuccessMessage(null);
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>): Promise<void> {
    event.preventDefault();
    if (loadState !== 'loaded') {
      return;
    }

    for (const row of franjas) {
      const inicio = normalizeTimeForCompare(row.horaInicio);
      const fin = normalizeTimeForCompare(row.horaFin);
      if (row.horaInicio.trim().length === 0 || row.horaFin.trim().length === 0 || inicio >= fin) {
        setErrorMessage('Cada franja debe tener una hora de inicio anterior a la hora de fin.');
        setSuccessMessage(null);
        return;
      }
    }

    setSaving(true);
    setErrorMessage(null);
    setSuccessMessage(null);

    const result = await apiClient.replaceHorario({ franjas });
    setSaving(false);

    if (!result.ok) {
      if (result.codigo === 'SESSION_REQUIRED') {
        onSessionExpired(result.message);
        return;
      }
      setErrorMessage(result.message);
      return;
    }

    setFranjas(result.data.franjas);
    setSuccessMessage('Horario semanal guardado.');
  }

  return (
    <section className="stack" aria-labelledby="horario-heading">
      <h2 id="horario-heading">Horario semanal y pausas</h2>
      <p className="subtitle">
        Configurá las franjas de atención y sus pausas contenidas en la zona horaria del consultorio.
      </p>

      {errorMessage !== null ? (
        <p className="feedback feedback-error" role="alert" aria-live="assertive">
          {errorMessage}
        </p>
      ) : null}

      {successMessage !== null ? (
        <p className="feedback feedback-success" role="status" aria-live="polite">
          {successMessage}
        </p>
      ) : null}

      {loadState === 'loading' ? <p aria-busy="true">Cargando horario…</p> : null}

      {loadState === 'error' ? (
        <div className="actions">
          <button type="button" className="btn btn-secondary" onClick={() => void loadHorario()}>
            Reintentar
          </button>
        </div>
      ) : null}

      {loadState === 'loaded' ? (
        <form onSubmit={(event) => void handleSubmit(event)} className="stack">
          {franjas.length === 0 ? (
            <p className="muted">No hay franjas configuradas todavía.</p>
          ) : (
            <div className="stack">
              {franjas.map((franja, index) => {
                const inicioFraction = extractSubsecondFraction(franja.horaInicio);
                const finFraction = extractSubsecondFraction(franja.horaFin);
                return (
                  <div key={franja.id} className="card franja-row">
                    <div className="field">
                      <label htmlFor={`franja-dia-${franja.id}`}>Día ({index + 1})</label>
                      <select
                        id={`franja-dia-${franja.id}`}
                        value={franja.diaSemana}
                        onChange={(event) =>
                          updateFranja(franja.id, { diaSemana: Number(event.target.value) })
                        }
                        disabled={saving}
                      >
                        {DIAS_SEMANA.map((dia) => (
                          <option key={dia.value} value={dia.value}>
                            {dia.label}
                          </option>
                        ))}
                      </select>
                    </div>

                    <div className="field">
                      <label htmlFor={`franja-tipo-${franja.id}`}>Tipo</label>
                      <select
                        id={`franja-tipo-${franja.id}`}
                        value={franja.tipo}
                        onChange={(event) =>
                          updateFranja(franja.id, { tipo: event.target.value as FranjaTipo })
                        }
                        disabled={saving}
                      >
                        <option value="ATENCION">Atención</option>
                        <option value="PAUSA">Pausa</option>
                      </select>
                    </div>

                    <div className="field">
                      <label htmlFor={`franja-inicio-${franja.id}`}>Inicio</label>
                      <input
                        id={`franja-inicio-${franja.id}`}
                        type="time"
                        step="1"
                        value={toTimeInputValue(franja.horaInicio)}
                        onChange={(event) => updateFranja(franja.id, { horaInicio: event.target.value })}
                        disabled={saving}
                        required
                      />
                      {inicioFraction !== null ? <span className="muted">{inicioFraction}</span> : null}
                    </div>

                    <div className="field">
                      <label htmlFor={`franja-fin-${franja.id}`}>Fin</label>
                      <input
                        id={`franja-fin-${franja.id}`}
                        type="time"
                        step="1"
                        value={toTimeInputValue(franja.horaFin)}
                        onChange={(event) => updateFranja(franja.id, { horaFin: event.target.value })}
                        disabled={saving}
                        required
                      />
                      {finFraction !== null ? <span className="muted">{finFraction}</span> : null}
                    </div>

                    <div className="franja-actions">
                      <button
                        type="button"
                        className="btn btn-secondary"
                        onClick={() => removeFranja(franja.id)}
                        disabled={saving}
                      >
                        Quitar
                      </button>
                    </div>
                  </div>
                );
              })}
            </div>
          )}

          <div className="actions">
            <button type="button" className="btn btn-secondary" onClick={addFranja} disabled={saving}>
              Agregar franja
            </button>
            <button type="submit" className="btn btn-primary" disabled={saving}>
              {saving ? 'Guardando…' : 'Guardar horario'}
            </button>
          </div>
        </form>
      ) : null}
    </section>
  );
}
