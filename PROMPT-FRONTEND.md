# Prompt para continuar o Helplixo Web (frontend)

Copie tudo abaixo da linha e cole numa conversa nova do Claude Code, aberta na pasta `C:\Users\magnoC.dgi\Projetos\Helplixo-Web`.

---

Vamos continuar o **Helplixo Web**, a versão web do sistema de suporte da TI da prefeitura. O servidor está pronto; agora é a vez das **telas (frontend)**.

## Como trabalhar comigo

- Fale em **português**, em modo ensino: um passo de cada vez, explicações curtas, uma analogia e uma pergunta de revisão no fim de cada etapa.
- No fim de cada entrega, faça **perguntas numeradas** para eu responder e, ao fechar uma etapa grande, um **resumo do que foi feito com lugares para estudar** cada assunto.
- **Siga o plano sem pedir licença para continuar.** Peça permissão só antes de mudar algo já decidido (regras, visual, escopo) ou de mexer no Helplixo atual (Swing) e na pasta da rede.
- **Commit e integração na `main` sem perguntar**, ao fim de cada passo com os testes passando. Antes de cada commit, confira que nenhuma senha está nos arquivos. Termine as mensagens de commit com `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`. Escreva a mensagem num arquivo UTF-8 **sem BOM** (o pipe do PowerShell adiciona BOM). O repositório não tem remoto; push só com a minha confirmação.
- Autor dos commits deste repositório: Magno Braccini <braccinimagno@gmail.com> (já configurado só neste repositório).

## Regras que nunca podem ser quebradas

- Nunca alterar, mover ou apagar pastas de dados (data, processos, ramais, config, backups). Na pasta de rede `G:\Suporte\Suporte\Helplixo`, só o arquivo `.jar` pode ser substituído, e só depois da minha confirmação.
- **Nenhuma senha vai para arquivos do repositório**: nem a senha de edição, nem a senha de teste. A senha de edição vem da variável de ambiente `HELPLIXO_SENHA_EDICAO`. Nos testes, use uma senha inventada (os testes do servidor usam `senha-do-teste-unitario`).

## Onde estão as coisas

- **Projeto:** `C:\Users\magnoC.dgi\Projetos\Helplixo-Web` (branch `main`, 8 commits; o último é `269ab9f`). Leia primeiro o `PROXIMOS-PASSOS.md`.
- **Servidor:** pasta `servidor/` (Spring Boot 3.5.0, Java 21; a máquina tem Java 27 e Maven 3.10). Testes: `mvn test` dentro de `servidor/` (73 passando). Para rodar: `mvn spring-boot:run` (porta 8080).
- **Maquete aprovada** (o visual e o comportamento das telas): https://claude.ai/artifact/YKUfPne25G3Kt8vu67gsUd
- **Regras de negócio** (documento): https://claude.ai/artifact/5rud8uZ6ugMsvGSfwDCoaz
- **Helplixo atual (Swing):** `C:\Users\magnoC.dgi\Projetos\Helplixo`. Cópia de dados de teste: `C:\Users\magnoC.dgi\Projetos\helplixo-dados-teste` (só leitura).

## Pré-requisito

O **Node.js LTS** precisa estar instalado (eu instalo pelo nodejs.org). Antes de tudo, confira com `node -v` e `npm -v` num terminal novo. Se não estiver, me avise e pare.

## O que fazer (frontend)

1. Criar `telas/` com **React + TypeScript (Vite)**, falando com a API do servidor (em desenvolvimento, use o proxy do Vite para `http://localhost:8080`).
2. **Visual estilo Notion**, como na maquete:
   - Tema **claro creme**: fundo `#FAF6EE`, lateral `#F3EEE3`, cartões `#FFFDF8`, tinta `#37352F`, texto secundário `#5F5E5B`, apagado `#787774`, linhas `#E7E0D1`, ação azul `#2383E2`.
   - Tema **escuro estilo Notion**: fundo `#191919`, lateral `#202020`, cartões `#202020`, tinta `#E3E2DF`.
   - **Configurações** (⚙️ na lateral): Aparência Claro / Escuro / Igual ao sistema (salvo no computador) e o interruptor do **Modo edição** (pede a senha).
   - Fonte do sistema (Segoe UI no Windows), cantos pequenos (4 a 8 px), sem trocas bruscas de cor.
   - Menu lateral com **emojis**: 🏠 Início, 🎫 Chamados, 🖥️ Inventário, 📚 Processos, 📇 Contatos, 📅 Calendário, 🧰 Ferramentas.
   - Capa ilustrada no topo de cada aba (a do Zenas no Início), com as **mesmas cores nos dois temas**. Sem o balão de fala do macaco. Caminho, "Você: <perfil>" e o sino ficam sobre a capa, sem faixa branca.
   - Cores das variáveis e de todos os selos de status: copie da maquete (veja o bloco `:root{--c-...}` e `[data-tema="escuro"]{...}` no código dela).
3. Telas, nesta ordem, ligadas à API:
   1. **Chamados**: lista (filtros de status, período, secretaria, técnico e busca; linha clicável mostra a descrição; botões "Ver detalhes" e "Atualizar"; Nº Semin clicável; marca ↺ de outros chamados da máquina nos últimos 7 dias; coluna Min. com cores: aberto verde até 2 h, âmbar até 3 h, vermelho acima, em andamento azul). Ficha com abas **Dados → Resultado → Histórico da máquina**. O Resultado é **um formulário só** com Status (Aberto / Em andamento / Fechado); a anotação só é obrigatória para fechar. Mantém o chamado e a aba abertos ao trocar de seção.
   2. **Início**: saudação, 4 números clicáveis (Abertos, Em andamento, Esperando há mais tempo só dos abertos, Fechados hoje com a média de 30 dias), "Precisa de atenção", "Com você" ou "Sem técnico", Hoje e Amanhã do calendário, "Continue de onde parou".
   3. **Inventário**: busca primeiro (pelo menos 2 letras), ficha com 4 abas (Identificação, Compra, Softwares com o Office, Histórico), editar, movimentar e dar baixa.
   4. **Processos**: árvore à esquerda e o processo **ao lado**, com índice "Neste processo" que marca a seção ao rolar.
   5. **Contatos**: Internos e Externos com filtros e cadastro.
   6. **Calendário**: visões Dia, Semana, Mês e Agenda; agendas por tipo; lembretes no sino.
4. Para cada tela: testes (Vitest + Testing Library), commit e integração na `main`.

## A API do servidor (resumo)

- Erros sempre como `{"erros": ["..."]}`. 422 = regra quebrada, 403 = falta modo edição, 401 = senha errada, 429 = espere, 503 = modo edição não configurado, 404 = não existe.
- **Modo edição:** `POST /api/edicao/entrar` `{"senha"}` devolve `{chave, validaAte}` (8 horas); `POST /api/edicao/sair`; `GET /api/edicao`. Ações de edição mandam o cabeçalho `X-Chave-Edicao`.
- **Quem faz** (até existir o login): cabeçalhos `X-Usuario` (nome) e `X-Cargo` (Triagem, Técnico ou Chefe de TI).
- **Chamados:** `GET /api/chamados?de=&ate=&status=` (sem datas = 30 dias), `GET /api/chamados/{n}`, `POST /api/chamados/{n}/resultado` `{status, anotacao, quando, tecnico}`, que devolve `{chamado, avisos}`.
- **Processos:** `GET /api/processos/arvore`, `GET /api/processos/{id}` (com o `caminho`), `GET /api/processos?q=`.
- **Inventário:** `GET /api/inventario/resumo`, `GET /api/inventario?q=`, `GET /api/inventario/{nSemin}` (ficha com `office`, `garantia` e `historico`), `POST /api/inventario`, `PUT /api/inventario/{nSemin}` (devolve `mudancas`), `POST /api/inventario/{nSemin}/movimentacoes`, `POST` e `DELETE /api/inventario/{nSemin}/baixa`.
- **Contatos:** `GET /api/contatos/internos?secretaria=&andar=&ordem=&q=`, `GET /api/contatos/externos?categoria=&situacao=&q=`, `POST` nos dois.
- **Calendário:** `GET /api/calendario?de=&ate=&tipos=&q=`, `POST /api/calendario` (Aviso: qualquer cargo; o resto: modo edição), `DELETE /api/calendario/{id}`, `GET /api/calendario/lembretes`.
- **Saúde:** `GET /api/saude`.

## O que ainda falta no servidor (só se a tela precisar)

Adicionar software à máquina, movimentação junto do fechamento do chamado, registrar chamado novo, criar e editar processos com anexos (imagens até 10 MB, documentos até 25 MB, .zip até 100 MB, programas só dentro de .zip, 200 MB por processo), processos parecidos (palavras em comum, mínimo 2), login com permissões por perfil e banco de dados.

Comece conferindo o Node, lendo o `PROXIMOS-PASSOS.md` e me dizendo o primeiro passo das telas.
