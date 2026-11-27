import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import App from '../App'
import type { Ocorrencia } from '../api/calendario'
import type { Chamado } from '../api/chamados'
import { ProvedorEdicao } from '../edicao/ContextoEdicao'
import { SAUDE_OK, servidorFalso } from '../teste/servidorFalso'
import { DIA, inicioDoDia, isoData, paraServidor } from '../util/datas'

const MIN = 60_000
const haMinutos = (m: number) => `${paraServidor(Date.now() - m * MIN)}:00`
const hoje = isoData(new Date())
const amanha = isoData(new Date(inicioDoDia(Date.now()) + DIA))

function chamado(parcial: Partial<Chamado>): Chamado {
  return {
    numero: '1', nSemin: 'IC21', setor: 'Tributação - SEFAZ', contato: 'Ana', email: null, problema: 'Impressora',
    descricao: 'x', observacao: null, tecnico: null, area: 'Sup. Técnico', status: 'Aberto', urgencia: 'Normal',
    ocorrencia: 'Defeito', naoComputavel: false, abertoEm: haMinutos(30), andamento: [], fechamento: null,
    ...parcial,
  }
}

function marcacao(parcial: Partial<Ocorrencia>): Ocorrencia {
  return {
    id: 'm1', dia: hoje, titulo: 'Reunião', tipo: 'Reunião', horaInicio: '09:00:00', horaFim: '10:00:00', diaInteiro: false,
    descricao: null, repeteTexto: null, autor: null, cargo: null, urgente: false, automatica: false,
    ...parcial,
  }
}

const ABERTOS = [
  chamado({ numero: '177457', problema: 'Sistema da Folha', abertoEm: haMinutos(30) }),
  chamado({ numero: '177455', problema: 'Pasta compartilhada', tecnico: 'Diego', abertoEm: haMinutos(240) }),
]
const ANDAMENTO = [chamado({ numero: '177450', status: 'Em andamento', tecnico: 'Magno', abertoEm: haMinutos(600) })]
const AGENDA = [
  marcacao({ id: 'a', titulo: 'Backup do servidor', tipo: 'Manutenção', horaInicio: '07:30:00' }),
  marcacao({ id: 'b', titulo: 'Protocolo fora do ar', tipo: 'Aviso', urgente: true, diaInteiro: true, horaInicio: null }),
  marcacao({ id: 'c', dia: amanha, titulo: 'Feriado municipal', tipo: 'Feriado', diaInteiro: true, horaInicio: null }),
]

function rotas() {
  return {
    ...SAUDE_OK,
    'GET /api/chamados': (p: { url: string }) => {
      const status = new URL(p.url, 'http://x').searchParams.get('status')
      if (status === 'Aberto') return { corpo: ABERTOS }
      if (status === 'Em andamento') return { corpo: ANDAMENTO }
      if (status === 'Fechado') return { corpo: [] }
      return { corpo: [...ABERTOS, ...ANDAMENTO] }
    },
    'GET /api/chamados/177455': () => ({ corpo: ABERTOS[1] }),
    'GET /api/chamados/177457': () => ({ corpo: ABERTOS[0] }),
    'GET /api/calendario': () => ({ corpo: AGENDA }),
  }
}

function abrirApp() {
  return render(
    <ProvedorEdicao>
      <App />
    </ProvedorEdicao>,
  )
}

beforeEach(() => {
  window.localStorage.clear()
  window.sessionStorage.clear()
})
afterEach(() => vi.unstubAllGlobals())

describe('Início', () => {
  it('mostra saudação, resumo, aviso urgente e os 4 cards', async () => {
    const { pedidos } = servidorFalso(rotas())
    abrirApp()

    expect(await screen.findByText('3 chamado(s) em aberto · 2 marcação(ões) hoje')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Aviso urgente: Protocolo fora do ar/ })).toBeInTheDocument()

    const card = (rotulo: string) => screen.getByRole('button', { name: new RegExp(`^${rotulo}`) })
    expect(card('Abertos')).toHaveTextContent('Abertos21 sem técnico')
    expect(card('Em andamento')).toHaveTextContent('Em andamento10 com você')
    expect(card('Esperando há mais tempo')).toHaveTextContent('Nº 177455 · Pasta compartilhada')
    expect(card('Fechados hoje')).toHaveTextContent('média de 30 dias: —')

    // Pede todos os abertos, sem limite de data.
    expect(pedidos.some((p) => p.url.startsWith('/api/chamados?de=2000-01-01') && p.url.includes('status=Aberto'))).toBe(true)
  })

  it('"Precisa de atenção" e "Sem técnico" (perfil Triagem)', async () => {
    servidorFalso(rotas())
    abrirApp()

    const atencao = await screen.findByRole('region', { name: 'Precisa de atenção' })
    const linhas = await within(atencao).findAllByRole('button', { name: /^1774/ })
    expect(linhas.map((b) => b.textContent?.slice(0, 6))).toEqual(['177455', '177457', '177450'])
    expect(linhas[0]).toHaveTextContent('atrasado')

    const semTecnico = screen.getByRole('region', { name: 'Sem técnico' })
    expect(within(semTecnico).getAllByRole('button').map((b) => b.textContent?.slice(0, 6))).toEqual(['177457'])
  })

  it('Hoje e Amanhã do calendário', async () => {
    servidorFalso(rotas())
    abrirApp()

    const agenda = await screen.findByRole('region', { name: 'Hoje' })
    expect(await within(agenda).findByRole('button', { name: /07:30\s*Backup do servidor/ })).toBeInTheDocument()
    expect(within(agenda).getByRole('button', { name: /dia todo\s*Protocolo fora do ar/ })).toBeInTheDocument()
    expect(within(agenda).getByText('Amanhã')).toBeInTheDocument()
    expect(within(agenda).getByRole('button', { name: /Feriado municipal/ })).toBeInTheDocument()
  })

  it('clicar num card abre a lista de Chamados já filtrada', async () => {
    servidorFalso(rotas())
    abrirApp()

    await userEvent.click(await screen.findByRole('button', { name: /^Em andamento/ }))
    expect(screen.getByRole('heading', { level: 1, name: 'Chamados' })).toBeInTheDocument()
    expect(screen.getByRole('tab', { name: /Em andamento/ })).toHaveAttribute('aria-selected', 'true')
  })

  it('"Esperando há mais tempo" abre o chamado, que vai para "Continue de onde parou"', async () => {
    servidorFalso(rotas())
    abrirApp()

    const recentes = screen.getByRole('region', { name: 'Continue de onde parou' })
    expect(within(recentes).getByText('O que você abrir aparece aqui para voltar depressa.')).toBeInTheDocument()

    await userEvent.click(await screen.findByRole('button', { name: /^Esperando há mais tempo/ }))
    expect(await screen.findByRole('heading', { level: 1, name: 'Chamado 177455 · Pasta compartilhada' })).toBeInTheDocument()

    await userEvent.click(within(screen.getByRole('navigation', { name: 'Seções' })).getByRole('button', { name: /Início/ }))
    const item = within(screen.getByRole('region', { name: 'Continue de onde parou' })).getByRole('button', { name: /Chamado 177455 · Pasta compartilhada/ })
    expect(item).toHaveTextContent(/Chamados · Tributação - SEFAZ · hoje, \d\d:\d\d/)
    expect(JSON.parse(window.localStorage.getItem('helplixo-recentes')!)).toHaveLength(1)
  })
})
