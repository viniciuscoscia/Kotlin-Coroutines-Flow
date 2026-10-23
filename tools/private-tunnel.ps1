param(
 [string]$MacHost='viniciuscoscia@192.168.101.9',
 [ValidateSet('Backend','Dsh')][string]$Service='Backend',
 [ValidateRange(1024,65535)][int]$LocalPort=8080,
 [ValidateRange(1024,65535)][int]$RemotePort=8080
)
$ErrorActionPreference='Stop'
if($MacHost -notmatch '^[A-Za-z0-9_.@:-]+$'){throw 'Use a plain SSH host or alias'}
# Foreground SSH is owned by this run. IDE Stop closes this tunnel only.
$forward=if($Service -eq 'Dsh'){'-R'}else{'-L'}
if($Service -eq 'Dsh' -and -not $PSBoundParameters.ContainsKey('LocalPort')){$LocalPort=39241}
if($Service -eq 'Dsh' -and -not $PSBoundParameters.ContainsKey('RemotePort')){$RemotePort=39241}
$mapping=if($Service -eq 'Dsh'){"127.0.0.1:${RemotePort}:127.0.0.1:${LocalPort}"}else{"127.0.0.1:${LocalPort}:127.0.0.1:${RemotePort}"}
& ssh -N -T -o BatchMode=yes -o StrictHostKeyChecking=yes -o ExitOnForwardFailure=yes -o ServerAliveInterval=30 $forward $mapping $MacHost
if($LASTEXITCODE -ne 0){throw "Private $Service tunnel exited ($LASTEXITCODE)"}
