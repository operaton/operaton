import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { formatRelativeDate, formatRelativeDateTime } from './date_formatter.js';

describe('date_formatter', () => {
  let originalNavigator;

  beforeEach(() => {
    // Save original navigator
    originalNavigator = global.navigator;
  });

  afterEach(() => {
    // Restore original navigator
    global.navigator = originalNavigator;
    vi.useRealTimers();
  });

  describe('formatRelativeDate', () => {
    it('should format today as "today"', () => {
      const today = new Date();
      const result = formatRelativeDate(today);
      expect(result).toBe('today');
    });

    it('should format yesterday as "yesterday"', () => {
      const yesterday = new Date();
      yesterday.setDate(yesterday.getDate() - 1);
      const result = formatRelativeDate(yesterday);
      expect(result).toBe('yesterday');
    });

    it('should format tomorrow as "tomorrow"', () => {
      const tomorrow = new Date();
      tomorrow.setDate(tomorrow.getDate() + 1);
      const result = formatRelativeDate(tomorrow);
      expect(result).toBe('tomorrow');
    });

    it('should format past dates in days', () => {
      const threeDaysAgo = new Date();
      threeDaysAgo.setDate(threeDaysAgo.getDate() - 3);
      const result = formatRelativeDate(threeDaysAgo);
      expect(result).toBe('3 days ago');
    });

    it('should format future dates in days', () => {
      const inFiveDays = new Date();
      inFiveDays.setDate(inFiveDays.getDate() + 5);
      const result = formatRelativeDate(inFiveDays);
      expect(result).toBe('in 5 days');
    });

    it('should format dates more than a week ago in weeks', () => {
      const twoWeeksAgo = new Date();
      twoWeeksAgo.setDate(twoWeeksAgo.getDate() - 14);
      const result = formatRelativeDate(twoWeeksAgo);
      expect(result).toContain('week');
    });
  });

  describe('formatRelativeDateTime', () => {
    it('should format times a few seconds ago', () => {
      const fiveSecondsAgo = new Date();
      fiveSecondsAgo.setSeconds(fiveSecondsAgo.getSeconds() - 5);
      const result = formatRelativeDateTime(fiveSecondsAgo, false);
      expect(result).toBe('5 seconds ago');
    });

    it('should format times in minutes', () => {
      const tenMinutesAgo = new Date();
      tenMinutesAgo.setMinutes(tenMinutesAgo.getMinutes() - 10);
      const result = formatRelativeDateTime(tenMinutesAgo, false);
      expect(result).toBe('10 minutes ago');
    });

    it('should format times in hours', () => {
      const twoHoursAgo = new Date();
      twoHoursAgo.setHours(twoHoursAgo.getHours() - 2);
      const result = formatRelativeDateTime(twoHoursAgo, false);
      expect(result).toBe('2 hours ago');
    });

    it('should ignore time when ignoreTime is true', () => {
      const todayWithDifferentTime = new Date();
      todayWithDifferentTime.setHours(todayWithDifferentTime.getHours() - 2);
      const result = formatRelativeDateTime(todayWithDifferentTime, true);
      expect(result).toBe('today');
    });
  });

  describe('locale support', () => {
    it('should use browser locale from navigator.language', () => {
      // Mock navigator.language to German
      global.navigator = {
        language: 'de-DE',
        languages: ['de-DE', 'en-US'],
      };

      const yesterday = new Date();
      yesterday.setDate(yesterday.getDate() - 1);
      const result = formatRelativeDate(yesterday);

      // German locale should return "gestern" for yesterday
      expect(result).toBe('gestern');
    });

    it('should fallback to navigator.languages[0] if language is not set', () => {
      global.navigator = {
        language: undefined,
        languages: ['fr-FR', 'en-US'],
      };

      const yesterday = new Date();
      yesterday.setDate(yesterday.getDate() - 1);
      const result = formatRelativeDate(yesterday);

      // French locale should return "hier" for yesterday
      expect(result).toBe('hier');
    });

    it('should fallback to "en" if no locale is available', () => {
      global.navigator = {
        language: undefined,
        languages: undefined,
      };

      const yesterday = new Date();
      yesterday.setDate(yesterday.getDate() - 1);
      const result = formatRelativeDate(yesterday);

      // English fallback should return "yesterday"
      expect(result).toBe('yesterday');
    });
  });
});
