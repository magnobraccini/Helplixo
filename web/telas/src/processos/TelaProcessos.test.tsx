import { fireEvent, render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import App from '../App'
import type { Arvore, DetalheProcesso } from '../api/processos'
import { ProvedorEdicao } from '../edicao/ContextoEdicao'
import { SAUDE_OK, servidorFalso } from '../teste/servidorFalso'

const ARVORE: Arvore = {
  pastas: [
    { id: 'f-rede', nome: 'Rede', total: 2, processos: [{ id: 'p-mapear', titulo: 'Mapear unidade G:', atualizadoEm: '2026-11-23T10:00:00' }], pastas: [
      { id: 'f-wifi', nome: 'Wi-Fi', total: 1, pastas: [], processos: [{ id: 'p-wifi', titulo: 'Liberar acesso ao Wi-Fi', atualizadoEm: null }] },
    ] },
    { id: 'f-imp', nome: 'Impressoras', total: 1, pastas: [], processos: [{ id: 'p-imp', titulo: 'Configurar impressora de rede', atualizadoEm: '2026-11-25T16:40:00' }] },
  ],
  semPasta: [],
}

const b = (tipo: string, texto: string, url: string | null = null) => ({ id: null, tipo, texto, url, negrito: false, italico: false, sublinhado: false })

const IMPRESSORA: DetalheProcesso = {
  caminho: ['Impressoras'],
  processo: {
    id: 'p-imp', pasta: 'f-imp', titulo: 'Configurar impressora de rede', descricao: 'Instalar uma impressora compartilhada pelo IP.',
    objetivo: null, preRequisitos: null, responsavel: 'Sup. Técnico', categoria: 'Impressoras', tags: ['impressora', 'rede'], passos: [],
    conteudo: [
      b('HEADING', '1. Antes de começar'),
      b('PARAGRAPH', 'Confirme o andar e o nome da impressora.'),
      b('HEADING', '2. Adicionar pelo IP'),
      b('NUMBERED_LIST', 'Abra Configurações.\nClique em Adicionar.'),
      b('WARNING', 'Não instale drivers baixados de sites desconhecidos.'),
    ] as DetalheProcesso['processo']['conteudo'],
    observacoes: null, links: ['\\\\servidor\\ti\\drivers\\HP'], criadoEm: null, atualizadoEm: '2026-11-25T16:40:00',
  },
}

function rotas() {
  return {
    ...SAUDE_OK,
    'GET /api/chamados': () => ({ corpo: [] }),
    'GET /api/calendario': () => ({ corpo: [] }),
    'GET /api/processos/arvore': () => ({ corpo: ARVORE }),
    'GET /api/processos/p-imp': () => ({ corpo: IMPRESSORA }),
    'GET /api/processos': (p: { url: string }) => ({
      corpo: new URL(p.url, 'http://x').searchParams.get('q') === 'wifi' ? [{ id: 'p-wifi', titulo: 'Liberar acesso ao Wi-Fi', caminho: 'Rede › Wi-Fi' }] : [],
    }),
  }
}

function abrirApp() {
  return render(
    <ProvedorEdicao>
      <App />
    </ProvedorEdicao>,
  )
}

async function irParaProcessos() {
  await userEvent.click(within(screen.getByRole('navigation', { name: 'Seções' })).getByRole('button', { name: /Processos/ }))
  return screen.findByRole('tree', { name: 'Pastas e processos' })
}

beforeEach(() => {
  window.localStorage.clear()
  window.sessionStorage.clear()
})
afterEach(() => vi.unstubAllGlobals())

describe('árvore de processos', () => {
  it('mostra as pastas fechadas com o total; abre e fecha; "Abrir todas" abre os subníveis', async () => {
    servidorFalso(rotas())
    abrirApp()
    const arvore = await irParaProcessos()

    const rede = await within(arvore).findByRole('treeitem', { name: /Rede\s*2/ })
    expect(rede).toHaveAttribute('aria-expanded', 'false')
    expect(within(arvore).queryByRole('treeitem', { name: /Mapear unidade G:/ })).not.toBeInTheDocument()

    await userEvent.click(rede)
    expect(within(arvore).getByRole('treeitem', { name: /Mapear unidade G:/ })).toBeInTheDocument()
    expect(within(arvore).queryByRole('treeitem', { name: /Liberar acesso/ })).not.toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: 'Abrir todas' }))
    expect(within(arvore).getByRole('treeitem', { name: /Liberar acesso ao Wi-Fi/ })).toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: 'Fechar todas' }))
    expect(within(arvore).queryByRole('treeitem', { name: /Mapear unidade G:/ })).not.toBeInTheDocument()
  })

  it('a busca usa o servidor e deixa só o caminho até o processo achado', async () => {
    servidorFalso(rotas())
    abrirApp()
    const arvore = await irParaProcessos()

    await userEvent.type(screen.getByLabelText('Procurar processo'), 'wifi')
    expect(await within(arvore).findByRole('treeitem', { name: /Liberar acesso ao Wi-Fi/ })).toBeInTheDocument()
    expect(within(arvore).queryByRole('treeitem', { name: /Impressoras/ })).not.toBeInTheDocument()
    expect(within(arvore).queryByRole('treeitem', { name: /Mapear unidade/ })).not.toBeInTheDocument()

    await userEvent.clear(screen.getByLabelText('Procurar processo'))
    await userEvent.type(screen.getByLabelText('Procurar processo'), 'nada')
    expect(await screen.findByText('Nenhum processo encontrado para “nada”.')).toBeInTheDocument()
  })
})

describe('leitura do processo', () => {
  it('abre ao lado da árvore, com meta, índice e o conteúdo', async () => {
    servidorFalso(rotas())
    abrirApp()
    const arvore = await irParaProcessos()

    expect(screen.getByText('Escolha um processo na árvore para ler aqui, sem sair da tela.')).toBeInTheDocument()
    await userEvent.click(await within(arvore).findByRole('treeitem', { name: /Impressoras/ }))
    await userEvent.click(within(arvore).getByRole('treeitem', { name: /Configurar impressora de rede/ }))

    const painel = await screen.findByRole('region', { name: 'Processo aberto' })
    expect(await within(painel).findByRole('heading', { level: 2, name: 'Configurar impressora de rede' })).toBeInTheDocument()
    expect(within(painel).getByText('3 seções · 0 imagem(ns) · 0 anexo(s) · 1 link(s)')).toBeInTheDocument()
    expect(within(painel).getByText('Sup. Técnico')).toBeInTheDocument()
    expect(within(painel).getAllByRole('listitem').map((li) => li.textContent)).toEqual(['Abra Configurações.', 'Clique em Adicionar.'])
    expect(within(painel).getByRole('note')).toHaveTextContent('Não instale drivers')

    const indice = within(painel).getByRole('navigation', { name: 'Neste processo' })
    const itens = within(indice).getAllByRole('button').slice(0, 3)
    expect(itens.map((x) => x.textContent)).toEqual(['1. Antes de começar', '2. Adicionar pelo IP', 'Links e pastas'])
    expect(itens[0]).toHaveAttribute('aria-current', 'true')
  })

  it('o índice leva à seção e acompanha a rolagem do painel', async () => {
    servidorFalso(rotas())
    abrirApp()
    const arvore = await irParaProcessos()
    await userEvent.click(await within(arvore).findByRole('treeitem', { name: /Impressoras/ }))
    await userEvent.click(within(arvore).getByRole('treeitem', { name: /Configurar impressora/ }))
    const painel = await screen.findByRole('region', { name: 'Processo aberto' })
    const indice = await within(painel).findByRole('navigation', { name: 'Neste processo' })

    // Clicar no índice rola até a seção.
    const rolar = vi.fn()
    Element.prototype.scrollIntoView = rolar
    await userEvent.click(within(indice).getByRole('button', { name: '2. Adicionar pelo IP' }))
    expect(rolar).toHaveBeenCalled()
    expect(within(indice).getByRole('button', { name: '2. Adicionar pelo IP' })).toHaveAttribute('aria-current', 'true')

    // Rolando o painel até o fim, o índice marca a última seção.
    Object.defineProperty(painel, 'scrollHeight', { configurable: true, value: 1000 })
    Object.defineProperty(painel, 'clientHeight', { configurable: true, value: 400 })
    painel.scrollTop = 600
    fireEvent.scroll(painel)
    expect(within(indice).getByRole('button', { name: 'Links e pastas' })).toHaveAttribute('aria-current', 'true')
  })

  it('caminho de pasta: copia e avisa (o navegador não abre pastas locais)', async () => {
    const escrever = vi.fn().mockResolvedValue(undefined)
    Object.defineProperty(navigator, 'clipboard', { configurable: true, value: { writeText: escrever } })
    servidorFalso(rotas())
    abrirApp()
    const arvore = await irParaProcessos()
    await userEvent.click(await within(arvore).findByRole('treeitem', { name: /Impressoras/ }))
    await userEvent.click(within(arvore).getByRole('treeitem', { name: /Configurar impressora/ }))

    await userEvent.click(await screen.findByRole('button', { name: 'Copiar caminho' }))
    expect(escrever).toHaveBeenCalledWith('\\\\servidor\\ti\\drivers\\HP')
    expect(await screen.findByText(/Caminho copiado: \\\\servidor\\ti\\drivers\\HP\./)).toBeInTheDocument()
  })

  it('o processo lido entra em "Continue de onde parou" e reabre de lá', async () => {
    servidorFalso(rotas())
    abrirApp()
    const arvore = await irParaProcessos()
    await userEvent.click(await within(arvore).findByRole('treeitem', { name: /Impressoras/ }))
    await userEvent.click(within(arvore).getByRole('treeitem', { name: /Configurar impressora/ }))
    await screen.findByRole('heading', { level: 2, name: 'Configurar impressora de rede' })
    await userEvent.click(screen.getByRole('button', { name: 'Fechar processo' }))

    await userEvent.click(within(screen.getByRole('navigation', { name: 'Seções' })).getByRole('button', { name: /Início/ }))
    const recentes = screen.getByRole('region', { name: 'Continue de onde parou' })
    await userEvent.click(within(recentes).getByRole('button', { name: /Configurar impressora de rede\s*Impressoras/ }))
    expect(await screen.findByRole('heading', { level: 2, name: 'Configurar impressora de rede' })).toBeInTheDocument()
  })
})
