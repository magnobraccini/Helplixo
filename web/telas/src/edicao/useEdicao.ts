import { createContext, useContext } from 'react'

export type ValorEdicao = {
  ligado: boolean
  /** Chave para o cabeçalho X-Chave-Edicao das ações de edição. */
  chave: string | null
  entrar: (senha: string) => Promise<void>
  sair: () => Promise<void>
}

export const ContextoEdicao = createContext<ValorEdicao | null>(null)

/** Lê o modo edição de qualquer tela. Precisa estar dentro de <ProvedorEdicao>. */
export function useEdicao(): ValorEdicao {
  const valor = useContext(ContextoEdicao)
  if (!valor) throw new Error('useEdicao precisa estar dentro de <ProvedorEdicao>')
  return valor
}
