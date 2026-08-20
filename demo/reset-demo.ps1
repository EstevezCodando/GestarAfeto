<#
.SYNOPSIS
    Recoloca o ambiente no estado inicial do roteiro de demonstracao.

.DESCRIPTION
    Remove todas as gestantes cadastradas (o que tambem limpa os alertas no
    microsservico, via GestanteRemovidaEvent) e recria duas gestantes com perfis
    contrastantes, sem checklist gerado:

      - Maria Silva   semana 26  -> varios procedimentos atrasados
      - Joana Pereira semana  8  -> acompanhamento em dia

    Use entre ensaios para repetir a demonstracao do zero.

.EXAMPLE
    .\demo\reset-demo.ps1
#>

$ErrorActionPreference = 'Stop'

$principal = $env:GESTARAFETO_URL
if (-not $principal) { $principal = 'http://localhost:8080' }
$alertas = $env:GESTARAFETO_ALERTAS_URL
if (-not $alertas) { $alertas = 'http://localhost:8081' }

function Test-Servico($nome, $url) {
    try {
        $r = Invoke-RestMethod -Uri "$url/actuator/health" -TimeoutSec 5
        Write-Host ("  {0,-12} {1}" -f $nome, $r.status) -ForegroundColor Green
        return $true
    } catch {
        Write-Host ("  {0,-12} FORA DO AR ({1})" -f $nome, $url) -ForegroundColor Red
        return $false
    }
}

Write-Host "`nVerificando servicos" -ForegroundColor Cyan
$okPrincipal = Test-Servico 'principal' $principal
$okAlertas = Test-Servico 'alertas' $alertas

if (-not $okPrincipal) {
    Write-Host "`nO servico principal precisa estar no ar. Abortando.`n" -ForegroundColor Red
    exit 1
}
if (-not $okAlertas) {
    Write-Host "`nAviso: o microsservico esta fora do ar. As gestantes serao recriadas," -ForegroundColor Yellow
    Write-Host "mas os alertas antigos so serao limpos quando ele voltar.`n" -ForegroundColor Yellow
}

Write-Host "`nRemovendo gestantes existentes" -ForegroundColor Cyan
$gestantes = Invoke-RestMethod -Uri "$principal/api/gestantes"
if ($gestantes.Count -eq 0) {
    Write-Host "  nenhuma gestante cadastrada"
}
foreach ($g in $gestantes) {
    Invoke-RestMethod -Uri "$principal/api/gestantes/$($g.id)" -Method Delete | Out-Null
    Write-Host "  removida: $($g.nome) (id $($g.id))"
}

Start-Sleep -Seconds 2

# A semana gestacional e derivada da DUM: semanas completas + 1.
$dumMaria = (Get-Date).AddDays(-25 * 7).ToString('yyyy-MM-dd')   # semana 26
$dumJoana = (Get-Date).AddDays(-7 * 7).ToString('yyyy-MM-dd')    # semana 8

Write-Host "`nRecriando gestantes de demonstracao" -ForegroundColor Cyan

$corpoMaria = @{
    nome                  = 'Maria Silva'
    email                 = 'maria.silva@example.com'
    telefone              = '21988887777'
    dataUltimaMenstruacao = $dumMaria
    observacoes           = 'Iniciou o pre-natal tardiamente'
} | ConvertTo-Json

$corpoJoana = @{
    nome                  = 'Joana Pereira'
    email                 = 'joana.pereira@example.com'
    telefone              = '21977776666'
    dataUltimaMenstruacao = $dumJoana
    observacoes           = 'Primeira gestacao'
} | ConvertTo-Json

$maria = Invoke-RestMethod -Uri "$principal/api/gestantes" -Method Post `
    -ContentType 'application/json; charset=utf-8' -Body $corpoMaria
Write-Host "  Maria Silva    id=$($maria.id)  DUM=$dumMaria  (semana 26)"

$joana = Invoke-RestMethod -Uri "$principal/api/gestantes" -Method Post `
    -ContentType 'application/json; charset=utf-8' -Body $corpoJoana
Write-Host "  Joana Pereira  id=$($joana.id)  DUM=$dumJoana  (semana 8)"

if ($okAlertas) {
    $total = (Invoke-RestMethod -Uri "$alertas/api/alertas?page=0&size=1").totalElements
    Write-Host "`nAlertas no microsservico: $total (esperado: 0)" -ForegroundColor Cyan
}

Write-Host "`nPronto. Nenhum checklist foi gerado - esse e o primeiro passo do roteiro." -ForegroundColor Green
Write-Host "Front-end: http://localhost:5173/`n"
