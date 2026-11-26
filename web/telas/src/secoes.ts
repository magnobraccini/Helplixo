// As abas do menu lateral, com emoji, título e subtítulo copiados da maquete (navDefs e pages).
import type { Cena } from './leiaute/capaPaletas'

export type Secao = 'inicio' | 'chamados' | 'inventario' | 'processos' | 'contatos' | 'calendario' | 'ferramentas'

export type DefinicaoSecao = {
  chave: Secao
  rotulo: string
  emoji: string
  titulo: string
  subtitulo: string
  cena: Cena
}

export const SECOES: DefinicaoSecao[] = [
  { chave: 'inicio', rotulo: 'Início', emoji: '\u{1F3E0}', titulo: 'Início', subtitulo: '', cena: 'inicio' },
  { chave: 'chamados', rotulo: 'Chamados', emoji: '\u{1F3AB}', titulo: 'Chamados', subtitulo: 'Pedidos de suporte por período, secretaria e técnico.', cena: 'chamados' },
  { chave: 'inventario', rotulo: 'Inventário', emoji: '\u{1F5A5}\u{FE0F}', titulo: 'Inventário', subtitulo: 'Procure a máquina pelo Nº Semin, patrimônio, usuário ou setor.', cena: 'inventario' },
  { chave: 'processos', rotulo: 'Processos', emoji: '\u{1F4DA}', titulo: 'Processos', subtitulo: 'O passo a passo dos atendimentos, organizado em pastas.', cena: 'processos' },
  { chave: 'contatos', rotulo: 'Contatos', emoji: '\u{1F4C7}', titulo: 'Contatos', subtitulo: 'Ramais da prefeitura, por setor.', cena: 'contatos' },
  { chave: 'calendario', rotulo: 'Calendário', emoji: '\u{1F4C5}', titulo: 'Calendário', subtitulo: 'Feriados, manutenções, ausências, contratos e garantias. Expediente: 8h às 14h.', cena: 'calendario' },
  { chave: 'ferramentas', rotulo: 'Ferramentas', emoji: '\u{1F9F0}', titulo: 'Ferramentas', subtitulo: 'Comandos e programas do dia a dia.', cena: 'ferramentas' },
]

export function definicaoDe(secao: Secao): DefinicaoSecao {
  return SECOES.find((s) => s.chave === secao) ?? SECOES[0]
}

/** Perfil de quem está usando. Provisório até existir o login (o servidor usa o cabeçalho X-Cargo). */
export const PERFIL_PROVISORIO = 'Triagem'
