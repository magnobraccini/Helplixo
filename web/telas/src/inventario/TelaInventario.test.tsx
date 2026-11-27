import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import App from '../App'
import type { FichaMaquina, Maquina } from '../api/inventario'
import { ProvedorEdicao } from '../edicao/ContextoEdicao'
import { SAUDE_OK, servidorFalso } from '../teste/servidorFalso'

function maquina(parcial: Partial<Maquina> = {}): Maquina {
  return {
    nSemin: 'IC21', patrimonio: 'PMS-01350', tipo: 'Computador', modelo: 'Dell OptiPlex 7090', serie: '7HXK2Q3',
    processador: 'Intel Core i5', memoria: '16 GB', armazenamento: 'SSD 512 GB', sistema: 'Windows 11 Pro', ip: '10.0.13.21',
    mac: 'B0-7B', local: 'Informática - SMED', usuario: 'Magno', situacao: 'Em uso', observacao: null,
    compra: { fornecedor: 'Dell', notaFiscal: '000.184', data: '2023-03-14', valor: 'R$ 5.480,00', processo: 'Pregão 012/2023', garantiaAte: '2026-03-14' },
    softwares: [
      { nome: 'Microsoft Office 2021 Standard', licenca: 'ABC-123', tipo: 'Volume' },
      { nome: 'AutoCAD', licenca: null, tipo: 'Assinatura' },
    ],
    sistemasPmsl: [{ nome: 'SIGEM', linguagem: 'Delphi' }],
    consertos: [], baixa: null,
    ...parcial,
  }
}

const ficha = (m: Maquina): FichaMaquina => ({
  maquina: m,
  office: { situacao: 'sim', detalhe: 'Microsoft Office 2021 Standard (licença Volume).' },
  garantia: { situacao: 'vencida', texto: 'Vencida em 14/03/2026' },
  historico: [
    { quando: '2026-11-25T09:12:00', tipo: 'Chamado', texto: 'Impressora (aberto)', referencia: '177456', quem: 'Diego' },
    { quando: '2026-03-10T00:00:00', tipo: 'Conserto', texto: 'Fonte queimada → trocada (assistência)', referencia: null, quem: null },
  ],
})

const BUSCA = {
  maquinas: [{ nSemin: 'IC21', patrimonio: 'PMS-01350', tipo: 'Computador', modelo: 'Dell OptiPlex 7090', local: 'Informática - SMED', usuario: 'Magno', situacao: 'Em uso', office: 'sim' }],
  total: 1,
  temMais: false,
}

function rotas(extra = {}) {
  return {
    ...SAUDE_OK,
    'GET /api/chamados': () => ({ corpo: [] }),
    'GET /api/calendario': () => ({ corpo: [] }),
    'GET /api/inventario/resumo': () => ({ corpo: { total: 14, porTipo: { Computador: 6, Impressora: 2 } } }),
    'GET /api/inventario': () => ({ corpo: BUSCA }),
    'GET /api/inventario/IC21': () => ({ corpo: ficha(maquina()) }),
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

async function irParaInventario() {
  await userEvent.click(within(screen.getByRole('navigation', { name: 'Seções' })).getByRole('button', { name: /Inventário/ }))
}

/** Liga o modo edição com uma chave já guardada (o servidor confirma que vale). */
function comModoEdicao() {
  window.sessionStorage.setItem('helplixo-chave-edicao', JSON.stringify({ chave: 'chave-do-teste', validaAte: '2999-01-01T00:00:00Z' }))
  return { 'GET /api/edicao': () => ({ corpo: { configurado: true, ligado: true, validaAte: '2999-01-01T00:00:00Z' } }) }
}

beforeEach(() => {
  window.localStorage.clear()
  window.sessionStorage.clear()
})
afterEach(() => vi.unstubAllGlobals())

describe('busca do inventário', () => {
  it('sem busca mostra o resumo; só procura com 2 letras ou mais', async () => {
    const { pedidos } = servidorFalso(rotas())
    abrirApp()
    await irParaInventario()

    expect(await screen.findByText(/14 máquinas no inventário \(6 computador, 2 impressora\)/)).toBeInTheDocument()
    await userEvent.type(screen.getByLabelText('Procurar máquina'), 'i')
    await new Promise((r) => setTimeout(r, 350))
    expect(pedidos.some((p) => p.url.startsWith('/api/inventario?q='))).toBe(false)

    await userEvent.type(screen.getByLabelText('Procurar máquina'), 'c')
    expect(await screen.findByRole('button', { name: /IC21\s*Computador Dell OptiPlex 7090/ })).toBeInTheDocument()
    expect(pedidos.some((p) => p.url === '/api/inventario?q=ic')).toBe(true)
  })

  it('avisa quando nada é encontrado', async () => {
    servidorFalso(rotas({ 'GET /api/inventario': () => ({ corpo: { maquinas: [], total: 0, temMais: false } }) }))
    abrirApp()
    await irParaInventario()

    await userEvent.type(screen.getByLabelText('Procurar máquina'), 'zz')
    expect(await screen.findByText('Nenhuma máquina encontrada para “zz”.')).toBeInTheDocument()
  })
})

describe('ficha da máquina', () => {
  async function abrirIC21() {
    abrirApp()
    await irParaInventario()
    await userEvent.type(screen.getByLabelText('Procurar máquina'), 'ic21')
    await userEvent.click(await screen.findByRole('button', { name: /IC21\s*Computador/ }))
    return screen.findByRole('heading', { level: 1, name: 'IC21 · Computador Dell OptiPlex 7090' })
  }

  it('tem as 4 abas: Identificação, Compra, Softwares (com Office) e Histórico', async () => {
    servidorFalso(rotas())
    await abrirIC21()

    expect(screen.getByRole('tab', { name: 'Identificação e configuração' })).toHaveAttribute('aria-selected', 'true')
    expect(screen.getByText('10.0.13.21')).toBeInTheDocument()

    await userEvent.click(screen.getByRole('tab', { name: 'Informações de compra' }))
    expect(screen.getByText('Pregão 012/2023')).toBeInTheDocument()
    expect(screen.getByText('14/03/2023')).toBeInTheDocument()

    await userEvent.click(screen.getByRole('tab', { name: 'Softwares instalados' }))
    expect(screen.getByText('Microsoft Office 2021 Standard (licença Volume).')).toBeInTheDocument()
    expect(screen.getByRole('cell', { name: 'Sem licença registrada' })).toBeInTheDocument()
    expect(screen.getByRole('cell', { name: 'Delphi' })).toBeInTheDocument()

    await userEvent.click(screen.getByRole('tab', { name: 'Consertos e histórico' }))
    expect(screen.getByText(/Fonte queimada/)).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: '#177456' }))
    expect(screen.getByRole('navigation', { name: 'Caminho' })).toHaveTextContent(/Chamados\/\s*177456/)
  })

  it('sem modo edição não mostra Editar nem Dar baixa; movimentação sempre aparece', async () => {
    servidorFalso(rotas())
    await abrirIC21()

    expect(screen.queryByRole('button', { name: 'Editar máquina' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Dar baixa' })).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Registrar movimentação' })).toBeInTheDocument()
  })

  it('registra movimentação e recarrega a ficha', async () => {
    let atual = maquina()
    const { pedidos } = servidorFalso(
      rotas({
        'GET /api/inventario/IC21': () => ({ corpo: ficha(atual) }),
        'POST /api/inventario/IC21/movimentacoes': () => {
          atual = maquina({ local: 'Assistência técnica', situacao: 'Em manutenção' })
          return { corpo: atual }
        },
      }),
    )
    await abrirIC21()

    await userEvent.click(screen.getByRole('button', { name: 'Registrar movimentação' }))
    const form = screen.getByRole('form', { name: 'Registrar movimentação de equipamento' })
    expect(within(form).getByLabelText('Nº Semin *')).toHaveValue('IC21')
    await userEvent.selectOptions(within(form).getByLabelText('Foi para *'), 'Assistência técnica')
    await userEvent.type(within(form).getByLabelText('Motivo *'), 'Fonte com defeito')
    await userEvent.click(within(form).getByRole('button', { name: 'Registrar' }))

    expect(await screen.findByText('Movimentação registrada: IC21 foi para Assistência técnica.')).toBeInTheDocument()
    await waitFor(() => expect(screen.getByText('Em manutenção', { selector: '.resumo-valor' })).toBeInTheDocument())
    const envio = pedidos.find((p) => p.metodo === 'POST')!
    expect(envio.corpo).toMatchObject({ destino: 'Assistência técnica', motivo: 'Fonte com defeito', tecnico: 'Magno', chamado: '' })
  })

  it('no modo edição, edita a máquina mandando a chave e quem fez', async () => {
    const { pedidos } = servidorFalso(
      rotas({
        ...comModoEdicao(),
        'PUT /api/inventario/IC21': () => ({ corpo: { maquina: maquina({ usuario: 'Diego' }), mudancas: ['Usuário: Magno → Diego'] } }),
      }),
    )
    await abrirIC21()

    await userEvent.click(screen.getByRole('button', { name: 'Editar máquina' }))
    const form = screen.getByRole('form', { name: 'Editar máquina IC21' })
    expect(within(form).queryByLabelText('Nº Semin *')).not.toBeInTheDocument()
    const usuario = within(form).getByLabelText('Usuário')
    await userEvent.clear(usuario)
    await userEvent.type(usuario, 'Diego')
    await userEvent.click(within(form).getByRole('button', { name: 'Salvar alterações' }))

    expect(await screen.findByText('Alterações salvas em IC21: Usuário: Magno → Diego.')).toBeInTheDocument()
    const envio = pedidos.find((p) => p.metodo === 'PUT')!
    expect(envio.cabecalhos['x-chave-edicao']).toBe('chave-do-teste')
    expect(envio.cabecalhos['x-usuario']).toBe('Lúcia')
    expect(envio.corpo).toMatchObject({ nSemin: 'IC21', patrimonio: 'PMS-01350', usuario: 'Diego' })
  })

  it('baixa mostra o erro do servidor (chamado aberto) e, depois, a faixa de máquina baixada', async () => {
    let tentativas = 0
    let atual = maquina()
    servidorFalso(
      rotas({
        ...comModoEdicao(),
        'GET /api/inventario/IC21': () => ({ corpo: ficha(atual) }),
        'POST /api/inventario/IC21/baixa': () => {
          tentativas += 1
          if (tentativas === 1) return { status: 422, corpo: { erros: ['Feche antes o(s) chamado(s) aberto(s) desta máquina: 177456.'] } }
          atual = maquina({ situacao: 'Baixada', baixa: { motivo: 'Descarte', observacao: 'placa queimada', dia: '2026-11-27', quem: 'Lúcia' } })
          return { corpo: atual }
        },
      }),
    )
    await abrirIC21()

    await userEvent.click(screen.getByRole('button', { name: 'Dar baixa' }))
    const form = screen.getByRole('form', { name: 'Dar baixa' })
    await userEvent.type(within(form).getByLabelText('Observação'), 'placa queimada')
    await userEvent.click(within(form).getByRole('button', { name: 'Confirmar baixa' }))
    expect(await within(form).findByRole('alert')).toHaveTextContent('Feche antes o(s) chamado(s) aberto(s) desta máquina: 177456.')

    await userEvent.click(within(form).getByRole('button', { name: 'Confirmar baixa' }))
    expect(await screen.findByText(/Máquina baixada\./)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Desfazer baixa' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Registrar movimentação' })).not.toBeInTheDocument()
  })

  it('a máquina aberta entra em "Vistas recentemente"', async () => {
    servidorFalso(rotas())
    await abrirIC21()

    await userEvent.click(screen.getByRole('button', { name: '← Voltar para o Inventário' }))
    await userEvent.clear(screen.getByLabelText('Procurar máquina'))
    expect(screen.getByRole('button', { name: /IC21\s*IC21 · Computador Dell OptiPlex 7090\s*Inventário · Informática - SMED/ })).toBeInTheDocument()
  })
})
