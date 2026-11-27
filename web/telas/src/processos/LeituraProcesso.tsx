import { useRef, useState } from 'react'
import type { DetalheProcesso } from '../api/processos'
import { fmtData, instante } from '../util/datas'
import { blocosDoProcesso, contagem, type BlocoTela } from './conteudo'

type Props = {
  detalhe: DetalheProcesso
  aoFechar: () => void
  avisar: (mensagem: string) => void
}

/** O processo aberto ao lado da árvore, com rolagem própria e o índice "Neste processo". */
export function LeituraProcesso({ detalhe, aoFechar, avisar }: Props) {
  const p = detalhe.processo
  const blocos = blocosDoProcesso(p)
  const titulos = blocos.filter((b): b is Extract<BlocoTela, { tipo: 'titulo' }> => b.tipo === 'titulo')
  const [ativa, setAtiva] = useState(titulos[0]?.id ?? '')
  const painel = useRef<HTMLElement>(null)

  // Marca no índice a seção que está no alto do painel enquanto rola (como na maquete: onRolar).
  const aoRolar = () => {
    const el = painel.current
    if (!el) return
    const topo = el.getBoundingClientRect().top
    const hs = [...el.querySelectorAll<HTMLElement>('h3[id^="ps-"]')]
    let atual = hs[0]?.id ?? ''
    hs.forEach((h) => {
      if (h.getBoundingClientRect().top - topo <= 60) atual = h.id
    })
    if (el.scrollTop + el.clientHeight >= el.scrollHeight - 4 && hs.length) atual = hs[hs.length - 1].id
    if (atual !== ativa) setAtiva(atual)
  }

  const irPara = (id: string) => {
    setAtiva(id)
    document.getElementById(id)?.scrollIntoView?.({ behavior: 'smooth', block: 'start' })
  }

  const copiar = async (caminho: string) => {
    try {
      await navigator.clipboard.writeText(caminho)
    } catch {
      // sem permissão para a área de transferência: o aviso mostra o caminho para copiar à mão
    }
    avisar(`Caminho copiado: ${caminho}. Cole na barra do Explorador de Arquivos.`)
  }

  return (
    <section ref={painel} className="painel-processo" aria-label="Processo aberto" onScroll={aoRolar}>
      <div className="processo-topo">
        <span className="processo-titulos">
          <h2>{p.titulo}</h2>
          <span className="nota-campo">{contagem(blocos)}</span>
        </span>
        <button type="button" className="dialogo-fechar" aria-label="Fechar processo" onClick={aoFechar}>
          ×
        </button>
      </div>
      <div className="processo-meta">
        <span>
          Pasta: <b>{detalhe.caminho.join(' › ') || '—'}</b>
        </span>
        <span>
          Responsável: <b>{p.responsavel || '—'}</b>
        </span>
        <span>
          Atualizado: <b>{p.atualizadoEm ? fmtData(instante(p.atualizadoEm)) : '—'}</b>
        </span>
        {p.tags.length > 0 && (
          <span className="processo-tags">
            {p.tags.map((t) => (
              <span key={t} className="processo-tag">
                {t}
              </span>
            ))}
          </span>
        )}
      </div>
      {p.descricao && <p className="processo-resumo">{p.descricao}</p>}

      <div className="processo-corpo">
        {titulos.length > 0 && (
          <nav aria-label="Neste processo" className="indice-processo">
            <span className="indice-titulo">Neste processo</span>
            {titulos.map((t) => (
              <button key={t.id} type="button" aria-current={ativa === t.id ? 'true' : undefined} onClick={() => irPara(t.id)}>
                {t.texto}
              </button>
            ))}
            <button type="button" className="indice-topo" onClick={() => painel.current?.scrollTo?.({ top: 0, behavior: 'smooth' })}>
              ↑ Voltar ao topo
            </button>
          </nav>
        )}
        <article className="processo-texto">
          {blocos.map((b, i) => (
            <Bloco key={i} bloco={b} aoCopiar={copiar} />
          ))}
          {blocos.length === 0 && <p className="nota-campo">Este processo ainda não tem conteúdo.</p>}
        </article>
      </div>
    </section>
  )
}

function Bloco({ bloco: b, aoCopiar }: { bloco: BlocoTela; aoCopiar: (caminho: string) => void }) {
  switch (b.tipo) {
    case 'titulo':
      return <h3 id={b.id}>{b.texto}</h3>
    case 'paragrafo':
      return (
        <p style={{ fontWeight: b.estilo.negrito ? 600 : undefined, fontStyle: b.estilo.italico ? 'italic' : undefined, textDecoration: b.estilo.sublinhado ? 'underline' : undefined }}>
          {b.texto}
        </p>
      )
    case 'lista': {
      const Lista = b.numerada ? 'ol' : 'ul'
      return (
        <Lista className="processo-lista">
          {b.itens.map((it, i) => (
            <li key={i}>{it}</li>
          ))}
        </Lista>
      )
    }
    case 'codigo':
      return (
        <pre className="processo-codigo">
          <code>{b.texto}</code>
        </pre>
      )
    case 'aviso':
      return (
        <div className={`processo-aviso processo-aviso-${b.nivel}`} role="note">
          {b.texto}
        </div>
      )
    case 'imagem':
      // As imagens ficam na pasta do processo no servidor; a web ainda não as entrega (pendência dos anexos).
      return (
        <figure className="processo-figura">
          <div role="img" aria-label={b.legenda} className="processo-imagem" title={b.arquivo}>
            Imagem do processo
          </div>
          <figcaption>{b.legenda}</figcaption>
        </figure>
      )
    case 'link':
      return (
        <a className="processo-link" href={b.url} target="_blank" rel="noreferrer noopener">
          ↗ {b.texto}
          <span className="processo-link-site">· {b.site}</span>
        </a>
      )
    case 'pasta':
      return (
        <span className="processo-pasta">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="#F2DDA4" stroke="#8A6A1F" strokeWidth="1.6" strokeLinejoin="round" aria-hidden="true">
            <path d="M3 7a2 2 0 0 1 2-2h4l2 2h8a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z" />
          </svg>
          <code>{b.caminho}</code>
          <button type="button" className="link-acao" onClick={() => aoCopiar(b.caminho)}>
            Copiar caminho
          </button>
        </span>
      )
    case 'passo':
      return (
        <div className="processo-passo">
          <b>
            {b.numero}. {b.titulo}
          </b>
          {b.descricao && <p>{b.descricao}</p>}
          {b.checklist.length > 0 && (
            <ul className="processo-lista">
              {b.checklist.map((c, i) => (
                <li key={i}>{c}</li>
              ))}
            </ul>
          )}
          {b.resultado && <span className="nota-campo">Resultado esperado: {b.resultado}</span>}
        </div>
      )
  }
}
