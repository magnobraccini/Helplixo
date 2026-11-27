import { useEffect, useState } from 'react'
import { ErroDaApi } from '../api/cliente'
import { buscarFichaMaquina, type FichaMaquina } from '../api/inventario'
import { fmtData, instante } from '../util/datas'

type Props = {
  /** Chamado aberto agora: ele não entra na lista ("só o que aconteceu além deste chamado"). */
  numero: string
  nSemin: string | null
  aoAbrirChamado: (numero: string) => void
}

const CORES_TIPO: Record<string, [string, string]> = {
  'Chamado': ['var(--c-e7f3f8)', 'var(--c-1d4f68)'],
  'Conserto': ['var(--c-fbf3db)', 'var(--c-6b4f0e)'],
}
const CORES_OUTROS: [string, string] = ['var(--c-f4f0f7)', 'var(--c-5a3d78)']

type Estado = { ficha: FichaMaquina | null; erro: string; carregando: boolean }

/** Aba "Histórico da máquina": chamados, consertos e movimentações do mesmo Nº Semin (GET /api/inventario/{nSemin}). */
export function HistoricoMaquina({ numero, nSemin, aoAbrirChamado }: Props) {
  const [estado, setEstado] = useState<Estado>({ ficha: null, erro: '', carregando: !!nSemin })

  useEffect(() => {
    if (!nSemin) return
    let ativo = true
    buscarFichaMaquina(nSemin)
      .then((ficha) => ativo && setEstado({ ficha, erro: '', carregando: false }))
      .catch((falha) => {
        if (!ativo) return
        const erro =
          falha instanceof ErroDaApi && falha.status === 404
            ? `A máquina ${nSemin} não está no inventário.`
            : 'Não foi possível carregar o histórico da máquina. Confira se o servidor está no ar.'
        setEstado({ ficha: null, erro, carregando: false })
      })
    return () => {
      ativo = false
    }
  }, [nSemin])

  if (!nSemin) {
    return (
      <div className="historico-topo">
        <h2>Chamado sem máquina</h2>
        <span className="apagado">Este chamado não tem Nº Semin, então não há histórico de máquina.</span>
      </div>
    )
  }

  const linhas = (estado.ficha?.historico ?? []).filter((h) => !(h.tipo === 'Chamado' && h.referencia === numero))
  const conta = (tipo: string) => linhas.filter((h) => h.tipo === tipo).length

  return (
    <>
      <div className="historico-topo">
        <h2>Histórico da máquina {nSemin}</h2>
        <span className="apagado">
          {estado.carregando
            ? 'Carregando…'
            : estado.erro || `${conta('Chamado')} outro(s) chamado(s), ${conta('Conserto')} conserto(s), ${conta('Movimentação')} movimentação(ões).`}
        </span>
      </div>
      <div className="historico-lista">
        {linhas.map((h, i) => {
          const [fundo, texto] = CORES_TIPO[h.tipo] ?? CORES_OUTROS
          return (
            <div key={i} className="historico-linha">
              <span className="apagado numeros">{fmtData(instante(h.quando))}</span>
              <span>
                <span className="pilula-status" style={{ background: fundo, color: texto }}>
                  {h.tipo}
                </span>
              </span>
              <span className="historico-texto">
                {h.tipo === 'Chamado' && h.referencia && (
                  <button type="button" className="link-chamado" onClick={() => aoAbrirChamado(h.referencia!)}>
                    Nº {h.referencia}
                  </button>
                )}
                <span>{h.texto}</span>
                <span className="nota-campo">Técnico: {h.quem || '—'}</span>
              </span>
            </div>
          )
        })}
        {!estado.carregando && !estado.erro && linhas.length === 0 && <p className="lista-vazia">Nada registrado além deste chamado.</p>}
      </div>
    </>
  )
}
