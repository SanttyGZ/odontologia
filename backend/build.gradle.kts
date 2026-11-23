import org.gradle.api.artifacts.dsl.LockMode

plugins {
    java
    id("org.springframework.boot") version "4.2.0"
}

group = "dev.odontologia"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
        vendor = JvmVendorSpec.ADOPTIUM
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES))
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.springframework.boot:spring-boot-starter-security")
    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("org.flywaydb:flyway-database-postgresql")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

val integrationTest = sourceSets.create("integrationTest") {
    compileClasspath += sourceSets.main.get().output
    runtimeClasspath += sourceSets.main.get().output
}

configurations[integrationTest.implementationConfigurationName]
    .extendsFrom(configurations.testImplementation.get())
configurations[integrationTest.runtimeOnlyConfigurationName]
    .extendsFrom(configurations.testRuntimeOnly.get())

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

tasks.register<Test>("integrationTest") {
    description = "Comprueba migraciones, arranque y concurrencia contra bases efímeras en PostgreSQL."
    group = "verification"
    testClassesDirs = integrationTest.output.classesDirs
    classpath = integrationTest.runtimeClasspath
    shouldRunAfter(tasks.test)
    // La disponibilidad de PostgreSQL y las variables locales cambian fuera del árbol de fuentes.
    outputs.upToDateWhen { false }
    outputs.doNotCacheIf("La conexión real debe comprobarse en cada ejecución") { true }
}

dependencyLocking {
    lockAllConfigurations()
    lockMode = LockMode.STRICT
}

tasks.register("resolveAndLockAll") {
    description = "Resuelve todas las configuraciones; usar --write-locks solo al actualizar dependencias."
    doLast {
        configurations.filter { it.isCanBeResolved }.forEach { it.resolve() }
    }
}
