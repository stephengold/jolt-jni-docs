// global build settings shared by all jolt-jni-docs subprojects

rootProject.name = "jolt-jni-docs"

dependencyResolutionManagement {
    repositories {
        //mavenLocal() // to find libraries installed locally
        mavenCentral() // to find libraries released to the Maven Central repository
        maven {
            name = "Central Portal Snapshots"
            url = uri("https://central.sonatype.com/repository/maven-snapshots/")
        }
    }
}

// subprojects:
include("docs")
include("groovy-apps")
include("java-apps")
include("jython-apps")
include("kotlin-apps")
include("nashorn-apps")
include("scala-apps")
