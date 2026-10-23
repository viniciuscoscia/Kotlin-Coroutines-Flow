param(
 [ValidateSet('Run','Start','Status','Stop')][string]$Action='Run',
 [ValidateSet('dev','optimized')][string]$Profile='dev',
 [string]$MacHost='viniciuscoscia@192.168.101.9',
 [string]$RemoteProject='/Users/viniciuscoscia/Workspace/Android/KotlinCoroutinesFlow',
 [ValidateRange(1024,65535)][int]$Port=8080
)
$ErrorActionPreference='Stop'
if($MacHost -notmatch '^[A-Za-z0-9_.@:-]+$' -or $RemoteProject -notmatch '^/[A-Za-z0-9_./+-]+$'){throw 'Use a plain SSH host and absolute remote project path'}
$sshArgs=@('-T','-o','BatchMode=yes','-o','StrictHostKeyChecking=yes','-o','ServerAliveInterval=30',$MacHost)
$command="/opt/homebrew/opt/python@3.14/bin/python3.14 $RemoteProject/tools/mac_backend.py $($Action.ToLowerInvariant()) --profile $Profile --port $Port"
try { & ssh @sshArgs $command; if($LASTEXITCODE -ne 0){throw "Mac backend $Action failed ($LASTEXITCODE)"} }
finally { if($Action -eq 'Run'){ & ssh @sshArgs "/opt/homebrew/opt/python@3.14/bin/python3.14 $RemoteProject/tools/mac_backend.py stop --profile $Profile --port $Port" } }
