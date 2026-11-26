import { SECOES, type Secao } from '../secoes'

type Props = {
  secaoAtual: Secao
  aoEscolher: (secao: Secao) => void
  aoAbrirConfiguracoes: () => void
}

export function Lateral({ secaoAtual, aoEscolher, aoAbrirConfiguracoes }: Props) {
  return (
    <nav aria-label="Seções" className="lateral">
      <div className="lateral-marca">
        <LogoHelplixo />
        <span className="lateral-marca-nome">Helplixo</span>
        <span className="lateral-marca-setor">TI</span>
      </div>
      <button type="button" className="lateral-item" onClick={aoAbrirConfiguracoes}>
        <span aria-hidden="true" className="lateral-emoji-config">⚙️</span>
        <span className="lateral-rotulo">Configurações</span>
      </button>

      <div className="lateral-espaco" />
      {SECOES.map((s) => (
        <button
          key={s.chave}
          type="button"
          className="lateral-item"
          aria-current={s.chave === secaoAtual ? 'page' : undefined}
          onClick={() => aoEscolher(s.chave)}
        >
          <span aria-hidden="true" className="lateral-emoji">{s.emoji}</span>
          <span className="lateral-rotulo">{s.rotulo}</span>
        </button>
      ))}
    </nav>
  )
}

/** O macaco do Helplixo, copiado da maquete (linha 39). */
function LogoHelplixo() {
  return (
    <svg width="24" height="24" viewBox="0 0 48 48" fill="none" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <circle cx="9" cy="24" r="6" fill="#F2C46D" />
      <circle cx="39" cy="24" r="6" fill="#F2C46D" />
      <path d="M10 25c-1-11 6-18 14-18s15 7 14 18c-1 9-7 15-14 15s-13-6-14-15z" fill="#C68B59" />
      <path d="M16 28c-1-6 4-8 8-5 4-3 9-1 8 5-1 6-4 9-8 9s-7-3-8-9z" fill="#F7DFC3" />
      <circle cx="20" cy="26" r="1.4" fill="#1E2430" />
      <circle cx="28" cy="26" r="1.4" fill="#1E2430" />
      <path d="M21 32q3 2 6 0" />
    </svg>
  )
}
