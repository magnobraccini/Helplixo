import { useEffect, useState } from 'react'
import { buscarChamado, listarChamados, type Chamado } from '../api/chamados'
import { CabecalhoPagina } from '../leiaute/CabecalhoPagina'
import { DIA, idade, inicioDoDia, instante, isoData } from '../util/datas'
import { formularioNovo, type DadosFormulario, type Filtro } from './estado'
import { FichaChamado, type AbaChamado } from './FichaChamado'
import { ListaChamados } from './ListaChamados'
import { intervaloDoPeriodo } from './regras'
import './chamados.css'

type Props = {
  /** Nº do chamado aberto na ficha (null = lista). Fica no App para sobreviver à troca de seção. */
  aberto: string | null
  aba: AbaChamado
  aoAbrir: (numero: string, aba: AbaChamado) => void
  aoVoltar: () => void
  aoAbrirMaquina: (nSemin: string) => void
  /** Chamado aberto na ficha (para o "Continue de onde parou"). */
  aoVisitarChamado: (c: Chamado) => void
  /** O filtro fica no App: os cards do Início abrem a lista já filtrada. */
  filtro: Filtro
  aoMudarFiltro: (f: Filtro) => void
  avisar: (mensagem: string) => void
  tecnicos: string[]
  agora: number
}

/** O que chegou do servidor e de qual pedido (chave). Enquanto a chave não bate, a lista está carregando. */
type Carga = { chave: string; doPeriodo: Chamado[]; recentes: Chamado[]; erro: string }

export function TelaChamados(p: Props) {
  const filtro = p.filtro
  const [carga, setCarga] = useState<Carga>({ chave: '', doPeriodo: [], recentes: [], erro: '' })
  const [detalhes, setDetalhes] = useState<Record<string, Chamado>>({})
  const [formularios, setFormularios] = useState<Record<string, DadosFormulario>>({})
  const [recarga, setRecarga] = useState(0)

  // O período decide o que vem do servidor; status, secretaria, técnico e busca filtram aqui.
  // (O pedido só muda quando vira o dia, por isso o "hoje" e não o minuto.)
  const hoje = isoData(new Date(p.agora))
  const intervalo = intervaloDoPeriodo(filtro.periodo, filtro.de, filtro.ate, inicioDoDia(p.agora))
  const de = intervalo?.de
  const ate = intervalo?.ate
  const chave = de && ate ? `${de}|${ate}|${hoje}|${recarga}` : ''
  const carregando = chave !== '' && carga.chave !== chave
  const doPeriodo = carga.doPeriodo

  useEffect(() => {
    if (!de || !ate) return
    let ativo = true
    const seteDias = isoData(new Date(instante(`${hoje}T00:00:00`) - 7 * DIA))
    Promise.all([listarChamados(de, ate), listarChamados(seteDias, hoje)])
      .then(([lista, ultimos]) => ativo && setCarga({ chave, doPeriodo: lista, recentes: ultimos, erro: '' }))
      .catch(
        () => ativo && setCarga((c) => ({ ...c, chave, erro: 'Não foi possível carregar os chamados. Confira se o servidor está no ar.' })),
      )
    return () => {
      ativo = false
    }
  }, [de, ate, hoje, chave])

  // Ao abrir a ficha, busca o chamado completo (pode ter vindo de um link, fora do período da lista).
  const aberto = p.aberto
  useEffect(() => {
    if (!aberto) return
    let ativo = true
    buscarChamado(aberto)
      .then((c) => {
        if (!ativo) return
        setDetalhes((d) => ({ ...d, [c.numero]: c }))
        p.aoVisitarChamado(c)
      })
      .catch(() => ativo && p.avisar(`Não foi possível abrir o chamado ${aberto}.`))
    return () => {
      ativo = false
    }
    // só quando muda o chamado aberto (as funções do App não mudam o que se busca)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [aberto])

  const chamado = aberto ? (detalhes[aberto] ?? doPeriodo.find((c) => c.numero === aberto) ?? null) : null

  const salvo = (c: Chamado, mensagem: string) => {
    setDetalhes((d) => ({ ...d, [c.numero]: c }))
    setCarga((k) => ({ ...k, doPeriodo: k.doPeriodo.map((x) => (x.numero === c.numero ? c : x)) }))
    setFormularios((f) => ({ ...f, [c.numero]: formularioNovo(c, Date.now()) }))
    p.avisar(mensagem)
  }

  const abrir = (numero: string, aba: AbaChamado) => {
    // O formulário do chamado é guardado: trocar de aba ou de seção não perde o que foi digitado.
    if (!formularios[numero]) {
      const c = doPeriodo.find((x) => x.numero === numero) ?? detalhes[numero]
      if (c) setFormularios((f) => ({ ...f, [numero]: formularioNovo(c, Date.now()) }))
    }
    p.aoAbrir(numero, aba)
  }

  if (aberto) {
    if (!chamado) {
      return (
        <>
          <CabecalhoPagina titulo={`Chamado ${aberto}`} subtitulo="Carregando…" />
        </>
      )
    }
    const formulario = formularios[chamado.numero] ?? formularioNovo(chamado, p.agora)
    return (
      <>
        <CabecalhoPagina
          titulo={`Chamado ${chamado.numero} · ${chamado.problema}`}
          subtitulo={`${chamado.contato} · ${chamado.setor} · ${chamado.nSemin ? `máquina ${chamado.nSemin}` : 'sem máquina'} · aberto ${idade(instante(chamado.abertoEm), p.agora)}`}
        />
        <FichaChamado
          chamado={chamado}
          aba={p.aba}
          aoTrocarAba={(aba) => abrir(chamado.numero, aba)}
          aoVoltar={() => {
            setRecarga((n) => n + 1)
            p.aoVoltar()
          }}
          formulario={formulario}
          aoMudarFormulario={(d) => setFormularios((f) => ({ ...f, [chamado.numero]: d }))}
          aoSalvar={salvo}
          aoAbrirChamado={(n) => abrir(n, 'dados')}
          aoAbrirMaquina={p.aoAbrirMaquina}
          tecnicos={p.tecnicos}
          agora={p.agora}
        />
      </>
    )
  }

  return (
    <>
      <CabecalhoPagina titulo="Chamados" subtitulo="Pedidos de suporte por período, secretaria e técnico." />
      <ListaChamados
        filtro={filtro}
        aoMudarFiltro={p.aoMudarFiltro}
        doPeriodo={doPeriodo}
        recentes={carga.recentes}
        textoPeriodo={intervalo?.texto ?? ''}
        carregando={carregando}
        erro={intervalo ? carga.erro : 'A data “De” precisa ser antes da data “Até”.'}
        tecnicos={p.tecnicos}
        agora={p.agora}
        aoVerDetalhes={(n) => abrir(n, 'dados')}
        aoAtualizar={(n) => abrir(n, 'resultado')}
        aoAbrirMaquina={p.aoAbrirMaquina}
      />
    </>
  )
}
