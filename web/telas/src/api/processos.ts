import { pedir } from './cliente'

// Contrato de web/servidor/.../processo/ProcessoController.java e Processo.java (só leitura).

export type ItemProcesso = { id: string; titulo: string; atualizadoEm: string | null }
export type NoPasta = { id: string; nome: string; pastas: NoPasta[]; processos: ItemProcesso[]; total: number }
export type Arvore = { pastas: NoPasta[]; semPasta: ItemProcesso[] }

export type TipoBloco = 'PARAGRAPH' | 'HEADING' | 'BULLET_LIST' | 'NUMBERED_LIST' | 'CODE' | 'LINK' | 'IMAGE' | 'INFO' | 'WARNING' | 'NOTE'
export type Bloco = { id: string | null; tipo: TipoBloco; texto: string | null; url: string | null; negrito: boolean; italico: boolean; sublinhado: boolean }
export type Passo = { id: string | null; numero: number; titulo: string | null; descricao: string | null; resultadoEsperado: string | null; checklist: string[] | null }

export type Processo = {
  id: string
  pasta: string | null
  titulo: string
  descricao: string | null
  objetivo: string | null
  preRequisitos: string | null
  responsavel: string | null
  categoria: string | null
  tags: string[]
  passos: Passo[]
  conteudo: Bloco[]
  observacoes: string | null
  links: string[]
  criadoEm: string | null
  atualizadoEm: string | null
}

export type DetalheProcesso = { processo: Processo; caminho: string[] }
export type ItemBusca = { id: string; titulo: string; caminho: string }

export const arvoreProcessos = () => pedir<Arvore>('/api/processos/arvore')
export const buscarProcesso = (id: string) => pedir<DetalheProcesso>(`/api/processos/${encodeURIComponent(id)}`)
export const procurarProcessos = (q: string) => pedir<ItemBusca[]>(`/api/processos?${new URLSearchParams({ q })}`)
