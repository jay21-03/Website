import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from "react";

export type Lang = "vi" | "en";

const STORAGE_KEY = "dangxem-lang";

type I18nValue = {
  lang: Lang;
  setLang: (l: Lang) => void;
  toggle: () => void;
  t: (vi: string, en: string) => string;
};

const I18nContext = createContext<I18nValue | null>(null);

export function I18nProvider({ children }: { children: ReactNode }) {
  const [lang, setLangState] = useState<Lang>("vi");

  useEffect(() => {
    const stored = window.localStorage.getItem(STORAGE_KEY);
    if (stored === "en" || stored === "vi") setLangState(stored);
  }, []);

  const setLang = useCallback((l: Lang) => {
    setLangState(l);
    window.localStorage.setItem(STORAGE_KEY, l);
    document.documentElement.lang = l;
  }, []);

  const value = useMemo<I18nValue>(
    () => ({
      lang,
      setLang,
      toggle: () => setLang(lang === "vi" ? "en" : "vi"),
      t: (vi: string, en: string) => (lang === "vi" ? vi : en),
    }),
    [lang, setLang],
  );

  return <I18nContext.Provider value={value}>{children}</I18nContext.Provider>;
}

export function useI18n() {
  const ctx = useContext(I18nContext);
  if (!ctx) throw new Error("useI18n must be used inside I18nProvider");
  return ctx;
}
