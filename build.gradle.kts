import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
	java
	id("org.springframework.boot") version libs.versions.spring.boot.get()
	alias(libs.plugins.spring.dependency.management)
	id("checkstyle")
	id("application")
	id("org.sonarqube") version libs.versions.sonar.plugin.get()
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
	implementation("org.springframework.boot:spring-boot-starter")
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")

	// OpenAPI / Swagger
	implementation(libs.springdoc)
	implementation(libs.jackson.databind.nullable)

	// Object mapping
	implementation(libs.mapstruct)
	annotationProcessor(libs.mapstruct.processor)

	// Lombok
	compileOnly(libs.lombok)
	annotationProcessor(libs.lombok)

	// Databases
	runtimeOnly("com.h2database:h2")
	runtimeOnly("org.postgresql:postgresql")

	// Configuration metadata
	annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

	// Testing
	testImplementation("org.springframework.boot:spring-boot-starter-test") {
		exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
	}
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
	testImplementation("org.springframework:spring-webmvc")
	testImplementation("org.springframework.boot:spring-boot-test-autoconfigure")
	testImplementation("org.springframework.security:spring-security-test")
	testImplementation(libs.json.unit.assertj)
	testImplementation(libs.instancio)
	testImplementation(libs.datafaker)
	testCompileOnly(libs.lombok)
	testAnnotationProcessor(libs.lombok)
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
