// global build settings shared by all jolt-jni-docs subprojects except src/clojure

rootProject.name = "jolt-jni-docs"

dependencyResolutionManagement {
    repositories {
        mavenCentral() // to find libraries released to the Maven Central repository
        maven {
            name = "Central Portal Snapshots"
            url = uri("https://central.sonatype.com/repository/maven-snapshots/")
        }
        //mavenLocal() // to find libraries installed locally
    }
}

// subprojects:
include("docs")
include("groovy-apps")
include("groovy223-apps")
include("java-apps")
include("jruby-apps")
include("jython-apps")
include("kotlin-apps")
include("luaj-apps")
include("luajava-apps")
include("nashorn-apps")
include("scala-apps")
