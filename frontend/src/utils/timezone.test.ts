import { describe, expect, it } from 'vitest';
import {
  formatInstantInZone,
  formatSlotRangeInZone,
  getConsultorioTimeZone,
  localDateTimeToInstantIso,
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
});
