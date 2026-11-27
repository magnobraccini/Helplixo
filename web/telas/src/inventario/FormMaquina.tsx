import { useState, type FormEvent } from 'react'
import { ErroDaApi } from '../api/cliente'
import { adicionarMaquina, editarMaquina, type Autoria, type DadosMaquina, type Maquina } from '../api/inventario'
import { LOCAIS, TIPOS } from './regras'

type Campo = { chave: keyof Texto; rotulo: string; exemplo?: string; tipo?: 'date' }
/** Os campos do formulário, todos como texto (vazio = não informado). */
type Texto = {
  nSemin: string; patrimonio: string; tipo: string; modelo: string; serie: string; processador: string; memoria: string
  armazenamento: string; sistema: string; ip: string; mac: string; local: string; usuario: string; observacao: string
  fornecedor: string; notaFiscal: string; data: string; valor: string; processo: string; garantiaAte: string
}

// Rótulos e exemplos copiados da maquete (nmCamposIdent e nmCamposCompra).
const IDENTIFICACAO: Campo[] = [
  { chave: 'nSemin', rotulo: 'Nº Semin *', exemplo: 'Ex.: IC52' },
  { chave: 'patrimonio', rotulo: 'Patrimônio *', exemplo: 'Ex.: PMS-01400' },
  { chave: 'modelo', rotulo: 'Marca e modelo *', exemplo: 'Ex.: Dell OptiPlex 7010' },
  { chave: 'serie', rotulo: 'Nº de série' },
  { chave: 'usuario', rotulo: 'Usuário', exemplo: 'Quem usa a máquina' },
  { chave: 'processador', rotulo: 'Processador', exemplo: 'Ex.: Intel Core i5-12500 3.0 GHz' },
  { chave: 'memoria', rotulo: 'Memória RAM', exemplo: 'Ex.: 16 GB DDR4' },
  { chave: 'armazenamento', rotulo: 'Armazenamento', exemplo: 'Ex.: SSD 512 GB' },
  { chave: 'sistema', rotulo: 'Sistema operacional', exemplo: 'Ex.: Windows 11 Pro' },
  { chave: 'ip', rotulo: 'Endereço IP', exemplo: 'Ex.: 10.0.13.52' },
  { chave: 'mac', rotulo: 'Endereço MAC', exemplo: 'Ex.: B0-7B-25-1A-4C-52' },
  { chave: 'observacao', rotulo: 'Observações' },
]
const COMPRA: Campo[] = [
  { chave: 'fornecedor', rotulo: 'Fornecedor' },
  { chave: 'notaFiscal', rotulo: 'Nota fiscal' },
  { chave: 'data', rotulo: 'Data da compra', tipo: 'date' },
  { chave: 'valor', rotulo: 'Valor', exemplo: 'Ex.: R$ 4.500,00' },
  { chave: 'processo', rotulo: 'Processo de compra', exemplo: 'Ex.: Pregão 010/2026' },
  { chave: 'garantiaAte', rotulo: 'Garantia até', tipo: 'date' },
]

const vazio = (): Texto => ({
  nSemin: '', patrimonio: '', tipo: 'Computador', modelo: '', serie: '', processador: '', memoria: '', armazenamento: '',
  sistema: '', ip: '', mac: '', local: '', usuario: '', observacao: '', fornecedor: '', notaFiscal: '', data: '', valor: '',
  processo: '', garantiaAte: '',
})

const deMaquina = (m: Maquina): Texto => ({
  nSemin: m.nSemin, patrimonio: m.patrimonio, tipo: m.tipo, modelo: m.modelo, serie: m.serie ?? '', processador: m.processador ?? '',
  memoria: m.memoria ?? '', armazenamento: m.armazenamento ?? '', sistema: m.sistema ?? '', ip: m.ip ?? '', mac: m.mac ?? '',
  local: m.local, usuario: m.usuario ?? '', observacao: m.observacao ?? '', fornecedor: m.compra?.fornecedor ?? '',
  notaFiscal: m.compra?.notaFiscal ?? '', data: m.compra?.data ?? '', valor: m.compra?.valor ?? '', processo: m.compra?.processo ?? '',
  garantiaAte: m.compra?.garantiaAte ?? '',
})

const ou = (s: string) => (s.trim() ? s.trim() : null)
const paraServidor = (t: Texto): DadosMaquina => ({
  nSemin: t.nSemin.trim().toUpperCase(), patrimonio: t.patrimonio.trim().toUpperCase(), tipo: t.tipo, modelo: t.modelo.trim(),
  serie: ou(t.serie), processador: ou(t.processador), memoria: ou(t.memoria), armazenamento: ou(t.armazenamento), sistema: ou(t.sistema),
  ip: ou(t.ip), mac: ou(t.mac), local: t.local, usuario: ou(t.usuario), observacao: ou(t.observacao),
  compra: { fornecedor: ou(t.fornecedor), notaFiscal: ou(t.notaFiscal), data: ou(t.data), valor: ou(t.valor), processo: ou(t.processo), garantiaAte: ou(t.garantiaAte) },
})

type Props = {
  /** Sem máquina = adicionar; com máquina = editar (Nº Semin e patrimônio não mudam). */
  maquina?: Maquina
  autoria: Autoria
  aoSalvar: (m: Maquina, mensagem: string) => void
  aoCancelar: () => void
}

export function FormMaquina({ maquina, autoria, aoSalvar, aoCancelar }: Props) {
  const editando = !!maquina
  const [t, setT] = useState<Texto>(() => (maquina ? deMaquina(maquina) : vazio()))
  const [erros, setErros] = useState<string[]>([])
  const [enviando, setEnviando] = useState(false)
  const mudar = (chave: keyof Texto, valor: string) => {
    setErros([])
    setT((x) => ({ ...x, [chave]: valor }))
  }

  const enviar = async (e: FormEvent) => {
    e.preventDefault()
    setEnviando(true)
    try {
      if (maquina) {
        const r = await editarMaquina(maquina.nSemin, paraServidor(t), autoria)
        aoSalvar(r.maquina, r.mudancas.length ? `Alterações salvas em ${maquina.nSemin}: ${r.mudancas.join('; ')}.` : 'Nada mudou.')
      } else {
        const m = await adicionarMaquina(paraServidor(t), autoria)
        aoSalvar(m, `Máquina ${m.nSemin} adicionada ao inventário.`)
      }
    } catch (falha) {
      setErros(falha instanceof ErroDaApi ? falha.erros : ['Não foi possível falar com o servidor. Confira se ele está no ar e tente de novo.'])
    } finally {
      setEnviando(false)
    }
  }

  const campos = editando ? IDENTIFICACAO.filter((c) => c.chave !== 'nSemin' && c.chave !== 'patrimonio') : IDENTIFICACAO
  const titulo = editando ? `Editar máquina ${maquina.nSemin}` : 'Adicionar máquina ao inventário'

  return (
    <form className="painel-form" onSubmit={enviar} aria-label={titulo}>
      <div className="painel-form-topo">
        <h2>{titulo}</h2>
        <span className="selo-edicao">Liberado pelo modo edição (senha de edição)</span>
      </div>
      <span className="painel-form-secao">Identificação e configuração</span>
      <div className="grade-campos">
        <label className="campo">
          Tipo *
          <select value={t.tipo} onChange={(e) => mudar('tipo', e.target.value)}>
            {TIPOS.map((x) => (
              <option key={x}>{x}</option>
            ))}
          </select>
        </label>
        <label className="campo">
          Onde está *
          <select value={t.local} onChange={(e) => mudar('local', e.target.value)}>
            <option value="">Escolha o local…</option>
            {LOCAIS.map((x) => (
              <option key={x}>{x}</option>
            ))}
          </select>
        </label>
        {campos.map((c) => (
          <label key={c.chave} className="campo">
            {c.rotulo}
            <input type="text" value={t[c.chave]} placeholder={c.exemplo} onChange={(e) => mudar(c.chave, e.target.value)} />
          </label>
        ))}
      </div>
      <span className="painel-form-secao">Informações de compra</span>
      <div className="grade-campos">
        {COMPRA.map((c) => (
          <label key={c.chave} className="campo">
            {c.rotulo}
            <input type={c.tipo ?? 'text'} value={t[c.chave]} placeholder={c.exemplo} onChange={(e) => mudar(c.chave, e.target.value)} />
          </label>
        ))}
      </div>
      <span className="nota-campo">
        {editando
          ? 'O Nº Semin não muda: os chamados e as movimentações apontam para ele.'
          : 'Os softwares instalados são cadastrados depois, na ficha da máquina (aba Softwares instalados).'}
      </span>
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
          {enviando ? 'Salvando…' : editando ? 'Salvar alterações' : 'Adicionar máquina'}
        </button>
      </div>
    </form>
  )
}
