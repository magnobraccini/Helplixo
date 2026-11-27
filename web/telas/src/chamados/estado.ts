// Estados da tela de Chamados que não são componentes (filtro da lista e formulário de Resultado).
import type { Chamado, StatusChamado } from '../api/chamados'
import { isoData, isoHora } from '../util/datas'
import type { Periodo } from './regras'

export type FiltroStatus = StatusChamado | 'Todos'

export type Filtro = {
  status: FiltroStatus
  periodo: Periodo
  de: string
  ate: string
  secretaria: string
  /** '' = todos, '—' = sem técnico */
  tecnico: string
  busca: string
}

/** A lista abre nos Abertos dos últimos 30 dias. */
export const FILTRO_INICIAL: Filtro = { status: 'Aberto', periodo: '30', de: '', ate: '', secretaria: '', tecnico: '', busca: '' }

export type DadosFormulario = { status: StatusChamado; anotacao: string; data: string; hora: string; tecnico: string }

/** Como o formulário começa (maquete: ffPara): Em andamento, agora, e o técnico do chamado. */
export function formularioNovo(c: Chamado, agora: number): DadosFormulario {
  const d = new Date(agora)
  return { status: 'Em andamento', anotacao: '', data: isoData(d), hora: isoHora(d), tecnico: c.tecnico ?? '' }
}
