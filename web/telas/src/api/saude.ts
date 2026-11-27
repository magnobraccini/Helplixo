import { pedir } from './cliente'

/** Resposta de GET /api/saude (ver web/servidor/.../SaudeController.java). */
export type Saude = {
  status: string
  sistema: string
  agora: string
}

export function buscarSaude(): Promise<Saude> {
  return pedir<Saude>('/api/saude')
}
