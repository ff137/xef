import org.jetbrains.dokka.gradle.DokkaTask

repositories { mavenCentral() }

plugins {
    base
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotest.multiplatform)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.spotless)
    alias(libs.plugins.dokka)
    alias(libs.plugins.arrow.gradle.publish)
    alias(libs.plugins.semver.gradle)
    alias(libs.plugins.detekt)
}

dependencies { detektPlugins(project(":detekt-rules")) }

detekt {
    toolVersion = "1.23.1"
    source = files("src/commonMain/kotlin", "src/jvmMain/kotlin")
    config.setFrom("../config/detekt/detekt.yml")
    autoCorrect = true
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
    toolchain { languageVersion = JavaLanguageVersion.of(11) }
}

kotlin {
    jvm()
    js(IR) {
        browser()
        nodejs()
    }

    linuxX64()
    macosX64()
    macosArm64()
    mingwX64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(projects.xefCore)
                api(projects.xefOpenai)
            }
        }
    }
}

spotless {
    kotlin {
        target("**/*.kt")
        ktfmt().googleStyle()
    }
}

tasks {
    withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
        dependsOn(":detekt-rules:assemble")
        autoCorrect = true
    }
    named("detektJvmMain") {
        dependsOn(":detekt-rules:assemble")
        getByName("build").dependsOn(this)
    }
    named("detekt") {
        dependsOn(":detekt-rules:assemble")
        getByName("build").dependsOn(this)
    }
    withType<Test>().configureEach {
        maxParallelForks = Runtime.getRuntime().availableProcessors()
        useJUnitPlatform()
        testLogging {
            setExceptionFormat("full")
            setEvents(listOf("passed", "skipped", "failed", "standardOut", "standardError"))
        }
    }

    withType<DokkaTask>().configureEach {
        kotlin.sourceSets.forEach { kotlinSourceSet ->
            dokkaSourceSets.named(kotlinSourceSet.name) {
                perPackageOption {
                    matchingRegex.set(".*\\.internal.*")
                    suppress.set(true)
                }
                skipDeprecated.set(true)
                reportUndocumented.set(false)
                val baseUrl: String = checkNotNull(project.properties["pom.smc.url"]?.toString())

                kotlinSourceSet.kotlin.srcDirs.filter { it.exists() }.forEach { srcDir ->
                    sourceLink {
                        localDirectory.set(srcDir)
                        remoteUrl.set(
                                uri("$baseUrl/blob/main/${srcDir.relativeTo(rootProject.rootDir)}")
                                        .toURL()
                        )
                        remoteLineSuffix.set("#L")
                    }
                }
            }
        }
    }
}

tasks.withType<AbstractPublishToMaven> { dependsOn(tasks.withType<Sign>()) }
