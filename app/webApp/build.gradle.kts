import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
}
kotlin {
    js { browser { commonWebpackConfig { devServer=(devServer ?: KotlinWebpackConfig.DevServer()).apply { port=8081 } } }; binaries.executable() }
    sourceSets.commonMain.dependencies {
        implementation(project(":app:shared"))
        implementation(compose.ui)
    }
}
