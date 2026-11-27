// Regras da tela de Chamados, copiadas da maquete e de web/docs/regras-de-negocio.md.
import type { Chamado, StatusChamado } from '../api/chamados'
import { DIA, MINUTO, duracao, fmt, inicioDoDia, instante, isoData, noExpediente } from '../util/datas'

export type Cores = { fundo: string; texto: string }

export const CORES_STATUS: Record<StatusChamado, Cores> = {
  'Aberto': { fundo: 'var(--c-fdebec)', texto: 'var(--c-8a2a27)' },
  'Em andamento': { fundo: 'var(--c-fbf3db)', texto: 'var(--c-6b4f0e)' },
  'Fechado': { fundo: 'var(--c-edf3ec)', texto: 'var(--c-2f5a2b)' },
}

const CORES_URGENCIA: Record<string, Cores> = {
  'Baixa': { fundo: 'var(--c-edf3ec)', texto: 'var(--c-2f5a2b)' },
  'Normal': { fundo: 'var(--c-eef1f5)', texto: 'var(--c-3f4757)' },
  'Alta': { fundo: 'var(--c-fbf3db)', texto: 'var(--c-6b4f0e)' },
  'Urgente': { fundo: 'var(--c-fdebec)', texto: 'var(--c-8a2a27)' },
}
export const coresUrgencia = (u: string): Cores => CORES_URGENCIA[u] ?? CORES_URGENCIA.Normal

/**
 * Coluna "Min.": minutos de espera (abertos e em andamento) ou de atendimento (fechados).
 * Cor: Aberto < 2 h verde, 2–3 h âmbar, > 3 h vermelho; Em andamento azul; Fechado ou não computável cinza.
 */
export function minutosDoChamado(c: Chamado, agora: number): { minutos: number; cor: string; rotulo: string } {
  const fechado = c.status === 'Fechado'
  const fim = fechado && c.fechamento ? instante(c.fechamento.em) : agora
  const minutos = Math.max(0, Math.round((fim - instante(c.abertoEm)) / MINUTO))
  if (fechado || c.naoComputavel) return { minutos, cor: 'var(--c-677085)', rotulo: fechado ? 'tempo de atendimento' : 'não computável' }
  if (c.status === 'Em andamento') return { minutos, cor: 'var(--c-337ea9)', rotulo: 'em andamento' }
  if (minutos < 120) return { minutos, cor: 'var(--c-2f7a3a)', rotulo: 'no prazo' }
  if (minutos < 180) return { minutos, cor: 'var(--c-b7791f)', rotulo: 'atenção' }
  return { minutos, cor: 'var(--c-c4302b)', rotulo: 'atrasado' }
}

/** "Controle Interno - GABINETE" → "GABINETE" */
export const secretariaDe = (setor: string) => setor.split(' - ')[1] ?? setor

export type Periodo = 'hoje' | '7' | '30' | '90' | 'custom'

export const PERIODOS: { valor: Periodo; rotulo: string }[] = [
  { valor: 'hoje', rotulo: 'Hoje' },
  { valor: '7', rotulo: 'Últimos 7 dias' },
  { valor: '30', rotulo: 'Últimos 30 dias' },
  { valor: '90', rotulo: 'Últimos 90 dias' },
  { valor: 'custom', rotulo: 'Escolher datas…' },
]

/** As datas que vão para o servidor (de/ate) e o texto do resumo, como na maquete. */
export function intervaloDoPeriodo(p: Periodo, de: string, ate: string, agora: number): { de: string; ate: string; texto: string } | null {
  const hoje = isoData(new Date(agora))
  if (p === 'hoje') return { de: hoje, ate: hoje, texto: 'hoje' }
  if (p === 'custom') {
    if (de && ate && de > ate) return null
    return {
      de: de || '2000-01-01',
      ate: ate || hoje,
      texto: (de ? `de ${br(de)}` : 'desde o início') + (ate ? ` até ${br(ate)}` : ' até hoje'),
    }
  }
  const n = Number(p)
  return { de: isoData(new Date(inicioDoDia(agora) - (n - 1) * DIA)), ate: hoje, texto: `nos últimos ${n} dias` }
}
const br = (iso: string) => iso.split('-').reverse().join('/')

/** ↺: quantos OUTROS chamados a mesma máquina teve nos últimos 7 dias (contando a partir de agora). */
export function reincidencias(c: Chamado, recentes: Chamado[], agora: number): number {
  if (!c.nSemin) return 0
  const desde = agora - 7 * DIA
  return recentes.filter((o) => o.nSemin === c.nSemin && o.numero !== c.numero && instante(o.abertoEm) >= desde).length
}

export type Conferencia = { tipo: 'ok' | 'erro' | 'aviso' | 'info'; texto: string }

/** A caixa "Conferência do horário" do formulário de Resultado (maquete: conferir). */
export function conferirHorario(ms: number | null, abertoEm: number, agora: number): Conferencia[] {
  if (ms === null) return [{ tipo: 'erro', texto: 'Informe a data e a hora.' }]
  const saida: Conferencia[] = []
  if (ms > agora + 5 * MINUTO) saida.push({ tipo: 'erro', texto: `Está no futuro. O horário não pode ser depois de agora (${fmt(agora)}).` })
  else saida.push({ tipo: 'ok', texto: 'Não está no futuro.' })
  const inicio = Math.floor(abertoEm / MINUTO) * MINUTO
  if (ms < inicio) saida.push({ tipo: 'erro', texto: `É antes da abertura do chamado (${fmt(abertoEm)}).` })
  else saida.push({ tipo: 'ok', texto: `Depois da abertura do chamado (${fmt(abertoEm)}).` })
  if (!noExpediente(ms)) saida.push({ tipo: 'aviso', texto: 'Fora do expediente (segunda a sexta, 8h às 14h). É só um aviso: confira se está certo.' })
  if (ms >= inicio) saida.push({ tipo: 'info', texto: `Tempo de atendimento: ${duracao(ms - abertoEm)}.` })
  return saida
}
