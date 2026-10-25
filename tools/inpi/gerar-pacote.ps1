<#
Gera o pacote do registro no INPI: o código-fonte de uma versão marcada (tag) num único
arquivo .zip e o resumo SHA-512 desse arquivo, que vai no formulário e-Software.

Uso, na pasta do repositório:
    .\tools\inpi\gerar-pacote.ps1 -Tag v1.0.0

O pacote fica fora do repositório (por padrão em $HOME\CaLju-INPI\<tag>). Guarde-o sem
alterações e com cópias de segurança: é ele que prova o que foi registrado.
#>
param(
    [Parameter(Mandatory = $true)][string]$Tag,
    [string]$Destino = (Join-Path $HOME "CaLju-INPI")
)
$ErrorActionPreference = "Stop"

$commit = git rev-parse --verify --quiet "$Tag^{commit}"
if (-not $commit) {
    throw "A tag '$Tag' não existe. Crie com: git tag -a $Tag -m `"Versão registrada no INPI`""
}

$pasta = Join-Path $Destino $Tag
New-Item -ItemType Directory -Force $pasta | Out-Null
$zip = Join-Path $pasta "calju-$Tag-codigo-fonte.zip"
if (Test-Path $zip) {
    throw "Já existe $zip. Um pacote já gerado não deve ser sobrescrito."
}

# Arquivos oficiais de terceiros (planilha da TACO, tabelas do IBGE) ficam de fora: não são obra do autor.
git archive --format=zip --prefix="calju-$Tag/" -o $zip $Tag -- . ':(exclude)data/fontes'
if ($LASTEXITCODE -ne 0) {
    throw "O git archive falhou."
}

$hash = (Get-FileHash -Algorithm SHA512 -Path $zip).Hash.ToLowerInvariant()
Set-Content -Path (Join-Path $pasta "resumo_hash.txt") -Value $hash -Encoding ascii -NoNewline
$dataCommit = git log -1 --format=%cs $commit
@"
Pacote do registro no INPI - CaLju
Tag: $Tag
Commit: $commit
Data do commit: $dataCommit
Arquivo: $(Split-Path $zip -Leaf)
Tamanho: $((Get-Item $zip).Length) bytes
Algoritmo: SHA-512
Resumo hash: $hash
"@ | Set-Content -Path (Join-Path $pasta "LEIA-ME.txt") -Encoding utf8

Write-Host "Pacote gerado em $pasta"
Write-Host "Resumo SHA-512 (copie para o formulário e-Software):"
Write-Host $hash
