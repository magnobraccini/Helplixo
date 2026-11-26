# Helplixo Web: telas

Frontend do Helplixo Web: React + TypeScript com Vite. Testes com Vitest + Testing Library.

## Comandos (dentro de `web/telas`)

| Comando | O que faz |
| --- | --- |
| `npm install` | Instala as dependências (só na primeira vez ou quando o `package.json` mudar). |
| `npm run dev` | Abre as telas em http://localhost:5173. Tudo em `/api` vai para o servidor em http://localhost:8080. |
| `npm test` | Roda os testes uma vez. |
| `npm run test:watch` | Roda os testes de novo a cada arquivo salvo. |
| `npm run build` | Confere os tipos e gera a versão final em `dist/`. |
| `npm run lint` | Procura problemas comuns no código. |

Para usar as telas com dados, suba antes o servidor: `mvn spring-boot:run` em `web/servidor`.

Referências visuais e de regras: `web/docs/`.
