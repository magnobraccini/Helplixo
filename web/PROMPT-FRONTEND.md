# Prompt de continuidade do Helplixo Web: as telas

Para usar, abra uma conversa nova do Claude Code e cole tudo abaixo da linha. Há dois jeitos:

- **Na nuvem (mais simples hoje):** abra a conversa a partir do repositório `magnobraccini/Helplixo` no GitHub. Não precisa instalar nada no computador.
- **No computador do Magno:** abra a conversa na pasta `C:\Users\magnoC.dgi\Projetos\Helplixo`. Antes, instale o **Node.js LTS** (em 26/11/2026 ele ainda não estava instalado).

---

Vamos continuar o **Helplixo Web**, a versão web do sistema de suporte da TI da prefeitura. O servidor está pronto, testado e já está no GitHub. Agora vamos fazer as **telas** (frontend).

## Situação de hoje (26/11/2026)

- O repositório é **`magnobraccini/Helplixo`** (branch principal `master`). Ele guarda dois programas:
  - o **Helplixo atual** (Java Swing), fora da pasta `web/`: **não mexa nele**;
  - o **Helplixo Web**, dentro de `web/`, que já está na `master` (pull request #5).
- Dentro de `web/`:
  - `servidor/`: Spring Boot 3.5.0, Java 21, 73 testes (`mvn test` dentro de `web/servidor`).
  - `PROXIMOS-PASSOS.md`: o plano e o que já foi feito. **Leia primeiro.**
  - `.gitignore`: já ignora `telas/node_modules/` e `telas/dist/`.
- Ainda não existe nada de telas.

## Como trabalhar comigo

- Fale em **português**, em modo ensino: um passo de cada vez, explicações curtas, uma analogia e uma pergunta de revisão no fim de cada etapa.
- No fim de cada entrega, faça **perguntas numeradas** para eu responder. Ao fechar uma etapa grande, faça um **resumo do que foi feito, com lugares para estudar** cada assunto (livros, guias oficiais, documentação).
- **Siga o plano sem pedir licença para continuar.** Peça permissão só antes de:
  - mudar algo já decidido (regras de negócio, visual, escopo);
  - mexer no Helplixo atual (o programa Swing) ou na pasta da rede;
  - enviar código para o GitHub (push) quando estiver no meu computador;
  - fazer merge na `master` (nunca faça sem a minha ordem).
- **Commit sem perguntar**, ao fim de cada passo com os testes passando.
  - Antes de cada commit, confira que nenhuma senha está nos arquivos.
  - Termine as mensagens de commit com `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
  - Autor dos commits: **Magno Braccini <braccinimagno@gmail.com>**. Confira com `git config user.email`.
  - No Windows: escreva a mensagem num arquivo UTF-8 **sem BOM** e use `git commit -F arquivo`. No PowerShell, `git grep` sem resultado devolve código 1, e isso não é erro.

## Onde trabalhar

- **No meu computador:** use a branch `helplixo-web-telas`, que já existe no clone e começa na `master` com o servidor. Se ela não existir, crie a partir de `origin/master`. Faça push só com a minha confirmação.
- **Na nuvem:** use a branch da sessão, a partir da `master`, e entregue por pull request para a `master`, sem merge. Na nuvem não existem os caminhos `C:\` e `G:\`, a memória local nem a cópia `helplixo-dados-teste`: trabalhe só com o que está em `web/`. Os dados de teste do servidor ficam em `web/servidor/src/main/resources/dados-teste/`.

## Regras que nunca podem ser quebradas

- **Só a pasta `web/` muda.** Nada do programa Swing (`src/`, `pom.xml` da raiz, `scripts/`, `.bat`) é alterado.
- Nunca alterar, mover ou apagar as pastas de dados (data, processos, ramais, config, backups). Na pasta de rede `G:\Suporte\Suporte\Helplixo`, só o arquivo `.jar` pode ser substituído, e só depois da minha confirmação.
- **Nenhuma senha vai para arquivos do repositório** (nem a senha de edição, nem a senha de teste do Helplixo): nem no código, nem nos testes, na documentação ou nas mensagens de commit.
  - A senha de edição vem da variável de ambiente `HELPLIXO_SENHA_EDICAO`.
  - Nos testes, use uma senha inventada (os testes do servidor usam `senha-do-teste-unitario`).
  - Para rodar o servidor com o modo edição, defina a variável só no terminal, com uma senha inventada.

## Pré-requisitos

- **Node.js LTS:** confira com `node -v` e `npm -v`. Se não existir no meu computador, me avise e pare. Na nuvem ele costuma estar disponível.
- **Java 21+ e Maven** para rodar o servidor: `mvn spring-boot:run` dentro de `web/servidor` (porta 8080).

## Material de referência

- **Maquete aprovada**, com o visual e o comportamento de todas as telas: https://claude.ai/artifact/YKUfPne25G3Kt8vu67gsUd
- **Regras de negócio** (documento): https://claude.ai/artifact/5rud8uZ6ugMsvGSfwDCoaz
- Leia os dois com a ferramenta de artifacts. Se ela não estiver disponível (por exemplo, na nuvem), me avise: eu colo o trecho que for preciso.

## O que fazer

1. **Criar `web/telas/`** com **React + TypeScript (Vite)**, falando com a API do servidor. Em desenvolvimento, use o proxy do Vite para `http://localhost:8080`.
2. **Visual estilo Notion**, como na maquete:
   - **Tema claro creme:** fundo `#FAF6EE`, lateral `#F3EEE3`, cartões `#FFFDF8`, tinta `#37352F`, texto secundário `#5F5E5B`, apagado `#787774`, linhas `#E7E0D1`, ação azul `#2383E2`.
   - **Tema escuro estilo Notion:** fundo `#191919`, lateral `#202020`, cartões `#202020`, tinta `#E3E2DF`.
   - As cores de todos os selos de status, nos dois temas, estão no código da maquete (blocos `:root{--c-...}` e `[data-tema="escuro"]{...}`). Copie de lá.
   - **Configurações** (⚙️ na lateral): Aparência Claro / Escuro / Igual ao sistema (salvo no computador) e o interruptor do **Modo edição**, que pede a senha.
   - Fonte do sistema (Segoe UI no Windows), cantos pequenos (4 a 8 px), sem trocas bruscas de cor.
   - Menu lateral com **emojis**: 🏠 Início, 🎫 Chamados, 🖥️ Inventário, 📚 Processos, 📇 Contatos, 📅 Calendário, 🧰 Ferramentas.
   - Capa ilustrada no topo de cada aba (a do macaco Zenas no Início), com **as mesmas cores nos dois temas** e **sem o balão de fala**. O caminho, "Você: <perfil>" e o sino ficam sobre a capa, sem faixa branca.
3. **Telas, nesta ordem**, ligadas à API:
   1. **Chamados**
      - Lista com filtros de status, período, secretaria, técnico e busca.
      - Clicar na linha mostra a descrição; botões "Ver detalhes" e "Atualizar"; Nº Semin clicável.
      - Marca ↺ quando a máquina teve outros chamados nos últimos 7 dias.
      - Coluna Min. com cores: aberto verde até 2 h, âmbar até 3 h, vermelho acima; em andamento azul; fechado e não computável cinza.
      - Ficha com abas **Dados → Resultado → Histórico da máquina**.
      - O Resultado é **um formulário só**, com Status (Aberto / Em andamento / Fechado). A anotação só é obrigatória para fechar.
      - Ao trocar de seção e voltar, o chamado e a aba continuam abertos.
   2. **Início**
      - Saudação.
      - 4 números clicáveis: Abertos, Em andamento, Esperando há mais tempo (só os abertos), Fechados hoje (com a média de 30 dias).
      - "Precisa de atenção", "Com você" ou "Sem técnico", Hoje e Amanhã do calendário, "Continue de onde parou".
   3. **Inventário:** busca primeiro (pelo menos 2 letras); ficha com 4 abas (Identificação, Compra, Softwares com o Office, Histórico); editar, movimentar e dar baixa.
   4. **Processos:** árvore à esquerda e o processo **ao lado**, com o índice "Neste processo" que marca a seção ao rolar.
   5. **Contatos:** Internos e Externos com filtros e cadastro.
   6. **Calendário:** visões Dia, Semana, Mês e Agenda; agendas por tipo; lembretes no sino.
4. **Para cada tela:** testes (Vitest + Testing Library), os testes do servidor continuando a passar (`mvn test`), commit e atualização do `web/PROXIMOS-PASSOS.md`.

## A API do servidor (resumo)

- **Erros:** sempre `{"erros": ["..."]}`. 422 = regra quebrada, 403 = falta o modo edição, 401 = senha errada, 429 = espere, 503 = modo edição não configurado, 404 = não existe.
- **Modo edição:**
  - `POST /api/edicao/entrar` com `{"senha"}` devolve `{chave, validaAte}` (vale 8 horas).
  - `POST /api/edicao/sair` e `GET /api/edicao`.
  - As ações de edição mandam o cabeçalho `X-Chave-Edicao`.
- **Quem faz** (até existir o login): cabeçalhos `X-Usuario` (nome) e `X-Cargo` (Triagem, Técnico ou Chefe de TI).
- **Chamados:**
  - `GET /api/chamados?de=&ate=&status=` (sem datas = 30 dias) e `GET /api/chamados/{n}`.
  - `POST /api/chamados/{n}/resultado` com `{status, anotacao, quando, tecnico}` devolve `{chamado, avisos}`.
- **Processos:** `GET /api/processos/arvore`, `GET /api/processos/{id}` (com o `caminho`), `GET /api/processos?q=`.
- **Inventário:**
  - `GET /api/inventario/resumo`, `GET /api/inventario?q=`, `GET /api/inventario/{nSemin}` (ficha com `office`, `garantia` e `historico`).
  - `POST /api/inventario` e `PUT /api/inventario/{nSemin}` (este devolve `mudancas`).
  - `POST /api/inventario/{nSemin}/movimentacoes`, e `POST` e `DELETE /api/inventario/{nSemin}/baixa`.
- **Contatos:** `GET /api/contatos/internos?secretaria=&andar=&ordem=&q=`, `GET /api/contatos/externos?categoria=&situacao=&q=` e `POST` nos dois.
- **Calendário:**
  - `GET /api/calendario?de=&ate=&tipos=&q=` e `GET /api/calendario/lembretes`.
  - `POST /api/calendario` (Aviso: qualquer cargo; o resto: modo edição) e `DELETE /api/calendario/{id}`.
- **Saúde:** `GET /api/saude`.
- Na dúvida sobre um campo, leia o controller em `web/servidor/src/main/java/helplixo/web/`.

## O que ainda falta no servidor (só se a tela precisar)

- Adicionar software à máquina.
- Movimentação junto do fechamento do chamado.
- Registrar chamado novo.
- Criar e editar processos com anexos. Limites: imagens até 10 MB, documentos até 25 MB, .zip até 100 MB, programas só dentro de .zip, 200 MB por processo.
- Processos parecidos (palavras em comum, mínimo 2).
- Login com permissões por perfil.
- Banco de dados.

Se uma tela precisar de algo desta lista, me diga antes e proponha a mudança no servidor com testes.

---

Comece lendo o `web/PROXIMOS-PASSOS.md` e conferindo os pré-requisitos. Depois me explique em uma frase o que vai fazer no primeiro passo (criar `web/telas/`).
