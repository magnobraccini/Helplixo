import { useEffect, useRef, useState } from 'react'

export type Notificacao = { id: string; texto: string; quando: string }

type Props = {
  caminho: string[]
  perfil: string
  editando: boolean
  /** Lembretes do calendário (e, mais tarde, avisos de chamados). */
  notificacoes: Notificacao[]
  aoAbrirNotificacao: (n: Notificacao) => void
}

/** Barra sobre a capa: caminho, selo "Editando", "Você: <perfil>" e o sino. */
export function Topo({ caminho, perfil, editando, notificacoes, aoAbrirNotificacao }: Props) {
  const [sinoAberto, setSinoAberto] = useState(false)
  const [lidas, setLidas] = useState<Set<string>>(new Set())
  const areaSino = useRef<HTMLDivElement>(null)
  const naoLidas = notificacoes.filter((n) => !lidas.has(n.id)).length

  // Fecha a lista de notificações com Esc ou com um clique fora dela.
  useEffect(() => {
    if (!sinoAberto) return
    const aoTeclar = (e: KeyboardEvent) => e.key === 'Escape' && setSinoAberto(false)
    const aoClicar = (e: MouseEvent) => {
      if (areaSino.current && !areaSino.current.contains(e.target as Node)) setSinoAberto(false)
    }
    document.addEventListener('keydown', aoTeclar)
    document.addEventListener('mousedown', aoClicar)
    return () => {
      document.removeEventListener('keydown', aoTeclar)
      document.removeEventListener('mousedown', aoClicar)
    }
  }, [sinoAberto])

  return (
    <header className="topo">
      <nav aria-label="Caminho" className="caminho">
        {caminho.map((parte, i) => (
          <span key={i} className="caminho-parte">
            {i > 0 ? '/  ' : ''}
            {parte}
          </span>
        ))}
      </nav>
      <div className="topo-direita" ref={areaSino}>
        {editando && (
          <span role="status" className="selo-editando">
            Editando · salva para todos
          </span>
        )}
        <span className="selo-perfil" title="Perfil de acesso de quem está usando">
          Você: {perfil}
        </span>
        <button
          type="button"
          className="sino"
          aria-label="Notificações"
          aria-expanded={sinoAberto}
          onClick={() => setSinoAberto((aberto) => !aberto)}
        >
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
            <path d="M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9" />
            <path d="M10.3 21a1.94 1.94 0 0 0 3.4 0" />
          </svg>
          {naoLidas > 0 && (
            <span className="sino-contador" aria-label={`${naoLidas} não lida(s)`}>
              {naoLidas}
            </span>
          )}
        </button>
        {sinoAberto && (
          <div role="dialog" aria-label="Notificações" className="notificacoes">
            <div className="notificacoes-topo">
              <span className="notificacoes-titulo">Notificações</span>
              {naoLidas > 0 && (
                <button type="button" className="link-acao" onClick={() => setLidas(new Set(notificacoes.map((n) => n.id)))}>
                  Marcar todas como lidas
                </button>
              )}
            </div>
            {notificacoes.map((n) => {
              const lida = lidas.has(n.id)
              return (
                <button
                  key={n.id}
                  type="button"
                  className={lida ? 'notificacao' : 'notificacao nao-lida'}
                  onClick={() => {
                    setLidas((s) => new Set(s).add(n.id))
                    setSinoAberto(false)
                    aoAbrirNotificacao(n)
                  }}
                >
                  <span aria-hidden="true" className="notificacao-ponto" />
                  <span className="notificacao-texto">
                    <span>{n.texto}</span>
                    <span className="nota-campo">{n.quando}</span>
                  </span>
                </button>
              )
            })}
            {notificacoes.length === 0 && <p className="notificacoes-vazio">Nenhuma notificação por enquanto.</p>}
          </div>
        )}
      </div>
    </header>
  )
}
