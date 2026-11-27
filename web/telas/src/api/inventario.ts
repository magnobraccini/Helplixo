import { pedir } from './cliente'

// Contrato de web/servidor/.../inventario/InventarioController.java (GET /api/inventario/{nSemin})

export type ItemHistorico = {
  quando: string
  /** "Conserto", "Chamado", "Movimentação" ou registros do cadastro. */
  tipo: string
  texto: string
  /** Nº do chamado ligado, quando houver. */
  referencia: string | null
  quem: string | null
}

export type MaquinaResumo = {
  nSemin: string
  patrimonio: string
  tipo: string
  modelo: string
  local: string
  usuario: string
  situacao: string
}

export type FichaMaquina = {
  maquina: MaquinaResumo
  historico: ItemHistorico[]
}

export function buscarFichaMaquina(nSemin: string): Promise<FichaMaquina> {
  return pedir<FichaMaquina>(`/api/inventario/${encodeURIComponent(nSemin)}`)
}
