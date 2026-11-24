import { useEffect, useState } from 'react';
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

function toInputTime(raw: string): string {
  return raw.length >= 5 ? raw.slice(0, 5) : raw;
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
}

export function HorarioSection({ apiClient, onSessionExpired }: HorarioSectionProps) {
  const [franjas, setFranjas] = useState<FranjaSemanal[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    async function loadHorario(): Promise<void> {
      setLoading(true);
      const result = await apiClient.getHorario();
      if (!active) {
        return;
      }
      setLoading(false);
      if (!result.ok) {
        if (result.codigo === 'SESSION_REQUIRED') {
          onSessionExpired(result.message);
          return;
        }
        setErrorMessage(result.message);
        return;
      }
      setFranjas(
        result.data.franjas.map((item) => ({
          ...item,
          horaInicio: toInputTime(item.horaInicio),
          horaFin: toInputTime(item.horaFin),
        })),
      );
    }
    void loadHorario();
    return () => {
      active = false;
    };
  }, [apiClient, onSessionExpired]);

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
    for (const row of franjas) {
      const inicio = toInputTime(row.horaInicio);
      const fin = toInputTime(row.horaFin);
      if (inicio.length === 0 || fin.length === 0 || inicio >= fin) {
        setErrorMessage('Cada franja debe tener una hora de inicio anterior a la hora de fin.');
        setSuccessMessage(null);
        return;
      }
    }

    setSaving(true);
    setErrorMessage(null);
    setSuccessMessage(null);

    const payload = {
      franjas: franjas.map((row) => ({
        ...row,
        horaInicio: toInputTime(row.horaInicio),
        horaFin: toInputTime(row.horaFin),
      })),
    };

    const result = await apiClient.replaceHorario(payload);
    setSaving(false);

    if (!result.ok) {
      if (result.codigo === 'SESSION_REQUIRED') {
        onSessionExpired(result.message);
        return;
      }
      setErrorMessage(result.message);
      return;
    }

    setFranjas(
      result.data.franjas.map((item) => ({
        ...item,
        horaInicio: toInputTime(item.horaInicio),
        horaFin: toInputTime(item.horaFin),
      })),
    );
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

      {loading ? <p aria-busy="true">Cargando horario…</p> : null}

      {!loading ? (
        <form onSubmit={(event) => void handleSubmit(event)} className="stack">
          {franjas.length === 0 ? (
            <p className="muted">No hay franjas configuradas todavía.</p>
          ) : (
            <div className="stack">
              {franjas.map((franja, index) => (
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
                      value={toInputTime(franja.horaInicio)}
                      onChange={(event) => updateFranja(franja.id, { horaInicio: event.target.value })}
                      disabled={saving}
                      required
                    />
                  </div>

                  <div className="field">
                    <label htmlFor={`franja-fin-${franja.id}`}>Fin</label>
                    <input
                      id={`franja-fin-${franja.id}`}
                      type="time"
                      value={toInputTime(franja.horaFin)}
                      onChange={(event) => updateFranja(franja.id, { horaFin: event.target.value })}
                      disabled={saving}
                      required
                    />
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
              ))}
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
