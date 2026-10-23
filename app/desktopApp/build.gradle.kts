plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
}
dependencies {
    implementation(project(":app:shared"))
    implementation(compose.desktop.currentOs)
    implementation(libs.coroutines.swing)
}
compose.desktop {
    application {
        mainClass = "lab.MainKt"
        // IDE runtimes may omit jpackage; allow an explicit full JDK for packaging.
        providers.gradleProperty("lab.desktop.javaHome").orNull?.let { javaHome = it }
        jvmArgs += "--enable-native-access=ALL-UNNAMED"
        nativeDistributions { modules("java.net.http") }
    }
}
