import type { Chamado } from '../api/chamados'
import type { Externo, Interno } from '../api/contatos'
import type { ItemMaquina } from '../api/inventario'
import type { ItemBusca } from '../api/processos'

// A busca única (Ctrl K) da maquete (web/docs/maquete-v32.dc.html, linhas 3123 a 3135):
// cinco grupos, até 5 itens cada quando há texto (3 sem texto), e grupos vazios somem.

export type Destino =
  | { tipo: 'chamado'; id: string }
  | { tipo: 'maquina'; id: string }
  | { tipo: 'processo'; id: string }
  | { tipo: 'contato'; visao: 'internos' | 'externos'; q: string }

export type ItemGrupo = { chave: string; codigo: string; rotulo: string; dica: string; destino: Destino }
export type Grupo = { nome: string; itens: ItemGrupo[] }

export type Achados = {
  chamados: Chamado[]
  maquinas: ItemMaquina[]
  processos: ItemBusca[]
  internos: Interno[]
  externos: Externo[]
}

/** Máquinas, processos e contatos só são pedidos ao servidor a partir de 2 letras (como na busca do Inventário). */
export const MINIMO_LETRAS = 2

export function montarGrupos(q: string, a: Achados): Grupo[] {
  const bq = q.trim().toLowerCase()
  const casa = (txt: string) => !bq || txt.toLowerCase().includes(bq)
  const grupos: Grupo[] = [
    {
      nome: 'Chamados',
      itens: a.chamados
        .filter((c) => casa(`${c.numero} ${c.nSemin ?? ''} ${c.contato} ${c.setor} ${c.problema}`))
        .map((c) => ({
          chave: `c-${c.numero}`,
          codigo: c.numero,
          rotulo: `${c.problema} · ${c.contato}`,
          dica: `${c.nSemin || 'sem máquina'} · ${c.status}`,
          destino: { tipo: 'chamado', id: c.numero },
        })),
    },
    {
      nome: 'Máquinas',
      itens: a.maquinas.map((m) => ({
        chave: `m-${m.nSemin}`,
        codigo: m.nSemin,
        rotulo: `${m.tipo} ${m.modelo}`,
        dica: `${m.usuario ?? 'sem usuário'} · ${m.local}`,
        destino: { tipo: 'maquina', id: m.nSemin },
      })),
    },
    {
      nome: 'Processos',
      itens: a.processos.map((p) => ({
        chave: `p-${p.id}`,
        codigo: 'processo',
        rotulo: p.titulo,
        dica: p.caminho,
        destino: { tipo: 'processo', id: p.id },
      })),
    },
    {
      nome: 'Contatos internos',
      itens: a.internos.map((c) => ({
        chave: `i-${c.ramal}-${c.nome}`,
        codigo: `ramal ${c.ramal}`,
        rotulo: c.nome,
        dica: c.setor,
        destino: { tipo: 'contato', visao: 'internos', q: c.nome },
      })),
    },
    {
      nome: 'Contatos externos',
      itens: a.externos.map((x) => ({
        chave: `x-${x.empresa}`,
        codigo: 'externo',
        rotulo: x.empresa,
        dica: x.servico,
        destino: { tipo: 'contato', visao: 'externos', q: x.empresa },
      })),
    },
  ]
  return grupos.map((g) => ({ ...g, itens: g.itens.slice(0, bq ? 5 : 3) })).filter((g) => g.itens.length > 0)
}
