import { useCallback, useEffect, useState } from 'react'
import { buscarSaude } from './api/saude'
import type { AbaChamado } from './chamados/FichaChamado'
import { TelaChamados } from './chamados/TelaChamados'
import { Configuracoes } from './configuracoes/Configuracoes'
import { SenhaEdicao } from './configuracoes/SenhaEdicao'
import { useEdicao } from './edicao/useEdicao'
import { CabecalhoPagina } from './leiaute/CabecalhoPagina'
import { Capa } from './leiaute/Capa'
import { Lateral } from './leiaute/Lateral'
import { Topo } from './leiaute/Topo'
import { useAvisoRapido } from './leiaute/useAvisoRapido'
import './leiaute/leiaute.css'
import { definicaoDe, PERFIL_PROVISORIO, TECNICOS_PROVISORIOS, type Secao } from './secoes'
import { useTema } from './tema/useTema'
import { useAgora } from './util/useAgora'

type Dialogo = 'nenhum' | 'configuracoes' | 'senha'

/** O chamado aberto e a aba ficam aqui para sobreviver à troca de seção. */
type EstadoChamados = { aberto: string | null; aba: AbaChamado }

function App() {
  const [secao, setSecao] = useState<Secao>('inicio')
  const [dialogo, setDialogo] = useState<Dialogo>('nenhum')
  const [servidorFora, setServidorFora] = useState(false)
  const [chamados, setChamados] = useState<EstadoChamados>({ aberto: null, aba: 'dados' })
  const [maquinaPedida, setMaquinaPedida] = useState<string | null>(null)
  const tema = useTema()
  const edicao = useEdicao()
  const { mensagem, avisar } = useAvisoRapido()
  const agora = useAgora()
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
      avisar('Modo edição desligado.')
    } else {
      setDialogo('senha')
    }
  }

  const abrirMaquina = (nSemin: string) => {
    setMaquinaPedida(nSemin)
    setSecao('inventario')
  }

  const caminho = secao === 'chamados' && chamados.aberto ? ['Chamados', chamados.aberto] : [def.titulo]

  return (
    <div className="app">
      <Lateral secaoAtual={secao} aoEscolher={setSecao} aoAbrirConfiguracoes={() => setDialogo('configuracoes')} />

      <div className="conteudo">
        <Topo caminho={caminho} perfil={PERFIL_PROVISORIO} editando={edicao.ligado} />
        <Capa cena={def.cena} tema={tema.efetivo} />

        <main className="pagina">
          {servidorFora && (
            <p role="alert" className="pagina-em-breve">
              Servidor fora do ar. Confira se ele está rodando na porta 8080.
            </p>
          )}

          {/* Sempre montada: filtros, chamado aberto e aba continuam iguais ao voltar. */}
          <section hidden={secao !== 'chamados'} className="secao">
            <TelaChamados
              aberto={chamados.aberto}
              aba={chamados.aba}
              aoAbrir={(numero, aba) => setChamados({ aberto: numero, aba })}
              aoVoltar={() => setChamados({ aberto: null, aba: 'dados' })}
              aoAbrirMaquina={abrirMaquina}
              avisar={avisar}
              tecnicos={TECNICOS_PROVISORIOS}
              agora={agora}
            />
          </section>

          {secao !== 'chamados' && (
            <section className="secao">
              <CabecalhoPagina titulo={def.titulo} subtitulo={def.subtitulo} />
              {secao === 'inventario' && maquinaPedida && (
                <p className="pagina-em-breve">Ficha da máquina {maquinaPedida}: abre aqui quando a tela de Inventário ficar pronta.</p>
              )}
              <p className="pagina-em-breve">Esta tela entra nos próximos passos.</p>
            </section>
          )}
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
            avisar('Modo edição ligado. As alterações valem para todos.')
          }}
          aoCancelar={fecharDialogo}
        />
      )}
      {mensagem && (
        <div role="status" className="aviso-rapido">
          {mensagem}
        </div>
      )}
    </div>
  )
}

export default App
