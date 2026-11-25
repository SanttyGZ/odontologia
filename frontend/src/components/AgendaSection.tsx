import { useCallback, useEffect, useRef, useState } from 'react';
import type { ApiClient } from '../api/apiClient';
import type {
  AgendaQueryParams,
  AgendaResponse,
  AgendaVista,
  TurnoDetalleResponse,
} from '../types/odontologia';
import {
  formatInstantInZone,
  formatIsoCalendarDate,
  formatSlotRangeInZone,
  getConsultorioTimeZone,
  isValidIsoCalendarDate,
  shiftIsoCalendarDate,
} from '../utils/timezone';

interface AgendaSectionProps {
  apiClient: ApiClient;
  onSessionExpired: (message: string) => void;
}

function formatPeriodLabel(agenda: AgendaResponse): string {
  if (agenda.vista === 'SEMANA') {
    return `${formatIsoCalendarDate(agenda.desde)} al ${formatIsoCalendarDate(agenda.hasta)}`;
  }
  return formatIsoCalendarDate(agenda.desde);
}

function buildAgendaQuery(vista: AgendaVista, fecha: string | null): AgendaQueryParams {
  return fecha !== null ? { vista, fecha } : { vista };
}

export function AgendaSection({ apiClient, onSessionExpired }: AgendaSectionProps) {
  const [vista, setVista] = useState<AgendaVista>('DIA');
  const [selectedFecha, setSelectedFecha] = useState<string | null>(null);
  const [dateInput, setDateInput] = useState<string>('');
  const [agenda, setAgenda] = useState<AgendaResponse | null>(null);
  const [loadingAgenda, setLoadingAgenda] = useState<boolean>(true);
  const [agendaError, setAgendaError] = useState<string | null>(null);

  const [selectedTurnoId, setSelectedTurnoId] = useState<string | null>(null);
  const [turnoDetalle, setTurnoDetalle] = useState<TurnoDetalleResponse | null>(null);
  const [loadingDetalle, setLoadingDetalle] = useState<boolean>(false);
  const [detalleError, setDetalleError] = useState<string | null>(null);

  const latestAgendaRequestIdRef = useRef<number>(0);
  const latestDetalleRequestIdRef = useRef<number>(0);
  const selectedTurnoIdRef = useRef<string | null>(null);
  const isMountedRef = useRef<boolean>(true);

  useEffect(() => {
    isMountedRef.current = true;
    return () => {
      isMountedRef.current = false;
    };
  }, []);

  const clearDetalleState = useCallback(() => {
    latestDetalleRequestIdRef.current += 1;
    selectedTurnoIdRef.current = null;
    setSelectedTurnoId(null);
    setTurnoDetalle(null);
    setLoadingDetalle(false);
    setDetalleError(null);
  }, []);

  const fetchAgenda = useCallback(
    async (targetVista: AgendaVista, targetFecha: string | null): Promise<void> => {
      latestAgendaRequestIdRef.current += 1;
      const requestId = latestAgendaRequestIdRef.current;

      setLoadingAgenda(true);
      setAgendaError(null);

      const result = await apiClient.getAgenda(buildAgendaQuery(targetVista, targetFecha));

      if (!isMountedRef.current || requestId !== latestAgendaRequestIdRef.current) {
        return;
      }

      setLoadingAgenda(false);

      if (!result.ok) {
        if (result.codigo === 'SESSION_REQUIRED') {
          onSessionExpired(result.message);
          return;
        }
        setAgenda(null);
        clearDetalleState();
        setAgendaError(result.message);
        return;
      }

      setAgenda(result.data);
      setDateInput(targetFecha ?? result.data.desde);

      const currentSelectedId = selectedTurnoIdRef.current;
      if (
        currentSelectedId !== null &&
        !result.data.turnos.some((turno) => turno.id === currentSelectedId)
      ) {
        clearDetalleState();
      }
    },
    [apiClient, clearDetalleState, onSessionExpired],
  );

  useEffect(() => {
    void fetchAgenda('DIA', null);
  }, [fetchAgenda]);

  async function fetchDetalle(turnoId: string): Promise<void> {
    latestDetalleRequestIdRef.current += 1;
    const requestId = latestDetalleRequestIdRef.current;

    selectedTurnoIdRef.current = turnoId;
    setSelectedTurnoId(turnoId);
    setLoadingDetalle(true);
    setDetalleError(null);
    setTurnoDetalle(null);

    const result = await apiClient.getTurnoDetalle(turnoId);

    if (!isMountedRef.current || requestId !== latestDetalleRequestIdRef.current) {
      return;
    }

    setLoadingDetalle(false);

    if (!result.ok) {
      if (result.codigo === 'SESSION_REQUIRED') {
        onSessionExpired(result.message);
        return;
      }
      setDetalleError(result.message);
      return;
    }

    setTurnoDetalle(result.data);
  }

  async function handleRefresh(): Promise<void> {
    if (selectedTurnoId === null) {
      await fetchAgenda(vista, selectedFecha);
      return;
    }

    const currentTurnoId = selectedTurnoId;
    latestAgendaRequestIdRef.current += 1;
    const agendaRequestId = latestAgendaRequestIdRef.current;
    latestDetalleRequestIdRef.current += 1;
    const detalleRequestId = latestDetalleRequestIdRef.current;

    setLoadingAgenda(true);
    setAgendaError(null);
    setLoadingDetalle(true);
    setDetalleError(null);

    const [agendaResult, detalleResult] = await Promise.all([
      apiClient.getAgenda(buildAgendaQuery(vista, selectedFecha)),
      apiClient.getTurnoDetalle(currentTurnoId),
    ]);

    if (!isMountedRef.current) {
      return;
    }

    const isLatestAgenda = agendaRequestId === latestAgendaRequestIdRef.current;
    const isLatestDetalle = detalleRequestId === latestDetalleRequestIdRef.current;

    if (!isLatestAgenda && !isLatestDetalle) {
      return;
    }

    if (isLatestAgenda && !agendaResult.ok && agendaResult.codigo === 'SESSION_REQUIRED') {
      onSessionExpired(agendaResult.message);
      return;
    }
    if (isLatestDetalle && !detalleResult.ok && detalleResult.codigo === 'SESSION_REQUIRED') {
      onSessionExpired(detalleResult.message);
      return;
    }

    if (isLatestAgenda) {
      setLoadingAgenda(false);
      if (!agendaResult.ok) {
        setAgenda(null);
        clearDetalleState();
        setAgendaError(agendaResult.message);
        return;
      }
      setAgenda(agendaResult.data);
      setDateInput(selectedFecha ?? agendaResult.data.desde);
    }

    if (!isLatestDetalle) {
      return;
    }

    setLoadingDetalle(false);

    const stillInAgenda =
      agendaResult.ok && agendaResult.data.turnos.some((turno) => turno.id === currentTurnoId);

    if (!stillInAgenda || (!detalleResult.ok && detalleResult.codigo === 'TURNO_NOT_FOUND')) {
      clearDetalleState();
      return;
    }

    if (!detalleResult.ok) {
      setTurnoDetalle(null);
      setDetalleError(detalleResult.message);
      return;
    }

    setTurnoDetalle(detalleResult.data);
  }

  function handleVistaChange(nextVista: AgendaVista): void {
    if (nextVista === vista) {
      return;
    }
    setVista(nextVista);
    void fetchAgenda(nextVista, selectedFecha);
  }

  function handleShiftPeriod(direction: -1 | 1): void {
    const baseDate = selectedFecha ?? agenda?.desde ?? null;
    if (baseDate === null || !isValidIsoCalendarDate(baseDate)) {
      return;
    }
    const stepDays = vista === 'SEMANA' ? 7 : 1;
    const nextFecha = shiftIsoCalendarDate(baseDate, direction * stepDays);
    setSelectedFecha(nextFecha);
    setDateInput(nextFecha);
    void fetchAgenda(vista, nextFecha);
  }

  function handleDateInputChange(value: string): void {
    setDateInput(value);
    if (!isValidIsoCalendarDate(value)) {
      latestAgendaRequestIdRef.current += 1;
      setLoadingAgenda(false);
      setAgenda(null);
      clearDetalleState();
      setAgendaError('Ingresá una fecha válida en formato AAAA-MM-DD.');
      return;
    }
    setSelectedFecha(value);
    void fetchAgenda(vista, value);
  }

  function handleToday(): void {
    setSelectedFecha(null);
    void fetchAgenda(vista, null);
  }

  const timeZone = agenda?.zonaHoraria ?? getConsultorioTimeZone();

  return (
    <section className="stack" aria-labelledby="agenda-heading">
      <div className="agenda-period-bar">
        <div>
          <h2 id="agenda-heading">Agenda</h2>
          {agenda !== null ? (
            <p className="subtitle">
              Período: <strong>{formatPeriodLabel(agenda)}</strong> · Zona horaria: {agenda.zonaHoraria}
            </p>
          ) : null}
        </div>
        <button
          type="button"
          className="btn btn-secondary"
          onClick={() => void handleRefresh()}
          disabled={loadingAgenda}
        >
          Actualizar
        </button>
      </div>

      <div className="card agenda-toolbar">
        <div className="actions" role="group" aria-label="Vista de agenda">
          <button
            type="button"
            className={vista === 'DIA' ? 'btn btn-primary' : 'btn btn-secondary'}
            aria-pressed={vista === 'DIA'}
            onClick={() => handleVistaChange('DIA')}
          >
            Día
          </button>
          <button
            type="button"
            className={vista === 'SEMANA' ? 'btn btn-primary' : 'btn btn-secondary'}
            aria-pressed={vista === 'SEMANA'}
            onClick={() => handleVistaChange('SEMANA')}
          >
            Semana
          </button>
        </div>

        <div className="actions" role="group" aria-label="Navegación de período">
          <button
            type="button"
            className="btn btn-secondary"
            onClick={() => handleShiftPeriod(-1)}
            disabled={loadingAgenda || (selectedFecha === null && agenda === null)}
          >
            Anterior
          </button>
          <button
            type="button"
            className="btn btn-secondary"
            onClick={handleToday}
            disabled={loadingAgenda}
          >
            Hoy
          </button>
          <button
            type="button"
            className="btn btn-secondary"
            onClick={() => handleShiftPeriod(1)}
            disabled={loadingAgenda || (selectedFecha === null && agenda === null)}
          >
            Siguiente
          </button>
        </div>

        <div className="field agenda-date-field">
          <label htmlFor="agenda-fecha">Fecha</label>
          <input
            id="agenda-fecha"
            type="date"
            value={dateInput}
            onChange={(event) => handleDateInputChange(event.target.value)}
          />
        </div>
      </div>

      {agendaError !== null ? (
        <div className="feedback feedback-error agenda-error-box" role="alert" aria-live="assertive">
          <p className="agenda-error-text">{agendaError}</p>
          <button
            type="button"
            className="btn btn-secondary"
            onClick={() => void fetchAgenda(vista, selectedFecha)}
          >
            Reintentar
          </button>
        </div>
      ) : null}

      {loadingAgenda ? <p aria-busy="true">Cargando agenda…</p> : null}

      {!loadingAgenda && agenda !== null ? (
        <div className="agenda-layout">
          <div className="agenda-list-column">
            {agenda.turnos.length === 0 ? (
              <p className="card muted">No hay turnos registrados para este período.</p>
            ) : (
              <ul className="item-list">
                {agenda.turnos.map((turno) => {
                  const isSelected = selectedTurnoId === turno.id;
                  const isCancelado = turno.estado === 'CANCELADO';
                  const rowClasses = [
                    'item-row',
                    isSelected ? 'item-row-selected' : '',
                    isCancelado ? 'item-row-cancelado' : '',
                  ]
                    .filter((cls) => cls.length > 0)
                    .join(' ');

                  return (
                    <li key={turno.id} className={rowClasses}>
                      <button
                        type="button"
                        className="agenda-turno-btn"
                        aria-pressed={isSelected}
                        onClick={() => void fetchDetalle(turno.id)}
                      >
                        <div className="agenda-turno-header">
                          <strong>{formatSlotRangeInZone(turno.inicio, turno.fin, timeZone)}</strong>
                          <span
                            className={
                              isCancelado
                                ? 'badge badge-cancelado'
                                : 'badge badge-confirmado'
                            }
                          >
                            {isCancelado ? 'Cancelado · horario liberado' : 'Confirmado'}
                          </span>
                        </div>
                        <div className="agenda-turno-meta">
                          <span>
                            {turno.pacienteNombre} {turno.pacienteApellido}
                          </span>
                          <span className="muted">
                            {turno.motivoNombre} · {turno.duracionReservadaMinutos} min
                          </span>
                        </div>
                      </button>
                    </li>
                  );
                })}
              </ul>
            )}
          </div>

          {selectedTurnoId !== null ? (
            <section className="card agenda-detail" aria-label="Detalle del turno">
              <div className="agenda-detail-header">
                <h3>Detalle del turno</h3>
                <button
                  type="button"
                  className="btn btn-secondary"
                  onClick={clearDetalleState}
                >
                  Cerrar detalle
                </button>
              </div>

              {loadingDetalle ? <p aria-busy="true">Cargando detalle…</p> : null}

              {!loadingDetalle && detalleError !== null ? (
                <p className="feedback feedback-error" role="alert" aria-live="assertive">
                  {detalleError}
                </p>
              ) : null}

              {!loadingDetalle && turnoDetalle !== null ? (
                <dl className="agenda-detail-list">
                  <div>
                    <dt>Motivo</dt>
                    <dd>{turnoDetalle.motivoNombre}</dd>
                  </div>
                  <div>
                    <dt>Horario</dt>
                    <dd>{formatSlotRangeInZone(turnoDetalle.inicio, turnoDetalle.fin, timeZone)}</dd>
                  </div>
                  <div>
                    <dt>Duración reservada</dt>
                    <dd>{turnoDetalle.duracionReservadaMinutos} min</dd>
                  </div>
                  <div>
                    <dt>Estado</dt>
                    <dd>
                      {turnoDetalle.estado === 'CONFIRMADO'
                        ? 'Confirmado'
                        : 'Cancelado · horario liberado'}
                    </dd>
                  </div>
                  <div>
                    <dt>Revisión</dt>
                    <dd>Revisión: {turnoDetalle.revision}</dd>
                  </div>
                  <div>
                    <dt>Creado en</dt>
                    <dd>{formatInstantInZone(turnoDetalle.creadoEn, timeZone)}</dd>
                  </div>
                  {turnoDetalle.canceladoEn !== null ? (
                    <div>
                      <dt>Cancelado en</dt>
                      <dd>{formatInstantInZone(turnoDetalle.canceladoEn, timeZone)}</dd>
                    </div>
                  ) : null}
                  <div>
                    <dt>Paciente</dt>
                    <dd>
                      {turnoDetalle.paciente.nombre} {turnoDetalle.paciente.apellido}
                    </dd>
                  </div>
                  <div>
                    <dt>Email</dt>
                    <dd>{turnoDetalle.paciente.email}</dd>
                  </div>
                  <div>
                    <dt>Teléfono</dt>
                    <dd>{turnoDetalle.paciente.telefono}</dd>
                  </div>
                  <div>
                    <dt>DNI</dt>
                    <dd>{turnoDetalle.paciente.dni}</dd>
                  </div>
                </dl>
              ) : null}
            </section>
          ) : null}
        </div>
      ) : null}
    </section>
  );
}
