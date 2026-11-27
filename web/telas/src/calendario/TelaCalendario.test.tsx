import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import App from '../App'
import type { Ocorrencia } from '../api/calendario'
import { ProvedorEdicao } from '../edicao/ContextoEdicao'
import { SAUDE_OK, servidorFalso, type PedidoRecebido } from '../teste/servidorFalso'
import { isoData } from '../util/datas'
import { diaLongo, somarDias } from './regras'

const hoje = isoData(new Date())
const amanha = somarDias(hoje, 1)

const oc = (p: Partial<Ocorrencia>): Ocorrencia => ({
  id: 'x', dia: hoje, titulo: 'X', tipo: 'Aviso', horaInicio: null, horaFim: null, diaInteiro: true, descricao: null,
  repeteTexto: null, autor: null, cargo: null, urgente: false, automatica: false, ...p,
})

const OCORRENCIAS = [
  oc({ id: 'r1', titulo: 'Reunião da equipe', tipo: 'Reunião', horaInicio: '09:00:00', horaFim: '10:00:00', diaInteiro: false, repeteTexto: 'Toda semana (segunda-feira)', descricao: 'Pauta: chamados parados.' }),
  oc({ id: 'a1', titulo: 'Protocolo fora do ar', tipo: 'Aviso', autor: 'Lúcia', cargo: 'Triagem', urgente: true }),
  oc({ id: 'f1', dia: amanha, titulo: 'Feriado municipal', tipo: 'Feriado', automatica: true }),
]

function rotas(extra = {}) {
  return {
    ...SAUDE_OK,
    'GET /api/chamados': () => ({ corpo: [] }),
    'GET /api/calendario': () => ({ corpo: OCORRENCIAS }),
    'GET /api/calendario/lembretes': () => ({ corpo: [{ id: 'r1', texto: 'Lembrete: Reunião da equipe · hoje às 09:00.', inicio: `${hoje}T09:00:00`, faltaMinutos: 25 }] }),
    ...extra,
  }
}

function abrirApp() {
  return render(
    <ProvedorEdicao>
      <App />
    </ProvedorEdicao>,
  )
}
async function irParaCalendario() {
  await userEvent.click(within(screen.getByRole('navigation', { name: 'Seções' })).getByRole('button', { name: /Calendário/ }))
  return screen.findByRole('grid')
}

beforeEach(() => {
  window.localStorage.clear()
  window.sessionStorage.clear()
})
afterEach(() => vi.unstubAllGlobals())

describe('calendário', () => {
  it('abre no Mês de hoje, com as marcações nos dias e o dia selecionado na lateral', async () => {
    const { pedidos } = servidorFalso(rotas())
    abrirApp()
    const grade = await irParaCalendario()

    const celulaHoje = within(grade).getByRole('gridcell', { name: diaLongo(hoje) })
    expect(celulaHoje).toHaveAttribute('aria-selected', 'true')
    expect(await within(celulaHoje).findByRole('button', { name: '09:00 Reunião da equipe' })).toBeInTheDocument()
    const lateral = screen.getByRole('region', { name: 'Dia selecionado' })
    expect(within(lateral).getByText('Protocolo fora do ar')).toBeInTheDocument()
    expect(within(lateral).getByText('por Lúcia (Triagem)')).toBeInTheDocument()
    // Pede ao servidor só os dias da visão (e os próximos 15).
    expect(pedidos.some((p: PedidoRecebido) => p.url.startsWith('/api/calendario?de='))).toBe(true)
  })

  it('desligar uma agenda esconde as marcações daquele tipo', async () => {
    servidorFalso(rotas())
    abrirApp()
    const grade = await irParaCalendario()
    await within(grade).findAllByRole('button', { name: /Reunião da equipe/ })

    await userEvent.click(screen.getByRole('checkbox', { name: /Reunião/ }))
    expect(within(grade).queryByRole('button', { name: /Reunião da equipe/ })).not.toBeInTheDocument()
  })

  it('a busca filtra e conta as marcações', async () => {
    servidorFalso(rotas())
    abrirApp()
    await irParaCalendario()
    await userEvent.type(screen.getByLabelText('Procurar no calendário'), 'protocolo')
    expect(await screen.findByText('1 marcação(ões) com “protocolo”')).toBeInTheDocument()
  })

  it('as visões Semana, Dia e Agenda', async () => {
    servidorFalso(rotas())
    abrirApp()
    await irParaCalendario()

    await userEvent.click(screen.getByRole('radio', { name: 'Semana' }))
    expect(await screen.findByRole('button', { name: /Reunião da equipe\s*09:00–10:00/ })).toBeInTheDocument()
    expect(screen.getAllByText('dia inteiro').length).toBeGreaterThan(0)

    await userEvent.click(screen.getByRole('radio', { name: 'Dia' }))
    expect(screen.getByRole('heading', { level: 2, name: `${diaLongo(hoje)} de ${new Date().getFullYear()}` })).toBeInTheDocument()

    await userEvent.click(screen.getByRole('radio', { name: 'Agenda' }))
    expect(screen.getByRole('button', { name: /Dia inteiro\s*Feriado municipal\s*Feriado/ })).toBeInTheDocument()
  })

  it('o detalhe mostra repetição e descrição; automáticos não podem ser removidos', async () => {
    servidorFalso(rotas())
    abrirApp()
    const grade = await irParaCalendario()

    await userEvent.click(await within(grade).findByRole('button', { name: '09:00 Reunião da equipe' }))
    const detalhe = screen.getByRole('dialog', { name: 'Reunião da equipe' })
    expect(within(detalhe).getByText('↻ Toda semana (segunda-feira)')).toBeInTheDocument()
    expect(within(detalhe).getByText('Pauta: chamados parados.')).toBeInTheDocument()
    await userEvent.click(within(detalhe).getByRole('button', { name: 'Fechar' }))

    await userEvent.click(within(grade).getByRole('button', { name: 'Feriado municipal' }))
    const feriado = screen.getByRole('dialog', { name: 'Feriado municipal' })
    expect(within(feriado).getByText('Entrou sozinho: feriado nacional.')).toBeInTheDocument()
    expect(within(feriado).queryByRole('button', { name: 'Remover' })).not.toBeInTheDocument()
  })

  it('remove o aviso do próprio cargo mandando o cargo no cabeçalho', async () => {
    const { pedidos } = servidorFalso(rotas({ 'DELETE /api/calendario/a1': () => ({ status: 204 }) }))
    abrirApp()
    const grade = await irParaCalendario()

    await userEvent.click(await within(grade).findByRole('button', { name: 'Protocolo fora do ar' }))
    await userEvent.click(within(screen.getByRole('dialog', { name: 'Protocolo fora do ar' })).getByRole('button', { name: 'Remover' }))

    expect(await screen.findByText('Removido do calendário: Protocolo fora do ar.')).toBeInTheDocument()
    const remocao = pedidos.find((p) => p.metodo === 'DELETE')!
    expect(remocao.cabecalhos['x-cargo']).toBe('Triagem')
  })

  it('sem modo edição, "Novo aviso" só deixa marcar Aviso, e manda quem marcou', async () => {
    const { pedidos } = servidorFalso(rotas({ 'POST /api/calendario': () => ({ status: 201, corpo: { marcacao: { id: 'n1', titulo: 'Rede lenta' }, avisos: [] } }) }))
    abrirApp()
    await irParaCalendario()
    expect(screen.queryByRole('button', { name: 'Nova marcação' })).not.toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: 'Novo aviso' }))
    const form = screen.getByRole('form', { name: 'Nova marcação' })
    expect(within(within(form).getByLabelText('Tipo')).getAllByRole('option').map((o) => o.textContent)).toEqual(['Aviso'])
    await userEvent.type(within(form).getByLabelText('Título'), 'Rede lenta')
    await userEvent.click(within(form).getByRole('button', { name: 'Salvar' }))

    expect(await screen.findByText('Marcado no calendário: Rede lenta.')).toBeInTheDocument()
    const envio = pedidos.find((p) => p.metodo === 'POST')!
    expect(envio.corpo).toMatchObject({ titulo: 'Rede lenta', tipo: 'Aviso', inicio: hoje, horaInicio: null, lembrete: 'dia' })
    expect(envio.cabecalhos['x-usuario']).toBe('Lúcia')
    expect(envio.cabecalhos['x-cargo']).toBe('Triagem')
  })

  it('clicar num horário da Semana abre o formulário com a hora e mostra o erro do servidor', async () => {
    servidorFalso(rotas({ 'POST /api/calendario': () => ({ status: 422, corpo: { erros: ['Escreva o que é a marcação.'] } }) }))
    abrirApp()
    await irParaCalendario()
    await userEvent.click(screen.getByRole('radio', { name: 'Semana' }))

    const d = new Date()
    await userEvent.click(screen.getAllByRole('button', { name: new RegExp(`^Marcar \\w+ ${d.getDate()} às 10h$`) })[0])
    const form = screen.getByRole('form', { name: 'Nova marcação' })
    expect(within(form).getByLabelText('Das')).toHaveValue('10:00')
    expect(within(form).getByLabelText('Até')).toHaveValue('11:00')
    await userEvent.click(within(form).getByRole('button', { name: 'Salvar' }))
    expect(await within(form).findByRole('alert')).toHaveTextContent('Escreva o que é a marcação.')
  })
})

describe('sino', () => {
  it('mostra os lembretes do calendário com o contador e marca como lidos', async () => {
    servidorFalso(rotas())
    abrirApp()

    const sino = await screen.findByRole('button', { name: /Notificações/ })
    expect(await within(sino).findByLabelText('1 não lida(s)')).toBeInTheDocument()
    await userEvent.click(sino)
    const lista = screen.getByRole('dialog', { name: 'Notificações' })
    expect(within(lista).getByText('Lembrete: Reunião da equipe · hoje às 09:00.')).toBeInTheDocument()
    expect(within(lista).getByText('em 25 min')).toBeInTheDocument()

    await userEvent.click(within(lista).getByRole('button', { name: 'Marcar todas como lidas' }))
    expect(within(sino).queryByLabelText('1 não lida(s)')).not.toBeInTheDocument()
  })
})
