plugins {
    kotlin("jvm") version "2.0.0"
    idea
}

group = "com.aspectgaming"
version = ""

repositories {
    mavenCentral()
}

val vertxVersion = "4.5.9"

dependencies {
    implementation(kotlin("stdlib"))
    // vertx-stack-depchain 是 Vert.x 提供的一个 BOM（物料清单），它包含了 Vert.x 所有的依赖库的版本信息
    implementation(platform("io.vertx:vertx-stack-depchain:$vertxVersion"))
    implementation("io.vertx:vertx-core")
    implementation("io.vertx:vertx-web")
    implementation("io.vertx:vertx-lang-kotlin")
    implementation("io.vertx:vertx-lang-kotlin-coroutines")
    implementation("io.vertx:vertx-pg-client")
    implementation("io.vertx:vertx-sql-client")
    implementation("io.vertx:vertx-redis-client")
    // auto
    implementation("io.vertx:vertx-auth-jwt")

    implementation("com.ongres.scram:client:2.1")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.12.3")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.12.3")
    implementation("org.slf4j:slf4j-api:1.7.30")
    implementation("ch.qos.logback:logback-classic:1.2.3")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.0")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}