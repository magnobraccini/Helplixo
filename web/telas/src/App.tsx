import { useEffect, useState } from 'react'
import { buscarSaude } from './api/saude'

type Conexao = 'conferindo' | 'no-ar' | 'fora-do-ar'

const TEXTO: Record<Conexao, string> = {
  'conferindo': 'Conferindo o servidor…',
  'no-ar': 'Servidor no ar.',
  'fora-do-ar': 'Servidor fora do ar. Confira se ele está rodando na porta 8080.',
}

function App() {
  const [conexao, setConexao] = useState<Conexao>('conferindo')

  useEffect(() => {
    let ativo = true
    buscarSaude()
      .then(() => ativo && setConexao('no-ar'))
      .catch(() => ativo && setConexao('fora-do-ar'))
    return () => {
      ativo = false
    }
  }, [])

  return (
    <main>
      <h1>Helplixo Web</h1>
      <p role="status">{TEXTO[conexao]}</p>
    </main>
  )
}

export default App
