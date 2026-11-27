import { useEffect, useState } from 'react'

/** A hora atual, atualizada a cada minuto (para os tempos de espera andarem sozinhos na tela). */
export function useAgora(intervaloMs = 60_000): number {
  const [agora, setAgora] = useState(() => Date.now())
  useEffect(() => {
    const relogio = window.setInterval(() => setAgora(Date.now()), intervaloMs)
    return () => window.clearInterval(relogio)
  }, [intervaloMs])
  return agora
}
