// Transforma um processo do Helplixo atual nos blocos que a tela mostra (maquete: proc.blocos e proc.indice).
import type { NoPasta, Processo } from '../api/processos'

export type Estilo = { negrito: boolean; italico: boolean; sublinhado: boolean }

export type BlocoTela =
  | { tipo: 'titulo'; id: string; texto: string }
  | { tipo: 'paragrafo'; texto: string; estilo: Estilo }
  | { tipo: 'lista'; numerada: boolean; itens: string[] }
  | { tipo: 'codigo'; texto: string }
  | { tipo: 'aviso'; nivel: 'info' | 'alerta' | 'nota'; texto: string }
  | { tipo: 'imagem'; legenda: string; arquivo: string }
  | { tipo: 'link'; texto: string; url: string; site: string }
  | { tipo: 'pasta'; caminho: string; texto: string }
  | { tipo: 'passo'; numero: number; titulo: string; descricao: string; resultado: string; checklist: string[] }

/** Caminho de pasta do Windows ou da rede (\\servidor\ti, G:\Suporte): o navegador não abre, o sistema copia. */
export const ehCaminhoDePasta = (url: string) => /^(\\\\|[a-zA-Z]:\\)/.test(url.trim())

const site = (url: string) => {
  try {
    return new URL(url).host
  } catch {
    return url
  }
}

export function blocosDoProcesso(p: Processo): BlocoTela[] {
  const saida: BlocoTela[] = []
  let titulos = 0
  const titulo = (texto: string) => saida.push({ tipo: 'titulo', id: `ps-${titulos++}`, texto })
  const linhas = (t: string | null) => (t ?? '').split('\n').map((x) => x.trim()).filter(Boolean)

  if (p.objetivo) {
    titulo('Objetivo')
    saida.push({ tipo: 'paragrafo', texto: p.objetivo, estilo: { negrito: false, italico: false, sublinhado: false } })
  }
  if (p.preRequisitos) {
    titulo('Pré-requisitos')
    saida.push({ tipo: 'paragrafo', texto: p.preRequisitos, estilo: { negrito: false, italico: false, sublinhado: false } })
  }

  for (const b of p.conteudo) {
    const texto = b.texto ?? ''
    switch (b.tipo) {
      case 'HEADING':
        titulo(texto)
        break
      case 'PARAGRAPH':
        saida.push({ tipo: 'paragrafo', texto, estilo: { negrito: b.negrito, italico: b.italico, sublinhado: b.sublinhado } })
        break
      case 'BULLET_LIST':
      case 'NUMBERED_LIST':
        saida.push({ tipo: 'lista', numerada: b.tipo === 'NUMBERED_LIST', itens: linhas(texto) })
        break
      case 'CODE':
        saida.push({ tipo: 'codigo', texto })
        break
      case 'INFO':
        saida.push({ tipo: 'aviso', nivel: 'info', texto })
        break
      case 'WARNING':
        saida.push({ tipo: 'aviso', nivel: 'alerta', texto })
        break
      case 'NOTE':
        saida.push({ tipo: 'aviso', nivel: 'nota', texto })
        break
      case 'IMAGE':
        saida.push({ tipo: 'imagem', legenda: texto, arquivo: b.url ?? '' })
        break
      case 'LINK': {
        const url = b.url ?? texto
        if (ehCaminhoDePasta(url)) saida.push({ tipo: 'pasta', caminho: url, texto: texto || url })
        else saida.push({ tipo: 'link', texto: texto || url, url, site: site(url) })
        break
      }
    }
  }

  // Formato antigo do Helplixo: passos numerados.
  if (p.passos.length) {
    titulo('Passo a passo')
    for (const s of [...p.passos].sort((a, b) => a.numero - b.numero)) {
      saida.push({ tipo: 'passo', numero: s.numero, titulo: s.titulo ?? '', descricao: s.descricao ?? '', resultado: s.resultadoEsperado ?? '', checklist: s.checklist ?? [] })
    }
  }

  if (p.observacoes) {
    titulo('Observações')
    saida.push({ tipo: 'aviso', nivel: 'nota', texto: p.observacoes })
  }
  if (p.links.length) {
    titulo('Links e pastas')
    for (const url of p.links) {
      if (ehCaminhoDePasta(url)) saida.push({ tipo: 'pasta', caminho: url, texto: url })
      else saida.push({ tipo: 'link', texto: url, url, site: site(url) })
    }
  }
  return saida
}

/** "3 seções · 1 imagem(ns) · 0 anexo(s) · 2 link(s)" (como na maquete). */
export function contagem(blocos: BlocoTela[]): string {
  const n = (t: BlocoTela['tipo']) => blocos.filter((b) => b.tipo === t).length
  return `${n('titulo')} seções · ${n('imagem')} imagem(ns) · 0 anexo(s) · ${n('link') + n('pasta')} link(s)`
}

/** Filtra a árvore deixando só as pastas que levam a um dos processos achados. */
export function filtrarArvore(pastas: NoPasta[], ids: Set<string>): NoPasta[] {
  return pastas
    .map((p) => {
      const sub = filtrarArvore(p.pastas, ids)
      const processos = p.processos.filter((x) => ids.has(x.id))
      return { ...p, pastas: sub, processos, total: processos.length + sub.reduce((s, x) => s + x.total, 0) }
    })
    .filter((p) => p.total > 0)
}

/** Todos os ids de pasta (para "Abrir todas"). */
export const idsDasPastas = (pastas: NoPasta[]): string[] => pastas.flatMap((p) => [p.id, ...idsDasPastas(p.pastas)])
