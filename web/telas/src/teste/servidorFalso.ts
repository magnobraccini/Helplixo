import { vi } from 'vitest'

// Um "servidor de mentira" para os testes de tela: responde por endereço e guarda os pedidos recebidos.

export type PedidoRecebido = { url: string; metodo: string; cabecalhos: Record<string, string>; corpo: unknown }
type Resposta = { status?: number; corpo?: unknown }
type Rota = (pedido: PedidoRecebido) => Resposta

export function servidorFalso(rotas: Record<string, Rota>) {
  const pedidos: PedidoRecebido[] = []
  const fetchFalso = vi.fn(async (entrada: RequestInfo | URL, opcoes?: RequestInit) => {
    const url = String(entrada)
    const metodo = opcoes?.method ?? 'GET'
    const pedido: PedidoRecebido = {
      url,
      metodo,
      cabecalhos: Object.fromEntries(new Headers(opcoes?.headers).entries()),
      corpo: typeof opcoes?.body === 'string' ? JSON.parse(opcoes.body) : undefined,
    }
    pedidos.push(pedido)
    const rota = rotas[`${metodo} ${url}`]
    if (!rota) return new Response(JSON.stringify({ erros: ['rota não prevista no teste'] }), { status: 404 })
    const { status = 200, corpo } = rota(pedido)
    return new Response(corpo === undefined ? null : JSON.stringify(corpo), { status })
  })
  vi.stubGlobal('fetch', fetchFalso)
  return { pedidos }
}

export const SAUDE_OK: Record<string, Rota> = {
  'GET /api/saude': () => ({ corpo: { status: 'ok', sistema: 'Helplixo Web', agora: '2026-11-26T10:00' } }),
}
