import { useEffect, useState, type FormEvent } from 'react'
import { ErroDaApi } from '../api/cliente'

type Props = {
  /** Manda a senha para o servidor (POST /api/edicao/entrar). Rejeita com ErroDaApi se ele recusar. */
  entrar: (senha: string) => Promise<void>
  aoEntrar: () => void
  aoCancelar: () => void
}

/** Pede a senha de edição. Quem confere é o servidor; a tela não sabe qual é a senha. */
export function SenhaEdicao({ entrar, aoEntrar, aoCancelar }: Props) {
  const [senha, setSenha] = useState('')
  const [erro, setErro] = useState('')
  const [enviando, setEnviando] = useState(false)

  useEffect(() => {
    const aoTeclar = (e: KeyboardEvent) => e.key === 'Escape' && aoCancelar()
    document.addEventListener('keydown', aoTeclar)
    return () => document.removeEventListener('keydown', aoTeclar)
  }, [aoCancelar])

  const confirmar = async (e: FormEvent) => {
    e.preventDefault()
    if (!senha) {
      setErro('Digite a senha de edição.')
      return
    }
    setEnviando(true)
    try {
      await entrar(senha)
      aoEntrar()
    } catch (falha) {
      setSenha('')
      setErro(
        falha instanceof ErroDaApi
          ? falha.erros.join(' ')
          : 'Não foi possível falar com o servidor. Confira se ele está no ar e tente de novo.',
      )
    } finally {
      setEnviando(false)
    }
  }

  return (
    <div className="fundo-dialogo fundo-dialogo-senha">
      <form role="dialog" aria-modal="true" aria-labelledby="senha-titulo" className="dialogo dialogo-senha" onSubmit={confirmar}>
        <h2 id="senha-titulo" className="dialogo-titulo">Entrar no modo edição</h2>
        <p className="dialogo-texto">O modo edição permite adicionar máquinas, softwares e processos. Digite a senha de edição.</p>
        <label className="campo">
          Senha de edição
          <input
            type="password"
            autoFocus
            autoComplete="off"
            value={senha}
            onChange={(e) => {
              setSenha(e.target.value)
              setErro('')
            }}
          />
        </label>
        {erro && (
          <span role="alert" className="erro">
            {erro}
          </span>
        )}
        <div className="botoes">
          <button type="button" className="botao" onClick={aoCancelar}>
            Cancelar
          </button>
          <button type="submit" className="botao botao-principal" disabled={enviando}>
            {enviando ? 'Conferindo…' : 'Entrar'}
          </button>
        </div>
      </form>
    </div>
  )
}
