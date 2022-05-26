import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    repositories {
        maven("https://jitpack.io")
    }
    dependencies {
        classpath("com.github.kotlinqs.kotlinq:com.github.kotlinqs.gradle.plugin:0.1-SNAPSHOT")
    }
}

plugins {
    kotlin("jvm") version "1.6.20"
    `maven-publish`
}

apply(plugin="com.github.kotlinqs")

group = "com.github.kotlinqs"
version = "1.0-SNAPSHOT"

allprojects {
    repositories {
        maven("https://jitpack.io")
        //mavenLocal()
        mavenCentral()
    }
}

dependencies {
    implementation("org.jooq:jooq:3.16.6")
    implementation(kotlin("reflect"))

    testImplementation("com.h2database:h2:2.1.212")
    testImplementation(kotlin("test"))
}


tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

kotlin {
    sourceSets.main {
        kotlin.srcDir("build/generated/ksp/main/kotlin")
    }
    sourceSets.test {
        kotlin.srcDir("build/generated/ksp/test/kotlin")
    }
}

publishing {
    publications {
        create("maven_public", MavenPublication::class) {
            groupId = "com.github.kotlinqs"
            artifactId = "kotlinq-jooq"
            version = "0.1-SNAPSHOT"

            from(components.getByName("java"))
        }
    }
}

kotlinq {
    debug = true
    `package`("io.github.kotlinq.jooq.tableTest")
}