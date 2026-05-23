plugins {
    `java-library`
}

dependencies {
    api("net.kyori:adventure-api:4.17.0")
    api("net.kyori:adventure-text-minimessage:4.17.0")
    api("com.google.code.gson:gson:2.11.0")
    api("org.slf4j:slf4j-api:2.0.16")
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
