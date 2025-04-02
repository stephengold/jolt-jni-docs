// root-level Gradle script to build and run the jolt-jni-doc project

configurations.all {
    resolutionStrategy.cacheChangingModulesFor(0, "seconds") // to disable caching of snapshots
}
