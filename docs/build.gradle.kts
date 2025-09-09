// Gradle script to build the "docs" subproject of jolt-jni-docs

plugins {
    java
    alias(libs.plugins.download) // to retrieve files from URLs
}

import de.undercouch.gradle.tasks.download.Download

sourceSets.main {
    resources {
        srcDir("en") // for NetBeans access
    }
}

// Register javadoc-to-(web)site tasks, triggered by push-master.yml:

val artifactId = "jolt-jni-Windows64"
val centralUrl = "https://repo1.maven.org/maven2/com/github/stephengold"
val jarVersion = "3.1.0"
val jarName = artifactId + "-" + jarVersion + "-javadoc.jar"
val downloadUrl = centralUrl + "/" + artifactId + "/" + jarVersion + "/" + jarName

tasks.register<Delete>("cleanJavadocJar") {
    delete(jarName)
}

tasks.register<Copy>("copyJavadocToSite") {
    dependsOn("downloadJavadocJar")
    from(zipTree(jarName))
    into("build/site/javadoc/latest")
}

tasks.register<Download>("downloadJavadocJar") {
    dest(file(jarName))
    src(downloadUrl)
}
