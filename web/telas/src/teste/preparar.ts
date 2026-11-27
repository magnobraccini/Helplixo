// Roda antes de cada arquivo de teste: liga os "expect" do Testing Library
// (toBeInTheDocument, toHaveTextContent...) e limpa a tela entre um teste e outro.
import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { afterEach } from 'vitest'

afterEach(() => {
  cleanup()
})
