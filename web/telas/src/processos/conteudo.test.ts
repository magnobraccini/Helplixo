import { describe, expect, it } from 'vitest'
import type { NoPasta, Processo } from '../api/processos'
import { blocosDoProcesso, contagem, ehCaminhoDePasta, filtrarArvore, idsDasPastas } from './conteudo'

const bloco = (tipo: Processo['conteudo'][number]['tipo'], texto: string, url: string | null = null) => ({ id: null, tipo, texto, url, negrito: false, italico: false, sublinhado: false })

const PROCESSO: Processo = {
  id: 'p1', pasta: 'f1', titulo: 'Configurar impressora de rede', descricao: 'Instalar pelo IP.', objetivo: 'Usuário imprimindo.',
  preRequisitos: null, responsavel: 'Sup. Técnico', categoria: null, tags: ['impressora'], passos: [],
  conteudo: [
    bloco('HEADING', '1. Antes de começar'),
    bloco('NUMBERED_LIST', 'Abra Configurações.\n\nClique em Adicionar.'),
    bloco('IMAGE', 'Tela de adicionar', 'anexos/a.png'),
    bloco('WARNING', 'Não instale drivers de fora.'),
    bloco('LINK', 'Suporte HP', 'https://support.hp.com/drivers'),
  ],
  observacoes: null, links: ['\\\\servidor\\ti\\drivers\\HP'], criadoEm: null, atualizadoEm: '2026-11-25T16:40:00',
}

describe('blocos do processo', () => {
  const blocos = blocosDoProcesso(PROCESSO)

  it('transforma cada tipo do Helplixo atual e numera os títulos para o índice', () => {
    expect(blocos.map((b) => b.tipo)).toEqual(['titulo', 'paragrafo', 'titulo', 'lista', 'imagem', 'aviso', 'link', 'titulo', 'pasta'])
    expect(blocos.filter((b) => b.tipo === 'titulo').map((b) => (b.tipo === 'titulo' ? `${b.id}:${b.texto}` : ''))).toEqual([
      'ps-0:Objetivo',
      'ps-1:1. Antes de começar',
      'ps-2:Links e pastas',
    ])
    expect(blocos[3]).toEqual({ tipo: 'lista', numerada: true, itens: ['Abra Configurações.', 'Clique em Adicionar.'] })
    expect(blocos[6]).toMatchObject({ tipo: 'link', site: 'support.hp.com' })
    expect(blocos[8]).toMatchObject({ tipo: 'pasta', caminho: '\\\\servidor\\ti\\drivers\\HP' })
  })

  it('conta seções, imagens e links como a maquete', () => {
    expect(contagem(blocos)).toBe('3 seções · 1 imagem(ns) · 0 anexo(s) · 2 link(s)')
  })

  it('passos do formato antigo viram uma seção "Passo a passo", em ordem', () => {
    const antigo = { ...PROCESSO, conteudo: [], objetivo: null, links: [], passos: [
      { id: null, numero: 2, titulo: 'Mapear', descricao: 'net use G:', resultadoEsperado: null, checklist: null },
      { id: null, numero: 1, titulo: 'Abrir o Explorer', descricao: null, resultadoEsperado: 'Explorer aberto', checklist: ['ok'] },
    ] }
    const b = blocosDoProcesso(antigo)
    expect(b[0]).toMatchObject({ tipo: 'titulo', texto: 'Passo a passo' })
    expect(b.slice(1).map((x) => (x.tipo === 'passo' ? x.numero : 0))).toEqual([1, 2])
  })
})

it('reconhece caminhos de pasta da rede e do Windows', () => {
  expect(ehCaminhoDePasta('\\\\servidor\\ti')).toBe(true)
  expect(ehCaminhoDePasta('G:\\Suporte')).toBe(true)
  expect(ehCaminhoDePasta('https://exemplo.gov.br')).toBe(false)
})

describe('árvore', () => {
  const arvore: NoPasta[] = [
    { id: 'a', nome: 'Rede', total: 2, processos: [{ id: 'p1', titulo: 'Mapear G:', atualizadoEm: null }], pastas: [
      { id: 'b', nome: 'Wi-Fi', total: 1, pastas: [], processos: [{ id: 'p2', titulo: 'Liberar Wi-Fi', atualizadoEm: null }] },
    ] },
    { id: 'c', nome: 'Impressoras', total: 1, pastas: [], processos: [{ id: 'p3', titulo: 'Fila', atualizadoEm: null }] },
  ]

  it('na busca, só ficam as pastas que levam aos processos achados', () => {
    const f = filtrarArvore(arvore, new Set(['p2']))
    expect(f.map((p) => p.nome)).toEqual(['Rede'])
    expect(f[0].processos).toEqual([])
    expect(f[0].pastas[0].processos.map((x) => x.id)).toEqual(['p2'])
    expect(f[0].total).toBe(1)
  })

  it('"Abrir todas" pega as pastas de todos os níveis', () => {
    expect(idsDasPastas(arvore)).toEqual(['a', 'b', 'c'])
  })
})
