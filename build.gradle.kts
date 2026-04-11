import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
	alias(libs.plugins.spring.boot)
	alias(libs.plugins.spring.dependency.management)
	id("checkstyle")
	application
	alias(libs.plugins.sonarqube)
	jacoco
}

group = "hexlet.code"
version = "0.0.1-SNAPSHOT"

application {
	mainClass = "hexlet.code.AppApplication"
}


java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	// Spring Boot starters
	implementation(libs.spring.boot.starter)
	implementation(libs.spring.boot.starter.web)
	implementation(libs.spring.boot.starter.data.jpa)
	implementation(libs.spring.boot.starter.validation)
	implementation(libs.spring.boot.starter.security)
	implementation(libs.spring.boot.starter.oauth2.resource.server)

	// OpenAPI / Swagger
	implementation(libs.springdoc.openapi)
	implementation(libs.jackson.databind.nullable)

	// Object mapping
	implementation(libs.mapstruct)

	// Lombok
	compileOnly(libs.lombok)

	// Annotaions
	annotationProcessor(libs.lombok)
	annotationProcessor(libs.lombok.mapstruct.binding)
	annotationProcessor(libs.spring.boot.configuration.processor)
	annotationProcessor(libs.mapstruct.processor)


	// Database
	runtimeOnly(libs.h2)
	runtimeOnly(libs.postgresql)


	// Testing
	testRuntimeOnly(libs.junit.platform.launcher)
	testCompileOnly(libs.lombok)
	testAnnotationProcessor(libs.lombok)

	testImplementation(libs.spring.boot.starter.test) {
		exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
	}
	testImplementation(libs.spring.webmvc)
	testImplementation(libs.spring.boot.test.autoconfigure)
	testImplementation(libs.spring.security.test)
	testImplementation(libs.json.unit.assertj)
	testImplementation(libs.instancio)
	testImplementation(libs.datafaker)
}

configurations.all {
	resolutionStrategy {
		dependencySubstitution {
			substitute(module("hexlet.code:app")).using(project(":"))
		}
	}
}

tasks.check {
	dependsOn(tasks.checkstyleMain, tasks.checkstyleTest)
}

tasks.build {
	dependsOn(tasks.check)
}

tasks.test {
	useJUnitPlatform()
	testLogging {
		exceptionFormat = TestExceptionFormat.FULL
		events = mutableSetOf(TestLogEvent.FAILED, TestLogEvent.PASSED, TestLogEvent.SKIPPED)
		// showStackTraces = true
		// showCauses = true
		showStandardStreams = true
	}
	finalizedBy(tasks.jacocoTestReport) // Для генерации отчета о покрытии
}

tasks.jacocoTestReport {
	dependsOn(tasks.test)
	reports {
		xml.required.set(true) // SonarQube требует XML отчет
		html.required.set(true)
	}
}



sonar {
	properties {
		property("sonar.projectKey", "valentin-osadchii_java-project-99")
		property("sonar.organization", "valentin-osadchii")
	}
}
