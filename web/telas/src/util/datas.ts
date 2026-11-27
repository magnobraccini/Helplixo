// Ajudantes de data e hora, iguais aos da maquete (pad, isoData, isoHora, fmt, fmtData, duracao, idade).
// O servidor manda datas sem fuso ("2026-11-25T09:12:00"): o navegador lê como hora local, que é o que queremos.

export const MINUTO = 60_000
export const DIA = 86_400_000

export const pad = (n: number) => String(n).padStart(2, '0')

/** 2026-11-25 */
export const isoData = (d: Date) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
/** 09:12 */
export const isoHora = (d: Date) => `${pad(d.getHours())}:${pad(d.getMinutes())}`
/** 25/11 às 09:12 */
export const fmt = (ms: number) => {
  const d = new Date(ms)
  return `${pad(d.getDate())}/${pad(d.getMonth() + 1)} às ${isoHora(d)}`
}
/** 25/11/2026 */
export const fmtData = (ms: number) => {
  const d = new Date(ms)
  return `${pad(d.getDate())}/${pad(d.getMonth() + 1)}/${d.getFullYear()}`
}

/** "45 min", "2 h 05 min", "3 dias 4 h" */
export function duracao(ms: number): string {
  const min = Math.max(0, Math.round(ms / MINUTO))
  const d = Math.floor(min / 1440)
  const h = Math.floor((min % 1440) / 60)
  const m = min % 60
  if (d > 0) return `${d}${d === 1 ? ' dia ' : ' dias '}${h} h`
  if (h > 0) return `${h} h ${pad(m)} min`
  return `${m} min`
}

/** "agora", "há 12 min", "há 3 h", "ontem", "23/11" */
export function idade(ms: number, agora: number): string {
  const min = Math.round((agora - ms) / MINUTO)
  if (min < 1) return 'agora'
  if (min < 60) return `há ${min} min`
  if (min < 60 * 24) return `há ${Math.round(min / 60)} h`
  if (min < 60 * 48) return 'ontem'
  const d = new Date(ms)
  return `${pad(d.getDate())}/${pad(d.getMonth() + 1)}`
}

/** Junta os campos de data (2026-11-25) e hora (09:12) num instante local, ou null se faltar algo. */
export function lerDataHora(data: string, hora: string): number | null {
  if (!data || !hora) return null
  const [a, m, d] = data.split('-').map(Number)
  const [h, mi] = hora.split(':').map(Number)
  if ([a, m, d, h, mi].some((x) => Number.isNaN(x))) return null
  const t = new Date(a, m - 1, d, h, mi).getTime()
  return Number.isNaN(t) ? null : t
}

/** Instante do servidor (LocalDateTime sem fuso) para milissegundos. */
export const instante = (texto: string) => new Date(texto).getTime()

/** Milissegundos para o formato que o servidor espera: 2026-11-25T09:12 */
export const paraServidor = (ms: number) => {
  const d = new Date(ms)
  return `${isoData(d)}T${isoHora(d)}`
}

/** Meia-noite (local) do dia de `ms`. */
export const inicioDoDia = (ms: number) => {
  const d = new Date(ms)
  return new Date(d.getFullYear(), d.getMonth(), d.getDate()).getTime()
}

/** Expediente: segunda a sexta, 8h às 14h. */
export function noExpediente(ms: number): boolean {
  const d = new Date(ms)
  const dia = d.getDay()
  const h = d.getHours() + d.getMinutes() / 60
  return dia >= 1 && dia <= 5 && h >= 8 && h <= 14
}
