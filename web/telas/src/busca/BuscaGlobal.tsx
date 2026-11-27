import { useEffect, useMemo, useState } from 'react'
import { listarChamados, type Chamado } from '../api/chamados'
import { listarExternos, listarInternos } from '../api/contatos'
import { buscarMaquinas } from '../api/inventario'
import { procurarProcessos } from '../api/processos'
import { isoData, DIA } from '../util/datas'
import { MINIMO_LETRAS, montarGrupos, type Achados, type Destino } from './grupos'
import './busca.css'

type Props = { agora: number; aoAbrir: (d: Destino) => void; aoFechar: () => void }
type DoServidor = Omit<Achados, 'chamados'>

const NADA: DoServidor = { maquinas: [], processos: [], internos: [], externos: [] }
/** Os chamados entram dos últimos 90 dias (o maior período da lista) e são filtrados aqui mesmo. */
const DIAS_DE_CHAMADOS = 90

/** A busca única (Ctrl K): chamados, máquinas, processos e contatos numa janela só. */
export function BuscaGlobal({ agora, aoAbrir, aoFechar }: Props) {
  const [q, setQ] = useState('')
  const [chamados, setChamados] = useState<Chamado[]>([])
  const [achados, setAchados] = useState<{ q: string; r: DoServidor }>({ q: '', r: NADA })
  const [falhou, setFalhou] = useState(false)

  useEffect(() => {
    const aoTeclar = (e: KeyboardEvent) => e.key === 'Escape' && aoFechar()
    document.addEventListener('keydown', aoTeclar)
    return () => document.removeEventListener('keydown', aoTeclar)
  }, [aoFechar])

  // Os chamados vêm uma vez, ao abrir; os mais novos primeiro.
  useEffect(() => {
    let ativo = true
    listarChamados(isoData(new Date(agora - DIAS_DE_CHAMADOS * DIA)), isoData(new Date(agora)))
      .then((l) => ativo && setChamados([...l].sort((a, b) => b.abertoEm.localeCompare(a.abertoEm))))
      .catch(() => ativo && setFalhou(true))
    return () => {
      ativo = false
    }
    // Só ao abrir: o relógio andando não deve pedir de novo.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  // O resto vai ao servidor, esperando a pessoa parar de digitar.
  const termo = q.trim()
  useEffect(() => {
    if (termo.length < MINIMO_LETRAS) return
    let ativo = true
    const espera = setTimeout(() => {
      Promise.all([
        buscarMaquinas(termo),
        procurarProcessos(termo),
        listarInternos({ secretaria: '', andar: '', ordem: 'nome', q: termo }),
        listarExternos({ categoria: '', situacao: '', q: termo }),
      ])
        .then(([m, p, i, x]) => {
          if (!ativo) return
          setAchados({ q: termo, r: { maquinas: m.maquinas, processos: p, internos: i.contatos, externos: x.contatos.map((c) => c.contato) } })
          setFalhou(false)
        })
        .catch(() => ativo && setFalhou(true))
    }, 250)
    return () => {
      ativo = false
      clearTimeout(espera)
    }
  }, [termo])

  // Achados de outro texto não valem para o texto de agora.
  const doServidor = termo.length >= MINIMO_LETRAS && achados.q === termo ? achados.r : NADA
  const procurando = termo.length >= MINIMO_LETRAS && achados.q !== termo
  const grupos = useMemo(() => montarGrupos(termo, { chamados, ...doServidor }), [termo, chamados, doServidor])

  const abrir = (d: Destino) => {
    aoFechar()
    aoAbrir(d)
  }

  return (
    <div className="fundo-dialogo fundo-busca" onClick={aoFechar}>
      <div role="dialog" aria-modal="true" aria-label="Buscar em tudo" className="busca" onClick={(e) => e.stopPropagation()}>
        <label className="busca-campo">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" aria-hidden="true">
            <circle cx="11" cy="11" r="7" />
            <path d="M21 21l-4.3-4.3" />
          </svg>
          <input
            type="search"
            autoFocus
            aria-label="Buscar em tudo"
            value={q}
            onChange={(e) => setQ(e.target.value)}
            placeholder="Chamados, processos, contatos, equipamentos…"
          />
          <span className="busca-tecla">Esc</span>
        </label>
        <div className="busca-resultados">
          {falhou && <p className="caixa-erro">Não foi possível procurar. Confira se o servidor está no ar.</p>}
          {grupos.map((g) => (
            <div key={g.nome} role="group" aria-label={g.nome}>
              <div className="busca-grupo">{g.nome}</div>
              {g.itens.map((it) => (
                <button key={it.chave} type="button" className="busca-item" onClick={() => abrir(it.destino)}>
                  <span className="busca-codigo">{it.codigo}</span>
                  <span className="busca-rotulo">{it.rotulo}</span>
                  <span className="busca-dica">{it.dica}</span>
                </button>
              ))}
            </div>
          ))}
          {termo.length > 0 && termo.length < MINIMO_LETRAS && (
            <p className="busca-vazia">Digite ao menos {MINIMO_LETRAS} letras para procurar também máquinas, processos e contatos.</p>
          )}
          {procurando && grupos.length === 0 && <p className="busca-vazia">Procurando…</p>}
          {!procurando && !falhou && termo && grupos.length === 0 && <p className="busca-vazia">Nada encontrado para “{termo}”.</p>}
        </div>
      </div>
    </div>
  )
}
