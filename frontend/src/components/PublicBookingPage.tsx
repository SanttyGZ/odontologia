import { useCallback, useEffect, useRef, useState } from 'react';
import type { FormEvent } from 'react';
import type { ApiClient } from '../api/apiClient';
import type {
  HorarioDisponible,
  Motivo,
  ReservaPublicaResponse,
} from '../types/odontologia';
import { formatSlotRangeInZone, getConsultorioTimeZone } from '../utils/timezone';

interface PublicBookingPageProps {
  apiClient: ApiClient;
  onGoToDentist?: () => void;
}

const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const PHONE_REGEX = /^[0-9+()-\s]{6,30}$/;
const DNI_REGEX = /^[A-Za-z0-9]{6,20}$/;

export function PublicBookingPage({ apiClient, onGoToDentist }: PublicBookingPageProps) {
  const [motivos, setMotivos] = useState<Motivo[]>([]);
  const [motivosLoading, setMotivosLoading] = useState(true);
  const [motivosError, setMotivosError] = useState<string | null>(null);

  const [selectedMotivoId, setSelectedMotivoId] = useState<string>('');
  const [desde, setDesde] = useState<string>('');
  const [hasta, setHasta] = useState<string>('');

  const [horarios, setHorarios] = useState<HorarioDisponible[]>([]);
  const [zonaHoraria, setZonaHoraria] = useState<string>(() => getConsultorioTimeZone());
  const [slotsLoading, setSlotsLoading] = useState(false);
  const [slotsError, setSlotsError] = useState<string | null>(null);

  const [selectedSlot, setSelectedSlot] = useState<HorarioDisponible | null>(null);

  const [nombre, setNombre] = useState('');
  const [apellido, setApellido] = useState('');
  const [email, setEmail] = useState('');
  const [telefono, setTelefono] = useState('');
  const [dni, setDni] = useState('');

  const [submitting, setSubmitting] = useState(false);
  const [bookingError, setBookingError] = useState<string | null>(null);
  const [uncertainOutcome, setUncertainOutcome] = useState<string | null>(null);
  const [confirmedBooking, setConfirmedBooking] = useState<ReservaPublicaResponse | null>(null);

  const motivosReqRef = useRef(0);
  const slotsReqRef = useRef(0);

  const loadMotivos = useCallback(async () => {
    const reqId = ++motivosReqRef.current;
    setMotivosLoading(true);
    setMotivosError(null);

    const result = await apiClient.getPublicMotivos();
    if (reqId !== motivosReqRef.current) {
      return;
    }
    setMotivosLoading(false);
    if (!result.ok) {
      setMotivosError(result.message);
      return;
    }
    setMotivos(result.data);
    if (result.data.length > 0) {
      setSelectedMotivoId((prev) =>
        prev.length > 0 && result.data.some((m) => m.id === prev) ? prev : result.data[0]!.id,
      );
    }
  }, [apiClient]);

  const loadDisponibilidad = useCallback(
    async (motivoId: string, fromDate: string, toDate: string) => {
      if (motivoId.length === 0) {
        return;
      }
      const reqId = ++slotsReqRef.current;
      setSelectedSlot(null);
      if (fromDate.length > 0 && toDate.length > 0 && fromDate > toDate) {
        setSlotsLoading(false);
        setSlotsError('La fecha inicial no puede ser posterior a la fecha final.');
        setHorarios([]);
        return;
      }
      setSlotsLoading(true);
      setSlotsError(null);
      setHorarios([]);

      const result = await apiClient.getPublicDisponibilidad({
        motivoId,
        ...(fromDate.length > 0 ? { desde: fromDate } : {}),
        ...(toDate.length > 0 ? { hasta: toDate } : {}),
      });
      if (reqId !== slotsReqRef.current) {
        return;
      }
      setSlotsLoading(false);
      if (!result.ok) {
        setSlotsError(result.message);
        setHorarios([]);
        setSelectedSlot(null);
        return;
      }
      setZonaHoraria(result.data.zonaHoraria);
      setHorarios(result.data.horarios);
      setSelectedSlot((prev) =>
        prev !== null && result.data.horarios.some((h) => h.inicio === prev.inicio) ? prev : null,
      );
    },
    [apiClient],
  );

  useEffect(() => {
    void loadMotivos();
    return () => {
      motivosReqRef.current += 1;
      slotsReqRef.current += 1;
    };
  }, [loadMotivos]);

  useEffect(() => {
    if (selectedMotivoId.length > 0 && confirmedBooking === null) {
      void loadDisponibilidad(selectedMotivoId, desde, hasta);
    }
  }, [selectedMotivoId, desde, hasta, confirmedBooking, loadDisponibilidad]);

  const selectedMotivo = motivos.find((m) => m.id === selectedMotivoId) ?? null;
  const hasValidSelectedSlot =
    !slotsLoading &&
    slotsError === null &&
    selectedSlot !== null &&
    horarios.some((h) => h.inicio === selectedSlot.inicio);

  function validatePatientFields(): string | null {
    const cleanNombre = nombre.trim();
    const cleanApellido = apellido.trim();
    const cleanEmail = email.trim();
    const cleanTelefono = telefono.trim();
    const cleanDni = dni.trim();

    if (
      cleanNombre.length === 0 ||
      cleanApellido.length === 0 ||
      cleanEmail.length === 0 ||
      cleanTelefono.length === 0 ||
      cleanDni.length === 0
    ) {
      return 'Completá los cinco datos obligatorios (nombre, apellido, email, teléfono y DNI).';
    }
    if (!EMAIL_REGEX.test(cleanEmail) || cleanEmail.length > 254) {
      return 'Ingresá un email con formato válido.';
    }
    if (!PHONE_REGEX.test(cleanTelefono) || cleanTelefono.replace(/[^0-9]/g, '').length < 6) {
      return 'Ingresá un teléfono válido (al menos 6 dígitos).';
    }
    if (!DNI_REGEX.test(cleanDni)) {
      return 'Ingresá un DNI válido sin espacios (6 a 20 caracteres alfanuméricos).';
    }
    return null;
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>): Promise<void> {
    event.preventDefault();
    if (submitting || !hasValidSelectedSlot || selectedSlot === null || selectedMotivoId.length === 0) {
      return;
    }

    setBookingError(null);
    setUncertainOutcome(null);

    const validationError = validatePatientFields();
    if (validationError !== null) {
      setBookingError(validationError);
      return;
    }

    setSubmitting(true);
    let result;
    try {
      result = await apiClient.createPublicTurno({
        motivoId: selectedMotivoId,
        inicio: selectedSlot.inicio,
        nombre: nombre.trim(),
        apellido: apellido.trim(),
        email: email.trim(),
        telefono: telefono.trim(),
        dni: dni.trim(),
      });
    } catch {
      setUncertainOutcome(
        'Resultado incierto: no se pudo confirmar si la reserva llegó a registrarse por un fallo al leer la respuesta. Revisá tu casilla de correo o actualizá la disponibilidad antes de volver a enviar.',
      );
      return;
    } finally {
      setSubmitting(false);
    }

    if (result.ok) {
      setConfirmedBooking(result.data);
      return;
    }

    if (result.codigo === 'NETWORK_ERROR' || result.codigo === 'INVALID_RESPONSE') {
      setUncertainOutcome(
        'Resultado incierto: no se pudo confirmar si la reserva llegó a registrarse por un fallo de conexión o respuesta. Revisá tu casilla de correo o actualizá la disponibilidad antes de volver a enviar.',
      );
      return;
    }

    if (result.codigo === 'SLOT_UNAVAILABLE') {
      setSelectedSlot(null);
      setBookingError(result.message);
      void loadDisponibilidad(selectedMotivoId, desde, hasta);
      return;
    }

    if (result.codigo === 'MOTIVO_NOT_FOUND') {
      setSelectedSlot(null);
      setBookingError(result.message);
      void loadMotivos();
      return;
    }

    setBookingError(result.message);
  }

  function handleResetForNewBooking(): void {
    setConfirmedBooking(null);
    setSelectedSlot(null);
    setBookingError(null);
    setUncertainOutcome(null);
    setNombre('');
    setApellido('');
    setEmail('');
    setTelefono('');
    setDni('');
  }

  return (
    <div className="workspace">
      <header className="topbar">
        <div>
          <h1>Reservar turno</h1>
          <p className="subtitle">Reserva directa sin cuenta · Zona {zonaHoraria}</p>
        </div>
        {onGoToDentist !== undefined ? (
          <button type="button" className="btn btn-secondary" onClick={onGoToDentist}>
            Acceso odontólogo
          </button>
        ) : null}
      </header>

      {confirmedBooking !== null ? (
        <section className="card stack" aria-labelledby="booking-confirmed-heading">
          <div className="feedback feedback-success">
            <h2 id="booking-confirmed-heading">Reserva confirmada</h2>
            <p className="muted">
              Estado del turno: <strong>{confirmedBooking.estado}</strong>. Tu turno ya quedó
              registrado y no requiere verificar previamente tu email ni pulsar un enlace adicional
              para confirmarlo.
            </p>
          </div>

          <div className="summary-grid">
            <div>
              <strong>Motivo:</strong> {selectedMotivo?.nombre ?? confirmedBooking.motivoId}
            </div>
            <div>
              <strong>Duración reservada:</strong> {confirmedBooking.duracionReservadaMinutos} min
            </div>
            <div>
              <strong>Horario ({zonaHoraria}):</strong>{' '}
              {formatSlotRangeInZone(confirmedBooking.inicio, confirmedBooking.fin, zonaHoraria)}
            </div>
          </div>

          <div className="feedback feedback-warning" role="status">
            <strong>Estado del correo de confirmación: {confirmedBooking.notificacionEstado}</strong>
            <p className="muted">
              El envío del correo con los enlaces de autogestión se procesa de manera independiente.
              Recibir esta confirmación en pantalla no acredita que el mensaje ya haya ingresado a
              tu buzón.
            </p>
          </div>

          <div className="actions">
            <button type="button" className="btn btn-secondary" onClick={handleResetForNewBooking}>
              Consultar otro turno
            </button>
          </div>
        </section>
      ) : (
        <>
          <section className="card stack" aria-labelledby="step-motivo-heading">
            <h2 id="step-motivo-heading">1. Elegí motivo y período</h2>

            {motivosLoading ? <p aria-busy="true">Cargando motivos de consulta…</p> : null}

            {motivosError !== null ? (
              <div className="stack">
                <p className="feedback feedback-error" role="alert">
                  {motivosError}
                </p>
                <div className="actions">
                  <button
                    type="button"
                    className="btn btn-secondary"
                    onClick={() => void loadMotivos()}
                  >
                    Reintentar motivos
                  </button>
                </div>
              </div>
            ) : null}

            {!motivosLoading && motivosError === null && motivos.length === 0 ? (
              <p className="muted">Todavía no hay motivos de consulta disponibles para reservar.</p>
            ) : null}

            {!motivosLoading && motivosError === null && motivos.length > 0 ? (
              <div className="form-grid">
                <div className="field">
                  <label htmlFor="public-motivo">Motivo de consulta</label>
                  <select
                    id="public-motivo"
                    value={selectedMotivoId}
                    disabled={submitting}
                    onChange={(e) => {
                      setSelectedMotivoId(e.target.value);
                      setSelectedSlot(null);
                      setBookingError(null);
                      setUncertainOutcome(null);
                    }}
                  >
                    {motivos.map((motivo) => (
                      <option key={motivo.id} value={motivo.id}>
                        {motivo.nombre} ({motivo.duracionMinutos} min)
                      </option>
                    ))}
                  </select>
                </div>

                <div className="field">
                  <label htmlFor="public-desde">Desde (opcional)</label>
                  <input
                    id="public-desde"
                    type="date"
                    value={desde}
                    disabled={submitting}
                    onChange={(e) => {
                      setDesde(e.target.value);
                      setSelectedSlot(null);
                      setBookingError(null);
                      setUncertainOutcome(null);
                    }}
                  />
                </div>

                <div className="field">
                  <label htmlFor="public-hasta">Hasta (opcional)</label>
                  <input
                    id="public-hasta"
                    type="date"
                    value={hasta}
                    disabled={submitting}
                    onChange={(e) => {
                      setHasta(e.target.value);
                      setSelectedSlot(null);
                      setBookingError(null);
                      setUncertainOutcome(null);
                    }}
                  />
                </div>
              </div>
            ) : null}
          </section>

          {selectedMotivoId.length > 0 ? (
            <section className="card stack" aria-labelledby="step-slots-heading">
              <div className="item-row">
                <h2 id="step-slots-heading">2. Seleccioná un horario disponible</h2>
                <button
                  type="button"
                  className="btn btn-secondary"
                  disabled={slotsLoading || submitting}
                  onClick={() => void loadDisponibilidad(selectedMotivoId, desde, hasta)}
                >
                  Actualizar horarios
                </button>
              </div>

              {slotsLoading ? <p aria-busy="true">Consultando horarios disponibles…</p> : null}

              {slotsError !== null ? (
                <div className="stack">
                  <p className="feedback feedback-error" role="alert">
                    {slotsError}
                  </p>
                  <div className="actions">
                    <button
                      type="button"
                      className="btn btn-secondary"
                      onClick={() => void loadDisponibilidad(selectedMotivoId, desde, hasta)}
                    >
                      Reintentar
                    </button>
                  </div>
                </div>
              ) : null}

              {!slotsLoading && slotsError === null && horarios.length === 0 ? (
                <p className="muted" role="status">
                  No hay horarios disponibles para el motivo y período seleccionados.
                </p>
              ) : null}

              {!slotsLoading && slotsError === null && horarios.length > 0 ? (
                <div className="slot-grid" role="group" aria-label="Horarios disponibles">
                  {horarios.map((slot) => {
                    const isSelected = selectedSlot?.inicio === slot.inicio;
                    return (
                      <button
                        key={slot.inicio}
                        type="button"
                        aria-pressed={isSelected}
                        disabled={submitting}
                        className={isSelected ? 'slot-btn slot-btn-selected' : 'slot-btn'}
                        onClick={() => {
                          setSelectedSlot(slot);
                          setBookingError(null);
                          setUncertainOutcome(null);
                        }}
                      >
                        {formatSlotRangeInZone(slot.inicio, slot.fin, zonaHoraria)}
                      </button>
                    );
                  })}
                </div>
              ) : null}
            </section>
          ) : null}

          {bookingError !== null ? (
            <p className="feedback feedback-error" role="alert" aria-live="assertive">
              {bookingError}
            </p>
          ) : null}

          {uncertainOutcome !== null ? (
            <p className="feedback feedback-warning" role="alert" aria-live="assertive">
              {uncertainOutcome}
            </p>
          ) : null}

          {hasValidSelectedSlot && selectedSlot !== null ? (
            <section className="card stack" aria-labelledby="step-patient-heading">
              <h2 id="step-patient-heading">3. Completá tus datos para confirmar</h2>
              <p className="muted">
                Horario seleccionado:{' '}
                <strong>
                  {formatSlotRangeInZone(selectedSlot.inicio, selectedSlot.fin, zonaHoraria)}
                </strong>
              </p>

              <form className="stack" onSubmit={(e) => void handleSubmit(e)} noValidate>
                <div className="form-grid">
                  <div className="field">
                    <label htmlFor="paciente-nombre">Nombre</label>
                    <input
                      id="paciente-nombre"
                      type="text"
                      autoComplete="given-name"
                      value={nombre}
                      disabled={submitting}
                      onChange={(e) => setNombre(e.target.value)}
                      required
                    />
                  </div>

                  <div className="field">
                    <label htmlFor="paciente-apellido">Apellido</label>
                    <input
                      id="paciente-apellido"
                      type="text"
                      autoComplete="family-name"
                      value={apellido}
                      disabled={submitting}
                      onChange={(e) => setApellido(e.target.value)}
                      required
                    />
                  </div>

                  <div className="field">
                    <label htmlFor="paciente-email">Email</label>
                    <input
                      id="paciente-email"
                      type="email"
                      autoComplete="email"
                      value={email}
                      disabled={submitting}
                      onChange={(e) => setEmail(e.target.value)}
                      required
                    />
                  </div>

                  <div className="field">
                    <label htmlFor="paciente-telefono">Teléfono</label>
                    <input
                      id="paciente-telefono"
                      type="tel"
                      autoComplete="tel"
                      value={telefono}
                      disabled={submitting}
                      onChange={(e) => setTelefono(e.target.value)}
                      required
                    />
                  </div>

                  <div className="field">
                    <label htmlFor="paciente-dni">DNI</label>
                    <input
                      id="paciente-dni"
                      type="text"
                      value={dni}
                      disabled={submitting}
                      onChange={(e) => setDni(e.target.value)}
                      required
                    />
                  </div>
                </div>

                <div className="actions">
                  <button type="submit" className="btn btn-primary" disabled={submitting}>
                    {submitting ? 'Confirmando reserva…' : 'Confirmar reserva'}
                  </button>
                </div>
              </form>
            </section>
          ) : null}
        </>
      )}
    </div>
  );
}
