import { describe, expect, it } from 'vitest'
import type { Chamado } from '../api/chamados'
import { conferirHorario, intervaloDoPeriodo, minutosDoChamado, reincidencias, secretariaDe } from './regras'

// Quarta-feira, 25/11/2026, 10:00 (hora local).
const AGORA = new Date(2026, 10, 25, 10, 0).getTime()

function chamado(parcial: Partial<Chamado>): Chamado {
  return {
    numero: '1', nSemin: 'IC21', setor: 'Tributação - SEFAZ', contato: 'Ana', email: null, problema: 'Impressora',
    descricao: 'x', observacao: null, tecnico: null, area: 'Sup. Técnico', status: 'Aberto', urgencia: 'Normal',
    ocorrencia: 'Defeito', naoComputavel: false, abertoEm: '2026-11-25T09:00:00', andamento: [], fechamento: null,
    ...parcial,
  }
}

describe('coluna Min.', () => {
  it('aberto há menos de 2 h fica verde', () => {
    const r = minutosDoChamado(chamado({ abertoEm: '2026-11-25T08:01:00' }), AGORA)
    expect(r).toMatchObject({ minutos: 119, cor: 'var(--c-2f7a3a)', rotulo: 'no prazo' })
  })
  it('aberto entre 2 h e 3 h fica âmbar', () => {
    expect(minutosDoChamado(chamado({ abertoEm: '2026-11-25T08:00:00' }), AGORA).cor).toBe('var(--c-b7791f)')
  })
  it('aberto há 3 h ou mais fica vermelho', () => {
    expect(minutosDoChamado(chamado({ abertoEm: '2026-11-25T07:00:00' }), AGORA).cor).toBe('var(--c-c4302b)')
  })
  it('em andamento fica azul, mesmo se demorado', () => {
    expect(minutosDoChamado(chamado({ status: 'Em andamento', abertoEm: '2026-11-24T07:00:00' }), AGORA).cor).toBe('var(--c-337ea9)')
  })
  it('não computável fica cinza', () => {
    expect(minutosDoChamado(chamado({ naoComputavel: true, abertoEm: '2026-11-25T05:00:00' }), AGORA).cor).toBe('var(--c-677085)')
  })
  it('fechado mostra o tempo de atendimento, em cinza', () => {
    const c = chamado({ status: 'Fechado', fechamento: { descricao: 'ok', em: '2026-11-25T09:45:00', tecnico: 'Diego' } })
    expect(minutosDoChamado(c, AGORA)).toMatchObject({ minutos: 45, cor: 'var(--c-677085)' })
  })
})

describe('período', () => {
  it('hoje, 7 e 30 dias (contando hoje)', () => {
    expect(intervaloDoPeriodo('hoje', '', '', AGORA)).toEqual({ de: '2026-11-25', ate: '2026-11-25', texto: 'hoje' })
    expect(intervaloDoPeriodo('7', '', '', AGORA)).toMatchObject({ de: '2026-11-19', ate: '2026-11-25' })
    expect(intervaloDoPeriodo('30', '', '', AGORA)).toMatchObject({ de: '2026-10-27', texto: 'nos últimos 30 dias' })
  })
  it('datas escolhidas: recusa início depois do fim', () => {
    expect(intervaloDoPeriodo('custom', '2026-11-01', '2026-11-10', AGORA)).toMatchObject({ texto: 'de 01/11/2026 até 10/11/2026' })
    expect(intervaloDoPeriodo('custom', '2026-11-10', '2026-11-01', AGORA)).toBeNull()
  })
})

describe('↺ reincidência', () => {
  it('conta só os OUTROS chamados da mesma máquina nos últimos 7 dias', () => {
    const atual = chamado({ numero: '10' })
    const recentes = [
      atual,
      chamado({ numero: '11', abertoEm: '2026-11-20T09:00:00' }),
      chamado({ numero: '12', abertoEm: '2026-11-10T09:00:00' }), // mais de 7 dias
      chamado({ numero: '13', nSemin: 'XX99' }), // outra máquina
    ]
    expect(reincidencias(atual, recentes, AGORA)).toBe(1)
    expect(reincidencias(chamado({ nSemin: '' }), recentes, AGORA)).toBe(0)
  })
})

describe('conferência do horário', () => {
  const aberto = new Date(2026, 10, 25, 9, 0).getTime()
  it('aponta futuro e antes da abertura como erro', () => {
    expect(conferirHorario(AGORA + 10 * 60_000, aberto, AGORA)[0].tipo).toBe('erro')
    expect(conferirHorario(aberto - 60_000, aberto, AGORA).some((c) => c.tipo === 'erro' && c.texto.includes('antes da abertura'))).toBe(true)
  })
  it('fora do expediente é só aviso', () => {
    const sabado = new Date(2026, 10, 28, 10, 0).getTime()
    const r = conferirHorario(sabado, aberto, sabado)
    expect(r.filter((c) => c.tipo === 'erro')).toHaveLength(0)
    expect(r.some((c) => c.tipo === 'aviso')).toBe(true)
  })
  it('mostra o tempo de atendimento', () => {
    expect(conferirHorario(AGORA, aberto, AGORA).at(-1)).toEqual({ tipo: 'info', texto: 'Tempo de atendimento: 1 h 00 min.' })
  })
})

it('secretaria vem depois do " - " do setor', () => {
  expect(secretariaDe('Controle Interno - GABINETE')).toBe('GABINETE')
})
