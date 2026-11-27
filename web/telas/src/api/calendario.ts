import { pedir } from './cliente'
import { CABECALHO_CHAVE } from './edicao'

// Contrato de web/servidor/.../calendario/CalendarioController.java (GET /api/calendario)

export type Ocorrencia = {
  id: string
  /** 2026-11-27 */
  dia: string
  titulo: string
  tipo: string
  /** 09:00:00 ou null (dia inteiro) */
  horaInicio: string | null
  horaFim: string | null
  diaInteiro: boolean
  descricao: string | null
  repeteTexto: string | null
  autor: string | null
  cargo: string | null
  urgente: boolean
  /** Feriados, contratos e garantias: entram sozinhos. */
  automatica: boolean
}

/** Ocorrências dia a dia entre `de` e `ate` (datas 2026-11-27). `tipos` filtra por tipo. */
export function listarOcorrencias(de: string, ate: string, tipos: string[] = []): Promise<Ocorrencia[]> {
  const q = new URLSearchParams({ de, ate })
  if (tipos.length) q.set('tipos', tipos.join(','))
  return pedir<Ocorrencia[]>(`/api/calendario?${q}`)
}

export type PedidoMarcacao = {
  titulo: string
  tipo: string
  inicio: string
  fim: string | null
  horaInicio: string | null
  horaFim: string | null
  /** '', 'semanal', 'mensal' ou 'anual' */
  repete: string
  repeteAte: string | null
  /** '', 'dia', '30', '60' ou '1440' */
  lembrete: string
  descricao: string
  urgente: boolean
}
export type Criada = { marcacao: { id: string; titulo: string }; avisos: string[] }
export type Lembrete = { id: string; texto: string; inicio: string; faltaMinutos: number }

/** Quem marca: até existir o login, nome e cargo vão nos cabeçalhos; a chave libera os tipos além de Aviso. */
export type QuemMarca = { usuario: string; cargo: string; chave: string | null }
const cabecalhos = (q: QuemMarca): Record<string, string> => ({
  'X-Usuario': q.usuario,
  'X-Cargo': q.cargo,
  ...(q.chave ? { [CABECALHO_CHAVE]: q.chave } : {}),
})

export const criarMarcacao = (p: PedidoMarcacao, q: QuemMarca) =>
  pedir<Criada>('/api/calendario', { method: 'POST', headers: { 'Content-Type': 'application/json', ...cabecalhos(q) }, body: JSON.stringify(p) })
export const removerMarcacao = (id: string, q: QuemMarca) =>
  pedir<void>(`/api/calendario/${encodeURIComponent(id)}`, { method: 'DELETE', headers: cabecalhos(q) })
export const listarLembretes = () => pedir<Lembrete[]>('/api/calendario/lembretes')

/** Cores por tipo de marcação, copiadas da maquete (tipoCor): [fundo, traço]. */
export const CORES_TIPO: Record<string, [string, string]> = {
  'Aviso': ['var(--c-fcebdd)', 'var(--c-9a3f0e)'],
  'Manutenção': ['var(--c-fbf3db)', 'var(--c-8a5a00)'],
  'Reunião': ['var(--c-edf3ec)', 'var(--c-2f6a2b)'],
  'Ausência': ['var(--c-f4f0f7)', 'var(--c-6a3fa0)'],
  'Aniversário': ['var(--c-fbecdd)', 'var(--c-a0521a)'],
  'Feriado': ['var(--c-fdebec)', 'var(--c-b4231f)'],
  'Contrato': ['var(--c-e7f3f8)', 'var(--c-1d5f88)'],
  'Garantia': ['var(--c-eef1f5)', 'var(--c-55534e)'],
  'Outro': ['var(--c-eef1f5)', 'var(--c-55534e)'],
}
export const corDoTipo = (tipo: string) => (CORES_TIPO[tipo] ?? CORES_TIPO.Outro)[1]
