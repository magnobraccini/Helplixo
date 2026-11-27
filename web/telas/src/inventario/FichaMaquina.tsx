import { useState, type FormEvent } from 'react'
import { ErroDaApi } from '../api/cliente'
import { darBaixa, desfazerBaixa, type Autoria, type FichaMaquina as Ficha, type Maquina } from '../api/inventario'
import { fmt, instante } from '../util/datas'
import { FormMaquina } from './FormMaquina'
import { FormMovimentacao } from './FormMovimentacao'
import { MOTIVOS_BAIXA, coresGarantia, coresHistorico, coresOffice, corSituacao, diaBr } from './regras'

export type AbaMaquina = 'ident' | 'compra' | 'soft' | 'hist'

const ABAS: { chave: AbaMaquina; nome: string }[] = [
  { chave: 'ident', nome: 'Identificação e configuração' },
  { chave: 'compra', nome: 'Informações de compra' },
  { chave: 'soft', nome: 'Softwares instalados' },
  { chave: 'hist', nome: 'Consertos e histórico' },
]

type Painel = 'nenhum' | 'editar' | 'mover' | 'baixa'

type Props = {
  ficha: Ficha
  aba: AbaMaquina
  aoTrocarAba: (aba: AbaMaquina) => void
  aoVoltar: () => void
  aoAbrirChamado: (numero: string) => void
  /** A máquina mudou no servidor: recarregar a ficha e mostrar o aviso. */
  aoMudar: (m: Maquina, mensagem: string) => void
  editando: boolean
  autoria: Autoria
  tecnicos: string[]
  agora: number
}

export function FichaMaquina(p: Props) {
  const m = p.ficha.maquina
  const [painel, setPainel] = useState<Painel>('nenhum')
  const [baixa, setBaixa] = useState({ motivo: MOTIVOS_BAIXA[0], observacao: '' })
  const [erroBaixa, setErroBaixa] = useState('')
  const mudou = (nova: Maquina, mensagem: string) => {
    setPainel('nenhum')
    p.aoMudar(nova, mensagem)
  }
  const falhou = (falha: unknown) =>
    falha instanceof ErroDaApi ? falha.erros.join(' ') : 'Não foi possível falar com o servidor. Confira se ele está no ar e tente de novo.'

  const confirmarBaixa = async (e: FormEvent) => {
    e.preventDefault()
    try {
      mudou(await darBaixa(m.nSemin, baixa, p.autoria), `Máquina ${m.nSemin} baixada (${baixa.motivo.toLowerCase()}).`)
    } catch (falha) {
      setErroBaixa(falhou(falha))
    }
  }
  const voltarBaixa = async () => {
    try {
      mudou(await desfazerBaixa(m.nSemin, p.autoria), `Baixa da máquina ${m.nSemin} desfeita.`)
    } catch (falha) {
      setErroBaixa(falhou(falha))
    }
  }

  const office = coresOffice(p.ficha.office.situacao)
  const garantia = coresGarantia(p.ficha.garantia.situacao)
  const baixada = !!m.baixa

  return (
    <>
      <div className="ficha-barra">
        <button type="button" className="link-voltar" onClick={p.aoVoltar}>
          ← Voltar para o Inventário
        </button>
        <span className="espaco" />
        {!baixada && (
          <button type="button" className="botao-contorno" onClick={() => setPainel('mover')}>
            Registrar movimentação
          </button>
        )}
        {p.editando && !baixada && (
          <button type="button" className="botao-contorno botao-perigo" onClick={() => setPainel('baixa')}>
            Dar baixa
          </button>
        )}
        {p.editando && baixada && (
          <button type="button" className="botao-contorno" onClick={voltarBaixa}>
            Desfazer baixa
          </button>
        )}
        {p.editando && (
          <button type="button" className="botao-edicao" onClick={() => setPainel('editar')}>
            Editar máquina
          </button>
        )}
      </div>

      {baixada && (
        <div role="status" className="faixa-baixada">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" aria-hidden="true">
            <circle cx="12" cy="12" r="9" />
            <path d="M5.6 5.6l12.8 12.8" />
          </svg>
          <span>
            <b>Máquina baixada.</b> Baixada em {diaBr(m.baixa!.dia)} · {m.baixa!.motivo}
            {m.baixa!.observacao ? ` · ${m.baixa!.observacao}` : ''} · por {m.baixa!.quem}. Não recebe chamados nem movimentações.
          </span>
        </div>
      )}
      {erroBaixa && painel !== 'baixa' && (
        <p role="alert" className="caixa-erro">
          {erroBaixa}
        </p>
      )}

      {painel === 'baixa' && (
        <form className="form-baixa" onSubmit={confirmarBaixa} aria-label="Dar baixa">
          <label className="campo">
            Motivo da baixa
            <select value={baixa.motivo} onChange={(e) => setBaixa({ ...baixa, motivo: e.target.value })}>
              {MOTIVOS_BAIXA.map((x) => (
                <option key={x}>{x}</option>
              ))}
            </select>
          </label>
          <label className="campo campo-largo">
            Observação
            <input type="text" value={baixa.observacao} placeholder="Ex.: placa-mãe queimada, sem conserto" onChange={(e) => setBaixa({ ...baixa, observacao: e.target.value })} />
          </label>
          <div className="botoes">
            <button type="button" className="botao" onClick={() => setPainel('nenhum')}>
              Cancelar
            </button>
            <button type="submit" className="botao botao-perigo-cheio">
              Confirmar baixa
            </button>
          </div>
          {erroBaixa && (
            <span role="alert" className="erro">
              {erroBaixa}
            </span>
          )}
        </form>
      )}
      {painel === 'editar' && <FormMaquina maquina={m} autoria={p.autoria} aoSalvar={mudou} aoCancelar={() => setPainel('nenhum')} />}
      {painel === 'mover' && (
        <FormMovimentacao nSemin={m.nSemin} tecnicos={p.tecnicos} agora={p.agora} aoSalvar={mudou} aoCancelar={() => setPainel('nenhum')} />
      )}

      <div className="resumo-maquina">
        <Resumo nome="Situação" valor={m.situacao} cor={corSituacao(m.situacao)} />
        <Resumo nome="Onde está" valor={m.local} />
        <Resumo nome="Usuário" valor={m.usuario || '—'} />
        <Resumo nome="Pacote Office" valor={p.ficha.office.situacao} cor={office.texto} fundo={office.fundo} />
        <Resumo nome="Garantia" valor={p.ficha.garantia.texto} cor={garantia.texto} fundo={garantia.fundo} />
      </div>

      <div role="tablist" aria-label="Ficha da máquina" className="abas-linha abas-quebram">
        {ABAS.map((a) => (
          <button key={a.chave} type="button" role="tab" aria-selected={p.aba === a.chave} onClick={() => p.aoTrocarAba(a.chave)}>
            {a.nome}
          </button>
        ))}
      </div>

      <div role="tabpanel" className="ficha-maquina-corpo">
        {p.aba === 'ident' && (
          <dl className="propriedades propriedades-largas">
            <Prop nome="Nº Semin" valor={m.nSemin} mono />
            <Prop nome="Patrimônio" valor={m.patrimonio} mono />
            <Prop nome="Tipo" valor={m.tipo} />
            <Prop nome="Marca e modelo" valor={m.modelo} />
            <Prop nome="Nº de série" valor={m.serie} mono />
            <Prop nome="Processador" valor={m.processador} />
            <Prop nome="Memória RAM" valor={m.memoria} />
            <Prop nome="Armazenamento" valor={m.armazenamento} />
            <Prop nome="Sistema operacional" valor={m.sistema} />
            <Prop nome="Endereço IP" valor={m.ip} mono />
            <Prop nome="Endereço MAC" valor={m.mac} mono />
            <Prop nome="Onde está" valor={m.local} />
            <Prop nome="Usuário" valor={m.usuario} />
            <Prop nome="Situação" valor={m.situacao} />
            <Prop nome="Observações" valor={m.observacao} />
          </dl>
        )}

        {p.aba === 'compra' && (
          <dl className="propriedades propriedades-largas">
            <Prop nome="Fornecedor" valor={m.compra?.fornecedor} />
            <Prop nome="Nota fiscal" valor={m.compra?.notaFiscal} />
            <Prop nome="Data da compra" valor={diaBr(m.compra?.data)} />
            <Prop nome="Valor" valor={m.compra?.valor} />
            <Prop nome="Processo de compra" valor={m.compra?.processo} />
            <div className="propriedade">
              <dt>Garantia</dt>
              <dd>
                <span className="pilula-status" style={{ background: garantia.fundo, color: garantia.texto }}>
                  {p.ficha.garantia.texto}
                </span>
              </dd>
            </div>
          </dl>
        )}

        {p.aba === 'soft' && (
          <>
            <div role="status" className="faixa-office" style={{ background: office.fundo, color: office.texto }}>
              <b>Pacote Office:</b>
              <span>{p.ficha.office.detalhe}</span>
            </div>
            <h2 className="subtitulo-ficha">Software comercial</h2>
            {m.softwares.length > 0 ? (
              <table className="tabela-simples">
                <thead>
                  <tr>
                    <th>Instalado</th>
                    <th>Licenciado</th>
                    <th>Tipo de licença</th>
                  </tr>
                </thead>
                <tbody>
                  {m.softwares.map((s) => {
                    const semLicenca = !s.licenca && s.tipo !== 'Gratuito'
                    return (
                      <tr key={s.nome}>
                        <td>{s.nome}</td>
                        <td style={semLicenca ? { color: 'var(--c-8a2a27)' } : undefined}>{semLicenca ? 'Sem licença registrada' : s.licenca || 'Não precisa'}</td>
                        <td>{s.tipo}</td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
            ) : (
              <p className="lista-vazia">Nenhum software comercial cadastrado.</p>
            )}
            <h2 className="subtitulo-ficha">Softwares PMSL</h2>
            {m.sistemasPmsl.length > 0 ? (
              <table className="tabela-simples">
                <thead>
                  <tr>
                    <th>Sistema</th>
                    <th>Linguagem</th>
                  </tr>
                </thead>
                <tbody>
                  {m.sistemasPmsl.map((s) => (
                    <tr key={s.nome}>
                      <td>{s.nome}</td>
                      <td>{s.linguagem}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            ) : (
              <p className="lista-vazia">Nenhum sistema da prefeitura cadastrado.</p>
            )}
          </>
        )}

        {p.aba === 'hist' && (
          <>
            {p.ficha.historico.length > 0 ? (
              <div className="tabela-rolagem">
                <table className="tabela-simples">
                  <thead>
                    <tr>
                      <th>Data</th>
                      <th>Tipo</th>
                      <th>O que aconteceu</th>
                      <th>Quem</th>
                    </tr>
                  </thead>
                  <tbody>
                    {p.ficha.historico.map((h, i) => {
                      const c = coresHistorico(h.tipo)
                      return (
                        <tr key={i}>
                          <td className="nowrap">{h.tipo === 'Conserto' ? diaBr(h.quando) : fmt(instante(h.quando))}</td>
                          <td>
                            <span className="pilula-status" style={{ background: c.fundo, color: c.texto }}>
                              {h.tipo}
                            </span>
                          </td>
                          <td>
                            {h.referencia && (
                              <button type="button" className="link-chamado" onClick={() => p.aoAbrirChamado(h.referencia!)}>
                                #{h.referencia}
                              </button>
                            )}{' '}
                            {h.texto}
                          </td>
                          <td>{h.quem || '—'}</td>
                        </tr>
                      )
                    })}
                  </tbody>
                </table>
              </div>
            ) : (
              <p className="lista-vazia">Nenhum conserto, chamado ou movimentação registrado.</p>
            )}
            <p className="legenda">Chamados e movimentações desta máquina entram aqui sozinhos.</p>
          </>
        )}
      </div>
    </>
  )
}

function Resumo({ nome, valor, cor = 'var(--c-1e2430)', fundo = 'var(--c-f3f5f8)' }: { nome: string; valor: string; cor?: string; fundo?: string }) {
  return (
    <span className="resumo-item" style={{ background: fundo }}>
      <span className="resumo-nome">{nome}</span>
      <span className="resumo-valor" style={{ color: cor }}>
        {valor}
      </span>
    </span>
  )
}

function Prop({ nome, valor, mono }: { nome: string; valor: string | null | undefined; mono?: boolean }) {
  return (
    <div className="propriedade">
      <dt>{nome}</dt>
      <dd className={mono ? 'mono' : undefined}>{valor || '—'}</dd>
    </div>
  )
}
