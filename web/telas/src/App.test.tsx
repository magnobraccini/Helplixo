import { render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import App from './App'

describe('App', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('mostra que o servidor está no ar quando /api/saude responde', async () => {
    const fetchFalso = vi.fn().mockResolvedValue(
      new Response(JSON.stringify({ status: 'ok', sistema: 'Helplixo Web', agora: '2026-11-26T12:00' })),
    )
    vi.stubGlobal('fetch', fetchFalso)

    render(<App />)

    expect(screen.getByRole('heading', { name: 'Helplixo Web' })).toBeInTheDocument()
    expect(await screen.findByText('Servidor no ar.')).toBeInTheDocument()
    expect(fetchFalso).toHaveBeenCalledWith('/api/saude')
  })

  it('avisa quando o servidor não responde', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('Failed to fetch')))

    render(<App />)

    expect(await screen.findByText(/Servidor fora do ar/)).toBeInTheDocument()
  })
})
