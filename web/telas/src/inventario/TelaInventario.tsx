import { useEffect, useState } from 'react'
import { ErroDaApi } from '../api/cliente'
import {
  buscarFichaMaquina,
  buscarMaquinas,
  resumoInventario,
  type Autoria,
  type Busca,
  type FichaMaquina as Ficha,
  type Maquina,
  type Resumo,
} from '../api/inventario'
import { CabecalhoPagina } from '../leiaute/CabecalhoPagina'
import type { Recente } from '../util/recentes'
import { FichaMaquina, type AbaMaquina } from './FichaMaquina'
import { FormMaquina } from './FormMaquina'
import { FormMovimentacao } from './FormMovimentacao'
import { coresOffice, corSituacao, textoResumo } from './regras'
import '../chamados/chamados.css'
import './inventario.css'

type Props = {
  /** Nº Semin aberto na ficha (null = busca). Fica no App para o Nº Semin dos chamados abrir aqui. */
  aberta: string | null
  aba: AbaMaquina
  aoAbrir: (nSemin: string, aba: AbaMaquina) => void
  aoVoltar: () => void
  aoAbrirChamado: (numero: string) => void
  /** Máquina aberta (para "Vistas recentemente" e "Continue de onde parou"). */
  aoVisitar: (m: Maquina) => void
  vistas: Recente[]
  editando: boolean
  autoria: Autoria
  avisar: (mensagem: string) => void
  tecnicos: string[]
  agora: number
}

type Painel = 'nenhum' | 'adicionar' | 'mover'
type Resultado = { termo: string; busca: Busca | null; erro: string }
type Carga = { nSemin: string; ficha: Ficha | null; erro: string }

/** A busca só começa com 2 letras (regra de negócio). */
const MINIMO = 2

export function TelaInventario(p: Props) {
  const [termo, setTermo] = useState('')
  const [resultado, setResultado] = useState<Resultado>({ termo: '', busca: null, erro: '' })
  const [resumo, setResumo] = useState<Resumo | null>(null)
  const [painel, setPainel] = useState<Painel>('nenhum')
  const [carga, setCarga] = useState<Carga>({ nSemin: '', ficha: null, erro: '' })
  const [recarga, setRecarga] = useState(0)

  useEffect(() => {
    let ativo = true
    resumoInventario()
      .then((r) => ativo && setResumo(r))
      .catch(() => {})
    return () => {
      ativo = false
    }
  }, [recarga])

  // Busca no servidor um instante depois de parar de digitar.
  const q = termo.trim()
  useEffect(() => {
    if (q.length < MINIMO) return
    let ativo = true
    const espera = window.setTimeout(() => {
      buscarMaquinas(q)
        .then((busca) => ativo && setResultado({ termo: q, busca, erro: '' }))
        .catch(() => ativo && setResultado({ termo: q, busca: null, erro: 'Não foi possível procurar. Confira se o servidor está no ar.' }))
    }, 250)
    return () => {
      ativo = false
      window.clearTimeout(espera)
    }
  }, [q, recarga])

  // Ficha aberta: busca no servidor.
  const aberta = p.aberta
  useEffect(() => {
    if (!aberta) return
    let ativo = true
    buscarFichaMaquina(aberta)
      .then((ficha) => {
        if (!ativo) return
        setCarga({ nSemin: aberta, ficha, erro: '' })
        p.aoVisitar(ficha.maquina)
      })
      .catch((falha) => {
        if (!ativo) return
        const erro = falha instanceof ErroDaApi && falha.status === 404 ? `A máquina ${aberta} não está no inventário.` : 'Não foi possível abrir a ficha. Confira se o servidor está no ar.'
        setCarga({ nSemin: aberta, ficha: null, erro })
      })
    return () => {
      ativo = false
    }
    // só quando muda a máquina aberta (ou depois de uma alteração)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [aberta, recarga])

  const depoisDeMudar = (m: Maquina, mensagem: string) => {
    setPainel('nenhum')
    setRecarga((n) => n + 1)
    p.avisar(mensagem)
    if (p.aberta !== m.nSemin) p.aoAbrir(m.nSemin, 'ident')
  }

  if (aberta) {
    const ficha = carga.nSemin === aberta ? carga.ficha : null
    const m = ficha?.maquina
    return (
      <>
        <CabecalhoPagina
          titulo={m ? `${m.nSemin} · ${m.tipo} ${m.modelo}` : aberta}
          subtitulo={m ? `Patrimônio ${m.patrimonio} · ${m.local} · ${m.usuario || 'sem usuário'}` : carga.nSemin === aberta ? carga.erro : 'Carregando…'}
        />
        {ficha ? (
          <FichaMaquina
            key={aberta}
            ficha={ficha}
            aba={p.aba}
            aoTrocarAba={(aba) => p.aoAbrir(aberta, aba)}
            aoVoltar={p.aoVoltar}
            aoAbrirChamado={p.aoAbrirChamado}
            aoMudar={depoisDeMudar}
            editando={p.editando}
            autoria={p.autoria}
            tecnicos={p.tecnicos}
            agora={p.agora}
          />
        ) : (
          <button type="button" className="link-voltar" onClick={p.aoVoltar}>
            ← Voltar para o Inventário
          </button>
        )}
      </>
    )
  }

  const procurando = q.length >= MINIMO
  const atual = procurando && resultado.termo === q ? resultado : null
  const maquinasVistas = p.vistas.filter((v) => v.tipo === 'maquina')

  return (
    <>
      <CabecalhoPagina titulo="Inventário" subtitulo="Procure a máquina pelo Nº Semin, patrimônio, usuário ou setor." />
      <div className="ficha-barra">
        <span className="espaco" />
        {p.editando && (
          <button type="button" className="botao-edicao" onClick={() => setPainel('adicionar')}>
            Adicionar máquina
          </button>
        )}
        <button type="button" className="botao-contorno" onClick={() => setPainel('mover')}>
          Registrar movimentação
        </button>
      </div>

      {painel === 'adicionar' && <FormMaquina autoria={p.autoria} aoSalvar={depoisDeMudar} aoCancelar={() => setPainel('nenhum')} />}
      {painel === 'mover' && <FormMovimentacao tecnicos={p.tecnicos} agora={p.agora} aoSalvar={depoisDeMudar} aoCancelar={() => setPainel('nenhum')} />}

      <label className="busca-grande">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" aria-hidden="true">
          <circle cx="11" cy="11" r="7" />
          <path d="M21 21l-4.3-4.3" />
        </svg>
        <input
          type="search"
          aria-label="Procurar máquina"
          value={termo}
          onChange={(e) => setTermo(e.target.value)}
          placeholder="Procure pelo Nº Semin, patrimônio, usuário, setor ou software"
        />
      </label>

      {!procurando && (
        <>
          <span className="inventario-resumo">{resumo ? textoResumo(resumo.total, resumo.porTipo) : 'Digite pelo menos 2 letras para procurar.'}</span>
          <h2 className="subtitulo-ficha">Vistas recentemente</h2>
          {maquinasVistas.map((v) => (
            <button key={v.id} type="button" className="item-maquina" onClick={() => p.aoAbrir(v.id, 'ident')}>
              <b className="item-maquina-codigo">{v.id}</b>
              <span className="item-maquina-meio">
                <span className="item-maquina-titulo">{v.titulo}</span>
                <span className="item-maquina-sub">{v.local}</span>
              </span>
            </button>
          ))}
          {maquinasVistas.length === 0 && <p className="lista-vazia">As máquinas que você abrir aparecem aqui.</p>}
        </>
      )}

      {procurando && !atual && <span className="inventario-resumo">Procurando…</span>}
      {atual?.erro && (
        <p role="alert" className="caixa-erro">
          {atual.erro}
        </p>
      )}
      {atual?.busca?.temMais && (
        <span className="aviso-muitos">
          Mostrando {atual.busca.maquinas.length} de {atual.busca.total}. Refine a busca para achar mais rápido.
        </span>
      )}
      {atual?.busca && (
        <div className="lista-maquinas">
          {atual.busca.maquinas.map((m) => {
            const office = coresOffice(m.office)
            return (
              <button key={m.nSemin} type="button" className="item-maquina" onClick={() => p.aoAbrir(m.nSemin, 'ident')}>
                <b className="item-maquina-codigo">{m.nSemin}</b>
                <span className="item-maquina-meio">
                  <span className="item-maquina-titulo">
                    {m.tipo} {m.modelo}
                  </span>
                  <span className="item-maquina-sub">
                    {m.local} · {m.usuario || 'sem usuário'} · patrimônio {m.patrimonio}
                  </span>
                </span>
                <span className="item-maquina-selos">
                  <span className="pilula-status" style={{ background: office.fundo, color: office.texto }}>
                    Office: {m.office}
                  </span>
                  <span className="pilula-status" style={{ background: 'var(--c-f3f5f8)', color: corSituacao(m.situacao) }}>
                    {m.situacao}
                  </span>
                </span>
              </button>
            )
          })}
          {atual.busca.maquinas.length === 0 && <p className="lista-vazia">Nenhuma máquina encontrada para “{q}”.</p>}
        </div>
      )}
    </>
  )
}
