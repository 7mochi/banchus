plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    kotlin("plugin.jpa")
    kotlin("kapt")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation(project(":banchus-packet"))

    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("tools.jackson.module:jackson-module-kotlin")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("me.paulschwarz:springboot4-dotenv:5.1.0")
    implementation("org.flywaydb:flyway-mysql")
    implementation("software.amazon.awssdk:s3:2.41.24")
    implementation("org.mapstruct:mapstruct:1.6.3")
    implementation("io.github.7mochi:rosu-pp-jar:0.2.0")
    implementation("io.github.7mochi:osu-native-jar:0.0.7")
    implementation("com.michael-bull.kotlin-result:kotlin-result:2.1.0")
    implementation("org.bouncycastle:bcprov-jdk18on:1.83")

    kapt("org.mapstruct:mapstruct-processor:1.6.3")

    runtimeOnly("org.mariadb.jdbc:mariadb-java-client")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
    }
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

springBoot {
    mainClass.set("pe.nanamochi.banchus.BanchusApplicationKt")
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    enabled = true
}

tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
    workingDir = rootProject.projectDir
}

tasks.withType<Test> {
    useJUnitPlatform()
}
