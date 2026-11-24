import type {
  ApiErrorCode,
  ApiResult,
  Bloqueo,
  BloqueoInput,
  CsrfResponse,
  HorarioResponse,
  Motivo,
  MotivoInput,
  SessionResponse,
} from '../types/odontologia';
import {
  isBloqueo,
  isBloqueoList,
  isCsrfResponse,
  isErrorResponse,
  isHorarioResponse,
  isMotivo,
  isMotivoList,
  isSessionResponse,
  toApiErrorCode,
} from '../types/odontologia';
import { getErrorMessage } from '../utils/errors';

export interface ApiClient {
  checkSession: () => Promise<ApiResult<SessionResponse>>;
  login: (username: string, password: string) => Promise<ApiResult<void>>;
  logout: () => Promise<ApiResult<void>>;
  getMotivos: () => Promise<ApiResult<Motivo[]>>;
  createMotivo: (input: MotivoInput) => Promise<ApiResult<Motivo>>;
  updateMotivo: (motivoId: string, input: MotivoInput) => Promise<ApiResult<Motivo>>;
  getHorario: () => Promise<ApiResult<HorarioResponse>>;
  replaceHorario: (input: HorarioResponse) => Promise<ApiResult<HorarioResponse>>;
  getBloqueos: () => Promise<ApiResult<Bloqueo[]>>;
  createBloqueo: (input: BloqueoInput) => Promise<ApiResult<Bloqueo>>;
  deleteBloqueo: (bloqueoId: string) => Promise<ApiResult<void>>;
  clearCsrfToken: () => void;
}

function fail(status: number, codigo: ApiErrorCode): ApiResult<never> {
  return {
    ok: false,
    status,
    codigo,
    message: getErrorMessage(codigo),
  };
}

async function parseJsonSafe(response: Response): Promise<unknown> {
  const text = await response.text();
  if (text.trim().length === 0) {
    return null;
  }
  try {
    return JSON.parse(text) as unknown;
  } catch {
    return null;
  }
}

export function createApiClient(fetchImpl: typeof fetch = fetch): ApiClient {
  let csrfState: CsrfResponse | null = null;

  function clearCsrfToken(): void {
    csrfState = null;
  }

  async function ensureCsrfToken(): Promise<ApiResult<CsrfResponse>> {
    if (csrfState !== null) {
      return { ok: true, data: csrfState };
    }
    let response: Response;
    try {
      response = await fetchImpl('/api/odontologo/csrf', {
        method: 'GET',
        credentials: 'same-origin',
      });
    } catch {
      return fail(0, 'NETWORK_ERROR');
    }

    const body = await parseJsonSafe(response);
    if (!response.ok) {
      const codigo = isErrorResponse(body) ? toApiErrorCode(body.codigo) : 'TECHNICAL_FAILURE';
      return fail(response.status, codigo);
    }
    if (!isCsrfResponse(body)) {
      return fail(response.status, 'INVALID_RESPONSE');
    }
    csrfState = body;
    return { ok: true, data: body };
  }

  async function parseErrorResponse(response: Response): Promise<ApiResult<never>> {
    const body = await parseJsonSafe(response);
    let codigo: ApiErrorCode;
    if (isErrorResponse(body)) {
      codigo = toApiErrorCode(body.codigo);
    } else if (response.status === 401) {
      codigo = 'SESSION_REQUIRED';
    } else if (response.status === 403) {
      codigo = 'CSRF_INVALID';
    } else {
      codigo = 'TECHNICAL_FAILURE';
    }

    if (codigo === 'CSRF_INVALID' || codigo === 'SESSION_REQUIRED') {
      clearCsrfToken();
    }
    return fail(response.status, codigo);
  }

  async function getJson<T>(url: string, guard: (value: unknown) => value is T): Promise<ApiResult<T>> {
    let response: Response;
    try {
      response = await fetchImpl(url, {
        method: 'GET',
        credentials: 'same-origin',
      });
    } catch {
      return fail(0, 'NETWORK_ERROR');
    }

    if (!response.ok) {
      return parseErrorResponse(response);
    }

    const body = await parseJsonSafe(response);
    if (!guard(body)) {
      return fail(response.status, 'INVALID_RESPONSE');
    }
    return { ok: true, data: body };
  }

  async function mutateJson<T>(
    method: 'POST' | 'PUT' | 'DELETE',
    url: string,
    payload: unknown,
    guard: ((value: unknown) => value is T) | null,
  ): Promise<ApiResult<T>> {
    const csrf = await ensureCsrfToken();
    if (!csrf.ok) {
      return csrf;
    }

    const headers: Record<string, string> = {
      [csrf.data.headerName]: csrf.data.token,
    };
    const init: RequestInit = {
      method,
      credentials: 'same-origin',
      headers,
    };
    if (payload !== undefined) {
      headers['Content-Type'] = 'application/json';
      init.body = JSON.stringify(payload);
    }

    let response: Response;
    try {
      response = await fetchImpl(url, init);
    } catch {
      return fail(0, 'NETWORK_ERROR');
    }

    if (!response.ok) {
      return parseErrorResponse(response);
    }

    if (guard === null) {
      return { ok: true, data: undefined as T };
    }

    const body = await parseJsonSafe(response);
    if (!guard(body)) {
      return fail(response.status, 'INVALID_RESPONSE');
    }
    return { ok: true, data: body };
  }

  return {
    clearCsrfToken,
    checkSession: () => getJson('/api/odontologo/sesion', isSessionResponse),
    login: async (username: string, password: string) => {
      const csrf = await ensureCsrfToken();
      if (!csrf.ok) {
        return csrf;
      }

      const params = new URLSearchParams();
      params.set('username', username);
      params.set('password', password);

      let response: Response;
      try {
        response = await fetchImpl('/api/odontologo/sesion', {
          method: 'POST',
          credentials: 'same-origin',
          headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
            [csrf.data.headerName]: csrf.data.token,
          },
          body: params.toString(),
        });
      } catch {
        return fail(0, 'NETWORK_ERROR');
      }

      if (!response.ok) {
        return parseErrorResponse(response);
      }

      // C-03 D-04: descartar el token anterior al cambiar la sesión.
      clearCsrfToken();
      return { ok: true, data: undefined };
    },
    logout: async () => {
      const result = await mutateJson<void>('POST', '/api/odontologo/sesion/cierre', undefined, null);
      if (result.ok) {
        clearCsrfToken();
      }
      return result;
    },
    getMotivos: () => getJson('/api/odontologo/motivos', isMotivoList),
    createMotivo: (input: MotivoInput) =>
      mutateJson('POST', '/api/odontologo/motivos', input, isMotivo),
    updateMotivo: (motivoId: string, input: MotivoInput) =>
      mutateJson('PUT', `/api/odontologo/motivos/${encodeURIComponent(motivoId)}`, input, isMotivo),
    getHorario: () => getJson('/api/odontologo/horario', isHorarioResponse),
    replaceHorario: (input: HorarioResponse) =>
      mutateJson('PUT', '/api/odontologo/horario', input, isHorarioResponse),
    getBloqueos: () => getJson('/api/odontologo/bloqueos', isBloqueoList),
    createBloqueo: (input: BloqueoInput) =>
      mutateJson('POST', '/api/odontologo/bloqueos', input, isBloqueo),
    deleteBloqueo: (bloqueoId: string) =>
      mutateJson<void>('DELETE', `/api/odontologo/bloqueos/${encodeURIComponent(bloqueoId)}`, undefined, null),
  };
}

export const apiClient = createApiClient();
