<#
.SYNOPSIS
    Compila o Helplixo e publica o JAR na pasta de rede, guardando a versão anterior.

.DESCRIPTION
    1. Confere o repositório (sem alterações locais pendentes) e atualiza com "git pull".
    2. Compila com Maven (roda os testes, a menos que -PularTestes).
    3. Mostra a versão publicada hoje e a nova, e pede confirmação.
    4. Copia o JAR atual da rede para "_versoes-anteriores\<data-hora>\".
    5. Substitui o JAR na rede e confere se a cópia ficou idêntica (hash SHA-256).

    Só o arquivo .jar é tocado. Pastas de dados (data, processos, ramais, config etc.)
    nunca são lidas, copiadas nem alteradas.

.EXAMPLE
    .\scripts\publicar-na-rede.ps1 -Simular
    Mostra tudo o que seria feito, sem alterar a pasta de rede.

.EXAMPLE
    .\scripts\publicar-na-rede.ps1
    Publica de verdade.

.EXAMPLE
    .\scripts\publicar-na-rede.ps1 -Restaurar
    Volta para a última versão guardada em "_versoes-anteriores".
#>
[CmdletBinding()]
param(
    [string]$Destino = 'G:\Suporte\Suporte\Helplixo',
    [string]$NomeJar,
    [switch]$Simular,
    [switch]$PularTestes,
    [switch]$PularGit,
    [switch]$Restaurar,
    [int]$Manter = 5
)

$ErrorActionPreference = 'Stop'
$Raiz = Split-Path -Parent $PSScriptRoot
$PastaBackups = Join-Path $Destino '_versoes-anteriores'
$Log = Join-Path $PastaBackups 'publicacoes.log'
Add-Type -AssemblyName System.IO.Compression.FileSystem

function Etapa($texto) { Write-Host ''; Write-Host "== $texto" -ForegroundColor Cyan }
function Ok($texto) { Write-Host "   $texto" -ForegroundColor Green }
function Aviso($texto) { Write-Host "   $texto" -ForegroundColor Yellow }
function Parar($texto) { Write-Host ''; Write-Host "PARADO: $texto" -ForegroundColor Red; exit 1 }

function Get-VersaoJar([string]$caminho) {
    try {
        $zip = [IO.Compression.ZipFile]::OpenRead($caminho)
        try {
            $entrada = $zip.Entries | Where-Object { $_.FullName -like 'META-INF/maven/*/helplixo/pom.properties' } | Select-Object -First 1
            if (-not $entrada) { return 'desconhecida' }
            $leitor = New-Object IO.StreamReader($entrada.Open())
            $texto = $leitor.ReadToEnd()
            $leitor.Close()
            if ($texto -match 'version=(.+)') { return $Matches[1].Trim() }
            return 'desconhecida'
        } finally { $zip.Dispose() }
    } catch { return 'desconhecida' }
}

function Test-EmUso([string]$caminho) {
    if (-not (Test-Path $caminho)) { return $false }
    try {
        $f = [IO.File]::Open($caminho, 'Open', 'ReadWrite', 'None')
        $f.Close()
        return $false
    } catch { return $true }
}

function Registrar([string]$texto) {
    if ($Simular) { return }
    $linha = '{0:yyyy-MM-dd HH:mm:ss}  {1}  {2}' -f (Get-Date), $env:USERNAME, $texto
    Add-Content -Path $Log -Value $linha -Encoding UTF8
}

function Substituir([string]$origem, [string]$alvo) {
    $temporario = "$alvo.novo"
    Copy-Item -LiteralPath $origem -Destination $temporario -Force
    while ($true) {
        if (-not (Test-EmUso $alvo)) {
            try {
                if (Test-Path -LiteralPath $alvo) { Remove-Item -LiteralPath $alvo -Force }
                Rename-Item -LiteralPath $temporario -NewName (Split-Path -Leaf $alvo)
                return
            } catch { }
        }
        Aviso "O Helplixo está aberto em alguma máquina e o arquivo está bloqueado:"
        Aviso "   $alvo"
        Aviso "Peça para fecharem o Helplixo (inclusive na bandeja) e tente de novo."
        $resposta = Read-Host '   Tentar de novo? (S/N)'
        if ($resposta -notmatch '^[sS]') {
            Remove-Item -LiteralPath $temporario -Force -ErrorAction SilentlyContinue
            Parar 'publicação cancelada. A versão da rede não foi alterada.'
        }
    }
}

# ------------------------------------------------------------------ pasta de rede

Etapa "Pasta de rede: $Destino"
if (-not (Test-Path -LiteralPath $Destino -PathType Container)) {
    Parar "pasta não encontrada. Confira se a unidade G: está conectada."
}

if (-not $NomeJar) {
    $jars = @(Get-ChildItem -LiteralPath $Destino -Filter '*.jar' -File)
    if ($jars.Count -eq 1) {
        $NomeJar = $jars[0].Name
    } elseif ($jars.Count -eq 0) {
        Parar ("nenhum .jar na pasta. Informe o nome com -NomeJar (ex.: -NomeJar helplixo.jar) " +
               "para criar o primeiro.")
    } else {
        Aviso 'Há mais de um .jar na pasta:'
        $jars | ForEach-Object { Aviso "   $($_.Name)" }
        Parar 'informe qual deve ser substituído com -NomeJar "<nome>.jar".'
    }
}
$JarRede = Join-Path $Destino $NomeJar
Ok "Arquivo que será atualizado: $NomeJar"

# ------------------------------------------------------------------ restaurar

if ($Restaurar) {
    Etapa 'Restaurar versão anterior'
    $ultimo = Get-ChildItem -LiteralPath $PastaBackups -Directory -ErrorAction SilentlyContinue |
              Sort-Object Name -Descending | Select-Object -First 1
    $copia = if ($ultimo) { Join-Path $ultimo.FullName $NomeJar }
    if (-not $copia -or -not (Test-Path -LiteralPath $copia)) { Parar 'nenhuma versão anterior guardada.' }
    Ok "Versão na rede hoje:   $(Get-VersaoJar $JarRede)"
    Ok "Versão a restaurar:    $(Get-VersaoJar $copia)  ($($ultimo.Name))"
    if ($Simular) { Aviso 'Simulação: nada foi alterado.'; exit 0 }
    if ((Read-Host '   Digite RESTAURAR para confirmar') -cne 'RESTAURAR') { Parar 'cancelado.' }
    Substituir $copia $JarRede
    Registrar "RESTAURADO $NomeJar a partir de $($ultimo.Name) (versão $(Get-VersaoJar $JarRede))"
    Ok 'Versão anterior restaurada.'
    exit 0
}

# ------------------------------------------------------------------ git

Set-Location $Raiz
if (-not $PularGit) {
    Etapa 'Atualizando o código (git)'
    if (-not (Get-Command git -ErrorAction SilentlyContinue)) { Parar 'git não encontrado no PATH.' }
    $pendentes = git status --porcelain
    if ($pendentes) {
        Aviso 'Há alterações locais que não estão no GitHub:'
        $pendentes | ForEach-Object { Aviso "   $_" }
        Parar 'faça commit/push delas (ou descarte) antes de publicar, para a rede não ficar diferente do GitHub.'
    }
    $branch = (git rev-parse --abbrev-ref HEAD).Trim()
    if ($branch -ne 'master') {
        Aviso "Você está no branch '$branch', não no master."
        if ((Read-Host '   Publicar mesmo assim? (S/N)') -notmatch '^[sS]') { Parar 'cancelado.' }
    }
    git pull --ff-only
    if ($LASTEXITCODE -ne 0) { Parar 'git pull falhou. Resolva e rode de novo.' }
    $commit = (git log -1 --format='%h %s').Trim()
    Ok "Código em: $commit"
} else {
    $commit = 'git ignorado (-PularGit)'
}

# ------------------------------------------------------------------ compilar

Etapa ('Compilando' + $(if ($PularTestes) { ' (sem testes)' } else { ' e rodando os testes' }))
if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) { Parar 'mvn não encontrado no PATH.' }
$argumentos = @('-q', 'clean', 'package')
if ($PularTestes) { $argumentos += '-DskipTests' }
& mvn @argumentos
if ($LASTEXITCODE -ne 0) { Parar 'a compilação ou os testes falharam. A rede não foi alterada.' }
$JarNovo = Join-Path $Raiz 'target\helplixo.jar'
if (-not (Test-Path -LiteralPath $JarNovo)) { Parar "JAR não gerado em $JarNovo." }
Ok 'Compilado.'

# ------------------------------------------------------------------ confirmar

Etapa 'Conferindo versões'
$versaoNova = Get-VersaoJar $JarNovo
$versaoRede = if (Test-Path -LiteralPath $JarRede) { Get-VersaoJar $JarRede } else { '(nenhuma)' }
Ok "Na rede hoje:  $versaoRede"
Ok "Nova:          $versaoNova  ($commit)"
try {
    if ($versaoRede -notin @('(nenhuma)', 'desconhecida') -and [version]$versaoRede -ge [version]$versaoNova) {
        Aviso 'ATENÇÃO: a versão da rede é igual ou mais nova que a compilada agora.'
        Aviso 'Ela pode ter alterações que não estão no GitHub e que seriam perdidas.'
    }
} catch { }

if ($Simular) {
    Etapa 'Simulação concluída'
    Aviso "Seria guardada uma cópia de $NomeJar em $PastaBackups\<data-hora>\"
    Aviso "e $NomeJar seria substituído pela versão $versaoNova. Nada foi alterado."
    exit 0
}
if ((Read-Host '   Digite PUBLICAR para substituir a versão da rede') -cne 'PUBLICAR') { Parar 'cancelado.' }

# ------------------------------------------------------------------ backup + publicar

Etapa 'Guardando a versão atual'
New-Item -ItemType Directory -Path $PastaBackups -Force | Out-Null
if (Test-Path -LiteralPath $JarRede) {
    $pastaCopia = Join-Path $PastaBackups ('{0:yyyyMMdd-HHmmss}' -f (Get-Date))
    New-Item -ItemType Directory -Path $pastaCopia -Force | Out-Null
    Copy-Item -LiteralPath $JarRede -Destination $pastaCopia
    Ok "Cópia em $pastaCopia"
    Get-ChildItem -LiteralPath $PastaBackups -Directory | Sort-Object Name -Descending |
        Select-Object -Skip $Manter | Remove-Item -Recurse -Force
}

Etapa 'Publicando'
Substituir $JarNovo $JarRede
$hashNovo = (Get-FileHash -LiteralPath $JarNovo -Algorithm SHA256).Hash
$hashRede = (Get-FileHash -LiteralPath $JarRede -Algorithm SHA256).Hash
if ($hashNovo -ne $hashRede) { Parar 'a cópia na rede não confere com o JAR compilado. Rode -Restaurar.' }
Registrar "PUBLICADO $NomeJar versão $versaoNova ($commit), anterior $versaoRede"
Ok "Publicado: $NomeJar versão $versaoNova"
Write-Host ''
Write-Host 'Para voltar à versão anterior:  .\scripts\publicar-na-rede.ps1 -Restaurar' -ForegroundColor Gray
