import { describe, expect, it } from 'vitest';
import {
  formatInstantInZone,
  formatIsoCalendarDate,
  formatSlotRangeInZone,
  getConsultorioTimeZone,
  isValidIsoCalendarDate,
  localDateTimeToInstantIso,
  shiftIsoCalendarDate,
} from './timezone';

describe('timezone helpers', () => {
  it('devuelve America/Argentina/Mendoza por defecto o el valor configurado explicito', () => {
    expect(getConsultorioTimeZone(undefined)).toBe('America/Argentina/Mendoza');
    expect(getConsultorioTimeZone('')).toBe('America/Argentina/Mendoza');
    expect(getConsultorioTimeZone('America/Argentina/Cordoba')).toBe('America/Argentina/Cordoba');
  });

  it('convierte fecha y hora local en zona del consultorio a Instant ISO-8601 UTC', () => {
    const iso = localDateTimeToInstantIso('2026-12-01', '09:00', 'America/Argentina/Mendoza');
    expect(iso).toBe('2026-12-01T12:00:00.000Z');
  });

  it('formatea un Instant ISO-8601 en la zona del consultorio', () => {
    const formatted = formatInstantInZone('2026-12-01T12:00:00Z', 'America/Argentina/Mendoza');
    expect(formatted).toContain('09:00');
  });

  it('formatea un rango de turno mostrando segundos si el inicio o fin los incluye', () => {
    const exactMinute = formatSlotRangeInZone(
      '2030-01-08T12:00:00Z',
      '2030-01-08T12:30:00Z',
      'America/Argentina/Mendoza',
    );
    expect(exactMinute).toContain('09:00');
    expect(exactMinute).toContain('09:30');

    const withSeconds = formatSlotRangeInZone(
      '2030-01-08T12:00:30Z',
      '2030-01-08T12:30:30Z',
      'America/Argentina/Mendoza',
    );
    expect(withSeconds).toContain('09:00:30');
    expect(withSeconds).toContain('09:30:30');
  });

  it('valida fechas calendario ISO-8601 estrictas incluyendo bisiestos', () => {
    expect(isValidIsoCalendarDate('2026-11-25')).toBe(true);
    expect(isValidIsoCalendarDate('2028-02-29')).toBe(true);

    expect(isValidIsoCalendarDate('')).toBe(false);
    expect(isValidIsoCalendarDate('2026-2-5')).toBe(false);
    expect(isValidIsoCalendarDate('2026-02-29')).toBe(false);
    expect(isValidIsoCalendarDate('2026-02-30')).toBe(false);
    expect(isValidIsoCalendarDate('2026-13-01')).toBe(false);
    expect(isValidIsoCalendarDate('2026-00-10')).toBe(false);
    expect(isValidIsoCalendarDate('2026-11-25Z')).toBe(false);
    expect(isValidIsoCalendarDate('2026-11-25-03:00')).toBe(false);
    expect(isValidIsoCalendarDate(' 2026-11-25')).toBe(false);
    expect(isValidIsoCalendarDate('+10000-01-01')).toBe(false);
  });

  it('desplaza fechas calendario ISO-8601 sin deriva por zona horaria local', () => {
    expect(shiftIsoCalendarDate('2026-11-30', 1)).toBe('2026-12-01');
    expect(shiftIsoCalendarDate('2026-12-01', -1)).toBe('2026-11-30');
    expect(shiftIsoCalendarDate('2026-12-28', 7)).toBe('2027-01-04');
    expect(shiftIsoCalendarDate('2027-01-04', -7)).toBe('2026-12-28');
    expect(shiftIsoCalendarDate('2028-02-28', 1)).toBe('2028-02-29');
  });

  it('formatea fecha calendario ISO-8601 a DD/MM/YYYY sin desplazarse en UTC-3', () => {
    expect(formatIsoCalendarDate('2026-11-25')).toBe('25/11/2026');
  });
});
