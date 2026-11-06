import { isRtl, resolveLanguage, t, type Language, type Settings } from '@salat/core';

/** Language for this page, from settings or Chrome's UI language. */
export function pageLanguage(settings: Settings): Language {
  return resolveLanguage(settings.language, chrome.i18n?.getUILanguage?.() ?? navigator.language);
}

/**
 * Apply translations to the static DOM: elements with data-i18n="key" get their
 * text replaced; data-i18n-placeholder sets the placeholder attribute.
 * Also sets lang/dir on <html> so Arabic lays out right-to-left.
 */
export function localiseDocument(lang: Language): void {
  document.documentElement.lang = lang;
  document.documentElement.dir = isRtl(lang) ? 'rtl' : 'ltr';
  document.querySelectorAll<HTMLElement>('[data-i18n]').forEach((el) => {
    el.textContent = t(lang, el.dataset.i18n!);
  });
  document.querySelectorAll<HTMLElement>('[data-i18n-placeholder]').forEach((el) => {
    el.setAttribute('placeholder', t(lang, el.dataset.i18nPlaceholder!));
  });
  document.title = t(lang, 'app.name');
}
