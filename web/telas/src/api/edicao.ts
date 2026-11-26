import { pedir } from './cliente'

// Contrato de web/servidor/.../edicao/EdicaoController.java

export const CABECALHO_CHAVE = 'X-Chave-Edicao'

export type Entrada = { chave: string; validaAte: string }
export type Situacao = { configurado: boolean; ligado: boolean; validaAte: string | null }

export function entrarNaEdicao(senha: string): Promise<Entrada> {
  return pedir<Entrada>('/api/edicao/entrar', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ senha }),
  })
}

export function sairDaEdicao(chave: string): Promise<void> {
  return pedir<void>('/api/edicao/sair', { method: 'POST', headers: { [CABECALHO_CHAVE]: chave } })
}

export function situacaoDaEdicao(chave: string): Promise<Situacao> {
  return pedir<Situacao>('/api/edicao', { headers: { [CABECALHO_CHAVE]: chave } })
}
