plugins {
    `java-library`
}

dependencies {
    // Adventure and SLF4J are provided at runtime by Paper / Velocity. We keep
    // them off the published artifact and out of the shaded jar so that xCore
    // doesn't drag a stale Adventure 4.17 alongside Paper 26.1.2's 4.26.
    compileOnly("net.kyori:adventure-api:4.17.0")
    compileOnly("net.kyori:adventure-text-minimessage:4.17.0")
    compileOnly("org.slf4j:slf4j-api:2.0.16")

    // Gson is relocated when shaded, so we keep it on the API surface for
    // downstream consumers that don't pull paper-api directly.
    api("com.google.code.gson:gson:2.11.0")

    compileOnly("org.jetbrains:annotations:26.0.1")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = "xcore-common"
            from(components["java"])
        }
    }
}
