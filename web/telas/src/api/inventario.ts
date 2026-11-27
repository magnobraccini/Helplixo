import { CABECALHO_CHAVE } from './edicao'
import { pedir } from './cliente'

// Contrato de web/servidor/.../inventario/InventarioController.java, InventarioServico.java e Maquina.java

export const CABECALHO_USUARIO = 'X-Usuario'

export type Compra = {
  fornecedor: string | null
  notaFiscal: string | null
  /** 2023-03-14 */
  data: string | null
  valor: string | null
  processo: string | null
  garantiaAte: string | null
}
export type Software = { nome: string; licenca: string | null; tipo: string }
export type SistemaPmsl = { nome: string; linguagem: string }
export type Conserto = { data: string; defeito: string; solucao: string; onde: string; custo: string | null }
export type Baixa = { motivo: string; observacao: string | null; dia: string; quem: string }

export type Maquina = {
  nSemin: string
  patrimonio: string
  tipo: string
  modelo: string
  serie: string | null
  processador: string | null
  memoria: string | null
  armazenamento: string | null
  sistema: string | null
  ip: string | null
  mac: string | null
  local: string
  usuario: string | null
  situacao: string
  observacao: string | null
  compra: Compra | null
  softwares: Software[]
  sistemasPmsl: SistemaPmsl[]
  consertos: Conserto[]
  baixa: Baixa | null
}

/** situacao: sim, sim, sem licença, não, não informado ou não se aplica. */
export type Office = { situacao: string; detalhe: string }
/** situacao: vigente, vence, vencida ou sem informação. */
export type Garantia = { situacao: string; texto: string }

export type ItemHistorico = {
  quando: string
  /** "Conserto", "Chamado", "Movimentação", "Cadastro", "Edição", "Baixa"… */
  tipo: string
  texto: string
  /** Nº do chamado ligado, quando houver. */
  referencia: string | null
  quem: string | null
}

export type FichaMaquina = { maquina: Maquina; office: Office; garantia: Garantia; historico: ItemHistorico[] }

export type ItemMaquina = { nSemin: string; patrimonio: string; tipo: string; modelo: string; local: string; usuario: string | null; situacao: string; office: string }
export type Busca = { maquinas: ItemMaquina[]; total: number; temMais: boolean }
export type Resumo = { total: number; porTipo: Record<string, number> }

/** O que a tela manda ao adicionar ou editar. */
export type DadosMaquina = Omit<Maquina, 'situacao' | 'softwares' | 'sistemasPmsl' | 'consertos' | 'baixa'>

export type PedidoMovimentacao = { destino: string; motivo: string; quando: string; tecnico: string; chamado: string }
export type PedidoBaixa = { motivo: string; observacao: string }
export type ResultadoEdicao = { maquina: Maquina; mudancas: string[] }

/** Quem faz a alteração (até existir o login) e a chave do modo edição. */
export type Autoria = { chave: string | null; usuario: string }

const json = (corpo: unknown, a?: Autoria): RequestInit => ({
  headers: {
    'Content-Type': 'application/json',
    ...(a?.chave ? { [CABECALHO_CHAVE]: a.chave } : {}),
    ...(a ? { [CABECALHO_USUARIO]: a.usuario } : {}),
  },
  body: JSON.stringify(corpo),
})
const base = (n: string) => `/api/inventario/${encodeURIComponent(n)}`

export const resumoInventario = () => pedir<Resumo>('/api/inventario/resumo')
export const buscarMaquinas = (q: string) => pedir<Busca>(`/api/inventario?${new URLSearchParams({ q })}`)
export const buscarFichaMaquina = (nSemin: string) => pedir<FichaMaquina>(base(nSemin))

export const adicionarMaquina = (d: DadosMaquina, a: Autoria) => pedir<Maquina>('/api/inventario', { method: 'POST', ...json(d, a) })
export const editarMaquina = (nSemin: string, d: DadosMaquina, a: Autoria) => pedir<ResultadoEdicao>(base(nSemin), { method: 'PUT', ...json(d, a) })
export const movimentarMaquina = (nSemin: string, p: PedidoMovimentacao) => pedir<Maquina>(`${base(nSemin)}/movimentacoes`, { method: 'POST', ...json(p) })
export const darBaixa = (nSemin: string, p: PedidoBaixa, a: Autoria) => pedir<Maquina>(`${base(nSemin)}/baixa`, { method: 'POST', ...json(p, a) })
export const desfazerBaixa = (nSemin: string, a: Autoria) =>
  pedir<Maquina>(`${base(nSemin)}/baixa`, {
    method: 'DELETE',
    headers: { ...(a.chave ? { [CABECALHO_CHAVE]: a.chave } : {}), [CABECALHO_USUARIO]: a.usuario },
  })
