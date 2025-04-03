// Gradle script to build the "docs" subproject of jolt-jni-docs

plugins {
    `java`
}

sourceSets.main {
    java {
        srcDir("en") // for IDE access (no Java there)
    }
}
