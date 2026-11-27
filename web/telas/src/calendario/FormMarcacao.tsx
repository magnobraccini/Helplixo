import { useState, type FormEvent } from 'react'
import { criarMarcacao, type QuemMarca } from '../api/calendario'
import { ErroDaApi } from '../api/cliente'
import { pad } from '../util/datas'
import { LEMBRETES, TIPOS_MARCACAO } from './regras'

type Props = {
  /** Dia (e, se veio de um horário da grade, a hora) já escolhidos. */
  dia: string
  hora?: string
  editando: boolean
  quem: QuemMarca
  aoSalvar: (mensagem: string) => void
  aoCancelar: () => void
}

/** Nova marcação. Aviso: qualquer cargo. Os outros tipos: modo edição (como na maquete: novoEv). */
export function FormMarcacao({ dia, hora, editando, quem, aoSalvar, aoCancelar }: Props) {
  const h = hora ? Number(hora.slice(0, 2)) : 0
  const [f, setF] = useState({
    titulo: '',
    tipo: editando ? 'Manutenção' : 'Aviso',
    inicio: dia,
    fim: '',
    diaInteiro: !hora,
    horaInicio: hora ?? '',
    horaFim: hora ? `${pad(Math.min(h + 1, 23))}:00` : '',
    repete: '',
    repeteAte: '',
    lembrete: hora ? '30' : 'dia',
    descricao: '',
  })
  const [erro, setErro] = useState('')
  const [enviando, setEnviando] = useState(false)
  const mudar = (parcial: Partial<typeof f>) => {
    setErro('')
    setF((x) => ({ ...x, ...parcial }))
  }

  const enviar = async (e: FormEvent) => {
    e.preventDefault()
    setEnviando(true)
    try {
      const comHora = !f.diaInteiro && f.horaInicio
      const r = await criarMarcacao(
        {
          titulo: f.titulo.trim(),
          tipo: f.tipo,
          inicio: f.inicio,
          fim: f.fim || null,
          horaInicio: comHora ? f.horaInicio : null,
          horaFim: comHora && f.horaFim ? f.horaFim : null,
          repete: f.repete,
          repeteAte: f.repete && f.repeteAte ? f.repeteAte : null,
          lembrete: f.lembrete,
          descricao: f.descricao.trim(),
          urgente: false,
        },
        quem,
      )
      aoSalvar([`Marcado no calendário: ${r.marcacao.titulo}.`, ...r.avisos].join(' '))
    } catch (falha) {
      setErro(falha instanceof ErroDaApi ? falha.erros.join(' ') : 'Não foi possível falar com o servidor. Confira se ele está no ar e tente de novo.')
    } finally {
      setEnviando(false)
    }
  }

  const tipos = editando ? TIPOS_MARCACAO : ['Aviso']

  return (
    <form className="painel-form form-marcacao" onSubmit={enviar} aria-label="Nova marcação">
      <input type="text" className="titulo-marcacao" aria-label="Título" value={f.titulo} placeholder="Adicionar título" onChange={(e) => mudar({ titulo: e.target.value })} />
      <div className="linha-campos">
        <label className="campo">
          Tipo
          <select value={f.tipo} onChange={(e) => mudar({ tipo: e.target.value })}>
            {tipos.map((t) => (
              <option key={t}>{t}</option>
            ))}
          </select>
        </label>
        <label className="campo">
          Dia *
          <input type="date" value={f.inicio} onChange={(e) => mudar({ inicio: e.target.value })} />
        </label>
        <label className="campo">
          Até (opcional)
          <input type="date" value={f.fim} onChange={(e) => mudar({ fim: e.target.value })} />
        </label>
        <label className="caixa">
          <input type="checkbox" checked={f.diaInteiro} onChange={(e) => mudar({ diaInteiro: e.target.checked })} /> Dia inteiro
        </label>
      </div>
      {!f.diaInteiro && (
        <div className="linha-campos">
          <label className="campo">
            Das
            <input type="time" value={f.horaInicio} onChange={(e) => mudar({ horaInicio: e.target.value })} />
          </label>
          <label className="campo">
            Até
            <input type="time" value={f.horaFim} onChange={(e) => mudar({ horaFim: e.target.value })} />
          </label>
        </div>
      )}
      <div className="linha-campos">
        <label className="campo">
          Repetir
          <select value={f.repete} onChange={(e) => mudar({ repete: e.target.value })}>
            <option value="">Não se repete</option>
            <option value="semanal">Toda semana</option>
            <option value="mensal">Todo mês</option>
            <option value="anual">Todo ano</option>
          </select>
        </label>
        {f.repete && (
          <label className="campo">
            Termina em (vazio = nunca)
            <input type="date" value={f.repeteAte} onChange={(e) => mudar({ repeteAte: e.target.value })} />
          </label>
        )}
        <label className="campo">
          Lembrete
          <select value={f.lembrete} onChange={(e) => mudar({ lembrete: e.target.value })}>
            {LEMBRETES.map((l) => (
              <option key={l.valor} value={l.valor}>
                {l.texto}
              </option>
            ))}
          </select>
        </label>
        <label className="campo campo-largo">
          Descrição
          <input type="text" value={f.descricao} placeholder="Detalhes para a equipe (opcional)" onChange={(e) => mudar({ descricao: e.target.value })} />
        </label>
      </div>
      <div className="botoes">
        {erro && (
          <span role="alert" className="erro espaco">
            {erro}
          </span>
        )}
        <button type="button" className="botao" onClick={aoCancelar}>
          Cancelar
        </button>
        <button type="submit" className="botao botao-principal" disabled={enviando}>
          {enviando ? 'Salvando…' : 'Salvar'}
        </button>
      </div>
    </form>
  )
}
