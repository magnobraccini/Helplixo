import { useEffect, useState, type ReactElement } from 'react'
import { CORES_TIPO, listarOcorrencias, removerMarcacao, type Ocorrencia, type QuemMarca } from '../api/calendario'
import { ErroDaApi } from '../api/cliente'
import { CabecalhoPagina } from '../leiaute/CabecalhoPagina'
import { isoData } from '../util/datas'
import { FormMarcacao } from './FormMarcacao'
import {
  AGENDAS,
  AUTOMATICOS,
  GRADE,
  MESES_CURTO,
  SEMANA_CURTA,
  VISOES,
  blocosDoDia,
  diaDe,
  diaLongo,
  diasDaVisao,
  diasDoMes,
  ehFimDeSemana,
  horasDaGrade,
  navegar,
  podeRemover,
  porHora,
  somarDias,
  textoHora,
  tituloDaVisao,
  type Visao,
} from './regras'
import '../chamados/chamados.css'
import '../inventario/inventario.css'
import './calendario.css'

type Props = {
  agora: number
  editando: boolean
  quem: QuemMarca
  avisar: (mensagem: string) => void
  /** Avisa o App que o calendário mudou (para o sino e o Início). */
  aoMudar: () => void
}

const cor = (tipo: string) => CORES_TIPO[tipo] ?? CORES_TIPO.Outro
type Selecao = { ocorrencia: Ocorrencia; dia: string }
type Novo = { dia: string; hora?: string }

export function TelaCalendario({ agora, editando, quem, avisar, aoMudar }: Props) {
  const hoje = isoData(new Date(agora))
  const [dia, setDia] = useState(hoje)
  const [visao, setVisao] = useState<Visao>('mes')
  const [ocultos, setOcultos] = useState<Set<string>>(new Set())
  const [fds, setFds] = useState(true)
  const [busca, setBusca] = useState('')
  const [carga, setCarga] = useState<{ chave: string; lista: Ocorrencia[] }>({ chave: '', lista: [] })
  const [erro, setErro] = useState('')
  const [novo, setNovo] = useState<Novo | null>(null)
  const [selecao, setSelecao] = useState<Selecao | null>(null)
  const [recarga, setRecarga] = useState(0)

  // Pede ao servidor só os dias que a visão mostra (e os próximos 15 dias da lateral).
  const dias = diasDaVisao(visao, dia)
  const de = [dias[0], hoje].sort()[0]
  const ate = [dias[dias.length - 1], somarDias(hoje, 15)].sort()[1]
  const chave = `${de}|${ate}|${recarga}`
  useEffect(() => {
    let ativo = true
    listarOcorrencias(de, ate)
      .then((lista) => ativo && (setCarga({ chave, lista }), setErro('')))
      .catch(() => ativo && setErro('Não foi possível carregar o calendário. Confira se o servidor está no ar.'))
    return () => {
      ativo = false
    }
  }, [de, ate, chave])

  // Agendas desligadas e a busca filtram aqui (a lateral "Próximos 15 dias" não filtra, para nada importante sumir).
  const q = busca.trim().toLowerCase()
  const todas = carga.lista
  const visiveis = todas.filter((o) => !ocultos.has(o.tipo) && (!q || `${o.titulo} ${o.descricao ?? ''} ${o.tipo}`.toLowerCase().includes(q)))
  const doDia = (iso: string, lista = visiveis) => lista.filter((o) => o.dia === iso).sort(porHora)
  const mostraDia = (iso: string) => fds || !ehFimDeSemana(iso)

  const abrir = (o: Ocorrencia, iso: string) => setSelecao({ ocorrencia: o, dia: iso })
  const marcar = (iso: string, hora?: string) => {
    setSelecao(null)
    setNovo({ dia: iso, hora })
  }
  const mudou = (mensagem: string) => {
    setNovo(null)
    setSelecao(null)
    setRecarga((n) => n + 1)
    aoMudar()
    avisar(mensagem)
  }
  const remover = async (o: Ocorrencia) => {
    try {
      await removerMarcacao(o.id, quem)
      mudou(`Removido do calendário: ${o.titulo}.`)
    } catch (falha) {
      avisar(falha instanceof ErroDaApi ? falha.erros.join(' ') : 'Não foi possível remover agora.')
    }
  }

  const chip = (o: Ocorrencia, iso: string, texto = (o.horaInicio ? `${o.horaInicio.slice(0, 5)} ` : '') + o.titulo) => {
    const [fundo, frente] = cor(o.tipo)
    return (
      <button
        key={`${o.id}-${iso}`}
        type="button"
        className="chip-cal"
        style={{ background: fundo, color: frente }}
        title={o.titulo}
        onClick={(e) => {
          e.stopPropagation()
          abrir(o, iso)
        }}
      >
        {texto}
      </button>
    )
  }

  // Próximos 15 dias (sem filtros), até 8 itens.
  const proximos: { quando: string; texto: string; cor: string }[] = []
  for (let i = 0; i <= 15 && proximos.length < 8; i++) {
    const iso = somarDias(hoje, i)
    for (const o of doDia(iso, todas)) {
      if (proximos.length < 8) proximos.push({ quando: i === 0 ? 'hoje' : iso.slice(8) + '/' + iso.slice(5, 7), texto: (o.horaInicio ? `${o.horaInicio.slice(0, 5)} ` : '') + o.titulo, cor: cor(o.tipo)[1] })
    }
  }

  return (
    <>
      <CabecalhoPagina titulo="Calendário" subtitulo="Feriados, manutenções, ausências, contratos e garantias. Expediente: 8h às 14h." />
      <div className="cal-barra">
        <button type="button" className="botao-contorno" onClick={() => setDia(hoje)}>
          Hoje
        </button>
        <button type="button" className="cal-seta" aria-label="Anterior" onClick={() => setDia(navegar(visao, dia, -1))}>
          ‹
        </button>
        <button type="button" className="cal-seta" aria-label="Seguinte" onClick={() => setDia(navegar(visao, dia, 1))}>
          ›
        </button>
        <h2 className="cal-titulo">{tituloDaVisao(visao, dia, fds)}</h2>
        <span className="espaco" />
        <label className="busca-processo cal-busca">
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" aria-hidden="true">
            <circle cx="11" cy="11" r="7" />
            <path d="M21 21l-4.3-4.3" />
          </svg>
          <input type="search" aria-label="Procurar no calendário" value={busca} placeholder="Procurar marcação" onChange={(e) => setBusca(e.target.value)} />
        </label>
        <div role="radiogroup" aria-label="Visão" className="abas-pilula">
          {VISOES.map((v) => (
            <button key={v.chave} type="button" role="radio" aria-checked={visao === v.chave} onClick={() => setVisao(v.chave)}>
              {v.nome}
            </button>
          ))}
        </div>
        <button type="button" className="botao-contorno" onClick={() => marcar(dia)}>
          Novo aviso
        </button>
        {editando && (
          <button type="button" className="botao botao-principal" onClick={() => marcar(dia)}>
            Nova marcação
          </button>
        )}
      </div>
      {q && <span className="nota-campo">{visiveis.length} marcação(ões) com “{busca.trim()}”</span>}
      {erro && (
        <p role="alert" className="caixa-erro">
          {erro}
        </p>
      )}
      {novo && <FormMarcacao key={`${novo.dia}-${novo.hora}`} dia={novo.dia} hora={novo.hora} editando={editando} quem={quem} aoSalvar={mudou} aoCancelar={() => setNovo(null)} />}

      <div className="cal-colunas">
        <aside className="cal-lateral">
          <section>
            <h2>Minhas agendas</h2>
            {AGENDAS.map((t) => (
              <label key={t} className="cal-agenda">
                <input
                  type="checkbox"
                  checked={!ocultos.has(t)}
                  style={{ accentColor: cor(t)[1] }}
                  onChange={() =>
                    setOcultos((s) => {
                      const n = new Set(s)
                      if (n.has(t)) n.delete(t)
                      else n.add(t)
                      return n
                    })
                  }
                />
                <span className="espaco">{t}</span>
                <span className="nota-campo">{todas.filter((o) => o.tipo === t).length}</span>
              </label>
            ))}
          </section>
          <section>
            <h2>Opções</h2>
            <label className="cal-agenda">
              <input type="checkbox" checked={fds} onChange={() => setFds(!fds)} /> Mostrar fins de semana
            </label>
            <span className="cal-expediente">
              <span aria-hidden="true" />
              Expediente 8h–14h (na Semana)
            </span>
          </section>
          <section aria-label="Dia selecionado">
            <h2 className="cal-dia-titulo">{diaLongo(dia)}</h2>
            {doDia(dia).map((o) => (
              <button key={o.id} type="button" className="cal-item-dia" onClick={() => abrir(o, dia)}>
                <span className="nota-campo" style={{ color: cor(o.tipo)[1] }}>
                  {o.tipo} · {textoHora(o)}
                </span>
                <span>{o.titulo}</span>
                {o.autor && <span className="nota-campo">por {o.autor} ({o.cargo})</span>}
              </button>
            ))}
            {doDia(dia).length === 0 && <span className="nota-campo">Nada marcado neste dia.</span>}
            {(
              <button type="button" className="link-acao cal-marcar" onClick={() => marcar(dia)}>
                + Marcar neste dia
              </button>
            )}
          </section>
          <section aria-label="Próximos 15 dias">
            <h2>Próximos 15 dias</h2>
            {proximos.map((p, i) => (
              <div key={i} className="cal-proximo">
                <span className="nota-campo">{p.quando}</span>
                <span className="cal-proximo-cor" style={{ background: p.cor }} />
                <span>{p.texto}</span>
              </div>
            ))}
            {proximos.length === 0 && <span className="nota-campo">Nada marcado.</span>}
          </section>
        </aside>

        <div className="cal-principal">
          {visao === 'mes' && (
            <div className="tabela-rolagem">
              <div role="grid" aria-label={tituloDaVisao('mes', dia, fds)} className="cal-mes" style={{ gridTemplateColumns: `repeat(${fds ? 7 : 5}, minmax(72px, 1fr))`, minWidth: fds ? 520 : 380 }}>
                {SEMANA_CURTA.filter((_, i) => fds || (i !== 0 && i !== 6)).map((c) => (
                  <span key={c} role="columnheader" className="cal-mes-cabecalho">
                    {c}
                  </span>
                ))}
                {diasDoMes(dia)
                  .filter((d) => mostraDia(d.iso))
                  .map((d) => {
                    const evs = doDia(d.iso)
                    const feriado = evs.some((o) => o.tipo === 'Feriado')
                    const classe = ['cal-mes-dia', d.foraDoMes && 'fora', feriado && 'feriado', ehFimDeSemana(d.iso) && 'fds', d.iso === dia && 'sel'].filter(Boolean).join(' ')
                    return (
                      <div key={d.iso} role="gridcell" aria-selected={d.iso === dia} aria-label={diaLongo(d.iso)} className={classe} onClick={() => setDia(d.iso)}>
                        <span className={d.iso === hoje ? 'cal-num hoje' : 'cal-num'}>{diaDe(d.iso).getDate()}</span>
                        {evs.slice(0, 3).map((o) => chip(o, d.iso))}
                        {evs.length > 3 && <span className="cal-mais">+{evs.length - 3} mais</span>}
                      </div>
                    )
                  })}
              </div>
            </div>
          )}

          {(visao === 'semana' || visao === 'dia') && (
            <GradeHoras dias={(visao === 'dia' ? [dia] : dias.filter(mostraDia))} hoje={hoje} agora={agora} doDia={doDia} chip={chip} aoEscolherDia={setDia} aoAbrir={abrir} aoMarcar={marcar} />
          )}

          {visao === 'agenda' && (
            <div className="cal-agenda-lista">
              {dias
                .map((iso) => ({ iso, evs: doDia(iso) }))
                .filter((x) => x.evs.length)
                .map(({ iso, evs }) => {
                  const d = diaDe(iso)
                  return (
                    <div key={iso} className="cal-agenda-dia">
                      <div className="cal-agenda-data">
                        <span className={iso === hoje ? 'cal-num grande hoje' : 'cal-num grande'}>{d.getDate()}</span>
                        <span className="cal-agenda-mes">
                          {MESES_CURTO[d.getMonth()]}, {SEMANA_CURTA[d.getDay()]}
                        </span>
                      </div>
                      <div className="cal-agenda-itens">
                        {evs.map((o) => (
                          <button key={o.id} type="button" className="cal-agenda-item" onClick={() => abrir(o, iso)}>
                            <span className="cal-ponto" style={{ background: cor(o.tipo)[1] }} />
                            <span className="cal-agenda-hora">{textoHora(o)}</span>
                            <span className="espaco">{o.titulo}</span>
                            <span className="nota-campo">{o.tipo}</span>
                          </button>
                        ))}
                      </div>
                    </div>
                  )
                })}
              {dias.every((iso) => doDia(iso).length === 0) && <p className="lista-vazia">Nada marcado nos próximos 30 dias com esses filtros.</p>}
            </div>
          )}
        </div>
      </div>

      {selecao && (
        <Detalhe selecao={selecao} podeRemover={podeRemover(selecao.ocorrencia, quem.cargo)} aoRemover={remover} aoFechar={() => setSelecao(null)} />
      )}
      <p className="legenda">
        Feriados, contratos e garantias entram sozinhos. Avisos podem ser lançados por qualquer cargo; quem está acima no mesmo setor pode remover o aviso
        de quem está abaixo. As outras marcações são lançadas no modo edição.
      </p>
    </>
  )
}

type PropsGrade = {
  dias: string[]
  hoje: string
  agora: number
  doDia: (iso: string) => Ocorrencia[]
  chip: (o: Ocorrencia, iso: string, texto?: string) => ReactElement
  aoEscolherDia: (iso: string) => void
  aoAbrir: (o: Ocorrencia, iso: string) => void
  aoMarcar?: (iso: string, hora?: string) => void
}

/** Semana e Dia: horas das 7h às 19h, com o expediente destacado; clicar num horário vazio cria uma marcação. */
function GradeHoras({ dias, hoje, agora, doDia, chip, aoEscolherDia, aoAbrir, aoMarcar }: PropsGrade) {
  const colunas = `52px repeat(${dias.length}, minmax(84px, 1fr))`
  const horas = horasDaGrade()
  const d = new Date(agora)
  const agoraMin = d.getHours() * 60 + d.getMinutes()
  return (
    <div className="tabela-rolagem">
      <div className="cal-grade">
        <div className="cal-grade-linha" style={{ gridTemplateColumns: colunas }}>
          <span />
          {dias.map((iso) => (
            <button key={iso} type="button" className={iso === hoje ? 'cal-grade-dia hoje' : 'cal-grade-dia'} onClick={() => aoEscolherDia(iso)}>
              {SEMANA_CURTA[diaDe(iso).getDay()]} {diaDe(iso).getDate()}
            </button>
          ))}
        </div>
        <div className="cal-grade-linha cal-grade-inteiro" style={{ gridTemplateColumns: colunas }}>
          <span className="cal-grade-rotulo">dia inteiro</span>
          {dias.map((iso) => (
            <div key={iso} className="cal-grade-inteiro-dia">
              {doDia(iso)
                .filter((o) => !o.horaInicio)
                .map((o) => chip(o, iso, o.titulo))}
            </div>
          ))}
        </div>
        <div className="cal-grade-corpo" style={{ gridTemplateColumns: colunas }}>
          <div className="cal-grade-horas">
            {horas.map((h) => (
              <span key={h} style={{ height: GRADE.px }}>
                {h}
              </span>
            ))}
          </div>
          {dias.map((iso) => {
            const altura = (GRADE.fim - GRADE.inicio) * GRADE.px
            return (
              <div key={iso} className="cal-grade-coluna" style={{ height: altura }} aria-label={`Horários de ${diaLongo(iso)}`}>
                <span aria-hidden="true" className="cal-expediente-faixa" style={{ top: (8 - GRADE.inicio) * GRADE.px, height: 6 * GRADE.px }} />
                {aoMarcar &&
                  horas.map((h, i) => (
                    <button
                      key={h}
                      type="button"
                      className="cal-slot"
                      style={{ top: i * GRADE.px, height: GRADE.px }}
                      aria-label={`Marcar ${SEMANA_CURTA[diaDe(iso).getDay()]} ${diaDe(iso).getDate()} às ${h.slice(0, 2)}h`}
                      onClick={() => aoMarcar(iso, h)}
                    />
                  ))}
                {blocosDoDia(doDia(iso)).map((b) => {
                  const [fundo, frente] = cor(b.ocorrencia.tipo)
                  return (
                    <button
                      key={b.ocorrencia.id}
                      type="button"
                      className="cal-bloco"
                      title={b.ocorrencia.titulo}
                      style={{ top: b.topo, height: b.altura, left: `${b.esquerda}%`, width: `calc(${b.largura}% - 4px)`, background: fundo, color: frente, borderLeftColor: frente }}
                      onClick={() => aoAbrir(b.ocorrencia, iso)}
                    >
                      <b>{b.ocorrencia.titulo}</b>
                      <span>{textoHora(b.ocorrencia)}</span>
                    </button>
                  )
                })}
                {iso === hoje && agoraMin >= GRADE.inicio * 60 && agoraMin < GRADE.fim * 60 && (
                  <span aria-hidden="true" className="cal-agora" style={{ top: ((agoraMin - GRADE.inicio * 60) / 60) * GRADE.px }} />
                )}
              </div>
            )
          })}
        </div>
      </div>
    </div>
  )
}

function Detalhe({ selecao, podeRemover: pode, aoRemover, aoFechar }: { selecao: Selecao; podeRemover: boolean; aoRemover: (o: Ocorrencia) => void; aoFechar: () => void }) {
  const o = selecao.ocorrencia
  const [fundo, frente] = cor(o.tipo)
  useEffect(() => {
    const tecla = (e: KeyboardEvent) => e.key === 'Escape' && aoFechar()
    document.addEventListener('keydown', tecla)
    return () => document.removeEventListener('keydown', tecla)
  }, [aoFechar])
  return (
    <div className="fundo-dialogo" onClick={aoFechar}>
      <div role="dialog" aria-modal="true" aria-label={o.titulo} className="dialogo cal-detalhe" onClick={(e) => e.stopPropagation()}>
        <div className="dialogo-topo cal-detalhe-topo">
          <span className="cal-detalhe-cor" style={{ background: frente }} />
          <div className="espaco cal-detalhe-textos">
            <h2 className="dialogo-titulo">{o.titulo}</h2>
            <span className="nota-campo">
              {diaLongo(selecao.dia)} · {textoHora(o)}
            </span>
            {o.repeteTexto && <span className="nota-campo">↻ {o.repeteTexto}</span>}
          </div>
          <button type="button" className="dialogo-fechar" aria-label="Fechar" onClick={aoFechar}>
            ×
          </button>
        </div>
        <span className="pilula-status cal-detalhe-tipo" style={{ background: fundo, color: frente }}>
          {o.tipo}
          {o.urgente ? ' · urgente' : ''}
        </span>
        {o.descricao && <p className="dialogo-texto">{o.descricao}</p>}
        {o.autor && (
          <span className="nota-campo">
            por {o.autor} ({o.cargo})
          </span>
        )}
        {o.automatica && <span className="nota-campo">Entrou sozinho: {AUTOMATICOS[o.tipo] ?? 'automático'}.</span>}
        {pode && (
          <div className="botoes">
            <button type="button" className="botao-contorno botao-perigo" onClick={() => aoRemover(o)}>
              Remover
            </button>
          </div>
        )}
      </div>
    </div>
  )
}
