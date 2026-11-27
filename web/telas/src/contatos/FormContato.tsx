import { useState, type FormEvent } from 'react'
import { ErroDaApi } from '../api/cliente'
import { adicionarExterno, adicionarInterno } from '../api/contatos'
import { SETORES } from '../inventario/regras'

type Props = {
  tipo: 'internos' | 'externos'
  chave: string | null
  /** Categorias que já existem (sugestões; uma nova vira opção sozinha). */
  categorias: string[]
  aoSalvar: (mensagem: string) => void
  aoCancelar: () => void
}

const INTERNO = { nome: '', setor: '', andar: '', ramal: '', email: '' }
const EXTERNO = { empresa: '', servico: '', categoria: '', contato: '', telefone: '', email: '', contrato: '', vence: '' }
const ou = (s: string) => (s.trim() ? s.trim() : null)

/** Novo contato (modo edição). O servidor confere as regras; ramal repetido só avisa com quem é dividido. */
export function FormContato({ tipo, chave, categorias, aoSalvar, aoCancelar }: Props) {
  const [i, setI] = useState(INTERNO)
  const [x, setX] = useState(EXTERNO)
  const [erros, setErros] = useState<string[]>([])
  const [enviando, setEnviando] = useState(false)

  const enviar = async (e: FormEvent) => {
    e.preventDefault()
    setEnviando(true)
    try {
      if (tipo === 'internos') {
        const r = await adicionarInterno({ nome: i.nome, setor: i.setor, andar: ou(i.andar), ramal: i.ramal.trim(), email: ou(i.email) }, chave)
        const divide = r.compartilhadoCom.length ? ` (compartilhado com ${r.compartilhadoCom.join(', ')})` : ''
        aoSalvar(`Contato salvo: ${r.contato.nome} · ramal ${r.contato.ramal}${divide}.`)
      } else {
        const r = await adicionarExterno(
          { empresa: x.empresa, servico: x.servico, categoria: x.categoria, contato: ou(x.contato), telefone: ou(x.telefone), email: ou(x.email), contrato: ou(x.contrato), vence: x.vence },
          chave,
        )
        aoSalvar(`Contato salvo: ${r.empresa}.`)
      }
    } catch (falha) {
      setErros(falha instanceof ErroDaApi ? falha.erros : ['Não foi possível falar com o servidor. Confira se ele está no ar e tente de novo.'])
    } finally {
      setEnviando(false)
    }
  }

  const campoI = (k: keyof typeof INTERNO, rotulo: string, extra: { exemplo?: string; tipo?: string } = {}) => (
    <label className="campo">
      {rotulo}
      <input type={extra.tipo ?? 'text'} value={i[k]} placeholder={extra.exemplo} onChange={(e) => (setErros([]), setI({ ...i, [k]: e.target.value }))} />
    </label>
  )
  const campoX = (k: keyof typeof EXTERNO, rotulo: string, extra: { exemplo?: string; tipo?: string; lista?: string } = {}) => (
    <label className="campo">
      {rotulo}
      <input type={extra.tipo ?? 'text'} list={extra.lista} value={x[k]} placeholder={extra.exemplo} onChange={(e) => (setErros([]), setX({ ...x, [k]: e.target.value }))} />
    </label>
  )

  return (
    <form className="painel-form" onSubmit={enviar} aria-label="Novo contato">
      <div className="painel-form-topo">
        <h2>{tipo === 'internos' ? 'Novo ramal' : 'Novo contato externo'}</h2>
        <span className="selo-edicao">Liberado pelo modo edição (senha de edição)</span>
      </div>
      {tipo === 'internos' ? (
        <div className="grade-campos">
          {campoI('nome', 'Nome *', { exemplo: 'Pessoa ou sala (ex.: Recepção)' })}
          <label className="campo">
            Setor *
            <select value={i.setor} onChange={(e) => (setErros([]), setI({ ...i, setor: e.target.value }))}>
              <option value="">Escolha…</option>
              {SETORES.map((s) => (
                <option key={s}>{s}</option>
              ))}
            </select>
          </label>
          {campoI('andar', 'Andar', { exemplo: 'Ex.: 2º' })}
          {campoI('ramal', 'Ramal *', { exemplo: '4 números' })}
          {campoI('email', 'E-mail', { tipo: 'email' })}
        </div>
      ) : (
        <div className="grade-campos">
          {campoX('empresa', 'Empresa *')}
          {campoX('servico', 'Serviço *', { exemplo: 'Ex.: Manutenção de nobreaks' })}
          {campoX('categoria', 'Categoria *', { exemplo: 'Escolha ou digite', lista: 'categorias-externos' })}
          <datalist id="categorias-externos">
            {categorias.map((c) => (
              <option key={c} value={c} />
            ))}
          </datalist>
          {campoX('contato', 'Falar com')}
          {campoX('telefone', 'Telefone', { tipo: 'tel' })}
          {campoX('email', 'E-mail', { tipo: 'email' })}
          {campoX('contrato', 'Nº do contrato')}
          {campoX('vence', 'Vencimento *', { tipo: 'date' })}
        </div>
      )}
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
          {enviando ? 'Salvando…' : 'Salvar contato'}
        </button>
      </div>
    </form>
  )
}
