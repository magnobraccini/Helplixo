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

## Compilar

```powershell
mvn -DskipTests package
```

## Fluxo para disponibilizar uma versão

1. Desenvolver e validar nesta cópia, usando `data-dev/`.
2. Revisar as alterações e compilar o artefato de distribuição.
3. Testar a atualização em uma cópia de homologação com dados fictícios e confirmar o retorno à versão anterior.
4. Publicar a versão aprovada no mecanismo de distribuição, mantendo aplicação e conteúdo operacional separados.

Nunca adicione processos, ramais, avisos, histórico, configurações locais ou backups ao Git. O `.gitignore` cobre os nomes e diretórios usados pela aplicação, mas confira `git status` antes de publicar.
