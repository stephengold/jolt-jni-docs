// root-level Gradle script to build the jolt-jni-doc project

plugins {
    `base` // to add a "clean" task to the root project
}

configurations.all {
    resolutionStrategy.cacheChangingModulesFor(0, "seconds") // to disable caching of snapshots
}

tasks.register("checkstyle") {
    dependsOn(":java-apps:checkstyleMain")
    description = "Checks the style of all Java sourcecode."
}

// Register cleanup tasks:

tasks.named("clean") {
    dependsOn("cleanDocsBuild", "cleanNodeModules")
}
tasks.register<Delete>("cleanDocsBuild") {
    delete("docs/build")
}
tasks.register<Delete>("cleanNodeModules") {
    delete("node_modules")
}
