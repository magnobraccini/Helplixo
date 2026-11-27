import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import App from '../App'
import type { ListaExternos, ListaInternos } from '../api/contatos'
import { ProvedorEdicao } from '../edicao/ContextoEdicao'
import { SAUDE_OK, servidorFalso, type PedidoRecebido } from '../teste/servidorFalso'

const INTERNOS: ListaInternos = {
  contatos: [
    { nome: 'Ana Souza', setor: 'Tributação - SEFAZ', andar: '2º', ramal: '2214', email: 'ana.souza@exemplo.gov.br' },
    { nome: 'Bruno Lima', setor: 'Recursos Humanos - SEAD', andar: '1º', ramal: '2120', email: null },
  ],
  total: 2, deTotal: 11, opcoes: { secretarias: ['SEAD', 'SEFAZ'], andares: ['1º', '2º'] },
}
const EXTERNOS: ListaExternos = {
  contatos: [
    { contato: { categoria: 'Sistemas', empresa: 'SoftGov Sistemas', servico: 'Sistema de protocolo', contato: 'Atendimento', telefone: '(11) 3333-2000', email: null, contrato: '078/2023', vence: '2026-11-10' }, situacao: 'vencido', texto: 'Venceu em 10/11/2026', dias: -17 },
    { contato: { categoria: 'Impressão', empresa: 'Printer Center', servico: 'Impressoras alugadas', contato: null, telefone: null, email: 'suporte@printer.exemplo', contrato: '045/2025', vence: '2026-12-15' }, situacao: 'vence', texto: 'Vence em 18 dias', dias: 18 },
  ],
  total: 2, deTotal: 6, categorias: ['Impressão', 'Sistemas'],
}

const parametro = (p: PedidoRecebido, nome: string) => new URL(p.url, 'http://x').searchParams.get(nome)

function rotas(extra = {}) {
  return {
    ...SAUDE_OK,
    'GET /api/chamados': () => ({ corpo: [] }),
    'GET /api/calendario': () => ({ corpo: [] }),
    'GET /api/contatos/internos': () => ({ corpo: INTERNOS }),
    'GET /api/contatos/externos': () => ({ corpo: EXTERNOS }),
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
async function irParaContatos() {
  await userEvent.click(within(screen.getByRole('navigation', { name: 'Seções' })).getByRole('button', { name: /Contatos/ }))
  return screen.findByRole('cell', { name: 'Ana Souza' })
}

beforeEach(() => {
  window.localStorage.clear()
  window.sessionStorage.clear()
})
afterEach(() => vi.unstubAllGlobals())

describe('contatos', () => {
  it('abre nos internos, com contagem nas duas abas e o resumo', async () => {
    servidorFalso(rotas())
    abrirApp()
    await irParaContatos()

    expect(screen.getByRole('tab', { name: /Internos · ramais\s*11/ })).toHaveAttribute('aria-selected', 'true')
    expect(screen.getByRole('tab', { name: /Externos\s*6/ })).toBeInTheDocument()
    expect(screen.getByText('2 de 11 ramais')).toBeInTheDocument()
    expect(screen.getByRole('cell', { name: '2214' })).toBeInTheDocument()
  })

  it('os filtros dos internos vão para o servidor, com as opções que ele mandou', async () => {
    const { pedidos } = servidorFalso(rotas())
    abrirApp()
    await irParaContatos()

    // A lista de Chamados (escondida) também tem "Secretaria": procuro só nos filtros de Contatos.
    const filtros = screen.getByRole('group', { name: 'Filtros' })
    await userEvent.selectOptions(within(filtros).getByLabelText('Secretaria'), 'SEAD')
    await userEvent.selectOptions(within(filtros).getByLabelText('Ordenar por'), 'Ramal')
    const ultimo = pedidos.filter((p) => p.url.startsWith('/api/contatos/internos')).at(-1)!
    expect(parametro(ultimo, 'secretaria')).toBe('SEAD')
    expect(parametro(ultimo, 'ordem')).toBe('ramal')

    await userEvent.click(screen.getByRole('button', { name: 'Limpar filtros' }))
    expect(within(filtros).getByLabelText('Secretaria')).toHaveValue('')
  })

  it('externos: situação do contrato com cor, e filtro de contrato', async () => {
    const { pedidos } = servidorFalso(rotas())
    abrirApp()
    await irParaContatos()

    await userEvent.click(screen.getByRole('tab', { name: /Externos/ }))
    expect(screen.getByText('Venceu em 10/11/2026').style.color).toBe('var(--c-8a2a27)')
    expect(screen.getByText('Vence em 18 dias').style.color).toBe('var(--c-6b4f0e)')
    expect(screen.getByText('nº 078/2023')).toBeInTheDocument()

    await userEvent.selectOptions(screen.getByLabelText('Contrato'), 'Vencido')
    expect(parametro(pedidos.filter((p) => p.url.startsWith('/api/contatos/externos')).at(-1)!, 'situacao')).toBe('vencido')
  })

  it('"Novo contato" só aparece no modo edição; ramal repetido salva e avisa com quem divide', async () => {
    window.sessionStorage.setItem('helplixo-chave-edicao', JSON.stringify({ chave: 'chave-do-teste', validaAte: '2999-01-01T00:00:00Z' }))
    const { pedidos } = servidorFalso(
      rotas({
        'GET /api/edicao': () => ({ corpo: { configurado: true, ligado: true, validaAte: '2999-01-01T00:00:00Z' } }),
        'POST /api/contatos/internos': () => ({
          status: 201,
          corpo: { contato: { nome: 'Recepção', setor: 'Tributação - SEFAZ', andar: '2º', ramal: '2214', email: null }, compartilhadoCom: ['Ana Souza'] },
        }),
      }),
    )
    abrirApp()
    await irParaContatos()

    await userEvent.click(screen.getByRole('button', { name: 'Novo contato' }))
    const form = screen.getByRole('form', { name: 'Novo contato' })
    await userEvent.type(within(form).getByLabelText('Nome *'), 'Recepção')
    await userEvent.selectOptions(within(form).getByLabelText('Setor *'), 'Tributação - SEFAZ')
    await userEvent.type(within(form).getByLabelText('Ramal *'), '2214')
    await userEvent.click(within(form).getByRole('button', { name: 'Salvar contato' }))

    expect(await screen.findByText('Contato salvo: Recepção · ramal 2214 (compartilhado com Ana Souza).')).toBeInTheDocument()
    const envio = pedidos.find((p) => p.metodo === 'POST')!
    expect(envio.cabecalhos['x-chave-edicao']).toBe('chave-do-teste')
    expect(envio.corpo).toMatchObject({ nome: 'Recepção', ramal: '2214', andar: null })
    // a lista é pedida de novo, para o contato novo aparecer na hora
    expect(pedidos.filter((p) => p.url.startsWith('/api/contatos/internos')).length).toBeGreaterThan(1)
  })

  it('sem modo edição não há "Novo contato"', async () => {
    servidorFalso(rotas())
    abrirApp()
    await irParaContatos()
    expect(screen.queryByRole('button', { name: 'Novo contato' })).not.toBeInTheDocument()
  })
})
