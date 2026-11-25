const DEFAULT_TIME_ZONE = 'America/Argentina/Mendoza';
const ISO_CALENDAR_DATE_REGEX = /^(\d{4})-(\d{2})-(\d{2})$/;

export function isValidIsoCalendarDate(value: string): boolean {
  const match = ISO_CALENDAR_DATE_REGEX.exec(value);
  if (match === null) {
    return false;
  }
  const year = Number(match[1]);
  const month = Number(match[2]);
  const day = Number(match[3]);
  if (month < 1 || month > 12 || day < 1 || day > 31) {
    return false;
  }
  const date = new Date(Date.UTC(year, month - 1, day));
  return (
    date.getUTCFullYear() === year &&
    date.getUTCMonth() === month - 1 &&
    date.getUTCDate() === day
  );
}

export function shiftIsoCalendarDate(ymd: string, deltaDays: number): string {
  const match = ISO_CALENDAR_DATE_REGEX.exec(ymd);
  if (match === null || !isValidIsoCalendarDate(ymd)) {
    return ymd;
  }
  const year = Number(match[1]);
  const month = Number(match[2]);
  const day = Number(match[3]);
  const shifted = new Date(Date.UTC(year, month - 1, day + deltaDays));
  const yyyy = String(shifted.getUTCFullYear()).padStart(4, '0');
  const mm = String(shifted.getUTCMonth() + 1).padStart(2, '0');
  const dd = String(shifted.getUTCDate()).padStart(2, '0');
  return `${yyyy}-${mm}-${dd}`;
}

export function formatIsoCalendarDate(ymd: string): string {
  const match = ISO_CALENDAR_DATE_REGEX.exec(ymd);
  if (match === null) {
    return ymd;
  }
  return `${match[3]}/${match[2]}/${match[1]}`;
}

export function getConsultorioTimeZone(configured?: string): string {
  const candidate = configured ?? import.meta.env.VITE_APP_TIME_ZONE;
  if (typeof candidate === 'string' && candidate.trim().length > 0) {
    return candidate.trim();
  }
  return DEFAULT_TIME_ZONE;
}

function getZonedParts(date: Date, timeZone: string): {
  year: number;
  month: number;
  day: number;
  hour: number;
  minute: number;
  second: number;
} {
  const formatter = new Intl.DateTimeFormat('en-US', {
    timeZone,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hourCycle: 'h23',
  });
  const parts = formatter.formatToParts(date);
  const lookup = (type: Intl.DateTimeFormatPartTypes): number => {
    const found = parts.find((part) => part.type === type)?.value;
    return Number(found ?? '0');
  };
  return {
    year: lookup('year'),
    month: lookup('month'),
    day: lookup('day'),
    hour: lookup('hour'),
    minute: lookup('minute'),
    second: lookup('second'),
  };
}

export function localDateTimeToInstantIso(
  dateStr: string,
  timeStr: string,
  timeZone: string,
): string | null {
  const dateMatch = /^(\d{4})-(\d{2})-(\d{2})$/.exec(dateStr.trim());
  const timeMatch = /^(\d{2}):(\d{2})$/.exec(timeStr.trim());
  if (dateMatch === null || timeMatch === null) {
    return null;
  }

  const year = Number(dateMatch[1]);
  const month = Number(dateMatch[2]);
  const day = Number(dateMatch[3]);
  const hour = Number(timeMatch[1]);
  const minute = Number(timeMatch[2]);

  const targetUtcMs = Date.UTC(year, month - 1, day, hour, minute, 0, 0);
  let candidateMs = targetUtcMs;

  for (let i = 0; i < 2; i += 1) {
    const zoned = getZonedParts(new Date(candidateMs), timeZone);
    const zonedAsUtcMs = Date.UTC(
      zoned.year,
      zoned.month - 1,
      zoned.day,
      zoned.hour,
      zoned.minute,
      zoned.second,
      0,
    );
    const diff = zonedAsUtcMs - targetUtcMs;
    candidateMs -= diff;
  }

  return new Date(candidateMs).toISOString();
}

export function formatInstantInZone(isoInstant: string, timeZone: string): string {
  const parsed = new Date(isoInstant);
  if (Number.isNaN(parsed.getTime())) {
    return isoInstant;
  }
  return new Intl.DateTimeFormat('es-AR', {
    timeZone,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).format(parsed);
}

export function formatSlotRangeInZone(
  inicioIso: string,
  finIso: string,
  timeZone: string,
): string {
  const start = new Date(inicioIso);
  const end = new Date(finIso);
  if (Number.isNaN(start.getTime()) || Number.isNaN(end.getTime())) {
    return `${inicioIso} – ${finIso}`;
  }
  const hasSeconds = start.getUTCSeconds() !== 0 || end.getUTCSeconds() !== 0;
  const dateFmt = new Intl.DateTimeFormat('es-AR', {
    timeZone,
    weekday: 'short',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  });
  const timeFmt = new Intl.DateTimeFormat('es-AR', {
    timeZone,
    hour: '2-digit',
    minute: '2-digit',
    ...(hasSeconds ? { second: '2-digit' } : {}),
    hour12: false,
  });
  return `${dateFmt.format(start)} · ${timeFmt.format(start)} a ${timeFmt.format(end)}`;
}
