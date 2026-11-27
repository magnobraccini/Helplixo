import { CABECALHO_CHAVE } from './edicao'
import { pedir } from './cliente'

// Contrato de web/servidor/.../contato/ContatoController.java

export type Interno = { nome: string; setor: string; andar: string | null; ramal: string; email: string | null }
export type Externo = {
  categoria: string
  empresa: string
  servico: string
  contato: string | null
  telefone: string | null
  email: string | null
  contrato: string | null
  /** 2026-12-15 */
  vence: string
}
export type SituacaoContrato = 'emdia' | 'vence' | 'vencido'

export type ListaInternos = { contatos: Interno[]; total: number; deTotal: number; opcoes: { secretarias: string[]; andares: string[] } }
export type ExternoComSituacao = { contato: Externo; situacao: SituacaoContrato; texto: string; dias: number }
export type ListaExternos = { contatos: ExternoComSituacao[]; total: number; deTotal: number; categorias: string[] }
export type InternoSalvo = { contato: Interno; compartilhadoCom: string[] }

export type FiltroInternos = { secretaria: string; andar: string; ordem: 'nome' | 'setor' | 'ramal'; q: string }
export type FiltroExternos = { categoria: string; situacao: '' | SituacaoContrato; q: string }

/** Só manda os filtros preenchidos. */
const consulta = (f: Record<string, string>) => new URLSearchParams(Object.entries(f).filter(([, v]) => v !== '')).toString()

export const listarInternos = (f: FiltroInternos) => pedir<ListaInternos>(`/api/contatos/internos?${consulta(f)}`)
export const listarExternos = (f: FiltroExternos) => pedir<ListaExternos>(`/api/contatos/externos?${consulta(f)}`)

const comChave = (corpo: unknown, chave: string | null): RequestInit => ({
  method: 'POST',
  headers: { 'Content-Type': 'application/json', ...(chave ? { [CABECALHO_CHAVE]: chave } : {}) },
  body: JSON.stringify(corpo),
})
export const adicionarInterno = (c: Interno, chave: string | null) => pedir<InternoSalvo>('/api/contatos/internos', comChave(c, chave))
export const adicionarExterno = (c: Externo, chave: string | null) => pedir<Externo>('/api/contatos/externos', comChave(c, chave))
