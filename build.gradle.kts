import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
	java
	id("org.springframework.boot") version "4.0.4"
	id("io.spring.dependency-management") version "1.1.7"
	id("checkstyle")
	id("application")
	id("org.sonarqube") version "7.2.3.7755"
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
	implementation("org.springframework.boot:spring-boot-starter")
	implementation("org.springframework.boot:spring-boot-starter-web")

	testImplementation("org.springframework.boot:spring-boot-starter-test")

	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
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
