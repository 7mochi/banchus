plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    kotlin("plugin.jpa")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

dependencies {
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("com.fasterxml.jackson.core:jackson-annotations")
    implementation("software.amazon.awssdk:s3:2.41.24")
    implementation("org.bouncycastle:bcprov-jdk18on:1.83")
    implementation("com.michael-bull.kotlin-result:kotlin-result:2.1.0")
}
