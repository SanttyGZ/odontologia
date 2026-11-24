import { useCallback, useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import type { ApiClient } from '../api/apiClient';
import type { Bloqueo } from '../types/odontologia';
import {
  formatInstantInZone,
  getConsultorioTimeZone,
  localDateTimeToInstantIso,
} from '../utils/timezone';

interface BloqueosSectionProps {
  apiClient: ApiClient;
  onSessionExpired: (message: string) => void;
  onBusyChange?: (busy: boolean) => void;
}

export function BloqueosSection({ apiClient, onSessionExpired, onBusyChange }: BloqueosSectionProps) {
  const timeZone = getConsultorioTimeZone();
  const [bloqueos, setBloqueos] = useState<Bloqueo[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [deletingId, setDeletingId] = useState<string | null>(null);
  const [fechaInicio, setFechaInicio] = useState('');
  const [horaInicio, setHoraInicio] = useState('');
  const [fechaFin, setFechaFin] = useState('');
  const [horaFin, setHoraFin] = useState('');
  const [descripcion, setDescripcion] = useState('');
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  useEffect(() => {
    onBusyChange?.(saving || deletingId !== null);
    return () => {
      onBusyChange?.(false);
    };
  }, [deletingId, onBusyChange, saving]);

  const loadBloqueos = useCallback(async (): Promise<void> => {
    setLoading(true);
    const result = await apiClient.getBloqueos();
    setLoading(false);
    if (!result.ok) {
      if (result.codigo === 'SESSION_REQUIRED') {
        onSessionExpired(result.message);
        return;
      }
      setErrorMessage(result.message);
      return;
    }
    setBloqueos(result.data);
  }, [apiClient, onSessionExpired]);

  useEffect(() => {
    void loadBloqueos();
  }, [loadBloqueos]);

  async function handleSubmit(event: FormEvent<HTMLFormElement>): Promise<void> {
    event.preventDefault();
    const inicioIso = localDateTimeToInstantIso(fechaInicio, horaInicio, timeZone);
    const finIso = localDateTimeToInstantIso(fechaFin, horaFin, timeZone);

    if (inicioIso === null || finIso === null) {
      setErrorMessage('Completá fecha y hora de inicio y fin válidas.');
      setSuccessMessage(null);
      return;
    }
    if (Date.parse(inicioIso) >= Date.parse(finIso)) {
      setErrorMessage('El inicio del bloqueo debe ser anterior al fin.');
      setSuccessMessage(null);
      return;
    }

    setSaving(true);
    setErrorMessage(null);
    setSuccessMessage(null);

    const trimmedDesc = descripcion.trim();
    const result = await apiClient.createBloqueo({
      inicio: inicioIso,
      fin: finIso,
      descripcion: trimmedDesc.length > 0 ? trimmedDesc : null,
    });
    setSaving(false);

    if (!result.ok) {
      if (result.codigo === 'SESSION_REQUIRED') {
        onSessionExpired(result.message);
        return;
      }
      setErrorMessage(result.message);
      return;
    }

    setBloqueos((prev) => [...prev, result.data]);
    setFechaInicio('');
    setHoraInicio('');
    setFechaFin('');
    setHoraFin('');
    setDescripcion('');
    setSuccessMessage('Bloqueo agregado.');
  }

  async function handleDelete(bloqueoId: string): Promise<void> {
    setDeletingId(bloqueoId);
    setErrorMessage(null);
    setSuccessMessage(null);

    const result = await apiClient.deleteBloqueo(bloqueoId);
    setDeletingId(null);

    if (!result.ok) {
      if (result.codigo === 'SESSION_REQUIRED') {
        onSessionExpired(result.message);
        return;
      }
      setErrorMessage(result.message);
      if (result.codigo === 'BLOQUEO_NOT_FOUND') {
        await loadBloqueos();
      }
      return;
    }

    setBloqueos((prev) => prev.filter((item) => item.id !== bloqueoId));
    setSuccessMessage('Bloqueo retirado.');
  }

  return (
    <section className="stack" aria-labelledby="bloqueos-heading">
      <h2 id="bloqueos-heading">Bloqueos de agenda</h2>
      <p className="subtitle">Zona horaria del consultorio: {timeZone}</p>

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

      <form onSubmit={(event) => void handleSubmit(event)} noValidate className="card form-grid">
        <div className="field">
          <label htmlFor="bloqueo-fecha-inicio">Fecha de inicio</label>
          <input
            id="bloqueo-fecha-inicio"
            type="date"
            value={fechaInicio}
            onChange={(event) => setFechaInicio(event.target.value)}
            disabled={saving}
            required
          />
        </div>

        <div className="field">
          <label htmlFor="bloqueo-hora-inicio">Hora de inicio</label>
          <input
            id="bloqueo-hora-inicio"
            type="time"
            value={horaInicio}
            onChange={(event) => setHoraInicio(event.target.value)}
            disabled={saving}
            required
          />
        </div>

        <div className="field">
          <label htmlFor="bloqueo-fecha-fin">Fecha de fin</label>
          <input
            id="bloqueo-fecha-fin"
            type="date"
            value={fechaFin}
            onChange={(event) => setFechaFin(event.target.value)}
            disabled={saving}
            required
          />
        </div>

        <div className="field">
          <label htmlFor="bloqueo-hora-fin">Hora de fin</label>
          <input
            id="bloqueo-hora-fin"
            type="time"
            value={horaFin}
            onChange={(event) => setHoraFin(event.target.value)}
            disabled={saving}
            required
          />
        </div>

        <div className="field field-full">
          <label htmlFor="bloqueo-descripcion">Descripción (opcional)</label>
          <input
            id="bloqueo-descripcion"
            type="text"
            value={descripcion}
            onChange={(event) => setDescripcion(event.target.value)}
            disabled={saving}
            placeholder="Ej. Vacaciones o capacitación"
          />
        </div>

        <div className="actions field-full">
          <button type="submit" className="btn btn-primary" disabled={saving}>
            {saving ? 'Agregando…' : 'Agregar bloqueo'}
          </button>
        </div>
      </form>

      {loading ? <p aria-busy="true">Cargando bloqueos…</p> : null}

      {!loading && bloqueos.length === 0 ? (
        <p className="muted">No hay bloqueos configurados.</p>
      ) : null}

      {!loading && bloqueos.length > 0 ? (
        <ul className="item-list">
          {bloqueos.map((bloqueo) => (
            <li key={bloqueo.id} className="item-row">
              <div>
                <strong>
                  {formatInstantInZone(bloqueo.inicio, timeZone)} —{' '}
                  {formatInstantInZone(bloqueo.fin, timeZone)}
                </strong>
                {bloqueo.descripcion !== null && bloqueo.descripcion.length > 0 ? (
                  <p className="muted">{bloqueo.descripcion}</p>
                ) : null}
              </div>
              <button
                type="button"
                className="btn btn-secondary"
                onClick={() => void handleDelete(bloqueo.id)}
                disabled={saving || deletingId === bloqueo.id}
              >
                {deletingId === bloqueo.id ? 'Retirando…' : 'Retirar'}
              </button>
            </li>
          ))}
        </ul>
      ) : null}
    </section>
  );
}
