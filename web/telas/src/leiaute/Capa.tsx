import { useEffect, useState } from 'react'
import type { TemaEfetivo } from '../tema/useTema'
import { CenasCapa } from './CenasCapa'
import { PALETAS_CAPA, type Cena } from './capaPaletas'

type Props = {
  cena: Cena
  tema: TemaEfetivo
}

const ehNoite = () => {
  const hora = new Date().getHours()
  return hora >= 18 || hora < 6
}

/** Capa ilustrada fixa no topo de cada aba. Sem balão de fala. */
export function Capa({ cena, tema }: Props) {
  // Na cena do Início, o sol vira lua às 18h (e volta às 6h). Confere a cada minuto.
  const [noite, setNoite] = useState(ehNoite)
  useEffect(() => {
    const relogio = window.setInterval(() => setNoite(ehNoite()), 60_000)
    return () => window.clearInterval(relogio)
  }, [])

  const paleta = tema === 'escuro' ? PALETAS_CAPA.noite : PALETAS_CAPA.papel

  return (
    <div className={cena === 'inicio' ? 'capa capa-inicio' : 'capa'} style={{ background: paleta.bg }} data-cena={cena}>
      <svg viewBox="0 0 1440 300" preserveAspectRatio="xMidYMid slice" width="100%" height="100%" aria-hidden="true">
        <CenasCapa cena={cena} noite={noite} t={paleta} />
      </svg>
    </div>
  )
}
