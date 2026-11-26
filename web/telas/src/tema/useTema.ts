import { useEffect, useState } from 'react'

// Regra de negócio: Claro, Escuro ou Igual ao sistema. A escolha fica salva no computador de cada um.
// Mesma chave do localStorage usada pela maquete.

export type PreferenciaTema = 'claro' | 'escuro' | 'sistema'
export type TemaEfetivo = 'claro' | 'escuro'

export const CHAVE_TEMA = 'helplixo-tema'
const CONSULTA_ESCURO = '(prefers-color-scheme: dark)'

function lerPreferencia(): PreferenciaTema {
  try {
    const salvo = window.localStorage.getItem(CHAVE_TEMA)
    return salvo === 'escuro' || salvo === 'sistema' ? salvo : 'claro'
  } catch {
    return 'claro'
  }
}

function sistemaEstaEscuro(): boolean {
  return typeof window.matchMedia === 'function' && window.matchMedia(CONSULTA_ESCURO).matches
}

export function useTema() {
  const [preferencia, setPreferencia] = useState<PreferenciaTema>(lerPreferencia)
  const [sistemaEscuro, setSistemaEscuro] = useState(sistemaEstaEscuro)

  // Se o Windows trocar de claro para escuro com a tela aberta, acompanha na hora.
  useEffect(() => {
    if (typeof window.matchMedia !== 'function') return
    const consulta = window.matchMedia(CONSULTA_ESCURO)
    const aoMudar = () => setSistemaEscuro(consulta.matches)
    consulta.addEventListener('change', aoMudar)
    return () => consulta.removeEventListener('change', aoMudar)
  }, [])

  const efetivo: TemaEfetivo = preferencia === 'sistema' ? (sistemaEscuro ? 'escuro' : 'claro') : preferencia

  // O atributo fica no <html>, então os tokens de cor valem para a página inteira, inclusive diálogos.
  useEffect(() => {
    document.documentElement.dataset.tema = efetivo
  }, [efetivo])

  const escolher = (nova: PreferenciaTema) => {
    setPreferencia(nova)
    try {
      window.localStorage.setItem(CHAVE_TEMA, nova)
    } catch {
      // navegador sem localStorage: a escolha vale só até fechar a aba
    }
  }

  return { preferencia, efetivo, escolher }
}
