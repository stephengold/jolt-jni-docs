// Gradle script to build and run the "scala-apps" subproject of jolt-jni-docs

import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform

plugins {
    application // to build JVM applications
    scala // to compile Scala
}

sourceSets.main {
    resources {
        srcDir("src/main/scala") // for NetBeans access
    }
}

tasks.named<Jar>("jar") {
    manifest {
        attributes["Main-Class"] = application.mainClass
    }
}

tasks.withType<ScalaCompile>().configureEach { // Scala compile-time options:
    scalaCompileOptions.additionalParameters = listOf("-Wunused:all", "-Xtarget:17")
}

application {
    mainClass = "com.github.stephengold.sportjolt.scala.console.HelloJoltJni"
}

// Register tasks to run specific applications:

// physics console apps (no graphics)
tasks.register<JavaExec>("HelloJoltJni") {
    description = "Runs the Scala port of the HelloJoltJni console app."
    mainClass = "com.github.stephengold.sportjolt.scala.console.HelloJoltJni"
}

// physics tutorial apps (very simple)
tasks.register<JavaExec>("HelloCcd") {
    description = "Runs the Scala port of the HelloCcd tutorial app."
    mainClass = "com.github.stephengold.sportjolt.scala.tutorial.HelloCcd"
}
tasks.register<JavaExec>("HelloBroadPhase") {
    description = "Runs the Scala port of the HelloBroadPhase tutorial app."
    mainClass = "com.github.stephengold.sportjolt.scala.tutorial.HelloBroadPhase"
}
tasks.register<JavaExec>("HelloCharacter") {
    description = "Runs the Scala port of the HelloCharacter tutorial app."
    mainClass = "com.github.stephengold.sportjolt.scala.tutorial.HelloCharacter"
}
tasks.register<JavaExec>("HelloCharacterVirtual") {
    description = "Runs the Scala port of the HelloCharacterVirtual tutorial app."
    mainClass = "com.github.stephengold.sportjolt.scala.tutorial.HelloCharacterVirtual"
}
tasks.register<JavaExec>("HelloCloth") {
    description = "Runs the Scala port of the HelloCloth tutorial app."
    mainClass = "com.github.stephengold.sportjolt.scala.tutorial.HelloCloth"
}
tasks.register<JavaExec>("HelloConstraint") {
    description = "Runs the Scala port of the HelloConstraint tutorial app."
    mainClass = "com.github.stephengold.sportjolt.scala.tutorial.HelloConstraint"
}
tasks.register<JavaExec>("HelloContactResponse") {
    description = "Runs the Scala port of the HelloContactResponse tutorial app."
    mainClass = "com.github.stephengold.sportjolt.scala.tutorial.HelloContactResponse"
}
tasks.register<JavaExec>("HelloDamping") {
    description = "Runs the Scala port of the HelloDamping tutorial app."
    mainClass = "com.github.stephengold.sportjolt.scala.tutorial.HelloDamping"
}
tasks.register<JavaExec>("HelloDeactivation") {
    description = "Runs the Scala port of the HelloDeactivation tutorial app."
    mainClass = "com.github.stephengold.sportjolt.scala.tutorial.HelloDeactivation"
}
tasks.register<JavaExec>("HelloDoubleEnded") {
    description = "Runs the Scala port of the HelloDoubleEnded tutorial app."
    mainClass = "com.github.stephengold.sportjolt.scala.tutorial.HelloDoubleEnded"
}
tasks.register<JavaExec>("HelloKinematics") {
    description = "Runs the Scala port of the HelloKinematics tutorial app."
    mainClass = "com.github.stephengold.sportjolt.scala.tutorial.HelloKinematics"
}
tasks.register<JavaExec>("HelloLimit") {
    description = "Runs the Scala port of the HelloLimit tutorial app."
    mainClass = "com.github.stephengold.sportjolt.scala.tutorial.HelloLimit"
}
tasks.register<JavaExec>("HelloMotor") {
    description = "Runs the Scala port of the HelloMotor tutorial app."
    mainClass = "com.github.stephengold.sportjolt.scala.tutorial.HelloMotor"
}
tasks.register<JavaExec>("HelloNarrowPhase") {
    description = "Runs the Scala port of the HelloNarrowPhase tutorial app."
    mainClass = "com.github.stephengold.sportjolt.scala.tutorial.HelloNarrowPhase"
}
tasks.register<JavaExec>("HelloPivot") {
    description = "Runs the Scala port of the HelloPivot tutorial app."
    mainClass = "com.github.stephengold.sportjolt.scala.tutorial.HelloPivot"
}
tasks.register<JavaExec>("HelloRigidBody") {
    description = "Runs the Scala port of the HelloRigidBody tutorial app."
    mainClass = "com.github.stephengold.sportjolt.scala.tutorial.HelloRigidBody"
}
tasks.register<JavaExec>("HelloSensor") {
    description = "Runs the Scala port of the HelloSensor tutorial app."
    mainClass = "com.github.stephengold.sportjolt.scala.tutorial.HelloSensor"
}
tasks.register<JavaExec>("HelloSoftBody") {
    description = "Runs the Scala port of the HelloSoftBody tutorial app."
    mainClass = "com.github.stephengold.sportjolt.scala.tutorial.HelloSoftBody"
}
tasks.register<JavaExec>("HelloSport") {
    description = "Runs the Scala port of the HelloSport tutorial app."
    mainClass = "com.github.stephengold.sportjolt.scala.tutorial.HelloSport"
}
tasks.register<JavaExec>("HelloStaticBody") {
    description = "Runs the Scala port of the HelloStaticBody tutorial app."
    mainClass = "com.github.stephengold.sportjolt.scala.tutorial.HelloStaticBody"
}
tasks.register<JavaExec>("HelloWalk") {
    description = "Runs the Scala port of the HelloWalk tutorial app."
    mainClass = "com.github.stephengold.sportjolt.scala.tutorial.HelloWalk"
}

val assertions = providers.gradleProperty("assertions").get().equals("true")

val os = DefaultNativePlatform.getCurrentOperatingSystem()
val includeLinux = os.isLinux
val includeMacOsX = os.isMacOsX
val includeWindows = os.isWindows
val enableNativeAccess = JavaVersion.current().isCompatibleWith(JavaVersion.VERSION_17)

tasks.withType<JavaExec>().configureEach { // JVM runtime options:
    if (os.isMacOsX) {
        jvmArgs("-XstartOnFirstThread") // required for GLFW on macOS
    }
    classpath = sourceSets.main.get().runtimeClasspath
    enableAssertions = assertions
    if (enableNativeAccess) {
        jvmArgs("--enable-native-access=ALL-UNNAMED") // suppress System::load() warning
    }
    jvmArgs("-XX:+UseG1GC", "-XX:MaxGCPauseMillis=10")
}

// which BTF (build type + flavor) of native physics libraries to include:
val btf = providers.gradleProperty("btf").get()

dependencies {
    implementation(libs.scala.library)
    implementation(libs.sport.jolt)

    if (includeLinux) {
        runtimeOnly(variantOf(libs.jolt.jni.linux64){classifier(btf)})
        runtimeOnly(variantOf(libs.jolt.jni.linux64fma){classifier(btf)})

        runtimeOnly(variantOf(libs.lwjgl){classifier("natives-linux")})
        runtimeOnly(variantOf(libs.lwjgl.assimp){classifier("natives-linux")})
        runtimeOnly(variantOf(libs.lwjgl.glfw){classifier("natives-linux")})
        runtimeOnly(variantOf(libs.lwjgl.opengl){classifier("natives-linux")})

        runtimeOnly(variantOf(libs.jolt.jni.linuxarm32hf){classifier(btf)})

        runtimeOnly(variantOf(libs.lwjgl){classifier("natives-linux-arm32")})
        runtimeOnly(variantOf(libs.lwjgl.assimp){classifier("natives-linux-arm32")})
        runtimeOnly(variantOf(libs.lwjgl.glfw){classifier("natives-linux-arm32")})
        runtimeOnly(variantOf(libs.lwjgl.opengl){classifier("natives-linux-arm32")})

        runtimeOnly(variantOf(libs.jolt.jni.linuxarm64){classifier(btf)})

        runtimeOnly(variantOf(libs.lwjgl){classifier("natives-linux-arm64")})
        runtimeOnly(variantOf(libs.lwjgl.assimp){classifier("natives-linux-arm64")})
        runtimeOnly(variantOf(libs.lwjgl.glfw){classifier("natives-linux-arm64")})
        runtimeOnly(variantOf(libs.lwjgl.opengl){classifier("natives-linux-arm64")})
    }

    if (includeMacOsX) {
        runtimeOnly(variantOf(libs.jolt.jni.macosx64){classifier(btf)})

        runtimeOnly(variantOf(libs.lwjgl){classifier("natives-macos")})
        runtimeOnly(variantOf(libs.lwjgl.assimp){classifier("natives-macos")})
        runtimeOnly(variantOf(libs.lwjgl.glfw){classifier("natives-macos")})
        runtimeOnly(variantOf(libs.lwjgl.opengl){classifier("natives-macos")})

        runtimeOnly(variantOf(libs.jolt.jni.macosxarm64){classifier(btf)})

        runtimeOnly(variantOf(libs.lwjgl){classifier("natives-macos-arm64")})
        runtimeOnly(variantOf(libs.lwjgl.assimp){classifier("natives-macos-arm64")})
        runtimeOnly(variantOf(libs.lwjgl.glfw){classifier("natives-macos-arm64")})
        runtimeOnly(variantOf(libs.lwjgl.opengl){classifier("natives-macos-arm64")})
    }

    if (includeWindows) {
        runtimeOnly(variantOf(libs.jolt.jni.windows64){classifier(btf)})
        runtimeOnly(variantOf(libs.jolt.jni.windows64avx2){classifier(btf)})

        runtimeOnly(variantOf(libs.lwjgl){classifier("natives-windows")})
        runtimeOnly(variantOf(libs.lwjgl.assimp){classifier("natives-windows")})
        runtimeOnly(variantOf(libs.lwjgl.glfw){classifier("natives-windows")})
        runtimeOnly(variantOf(libs.lwjgl.opengl){classifier("natives-windows")})
    }
}
