# Reading the release calendar

The upcoming releases live in the **Operaton Release Calendar** (Google Calendar). Events are all-day, titled `Operaton X.Y.Z` or `Operaton X.Y.Z-Mx`.

Calendar ID:
```
2492fa418b8818bcb8137cf3e0ea3dcf8e13faa03ef7fecc5b0307b732dd5eca@group.calendar.google.com
```

## Primary: Google Calendar MCP

Use the connected Google Calendar MCP (`list_events`) with that `calendarId`, `startTime` = today, `endTime` = today + ~14 days. Parse each `summary` of the form `Operaton <version>` into a target version.

```
list_events(
  calendarId="2492fa418b8818bcb8137cf3e0ea3dcf8e13faa03ef7fecc5b0307b732dd5eca@group.calendar.google.com",
  startTime="<today>T00:00:00Z",
  endTime="<today+14d>T00:00:00Z",
  orderBy="startTime")
```

The `start.date` of each event is the planned release date.

## Fallback: public ICS

If the MCP is unavailable (e.g. headless/cron run), fetch the public ICS feed and grep `SUMMARY:` lines:
```
https://calendar.google.com/calendar/ical/2492fa418b8818bcb8137cf3e0ea3dcf8e13faa03ef7fecc5b0307b732dd5eca%40group.calendar.google.com/public/basic.ics
```

## Mapping summary → action

| Summary | Type | Branch | Qualifier |
|---------|------|--------|-----------|
| `Operaton 1.1.4` | patch | `release/1.1.x` | — |
| `Operaton 2.1.2` | patch | `release/2.1.x` | — |
| `Operaton 2.2.0-M1` | milestone | `main` (or `release/2.2.x` if it exists) | `M1` |
| `Operaton 2.2.0` | minor | `release/2.2.x` if exists, else `main` | — |
