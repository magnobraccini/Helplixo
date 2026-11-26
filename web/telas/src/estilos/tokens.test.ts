import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

// Garante que os tokens de cor continuam IDÊNTICOS aos da maquete (linhas 14 e 15).
// Se alguém mexer numa cor por engano, este teste avisa.

const raiz = resolve(import.meta.dirname, '../..')
const sem = (texto: string) => texto.replace(/\s+/g, '')

describe('tokens de cor', () => {
  it('são iguais aos da maquete, no tema claro e no escuro', () => {
    const maquete = readFileSync(resolve(raiz, '../docs/maquete-v32.dc.html'), 'utf8').split(/\r?\n/)
    const tokens = readFileSync(resolve(raiz, 'src/estilos/tokens.css'), 'utf8').replace(/\/\*[\s\S]*?\*\//g, '')

    expect(maquete[13].startsWith(':root{')).toBe(true)
    expect(maquete[14].startsWith('[data-tema="escuro"]{')).toBe(true)
    expect(sem(tokens)).toBe(sem(maquete[13] + maquete[14]))
  })

  it('têm as cores principais do tema creme e do escuro', () => {
    const tokens = readFileSync(resolve(raiz, 'src/estilos/tokens.css'), 'utf8')
    for (const cor of ['--fundo:#FAF6EE', '--lateral:#F3EEE3', '--c-ffffff:#FFFDF8', '--c-1e2430:#37352F', '--c-0f6e66:#2383E2', '--fundo:#191919', '--c-1e2430:#E3E2DF']) {
      expect(tokens).toContain(cor)
    }
  })
})
