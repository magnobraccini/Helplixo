import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import App from '../App'
import type { Chamado } from '../api/chamados'
import { ProvedorEdicao } from '../edicao/ContextoEdicao'
import { SAUDE_OK, servidorFalso, type PedidoRecebido } from '../teste/servidorFalso'
import { isoData, paraServidor } from '../util/datas'

const MIN = 60_000
/** Um instante X minutos atrás, no formato do servidor (2026-11-25T09:12:00). */
const haMinutos = (m: number) => `${paraServidor(Date.now() - m * MIN)}:00`

function chamado(parcial: Partial<Chamado>): Chamado {
  return {
    numero: '100', nSemin: 'IC21', setor: 'Tributação - SEFAZ', contato: 'Ana Souza', email: null, problema: 'Impressora',
    descricao: 'A impressora parou.', observacao: null, tecnico: null, area: 'Sup. Técnico', status: 'Aberto',
    urgencia: 'Normal', ocorrencia: 'Defeito', naoComputavel: false, abertoEm: haMinutos(30), andamento: [], fechamento: null,
    ...parcial,
  }
}

const CHAMADOS: Chamado[] = [
  chamado({ numero: '177457', nSemin: 'CC1261', setor: 'Controle Interno - GABINETE', contato: 'Andreia Lopes', problema: 'Sistema da Folha', descricao: 'Configurar o SIGEM.', observacao: 'Sala 301', tecnico: 'Diego', abertoEm: haMinutos(30) }),
  chamado({ numero: '177456', nSemin: 'IM04', contato: 'Ana Souza', abertoEm: haMinutos(150), andamento: [{ texto: 'Pedi o toner.', em: haMinutos(20), tecnico: 'Diego' }] }),
  chamado({ numero: '177455', nSemin: 'CC1261', setor: 'Recursos Humanos - SEAD', contato: 'Bruno Lima', problema: 'Pasta compartilhada', abertoEm: haMinutos(240), naoComputavel: true }),
  chamado({ numero: '177450', nSemin: '', contato: 'Carla Mendes', status: 'Em andamento', tecnico: 'Magno', abertoEm: haMinutos(600) }),
  chamado({ numero: '177440', nSemin: 'EC500', contato: 'Paulo Reis', status: 'Fechado', tecnico: 'Rafael', abertoEm: haMinutos(2000), fechamento: { descricao: 'Troquei o cabo.', em: haMinutos(1955), tecnico: 'Rafael' } }),
]

function abrirApp() {
  return render(
    <ProvedorEdicao>
      <App />
    </ProvedorEdicao>,
  )
}

async function irParaChamados() {
  await userEvent.click(within(screen.getByRole('navigation', { name: 'Seções' })).getByRole('button', { name: /Chamados/ }))
  return screen.findByRole('cell', { name: /Andreia Lopes/ })
}

const linhaDo = (numero: string) => screen.getByRole('button', { name: `Descrição do chamado ${numero}` }).closest('tr')!

beforeEach(() => {
  window.localStorage.clear()
  window.sessionStorage.clear()
})
afterEach(() => vi.unstubAllGlobals())

describe('lista de chamados', () => {
  it('abre nos Abertos dos últimos 30 dias, com contagem por status', async () => {
    const { pedidos } = servidorFalso({ ...SAUDE_OK, 'GET /api/chamados': () => ({ corpo: CHAMADOS }) })
    abrirApp()
    await irParaChamados()

    const abas = screen.getByRole('tablist', { name: 'Status' })
    expect(within(abas).getByRole('tab', { name: /Abertos/ })).toHaveAttribute('aria-selected', 'true')
    expect(within(abas).getByRole('tab', { name: /Abertos/ })).toHaveTextContent('3')
    expect(within(abas).getByRole('tab', { name: /Todos/ })).toHaveTextContent('5')
    expect(screen.queryByRole('cell', { name: 'Carla Mendes' })).not.toBeInTheDocument()
    expect(screen.getByText(/3 chamado\(s\) nos últimos 30 dias · 2 contam nas estatísticas, 1 fora/)).toBeInTheDocument()

    const hoje = new Date()
    const de = isoData(new Date(hoje.getFullYear(), hoje.getMonth(), hoje.getDate() - 29))
    expect(pedidos.some((p: PedidoRecebido) => p.url === `/api/chamados?de=${de}&ate=${isoData(hoje)}`)).toBe(true)
  })

  it('pinta a coluna Min. pela regra e marca ↺ e NC', async () => {
    servidorFalso({ ...SAUDE_OK, 'GET /api/chamados': () => ({ corpo: CHAMADOS }) })
    abrirApp()
    await irParaChamados()

    const minDe = (n: string) => within(linhaDo(n)).getAllByRole('cell').at(-2)!
    // haMinutos corta os segundos, então "30 min atrás" pode dar 30 ou 31.
    expect(minDe('177457')).toHaveTextContent(/^3[01]$/)
    expect(minDe('177457').style.color).toBe('var(--c-2f7a3a)') // < 2 h: verde
    expect(minDe('177456').style.color).toBe('var(--c-b7791f)') // 2–3 h: âmbar
    expect(minDe('177455').style.color).toBe('var(--c-677085)') // não computável: cinza

    // CC1261 aparece em dois chamados dos últimos 7 dias
    expect(within(linhaDo('177457')).getByLabelText('Esta máquina teve 1 outro(s) chamado(s) nos últimos 7 dias')).toHaveTextContent('↺ 1')
    expect(within(linhaDo('177456')).queryByText(/↺/)).not.toBeInTheDocument()
    expect(within(linhaDo('177455')).getByText('NC')).toBeInTheDocument()
  })

  it('filtra por secretaria, técnico e busca', async () => {
    servidorFalso({ ...SAUDE_OK, 'GET /api/chamados': () => ({ corpo: CHAMADOS }) })
    abrirApp()
    await irParaChamados()

    await userEvent.selectOptions(screen.getByLabelText('Secretaria'), 'GABINETE')
    expect(screen.getByRole('cell', { name: 'Andreia Lopes' })).toBeInTheDocument()
    expect(screen.queryByRole('cell', { name: 'Bruno Lima' })).not.toBeInTheDocument()

    await userEvent.selectOptions(screen.getByLabelText('Secretaria'), '')
    await userEvent.selectOptions(screen.getByLabelText('Técnico'), 'Sem técnico')
    expect(screen.queryByRole('cell', { name: 'Andreia Lopes' })).not.toBeInTheDocument()
    expect(screen.getByRole('cell', { name: 'Bruno Lima' })).toBeInTheDocument()

    await userEvent.selectOptions(screen.getByLabelText('Técnico'), 'Todos')
    await userEvent.type(screen.getByLabelText('Procurar'), 'im04')
    expect(screen.getByRole('cell', { name: 'Ana Souza' })).toBeInTheDocument()
    expect(screen.queryByRole('cell', { name: 'Bruno Lima' })).not.toBeInTheDocument()
  })

  it('clicar na linha mostra a descrição, a observação e a última atualização', async () => {
    servidorFalso({ ...SAUDE_OK, 'GET /api/chamados': () => ({ corpo: CHAMADOS }) })
    abrirApp()
    await irParaChamados()

    await userEvent.click(screen.getByRole('cell', { name: 'Ana Souza' }))
    expect(screen.getByRole('button', { name: 'Descrição do chamado 177456' })).toHaveAttribute('aria-expanded', 'true')
    expect(screen.getByText(/Pedi o toner\./)).toBeInTheDocument()

    await userEvent.click(screen.getByRole('cell', { name: 'Andreia Lopes' }))
    expect(screen.getByText('Configurar o SIGEM.')).toBeInTheDocument()
    expect(screen.getByText('Observação: Sala 301')).toBeInTheDocument()
  })

  it('o Nº Semin abre a máquina no Inventário', async () => {
    servidorFalso({ ...SAUDE_OK, 'GET /api/chamados': () => ({ corpo: CHAMADOS }) })
    abrirApp()
    await irParaChamados()

    await userEvent.click(within(linhaDo('177456')).getByRole('button', { name: 'IM04' }))
    expect(screen.getByRole('heading', { level: 1, name: 'Inventário' })).toBeInTheDocument()
    expect(screen.getByText(/Ficha da máquina IM04/)).toBeInTheDocument()
  })
})

describe('ficha do chamado', () => {
  const rotasFicha = (extra = {}) => ({
    ...SAUDE_OK,
    'GET /api/chamados': () => ({ corpo: CHAMADOS }),
    'GET /api/chamados/177457': () => ({ corpo: CHAMADOS[0] }),
    'GET /api/chamados/177455': () => ({ corpo: CHAMADOS[2] }),
    ...extra,
  })

  it('"Ver detalhes" abre a aba Dados, com o caminho no topo', async () => {
    servidorFalso(rotasFicha())
    abrirApp()
    await irParaChamados()

    await userEvent.click(within(linhaDo('177457')).getByRole('button', { name: 'Ver detalhes' }))

    expect(await screen.findByRole('heading', { level: 1, name: 'Chamado 177457 · Sistema da Folha' })).toBeInTheDocument()
    expect(screen.getByRole('tab', { name: 'Dados' })).toHaveAttribute('aria-selected', 'true')
    expect(screen.getByRole('navigation', { name: 'Caminho' })).toHaveTextContent(/Chamados\/\s*177457/)
    expect(screen.getByText('Controle Interno - GABINETE', { selector: 'dd' })).toBeInTheDocument()
  })

  it('"Atualizar" abre o Resultado; fechar com anotação curta mostra o erro do servidor', async () => {
    servidorFalso(
      rotasFicha({
        'POST /api/chamados/177457/resultado': () => ({ status: 422, corpo: { erros: ['Para fechar, descreva o que foi feito (pelo menos 10 letras).'] } }),
      }),
    )
    abrirApp()
    await irParaChamados()

    await userEvent.click(within(linhaDo('177457')).getByRole('button', { name: 'Atualizar' }))
    const form = await screen.findByRole('form', { name: 'Resultado do chamado' })
    expect(screen.getByRole('tab', { name: 'Resultado' })).toHaveAttribute('aria-selected', 'true')

    await userEvent.selectOptions(within(form).getByLabelText('Status *'), 'Fechado')
    expect(within(form).getByLabelText('O que foi feito *')).toBeInTheDocument()
    await userEvent.type(within(form).getByLabelText('O que foi feito *'), 'ok')
    await userEvent.click(within(form).getByRole('button', { name: 'Salvar e fechar' }))

    expect(await within(form).findByRole('alert')).toHaveTextContent('Para fechar, descreva o que foi feito (pelo menos 10 letras).')
  })

  it('fecha o chamado: manda o formulário ao servidor e atualiza a ficha e a lista', async () => {
    const fechado: Chamado = {
      ...CHAMADOS[0],
      status: 'Fechado',
      fechamento: { descricao: 'Configurei o SIGEM e testei.', em: haMinutos(0), tecnico: 'Diego' },
    }
    // O servidor de mentira lembra do fechamento, como o de verdade.
    let lista = CHAMADOS
    const { pedidos } = servidorFalso(
      rotasFicha({
        'GET /api/chamados': () => ({ corpo: lista }),
        'POST /api/chamados/177457/resultado': () => {
          lista = CHAMADOS.map((c) => (c.numero === '177457' ? fechado : c))
          return { corpo: { chamado: fechado, avisos: ['Fora do expediente (segunda a sexta, 8h às 14h). Confira se está certo.'] } }
        },
      }),
    )
    abrirApp()
    await irParaChamados()

    await userEvent.click(within(linhaDo('177457')).getByRole('button', { name: 'Atualizar' }))
    const form = await screen.findByRole('form', { name: 'Resultado do chamado' })
    await userEvent.selectOptions(within(form).getByLabelText('Status *'), 'Fechado')
    await userEvent.type(within(form).getByLabelText('O que foi feito *'), 'Configurei o SIGEM e testei.')
    expect(within(form).getByLabelText('Técnico que realizou *')).toHaveValue('Diego')
    await userEvent.click(within(form).getByRole('button', { name: 'Salvar e fechar' }))

    expect(await screen.findByText(/Chamado 177457 fechado\. Fora do expediente/)).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Atendido' })).toBeInTheDocument()

    const envio = pedidos.find((p) => p.metodo === 'POST')!
    expect(envio.url).toBe('/api/chamados/177457/resultado')
    expect(envio.corpo).toMatchObject({ status: 'Fechado', anotacao: 'Configurei o SIGEM e testei.', tecnico: 'Diego' })
    expect((envio.corpo as { quando: string }).quando).toMatch(/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/)

    await userEvent.click(screen.getByRole('button', { name: '← Voltar para Chamados' }))
    await userEvent.click(screen.getByRole('tab', { name: /Fechados/ }))
    expect(await screen.findByRole('cell', { name: 'Andreia Lopes' })).toBeInTheDocument()
  })

  it('a conferência do horário avisa quando a hora está antes da abertura', async () => {
    servidorFalso(rotasFicha())
    abrirApp()
    await irParaChamados()

    await userEvent.click(within(linhaDo('177457')).getByRole('button', { name: 'Atualizar' }))
    const form = await screen.findByRole('form', { name: 'Resultado do chamado' })
    const conferencia = within(form).getByRole('group', { name: 'Conferência do horário' })
    expect(conferencia).toHaveTextContent('Não está no futuro.')

    const umDiaAntes = new Date(Date.now() - 24 * 60 * MIN)
    const data = within(form).getByLabelText('Data *')
    await userEvent.clear(data)
    await userEvent.type(data, isoData(umDiaAntes))
    expect(conferencia).toHaveTextContent('É antes da abertura do chamado')
  })

  it('trocar de seção e voltar mantém o chamado, a aba e o que foi digitado', async () => {
    servidorFalso(rotasFicha())
    abrirApp()
    await irParaChamados()

    await userEvent.click(within(linhaDo('177457')).getByRole('button', { name: 'Atualizar' }))
    const form = await screen.findByRole('form', { name: 'Resultado do chamado' })
    await userEvent.type(within(form).getByLabelText('Anotação (opcional)'), 'Aguardando o usuário')

    const menu = screen.getByRole('navigation', { name: 'Seções' })
    await userEvent.click(within(menu).getByRole('button', { name: /Início/ }))
    expect(screen.queryByRole('form', { name: 'Resultado do chamado' })).not.toBeInTheDocument()
    await userEvent.click(within(menu).getByRole('button', { name: /Chamados/ }))

    expect(screen.getByRole('tab', { name: 'Resultado' })).toHaveAttribute('aria-selected', 'true')
    expect(screen.getByLabelText('Anotação (opcional)')).toHaveValue('Aguardando o usuário')
  })

  it('Histórico da máquina mostra só os outros registros e abre o chamado ligado', async () => {
    servidorFalso(
      rotasFicha({
        'GET /api/inventario/CC1261': () => ({
          corpo: {
            maquina: { nSemin: 'CC1261', patrimonio: '1', tipo: 'Computador', modelo: 'X', local: 'GABINETE', usuario: 'Andreia', situacao: 'Em uso' },
            historico: [
              { quando: haMinutos(30), tipo: 'Chamado', texto: 'Sistema da Folha (aberto)', referencia: '177457', quem: 'Diego' },
              { quando: haMinutos(240), tipo: 'Chamado', texto: 'Pasta compartilhada (aberto)', referencia: '177455', quem: null },
              { quando: '2026-03-10T00:00:00', tipo: 'Conserto', texto: 'Fonte queimada → trocada (assistência)', referencia: null, quem: null },
            ],
          },
        }),
      }),
    )
    abrirApp()
    await irParaChamados()

    await userEvent.click(within(linhaDo('177457')).getByRole('button', { name: 'Ver detalhes' }))
    await userEvent.click(await screen.findByRole('tab', { name: 'Histórico da máquina' }))

    expect(await screen.findByText('1 outro(s) chamado(s), 1 conserto(s), 0 movimentação(ões).')).toBeInTheDocument()
    expect(screen.queryByText('Sistema da Folha (aberto)')).not.toBeInTheDocument()
    expect(screen.getByText('Fonte queimada → trocada (assistência)')).toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: 'Nº 177455' }))
    await waitFor(() => expect(screen.getByRole('heading', { level: 1, name: 'Chamado 177455 · Pasta compartilhada' })).toBeInTheDocument())
    expect(screen.getByRole('tab', { name: 'Dados' })).toHaveAttribute('aria-selected', 'true')
  })
})
