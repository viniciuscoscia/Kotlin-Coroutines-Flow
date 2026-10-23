param([string]$Java='java',[switch]$IncludeNative,[string]$OutputDirectory='build/benchmark')
$ErrorActionPreference='Stop'
$root=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$gradle=Join-Path $root 'gradlew'; if($IsWindows -or $env:OS -eq 'Windows_NT') { $gradle+='.bat' }
$destination=if([IO.Path]::IsPathRooted($OutputDirectory)) { $OutputDirectory } else { Join-Path $root $OutputDirectory }
New-Item -ItemType Directory -Path $destination -Force | Out-Null
$rows=@()
Push-Location $root
try {
 foreach($variant in @('jvm','spring-aot')) {
  $timer=[Diagnostics.Stopwatch]::StartNew()
  if($variant -eq 'spring-aot') { & $gradle :server:bootJar -PlabAot=true --console=plain } else { & $gradle :server:bootJar --console=plain }
  if($LASTEXITCODE -ne 0) { throw "$variant artifact preparation failed" }
  $folder=Join-Path $destination $variant; New-Item -ItemType Directory -Path $folder -Force | Out-Null
  Copy-Item -LiteralPath 'server/build/libs/server.jar' -Destination (Join-Path $folder 'server.jar')
  $rows += [pscustomobject]@{variant=$variant;preparationMs=$timer.Elapsed.TotalMilliseconds;os=[Environment]::OSVersion.VersionString}
 }
 $timer=[Diagnostics.Stopwatch]::StartNew()
 $cacheFolder=Join-Path $destination 'jdk-aot'
 if(Test-Path $cacheFolder) { throw 'JDK AOT output already exists; choose a fresh preparation directory explicitly' }
 & $Java '-Djarmode=tools' '-jar' (Join-Path $destination 'jvm/server.jar') extract --destination $cacheFolder
 if($LASTEXITCODE -ne 0) { throw 'JDK AOT extraction failed' }
 Push-Location $cacheFolder
 try {
  & $Java '-XX:AOTCacheOutput=app.aot' '-Dspring.context.exit=onRefresh' '-jar' 'server.jar' '--spring.profiles.active=optimized' "--lab.sandbox=$destination/training-sandbox"
  if($LASTEXITCODE -ne 0 -or !(Test-Path app.aot)) { throw 'JDK25 AOT cache training did not produce app.aot' }
 } finally { Pop-Location }
 $rows += [pscustomobject]@{variant='jdk-aot';preparationMs=$timer.Elapsed.TotalMilliseconds;os=[Environment]::OSVersion.VersionString}
 if($IncludeNative) {
  $timer=[Diagnostics.Stopwatch]::StartNew()
  & $gradle :server:nativeCompile -PlabNative=true --console=plain
  if($LASTEXITCODE -ne 0) { throw 'Native Image build failed; require GraalVM25 and target-platform compiler' }
  $rows += [pscustomobject]@{variant='native';preparationMs=$timer.Elapsed.TotalMilliseconds;os=[Environment]::OSVersion.VersionString}
 }
} finally { $rows | Export-Csv -NoTypeInformation (Join-Path $destination 'preparation.csv'); Pop-Location }
