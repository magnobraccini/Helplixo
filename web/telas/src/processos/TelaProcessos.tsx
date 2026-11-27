import { useEffect, useState, type ReactNode } from 'react'
import { arvoreProcessos, buscarProcesso, procurarProcessos, type Arvore, type DetalheProcesso, type ItemProcesso, type NoPasta } from '../api/processos'
import { CabecalhoPagina } from '../leiaute/CabecalhoPagina'
import { fmtData, instante } from '../util/datas'
import { filtrarArvore, idsDasPastas } from './conteudo'
import { LeituraProcesso } from './LeituraProcesso'
import './processos.css'

type Props = {
  /** Id do processo aberto ao lado da árvore (fica no App para sobreviver à troca de seção). */
  aberto: string | null
  aoAbrir: (id: string) => void
  aoFechar: () => void
  /** Processo lido (para "Continue de onde parou"). */
  aoVisitar: (d: DetalheProcesso) => void
  avisar: (mensagem: string) => void
}

type Achados = { termo: string; ids: Set<string> | null; erro: string }

/** Pastas que levam até o processo (para abrir a árvore no caminho dele). */
function caminhoAte(pastas: NoPasta[], id: string): string[] | null {
  for (const p of pastas) {
    if (p.processos.some((x) => x.id === id)) return [p.id]
    const sub = caminhoAte(p.pastas, id)
    if (sub) return [p.id, ...sub]
  }
  return null
}

export function TelaProcessos(p: Props) {
  const [arvore, setArvore] = useState<Arvore | null>(null)
  const [erro, setErro] = useState('')
  const [abertas, setAbertas] = useState<Set<string>>(new Set())
  const [termo, setTermo] = useState('')
  const [achados, setAchados] = useState<Achados>({ termo: '', ids: null, erro: '' })
  const [detalhes, setDetalhes] = useState<Record<string, DetalheProcesso>>({})

  useEffect(() => {
    let ativo = true
    arvoreProcessos()
      .then((a) => ativo && setArvore(a))
      .catch(() => ativo && setErro('Não foi possível carregar os processos. Confira se o servidor está no ar.'))
    return () => {
      ativo = false
    }
  }, [])

  // Busca no servidor (título, descrição, tags e conteúdo, sem ligar para acentos).
  const q = termo.trim()
  useEffect(() => {
    if (!q) return
    let ativo = true
    const espera = window.setTimeout(() => {
      procurarProcessos(q)
        .then((r) => ativo && setAchados({ termo: q, ids: new Set(r.map((x) => x.id)), erro: '' }))
        .catch(() => ativo && setAchados({ termo: q, ids: null, erro: 'Não foi possível procurar agora.' }))
    }, 250)
    return () => {
      ativo = false
      window.clearTimeout(espera)
    }
  }, [q])

  const aberto = p.aberto
  useEffect(() => {
    if (!aberto) return
    let ativo = true
    buscarProcesso(aberto)
      .then((d) => {
        if (!ativo) return
        setDetalhes((x) => ({ ...x, [aberto]: d }))
        p.aoVisitar(d)
      })
      .catch(() => ativo && p.avisar('Não foi possível abrir o processo.'))
    return () => {
      ativo = false
    }
    // só quando muda o processo aberto
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [aberto])

  const procurando = q !== '' && achados.termo === q && achados.ids !== null
  const pastas = arvore ? (procurando ? filtrarArvore(arvore.pastas, achados.ids!) : arvore.pastas) : []
  const semPasta = arvore ? arvore.semPasta.filter((x) => !procurando || achados.ids!.has(x.id)) : []
  // Na busca, todas as pastas que sobraram ficam abertas; o caminho do processo aberto também.
  const caminhoAberto = arvore && aberto ? (caminhoAte(arvore.pastas, aberto) ?? []) : []
  const estaAberta = (id: string) => procurando || abertas.has(id) || caminhoAberto.includes(id)
  const alternar = (id: string) =>
    setAbertas((s) => {
      const n = new Set(s)
      if (estaAberta(id)) n.delete(id)
      else n.add(id)
      return n
    })

  const linhaProcesso = (x: ItemProcesso, nivel: number) => (
    <button
      key={x.id}
      type="button"
      role="treeitem"
      aria-current={aberto === x.id ? 'true' : undefined}
      className="arvore-processo"
      style={{ paddingLeft: 8 + nivel * 22 }}
      onClick={() => p.aoAbrir(x.id)}
    >
      <span aria-hidden="true" className="arvore-seta" />
      <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinejoin="round" aria-hidden="true">
        <path d="M14 3H6a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V9z" />
        <path d="M14 3v6h6" />
      </svg>
      <span className="arvore-nome">{x.titulo}</span>
      <span className="arvore-quando">{x.atualizadoEm ? fmtData(instante(x.atualizadoEm)).slice(0, 5) : ''}</span>
    </button>
  )

  const linhasPasta = (lista: NoPasta[], nivel: number): ReactNode[] =>
    lista.flatMap((pasta) => {
      const aberta = estaAberta(pasta.id)
      return [
        <button
          key={pasta.id}
          type="button"
          role="treeitem"
          aria-expanded={aberta}
          className="arvore-pasta"
          style={{ paddingLeft: 8 + nivel * 22 }}
          onClick={() => alternar(pasta.id)}
        >
          <span aria-hidden="true" className="arvore-seta">
            {aberta ? '▾' : '▸'}
          </span>
          <svg width="16" height="16" viewBox="0 0 24 24" fill="#F2DDA4" stroke="#8A6A1F" strokeWidth="1.6" strokeLinejoin="round" aria-hidden="true">
            <path d="M3 7a2 2 0 0 1 2-2h4l2 2h8a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z" />
          </svg>
          <span className="arvore-nome">{pasta.nome}</span>
          <span className="arvore-quando">{pasta.total}</span>
        </button>,
        ...(aberta ? [...linhasPasta(pasta.pastas, nivel + 1), ...pasta.processos.map((x) => linhaProcesso(x, nivel + 1))] : []),
      ]
    })

  const detalhe = aberto ? detalhes[aberto] : undefined

  return (
    <>
      <CabecalhoPagina titulo="Processos" subtitulo="O passo a passo dos atendimentos, organizado em pastas." />
      <div className="processos-barra">
        <label className="busca-processo">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" aria-hidden="true">
            <circle cx="11" cy="11" r="7" />
            <path d="M21 21l-4.3-4.3" />
          </svg>
          <input type="search" aria-label="Procurar processo" value={termo} placeholder="Procurar processo" onChange={(e) => setTermo(e.target.value)} />
        </label>
        <button type="button" className="botao-leve" onClick={() => arvore && setAbertas(new Set(idsDasPastas(arvore.pastas)))}>
          Abrir todas
        </button>
        <button type="button" className="botao-leve" onClick={() => setAbertas(new Set())}>
          Fechar todas
        </button>
      </div>
      {erro && (
        <p role="alert" className="caixa-erro">
          {erro}
        </p>
      )}

      <div className="processos-colunas">
        <div role="tree" aria-label="Pastas e processos" className="arvore">
          {linhasPasta(pastas, 0)}
          {semPasta.map((x) => linhaProcesso(x, 0))}
          {procurando && pastas.length === 0 && semPasta.length === 0 && <p className="lista-vazia">Nenhum processo encontrado para “{q}”.</p>}
          {!arvore && !erro && <p className="lista-vazia">Carregando…</p>}
        </div>

        {detalhe ? (
          <LeituraProcesso key={detalhe.processo.id} detalhe={detalhe} aoFechar={p.aoFechar} avisar={p.avisar} />
        ) : (
          <section className="painel-processo" aria-label="Processo aberto">
            <p className="processo-vazio">{aberto ? 'Carregando…' : 'Escolha um processo na árvore para ler aqui, sem sair da tela.'}</p>
          </section>
        )}
      </div>
    </>
  )
}
