import { pedir } from './cliente'

// Contrato de web/servidor/.../chamado/ChamadoController.java e Chamado.java

export type StatusChamado = 'Aberto' | 'Em andamento' | 'Fechado'
export const STATUS: StatusChamado[] = ['Aberto', 'Em andamento', 'Fechado']

export type Anotacao = { texto: string; em: string; tecnico: string }
export type Fechamento = { descricao: string; em: string; tecnico: string }

export type Chamado = {
  numero: string
  /** Vazio = sem máquina. */
  nSemin: string | null
  setor: string
  contato: string
  email: string | null
  problema: string
  descricao: string
  observacao: string | null
  tecnico: string | null
  area: string
  status: StatusChamado
  urgencia: string
  ocorrencia: string
  naoComputavel: boolean
  abertoEm: string
  andamento: Anotacao[]
  fechamento: Fechamento | null
}

export type PedidoResultado = {
  status: StatusChamado
  anotacao: string
  /** 2026-11-26T10:30 */
  quando: string
  tecnico: string
}

export type Resultado = { chamado: Chamado; avisos: string[] }

/** Chamados abertos entre `de` e `ate` (datas 2026-11-25, o dia `ate` inteiro). */
export function listarChamados(de: string, ate: string): Promise<Chamado[]> {
  const q = new URLSearchParams({ de, ate })
  return pedir<Chamado[]>(`/api/chamados?${q}`)
}

export function buscarChamado(numero: string): Promise<Chamado> {
  return pedir<Chamado>(`/api/chamados/${encodeURIComponent(numero)}`)
}

/** O formulário único da aba Resultado. Erro de regra: ErroDaApi 422 com a lista de erros. */
export function registrarResultado(numero: string, pedido: PedidoResultado): Promise<Resultado> {
  return pedir<Resultado>(`/api/chamados/${encodeURIComponent(numero)}/resultado`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(pedido),
  })
}
