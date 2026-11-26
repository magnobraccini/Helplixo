import { useCallback, useEffect, useState } from 'react'
import { buscarSaude } from './api/saude'
import { Configuracoes } from './configuracoes/Configuracoes'
import { SenhaEdicao } from './configuracoes/SenhaEdicao'
import { useEdicao } from './edicao/useEdicao'
import { Capa } from './leiaute/Capa'
import { Lateral } from './leiaute/Lateral'
import { Topo } from './leiaute/Topo'
import { useAvisoRapido } from './leiaute/useAvisoRapido'
import './leiaute/leiaute.css'
import { definicaoDe, PERFIL_PROVISORIO, type Secao } from './secoes'
import { useTema } from './tema/useTema'

type Dialogo = 'nenhum' | 'configuracoes' | 'senha'

function App() {
  const [secao, setSecao] = useState<Secao>('inicio')
  const [dialogo, setDialogo] = useState<Dialogo>('nenhum')
  const [servidorFora, setServidorFora] = useState(false)
  const tema = useTema()
  const edicao = useEdicao()
  const aviso = useAvisoRapido()
  const def = definicaoDe(secao)

  // Confere uma vez se o servidor está no ar; se não estiver, a página avisa.
  useEffect(() => {
    let ativo = true
    buscarSaude()
      .then(() => ativo && setServidorFora(false))
      .catch(() => ativo && setServidorFora(true))
    return () => {
      ativo = false
    }
  }, [])

  const fecharDialogo = useCallback(() => setDialogo('nenhum'), [])

  const alternarEdicao = () => {
    if (edicao.ligado) {
      void edicao.sair()
      aviso.avisar('Modo edição desligado.')
    } else {
      setDialogo('senha')
    }
  }

  return (
    <div className="app">
      <Lateral secaoAtual={secao} aoEscolher={setSecao} aoAbrirConfiguracoes={() => setDialogo('configuracoes')} />

      <div className="conteudo">
        <Topo caminho={[def.titulo]} perfil={PERFIL_PROVISORIO} editando={edicao.ligado} />
        <Capa cena={def.cena} tema={tema.efetivo} />

        <main className="pagina">
          <div className="pagina-cabecalho">
            <h1 className="pagina-titulo">{def.titulo}</h1>
            {def.subtitulo && <p className="pagina-subtitulo">{def.subtitulo}</p>}
          </div>
          {servidorFora && (
            <p role="alert" className="pagina-em-breve">
              Servidor fora do ar. Confira se ele está rodando na porta 8080.
            </p>
          )}
          <p className="pagina-em-breve">Esta tela entra nos próximos passos.</p>
        </main>
      </div>

      {dialogo === 'configuracoes' && (
        <Configuracoes
          tema={tema.preferencia}
          aoEscolherTema={tema.escolher}
          editando={edicao.ligado}
          aoAlternarEdicao={alternarEdicao}
          aoFechar={fecharDialogo}
        />
      )}
      {dialogo === 'senha' && (
        <SenhaEdicao
          entrar={edicao.entrar}
          aoEntrar={() => {
            setDialogo('nenhum')
            aviso.avisar('Modo edição ligado. As alterações valem para todos.')
          }}
          aoCancelar={fecharDialogo}
        />
      )}
      {aviso.mensagem && (
        <div role="status" className="aviso-rapido">
          {aviso.mensagem}
        </div>
      )}
    </div>
  )
}

export default App
