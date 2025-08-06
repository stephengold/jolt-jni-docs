// root-level Gradle script to build the jolt-jni-docs project

plugins {
    base // to add a "clean" task to the root project
    alias(libs.plugins.diktat) // to analyze Kotlin sourcecode
}

configurations.all {
    resolutionStrategy.cacheChangingModulesFor(0, "seconds") // to disable caching of snapshots
}

tasks.register("checkstyle") {
    dependsOn(":java-apps:checkstyleMain")
    description = "Checks the style of all Java sourcecode."
}

diktat {
    diktatConfigFile = file(rootDir.path + "/config/diktat.yml")
    inputs {
        include("kotlin-apps/src/main/kotlin/**/*.kt")
    }
}

// Register cleanup tasks:

tasks.named("clean") {
    dependsOn("cleanDocsBuild", "cleanNodeModules", ":docs:cleanJavadocJar")
}
tasks.register<Delete>("cleanDocsBuild") {
    delete("docs/build")
}
tasks.register<Delete>("cleanNodeModules") {
    delete("node_modules")
}
