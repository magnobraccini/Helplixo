// Paletas da capa ilustrada, copiadas da maquete (renderVals -> temas).
// Regra de negócio: no tema escuro, a capa usa a versão noturna.

export type Cena = 'inicio' | 'chamados' | 'inventario' | 'processos' | 'contatos' | 'calendario' | 'ferramentas' | 'avisos'

export type CapaPaleta = {
  bg: string
  card: string
  blobA: string
  blobB: string
  sage: string
  skin: string
  mask: string
  ink: string
  text: string
  sub: string
}

export const PALETAS_CAPA = {
  papel: { bg: '#FBF1E1', card: '#FFFDF8', blobA: '#F2C46D', blobB: '#EE8F6F', sage: '#A9C5A0', skin: '#C68B59', mask: '#F7DFC3', ink: '#2E2A25', text: '#2E2A25', sub: '#5C5248' },
  menta: { bg: '#E7F2EC', card: '#FBFFFD', blobA: '#9FD3BE', blobB: '#F4A98C', sage: '#6FAF8E', skin: '#B98560', mask: '#F6E1CB', ink: '#23302A', text: '#1F2B25', sub: '#475A50' },
  noite: { bg: '#1B1A18', card: '#262420', blobA: '#3A3A2E', blobB: '#A8644D', sage: '#33463D', skin: '#9C6B45', mask: '#DCC4A3', ink: '#B9AF9C', text: '#E3E2DF', sub: '#A39C90' },
} satisfies Record<string, CapaPaleta>
