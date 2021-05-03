import com.google.protobuf.gradle.generateProtoTasks
import com.google.protobuf.gradle.id
import com.google.protobuf.gradle.ofSourceSet
import com.google.protobuf.gradle.plugins
import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

object Version {
    const val LOGNET_GRPC_STARTER = "4.4.5"
    const val PROTOBUF = "3.15.6" // use compatible version with grpc-spring-boot-starter
    const val GRPC = "1.35.1" // use compatible version with grpc-spring-boot-starter
    const val GRPC_KOTLIN = "1.0.0"
    const val GRPC_REACTOR = "1.0.1"
}

plugins {
    id("org.springframework.boot") version "2.3.2.RELEASE"
    id("io.spring.dependency-management") version "1.0.9.RELEASE"
    id("com.github.ben-manes.versions") version "0.29.0"

    id("com.google.protobuf") version "0.8.15" //protobuf gradle plugin
    idea
    kotlin("jvm") version "1.4.31"
    kotlin("kapt") version "1.4.31"
    kotlin("plugin.spring") version "1.4.31"
    id("org.jetbrains.kotlin.plugin.jpa") version "1.4.31"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_11

allprojects {
    repositories {
        mavenCentral()
    }
}

subprojects {
    apply {
        plugin("idea")
        plugin("org.springframework.boot")
        plugin("io.spring.dependency-management")
        plugin("com.google.protobuf")
        plugin("kotlin")
        plugin("kotlin-kapt")
        plugin("org.jetbrains.kotlin.jvm")
        plugin("org.jetbrains.kotlin.plugin.spring")
        plugin("org.jetbrains.kotlin.plugin.jpa")
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs = listOf("-Xjsr305=strict")
            jvmTarget = "11"
        }
    }

    dependencies {
        implementation("org.springframework.boot:spring-boot-starter-webflux")
        implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
        implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
        implementation("org.jetbrains.kotlin:kotlin-reflect")
        implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
        implementation("org.springframework.boot:spring-boot-starter-data-jpa")
        implementation("org.springframework.boot:spring-boot-starter-actuator")
        implementation("org.springframework.boot:spring-boot-starter-logging")

        //grpc
        implementation("com.salesforce.servicelibs:reactor-grpc-stub:${Version.GRPC_REACTOR}")
        implementation("io.grpc:grpc-protobuf:${Version.GRPC}")
        implementation("io.grpc:grpc-services:${Version.GRPC}")
        implementation("io.grpc:grpc-kotlin-stub:${Version.GRPC_KOTLIN}")
        implementation("io.github.lognet:grpc-spring-boot-starter:${Version.LOGNET_GRPC_STARTER}") //Spring Boot starter module for gRPC framework
        implementation("com.h2database:h2")
        protobuf(files("$rootDir/proto")) //proto 파일들이 들어있는 디렉토리를 지정
        //for test
        testImplementation("org.springframework.boot:spring-boot-starter-test") {
            exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
        }
        testImplementation("io.projectreactor:reactor-test")
        testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")
    }

    protobuf {
        protoc {
            artifact = "com.google.protobuf:protoc:${Version.PROTOBUF}"
        }
        plugins {
            id("grpc") {
                artifact = "io.grpc:protoc-gen-grpc-java:${Version.GRPC}"
            }
            id("reactor") {
                artifact = "com.salesforce.servicelibs:reactor-grpc:${Version.GRPC_REACTOR}"
            }
            id("grpckt") {
                artifact = "io.grpc:protoc-gen-grpc-kotlin:${Version.GRPC_KOTLIN}:jdk7@jar"
            }
        }
        generateProtoTasks {
            ofSourceSet("main").forEach {
                it.plugins {
                    id("grpc")
                    id("reactor")
                    id("grpckt")
                }
            }
        }
    }
}
