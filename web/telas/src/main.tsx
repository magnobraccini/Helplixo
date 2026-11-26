import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.tsx'
import { ProvedorEdicao } from './edicao/ContextoEdicao'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <ProvedorEdicao>
      <App />
    </ProvedorEdicao>
  </StrictMode>,
)
