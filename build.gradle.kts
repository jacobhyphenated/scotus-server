import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  java
  idea
  jacoco
  id("org.springframework.boot") version "2.3.3.RELEASE"
  id("io.spring.dependency-management") version "1.0.10.RELEASE"
  id("org.asciidoctor.convert") version "1.5.9.2"
  kotlin("jvm") version "1.4.0"
  kotlin("plugin.spring") version "1.4.0"
  kotlin("plugin.jpa") version "1.4.0"
}

group = "com.hyphenated"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_11

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
var coroutinesVersion = "1.3.9"

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-actuator")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-web")
  implementation("org.springframework.boot:spring-boot-starter-validation")
  implementation("org.springframework.boot:spring-boot-starter-data-elasticsearch")
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
  implementation("org.jetbrains.kotlin:kotlin-reflect")
  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
  implementation("org.apache.commons:commons-lang3")

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

  asciidoctor("org.springframework.restdocs:spring-restdocs-asciidoctor")
}

tasks.withType<Test> {
  useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
  kotlinOptions {
    freeCompilerArgs = listOf("-Xjsr305=strict")
    jvmTarget = "1.8"
  }
}

tasks.test {
  outputs.dir(snippetsDir)
  finalizedBy(tasks.jacocoTestReport)
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
  inputs.dir(snippetsDir)
  sourceDir(file("src/docs"))
  outputDir(file("build/generated-docs"))
  dependsOn(tasks.test)
}

tasks.bootJar {
  dependsOn(tasks.jacocoTestCoverageVerification)
  dependsOn(tasks.asciidoctor)
  from ("${tasks.asciidoctor.get().outputDir.path}/html5") {
    into("static/docs")
  }
}
