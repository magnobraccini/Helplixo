/** Resposta de GET /api/saude (ver web/servidor/.../SaudeController.java). */
export type Saude = {
  status: string
  sistema: string
  agora: string
}

export async function buscarSaude(): Promise<Saude> {
  const resposta = await fetch('/api/saude')
  if (!resposta.ok) {
    throw new Error(`Servidor respondeu ${resposta.status}`)
  }
  return (await resposta.json()) as Saude
}
