// Contas do Calendário, copiadas da maquete (grade do mês, semana com horas, agenda, permissões).
import type { Ocorrencia } from '../api/calendario'
import { DIA, isoData, pad } from '../util/datas'

export type Visao = 'dia' | 'semana' | 'mes' | 'agenda'

export const VISOES: { chave: Visao; nome: string }[] = [
  { chave: 'dia', nome: 'Dia' },
  { chave: 'semana', nome: 'Semana' },
  { chave: 'mes', nome: 'Mês' },
  { chave: 'agenda', nome: 'Agenda' },
]

/** Tipos que se pode marcar (iguais aos do servidor). Sem o modo edição, só Aviso. */
export const TIPOS_MARCACAO = ['Aviso', 'Manutenção', 'Reunião', 'Ausência', 'Aniversário', 'Feriado', 'Outro']
/** As "agendas" que ligam e desligam (maquete: tipoCor sem "Outro"). */
export const AGENDAS = ['Aviso', 'Manutenção', 'Reunião', 'Ausência', 'Aniversário', 'Feriado', 'Contrato', 'Garantia']

export const LEMBRETES = [
  { valor: '', texto: 'Sem lembrete' },
  { valor: 'dia', texto: 'No começo do dia (7h30)' },
  { valor: '30', texto: '30 minutos antes' },
  { valor: '60', texto: '1 hora antes' },
  { valor: '1440', texto: '1 dia antes' },
]

export const AUTOMATICOS: Record<string, string> = { Feriado: 'feriado nacional', Contrato: 'vem dos Contatos externos', Garantia: 'vem do Inventário' }

export const SEMANA_CURTA = ['Dom', 'Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sáb']
export const MESES = ['janeiro', 'fevereiro', 'março', 'abril', 'maio', 'junho', 'julho', 'agosto', 'setembro', 'outubro', 'novembro', 'dezembro']
export const MESES_CURTO = ['jan', 'fev', 'mar', 'abr', 'mai', 'jun', 'jul', 'ago', 'set', 'out', 'nov', 'dez']
const DIAS_SEMANA = ['domingo', 'segunda-feira', 'terça-feira', 'quarta-feira', 'quinta-feira', 'sexta-feira', 'sábado']

/** "2026-11-27" → Date local (meia-noite). */
export const diaDe = (iso: string) => {
  const [a, m, d] = iso.split('-').map(Number)
  return new Date(a, m - 1, d)
}
export const somarDias = (iso: string, n: number) => {
  const d = diaDe(iso)
  return isoData(new Date(d.getFullYear(), d.getMonth(), d.getDate() + n))
}
const maiuscula = (s: string) => s[0].toUpperCase() + s.slice(1)
/** "Sexta-feira, 27 de novembro" */
export const diaLongo = (iso: string) => {
  const d = diaDe(iso)
  return `${maiuscula(DIAS_SEMANA[d.getDay()])}, ${d.getDate()} de ${MESES[d.getMonth()]}`
}
export const ehFimDeSemana = (iso: string) => [0, 6].includes(diaDe(iso).getDay())

/** As células do mês: começa no domingo da semana do dia 1 e completa semanas inteiras. */
export function diasDoMes(ancora: string): { iso: string; foraDoMes: boolean }[] {
  const a = diaDe(ancora)
  const primeiro = new Date(a.getFullYear(), a.getMonth(), 1)
  const total = new Date(a.getFullYear(), a.getMonth() + 1, 0).getDate()
  const celulas = Math.ceil((primeiro.getDay() + total) / 7) * 7
  return Array.from({ length: celulas }, (_, i) => {
    const d = new Date(a.getFullYear(), a.getMonth(), 1 - primeiro.getDay() + i)
    return { iso: isoData(d), foraDoMes: d.getMonth() !== a.getMonth() }
  })
}

export const diasDaSemana = (ancora: string) => {
  const inicio = somarDias(ancora, -diaDe(ancora).getDay())
  return Array.from({ length: 7 }, (_, i) => somarDias(inicio, i))
}

/** Os dias que a visão mostra (antes de tirar os fins de semana). */
export function diasDaVisao(visao: Visao, ancora: string): string[] {
  if (visao === 'dia') return [ancora]
  if (visao === 'semana') return diasDaSemana(ancora)
  if (visao === 'agenda') return Array.from({ length: 30 }, (_, i) => somarDias(ancora, i))
  return diasDoMes(ancora).map((d) => d.iso)
}

/** Para onde vão "‹" e "›" em cada visão. */
export function navegar(visao: Visao, ancora: string, passo: 1 | -1): string {
  if (visao === 'dia') return somarDias(ancora, passo)
  if (visao === 'semana') return somarDias(ancora, 7 * passo)
  if (visao === 'agenda') return somarDias(ancora, 30 * passo)
  const a = diaDe(ancora)
  return isoData(new Date(a.getFullYear(), a.getMonth() + passo, 1))
}

export function tituloDaVisao(visao: Visao, ancora: string, fds: boolean): string {
  const a = diaDe(ancora)
  if (visao === 'dia') return `${diaLongo(ancora)} de ${a.getFullYear()}`
  if (visao === 'agenda') return `A partir de ${ancora.split('-').reverse().join('/')}`
  if (visao === 'semana') {
    const dias = diasDaSemana(ancora).filter((d) => fds || !ehFimDeSemana(d))
    const i = diaDe(dias[0])
    const f = diaDe(dias[dias.length - 1])
    return `${i.getDate()} ${MESES_CURTO[i.getMonth()]} – ${f.getDate()} ${MESES_CURTO[f.getMonth()]} de ${f.getFullYear()}`
  }
  return `${maiuscula(MESES[a.getMonth()])} de ${a.getFullYear()}`
}

export const hora = (o: Ocorrencia) => (o.horaInicio ? o.horaInicio.slice(0, 5) : '')
export const textoHora = (o: Ocorrencia) => (o.horaInicio ? hora(o) + (o.horaFim ? `–${o.horaFim.slice(0, 5)}` : '') : 'Dia inteiro')
/** Sem hora primeiro, depois pela hora (maquete: ordenar). */
export const porHora = (a: Ocorrencia, b: Ocorrencia) => hora(a).localeCompare(hora(b))

/** Grade de horas da Semana e do Dia: 7h às 19h, 44 px por hora, expediente 8h–14h destacado. */
export const GRADE = { inicio: 7, fim: 19, px: 44 }
const minutos = (h: string) => Number(h.slice(0, 2)) * 60 + Number(h.slice(3, 5))

export type BlocoGrade = { ocorrencia: Ocorrencia; topo: number; altura: number; esquerda: number; largura: number }

/** Posiciona as marcações com hora; as que se sobrepõem dividem a largura em faixas. */
export function blocosDoDia(ocorrencias: Ocorrencia[]): BlocoGrade[] {
  const faixas: number[] = []
  const comFaixa = ocorrencias
    .filter((o) => o.horaInicio)
    .sort(porHora)
    .map((o) => {
      const a = minutos(o.horaInicio!)
      const b = o.horaFim ? minutos(o.horaFim) : a + 60
      let f = faixas.findIndex((fim) => fim <= a)
      if (f < 0) {
        f = faixas.length
        faixas.push(b)
      } else faixas[f] = b
      return { o, a, b, f }
    })
  const n = Math.max(1, faixas.length)
  return comFaixa.map(({ o, a, b, f }) => ({
    ocorrencia: o,
    topo: Math.max(0, ((a - GRADE.inicio * 60) / 60) * GRADE.px),
    altura: Math.max(22, ((Math.min(b, GRADE.fim * 60) - Math.max(a, GRADE.inicio * 60)) / 60) * GRADE.px),
    esquerda: (f * 100) / n,
    largura: 100 / n,
  }))
}

export const horasDaGrade = () => Array.from({ length: GRADE.fim - GRADE.inicio }, (_, i) => `${pad(GRADE.inicio + i)}:00`)

/** Cargos: nível e setor (iguais a web/servidor/.../Cargos.java). */
const CARGOS: Record<string, { nivel: number; setor: string }> = {
  'Triagem': { nivel: 1, setor: 'Informática' },
  'Técnico': { nivel: 1, setor: 'Informática' },
  'Chefe de TI': { nivel: 3, setor: 'Informática' },
}
/** Remover: o próprio cargo do autor, ou um cargo de nível maior no mesmo setor. Automáticos nunca. */
export function podeRemover(o: Ocorrencia, meuCargo: string): boolean {
  if (o.automatica || !o.cargo) return false
  const a = CARGOS[meuCargo]
  const b = CARGOS[o.cargo]
  if (!a || !b) return false
  return meuCargo === o.cargo || (a.nivel > b.nivel && a.setor === b.setor)
}

/** Quantos minutos/horas faltam, para o sino: "em 25 min", "em 1 h 05 min", "agora". */
export const faltaTexto = (min: number) => (min <= 0 ? 'agora' : min < 60 ? `em ${min} min` : `em ${Math.floor(min / 60)} h ${pad(min % 60)} min`)

export const DIA_MS = DIA
