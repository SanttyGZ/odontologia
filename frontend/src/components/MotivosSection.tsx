import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import type { ApiClient } from '../api/apiClient';
import type { Motivo } from '../types/odontologia';
import { isValidDuration } from '../types/odontologia';

const ALLOWED_DURATIONS: readonly number[] = Array.from({ length: 16 }, (_, index) => (index + 1) * 15);

interface MotivosSectionProps {
  apiClient: ApiClient;
  onSessionExpired: (message: string) => void;
}

export function MotivosSection({ apiClient, onSessionExpired }: MotivosSectionProps) {
  const [motivos, setMotivos] = useState<Motivo[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [nombre, setNombre] = useState('');
  const [duracionMinutos, setDuracionMinutos] = useState<number>(30);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    async function loadMotivos(): Promise<void> {
      setLoading(true);
      const result = await apiClient.getMotivos();
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
      setMotivos(result.data);
    }
    void loadMotivos();
    return () => {
      active = false;
    };
  }, [apiClient, onSessionExpired]);

  function startEdit(motivo: Motivo): void {
    setEditingId(motivo.id);
    setNombre(motivo.nombre);
    setDuracionMinutos(motivo.duracionMinutos);
    setErrorMessage(null);
    setSuccessMessage(null);
  }

  function cancelEdit(): void {
    setEditingId(null);
    setNombre('');
    setDuracionMinutos(30);
    setErrorMessage(null);
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>): Promise<void> {
    event.preventDefault();
    const trimmedName = nombre.trim();
    if (trimmedName.length === 0) {
      setErrorMessage('Ingresá un nombre para el motivo de consulta.');
      setSuccessMessage(null);
      return;
    }
    if (!isValidDuration(duracionMinutos)) {
      setErrorMessage('La duración debe ser un múltiplo de 15 entre 15 y 240 minutos.');
      setSuccessMessage(null);
      return;
    }

    setSaving(true);
    setErrorMessage(null);
    setSuccessMessage(null);

    if (editingId === null) {
      const result = await apiClient.createMotivo({ nombre: trimmedName, duracionMinutos });
      setSaving(false);
      if (!result.ok) {
        if (result.codigo === 'SESSION_REQUIRED') {
          onSessionExpired(result.message);
          return;
        }
        setErrorMessage(result.message);
        return;
      }
      setMotivos((prev) => [...prev, result.data]);
      setNombre('');
      setDuracionMinutos(30);
      setSuccessMessage('Motivo creado.');
      return;
    }

    const result = await apiClient.updateMotivo(editingId, {
      nombre: trimmedName,
      duracionMinutos,
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

    setMotivos((prev) => prev.map((item) => (item.id === result.data.id ? result.data : item)));
    setEditingId(null);
    setNombre('');
    setDuracionMinutos(30);
    setSuccessMessage('Motivo actualizado.');
  }

  return (
    <section className="stack" aria-labelledby="motivos-heading">
      <h2 id="motivos-heading">Motivos de consulta</h2>

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
          <label htmlFor="motivo-nombre">Nombre del motivo</label>
          <input
            id="motivo-nombre"
            type="text"
            value={nombre}
            onChange={(event) => setNombre(event.target.value)}
            disabled={saving}
            placeholder="Ej. Control general"
          />
        </div>

        <div className="field">
          <label htmlFor="motivo-duracion">Duración (minutos)</label>
          <select
            id="motivo-duracion"
            value={duracionMinutos}
            onChange={(event) => setDuracionMinutos(Number(event.target.value))}
            disabled={saving}
          >
            {ALLOWED_DURATIONS.map((minutes) => (
              <option key={minutes} value={minutes}>
                {minutes} minutos
              </option>
            ))}
          </select>
        </div>

        <div className="actions">
          <button type="submit" className="btn btn-primary" disabled={saving}>
            {saving ? 'Guardando…' : editingId === null ? 'Crear motivo' : 'Guardar cambios'}
          </button>
          {editingId !== null ? (
            <button type="button" className="btn btn-secondary" onClick={cancelEdit} disabled={saving}>
              Cancelar edición
            </button>
          ) : null}
        </div>
      </form>

      {loading ? <p aria-busy="true">Cargando motivos…</p> : null}

      {!loading && motivos.length === 0 ? (
        <p className="muted">No hay motivos cargados todavía.</p>
      ) : null}

      {!loading && motivos.length > 0 ? (
        <ul className="item-list">
          {motivos.map((motivo) => (
            <li key={motivo.id} className="item-row">
              <div>
                <strong>{motivo.nombre}</strong>
                <span className="badge">{motivo.duracionMinutos} min</span>
              </div>
              <button
                type="button"
                className="btn btn-secondary"
                onClick={() => startEdit(motivo)}
                disabled={saving}
              >
                Editar
              </button>
            </li>
          ))}
        </ul>
      ) : null}
    </section>
  );
}
