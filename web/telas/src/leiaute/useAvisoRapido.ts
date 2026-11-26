import { useCallback, useEffect, useRef, useState } from 'react'

/** O "aviso rápido" (toast) da maquete: some sozinho depois de 4,5 segundos. */
export function useAvisoRapido() {
  const [mensagem, setMensagem] = useState('')
  const relogio = useRef<number | undefined>(undefined)

  const avisar = useCallback((texto: string) => {
    window.clearTimeout(relogio.current)
    setMensagem(texto)
    relogio.current = window.setTimeout(() => setMensagem(''), 4500)
  }, [])

  useEffect(() => () => window.clearTimeout(relogio.current), [])

  return { mensagem, avisar }
}
