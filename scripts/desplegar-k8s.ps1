#Requires -Version 7.0
<#
.SYNOPSIS
    Despliega en Kubernetes (Docker Desktop) las imágenes de GHCR de un commit.

.DESCRIPTION
    Actualiza los seis Deployments del namespace restaurante a las imágenes
    ghcr.io/<dueño>/<servicio>:<sha> y espera a que cada rollout termine.

    Antes de tocar nada comprueba el contexto de kubectl, el namespace, los
    Secrets, los ConfigMaps y los Deployments, y espera a que MySQL y Keycloak
    estén listos. Si un servicio ya tiene la imagen del SHA, solo verifica su
    rollout.

    Si algo falla muestra un diagnóstico (describe, Pods, logs y eventos
    Warning) y termina con código 1. No hay reversión automática.

    El script no aplica manifiestos, no crea Secrets ni ConfigMaps y no lee ni
    imprime sus valores: los manifiestos se aplican una sola vez a mano.

.PARAMETER ImageOwner
    Dueño de las imágenes en GHCR, en minúsculas. Por defecto, $env:IMAGE_OWNER.

.PARAMETER ImageSha
    SHA completo (40 caracteres) del commit. Por defecto, $env:IMAGE_SHA.

.EXAMPLE
    $env:IMAGE_OWNER = 'rosa-sdev'
    $env:IMAGE_SHA = (git rev-parse HEAD)
    ./scripts/desplegar-k8s.ps1
#>
[Diagnostics.CodeAnalysis.SuppressMessageAttribute('PSAvoidUsingWriteHost', '',
    Justification = 'El script escribe en el log del runner; no devuelve objetos.')]
[CmdletBinding(SupportsShouldProcess)]
param(
    [string] $ImageOwner = $env:IMAGE_OWNER,
    [string] $ImageSha = $env:IMAGE_SHA,
    [string] $Namespace = 'restaurante',
    [string] $Contexto = 'docker-desktop',
    [string] $TimeoutInfra = '300s',
    [string] $TimeoutServicio = '600s'
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
# Los códigos de salida de kubectl se revisan a mano con $LASTEXITCODE.
$PSNativeCommandUseErrorActionPreference = $false

# Orden de despliegue: usuarios primero (emite el token) y el gateway al final.
$servicios = @(
    'usuarios-service'
    'carta-service'
    'clientes-service'
    'salon-service'
    'pagos-service'
    'api-gateway'
)
$infraestructura = @('mysql', 'keycloak')
$secrets = @('restaurante-secretos', 'restaurante-claves')
$configMaps = @('mysql-inicializacion', 'keycloak-realm')

function Exit-Despliegue {
    param([Parameter(Mandatory)] [string] $Mensaje)
    Write-Host "::error::$Mensaje"
    exit 1
}

function Test-Recurso {
    param(
        [Parameter(Mandatory)] [string] $Tipo,
        [Parameter(Mandatory)] [string] $Nombre,
        [switch] $SinNamespace
    )
    $argumentos = @('get', $Tipo, $Nombre, '--context', $Contexto)
    if (-not $SinNamespace) { $argumentos += @('--namespace', $Namespace) }
    & kubectl @argumentos *> $null
    return ($LASTEXITCODE -eq 0)
}

function Show-Diagnostico {
    param([Parameter(Mandatory)] [string] $Deployment)
    $base = @('--context', $Contexto, '--namespace', $Namespace)

    Write-Host "::group::Diagnóstico de $Deployment"
    Write-Host "--- kubectl describe deployment/$Deployment"
    & kubectl describe "deployment/$Deployment" @base | Out-Host

    Write-Host "--- Pods de $Deployment"
    & kubectl get pods -l "app=$Deployment" -o wide @base | Out-Host

    $pods = (& kubectl get pods -l "app=$Deployment" -o 'jsonpath={.items[*].metadata.name}' @base) -split '\s+' |
        Where-Object { $_ }
    foreach ($pod in $pods) {
        Write-Host "--- Logs actuales de $pod (últimas 100 líneas)"
        & kubectl logs $pod --tail=100 @base 2>&1 | Out-Host

        Write-Host "--- Logs del contenedor anterior de $pod"
        $previos = & kubectl logs $pod --previous --tail=100 @base 2>&1
        if ($LASTEXITCODE -eq 0) { $previos | Out-Host } else { Write-Host '(sin contenedor anterior)' }
    }

    Write-Host "--- Eventos Warning del namespace $Namespace"
    & kubectl get events --field-selector type=Warning --sort-by=.lastTimestamp @base | Out-Host
    Write-Host '::endgroup::'
}

function Wait-Rollout {
    param(
        [Parameter(Mandatory)] [string] $Deployment,
        [Parameter(Mandatory)] [string] $Timeout
    )
    Write-Host "Esperando el rollout de $Deployment (máximo $Timeout)..."
    & kubectl rollout status "deployment/$Deployment" "--timeout=$Timeout" `
        --context $Contexto --namespace $Namespace | Out-Host
    if ($LASTEXITCODE -ne 0) {
        Show-Diagnostico -Deployment $Deployment
        Exit-Despliegue "El rollout de $Deployment no terminó en $Timeout."
    }
}

function Update-Servicio {
    [CmdletBinding(SupportsShouldProcess)]
    param(
        [Parameter(Mandatory)] [string] $Servicio,
        [Parameter(Mandatory)] [string] $Timeout
    )

    $destino = "ghcr.io/$ImageOwner/${Servicio}:$ImageSha"
    # El contenedor se llama igual que el Deployment.
    $actual = & kubectl get "deployment/$Servicio" --context $Contexto --namespace $Namespace `
        -o "jsonpath={.spec.template.spec.containers[?(@.name=='$Servicio')].image}"
    if ($LASTEXITCODE -ne 0 -or -not $actual) {
        Exit-Despliegue "No se pudo leer la imagen del contenedor $Servicio en deployment/$Servicio."
    }

    if ($actual -eq $destino) {
        Write-Host "$Servicio ya usa $destino; solo se verifica el rollout."
    }
    elseif ($PSCmdlet.ShouldProcess("deployment/$Servicio", "Cambiar la imagen de $actual a $destino")) {
        Write-Host "$Servicio`: $actual -> $destino"
        & kubectl set image "deployment/$Servicio" "$Servicio=$destino" `
            --context $Contexto --namespace $Namespace | Out-Host
        if ($LASTEXITCODE -ne 0) {
            Show-Diagnostico -Deployment $Servicio
            Exit-Despliegue "No se pudo cambiar la imagen de deployment/$Servicio."
        }
    }
    else {
        return
    }

    Wait-Rollout -Deployment $Servicio -Timeout $Timeout
}

# ---------- 1. Parámetros ----------
if ([string]::IsNullOrWhiteSpace($ImageOwner)) {
    Exit-Despliegue 'Falta IMAGE_OWNER (dueño de las imágenes en GHCR).'
}
if ($ImageOwner -cnotmatch '^[a-z0-9]([a-z0-9-]*[a-z0-9])?$') {
    Exit-Despliegue "IMAGE_OWNER '$ImageOwner' no es válido: debe ir en minúsculas, con letras, números o guiones."
}
if ($ImageSha -cnotmatch '^[0-9a-f]{40}$') {
    Exit-Despliegue "IMAGE_SHA '$ImageSha' no es válido: debe ser el SHA completo de 40 caracteres hexadecimales."
}
Write-Host "Imágenes: ghcr.io/$ImageOwner/<servicio>:$ImageSha"

# ---------- 2. Contexto y recursos previos ----------
$contextoActual = & kubectl config current-context 2>$null
if ($LASTEXITCODE -ne 0 -or $contextoActual -ne $Contexto) {
    Exit-Despliegue "El contexto de kubectl es '$contextoActual' y debe ser '$Contexto'."
}

if (-not (Test-Recurso -Tipo namespace -Nombre $Namespace -SinNamespace)) {
    Exit-Despliegue "No existe el namespace $Namespace. Aplica antes k8s/00-namespace.yaml."
}

$faltantes = @()
foreach ($nombre in $secrets) {
    if (-not (Test-Recurso -Tipo secret -Nombre $nombre)) { $faltantes += "Secret $nombre" }
}
foreach ($nombre in $configMaps) {
    if (-not (Test-Recurso -Tipo configmap -Nombre $nombre)) { $faltantes += "ConfigMap $nombre" }
}
foreach ($nombre in $infraestructura + $servicios) {
    if (-not (Test-Recurso -Tipo deployment -Nombre $nombre)) { $faltantes += "Deployment $nombre" }
}
if ($faltantes.Count -gt 0) {
    Exit-Despliegue "Faltan recursos en el namespace ${Namespace}: $($faltantes -join ', '). Créalos como indica el README (sección 2.5)."
}
Write-Host 'Contexto, namespace, Secrets, ConfigMaps y Deployments verificados.'

# ---------- 3. MySQL y Keycloak ----------
foreach ($deployment in $infraestructura) {
    Wait-Rollout -Deployment $deployment -Timeout $TimeoutInfra
}

# ---------- 4. Servicios ----------
foreach ($servicio in $servicios) {
    Update-Servicio -Servicio $servicio -Timeout $TimeoutServicio
}

Write-Host "Despliegue terminado: los seis servicios usan las imágenes de $ImageSha."
