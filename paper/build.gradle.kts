plugins {
    `java-library`
    id("com.gradleup.shadow") version "8.3.5"
    id("xyz.jpenilla.run-paper") version "2.3.1"
}

dependencies {
    api(project(":common"))
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    compileOnly("org.jetbrains:annotations:26.0.1")
}

tasks {
    processResources {
        filesMatching("plugin.yml") {
            expand("version" to project.version)
        }
    }

    shadowJar {
        archiveClassifier.set("")
        archiveBaseName.set("xCore-Paper")
        mergeServiceFiles()
        // Relocate Gson so we don't collide with anything Paper bundles.
        relocate("com.google.gson", "dev.xsuite.core.libs.gson")
    }

    jar { archiveClassifier.set("dev") }
    build { dependsOn(shadowJar) }

    runServer { minecraftVersion("1.21.4") }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = "xcore-paper"
            from(components["java"])
        }
    }
}
