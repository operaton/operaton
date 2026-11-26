/**
 * languages.js
 *
 * The languages the UI offers, in one place: the login card, the account
 * settings page and the header switcher all read this list. Only fully
 * translated locales are offered — es-ES, fr-FR and nl-NL exist under
 * public/locales but are ~75% complete, so offering them means a quarter of the
 * UI silently falls back to English. They come back here once complete.
 *
 * `short` is what fits in the header button: two letters for a latin language,
 * one character for Chinese — `ZH` cannot tell Simplified from Traditional.
 */

export const LANGUAGES = [
  { code: "en-US", label: "English", short: "EN" },
  { code: "de-DE", label: "Deutsch", short: "DE" },
  { code: "zh-CN", label: "简体中文", short: "简" },
  { code: "zh-TW", label: "繁體中文", short: "繁" },
];

/**
 * The entry for a resolved i18next language. An unknown tag (a stored `fr-FR`,
 * or `navigator.language` before detection settles) is shown as itself rather
 * than passed off as English, so the button never claims the wrong language.
 */
export const language_for = (code) => {
  const known = LANGUAGES.find((lng) => lng.code === code);
  if (known) return known;

  const tag = code ?? LANGUAGES[0].code;
  return { code: tag, label: tag, short: tag.split("-")[0].toUpperCase() };
};
