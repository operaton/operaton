// returns date relative formatted, e.g. yesterday, 2 weeks ago, 3 month ago, tomorrow, in 5 days, ...
const formatRelativeDate = (date) => {
  return formatRelativeDateTime(date, true);
};

// returns date/time relative formatted, e.g. yesterday, 2 hours ago, 3 month ago, tomorrow, in 5 minutes, ...
const formatRelativeDateTime = (date, ignoreTime) => {
  const locale = navigator.language || navigator.languages?.[0] || "en";
  const timeFormatter = new Intl.RelativeTimeFormat(locale, {
    numeric: "auto",
  });
  let relativeDate = new Date(date);
  let nowDate = new Date();

  // if we don't care for the time, we remove the time part here
  if (ignoreTime) {
    relativeDate = new Date(relativeDate.toDateString());
    nowDate = new Date(nowDate.toDateString());
  }

  // Calculate the difference in seconds between the given date and the current date
  const secondsDiff = Math.round((relativeDate - nowDate) / 1000);

  // if we ignore the time: for today the difference should be 0, so we can skip here
  if (ignoreTime && secondsDiff === 0) {
    return timeFormatter.format(0, "day");
  }

  // Array representing one minute, hour, day, week, month, etc. in seconds
  const unitsInSec = [
    60,
    3600,
    86400,
    86400 * 7,
    86400 * 30,
    86400 * 365,
    Infinity,
  ];
  // Array equivalent to the above but in the string representation of the units
  const unitStrings = [
    "second",
    "minute",
    "hour",
    "day",
    "week",
    "month",
    "year",
  ];
  // Find the appropriate unit based on the seconds difference
  const unitIndex = unitsInSec.findIndex(
    (cutoff) => cutoff > Math.abs(secondsDiff),
  );
  // Get the divisor to convert seconds to the appropriate unit
  const divisor = unitIndex ? unitsInSec[unitIndex - 1] : 1;

  return timeFormatter.format(
    Math.floor(secondsDiff / divisor),
    unitStrings[unitIndex],
  );
};

// returns a humanized duration from milliseconds, e.g. "3d 4h", "12m 5s", "800ms".
// Shows at most the two largest non-zero units so it stays glanceable.
const formatDuration = (ms) => {
  if (ms == null || Number.isNaN(ms)) return "";
  if (ms < 1000) return `${ms}ms`;
  const s = Math.floor(ms / 1000) % 60;
  const m = Math.floor(ms / 60000) % 60;
  const h = Math.floor(ms / 3600000) % 24;
  const d = Math.floor(ms / 86400000);
  const parts = [];
  if (d) parts.push(`${d}d`);
  if (h) parts.push(`${h}h`);
  if (m) parts.push(`${m}m`);
  if (s) parts.push(`${s}s`);
  return parts.slice(0, 2).join(" ");
};

const pad = (n) => String(n).padStart(2, "0");

// The engine wants `yyyy-MM-dd'T'HH:mm:ss.SSSZ`, where Z is an offset like +0200.
const localOffset = (date) => {
  const minutes = -date.getTimezoneOffset(),
    sign = minutes < 0 ? "-" : "+";
  return (
    sign + pad(Math.floor(Math.abs(minutes) / 60)) + pad(Math.abs(minutes) % 60)
  );
};

// A <input type="date"> and <input type="time"> hold local wall-clock time, so
// that is what goes in and comes out — not UTC.
const toLocalParts = (date) => ({
  date: `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`,
  time: `${pad(date.getHours())}:${pad(date.getMinutes())}`,
});

const fromLocalParts = (date, time) =>
  `${date}T${time}:00.000${localOffset(new Date(`${date}T${time}`))}`;

// A moment in a log, written out in full: `dd.MM.yyyy - HH:mm:ss`. Such a
// table is read for the exact order of events, and a relative wording
// ("today") answers how long ago, never when.
const formatTimestamp = (date) => {
  const at = date instanceof Date ? date : new Date(Date.parse(date));
  return Number.isNaN(at.getTime())
    ? ""
    : `${pad(at.getDate())}.${pad(at.getMonth() + 1)}.${at.getFullYear()}` +
        ` - ${pad(at.getHours())}:${pad(at.getMinutes())}:${pad(at.getSeconds())}`;
};

export {
  formatTimestamp,
  toLocalParts,
  fromLocalParts,
  formatRelativeDate,
  formatRelativeDateTime,
  formatDuration,
};
