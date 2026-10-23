plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.spring.boot)
}
kotlin { jvmToolchain(25) }
dependencies {
    implementation(platform("org.springframework.boot:spring-boot-dependencies:${libs.versions.springBoot.get()}"))
    implementation(project(":core"))
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation(libs.coroutines.core)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:${libs.versions.coroutines.get()}")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
tasks.test { useJUnitPlatform() }

// AOT preparation is explicit; normal development keeps its fast build path.
if (providers.gradleProperty("labAot").orNull == "true") {
    apply(plugin = "org.springframework.boot.aot")
    tasks.withType<org.springframework.boot.gradle.tasks.aot.ProcessAot>().configureEach {
        args("--spring.profiles.active=optimized", "--lab.sandbox=${layout.buildDirectory.dir("aot-sandbox").get().asFile.absolutePath}")
    }
}

if (providers.gradleProperty("labNative").orNull == "true") {
    apply(plugin = "org.graalvm.buildtools.native")
}
