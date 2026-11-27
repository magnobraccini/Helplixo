import { useEffect, useState } from 'react'
import { listarOcorrencias, type Ocorrencia } from '../api/calendario'
import { listarChamados, type Chamado, type StatusChamado } from '../api/chamados'
import { DIA, inicioDoDia, isoData } from '../util/datas'
import type { Recente } from '../util/recentes'
import {
  avisoUrgente,
  dataPorExtenso,
  itemDaAgenda,
  meusChamados,
  montarKpis,
  precisaDeAtencao,
  quandoAbriu,
  saudacao,
  type ItemAgenda,
  type LinhaChamado,
} from './painel'
import './inicio.css'

type Props = {
  perfil: string
  /** Nome de quem está usando (para "Bom dia, …" e "com você"). */
  eu: string
  agora: number
  recentes: Recente[]
  aoAbrirChamado: (numero: string) => void
  aoAbrirLista: (status: StatusChamado) => void
  aoIrCalendario: () => void
  aoAbrirRecente: (r: Recente) => void
}

type Dados = { emAberto: Chamado[]; fechados: Chamado[]; agenda: Ocorrencia[] }

/** Desde quando procurar chamados em aberto: todos, mesmo os muito antigos. */
const DESDE_SEMPRE = '2000-01-01'

export function TelaInicio(p: Props) {
  const [dados, setDados] = useState<Dados | null>(null)
  const [erro, setErro] = useState('')

  const hoje = isoData(new Date(p.agora))
  useEffect(() => {
    let ativo = true
    const hoje0 = inicioDoDia(Date.now())
    const dia = (n: number) => isoData(new Date(hoje0 + n * DIA))
    Promise.all([
      listarChamados(DESDE_SEMPRE, hoje, 'Aberto'),
      listarChamados(DESDE_SEMPRE, hoje, 'Em andamento'),
      // Fechados recentes, para "Fechados hoje" e a média de 30 dias (alguns foram abertos antes dos 30 dias).
      listarChamados(dia(-60), hoje, 'Fechado'),
      // Avisos ficam no dia em que foram marcados; o urgente da última semana ou das próximas duas aparece no topo.
      listarOcorrencias(dia(-7), dia(15)),
    ])
      .then(([abertos, andamento, fechados, agenda]) => ativo && setDados({ emAberto: [...abertos, ...andamento], fechados, agenda }))
      .catch(() => ativo && setErro('Não foi possível carregar o painel. Confira se o servidor está no ar.'))
    return () => {
      ativo = false
    }
  }, [hoje])

  const amanha = isoData(new Date(inicioDoDia(p.agora) + DIA))
  const agendaHoje = (dados?.agenda ?? []).filter((o) => o.dia === hoje).map(itemDaAgenda)
  const agendaAmanha = (dados?.agenda ?? []).filter((o) => o.dia === amanha).slice(0, 3).map(itemDaAgenda)
  const urgente = dados ? avisoUrgente(dados.agenda) : null
  const kpis = dados ? montarKpis(dados.emAberto, dados.fechados, p.eu, p.agora) : []
  const atencao = dados ? precisaDeAtencao(dados.emAberto, p.agora) : []
  const meus = meusChamados(dados?.emAberto ?? [], p.perfil, p.eu, p.agora)

  return (
    <>
      <div className="inicio-topo">
        <span className="inicio-data">{dataPorExtenso(p.agora)}</span>
        <h1 className="inicio-saudacao">
          {saudacao(p.agora)}, {p.eu}
        </h1>
        <span className="inicio-resumo">
          {dados ? `${dados.emAberto.length} chamado(s) em aberto · ${agendaHoje.length} marcação(ões) hoje` : erro || 'Carregando…'}
        </span>
      </div>

      {erro && (
        <p role="alert" className="caixa-erro">
          {erro}
        </p>
      )}

      {urgente && (
        <button type="button" className="aviso-urgente" onClick={p.aoIrCalendario}>
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
            <path d="M10.3 3.9L1.8 18a2 2 0 0 0 1.7 3h17a2 2 0 0 0 1.7-3L13.7 3.9a2 2 0 0 0-3.4 0z" />
            <path d="M12 9v4M12 17h.01" />
          </svg>
          <span className="aviso-urgente-texto">
            <b>Aviso urgente:</b> {urgente.titulo}
          </span>
          <span className="aviso-urgente-link">ver no calendário ›</span>
        </button>
      )}

      <div className="cards">
        {kpis.map((k) => (
          <button
            key={k.rotulo}
            type="button"
            className="card"
            onClick={() => (k.acao.tipo === 'chamado' ? p.aoAbrirChamado(k.acao.numero) : p.aoAbrirLista(k.acao.status))}
          >
            <span className="card-rotulo">{k.rotulo}</span>
            <b className="card-valor">{k.valor}</b>
            <span className="card-detalhe">{k.detalhe}</span>
          </button>
        ))}
      </div>

      <div className="inicio-colunas">
        <div className="inicio-coluna-principal">
          <section className="bloco" aria-labelledby="titulo-atencao">
            <div className="bloco-topo">
              <h2 id="titulo-atencao">Precisa de atenção</h2>
              <span className="bloco-nota">mais antigos e urgentes primeiro</span>
              <span className="espaco" />
              <button type="button" className="link-acao" onClick={() => p.aoAbrirLista('Aberto')}>
                Todos os chamados
              </button>
            </div>
            {atencao.map((l) => (
              <LinhaPainel key={l.numero} linha={l} aoAbrir={p.aoAbrirChamado} />
            ))}
            {dados && atencao.length === 0 && <p className="bloco-vazio">Nenhum chamado esperando. 🎉</p>}
          </section>

          <section className="bloco" aria-labelledby="titulo-meus">
            <h2 id="titulo-meus" className="bloco-titulo">
              {meus.titulo}
            </h2>
            {meus.lista.map((l) => (
              <LinhaPainel key={l.numero} linha={l} aoAbrir={p.aoAbrirChamado} />
            ))}
            {dados && meus.lista.length === 0 && <p className="bloco-vazio">{meus.vazio}</p>}
          </section>
        </div>

        <div className="inicio-coluna-lateral">
          <section className="bloco bloco-agenda" aria-labelledby="titulo-hoje">
            <div className="bloco-topo">
              <h2 id="titulo-hoje">Hoje</h2>
              <span className="espaco" />
              <button type="button" className="link-acao" onClick={p.aoIrCalendario}>
                Calendário
              </button>
            </div>
            {agendaHoje.map((a) => (
              <ItemAgendaPainel key={`${a.id}-${a.dia}`} item={a} aoAbrir={p.aoIrCalendario} />
            ))}
            {dados && agendaHoje.length === 0 && <span className="bloco-vazio">Nada marcado para hoje.</span>}
            {agendaAmanha.length > 0 && (
              <>
                <span className="agenda-amanha">Amanhã</span>
                {agendaAmanha.map((a) => (
                  <ItemAgendaPainel key={`${a.id}-${a.dia}`} item={a} aoAbrir={p.aoIrCalendario} pequeno />
                ))}
              </>
            )}
          </section>

          <section className="bloco bloco-agenda" aria-labelledby="titulo-recentes">
            <h2 id="titulo-recentes" className="bloco-titulo">
              Continue de onde parou
            </h2>
            {p.recentes.map((r) => (
              <button key={`${r.tipo}-${r.id}`} type="button" className="recente" onClick={() => p.aoAbrirRecente(r)}>
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinejoin="round" aria-hidden="true">
                  <path d="M14 3H6a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V9z" />
                  <path d="M14 3v6h6" />
                </svg>
                <span className="recente-texto">
                  <span className="recente-titulo">{r.titulo}</span>
                  <span className="recente-local">
                    {r.local} · {quandoAbriu(r.quando, p.agora)}
                  </span>
                </span>
              </button>
            ))}
            {p.recentes.length === 0 && <span className="bloco-vazio">O que você abrir aparece aqui para voltar depressa.</span>}
          </section>
        </div>
      </div>
    </>
  )
}

function LinhaPainel({ linha, aoAbrir }: { linha: LinhaChamado; aoAbrir: (numero: string) => void }) {
  return (
    <button type="button" className="linha-painel" onClick={() => aoAbrir(linha.numero)}>
      <span aria-hidden="true" className="linha-painel-ponto" style={{ background: linha.cor }} />
      <span className="linha-painel-meio">
        <span className="linha-painel-titulo">
          <b>{linha.numero}</b>
          <span>{linha.titulo}</span>
        </span>
        <span className="linha-painel-onde">
          {linha.onde} · {linha.tecnico}
        </span>
      </span>
      <span className="linha-painel-espera">
        <b>{linha.espera}</b>
        {linha.estado}
      </span>
    </button>
  )
}

function ItemAgendaPainel({ item, aoAbrir, pequeno }: { item: ItemAgenda; aoAbrir: () => void; pequeno?: boolean }) {
  return (
    <button type="button" className={pequeno ? 'item-agenda item-agenda-pequeno' : 'item-agenda'} onClick={aoAbrir}>
      <span className="item-agenda-hora">{item.hora}</span>
      <span className="item-agenda-cor" style={{ background: item.cor }} />
      <span className="item-agenda-titulo">{item.titulo}</span>
    </button>
  )
}
