// Contas do painel do Início, copiadas da maquete (kpis, atencao, meus, hoje/amanhã).
import type { Ocorrencia } from '../api/calendario'
import { corDoTipo } from '../api/calendario'
import type { Chamado } from '../api/chamados'
import { DIA, MINUTO, duracao, inicioDoDia, instante, isoHora, pad } from '../util/datas'

export type LinhaChamado = {
  numero: string
  titulo: string
  onde: string
  tecnico: string
  espera: string
  cor: string
  estado: string
}

export type Kpi = { rotulo: string; valor: string; detalhe: string; acao: AcaoKpi }
export type AcaoKpi = { tipo: 'lista'; status: 'Aberto' | 'Em andamento' | 'Fechado' } | { tipo: 'chamado'; numero: string }

const corEspera = (min: number) => (min < 120 ? 'var(--c-2f7a3a)' : min < 180 ? 'var(--c-b7791f)' : 'var(--c-c4302b)')
const rotuloEspera = (min: number) => (min < 120 ? 'no prazo' : min < 180 ? 'atenção' : 'atrasado')

export function linhaDoChamado(c: Chamado, agora: number): LinhaChamado {
  const desde = instante(c.abertoEm)
  const min = Math.round((agora - desde) / MINUTO)
  const andando = c.status === 'Em andamento'
  return {
    numero: c.numero,
    titulo: c.problema,
    onde: c.setor.split(' - ')[0] + (c.nSemin ? ` · ${c.nSemin}` : ''),
    tecnico: c.tecnico || 'sem técnico',
    espera: duracao(agora - desde),
    cor: andando ? 'var(--c-337ea9)' : corEspera(min),
    estado: andando ? 'em andamento' : rotuloEspera(min),
  }
}

/**
 * Os 4 cards. `emAberto` = Abertos + Em andamento (todos, sem limite de data);
 * `fechados` = fechados recentes (para "Fechados hoje" e a média de 30 dias).
 * Não computáveis ficam fora de "Esperando há mais tempo" e da média.
 */
export function montarKpis(emAberto: Chamado[], fechados: Chamado[], eu: string, agora: number): Kpi[] {
  const hoje0 = inicioDoDia(agora)
  const abertos = emAberto.filter((c) => c.status === 'Aberto')
  const andamento = emAberto.filter((c) => c.status === 'Em andamento')
  // Regra: "Esperando há mais tempo" conta só os ABERTOS (em andamento já está com alguém).
  const maisAntigo = abertos.filter((c) => !c.naoComputavel).sort((a, b) => instante(a.abertoEm) - instante(b.abertoEm))[0]
  const fim = (c: Chamado) => (c.fechamento ? instante(c.fechamento.em) : 0)
  const fechadosHoje = fechados.filter((c) => fim(c) >= hoje0)
  const ultimos30 = fechados.filter((c) => c.fechamento && !c.naoComputavel && fim(c) >= hoje0 - 30 * DIA)
  const media = ultimos30.length ? ultimos30.reduce((s, c) => s + (fim(c) - instante(c.abertoEm)), 0) / ultimos30.length : 0

  return [
    { rotulo: 'Abertos', valor: String(abertos.length), detalhe: `${emAberto.filter((c) => !c.tecnico).length} sem técnico`, acao: { tipo: 'lista', status: 'Aberto' } },
    { rotulo: 'Em andamento', valor: String(andamento.length), detalhe: `${emAberto.filter((c) => c.tecnico === eu).length} com você`, acao: { tipo: 'lista', status: 'Em andamento' } },
    maisAntigo
      ? { rotulo: 'Esperando há mais tempo', valor: duracao(agora - instante(maisAntigo.abertoEm)), detalhe: `Nº ${maisAntigo.numero} · ${maisAntigo.problema}`, acao: { tipo: 'chamado', numero: maisAntigo.numero } }
      : { rotulo: 'Esperando há mais tempo', valor: '—', detalhe: 'nada esperando', acao: { tipo: 'lista', status: 'Aberto' } },
    { rotulo: 'Fechados hoje', valor: String(fechadosHoje.length), detalhe: `média de 30 dias: ${media ? duracao(media) : '—'}`, acao: { tipo: 'lista', status: 'Fechado' } },
  ]
}

/** "Precisa de atenção": abertos primeiro, depois urgentes, depois os mais antigos. Até 5. Sem os não computáveis. */
export function precisaDeAtencao(emAberto: Chamado[], agora: number): LinhaChamado[] {
  return emAberto
    .filter((c) => !c.naoComputavel)
    .sort(
      (a, b) =>
        (a.status === 'Aberto' ? 0 : 1) - (b.status === 'Aberto' ? 0 : 1) ||
        Number(b.urgencia === 'Urgente') - Number(a.urgencia === 'Urgente') ||
        instante(a.abertoEm) - instante(b.abertoEm),
    )
    .slice(0, 5)
    .map((c) => linhaDoChamado(c, agora))
}

/** "Sem técnico" (Triagem) ou "Com você" (os outros perfis). Mais antigos primeiro, até 5. */
export function meusChamados(emAberto: Chamado[], perfil: string, eu: string, agora: number) {
  const triagem = perfil === 'Triagem'
  const lista = emAberto
    .filter((c) => (triagem ? !c.tecnico : c.tecnico === eu))
    .sort((a, b) => instante(a.abertoEm) - instante(b.abertoEm))
    .slice(0, 5)
    .map((c) => linhaDoChamado(c, agora))
  return {
    titulo: triagem ? 'Sem técnico' : 'Com você',
    vazio: triagem ? 'Todos os chamados abertos têm técnico.' : 'Nenhum chamado em aberto com você.',
    lista,
  }
}

export type ItemAgenda = { id: string; dia: string; hora: string; titulo: string; cor: string }

export const itemDaAgenda = (o: Ocorrencia): ItemAgenda => ({
  id: o.id,
  dia: o.dia,
  hora: o.diaInteiro || !o.horaInicio ? 'dia todo' : o.horaInicio.slice(0, 5),
  titulo: o.titulo,
  cor: corDoTipo(o.tipo),
})

/** O aviso urgente que vai para o topo do Início (Aviso ou Manutenção marcado como urgente). */
export const avisoUrgente = (ocorrencias: Ocorrencia[]) =>
  ocorrencias.find((o) => o.urgente && (o.tipo === 'Aviso' || o.tipo === 'Manutenção')) ?? null

const DIAS = ['domingo', 'segunda-feira', 'terça-feira', 'quarta-feira', 'quinta-feira', 'sexta-feira', 'sábado']
const MESES = ['janeiro', 'fevereiro', 'março', 'abril', 'maio', 'junho', 'julho', 'agosto', 'setembro', 'outubro', 'novembro', 'dezembro']

/** "Sexta-feira, 27 de novembro de 2026" */
export function dataPorExtenso(agora: number): string {
  const d = new Date(agora)
  const dia = DIAS[d.getDay()]
  return `${dia[0].toUpperCase()}${dia.slice(1)}, ${d.getDate()} de ${MESES[d.getMonth()]} de ${d.getFullYear()}`
}

/** "Bom dia" até 12h, "Boa tarde" até 18h, depois "Boa noite". */
export function saudacao(agora: number): string {
  const h = new Date(agora).getHours()
  return h < 12 ? 'Bom dia' : h < 18 ? 'Boa tarde' : 'Boa noite'
}

/** "hoje, 09:12", "ontem" ou "25/11" */
export function quandoAbriu(ms: number, agora: number): string {
  const hoje0 = inicioDoDia(agora)
  if (ms >= hoje0) return `hoje, ${isoHora(new Date(ms))}`
  if (ms >= hoje0 - DIA) return 'ontem'
  const d = new Date(ms)
  return `${pad(d.getDate())}/${pad(d.getMonth() + 1)}`
}
