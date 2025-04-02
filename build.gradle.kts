// root-level Gradle script to build and run the jolt-jni-doc project

configurations.all {
    resolutionStrategy.cacheChangingModulesFor(0, "seconds") // to disable caching of snapshots
}

tasks.register("checkstyle") {
    dependsOn(":java-apps:checkstyleMain")
    description = "Checks the style of all Java sourcecode."
}

