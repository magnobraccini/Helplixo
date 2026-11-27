import { Fragment, useState } from 'react'
import type { Chamado } from '../api/chamados'
import { fmt, fmtData, instante, isoHora } from '../util/datas'
import type { Filtro, FiltroStatus } from './estado'
import { CORES_STATUS, PERIODOS, minutosDoChamado, reincidencias, secretariaDe, type Periodo } from './regras'

const ABAS_STATUS: { chave: FiltroStatus; nome: string }[] = [
  { chave: 'Aberto', nome: 'Abertos' },
  { chave: 'Em andamento', nome: 'Em andamento' },
  { chave: 'Fechado', nome: 'Fechados' },
  { chave: 'Todos', nome: 'Todos' },
]

type Props = {
  filtro: Filtro
  aoMudarFiltro: (f: Filtro) => void
  /** Todos os chamados do período (todos os status). */
  doPeriodo: Chamado[]
  /** Chamados dos últimos 7 dias, para o ↺. */
  recentes: Chamado[]
  textoPeriodo: string
  carregando: boolean
  erro: string
  tecnicos: string[]
  agora: number
  aoVerDetalhes: (numero: string) => void
  aoAtualizar: (numero: string) => void
  aoAbrirMaquina: (nSemin: string) => void
}

export function ListaChamados(p: Props) {
  const { filtro } = p
  const [expandidos, setExpandidos] = useState<Record<string, boolean>>({})
  const mudar = (parcial: Partial<Filtro>) => p.aoMudarFiltro({ ...filtro, ...parcial })

  // Filtros que não dependem do servidor: secretaria, técnico e busca.
  const busca = filtro.busca.trim().toLowerCase()
  const filtrados = p.doPeriodo.filter(
    (c) =>
      (!filtro.secretaria || secretariaDe(c.setor) === filtro.secretaria) &&
      (!filtro.tecnico || (filtro.tecnico === '—' ? !c.tecnico : c.tecnico === filtro.tecnico)) &&
      (!busca || `${c.numero} ${c.nSemin ?? ''} ${c.contato} ${c.setor} ${c.problema}`.toLowerCase().includes(busca)),
  )
  const visiveis = filtrados.filter((c) => filtro.status === 'Todos' || c.status === filtro.status)
  const contar = (s: FiltroStatus) => (s === 'Todos' ? filtrados.length : filtrados.filter((c) => c.status === s).length)

  const secretarias = [...new Set(p.doPeriodo.map((c) => secretariaDe(c.setor)))].sort()
  const nc = visiveis.filter((c) => c.naoComputavel).length
  const resumo =
    `${visiveis.length} chamado(s) ${p.textoPeriodo}` +
    (nc > 0 ? ` · ${visiveis.length - nc} contam nas estatísticas, ${nc} fora (não computável).` : ' · todos contam nas estatísticas.')

  const alternar = (numero: string) => setExpandidos((e) => ({ ...e, [numero]: !e[numero] }))

  return (
    <>
      <div className="chamados-barra">
        <div role="tablist" aria-label="Status" className="abas-pilula">
          {ABAS_STATUS.map((a) => (
            <button key={a.chave} type="button" role="tab" aria-selected={filtro.status === a.chave} onClick={() => mudar({ status: a.chave })}>
              {a.nome} <span className="abas-pilula-conta">{contar(a.chave)}</span>
            </button>
          ))}
        </div>
      </div>

      <div className="filtros">
        <label>
          Período
          <select value={filtro.periodo} onChange={(e) => mudar({ periodo: e.target.value as Periodo })}>
            {PERIODOS.map((o) => (
              <option key={o.valor} value={o.valor}>
                {o.rotulo}
              </option>
            ))}
          </select>
        </label>
        {filtro.periodo === 'custom' && (
          <>
            <label>
              De
              <input type="date" value={filtro.de} onChange={(e) => mudar({ de: e.target.value })} />
            </label>
            <label>
              Até
              <input type="date" value={filtro.ate} onChange={(e) => mudar({ ate: e.target.value })} />
            </label>
          </>
        )}
        <label>
          Secretaria
          <select value={filtro.secretaria} onChange={(e) => mudar({ secretaria: e.target.value })}>
            <option value="">Todas</option>
            {secretarias.map((s) => (
              <option key={s} value={s}>
                {s}
              </option>
            ))}
          </select>
        </label>
        <label>
          Técnico
          <select value={filtro.tecnico} onChange={(e) => mudar({ tecnico: e.target.value })}>
            <option value="">Todos</option>
            <option value="—">Sem técnico</option>
            {p.tecnicos.map((t) => (
              <option key={t} value={t}>
                {t}
              </option>
            ))}
          </select>
        </label>
        <label>
          Procurar
          <input type="search" value={filtro.busca} onChange={(e) => mudar({ busca: e.target.value })} placeholder="Nº, Semin, contato…" />
        </label>
      </div>

      {p.erro && (
        <p role="alert" className="caixa-erro">
          {p.erro}
        </p>
      )}

      <div className="chamados-resumo">
        <span role="status">{p.carregando ? 'Carregando chamados…' : resumo}</span>
      </div>

      <div className="tabela-rolagem">
        <table className="tabela-chamados">
          <thead>
            <tr>
              <th>Nº</th>
              <th>Nº Semin</th>
              <th>Setor - Secretaria</th>
              <th>Contato</th>
              <th>Problema</th>
              <th>Técnico</th>
              <th>Status</th>
              <th>Data</th>
              <th>Hora</th>
              <th className="col-min" title="Minutos de espera (abertos) ou de atendimento (fechados)">
                Min.
              </th>
              <th>
                <span className="so-leitor">Ações</span>
              </th>
            </tr>
          </thead>
          <tbody>
            {visiveis.map((c) => {
              const aberto = !!expandidos[c.numero]
              const min = minutosDoChamado(c, p.agora)
              const reinc = reincidencias(c, p.recentes, p.agora)
              const ultima = c.andamento.at(-1)
              const abertoEm = instante(c.abertoEm)
              const cores = CORES_STATUS[c.status]
              return (
                <Fragment key={c.numero}>
                  <tr className="linha-chamado" onClick={() => alternar(c.numero)} title={`Mostrar ou esconder a descrição do chamado ${c.numero}`}>
                    <td className="nowrap">
                      <button
                        type="button"
                        className="numero-chamado"
                        aria-expanded={aberto}
                        aria-label={`Descrição do chamado ${c.numero}`}
                        onClick={(e) => {
                          e.stopPropagation()
                          alternar(c.numero)
                        }}
                      >
                        <span aria-hidden="true" className="seta">
                          {aberto ? '▾' : '▸'}
                        </span>
                        {c.numero}
                      </button>
                      {c.naoComputavel && (
                        <span className="etiqueta-nc" title="Não computável: fica fora das estatísticas">
                          NC
                        </span>
                      )}
                    </td>
                    <td className="nowrap" onClick={(e) => e.stopPropagation()}>
                      {c.nSemin ? (
                        <button type="button" className="link-semin" title={`Abrir a ficha de ${c.nSemin}`} onClick={() => p.aoAbrirMaquina(c.nSemin!)}>
                          {c.nSemin}
                        </button>
                      ) : (
                        <span className="sem-maquina">sem máquina</span>
                      )}
                      {reinc > 0 && (
                        <span
                          className="etiqueta-reinc"
                          title={`Esta máquina teve ${reinc} outro(s) chamado(s) nos últimos 7 dias`}
                          aria-label={`Esta máquina teve ${reinc} outro(s) chamado(s) nos últimos 7 dias`}
                        >
                          ↺ {reinc}
                        </span>
                      )}
                    </td>
                    <td>{c.setor}</td>
                    <td>{c.contato}</td>
                    <td className="apagado">{c.problema}</td>
                    <td>{c.tecnico || '—'}</td>
                    <td className="nowrap">
                      <span className="pilula-status" style={{ background: cores.fundo, color: cores.texto }}>
                        {c.status}
                      </span>
                    </td>
                    <td className="nowrap apagado">{fmtData(abertoEm)}</td>
                    <td className="nowrap apagado">{isoHora(new Date(abertoEm))}</td>
                    <td className="col-min" style={{ color: min.cor }} title={min.rotulo}>
                      {min.minutos}
                    </td>
                    <td className="nowrap acoes" onClick={(e) => e.stopPropagation()}>
                      <button type="button" className="botao-pequeno" onClick={() => p.aoVerDetalhes(c.numero)}>
                        Ver detalhes
                      </button>
                      <button type="button" className="botao-pequeno botao-pequeno-principal" onClick={() => p.aoAtualizar(c.numero)}>
                        Atualizar
                      </button>
                    </td>
                  </tr>
                  {aberto && (
                    <tr className="linha-descricao">
                      <td colSpan={11}>
                        <div className="descricao-cartao">
                          <p className="descricao-texto">{c.descricao}</p>
                          {c.observacao && <p className="descricao-obs">Observação: {c.observacao}</p>}
                          {ultima && (
                            <p className="descricao-ultima">
                              <b>Última atualização</b> · {fmt(instante(ultima.em))} · {ultima.tecnico}: {ultima.texto}
                            </p>
                          )}
                        </div>
                      </td>
                    </tr>
                  )}
                </Fragment>
              )
            })}
          </tbody>
        </table>
        {!p.carregando && visiveis.length === 0 && <p className="lista-vazia">Nenhum chamado com esses filtros neste período.</p>}
      </div>
      <p className="legenda">
        Min.: tempo de espera dos chamados abertos (verde até 2 h, amarelo até 3 h, vermelho acima). Em andamento fica em azul: já está com alguém e não
        conta atraso. Fechados mostram o tempo de atendimento.
      </p>
    </>
  )
}
