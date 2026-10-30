import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';

import Backend from 'i18next-http-backend';
import LanguageDetector from 'i18next-browser-languagedetector';

import { app_path } from '../config.js';

// don't want to use this?
// have a look at the Quick start guide
// for passing in lng and translations on init

// Translation files are region-specific (see /public/locales), but browsers
// report language-only codes like `de`. Map the detected code to a region file.
const SUPPORTED_LNGS = ['de-DE', 'en-US', 'es-ES', 'fr-FR', 'nl-NL', 'zh-CN', 'zh-TW'];

// One region file per language — except Chinese, where `zh` alone does not say
// whether the reader wants Simplified or Traditional. There the region or script
// decides: mainland/Singapore and `Hans` go to zh-CN, Taiwan/Hong Kong/Macau and
// `Hant` go to zh-TW; a bare `zh` falls back to Simplified.
const REGION_BY_LANG = {
  de: 'de-DE',
  en: 'en-US',
  es: 'es-ES',
  fr: 'fr-FR',
  nl: 'nl-NL',
};

const CHINESE_BY_REGION = {
  cn: 'zh-CN',
  sg: 'zh-CN',
  hans: 'zh-CN',
  tw: 'zh-TW',
  hk: 'zh-TW',
  mo: 'zh-TW',
  hant: 'zh-TW',
};

const normalize_lng = (lng) => {
  const [lang, ...regions] = lng.toLowerCase().split('-');

  if (lang === 'zh') {
    const region = regions.find((part) => CHINESE_BY_REGION[part]);
    return region ? CHINESE_BY_REGION[region] : 'zh-CN';
  }

  return REGION_BY_LANG[lang] ?? lng;
};

// Keep <html lang> in step with the rendered language, so assistive tech and the
// browser's font and line-breaking rules follow it (CJK punctuation in particular).
i18n.on('languageChanged', (lng) => {
  if (typeof document !== 'undefined') {
    document.documentElement.lang = lng;
  }
});

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
      // e.g. `de`, `de-AT` → `de-DE`, `zh-HK` → `zh-TW`; unknown codes fall
      // through to fallbackLng.
      convertDetectedLanguage: (lng) => normalize_lng(lng),
    },
    debug: true,

    backend: {
      // i18next-http-backend defaults to a root-absolute path, which misses the
      // bundle when the app is served from a sub-path. Anchor it to the
      // application root instead.
      loadPath: app_path('/locales/{{lng}}/{{ns}}.json'),
    },

    interpolation: {
      escapeValue: false, // not needed for react as it escapes by default
    }
  });


export default i18n;
