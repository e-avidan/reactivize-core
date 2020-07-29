plugins {
    kotlin("jvm") version "1.4.0-rc"
    maven
}

group = "il.ac.technion.cs"
version = "1.0-SNAPSHOT"

repositories {
    maven("https://dl.bintray.com/kotlin/kotlin-eap")
    mavenCentral()
    jcenter()
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven("https://soot-build.cs.upb.de/nexus/repository/soot-release/")
    maven("https://jitpack.io") {
        metadataSources {
            gradleMetadata()
            mavenPom()
        }
    }
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))
    implementation("io.reactivex.rxjava3", "rxjava", "3.0.4")
    implementation("io.reactivex.rxjava3", "rxkotlin", "3.0.0")
    implementation("com.github.kotlinx.ast", "common", "master-SNAPSHOT")
    implementation("com.github.kotlinx.ast", "parser-antlr-kotlin", "master-SNAPSHOT")
    implementation("com.github.kotlinx.ast", "grammar-kotlin-parser-antlr-kotlin", "master-SNAPSHOT")
    implementation("org.soot-oss", "soot", "4.1.0-SNAPSHOT")
    implementation("com.github.CROSSINGTUD", "SPDS", "3.0")

    testImplementation("org.assertj", "assertj-core", "3.16.1")
    testImplementation("org.junit.jupiter", "junit-jupiter-api", "5.6.2")
    testImplementation("org.junit.jupiter", "junit-jupiter-engine", "5.6.2")
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    test {
        useJUnitPlatform()
    }
}