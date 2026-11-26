import { useEffect } from 'react'
import type { PreferenciaTema } from '../tema/useTema'

type OpcaoTema = { valor: PreferenciaTema; nome: string; icone: string; fundo: string; tinta: string }

// Copiadas da maquete (temaOpcoes).
const OPCOES_TEMA: OpcaoTema[] = [
  { valor: 'claro', nome: 'Claro', icone: '☀️', fundo: '#FBFBFA', tinta: '#37352F' },
  { valor: 'escuro', nome: 'Escuro', icone: '\u{1F319}', fundo: '#191919', tinta: '#E3E2DF' },
  { valor: 'sistema', nome: 'Igual ao sistema', icone: '\u{1F4BB}', fundo: 'linear-gradient(135deg, #FBFBFA 50%, #191919 50%)', tinta: '#787774' },
]

type Props = {
  tema: PreferenciaTema
  aoEscolherTema: (tema: PreferenciaTema) => void
  editando: boolean
  /** Ligar pede a senha (abre outro diálogo); desligar sai na hora. */
  aoAlternarEdicao: () => void
  aoFechar: () => void
}

export function Configuracoes({ tema, aoEscolherTema, editando, aoAlternarEdicao, aoFechar }: Props) {
  useEffect(() => {
    const aoTeclar = (e: KeyboardEvent) => e.key === 'Escape' && aoFechar()
    document.addEventListener('keydown', aoTeclar)
    return () => document.removeEventListener('keydown', aoTeclar)
  }, [aoFechar])

  return (
    <div className="fundo-dialogo" onClick={aoFechar}>
      <div role="dialog" aria-modal="true" aria-labelledby="config-titulo" className="dialogo" onClick={(e) => e.stopPropagation()}>
        <div className="dialogo-topo">
          <h2 id="config-titulo" className="dialogo-titulo">Configurações</h2>
          <button type="button" className="dialogo-fechar" aria-label="Fechar configurações" onClick={aoFechar}>
            ×
          </button>
        </div>

        <section className="config-secao">
          <h3>Aparência</h3>
          <span className="config-ajuda">Escolha como o Helplixo aparece neste computador.</span>
          <div role="radiogroup" aria-label="Tema" className="temas">
            {OPCOES_TEMA.map((o) => (
              <button
                key={o.valor}
                type="button"
                role="radio"
                aria-checked={tema === o.valor}
                className="tema-opcao"
                onClick={() => aoEscolherTema(o.valor)}
              >
                <span aria-hidden="true" className="tema-amostra" style={{ background: o.fundo }}>
                  <span className="tema-amostra-linha" style={{ background: o.tinta }} />
                </span>
                <span className="tema-nome">
                  {o.icone} {o.nome}
                </span>
              </button>
            ))}
          </div>
        </section>

        <section className="config-secao">
          <h3>Modo edição</h3>
          <div className="edicao-linha">
            <span className="config-ajuda">
              {editando
                ? 'Ligado: dá para adicionar e editar máquinas, softwares, processos, contatos e marcações.'
                : 'Desligado. Para ligar, digite a senha de edição.'}
            </span>
            <button type="button" role="switch" aria-checked={editando} aria-label="Modo edição" className="interruptor" onClick={aoAlternarEdicao}>
              <span className="interruptor-bolinha" />
            </button>
          </div>
        </section>
      </div>
    </div>
  )
}
