import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
	java
	id("org.springframework.boot") version "3.3.9"
	id("io.spring.dependency-management") version "1.1.7"
	id("checkstyle")
	id("application")
	id("org.sonarqube") version "7.2.3.7755"
	jacoco

}

group = "hexlet.code"
version = "0.0.1-SNAPSHOT"

val lombokVersion = "1.18.34"

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
	implementation("org.springframework.boot:spring-boot-starter")
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")

	implementation("org.openapitools:jackson-databind-nullable:0.2.8")

	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0")


	testImplementation("org.springframework.boot:spring-boot-starter-test") {
		exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
	}

	testRuntimeOnly("org.junit.platform:junit-platform-launcher")

	// Additional test dependencies for MockMvc
	testImplementation("org.springframework:spring-webmvc")
	testImplementation("org.springframework.boot:spring-boot-test-autoconfigure")
	testImplementation("org.springframework.security:spring-security-test")


	//lombok
	compileOnly("org.projectlombok:lombok:$lombokVersion")
	annotationProcessor("org.projectlombok:lombok:$lombokVersion")

	testCompileOnly("org.projectlombok:lombok:$lombokVersion")
	testAnnotationProcessor("org.projectlombok:lombok:$lombokVersion")

	runtimeOnly("com.h2database:h2")

	runtimeOnly("org.postgresql:postgresql")

	annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

	implementation("org.mapstruct:mapstruct:1.5.5.Final")
	annotationProcessor("org.mapstruct:mapstruct-processor:1.5.5.Final")


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
