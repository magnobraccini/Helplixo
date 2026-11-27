// Um jeito só de conversar com o servidor. Erros de regra chegam como {"erros": ["..."]}
// (ver web/servidor/.../TratadorDeErros.java) e viram um ErroDaApi com a lista pronta para a tela.

export class ErroDaApi extends Error {
  readonly status: number
  readonly erros: string[]

  constructor(status: number, erros: string[]) {
    super(erros.join(' '))
    this.name = 'ErroDaApi'
    this.status = status
    this.erros = erros
  }
}

export async function pedir<T>(url: string, opcoes?: RequestInit): Promise<T> {
  const resposta = await fetch(url, opcoes)
  if (!resposta.ok) {
    throw new ErroDaApi(resposta.status, await lerErros(resposta))
  }
  if (resposta.status === 204) {
    return undefined as T
  }
  return (await resposta.json()) as T
}

async function lerErros(resposta: Response): Promise<string[]> {
  try {
    const corpo = (await resposta.json()) as { erros?: unknown }
    if (Array.isArray(corpo.erros) && corpo.erros.length > 0) {
      return corpo.erros.map(String)
    }
  } catch {
    // corpo vazio ou que não é JSON: cai na mensagem genérica abaixo
  }
  return [`O servidor respondeu com erro ${resposta.status}. Tente de novo.`]
}
