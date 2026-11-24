import { useState } from 'react';
import type { FormEvent } from 'react';
import type { ApiClient } from '../api/apiClient';

interface LoginFormProps {
  apiClient: ApiClient;
  onAuthenticated: () => void;
  initialMessage?: string | null;
}

export function LoginForm({ apiClient, onAuthenticated, initialMessage = null }: LoginFormProps) {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(initialMessage);

  async function handleSubmit(event: FormEvent<HTMLFormElement>): Promise<void> {
    event.preventDefault();
    const trimmedUser = username.trim();
    if (trimmedUser.length === 0 || password.length === 0) {
      setErrorMessage('Completá usuario y contraseña antes de ingresar.');
      return;
    }

    setSubmitting(true);
    setErrorMessage(null);
    const result = await apiClient.login(trimmedUser, password);
    setSubmitting(false);

    if (!result.ok) {
      setErrorMessage(result.message);
      return;
    }

    setPassword('');
    onAuthenticated();
  }

  return (
    <section className="card login-card" aria-labelledby="login-heading">
      <h1 id="login-heading">Acceso interno del odontólogo</h1>
      <p className="subtitle">
        Ingresá con tu credencial local para configurar motivos, horario semanal y bloqueos.
      </p>

      {errorMessage !== null ? (
        <p className="feedback feedback-error" role="alert" aria-live="assertive">
          {errorMessage}
        </p>
      ) : null}

      <form onSubmit={(event) => void handleSubmit(event)} noValidate className="stack">
        <div className="field">
          <label htmlFor="login-username">Usuario</label>
          <input
            id="login-username"
            name="username"
            type="text"
            autoComplete="username"
            value={username}
            onChange={(event) => setUsername(event.target.value)}
            disabled={submitting}
            required
          />
        </div>

        <div className="field">
          <label htmlFor="login-password">Contraseña</label>
          <input
            id="login-password"
            name="password"
            type="password"
            autoComplete="current-password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            disabled={submitting}
            required
          />
        </div>

        <button type="submit" className="btn btn-primary" disabled={submitting}>
          {submitting ? 'Ingresando…' : 'Ingresar'}
        </button>
      </form>
    </section>
  );
}
