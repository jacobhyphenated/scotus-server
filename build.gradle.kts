import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  java
  id("org.springframework.boot") version "2.2.4.RELEASE"
  id("io.spring.dependency-management") version "1.0.9.RELEASE"
  id("org.asciidoctor.convert") version "1.5.9.2"
  kotlin("jvm") version "1.3.61"
  kotlin("plugin.spring") version "1.3.61"
  kotlin("plugin.jpa") version "1.3.61"
}

group = "com.hyphenated"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_11

val developmentOnly: Configuration by configurations.creating
configurations {
  runtimeClasspath {
    extendsFrom(developmentOnly)
  }
}

repositories {
  mavenCentral()
}

val snippetsDir = file("build/generated-snippets")

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-actuator")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-web")
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
  implementation("org.jetbrains.kotlin:kotlin-reflect")
  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
  implementation("org.apache.commons:commons-lang3")

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
}

tasks.asciidoctor {
  inputs.dir(snippetsDir)
  sourceDir(file("src/docs"))
  outputDir(file("build/generated-docs"))
  dependsOn(tasks.test)
}

tasks.bootJar {
  dependsOn(tasks.asciidoctor)
  from ("${tasks.asciidoctor.get().outputDir.path}/html5") {
    into("static/docs")
  }
}
