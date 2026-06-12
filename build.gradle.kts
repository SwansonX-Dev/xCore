plugins {
    `java-library`
    `maven-publish`
}

allprojects {
    group = "dev.xsuite"
    version = "0.3.1"
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")

    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
        withSourcesJar()
        withJavadocJar()
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release.set(21)
    }

    // Javadoc is published for IDE integration but isn't load-bearing here;
    // missing tags shouldn't block a release build.
    tasks.withType<Javadoc>().configureEach {
        (options as StandardJavadocDocletOptions).apply {
            addStringOption("Xdoclint:none", "-quiet")
            encoding = "UTF-8"
        }
        isFailOnError = false
    }
}
