import { useCallback, useEffect, useState } from 'react';
import { apiClient as defaultApiClient } from './api/apiClient';
import type { ApiClient } from './api/apiClient';
import { BloqueosSection } from './components/BloqueosSection';
import { HorarioSection } from './components/HorarioSection';
import { LoginForm } from './components/LoginForm';
import { MotivosSection } from './components/MotivosSection';

type SectionTab = 'motivos' | 'horario' | 'bloqueos';
type AuthState = 'checking' | 'unauthenticated' | 'authenticated';

interface AppProps {
  client?: ApiClient;
}

export function App({ client = defaultApiClient }: AppProps) {
  const [authState, setAuthState] = useState<AuthState>('checking');
  const [authNotice, setAuthNotice] = useState<string | null>(null);
  const [logoutError, setLogoutError] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<SectionTab>('motivos');
  const [sectionBusy, setSectionBusy] = useState(false);
  const [loggingOut, setLoggingOut] = useState(false);

  const handleSessionExpired = useCallback(
    (message: string) => {
      client.clearCsrfToken();
      setSectionBusy(false);
      setLogoutError(null);
      setAuthNotice(message);
      setAuthState('unauthenticated');
    },
    [client],
  );

  useEffect(() => {
    let active = true;
    async function verifyInitialSession(): Promise<void> {
      const result = await client.checkSession();
      if (!active) {
        return;
      }
      if (result.ok && result.data.autenticado) {
        setAuthNotice(null);
        setAuthState('authenticated');
        return;
      }
      setAuthState('unauthenticated');
    }
    void verifyInitialSession();
    return () => {
      active = false;
    };
  }, [client]);

  async function handleLogout(): Promise<void> {
    setLoggingOut(true);
    setLogoutError(null);
    const result = await client.logout();
    setLoggingOut(false);
    if (!result.ok) {
      if (result.codigo === 'SESSION_REQUIRED') {
        handleSessionExpired(result.message);
        return;
      }
      setLogoutError(result.message);
      return;
    }
    setSectionBusy(false);
    setAuthNotice(null);
    setAuthState('unauthenticated');
  }

  return (
    <div className="app-shell">
      <main className="app-container">
        {authState === 'checking' ? (
          <section className="card" aria-busy="true">
            <p>Verificando sesión…</p>
          </section>
        ) : null}

        {authState === 'unauthenticated' ? (
          <LoginForm
            apiClient={client}
            initialMessage={authNotice}
            onAuthenticated={() => {
              setAuthNotice(null);
              setAuthState('authenticated');
            }}
          />
        ) : null}

        {authState === 'authenticated' ? (
          <div className="workspace">
            <header className="topbar">
              <div>
                <h1>Configuración de agenda</h1>
                <p className="subtitle">Consultorio local · Odontólogo</p>
              </div>
              <button
                type="button"
                className="btn btn-secondary"
                onClick={() => void handleLogout()}
                disabled={loggingOut}
              >
                {loggingOut ? 'Cerrando…' : 'Cerrar sesión'}
              </button>
            </header>

            {logoutError !== null ? (
              <p className="feedback feedback-error" role="alert" aria-live="assertive">
                {logoutError}
              </p>
            ) : null}

            <nav className="tabs" role="tablist" aria-label="Secciones de configuración">
              <button
                type="button"
                role="tab"
                id="tab-motivos"
                aria-selected={activeTab === 'motivos'}
                aria-controls="panel-motivos"
                className={activeTab === 'motivos' ? 'tab tab-active' : 'tab'}
                onClick={() => setActiveTab('motivos')}
                disabled={sectionBusy || loggingOut}
              >
                Motivos de consulta
              </button>
              <button
                type="button"
                role="tab"
                id="tab-horario"
                aria-selected={activeTab === 'horario'}
                aria-controls="panel-horario"
                className={activeTab === 'horario' ? 'tab tab-active' : 'tab'}
                onClick={() => setActiveTab('horario')}
                disabled={sectionBusy || loggingOut}
              >
                Horario semanal
              </button>
              <button
                type="button"
                role="tab"
                id="tab-bloqueos"
                aria-selected={activeTab === 'bloqueos'}
                aria-controls="panel-bloqueos"
                className={activeTab === 'bloqueos' ? 'tab tab-active' : 'tab'}
                onClick={() => setActiveTab('bloqueos')}
                disabled={sectionBusy || loggingOut}
              >
                Bloqueos
              </button>
            </nav>

            <div
              id={`panel-${activeTab}`}
              role="tabpanel"
              aria-labelledby={`tab-${activeTab}`}
              className="panel"
            >
              {activeTab === 'motivos' ? (
                <MotivosSection
                  apiClient={client}
                  onSessionExpired={handleSessionExpired}
                  onBusyChange={setSectionBusy}
                />
              ) : null}
              {activeTab === 'horario' ? (
                <HorarioSection
                  apiClient={client}
                  onSessionExpired={handleSessionExpired}
                  onBusyChange={setSectionBusy}
                />
              ) : null}
              {activeTab === 'bloqueos' ? (
                <BloqueosSection
                  apiClient={client}
                  onSessionExpired={handleSessionExpired}
                  onBusyChange={setSectionBusy}
                />
              ) : null}
            </div>
          </div>
        ) : null}
      </main>
    </div>
  );
}
