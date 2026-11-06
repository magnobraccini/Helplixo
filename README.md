# Helplixo — cópia de desenvolvimento

Esta cópia é isolada da instalação usada pelos usuários. Ao executar a partir desta pasta, os dados são gravados em `data-dev/`.

## Requisitos

- JDK 26
- Maven 3.9 ou superior

## Executar

Antes da primeira execução, cada desenvolvedor deve inserir suas próprias senhas nas variáveis de ambiente `HELPLIXO_EDIT_PASSWORD` e `HELPLIXO_MASTER_PASSWORD`. Não coloque credenciais no código ou no Git. Em PowerShell, defina-as na sessão antes de executar:

```powershell
$env:HELPLIXO_EDIT_PASSWORD = "sua-senha-de-edicao"
$env:HELPLIXO_MASTER_PASSWORD = "sua-senha-mestre"
```

No Windows, use `run-helplixo.bat` ou execute `mvn exec:java` nesta pasta. Os dados locais ficam em `data-dev/` e não devem ser commitados. Sem senha configurada, o modo de edição e a recuperação por senha mestre ficam bloqueados.

Para testar a cópia compilada mais recente no Windows, execute `Abrir-Helplixo-teste.bat`. Ela abre o JAR em `dist/Helplixo-teste-0.5.0.jar` e mantém os dados de teste separados em `data-dev/`.

A tela de edição usa a mesma disposição da leitura: título e resumo no topo, os passos ao centro (sempre abertos, ocupando a altura disponível) e a coluna "Detalhes" à direita. Blocos e "Notas e links" ficam em abas ao lado dos passos.

O editor de passos tem uma barra única que reflete a formatação onde está o cursor: estilos de parágrafo (texto, títulos 1–3, citação e bloco de código), negrito, itálico, sublinhado, tachado, código inline para comandos, cor do texto, realce, listas que convertem os parágrafos selecionados, recuo, alinhamento, links e divisores. Parágrafos simples continuam sem numeração automática; a numeração aparece quando uma lista numerada é escolhida. Atalhos principais: Ctrl+B/I/U, Ctrl+Shift+C (código), Ctrl+Shift+7/8 (listas), Ctrl+Alt+0–3 (estilos), Ctrl+K (link), Ctrl+Shift+V (colar sem formatação), Ctrl+[ e Ctrl+] (recuo), Ctrl+S (salvar) e Esc (sair da edição). Desfazer agrupa a digitação por trecho e cada campo tem o próprio histórico.

Na primeira execução da versão 0.5.0, passos do formato estruturado antigo são convertidos para HTML editável. Antes da conversão, cada arquivo original é copiado para `data/backups/migrations/steps-rich-text-v1/`.

Recentes e favoritos são separados automaticamente pelo perfil do Windows em uso, sem tela de login. Processos, ramais e os demais conteúdos permanecem compartilhados no diretório de dados.

## Compilar

```powershell
mvn -DskipTests package
```

## Publicar na pasta de rede

`Publicar-na-rede.bat` (ou `scripts\publicar-na-rede.ps1`) publica a versão do GitHub em `G:\Suporte\Suporte\Helplixo`:

1. Recusa publicar se houver alterações locais fora do GitHub e roda `git pull`.
2. Compila e roda os testes (`mvn clean package`).
3. Mostra a versão da rede e a nova e pede que se digite `PUBLICAR`.
4. Guarda o JAR atual em `_versoes-anteriores\<data-hora>\` (mantém as 5 últimas) e registra em `publicacoes.log`.
5. Substitui só o `.jar` e confere o hash. Pastas de dados não são tocadas.

Rode primeiro com `-Simular` para conferir sem alterar nada. Se o Helplixo estiver aberto em alguma máquina, o JAR fica bloqueado e o script pede para tentar de novo. Para voltar à versão anterior: `Publicar-na-rede.bat -Restaurar`.

## Fluxo para disponibilizar uma versão

1. Desenvolver e validar nesta cópia, usando `data-dev/`.
2. Revisar as alterações e compilar o artefato de distribuição.
3. Testar a atualização em uma cópia de homologação com dados fictícios e confirmar o retorno à versão anterior.
4. Publicar a versão aprovada no mecanismo de distribuição, mantendo aplicação e conteúdo operacional separados.

Nunca adicione processos, ramais, avisos, histórico, configurações locais ou backups ao Git. O `.gitignore` cobre os nomes e diretórios usados pela aplicação, mas confira `git status` antes de publicar.
