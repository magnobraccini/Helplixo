import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import App from './App'
import { ProvedorEdicao } from './edicao/ContextoEdicao'
import { SAUDE_OK, servidorFalso } from './teste/servidorFalso'

// Senha fictícia, só para teste. A senha real fica no servidor (HELPLIXO_SENHA_EDICAO).
const SENHA_DO_TESTE = 'senha-do-teste-unitario'

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
  delete document.documentElement.dataset.tema
})

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('menu lateral e capa', () => {
  it('mostra as 7 seções com emoji e começa no Início', () => {
    servidorFalso(SAUDE_OK)
    abrirApp()

    const menu = screen.getByRole('navigation', { name: 'Seções' })
    const rotulos = ['Início', 'Chamados', 'Inventário', 'Processos', 'Contatos', 'Calendário', 'Ferramentas']
    for (const r of rotulos) expect(within(menu).getByRole('button', { name: new RegExp(r) })).toBeInTheDocument()
    expect(within(menu).getByRole('button', { name: /Início/ })).toHaveAttribute('aria-current', 'page')
    expect(within(menu).getByRole('button', { name: /Chamados/ })).toHaveTextContent('🎫')
    expect(screen.getByRole('heading', { level: 1, name: 'Início' })).toBeInTheDocument()
  })

  it('ao trocar de seção, mudam o título, o caminho, a capa e o item marcado', async () => {
    servidorFalso(SAUDE_OK)
    const { container } = abrirApp()

    await userEvent.click(screen.getByRole('button', { name: /Chamados/ }))

    expect(screen.getByRole('heading', { level: 1, name: 'Chamados' })).toBeInTheDocument()
    expect(screen.getByText('Pedidos de suporte por período, secretaria e técnico.')).toBeInTheDocument()
    expect(screen.getByRole('navigation', { name: 'Caminho' })).toHaveTextContent('Chamados')
    expect(container.querySelector('.capa')).toHaveAttribute('data-cena', 'chamados')
    expect(screen.getByRole('button', { name: /Chamados/ })).toHaveAttribute('aria-current', 'page')
  })

  it('o topo mostra "Você: <perfil>" e o sino abre as notificações', async () => {
    servidorFalso(SAUDE_OK)
    abrirApp()

    expect(screen.getByText('Você: Triagem')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Notificações' }))
    expect(screen.getByRole('dialog', { name: 'Notificações' })).toBeInTheDocument()
    await userEvent.keyboard('{Escape}')
    expect(screen.queryByRole('dialog', { name: 'Notificações' })).not.toBeInTheDocument()
  })

  it('avisa quando o servidor não responde', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('Failed to fetch')))
    abrirApp()

    expect(await screen.findByRole('alert')).toHaveTextContent('Servidor fora do ar')
  })
})

describe('tema', () => {
  it('começa claro e, ao escolher Escuro, troca a página e a capa e guarda a escolha', async () => {
    servidorFalso(SAUDE_OK)
    const { container } = abrirApp()
    expect(document.documentElement.dataset.tema).toBe('claro')

    await userEvent.click(screen.getByRole('button', { name: /Configurações/ }))
    await userEvent.click(screen.getByRole('radio', { name: /Escuro/ }))

    expect(document.documentElement.dataset.tema).toBe('escuro')
    expect(window.localStorage.getItem('helplixo-tema')).toBe('escuro')
    expect(screen.getByRole('radio', { name: /Escuro/ })).toHaveAttribute('aria-checked', 'true')
    // Regra: no tema escuro, a capa usa a versão noturna.
    expect(container.querySelector<HTMLElement>('.capa')!.style.background).toBe('rgb(27, 26, 24)')
  })

  it('"Igual ao sistema" segue o Windows', async () => {
    vi.stubGlobal('matchMedia', (consulta: string) => ({
      matches: consulta === '(prefers-color-scheme: dark)',
      addEventListener: () => {},
      removeEventListener: () => {},
    }))
    window.localStorage.setItem('helplixo-tema', 'sistema')
    servidorFalso(SAUDE_OK)
    abrirApp()

    expect(document.documentElement.dataset.tema).toBe('escuro')
  })
})

describe('modo edição', () => {
  async function abrirSenha() {
    await userEvent.click(screen.getByRole('button', { name: /Configurações/ }))
    await userEvent.click(screen.getByRole('switch', { name: 'Modo edição' }))
    return screen.getByRole('dialog', { name: 'Entrar no modo edição' })
  }

  it('pede a senha e não chama o servidor se ela estiver vazia', async () => {
    const { pedidos } = servidorFalso(SAUDE_OK)
    abrirApp()

    const dialogo = await abrirSenha()
    await userEvent.click(within(dialogo).getByRole('button', { name: 'Entrar' }))

    expect(within(dialogo).getByRole('alert')).toHaveTextContent('Digite a senha de edição.')
    expect(pedidos.some((p) => p.url === '/api/edicao/entrar')).toBe(false)
  })

  it('mostra a mensagem do servidor quando a senha está errada', async () => {
    servidorFalso({
      ...SAUDE_OK,
      'POST /api/edicao/entrar': () => ({ status: 401, corpo: { erros: ['Senha incorreta. Confira e tente de novo.'] } }),
    })
    abrirApp()

    const dialogo = await abrirSenha()
    await userEvent.type(within(dialogo).getByLabelText('Senha de edição'), 'errada')
    await userEvent.click(within(dialogo).getByRole('button', { name: 'Entrar' }))

    expect(await within(dialogo).findByRole('alert')).toHaveTextContent('Senha incorreta. Confira e tente de novo.')
    expect(within(dialogo).getByLabelText('Senha de edição')).toHaveValue('')
    expect(screen.queryByText('Editando · salva para todos')).not.toBeInTheDocument()
  })

  it('com a senha certa, liga o modo edição; ao desligar, avisa o servidor com a chave', async () => {
    const { pedidos } = servidorFalso({
      ...SAUDE_OK,
      'POST /api/edicao/entrar': () => ({ corpo: { chave: 'chave-do-teste', validaAte: '2999-01-01T00:00:00Z' } }),
      'POST /api/edicao/sair': () => ({ status: 204 }),
    })
    abrirApp()

    const dialogo = await abrirSenha()
    await userEvent.type(within(dialogo).getByLabelText('Senha de edição'), SENHA_DO_TESTE)
    await userEvent.click(within(dialogo).getByRole('button', { name: 'Entrar' }))

    expect(await screen.findByText('Editando · salva para todos')).toBeInTheDocument()
    expect(screen.getByText('Modo edição ligado. As alterações valem para todos.')).toBeInTheDocument()
    expect(pedidos.find((p) => p.url === '/api/edicao/entrar')?.corpo).toEqual({ senha: SENHA_DO_TESTE })
    // A senha nunca fica guardada no navegador; só a chave que o servidor deu.
    expect(JSON.stringify(window.sessionStorage)).not.toContain(SENHA_DO_TESTE)
    expect(JSON.stringify(window.localStorage)).not.toContain(SENHA_DO_TESTE)

    await userEvent.click(screen.getByRole('button', { name: /Configurações/ }))
    const interruptor = screen.getByRole('switch', { name: 'Modo edição' })
    expect(interruptor).toHaveAttribute('aria-checked', 'true')
    await userEvent.click(interruptor)

    await waitFor(() => expect(screen.queryByText('Editando · salva para todos')).not.toBeInTheDocument())
    const saida = pedidos.find((p) => p.url === '/api/edicao/sair')
    expect(saida?.cabecalhos['x-chave-edicao']).toBe('chave-do-teste')
  })

  it('desliga sozinho quando a chave de edição vence', async () => {
    const venceLogo = new Date(Date.now() + 300).toISOString()
    window.sessionStorage.setItem('helplixo-chave-edicao', JSON.stringify({ chave: 'chave-curta', validaAte: venceLogo }))
    servidorFalso({
      ...SAUDE_OK,
      'GET /api/edicao': () => ({ corpo: { configurado: true, ligado: true, validaAte: venceLogo } }),
    })
    abrirApp()

    expect(screen.getByText('Editando · salva para todos')).toBeInTheDocument()
    await waitFor(() => expect(screen.queryByText('Editando · salva para todos')).not.toBeInTheDocument(), { timeout: 2000 })
  })

  it('ao abrir a página, descarta a chave guardada se o servidor disser que ela não vale mais', async () => {
    window.sessionStorage.setItem('helplixo-chave-edicao', JSON.stringify({ chave: 'chave-velha', validaAte: '2999-01-01T00:00:00Z' }))
    servidorFalso({
      ...SAUDE_OK,
      'GET /api/edicao': () => ({ corpo: { configurado: true, ligado: false, validaAte: null } }),
    })
    abrirApp()

    expect(screen.getByText('Editando · salva para todos')).toBeInTheDocument()
    await waitFor(() => expect(screen.queryByText('Editando · salva para todos')).not.toBeInTheDocument())
    expect(window.sessionStorage.getItem('helplixo-chave-edicao')).toBeNull()
  })
})
