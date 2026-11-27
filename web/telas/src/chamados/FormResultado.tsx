import { useState, type FormEvent } from 'react'
import { ErroDaApi } from '../api/cliente'
import { registrarResultado, STATUS, type Chamado, type StatusChamado } from '../api/chamados'
import { instante, lerDataHora, paraServidor } from '../util/datas'
import type { DadosFormulario } from './estado'
import { conferirHorario } from './regras'

type Props = {
  chamado: Chamado
  dados: DadosFormulario
  aoMudar: (d: DadosFormulario) => void
  tecnicos: string[]
  agora: number
  /** Chamado salvo pelo servidor, com os avisos (ex.: fora do expediente) e a mensagem para o aviso rápido. */
  aoSalvar: (salvo: Chamado, mensagem: string) => void
}

/** Um formulário só: o status escolhido decide se atualiza, fecha ou reabre (regras no servidor). */
export function FormResultado({ chamado, dados, aoMudar, tecnicos, agora, aoSalvar }: Props) {
  const [erros, setErros] = useState<string[]>([])
  const [enviando, setEnviando] = useState(false)
  const mudar = (parcial: Partial<DadosFormulario>) => {
    setErros([])
    aoMudar({ ...dados, ...parcial })
  }

  const fechar = dados.status === 'Fechado'
  const jaFechado = chamado.status === 'Fechado'
  const reabre = jaFechado && !fechar
  const textos = fechar
    ? { rotulo: 'O que foi feito *', quando: 'Atendido em *', tecnico: 'Técnico que realizou *', botao: 'Salvar e fechar', dica: 'Fecha o chamado com o resultado final.', exemplo: 'Ex.: configurei o SIGEM para abrir PDF e XLS nas duas máquinas e testei com o contato.' }
    : {
        rotulo: reabre ? 'Por que reabrir (opcional)' : 'Anotação (opcional)',
        quando: 'Data *',
        tecnico: 'Técnico *',
        botao: reabre ? 'Salvar e reabrir' : 'Salvar',
        dica: reabre ? 'O chamado volta a ficar aberto. O fechamento anterior fica no andamento.' : 'Registra o andamento. O chamado continua aberto.',
        exemplo: reabre ? 'Ex.: o problema voltou depois da atualização do Windows.' : 'Ex.: pedi o token novo à certificadora; aguardando a entrega.',
      }

  const quando = lerDataHora(dados.data, dados.hora)
  const conferencia = conferirHorario(quando, instante(chamado.abertoEm), agora)

  const enviar = async (e: FormEvent) => {
    e.preventDefault()
    if (quando === null) {
      setErros(['Informe a data e a hora.'])
      return
    }
    setEnviando(true)
    try {
      const r = await registrarResultado(chamado.numero, {
        status: dados.status,
        anotacao: dados.anotacao,
        quando: paraServidor(quando),
        tecnico: dados.tecnico,
      })
      const n = chamado.numero
      const msg = reabre
        ? `Chamado ${n} reaberto: ${dados.status.toLowerCase()}.`
        : fechar
          ? jaFechado
            ? `Anotação registrada no chamado ${n} (continua fechado).`
            : `Chamado ${n} fechado.`
          : `Chamado ${n} atualizado: ${dados.status.toLowerCase()}.`
      aoSalvar(r.chamado, [msg, ...r.avisos].join(' '))
    } catch (falha) {
      setErros(falha instanceof ErroDaApi ? falha.erros : ['Não foi possível falar com o servidor. Confira se ele está no ar e tente de novo.'])
    } finally {
      setEnviando(false)
    }
  }

  return (
    <form className="form-resultado" onSubmit={enviar} aria-label="Resultado do chamado">
      <div className="form-linha-status">
        <label className="campo">
          Status *
          <select value={dados.status} onChange={(e) => mudar({ status: e.target.value as StatusChamado })}>
            {STATUS.map((s) => (
              <option key={s} value={s}>
                {s}
              </option>
            ))}
          </select>
        </label>
        <span className="dica">{textos.dica}</span>
      </div>
      <label className="campo">
        {textos.rotulo}
        <textarea rows={4} value={dados.anotacao} placeholder={textos.exemplo} onChange={(e) => mudar({ anotacao: e.target.value })} />
      </label>
      <div className="grade-campos">
        <label className="campo">
          {textos.quando}
          <input type="date" value={dados.data} onChange={(e) => mudar({ data: e.target.value })} />
        </label>
        <label className="campo">
          Hora *
          <input type="time" value={dados.hora} onChange={(e) => mudar({ hora: e.target.value })} />
        </label>
        <label className="campo">
          {textos.tecnico}
          <select value={dados.tecnico} onChange={(e) => mudar({ tecnico: e.target.value })}>
            <option value="">Escolha…</option>
            {tecnicos.map((t) => (
              <option key={t} value={t}>
                {t}
              </option>
            ))}
          </select>
        </label>
      </div>
      <div className="grade-campos">
        <div className="campo">
          Contato(s)
          <span className="campo-fixo">{chamado.contato}</span>
        </div>
        <div className="campo">
          Setor - Secretaria
          <span className="campo-fixo">{chamado.setor}</span>
        </div>
      </div>
      <span className="nota-campo">Contato e setor vêm do registro do chamado e não mudam no fechamento.</span>

      <div role="group" aria-label="Conferência do horário" className="conferencia">
        {conferencia.map((c, i) => (
          <span key={i} className={`conferencia-item conferencia-${c.tipo}`}>
            <span aria-hidden="true" className="conferencia-icone">
              {{ ok: '✓', erro: '✕', aviso: '!', info: 'i' }[c.tipo]}
            </span>
            {c.texto}
          </span>
        ))}
      </div>

      {erros.length > 0 && (
        <div role="alert" className="lista-erros">
          {erros.map((er) => (
            <span key={er}>• {er}</span>
          ))}
        </div>
      )}
      <div className="botoes">
        <button type="submit" className="botao botao-principal" disabled={enviando}>
          {enviando ? 'Salvando…' : textos.botao}
        </button>
      </div>
    </form>
  )
}
