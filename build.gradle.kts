plugins {
    val kotlinVersion = "1.8.0"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion

    id("net.mamoe.mirai-console") version "2.16.0"
}

group = "com.tiedan"
version = "v1.0.1"

repositories {
    maven("https://maven.aliyun.com/repository/public")
    mavenCentral()
}

dependencies {}

mirai {
    jvmTarget = JavaVersion.VERSION_1_8
}
