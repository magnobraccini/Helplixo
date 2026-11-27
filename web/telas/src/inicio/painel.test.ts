import { describe, expect, it } from 'vitest'
import type { Chamado } from '../api/chamados'
import { avisoUrgente, dataPorExtenso, meusChamados, montarKpis, precisaDeAtencao, quandoAbriu, saudacao } from './painel'

// Sexta-feira, 27/11/2026, 10:00 (hora local).
const AGORA = new Date(2026, 10, 27, 10, 0).getTime()

function chamado(parcial: Partial<Chamado>): Chamado {
  return {
    numero: '1', nSemin: 'IC21', setor: 'Tributação - SEFAZ', contato: 'Ana', email: null, problema: 'Impressora',
    descricao: 'x', observacao: null, tecnico: null, area: 'Sup. Técnico', status: 'Aberto', urgencia: 'Normal',
    ocorrencia: 'Defeito', naoComputavel: false, abertoEm: '2026-11-27T09:00:00', andamento: [], fechamento: null,
    ...parcial,
  }
}

const EM_ABERTO = [
  chamado({ numero: 'A1', abertoEm: '2026-11-27T09:30:00' }),
  chamado({ numero: 'A2', abertoEm: '2026-11-26T08:00:00', tecnico: 'Diego' }),
  chamado({ numero: 'NC', abertoEm: '2026-11-01T08:00:00', naoComputavel: true }),
  chamado({ numero: 'U1', abertoEm: '2026-11-27T09:50:00', urgencia: 'Urgente' }),
  chamado({ numero: 'E1', status: 'Em andamento', abertoEm: '2026-11-20T08:00:00', tecnico: 'Magno' }),
]
const FECHADOS = [
  chamado({ numero: 'F1', status: 'Fechado', abertoEm: '2026-11-27T08:00:00', fechamento: { descricao: 'ok', em: '2026-11-27T09:00:00', tecnico: 'Diego' } }),
  chamado({ numero: 'F2', status: 'Fechado', abertoEm: '2026-11-20T08:00:00', fechamento: { descricao: 'ok', em: '2026-11-20T11:00:00', tecnico: 'Diego' } }),
]

describe('os 4 cards', () => {
  const [abertos, andamento, esperando, fechadosHoje] = montarKpis(EM_ABERTO, FECHADOS, 'Magno', AGORA)

  it('Abertos e Em andamento, com "sem técnico" e "com você"', () => {
    expect(abertos).toMatchObject({ valor: '4', detalhe: '3 sem técnico', acao: { tipo: 'lista', status: 'Aberto' } })
    expect(andamento).toMatchObject({ valor: '1', detalhe: '1 com você' })
  })

  it('"Esperando há mais tempo" é o aberto mais antigo, sem contar em andamento nem não computável', () => {
    expect(esperando).toMatchObject({ valor: '1 dia 2 h', detalhe: 'Nº A2 · Impressora', acao: { tipo: 'chamado', numero: 'A2' } })
  })

  it('"Fechados hoje" com a média de atendimento de 30 dias', () => {
    // F1 levou 1 h; F2 levou 3 h → média 2 h
    expect(fechadosHoje).toMatchObject({ valor: '1', detalhe: 'média de 30 dias: 2 h 00 min' })
  })

  it('sem nada esperando, mostra um traço', () => {
    expect(montarKpis([], [], 'Magno', AGORA)[2]).toMatchObject({ valor: '—', detalhe: 'nada esperando' })
  })
})

it('"Precisa de atenção": abertos, depois urgentes, depois os mais antigos', () => {
  expect(precisaDeAtencao(EM_ABERTO, AGORA).map((l) => l.numero)).toEqual(['U1', 'A2', 'A1', 'E1'])
  const [u1] = precisaDeAtencao(EM_ABERTO, AGORA)
  expect(u1).toMatchObject({ espera: '10 min', estado: 'no prazo', onde: 'Tributação · IC21', tecnico: 'sem técnico' })
})

it('"Sem técnico" para a Triagem e "Com você" para os outros', () => {
  expect(meusChamados(EM_ABERTO, 'Triagem', 'Lúcia', AGORA)).toMatchObject({ titulo: 'Sem técnico' })
  expect(meusChamados(EM_ABERTO, 'Triagem', 'Lúcia', AGORA).lista.map((l) => l.numero)).toEqual(['NC', 'A1', 'U1'])
  expect(meusChamados(EM_ABERTO, 'Técnico', 'Magno', AGORA).lista.map((l) => l.numero)).toEqual(['E1'])
})

it('aviso urgente só de Aviso ou Manutenção marcados como urgentes', () => {
  const base = { id: '1', dia: '2026-11-27', horaInicio: null, horaFim: null, diaInteiro: true, descricao: null, repeteTexto: null, autor: null, cargo: null, automatica: false }
  expect(avisoUrgente([{ ...base, titulo: 'Feriado', tipo: 'Feriado', urgente: true }])).toBeNull()
  expect(avisoUrgente([{ ...base, titulo: 'Protocolo fora', tipo: 'Aviso', urgente: true }])?.titulo).toBe('Protocolo fora')
})

it('saudação, data por extenso e "quando abriu"', () => {
  expect(saudacao(AGORA)).toBe('Bom dia')
  expect(saudacao(new Date(2026, 10, 27, 15).getTime())).toBe('Boa tarde')
  expect(dataPorExtenso(AGORA)).toBe('Sexta-feira, 27 de novembro de 2026')
  expect(quandoAbriu(new Date(2026, 10, 27, 9, 12).getTime(), AGORA)).toBe('hoje, 09:12')
  expect(quandoAbriu(new Date(2026, 10, 26, 9, 12).getTime(), AGORA)).toBe('ontem')
  expect(quandoAbriu(new Date(2026, 10, 20).getTime(), AGORA)).toBe('20/11')
})
