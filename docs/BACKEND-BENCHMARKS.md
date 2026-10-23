# Reproducible backend performance preparation

The optimized profile is a candidate, not a benchmark-selected winner. Run this on the actual MacBook before selecting a deployment variant. Windows build/readiness evidence does not establish Mac startup performance.

Requirements: JDK25; PowerShell7 on either platform; Gradle dependencies; GraalVM25 plus the target compiler only for Native Image. No login service, CRaC default, public binding or inference service is configured.

1. Set JAVA_HOME/JAVA to the exact JDK25 executable. Stop any backend already bound to the measurement port.
2. From the repository: `pwsh -File tools/prepare-backend-benchmarks.ps1 -Java /absolute/jdk25/bin/java`. Add `-IncludeNative` only when GraalVM25 and the target compiler are available. This records preparation/build times separately in build/benchmark/preparation.csv, preserving separate conventional and Spring-AOT artifacts. Re-run JDK training in a fresh explicitly chosen output directory rather than deleting caches indiscriminately.
3. Run `pwsh -File tools/benchmark-backend.ps1 -Java /absolute/jdk25/bin/java -Jar /absolute/repo/build/benchmark/jvm/server.jar -Variant jvm -Output jvm.csv`.
4. Repeat with the spring-aot/server.jar and `-Variant spring-aot`.
5. For JDK cache use the **extracted** build/benchmark/jdk-aot/server.jar and `-Variant jdk-aot -AotCache /absolute/repo/build/benchmark/jdk-aot/app.aot`. A cache against the nested executable bootJar is not accepted as a useful JDK AOT result. The JDK and application must match the training run.
6. Native Image: run against the platform-produced server/build/native/nativeCompile/server executable with `-Variant native -NativeExecutable /absolute/path/to/server` (Jar argument is retained for the common interface).
7. Each harness invocation starts/stops ten new processes. It records measured readiness time, resident memory, process ID, OS and Java path. Use the same machine/load/power state and report all ten samples plus median/p95 and total preparation cost. A warm filesystem cache is not a cold-machine result. Readiness polling has approximately 20ms granularity. No result files are committed by the harness.

The preparation itself is not startup acceptance. Verify multipart copy and WebSocket behavior for each candidate before choosing one. Compare conventional JVM, JDK cache, Spring AOT and Native Image. Keep optimized execution on demand from the IDE. Mac private forwarding and ten-start comparisons remain unexecuted here.

Official references consulted for exact Spring4.1.2 task/plugin and JDK25 cache behavior:

- [Spring Boot Gradle AOT](https://docs.spring.io/spring-boot/gradle-plugin/aot.html)
- [Spring Boot JDK AOT cache](https://docs.spring.io/spring-boot/reference/packaging/aot-cache.html)
- [GraalVM Native Build Tools](https://graalvm.github.io/native-build-tools/latest/gradle-plugin)


## Python measurement, no PowerShell requirement

Python3.10+ uses only its standard library. Supply a JSON document with `base_url` (loopback), `preparation` metadata, and `variants` entries containing unique `name`, exact executable `argv` array, optional `cwd` and `env`. Each supplied process must use the same isolated port and a dedicated sandbox. Example:

```json
{
  "base_url": "http://127.0.0.1:18080",
  "preparation": {"machine": "fill actual Mac model", "jdk": "fill actual java -version", "build_ms": "fill measured build time", "training_ms": "fill measured cache preparation"},
  "variants": [
    {"name": "jvm", "argv": ["/absolute/jdk25/bin/java", "-jar", "/absolute/benchmark/jvm/server.jar", "--server.port=18080", "--spring.profiles.active=optimized", "--lab.sandbox=/absolute/benchmark/sandbox"]},
    {"name": "spring-aot", "argv": ["/absolute/jdk25/bin/java", "-Dspring.aot.enabled=true", "-jar", "/absolute/benchmark/spring-aot/server.jar", "--server.port=18080", "--spring.profiles.active=optimized", "--lab.sandbox=/absolute/benchmark/sandbox"]}
  ]
}
```

Add JDK-cache variant with the extracted jar and `-XX:AOTCache=/absolute/app.aot`; native variant argv points to its real Mac-built executable. Run `python3 tools/benchmark_backend.py variants.json --output build/benchmark/mac-measured`. CSV and JSON record every sample, process PID, exact argv, OS/architecture, median and nearest-rank p95. Every sample independently uploads and reads a real copy and performs a standards-compliant WebSocket handshake/probe/ack; heartbeat receipt alone is not presented as roundtrip proof. Mac RSS uses `ps -o rss= -p PID` (KiB converted to bytes). Processes terminate in finally blocks.

For preparation without PowerShell: time `./gradlew :server:bootJar`; preserve conventional jar, time `./gradlew :server:bootJar -PlabAot=true`; preserve Spring-AOT jar. Extract conventional jar with `java -Djarmode=tools -jar server.jar extract --destination application`; inside application, time `java -XX:AOTCacheOutput=app.aot -Dspring.context.exit=onRefresh -jar server.jar --spring.profiles.active=optimized`. Native preparation is `./gradlew :server:nativeCompile -PlabNative=true` using a real GraalVM25/toolchain. Record each elapsed build/training time independently in config.preparation, then measure starts with the Python runner.


Validated Windows preparation may be rerun with `-OutputDirectory build/another-fresh-preparation`. Quote dotted JVM options when invoking Java from PowerShell, e.g. `'-Dspring.context.exit=onRefresh'`. A failed Java main-class launch can still emit a cache; require successful exit and actual application training before using it. For JDK-cache benchmark set variant.cwd to the extracted directory and use relative `-jar server.jar`, matching the training classpath. Windows caches cannot establish Mac behavior and should be regenerated on the Mac/JDK in use.
