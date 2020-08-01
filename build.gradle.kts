import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import com.google.protobuf.gradle.generateProtoTasks
import com.google.protobuf.gradle.id
import com.google.protobuf.gradle.ofSourceSet
import com.google.protobuf.gradle.plugins
import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc

object Ver {
	const val grpcSpringBootStarter = "3.5.6"
	const val protobuf = "3.12.3" // use compatible version with grpc-spring-boot-starter
	const val grpc = "1.31.0" // use compatible version with grpc-spring-boot-starter
	const val reactorGrpc = "1.0.1"
}

plugins {
	id("org.springframework.boot") version "2.3.2.RELEASE"
	id("io.spring.dependency-management") version "1.0.9.RELEASE"
	id ("com.github.ben-manes.versions") version "0.29.0"

	id("com.google.protobuf") version "0.8.12" //protobuf gradle plugin
	idea //IDE 플러그인 넣어줘야 generated 소스들이 인지됨.
	kotlin("jvm") version "1.3.72"
	kotlin("plugin.spring") version "1.3.72"
	id ("org.jetbrains.kotlin.plugin.jpa") version "1.3.72"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_1_8

repositories {
	mavenCentral()
}


dependencies {
	implementation("org.springframework.boot:spring-boot-starter-webflux")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	//grpc
	implementation("com.salesforce.servicelibs:reactor-grpc-stub:${Ver.reactorGrpc}")
	implementation("io.grpc:grpc-protobuf:${Ver.grpc}")
	implementation("io.grpc:grpc-services:${Ver.grpc}")
	implementation("io.github.lognet:grpc-spring-boot-starter:${Ver.grpcSpringBootStarter}") //Spring Boot starter module for gRPC framework

	runtimeOnly("io.grpc:grpc-netty") //없으면 grpc 포트가 열리지 않음, default 6565
	runtimeOnly("com.h2database:h2")
	protobuf(files("$projectDir/proto")) //proto 파일들이 들어있는 디렉토리를 지정
	//for test
	testImplementation("org.springframework.boot:spring-boot-starter-test") {
		exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
	}
	testImplementation("io.projectreactor:reactor-test")
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

protobuf {
	protoc {
		artifact = "com.google.protobuf:protoc:${Ver.protobuf}"
	}
	plugins {
		id("grpc") {
			artifact = "io.grpc:protoc-gen-grpc-java:${Ver.grpc}"
		}
		id("reactor") {
			artifact = "com.salesforce.servicelibs:reactor-grpc:${Ver.reactorGrpc}"
		}
	}
	generateProtoTasks {
		ofSourceSet("main").forEach {
			it.plugins {
				// Apply the "grpc" and "reactor" plugins whose specs are defined above, without options.
				id("grpc")
				id("reactor")
			}
		}
	}
}