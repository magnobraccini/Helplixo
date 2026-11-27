import { describe, expect, it } from 'vitest'
import type { Ocorrencia } from '../api/calendario'
import { blocosDoDia, diasDaVisao, diasDoMes, faltaTexto, navegar, podeRemover, tituloDaVisao } from './regras'

const oc = (p: Partial<Ocorrencia>): Ocorrencia => ({
  id: 'x', dia: '2026-11-27', titulo: 'X', tipo: 'Aviso', horaInicio: null, horaFim: null, diaInteiro: true, descricao: null,
  repeteTexto: null, autor: null, cargo: null, urgente: false, automatica: false, ...p,
})

describe('grade do mês', () => {
  it('novembro de 2026 começa no domingo 1º e tem 5 semanas', () => {
    const dias = diasDoMes('2026-11-27')
    expect(dias).toHaveLength(35)
    expect(dias[0]).toEqual({ iso: '2026-11-01', foraDoMes: false })
    expect(dias.at(-1)).toEqual({ iso: '2026-12-05', foraDoMes: true })
  })
  it('dezembro de 2026 começa na terça: as células antes são do mês anterior', () => {
    const dias = diasDoMes('2026-12-10')
    expect(dias[0]).toEqual({ iso: '2026-11-29', foraDoMes: true })
    expect(dias[2]).toEqual({ iso: '2026-12-01', foraDoMes: false })
  })
})

describe('navegação e títulos', () => {
  it('cada visão anda o seu passo', () => {
    expect(navegar('dia', '2026-11-27', 1)).toBe('2026-11-28')
    expect(navegar('semana', '2026-11-27', -1)).toBe('2026-11-20')
    expect(navegar('mes', '2026-11-27', 1)).toBe('2026-12-01')
    expect(navegar('agenda', '2026-11-27', 1)).toBe('2026-12-27')
  })
  it('títulos como na maquete', () => {
    expect(tituloDaVisao('mes', '2026-11-27', true)).toBe('Novembro de 2026')
    expect(tituloDaVisao('semana', '2026-11-27', true)).toBe('22 nov – 28 nov de 2026')
    expect(tituloDaVisao('semana', '2026-11-27', false)).toBe('23 nov – 27 nov de 2026')
    expect(tituloDaVisao('dia', '2026-11-27', true)).toBe('Sexta-feira, 27 de novembro de 2026')
    expect(tituloDaVisao('agenda', '2026-11-27', true)).toBe('A partir de 27/11/2026')
  })
  it('a agenda mostra 30 dias a partir do dia escolhido', () => {
    const d = diasDaVisao('agenda', '2026-11-27')
    expect(d).toHaveLength(30)
    expect(d.at(-1)).toBe('2026-12-26')
  })
})

describe('grade de horas', () => {
  it('posiciona pela hora (7h no topo, 44 px por hora) e divide as sobrepostas', () => {
    const blocos = blocosDoDia([
      oc({ id: 'a', horaInicio: '08:00:00', horaFim: '10:00:00', diaInteiro: false }),
      oc({ id: 'b', horaInicio: '09:00:00', horaFim: '09:30:00', diaInteiro: false }),
      oc({ id: 'c', horaInicio: '11:00:00', horaFim: null, diaInteiro: false }),
      oc({ id: 'd' }),
    ])
    expect(blocos.map((b) => [b.ocorrencia.id, b.topo, b.altura, b.esquerda, b.largura])).toEqual([
      ['a', 44, 88, 0, 50],
      ['b', 88, 22, 50, 50],
      ['c', 176, 44, 0, 50],
    ])
  })
})

describe('quem pode remover', () => {
  it('o mesmo cargo, ou um cargo acima no mesmo setor; automáticos nunca', () => {
    expect(podeRemover(oc({ cargo: 'Técnico' }), 'Técnico')).toBe(true)
    expect(podeRemover(oc({ cargo: 'Técnico' }), 'Chefe de TI')).toBe(true)
    expect(podeRemover(oc({ cargo: 'Chefe de TI' }), 'Técnico')).toBe(false)
    expect(podeRemover(oc({ cargo: 'Técnico' }), 'Triagem')).toBe(false)
    expect(podeRemover(oc({ tipo: 'Feriado', automatica: true, cargo: null }), 'Chefe de TI')).toBe(false)
  })
})

it('tempo até o lembrete', () => {
  expect(faltaTexto(0)).toBe('agora')
  expect(faltaTexto(25)).toBe('em 25 min')
  expect(faltaTexto(65)).toBe('em 1 h 05 min')
})
