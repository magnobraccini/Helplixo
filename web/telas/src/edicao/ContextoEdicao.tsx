import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { entrarNaEdicao, sairDaEdicao, situacaoDaEdicao } from '../api/edicao'
import { ContextoEdicao, type ValorEdicao } from './useEdicao'

// Modo edição: a senha é conferida SÓ no servidor (POST /api/edicao/entrar).
// A tela guarda apenas a chave de edição que o servidor devolve (vale 8 horas),
// na sessionStorage: some ao fechar o navegador. A senha nunca é guardada.

const CHAVE_SESSAO = 'helplixo-chave-edicao'
const MAIOR_ESPERA = 2_147_483_647 // maior espera aceita pelo setTimeout (em ms)

type Sessao = { chave: string; validaAte: string }

function lerSessao(): Sessao | null {
  try {
    const bruto = window.sessionStorage.getItem(CHAVE_SESSAO)
    if (!bruto) return null
    const s = JSON.parse(bruto) as Sessao
    return new Date(s.validaAte).getTime() > Date.now() ? s : null
  } catch {
    return null
  }
}

function gravarSessao(s: Sessao | null) {
  try {
    if (s) window.sessionStorage.setItem(CHAVE_SESSAO, JSON.stringify(s))
    else window.sessionStorage.removeItem(CHAVE_SESSAO)
  } catch {
    // sem sessionStorage: o modo edição vale só enquanto a página estiver aberta
  }
}

export function ProvedorEdicao({ children }: { children: ReactNode }) {
  const [sessao, setSessao] = useState<Sessao | null>(lerSessao)

  const trocarSessao = useCallback((s: Sessao | null) => {
    gravarSessao(s)
    setSessao(s)
  }, [])

  // Ao abrir a página com uma chave guardada, pergunta ao servidor se ela ainda vale
  // (o servidor pode ter sido reiniciado).
  const chaveInicial = sessao?.chave
  useEffect(() => {
    if (!chaveInicial) return
    let ativo = true
    situacaoDaEdicao(chaveInicial)
      .then((s) => {
        if (ativo && !s.ligado) trocarSessao(null)
      })
      .catch(() => {
        // servidor fora do ar: mantém; a próxima ação de edição vai dizer se a chave caiu
      })
    return () => {
      ativo = false
    }
    // só na abertura da página
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  // Desliga sozinho quando passam as 8 horas.
  // O setTimeout aceita no máximo ~24,8 dias; acima disso ele dispararia na hora.
  // Por isso o relógio é limitado e, se acordar antes do fim, confere de novo.
  useEffect(() => {
    if (!sessao) return
    const fim = new Date(sessao.validaAte).getTime()
    const relogio = window.setTimeout(
      () => (Date.now() >= fim ? trocarSessao(null) : setSessao({ ...sessao })),
      Math.min(Math.max(0, fim - Date.now()), MAIOR_ESPERA),
    )
    return () => window.clearTimeout(relogio)
  }, [sessao, trocarSessao])

  const entrar = useCallback(
    async (senha: string) => {
      const entrada = await entrarNaEdicao(senha)
      trocarSessao({ chave: entrada.chave, validaAte: entrada.validaAte })
    },
    [trocarSessao],
  )

  const sair = useCallback(async () => {
    const chave = sessao?.chave
    trocarSessao(null)
    if (chave) {
      try {
        await sairDaEdicao(chave)
      } catch {
        // mesmo se o servidor não responder, a tela já saiu do modo edição
      }
    }
  }, [sessao, trocarSessao])

  const valor = useMemo<ValorEdicao>(
    () => ({ ligado: sessao !== null, chave: sessao?.chave ?? null, entrar, sair }),
    [sessao, entrar, sair],
  )

  return <ContextoEdicao.Provider value={valor}>{children}</ContextoEdicao.Provider>
}
