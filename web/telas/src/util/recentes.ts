// "Continue de onde parou" e "Vistas recentemente": o que esta pessoa abriu por último,
// guardado neste computador (localStorage).

export type Recente = {
  tipo: 'chamado' | 'maquina' | 'processo'
  id: string
  titulo: string
  /** Onde fica (ex.: "Chamados · Tributação - SEFAZ") */
  local: string
  /** Quando foi aberto (ms). */
  quando: number
}

export const CHAVE_RECENTES = 'helplixo-recentes'
const MAXIMO = 8

export function lerRecentes(): Recente[] {
  try {
    const lista = JSON.parse(window.localStorage.getItem(CHAVE_RECENTES) ?? '[]') as Recente[]
    return Array.isArray(lista) ? lista : []
  } catch {
    return []
  }
}

/** Põe o item no topo (sem repetir) e guarda só os mais recentes. */
export function registrarRecente(item: Recente): Recente[] {
  const lista = [item, ...lerRecentes().filter((r) => !(r.tipo === item.tipo && r.id === item.id))].slice(0, MAXIMO)
  try {
    window.localStorage.setItem(CHAVE_RECENTES, JSON.stringify(lista))
  } catch {
    // sem localStorage: a lista vale só até fechar a aba
  }
  return lista
}
