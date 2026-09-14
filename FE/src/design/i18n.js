import { createContext, useContext } from 'react'

/** @type {import('react').Context<{lang: string, setLang: (lang: string) => void, t: (vi: string, en: string) => string, count: number}>} */
export const DesignContext = createContext({ lang: 'vi', setLang: () => {}, t: (vi, _en) => vi, count: 0 })
export const useI18n = () => useContext(DesignContext)
