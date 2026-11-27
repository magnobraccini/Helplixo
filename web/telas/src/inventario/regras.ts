// Listas e cores do Inventário, copiadas da maquete e de web/docs/regras-de-negocio.md.
import type { Cores } from '../chamados/regras'

/** Setores da prefeitura (maquete: setores()). Provisório até existir o cadastro de setores no servidor. */
export const SETORES = [
  'Controle Interno - GABINETE',
  'Gabinete - GABINETE',
  'Tributação - SEFAZ',
  'Recursos Humanos - SEAD',
  'Protocolo - SEAD',
  'Informática - SMED',
  'Diretoria Obras - SEMOB',
]
/** Onde uma máquina pode estar: os setores, o depósito e a assistência. */
export const LOCAIS = [...SETORES, 'Depósito da TI', 'Assistência técnica']

/** Iguais às do servidor (InventarioServico.TIPOS e MOTIVOS_BAIXA). */
export const TIPOS = ['Computador', 'Notebook', 'Impressora', 'Monitor', 'Telefone IP', 'Nobreak', 'Outro']
export const MOTIVOS_BAIXA = ['Descarte', 'Doação', 'Leilão', 'Furto ou perda', 'Outro']

const VERDE: Cores = { fundo: 'var(--c-edf3ec)', texto: 'var(--c-2f5a2b)' }
const AMBAR: Cores = { fundo: 'var(--c-fbf3db)', texto: 'var(--c-6b4f0e)' }
const CINZA: Cores = { fundo: 'var(--c-eef1f5)', texto: 'var(--c-3f4757)' }
const APAGADO: Cores = { fundo: 'var(--c-f3f5f8)', texto: 'var(--c-677085)' }

export function coresOffice(situacao: string): Cores {
  if (situacao === 'sim') return VERDE
  if (situacao.startsWith('sim')) return AMBAR
  if (situacao === 'não') return CINZA
  return APAGADO
}

export function coresGarantia(situacao: string): Cores {
  if (situacao === 'vigente') return VERDE
  if (situacao === 'vence') return AMBAR
  if (situacao === 'vencida') return CINZA
  return APAGADO
}

const CORES_SITUACAO: Record<string, string> = {
  'Em uso': 'var(--c-2f5a2b)',
  'Em manutenção': 'var(--c-6b4f0e)',
  'Reserva': 'var(--c-1d4f68)',
  'Aguardando baixa': 'var(--c-3f4757)',
  'Baixada': 'var(--c-8a2a27)',
}
export const corSituacao = (s: string) => CORES_SITUACAO[s] ?? 'var(--c-1e2430)'

const CORES_HISTORICO: Record<string, Cores> = {
  'Conserto': AMBAR,
  'Chamado': { fundo: 'var(--c-e7f3f8)', texto: 'var(--c-1d4f68)' },
  'Movimentação': { fundo: 'var(--c-f4f0f7)', texto: 'var(--c-5a3d78)' },
  'Baixa': { fundo: 'var(--c-fdebec)', texto: 'var(--c-8a2a27)' },
}
export const coresHistorico = (tipo: string): Cores => CORES_HISTORICO[tipo] ?? { fundo: 'var(--c-eef1f5)', texto: 'var(--c-4a5263)' }

/** Resumo da tela de busca: "14 máquinas no inventário (6 computador, 2 impressora…)." */
export function textoResumo(total: number, porTipo: Record<string, number>): string {
  const partes = Object.entries(porTipo).map(([tipo, n]) => `${n} ${tipo.toLowerCase()}`)
  return `${total} máquinas no inventário (${partes.join(', ')}). Digite pelo menos 2 letras para procurar.`
}

/** "2023-03-14" → "14/03/2023" */
export const diaBr = (iso: string | null | undefined) => (iso ? iso.slice(0, 10).split('-').reverse().join('/') : '—')
