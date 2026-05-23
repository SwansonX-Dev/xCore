plugins {
    `java-library`
    id("com.gradleup.shadow") version "8.3.5"
}

dependencies {
    api(project(":common"))
    compileOnly("com.velocitypowered:velocity-api:3.4.0-SNAPSHOT")
    annotationProcessor("com.velocitypowered:velocity-api:3.4.0-SNAPSHOT")
    compileOnly("org.jetbrains:annotations:26.0.1")
}

tasks {
    shadowJar {
        archiveClassifier.set("")
        archiveBaseName.set("xCore-Velocity")
        mergeServiceFiles()
        relocate("com.google.gson", "dev.xsuite.core.libs.gson")
    }

    jar { archiveClassifier.set("dev") }
    build { dependsOn(shadowJar) }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = "xcore-velocity"
            from(components["java"])
        }
    }
}
