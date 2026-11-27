import { useEffect, useState } from 'react'
import { listarExternos, listarInternos, type FiltroExternos, type FiltroInternos, type ListaExternos, type ListaInternos, type SituacaoContrato } from '../api/contatos'
import { CabecalhoPagina } from '../leiaute/CabecalhoPagina'
import { FormContato } from './FormContato'
import '../chamados/chamados.css'
import '../inventario/inventario.css'
import './contatos.css'

type Props = { editando: boolean; chave: string | null; avisar: (mensagem: string) => void }
type Visao = 'internos' | 'externos'

const CORES_CONTRATO: Record<SituacaoContrato, [string, string]> = {
  vencido: ['var(--c-fdebec)', 'var(--c-8a2a27)'],
  vence: ['var(--c-fbf3db)', 'var(--c-6b4f0e)'],
  emdia: ['var(--c-edf3ec)', 'var(--c-2f5a2b)'],
}

const INTERNOS_INICIAL: Omit<FiltroInternos, 'q'> = { secretaria: '', andar: '', ordem: 'nome' }
const EXTERNOS_INICIAL: Omit<FiltroExternos, 'q'> = { categoria: '', situacao: '' }

/** Internos (ramais) e Externos (fornecedores e contratos), trocando com um clique. Os filtros rodam no servidor. */
export function TelaContatos({ editando, chave, avisar }: Props) {
  const [visao, setVisao] = useState<Visao>('internos')
  const [q, setQ] = useState('')
  const [fi, setFi] = useState(INTERNOS_INICIAL)
  const [fx, setFx] = useState(EXTERNOS_INICIAL)
  const [internos, setInternos] = useState<ListaInternos | null>(null)
  const [externos, setExternos] = useState<ListaExternos | null>(null)
  const [erro, setErro] = useState('')
  const [novo, setNovo] = useState(false)
  const [recarga, setRecarga] = useState(0)

  const falhou = () => setErro('Não foi possível carregar os contatos. Confira se o servidor está no ar.')
  useEffect(() => {
    let ativo = true
    listarInternos({ ...fi, q: q.trim() })
      .then((r) => ativo && (setInternos(r), setErro('')))
      .catch(() => ativo && falhou())
    return () => {
      ativo = false
    }
  }, [fi, q, recarga])
  useEffect(() => {
    let ativo = true
    listarExternos({ ...fx, q: q.trim() })
      .then((r) => ativo && (setExternos(r), setErro('')))
      .catch(() => ativo && falhou())
    return () => {
      ativo = false
    }
  }, [fx, q, recarga])

  const filtrosAtivos = (visao === 'internos' ? [fi.secretaria, fi.andar] : [fx.categoria, fx.situacao]).filter(Boolean).length + (q ? 1 : 0)
  const info =
    visao === 'internos'
      ? internos && `${internos.total} de ${internos.deTotal} ramais`
      : externos && `${externos.total} de ${externos.deTotal} contatos externos`

  return (
    <>
      <CabecalhoPagina
        titulo="Contatos"
        subtitulo={visao === 'externos' ? 'Fornecedores, prestadores de serviço e contratos.' : 'Ramais da prefeitura, por setor.'}
      />
      <div className="contatos-barra">
        <div role="tablist" aria-label="Tipo de contato" className="abas-pilula">
          {(
            [
              ['internos', 'Internos · ramais', internos?.deTotal],
              ['externos', 'Externos', externos?.deTotal],
            ] as const
          ).map(([chaveVisao, nome, n]) => (
            <button key={chaveVisao} type="button" role="tab" aria-selected={visao === chaveVisao} onClick={() => (setVisao(chaveVisao), setNovo(false))}>
              {nome} <span className="abas-pilula-conta">{n ?? ''}</span>
            </button>
          ))}
        </div>
        <label className="busca-processo">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" aria-hidden="true">
            <circle cx="11" cy="11" r="7" />
            <path d="M21 21l-4.3-4.3" />
          </svg>
          <input
            type="search"
            aria-label="Filtrar contatos"
            value={q}
            onChange={(e) => setQ(e.target.value)}
            placeholder={visao === 'externos' ? 'Filtrar por empresa, serviço ou telefone' : 'Filtrar por nome, setor ou ramal'}
          />
        </label>
        <span className="espaco" />
        {editando && (
          <button type="button" className="botao-edicao" onClick={() => setNovo(true)}>
            Novo contato
          </button>
        )}
      </div>

      {novo && editando && (
        <FormContato
          key={visao}
          tipo={visao}
          chave={chave}
          categorias={externos?.categorias ?? []}
          aoSalvar={(m) => {
            setNovo(false)
            setRecarga((n) => n + 1) // o contato novo e as opções dos filtros aparecem na hora
            avisar(m)
          }}
          aoCancelar={() => setNovo(false)}
        />
      )}

      <div role="group" aria-label="Filtros" className="filtros-contatos">
        {visao === 'internos' ? (
          <>
            <label>
              Secretaria
              <select value={fi.secretaria} onChange={(e) => setFi({ ...fi, secretaria: e.target.value })}>
                <option value="">Todas</option>
                {internos?.opcoes.secretarias.map((o) => (
                  <option key={o}>{o}</option>
                ))}
              </select>
            </label>
            <label>
              Andar
              <select value={fi.andar} onChange={(e) => setFi({ ...fi, andar: e.target.value })}>
                <option value="">Todos</option>
                {internos?.opcoes.andares.map((o) => (
                  <option key={o}>{o}</option>
                ))}
              </select>
            </label>
            <label>
              Ordenar por
              <select value={fi.ordem} onChange={(e) => setFi({ ...fi, ordem: e.target.value as FiltroInternos['ordem'] })}>
                <option value="nome">Nome</option>
                <option value="setor">Setor</option>
                <option value="ramal">Ramal</option>
              </select>
            </label>
          </>
        ) : (
          <>
            <label>
              Serviço
              <select value={fx.categoria} onChange={(e) => setFx({ ...fx, categoria: e.target.value })}>
                <option value="">Todos</option>
                {externos?.categorias.map((o) => (
                  <option key={o}>{o}</option>
                ))}
              </select>
            </label>
            <label>
              Contrato
              <select value={fx.situacao} onChange={(e) => setFx({ ...fx, situacao: e.target.value as FiltroExternos['situacao'] })}>
                <option value="">Todos</option>
                <option value="emdia">Em dia</option>
                <option value="vence">Vence em até 60 dias</option>
                <option value="vencido">Vencido</option>
              </select>
            </label>
          </>
        )}
        <span className="espaco" />
        <span role="status" className="filtros-info">
          {info}
        </span>
        {filtrosAtivos > 0 && (
          <button type="button" className="link-acao" onClick={() => (setQ(''), setFi(INTERNOS_INICIAL), setFx(EXTERNOS_INICIAL))}>
            Limpar filtros
          </button>
        )}
      </div>

      {erro && (
        <p role="alert" className="caixa-erro">
          {erro}
        </p>
      )}

      {visao === 'internos' && internos && (
        <div className="tabela-rolagem">
          <table className="tabela-simples tabela-contatos">
            <thead>
              <tr>
                <th>Nome</th>
                <th>Setor</th>
                <th>Andar</th>
                <th>Ramal</th>
                <th>E-mail</th>
              </tr>
            </thead>
            <tbody>
              {internos.contatos.map((c) => (
                <tr key={`${c.nome}-${c.ramal}`}>
                  <td className="forte">{c.nome}</td>
                  <td>{c.setor}</td>
                  <td>{c.andar || '—'}</td>
                  <td className="mono forte">{c.ramal}</td>
                  <td>{c.email ? <a href={`mailto:${c.email}`}>{c.email}</a> : '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
          {internos.contatos.length === 0 && <p className="lista-vazia">{q ? `Nenhum ramal encontrado para “${q}”.` : 'Nenhum contato com esses filtros.'}</p>}
        </div>
      )}

      {visao === 'externos' && externos && (
        <>
          <div className="tabela-rolagem">
            <table className="tabela-simples tabela-contatos">
              <thead>
                <tr>
                  <th>Empresa</th>
                  <th>Serviço</th>
                  <th>Falar com</th>
                  <th>Telefone</th>
                  <th>E-mail</th>
                  <th>Contrato</th>
                </tr>
              </thead>
              <tbody>
                {externos.contatos.map(({ contato: c, situacao, texto }) => {
                  const [fundo, cor] = CORES_CONTRATO[situacao]
                  return (
                    <tr key={`${c.empresa}-${c.contrato}`}>
                      <td className="forte">{c.empresa}</td>
                      <td>{c.servico}</td>
                      <td>{c.contato || '—'}</td>
                      <td className="nowrap">{c.telefone || '—'}</td>
                      <td>{c.email ? <a href={`mailto:${c.email}`}>{c.email}</a> : '—'}</td>
                      <td>
                        <span className="contrato">
                          {c.contrato && <span className="nota-campo">nº {c.contrato}</span>}
                          <span className="pilula-status" style={{ background: fundo, color: cor }}>
                            {texto}
                          </span>
                        </span>
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
            {externos.contatos.length === 0 && (
              <p className="lista-vazia">{q ? `Nenhum contato externo encontrado para “${q}”.` : 'Nenhum contato com esses filtros.'}</p>
            )}
          </div>
          <p className="legenda">Contratos que vencem em até 60 dias ficam em amarelo. O vencimento de cada contrato aparece no calendário.</p>
        </>
      )}
    </>
  )
}
