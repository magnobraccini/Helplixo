import type { ReactNode } from 'react'
import type { Chamado } from '../api/chamados'
import { duracao, fmt, fmtData, instante, isoHora } from '../util/datas'
import type { DadosFormulario } from './estado'
import { FormResultado } from './FormResultado'
import { HistoricoMaquina } from './HistoricoMaquina'
import { CORES_STATUS, coresUrgencia } from './regras'

export type AbaChamado = 'dados' | 'resultado' | 'historico'

const ABAS: { chave: AbaChamado; nome: string }[] = [
  { chave: 'dados', nome: 'Dados' },
  { chave: 'resultado', nome: 'Resultado' },
  { chave: 'historico', nome: 'Histórico da máquina' },
]

type Props = {
  chamado: Chamado
  aba: AbaChamado
  aoTrocarAba: (aba: AbaChamado) => void
  aoVoltar: () => void
  formulario: DadosFormulario
  aoMudarFormulario: (d: DadosFormulario) => void
  aoSalvar: (salvo: Chamado, mensagem: string) => void
  aoAbrirChamado: (numero: string) => void
  aoAbrirMaquina: (nSemin: string) => void
  tecnicos: string[]
  agora: number
}

export function FichaChamado(p: Props) {
  const c = p.chamado
  const abertoEm = instante(c.abertoEm)
  const st = CORES_STATUS[c.status]
  const urg = coresUrgencia(c.urgencia)

  return (
    <>
      <div className="ficha-barra">
        <button type="button" className="link-voltar" onClick={p.aoVoltar}>
          ← Voltar para Chamados
        </button>
      </div>

      <div role="tablist" aria-label="Chamado" className="abas-linha">
        {ABAS.map((a) => (
          <button key={a.chave} type="button" role="tab" aria-selected={p.aba === a.chave} onClick={() => p.aoTrocarAba(a.chave)}>
            {a.nome}
          </button>
        ))}
      </div>

      <div role="tabpanel" className="ficha-corpo">
        {p.aba === 'dados' && (
          <>
            <div className="ficha-selos">
              <span className="numero-grande">Nº {c.numero}</span>
              <span className="pilula-status" style={{ background: st.fundo, color: st.texto }}>
                {c.status}
              </span>
              <span className="pilula-status" style={{ background: urg.fundo, color: urg.texto }}>
                {c.urgencia}
              </span>
              {c.naoComputavel && (
                <span className="pilula-status etiqueta-cinza" title="Fica fora das estatísticas">
                  Não computável
                </span>
              )}
            </div>
            <h2 className="ficha-problema">{c.problema}</h2>
            <p className="ficha-descricao">{c.descricao}</p>
            <dl className="propriedades">
              <Propriedade nome="Aberto em">{`${fmtData(abertoEm)} às ${isoHora(new Date(abertoEm))}`}</Propriedade>
              <Propriedade nome="Máquina">
                {c.nSemin ? (
                  <button type="button" className="link-semin" title="Abrir a ficha da máquina" onClick={() => p.aoAbrirMaquina(c.nSemin!)}>
                    {c.nSemin}
                  </button>
                ) : (
                  'Sem máquina'
                )}
              </Propriedade>
              <Propriedade nome="Setor">{c.setor}</Propriedade>
              <Propriedade nome="Contato(s)">{c.contato}</Propriedade>
              <Propriedade nome="Técnico">{c.tecnico || 'Ninguém ainda'}</Propriedade>
              <Propriedade nome="Área · Ocorrência">{`${c.area} · ${c.ocorrencia}`}</Propriedade>
            </dl>
            <p className="apagado-forte">
              <span className="apagado">Observação: </span>
              {c.observacao || '—'}
            </p>
          </>
        )}

        {p.aba === 'resultado' && (
          <>
            <section className="andamento">
              <h2>Andamento</h2>
              {c.andamento.map((a, i) => (
                <div key={i} className="andamento-item">
                  <span className="andamento-trilho" aria-hidden="true">
                    <span className="andamento-ponto" />
                    <span className="andamento-linha" />
                  </span>
                  <span className="andamento-texto">
                    <span>{a.texto}</span>
                    <span className="nota-campo">
                      {fmt(instante(a.em))} · {a.tecnico}
                    </span>
                  </span>
                </div>
              ))}
              {c.andamento.length === 0 && <span className="nota-campo">Nenhuma atualização ainda.</span>}
            </section>

            {c.status === 'Fechado' && c.fechamento && (
              <section className="atendido">
                <h2>Atendido</h2>
                <p>{c.fechamento.descricao}</p>
                <div className="atendido-grade">
                  <Info nome="Atendido em" valor={fmt(instante(c.fechamento.em))} />
                  <Info nome="Técnico" valor={c.fechamento.tecnico} />
                  <Info nome="Contato · setor" valor={`${c.contato} · ${c.setor}`} />
                  <Info nome="Tempo de atendimento" valor={duracao(instante(c.fechamento.em) - abertoEm)} />
                </div>
              </section>
            )}

            <FormResultado
              chamado={c}
              dados={p.formulario}
              aoMudar={p.aoMudarFormulario}
              tecnicos={p.tecnicos}
              agora={p.agora}
              aoSalvar={p.aoSalvar}
            />
          </>
        )}

        {p.aba === 'historico' && <HistoricoMaquina key={c.numero} numero={c.numero} nSemin={c.nSemin || null} aoAbrirChamado={p.aoAbrirChamado} />}
      </div>
    </>
  )
}

function Propriedade({ nome, children }: { nome: string; children: ReactNode }) {
  return (
    <div className="propriedade">
      <dt>{nome}</dt>
      <dd>{children}</dd>
    </div>
  )
}

function Info({ nome, valor }: { nome: string; valor: string }) {
  return (
    <span className="atendido-info">
      <span>{nome}</span>
      <b>{valor}</b>
    </span>
  )
}
