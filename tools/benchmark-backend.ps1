param([string]$Java='java',[Parameter(Mandatory=$true)][string]$Jar,[ValidateSet('jvm','jdk-aot','spring-aot','native')][string]$Variant='jvm',[string]$NativeExecutable,[string]$AotCache,[int]$Port=8080,[string]$Output='backend-benchmark.csv')
$ErrorActionPreference='Stop'
# Run on the target Mac with a PowerShell installation or port this process algorithm.
# Preparation/build costs must be measured separately. This script never fabricates results.
try { Invoke-RestMethod "http://127.0.0.1:$Port/ready" -TimeoutSec 1 | Out-Null; throw "Port $Port already has a ready backend; stop it before measuring" } catch { if($_.Exception.Message -like 'Port *') { throw } }
$rows=@()
foreach($iteration in 1..10) {
 $backendArgs=@('-Dspring.profiles.active=optimized',"-Dserver.port=$Port")
 if($Variant -eq 'spring-aot') { $backendArgs+='-Dspring.aot.enabled=true' }
 if($Variant -eq 'jdk-aot') { if(!$AotCache) { throw 'Supply a prepared JDK25 AOT cache' }; $backendArgs+="-XX:AOTCache=$AotCache" }
 $backendArgs+=@('-jar',$Jar)
 $exe=$Java; if($Variant -eq 'native') { if(!$NativeExecutable) { throw 'Supply a separately built native image' }; $exe=$NativeExecutable; $backendArgs=@('--spring.profiles.active=optimized',"--server.port=$Port") }
 $timer=[Diagnostics.Stopwatch]::StartNew()
 $start=@{FilePath=$exe;ArgumentList=$backendArgs;PassThru=$true}
 if($IsWindows -or $env:OS -eq 'Windows_NT') { $start.WindowStyle='Hidden' }
 $process=Start-Process @start
 try {
  $ready=$false
  while($timer.Elapsed.TotalSeconds -lt 60 -and !$process.HasExited) {
   try { $response=Invoke-RestMethod "http://127.0.0.1:$Port/ready" -TimeoutSec 1; if($response.ready) { $ready=$true; break } } catch {}
   Start-Sleep -Milliseconds 20
  }
  if(!$ready) { throw "Iteration $iteration did not become ready" }
  $rows += [pscustomobject]@{variant=$Variant;iteration=$iteration;readyMs=$timer.Elapsed.TotalMilliseconds;workingSetBytes=$process.WorkingSet64;pid=$process.Id;os=[Environment]::OSVersion.VersionString;java=$Java}
 } finally { if(!$process.HasExited) { Stop-Process -Id $process.Id }; $process.WaitForExit() }
}
$rows | Export-Csv -NoTypeInformation $Output
