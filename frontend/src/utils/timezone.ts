const DEFAULT_TIME_ZONE = 'America/Argentina/Mendoza';

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
