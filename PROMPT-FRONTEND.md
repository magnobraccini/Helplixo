# Prompt de continuidade do Helplixo Web

Este prompt tem duas partes:

- **Parte 1, no computador do Magno (local):** levar o projeto Helplixo Web para o repositório do GitHub `magnobraccini/Helplixo`, como a pasta `web/`.
- **Parte 2, local ou na nuvem:** desenvolver as telas (frontend).

Para usar: abra uma conversa nova do Claude Code **no computador do Magno**, na pasta `C:\Users\magnoC.dgi\Projetos\Helplixo`, e cole tudo abaixo da linha. Depois da Parte 1, a Parte 2 pode continuar na mesma conversa ou numa conversa na nuvem aberta a partir do repositório do GitHub.

---

Vamos continuar o **Helplixo Web**, a versão web do sistema de suporte da TI da prefeitura. O servidor está pronto e testado. Primeiro o projeto precisa ir para o GitHub (Parte 1); depois vêm as telas (Parte 2).

## Como trabalhar comigo

- Fale em **português**, em modo ensino: um passo de cada vez, explicações curtas, uma analogia e uma pergunta de revisão no fim de cada etapa.
- No fim de cada entrega, faça **perguntas numeradas** para eu responder. Ao fechar uma etapa grande, faça um **resumo do que foi feito, com lugares para estudar** cada assunto (livros, guias oficiais, documentação).
- **Siga o plano sem pedir licença para continuar.** Peça permissão só antes de:
  - mudar algo já decidido (regras de negócio, visual, escopo);
  - mexer no Helplixo atual (o programa Swing) ou na pasta da rede;
  - enviar código para o GitHub (push).
- **Commit e integração local sem perguntar**, ao fim de cada passo com os testes passando.
  - Antes de cada commit, confira que nenhuma senha está nos arquivos.
  - Termine as mensagens de commit com `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
  - No Windows, escreva a mensagem num arquivo UTF-8 **sem BOM** e use `git commit -F arquivo` (o pipe do PowerShell adiciona um caractere invisível no começo).
  - No PowerShell, `git grep` sem resultado devolve código 1: isso não é erro.
- Autor dos commits: **Magno Braccini <braccinimagno@gmail.com>**. Confira com `git config user.email` no repositório onde for commitar.

## Regras que nunca podem ser quebradas

- Nunca alterar, mover ou apagar as pastas de dados (data, processos, ramais, config, backups). Na pasta de rede `G:\Suporte\Suporte\Helplixo`, só o arquivo `.jar` pode ser substituído, e só depois da minha confirmação.
- **Nenhuma senha vai para arquivos do repositório**: nem a senha de edição, nem a senha de teste do Helplixo.
  - A senha de edição vem da variável de ambiente `HELPLIXO_SENHA_EDICAO`.
  - Nos testes, use uma senha inventada (os testes do servidor usam `senha-do-teste-unitario`).
- Não mexer no código do programa Swing nesta tarefa. O Helplixo Web fica só na pasta `web/`.

---

## Parte 1: levar o Helplixo Web para o GitHub (no computador do Magno)

**Situação de hoje**
- O Helplixo Web está num repositório Git **só local**, sem remoto: `C:\Users\magnoC.dgi\Projetos\Helplixo-Web`, branch `main`, com todo o histórico do projeto (o último commit é o deste prompt).
- O repositório do GitHub **`https://github.com/magnobraccini/Helplixo`** é o do programa atual (Swing). A branch principal é `master`, e o clone local fica em `C:\Users\magnoC.dgi\Projetos\Helplixo`.
- O Git desta máquina usa `http.sslBackend=schannel` por causa da inspeção SSL da rede da prefeitura.

**Objetivo:** o Helplixo Web passa a morar em `web/` dentro do repositório Helplixo, **com o histórico dos commits preservado**, numa branch separada, sem tocar no código Swing.

**Passos**
1. No clone `C:\Users\magnoC.dgi\Projetos\Helplixo`, conferir com `git status` que não há alterações pendentes. Se houver, pare e me pergunte. Depois, atualizar com `git fetch origin`.
2. Criar a branch `helplixo-web` a partir de `origin/master`.
3. Trazer o projeto com o histórico:
   `git subtree add --prefix=web C:/Users/magnoC.dgi/Projetos/Helplixo-Web main`
   Se o `subtree` não estiver disponível, use a alternativa: fazer `fetch` do repositório local, mover os arquivos para `web/` e fazer `merge --allow-unrelated-histories`, mantendo o histórico.
4. Conferir:
   - a pasta `web/` tem `servidor/`, `PROXIMOS-PASSOS.md`, `PROMPT-FRONTEND.md` e o `.gitignore`;
   - nada fora de `web/` mudou (`git diff --stat origin/master` só mostra `web/`);
   - **nenhuma senha** está em nenhum commit novo (procure a senha de teste com `git log -S` e `git grep` em todo o histórico importado);
   - os testes passam: `mvn test` dentro de `web/servidor` (devem ser 73).
5. Ajustar o `web/PROXIMOS-PASSOS.md` para dizer que o projeto agora mora em `web/` no repositório Helplixo. Commitar.
6. **Pedir minha confirmação** e então enviar: `git push -u origin helplixo-web`. O login no GitHub, se aparecer, é feito por mim.
7. Sugerir abrir um pull request de `helplixo-web` para `master`, mas **não fazer merge** sem a minha ordem.
8. O repositório local `Helplixo-Web` fica como está, como cópia de segurança. Não apague.

**Depois da Parte 1:** a Parte 2 pode seguir aqui mesmo ou numa conversa na nuvem aberta a partir do repositório do GitHub, na branch `helplixo-web`. Na nuvem não existem os caminhos `C:\` e `G:\`, a memória local nem a cópia `helplixo-dados-teste`: trabalhe só com o que está em `web/`.

---

## Parte 2: desenvolver as telas (frontend)

### Pré-requisito
- Node.js LTS: confira com `node -v` e `npm -v`. No computador do Magno ele pode ainda não estar instalado; nesse caso me avise e pare. Na nuvem ele costuma estar disponível.
- Java 21+ e Maven para rodar o servidor: `mvn spring-boot:run` dentro de `web/servidor` (porta 8080).

### Onde estão as coisas (dentro de `web/`)
- `servidor/`: Spring Boot 3.5.0, Java 21, 73 testes (`mvn test`).
- `PROXIMOS-PASSOS.md`: o plano e o que já foi feito. **Leia primeiro.**
- **Maquete aprovada**, o visual e o comportamento de todas as telas: https://claude.ai/artifact/YKUfPne25G3Kt8vu67gsUd
- **Regras de negócio** (documento): https://claude.ai/artifact/5rud8uZ6ugMsvGSfwDCoaz

### O que fazer
1. Criar `web/telas/` com **React + TypeScript (Vite)**, falando com a API do servidor. Em desenvolvimento, use o proxy do Vite para `http://localhost:8080`. O `.gitignore` já ignora `telas/node_modules/` e `telas/dist/`.
2. **Visual estilo Notion**, como na maquete:
   - **Tema claro creme:** fundo `#FAF6EE`, lateral `#F3EEE3`, cartões `#FFFDF8`, tinta `#37352F`, texto secundário `#5F5E5B`, apagado `#787774`, linhas `#E7E0D1`, ação azul `#2383E2`.
   - **Tema escuro estilo Notion:** fundo `#191919`, lateral `#202020`, cartões `#202020`, tinta `#E3E2DF`.
   - As cores de todos os selos de status, nos dois temas, estão no código da maquete (blocos `:root{--c-...}` e `[data-tema="escuro"]{...}`). Leia a maquete com a ferramenta de artifacts e copie de lá.
   - **Configurações** (⚙️ na lateral): Aparência Claro / Escuro / Igual ao sistema (salvo no computador) e o interruptor do **Modo edição**, que pede a senha.
   - Fonte do sistema (Segoe UI no Windows), cantos pequenos (4 a 8 px), sem trocas bruscas de cor.
   - Menu lateral com **emojis**: 🏠 Início, 🎫 Chamados, 🖥️ Inventário, 📚 Processos, 📇 Contatos, 📅 Calendário, 🧰 Ferramentas.
   - Capa ilustrada no topo de cada aba (a do macaco Zenas no Início), com **as mesmas cores nos dois temas** e **sem o balão de fala**. O caminho, "Você: <perfil>" e o sino ficam sobre a capa, sem faixa branca.
3. Telas, nesta ordem, ligadas à API:
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
4. Para cada tela: testes (Vitest + Testing Library), commit e integração.
   - No computador do Magno: commit na branch `helplixo-web` e push só com a confirmação dele.
   - Na nuvem: commits na branch da sessão, entregues por pull request.

### A API do servidor (resumo)
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

### O que ainda falta no servidor (só se a tela precisar)
- Adicionar software à máquina.
- Movimentação junto do fechamento do chamado.
- Registrar chamado novo.
- Criar e editar processos com anexos. Limites: imagens até 10 MB, documentos até 25 MB, .zip até 100 MB, programas só dentro de .zip, 200 MB por processo.
- Processos parecidos (palavras em comum, mínimo 2).
- Login com permissões por perfil.
- Banco de dados.

---

Comece pela **Parte 1, passo 1**, me explicando em uma frase o que vai fazer.
