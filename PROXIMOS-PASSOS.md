# Helplixo Web: próximos passos

Pausa em 25/11/2026. Este arquivo diz onde paramos e o que fazer ao voltar.

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
7. **Senha de edição** conferida no servidor, vinda da variável de ambiente `HELPLIXO_SENHA_EDICAO` (nunca no Git).
8. Inventário, contatos e calendário, nessa ordem.

## Passo a passo das telas (depois do Node)

1. Criar `telas/` com React + TypeScript (Vite).
2. Tema claro creme e escuro estilo Notion, com as mesmas variáveis de cor da maquete.
3. Tela de Chamados ligada à API, depois as outras abas na ordem do servidor.

## Regras que continuam valendo

- Nunca alterar, mover ou apagar as pastas de dados (data, processos, ramais, config, backups).
- Na pasta de rede, só o `.jar` pode ser trocado, e só depois da sua confirmação.
- Senhas nunca vão para o Git.
