import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';

import Backend from 'i18next-http-backend';
import LanguageDetector from 'i18next-browser-languagedetector';

// don't want to use this?
// have a look at the Quick start guide
// for passing in lng and translations on init

// Translation files are region-specific (see /public/locales), but browsers
// report language-only codes like `de`. Map the detected code to a region file.
const SUPPORTED_LNGS = ['de-DE', 'en-US', 'es-ES', 'fr-FR', 'nl-NL'];
const REGION_BY_LANG = Object.fromEntries(
  SUPPORTED_LNGS.map((lng) => [lng.split('-')[0], lng])
);

i18n
  // load translation using http -> see /public/locales (i.e. https://github.com/i18next/react-i18next/tree/master/example/react/public/locales)
  // learn more: https://github.com/i18next/i18next-http-backend
  // want your translations to be loaded from a professional CDN? => https://github.com/locize/react-tutorial#step-2---use-the-locize-cdn
  .use(Backend)
  // detect user language
  // learn more: https://github.com/i18next/i18next-browser-languageDetector
  .use(LanguageDetector)
  // pass the i18n instance to react-i18next.
  .use(initReactI18next)
  // init i18next
  // for all options read: https://www.i18next.com/overview/configuration-options
  .init({
    fallbackLng: 'en-US',
    supportedLngs: SUPPORTED_LNGS,
    // Only load the resolved language (+ fallback); never the language-only
    // `/locales/de/…` or `/dev/…`, which 404'd / failed JSON parse on startup.
    load: 'currentOnly',
    detection: {
      // e.g. `de`, `de-AT` → `de-DE`; unknown codes fall through to fallbackLng.
      convertDetectedLanguage: (lng) =>
        REGION_BY_LANG[lng.split('-')[0].toLowerCase()] ?? lng,
    },
    debug: true,

    interpolation: {
      escapeValue: false, // not needed for react as it escapes by default
    }
  });


export default i18n;
