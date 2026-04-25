plugins {
    id("org.springframework.boot") version "3.2.5"
    id("io.spring.dependency-management") version "1.1.4"
    id("java")
    id("jacoco")
    id("checkstyle")
    id("pmd")
    id("com.diffplug.spotless") version "6.25.0"
    id("com.github.spotbugs") version "6.0.14"
}

group = "com.res"
version = "1.0.0"
description = "Resilient HMS Backend"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-aop")
    implementation("org.flywaydb:flyway-core:10.13.0")
    implementation("org.flywaydb:flyway-database-postgresql:10.13.0")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.5.0")
    implementation("io.hypersistence:hypersistence-utils-hibernate-63:3.8.2")
    implementation("org.mapstruct:mapstruct:1.5.5.Final")
    compileOnly("org.projectlombok:lombok")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    runtimeOnly("org.postgresql:postgresql")
    annotationProcessor("org.projectlombok:lombok")
    annotationProcessor("org.mapstruct:mapstruct-processor:1.5.5.Final")
    annotationProcessor("org.projectlombok:lombok-mapstruct-binding:0.2.0")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("com.h2database:h2")
    testImplementation("com.tngtech.archunit:archunit-junit5:1.3.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

jacoco {
    toolVersion = "0.8.12"
}

spotless {
    java {
        googleJavaFormat("1.19.2")
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
    }
}

checkstyle {
    toolVersion = "10.12.1"
    configFile = file("${rootProject.projectDir}/config/checkstyle/checkstyle.xml")
    isIgnoreFailures = false
    maxWarnings = 0
}

tasks.withType<Checkstyle> {
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

pmd {
    isConsoleOutput = true
    toolVersion = "7.1.0"
    rulesMinimumPriority.set(5)
    ruleSetFiles = files("${rootProject.projectDir}/config/pmd/ruleset.xml")
    ruleSets = listOf() // Clear default rulesets
    isIgnoreFailures = true
}

spotbugs {
    ignoreFailures.set(true)
    showStackTraces = true
    showProgress = true
    effort = com.github.spotbugs.snom.Effort.MAX
    reportLevel = com.github.spotbugs.snom.Confidence.DEFAULT
}

tasks.withType<com.github.spotbugs.snom.SpotBugsTask> {
    reports {
        create("html") {
            required.set(true)
        }
    }
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)

    reports {
        xml.required.set(true)
        html.required.set(true)
    }

    classDirectories.setFrom(
        files(classDirectories.files.map {
            fileTree(it) {
                include(
                    "com/res/hms/**/service/**",
                    "com/res/hms/common/tenant/**",
                    "com/res/hms/common/exception/GlobalExceptionHandler.class"
                )
            }
        })
    )
}

tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.test)

    violationRules {
        rule {
            element = "BUNDLE"
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.70".toBigDecimal()
            }
        }
    }

    classDirectories.setFrom(tasks.jacocoTestReport.get().classDirectories)
}

tasks.check {
    dependsOn(tasks.jacocoTestCoverageVerification)
}
