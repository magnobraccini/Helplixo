# Helplixo Web: próximos passos

Pausa em 25/11/2026. Este arquivo diz onde paramos e o que fazer ao voltar.

> **Onde o projeto mora agora (26/11/2026):** na pasta `web/` do repositório `magnobraccini/Helplixo`, branch `helplixo-web`, com todo o histórico trazido por `git subtree`. O código Swing do Helplixo atual continua fora de `web/`, sem mudanças. O repositório local `C:\Users\magnoC.dgi\Projetos\Helplixo-Web` ficou só como cópia de segurança. Os caminhos citados abaixo (como `servidor/`) são relativos a `web/`.

## Onde paramos

- A maquete está aprovada e publicada (v32): https://claude.ai/artifact/YKUfPne25G3Kt8vu67gsUd
- As regras de negócio estão no documento: https://claude.ai/artifact/5rud8uZ6ugMsvGSfwDCoaz
- O projeto web começou nesta pasta. Só existe o esqueleto do servidor:
  - `servidor/pom.xml` (Spring Boot 3.5.0, Java 21)
  - `servidor/src/main/java/helplixo/web/HelplixoWebApplication.java`
  - `servidor/src/main/resources/application.properties`
- Nada foi compilado, testado nem enviado ao Git ainda.

## Antes de continuar (com você)

1. Instalar o **Node.js LTS** pelo site oficial (nodejs.org). Depois, conferir num terminal novo:
   `node -v` e `npm -v`.
2. ✅ O Maven baixa dependências pela rede da prefeitura sem problema.

## Passo a passo do servidor

Feito em 26/11/2026 (ainda sem commit):

1. ✅ **Git**: repositório iniciado (branch `main`) com `.gitignore` (target, node_modules, arquivos de senha e `.env`).
2. ✅ **Chamados**: `Chamado`, `StatusChamado` e os 11 chamados de teste da maquete em `src/main/resources/dados-teste/chamados.json`.
3. ✅ **API de chamados**: `GET /api/chamados?de=&ate=&status=` (sem datas = últimos 30 dias, filtro no servidor), `GET /api/chamados/{n}`, `GET /api/saude`.
4. ✅ **Testes**: 8 testes passando (`mvn test` na pasta `servidor`).

5. ✅ **Atualizar chamado**: `POST /api/chamados/{n}/resultado` com o formulário único (status, anotação, data e hora, técnico). Anotação obrigatória só para fechar; reabrir guarda o fechamento anterior no andamento; fora do expediente só avisa. Erros de regra voltam como 422 `{"erros": [...]}`. 18 testes passando. Falta: movimentação de equipamento junto do fechamento e a permissão por perfil (depois do login).

A seguir:

6. ✅ **Processos** (só leitura): lê `folders.json` e `processes/*.json` no formato do Helplixo atual. `GET /api/processos/arvore`, `GET /api/processos/{id}` (com o caminho de pastas) e `GET /api/processos?q=` (ignora acentos). Amostra em `src/main/resources/dados-teste/helplixo`; para ler a cópia de teste, mudar `helplixo.dados.processos` no `application.properties` (nunca a pasta da rede). 27 testes passando. Falta: criar e editar processos, anexos e processos parecidos.
7. ✅ **Senha de edição** conferida no servidor, vinda da variável de ambiente `HELPLIXO_SENHA_EDICAO` (nunca no código nem no Git; sem a variável, o modo edição fica desligado). `POST /api/edicao/entrar` dá uma chave de edição de 8 horas, `POST /api/edicao/sair`, `GET /api/edicao`. As ações de edição vão mandar a chave no cabeçalho `X-Chave-Edicao`. 5 erros seguidos: espera de 1 minuto. 36 testes passando.
8. ✅ **Inventário (leitura)**: as 14 máquinas da maquete em `dados-teste/maquinas.json`. `GET /api/inventario/resumo`, `GET /api/inventario?q=` (pelo menos 2 letras; Nº Semin exato primeiro; até 20 resultados) e `GET /api/inventario/{nSemin}` (ficha com Office, garantia e histórico de consertos e chamados). 44 testes passando.
9. ✅ **Inventário (edição)**: `POST /api/inventario` (adicionar), `PUT /api/inventario/{nSemin}` (editar; Nº Semin e patrimônio nunca mudam; o que mudou vai para o histórico), `POST /api/inventario/{nSemin}/movimentacoes`, `POST` e `DELETE /api/inventario/{nSemin}/baixa`. Adicionar, editar e baixa exigem a chave de edição; quem fez vem do cabeçalho `X-Usuario` até existir o login. 55 testes passando. Falta: adicionar software à máquina e a movimentação junto do fechamento do chamado.
10. ✅ **Contatos**: `GET /api/contatos/internos` (filtros secretaria, andar, ordem e busca) e `GET /api/contatos/externos` (categoria, situação do contrato, em ordem de vencimento), com as opções dos filtros tiradas dos próprios contatos. Cadastro com a chave de edição; ramal repetido é aceito e avisa com quem é compartilhado. 62 testes passando.
11. ✅ **Calendário**: `GET /api/calendario?de=&ate=&tipos=&q=` (ocorrências dia a dia, com períodos e repetição semanal, mensal ou anual com fim), `POST /api/calendario` (Aviso: qualquer cargo; o resto: modo edição), `DELETE /api/calendario/{id}` (autor ou cargo acima no mesmo setor), `GET /api/calendario/lembretes`. Feriados, contratos (Contatos) e garantias (Inventário) entram sozinhos. Quem marca vem dos cabeçalhos `X-Usuario` e `X-Cargo` até existir o login. 73 testes passando.

Pendências do servidor (pequenas): adicionar software à máquina; movimentação junto do fechamento do chamado; registrar chamado novo; criar e editar processos com anexos; processos parecidos; login (trocar os cabeçalhos `X-Usuario`/`X-Cargo` e aplicar as permissões por perfil); banco de dados no lugar dos arquivos de teste.

## Passo a passo das telas (depois do Node)

1. ✅ Criar `telas/` com React + TypeScript (Vite 8, React 19, TypeScript 7). Proxy de `/api` para `http://localhost:8080` (conferido com o servidor no ar). Testes com Vitest + Testing Library + jsdom: 2 testes passando (`npm test` na pasta `telas`). Cópias da maquete v32 e das regras em `docs/` (sem a senha de teste da maquete).
2. ✅ **Tema e estrutura** (26/11/2026):
   - Tokens `--c-...` copiados das linhas 14 e 15 da maquete (`telas/src/estilos/tokens.css`). Um teste confere que continuam idênticos.
   - Menu lateral com emojis, capa ilustrada por aba (gerada a partir do SVG da maquete em `CenasCapa.tsx`, sem balão de fala; no escuro usa a paleta noturna), topo com caminho, `Você: <perfil>` e sino.
   - Configurações: tema Claro/Escuro/Sistema (salvo em `localStorage`, chave `helplixo-tema`) e interruptor do Modo Edição. A senha vai para `POST /api/edicao/entrar`; a tela guarda só a chave de 8 horas (`sessionStorage`), confere a chave ao abrir e desliga sozinha quando ela vence.
   - 13 testes passando (`npm test`).
   - Provisório: o perfil é fixo em "Triagem" até existir o login; o sino ainda não tem notificações (entram com Chamados e Calendário); a busca única (Ctrl K) e o número de abertos ao lado de "Chamados" ficam para as telas que os alimentam.
3. ✅ **Chamados** (27/11/2026), ligada a `GET /api/chamados`, `GET /api/chamados/{n}`, `POST /api/chamados/{n}/resultado` e `GET /api/inventario/{nSemin}`:
   - Lista: abas de status com contagem, filtros (período no servidor; secretaria, técnico e busca na tela), linha que abre a descrição e a última atualização, "Ver detalhes" e "Atualizar", Nº Semin clicável (leva ao Inventário), `↺` (outros chamados da máquina nos últimos 7 dias) e `Min.` colorida pela regra.
   - Ficha: abas Dados → Resultado → Histórico da máquina; formulário único de Resultado (o servidor confere as regras; a tela mostra os erros 422 e a "Conferência do horário"); andamento e "Atendido".
   - O chamado aberto, a aba e o que foi digitado continuam iguais ao trocar de seção.
   - 37 testes passando. Contrato conferido com o servidor no ar (lista, 422, 404 e ficha da máquina).
   - Ficou para depois (pendências do servidor ou fora da lista desta etapa): registrar chamado, assumir, trocar técnico na linha, PDF e Excel, movimentação junto do fechamento, processos parecidos. Técnicos fixos (Magno, Diego, Rafael) até existir o cadastro de usuários.
4. Início, depois Inventário, Processos, Contatos e Calendário.

## Regras que continuam valendo

- Nunca alterar, mover ou apagar as pastas de dados (data, processos, ramais, config, backups).
- Na pasta de rede, só o `.jar` pode ser trocado, e só depois da sua confirmação.
- Senhas nunca vão para o Git.
