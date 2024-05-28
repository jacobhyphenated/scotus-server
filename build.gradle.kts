import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  java
  idea
  jacoco
  id("org.springframework.boot") version "3.1.5"
  id("io.spring.dependency-management") version "1.1.3"
  id("org.asciidoctor.jvm.convert") version "3.3.2"
  kotlin("jvm") version "1.8.22"
  kotlin("plugin.spring") version "1.8.22"
  kotlin("plugin.jpa") version "1.8.22"
}

group = "com.hyphenated"
version = "0.14.1"
java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
  mavenCentral()
}

idea {
  module {
    isDownloadJavadoc = true
    isDownloadSources = true
  }
}

val snippetsDir = file("build/generated-snippets")
val coroutinesVersion = "1.7.1"

// define "asciidoctor" as a custom dependency configuration
// The latest asciidoctor converter plugin no longer defines this in a global scope
configurations {
  create("asciidoctor")
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-actuator")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-web")
  implementation("org.springframework.boot:spring-boot-starter-validation")
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
  implementation("org.jetbrains.kotlin:kotlin-reflect")
  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
  implementation("org.apache.commons:commons-lang3")

  implementation("org.opensearch.client:spring-data-opensearch-starter:1.2.1")

  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${coroutinesVersion}")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:${coroutinesVersion}")

  developmentOnly("org.springframework.boot:spring-boot-devtools")

  runtimeOnly("com.h2database:h2")
  runtimeOnly("org.postgresql:postgresql")

  testImplementation("org.springframework.boot:spring-boot-starter-test") {
    exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
  }
  testImplementation("org.springframework.restdocs:spring-restdocs-mockmvc")
  testImplementation("org.springframework.security:spring-security-test")
  testImplementation("com.nhaarman.mockitokotlin2:mockito-kotlin:2.2.0")

  "asciidoctor"("org.springframework.restdocs:spring-restdocs-asciidoctor")
}

tasks.withType<KotlinCompile> {
  kotlinOptions {
    freeCompilerArgs = listOf("-Xjsr305=strict")
    jvmTarget = "17"
  }
}

// Removes plain jar from build files
tasks.jar {
  enabled = false
}

tasks.test {
  useJUnitPlatform()
  outputs.dir(snippetsDir)
  finalizedBy(tasks.jacocoTestReport)
  testLogging {
    events("passed", "skipped", "failed", "standardError")
  }
}

tasks.jacocoTestReport {
  dependsOn(tasks.test)
}

tasks.jacocoTestCoverageVerification {
  violationRules {
    rule {
      includes = listOf("com.hyphenated.scotus.*Controller*")
      limit {
        minimum = "0.9".toBigDecimal()
      }
    }
  }
}

tasks.asciidoctor {
  configurations("asciidoctor") // invoke custom dependency scope for this task
  inputs.dir(snippetsDir)
  sourceDir(file("src/docs"))
  setOutputDir(file("build/generated-docs"))
  dependsOn(tasks.test)
}

tasks.bootJar {
  dependsOn(tasks.jacocoTestCoverageVerification)
  dependsOn(tasks.asciidoctor)
  from (tasks.asciidoctor.get().outputDir.path) {
    into("static/docs")
  }
}
