import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import App from '../App'
import type { Chamado } from '../api/chamados'
import { ProvedorEdicao } from '../edicao/ContextoEdicao'
import { SAUDE_OK, servidorFalso } from '../teste/servidorFalso'
import { montarGrupos } from './grupos'

const chamado = (p: Partial<Chamado>): Chamado => ({
  numero: '1', nSemin: null, setor: 'SEMAD', contato: 'Ana', email: null, problema: 'Impressora', descricao: '', observacao: null,
  tecnico: null, area: 'Hardware', status: 'Aberto', urgencia: 'Normal', ocorrencia: '', naoComputavel: false,
  abertoEm: '2026-11-20T09:00:00', andamento: [], fechamento: null, ...p,
})

const CHAMADOS = [
  chamado({ numero: '2611-0001', problema: 'Impressora não imprime', contato: 'Ana', nSemin: 'SEMIN-0101', abertoEm: '2026-11-20T09:00:00' }),
  chamado({ numero: '2611-0002', problema: 'Sem internet', contato: 'Bruno', abertoEm: '2026-11-25T09:00:00' }),
]

const rotas = {
  ...SAUDE_OK,
  'GET /api/calendario': () => ({ corpo: [] }),
  'GET /api/calendario/lembretes': () => ({ corpo: [] }),
  'GET /api/chamados': () => ({ corpo: CHAMADOS }),
  'GET /api/chamados/2611-0001': () => ({ corpo: CHAMADOS[0] }),
  'GET /api/inventario': () => ({
    corpo: { maquinas: [{ nSemin: 'SEMIN-0101', patrimonio: '1', tipo: 'Impressora', modelo: 'HP 107', local: 'SEMAD', usuario: 'Ana', situacao: 'Ativo', office: '' }], total: 1, temMais: false },
  }),
  'GET /api/processos': () => ({ corpo: [{ id: 'p1', titulo: 'Instalar impressora de rede', caminho: 'Impressoras' }] }),
  'GET /api/contatos/internos': () => ({
    corpo: { contatos: [{ nome: 'Ana Impressão', setor: 'Gráfica', andar: null, ramal: '210', email: null }], total: 1, deTotal: 9, opcoes: { secretarias: [], andares: [] } },
  }),
  'GET /api/contatos/externos': () => ({ corpo: { contatos: [], total: 0, deTotal: 3, categorias: [] } }),
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

describe('montarGrupos', () => {
  const vazio = { chamados: [], maquinas: [], processos: [], internos: [], externos: [] }

  it('sem texto mostra até 3 chamados; com texto, até 5 que casam', () => {
    const muitos = Array.from({ length: 7 }, (_, i) => chamado({ numero: `n${i}`, problema: `Rede ${i}` }))
    expect(montarGrupos('', { ...vazio, chamados: muitos })[0].itens).toHaveLength(3)
    expect(montarGrupos('rede', { ...vazio, chamados: muitos })[0].itens).toHaveLength(5)
    expect(montarGrupos('teclado', { ...vazio, chamados: muitos })).toEqual([])
  })

  it('o chamado casa pelo número, máquina, contato, setor e problema', () => {
    const c = [chamado({ numero: '2611-0009', nSemin: 'SEMIN-0042', contato: 'Carla', setor: 'SEMED', problema: 'Mouse' })]
    for (const q of ['0009', 'semin-0042', 'carla', 'semed', 'mouse']) expect(montarGrupos(q, { ...vazio, chamados: c })).toHaveLength(1)
    expect(montarGrupos('', { ...vazio, chamados: c })[0].itens[0].dica).toBe('SEMIN-0042 · Aberto')
  })
})

describe('busca única', () => {
  it('Ctrl K abre, junta os grupos do servidor e Esc fecha', async () => {
    const { pedidos } = servidorFalso(rotas)
    abrirApp()
    await userEvent.keyboard('{Control>}k{/Control}')
    const janela = screen.getByRole('dialog', { name: 'Buscar em tudo' })
    // Sem texto: os chamados mais novos primeiro.
    const chamados = await within(janela).findByRole('group', { name: 'Chamados' })
    expect(within(chamados).getAllByRole('button')[0]).toHaveTextContent('Sem internet')

    await userEvent.type(within(janela).getByRole('searchbox', { name: 'Buscar em tudo' }), 'impr')
    expect(await within(janela).findByRole('group', { name: 'Máquinas' })).toHaveTextContent('Impressora HP 107')
    expect(within(janela).getByRole('group', { name: 'Processos' })).toHaveTextContent('Instalar impressora de rede')
    expect(within(janela).getByRole('group', { name: 'Contatos internos' })).toHaveTextContent('ramal 210')
    expect(within(janela).getByRole('group', { name: 'Chamados' })).not.toHaveTextContent('Sem internet')
    expect(within(janela).queryByRole('group', { name: 'Contatos externos' })).not.toBeInTheDocument()
    // Esperou parar de digitar: o servidor só recebeu o texto inteiro.
    expect(pedidos.filter((p) => p.url.startsWith('/api/inventario?')).map((p) => p.url)).toEqual(['/api/inventario?q=impr'])

    await userEvent.keyboard('{Escape}')
    expect(screen.queryByRole('dialog', { name: 'Buscar em tudo' })).not.toBeInTheDocument()
  })

  it('o botão Buscar da lateral abre; escolher um chamado abre a ficha', async () => {
    servidorFalso(rotas)
    abrirApp()
    await userEvent.click(within(screen.getByRole('navigation', { name: 'Seções' })).getByRole('button', { name: /Buscar/ }))
    const janela = screen.getByRole('dialog', { name: 'Buscar em tudo' })
    await userEvent.click(await within(janela).findByRole('button', { name: /Impressora não imprime/ }))

    expect(screen.queryByRole('dialog', { name: 'Buscar em tudo' })).not.toBeInTheDocument()
    await vi.waitFor(() => expect(screen.getByRole('navigation', { name: 'Caminho' })).toHaveTextContent(/Chamados.*2611-0001/))
  })

  it('escolher um contato abre os Contatos já filtrados por ele', async () => {
    servidorFalso(rotas)
    abrirApp()
    await userEvent.keyboard('{Control>}k{/Control}')
    await userEvent.type(screen.getByRole('searchbox', { name: 'Buscar em tudo' }), 'ana')
    await userEvent.click(await screen.findByRole('button', { name: /Ana Impressão/ }))

    expect(await screen.findByLabelText('Filtrar contatos')).toHaveValue('Ana Impressão')
  })

  it('avisa quando não acha nada e pede 2 letras para ir ao servidor', async () => {
    servidorFalso({
      ...rotas,
      'GET /api/inventario': () => ({ corpo: { maquinas: [], total: 0, temMais: false } }),
      'GET /api/processos': () => ({ corpo: [] }),
      'GET /api/contatos/internos': () => ({ corpo: { contatos: [], total: 0, deTotal: 9, opcoes: { secretarias: [], andares: [] } } }),
    })
    abrirApp()
    await userEvent.keyboard('{Control>}k{/Control}')
    const campo = screen.getByRole('searchbox', { name: 'Buscar em tudo' })
    await userEvent.type(campo, 'x')
    expect(screen.getByText(/Digite ao menos 2 letras/)).toBeInTheDocument()
    await userEvent.type(campo, 'yz')
    expect(await screen.findByText('Nada encontrado para “xyz”.')).toBeInTheDocument()
  })
})
