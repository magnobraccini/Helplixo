import { useState, type FormEvent } from 'react'
import { ErroDaApi } from '../api/cliente'
import { movimentarMaquina, type Maquina } from '../api/inventario'
import { isoData, isoHora, lerDataHora, paraServidor } from '../util/datas'
import { LOCAIS } from './regras'

type Props = {
  /** Nº Semin já escolhido (vindo da ficha); vazio na tela de busca. */
  nSemin?: string
  tecnicos: string[]
  agora: number
  aoSalvar: (m: Maquina, mensagem: string) => void
  aoCancelar: () => void
}

/** Movimentação: a máquina vai para outro lugar. Não precisa do modo edição (é trabalho do técnico). */
export function FormMovimentacao({ nSemin = '', tecnicos, agora, aoSalvar, aoCancelar }: Props) {
  const [f, setF] = useState(() => {
    const d = new Date(agora)
    return { nSemin, destino: '', data: isoData(d), hora: isoHora(d), tecnico: tecnicos[0] ?? '', chamado: '', motivo: '' }
  })
  const [erros, setErros] = useState<string[]>([])
  const [enviando, setEnviando] = useState(false)
  const mudar = (parcial: Partial<typeof f>) => {
    setErros([])
    setF((x) => ({ ...x, ...parcial }))
  }

  const enviar = async (e: FormEvent) => {
    e.preventDefault()
    const codigo = f.nSemin.trim().toUpperCase()
    const quando = lerDataHora(f.data, f.hora)
    const faltas = [!codigo && 'Informe o Nº Semin da máquina.', quando === null && 'Informe a data e a hora.'].filter(Boolean) as string[]
    if (faltas.length) {
      setErros(faltas)
      return
    }
    setEnviando(true)
    try {
      const m = await movimentarMaquina(codigo, { destino: f.destino, motivo: f.motivo, quando: paraServidor(quando!), tecnico: f.tecnico, chamado: f.chamado.trim() })
      aoSalvar(m, `Movimentação registrada: ${m.nSemin} foi para ${m.local}.`)
    } catch (falha) {
      setErros(
        falha instanceof ErroDaApi
          ? falha.status === 404
            ? [`A máquina ${codigo} não está no inventário.`]
            : falha.erros
          : ['Não foi possível falar com o servidor. Confira se ele está no ar e tente de novo.'],
      )
    } finally {
      setEnviando(false)
    }
  }

  return (
    <form className="painel-form" onSubmit={enviar} aria-label="Registrar movimentação de equipamento">
      <h2>Registrar movimentação de equipamento</h2>
      <div className="grade-campos">
        <label className="campo">
          Nº Semin *
          <input type="text" value={f.nSemin} placeholder="Ex.: IC21" onChange={(e) => mudar({ nSemin: e.target.value.toUpperCase() })} readOnly={!!nSemin} />
        </label>
        <label className="campo">
          Foi para *
          <select value={f.destino} onChange={(e) => mudar({ destino: e.target.value })}>
            <option value="">Escolha o destino…</option>
            {LOCAIS.map((x) => (
              <option key={x}>{x}</option>
            ))}
          </select>
        </label>
        <label className="campo">
          Data *
          <input type="date" value={f.data} onChange={(e) => mudar({ data: e.target.value })} />
        </label>
        <label className="campo">
          Hora *
          <input type="time" value={f.hora} onChange={(e) => mudar({ hora: e.target.value })} />
        </label>
        <label className="campo">
          Técnico *
          <select value={f.tecnico} onChange={(e) => mudar({ tecnico: e.target.value })}>
            {tecnicos.map((x) => (
              <option key={x}>{x}</option>
            ))}
          </select>
        </label>
        <label className="campo">
          Chamado relacionado
          <input type="text" value={f.chamado} placeholder="Nº do chamado (opcional)" onChange={(e) => mudar({ chamado: e.target.value })} />
        </label>
      </div>
      <label className="campo">
        Motivo *
        <input type="text" value={f.motivo} placeholder="Ex.: substituição de monitor com defeito" onChange={(e) => mudar({ motivo: e.target.value })} />
      </label>
      {erros.length > 0 && (
        <div role="alert" className="lista-erros">
          {erros.map((er) => (
            <span key={er}>• {er}</span>
          ))}
        </div>
      )}
      <div className="botoes">
        <button type="button" className="botao" onClick={aoCancelar}>
          Cancelar
        </button>
        <button type="submit" className="botao botao-principal" disabled={enviando}>
          {enviando ? 'Registrando…' : 'Registrar'}
        </button>
      </div>
    </form>
  )
}
