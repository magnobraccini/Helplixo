import { useCallback, useEffect, useState } from 'react'
import type { Chamado, StatusChamado } from './api/chamados'
import { buscarSaude } from './api/saude'
import { FILTRO_INICIAL, type Filtro } from './chamados/estado'
import type { AbaChamado } from './chamados/FichaChamado'
import { TelaChamados } from './chamados/TelaChamados'
import { TelaInicio } from './inicio/TelaInicio'
import type { Maquina } from './api/inventario'
import type { AbaMaquina } from './inventario/FichaMaquina'
import { TelaInventario } from './inventario/TelaInventario'
import type { DetalheProcesso } from './api/processos'
import { TelaProcessos } from './processos/TelaProcessos'
import { TelaContatos } from './contatos/TelaContatos'
import { Configuracoes } from './configuracoes/Configuracoes'
import { SenhaEdicao } from './configuracoes/SenhaEdicao'
import { useEdicao } from './edicao/useEdicao'
import { CabecalhoPagina } from './leiaute/CabecalhoPagina'
import { Capa } from './leiaute/Capa'
import { Lateral } from './leiaute/Lateral'
import { Topo } from './leiaute/Topo'
import { useAvisoRapido } from './leiaute/useAvisoRapido'
import './leiaute/leiaute.css'
import { definicaoDe, PERFIL_PROVISORIO, PESSOA_DO_PERFIL, TECNICOS_PROVISORIOS, type Secao } from './secoes'
import { useTema } from './tema/useTema'
import { lerRecentes, registrarRecente, type Recente } from './util/recentes'
import { useAgora } from './util/useAgora'

type Dialogo = 'nenhum' | 'configuracoes' | 'senha'

/** O chamado (ou a máquina) aberto e a aba ficam aqui para sobreviver à troca de seção. */
type EstadoChamados = { aberto: string | null; aba: AbaChamado }
type EstadoInventario = { aberta: string | null; aba: AbaMaquina }

function App() {
  const [secao, setSecao] = useState<Secao>('inicio')
  const [dialogo, setDialogo] = useState<Dialogo>('nenhum')
  const [servidorFora, setServidorFora] = useState(false)
  const [chamados, setChamados] = useState<EstadoChamados>({ aberto: null, aba: 'dados' })
  const [filtroChamados, setFiltroChamados] = useState<Filtro>(FILTRO_INICIAL)
  const [inventario, setInventario] = useState<EstadoInventario>({ aberta: null, aba: 'ident' })
  const [processoAberto, setProcessoAberto] = useState<string | null>(null)
  const [recentes, setRecentes] = useState<Recente[]>(lerRecentes)
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
    setInventario({ aberta: nSemin, aba: 'ident' })
    setSecao('inventario')
  }

  const abrirChamado = (numero: string) => {
    setChamados({ aberto: numero, aba: 'dados' })
    setSecao('chamados')
  }

  // Os cards do Início abrem a lista já filtrada (como na maquete: limpa técnico, secretaria e busca).
  const abrirListaChamados = (status: StatusChamado) => {
    setFiltroChamados((f) => ({ ...f, status, tecnico: '', secretaria: '', busca: '', ...(status === 'Fechado' ? { periodo: '30' as const } : {}) }))
    setChamados({ aberto: null, aba: 'dados' })
    setSecao('chamados')
  }

  const visitarChamado = useCallback((c: Chamado) => {
    setRecentes(
      registrarRecente({ tipo: 'chamado', id: c.numero, titulo: `Chamado ${c.numero} · ${c.problema}`, local: `Chamados · ${c.setor}`, quando: Date.now() }),
    )
  }, [])

  const visitarMaquina = useCallback((m: Maquina) => {
    setRecentes(registrarRecente({ tipo: 'maquina', id: m.nSemin, titulo: `${m.nSemin} · ${m.tipo} ${m.modelo}`, local: `Inventário · ${m.local}`, quando: Date.now() }))
  }, [])

  const visitarProcesso = useCallback((d: DetalheProcesso) => {
    setRecentes(registrarRecente({ tipo: 'processo', id: d.processo.id, titulo: d.processo.titulo, local: d.caminho.join(' › ') || 'Processos', quando: Date.now() }))
  }, [])

  const abrirProcesso = (id: string) => {
    setProcessoAberto(id)
    setSecao('processos')
  }

  const abrirRecente = (r: Recente) => (r.tipo === 'maquina' ? abrirMaquina(r.id) : r.tipo === 'processo' ? abrirProcesso(r.id) : abrirChamado(r.id))

  const caminho =
    secao === 'chamados' && chamados.aberto
      ? ['Chamados', chamados.aberto]
      : secao === 'inventario' && inventario.aberta
        ? ['Inventário', inventario.aberta]
        : [def.titulo]

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
              aoVisitarChamado={visitarChamado}
              filtro={filtroChamados}
              aoMudarFiltro={setFiltroChamados}
              avisar={avisar}
              tecnicos={TECNICOS_PROVISORIOS}
              agora={agora}
            />
          </section>

          {secao === 'inicio' && (
            <section className="secao">
              <TelaInicio
                perfil={PERFIL_PROVISORIO}
                eu={PESSOA_DO_PERFIL[PERFIL_PROVISORIO] ?? PERFIL_PROVISORIO}
                agora={agora}
                recentes={recentes.slice(0, 5)}
                aoAbrirChamado={abrirChamado}
                aoAbrirLista={abrirListaChamados}
                aoIrCalendario={() => setSecao('calendario')}
                aoAbrirRecente={abrirRecente}
              />
            </section>
          )}

          {/* Sempre montada: a busca, a máquina aberta e a aba continuam iguais ao voltar. */}
          <section hidden={secao !== 'inventario'} className="secao">
            <TelaInventario
              aberta={inventario.aberta}
              aba={inventario.aba}
              aoAbrir={(nSemin, aba) => setInventario({ aberta: nSemin, aba })}
              aoVoltar={() => setInventario({ aberta: null, aba: 'ident' })}
              aoAbrirChamado={abrirChamado}
              aoVisitar={visitarMaquina}
              vistas={recentes}
              editando={edicao.ligado}
              autoria={{ chave: edicao.chave, usuario: PESSOA_DO_PERFIL[PERFIL_PROVISORIO] ?? PERFIL_PROVISORIO }}
              avisar={avisar}
              tecnicos={TECNICOS_PROVISORIOS}
              agora={agora}
            />
          </section>

          {/* Sempre montada: a árvore aberta, a busca e o processo lido continuam iguais ao voltar. */}
          <section hidden={secao !== 'processos'} className="secao">
            <TelaProcessos
              aberto={processoAberto}
              aoAbrir={setProcessoAberto}
              aoFechar={() => setProcessoAberto(null)}
              aoVisitar={visitarProcesso}
              avisar={avisar}
            />
          </section>

          {secao === 'contatos' && (
            <section className="secao">
              <TelaContatos editando={edicao.ligado} chave={edicao.chave} avisar={avisar} />
            </section>
          )}

          {!['chamados', 'inicio', 'inventario', 'processos', 'contatos'].includes(secao) && (
            <section className="secao">
              <CabecalhoPagina titulo={def.titulo} subtitulo={def.subtitulo} />
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
