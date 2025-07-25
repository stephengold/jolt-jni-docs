// Gradle script to build and run the "java-apps" subproject of jolt-jni-docs

import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform

plugins {
    application // to build JVM applications
    checkstyle  // to analyze Java sourcecode for style violations
}

checkstyle {
    toolVersion = libs.versions.checkstyle.get()
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

tasks.withType<JavaCompile>().all { // Java compile-time options:
    options.compilerArgs.add("-Xdiags:verbose")
    options.compilerArgs.add("-Xlint:unchecked")
    options.encoding = "UTF-8"
    options.isDeprecation = true // to provide detailed deprecation warnings
    options.release = 11
}

// Register tasks to run specific applications:

tasks.register<JavaExec>("HelloBroadPhase") {
    description = "Runs the HelloBroadPhase tutorial app."
    mainClass = "com.github.stephengold.sportjolt.javaapp.sample.HelloBroadPhase"
}
tasks.register<JavaExec>("HelloCcd") {
    description = "Runs the HelloCcd tutorial app."
    mainClass = "com.github.stephengold.sportjolt.javaapp.sample.HelloCcd"
}
tasks.register<JavaExec>("HelloCharacter") {
    description = "Runs the HelloCharacter tutorial app."
    mainClass = "com.github.stephengold.sportjolt.javaapp.sample.HelloCharacter"
}
tasks.register<JavaExec>("HelloCharacterVirtual") {
    description = "Runs the HelloCharacterVirtual tutorial app."
    mainClass = "com.github.stephengold.sportjolt.javaapp.sample.HelloCharacterVirtual"
}
tasks.register<JavaExec>("HelloCloth") {
    description = "Runs the HelloCloth tutorial app."
    mainClass = "com.github.stephengold.sportjolt.javaapp.sample.HelloCloth"
}
tasks.register<JavaExec>("HelloConstraint") {
    description = "Runs the HelloConstraint tutorial app."
    mainClass = "com.github.stephengold.sportjolt.javaapp.sample.HelloConstraint"
}
tasks.register<JavaExec>("HelloContactResponse") {
    description = "Runs the HelloContactResponse tutorial app."
    mainClass = "com.github.stephengold.sportjolt.javaapp.sample.HelloContactResponse"
}
tasks.register<JavaExec>("HelloDamping") {
    description = "Runs the HelloDamping tutorial app."
    mainClass = "com.github.stephengold.sportjolt.javaapp.sample.HelloDamping"
}
tasks.register<JavaExec>("HelloDeactivation") {
    description = "Runs the HelloDeactivation tutorial app."
    mainClass = "com.github.stephengold.sportjolt.javaapp.sample.HelloDeactivation"
}
tasks.register<JavaExec>("HelloDoubleEnded") {
    description = "Runs the HelloDoubleEnded tutorial app."
    mainClass = "com.github.stephengold.sportjolt.javaapp.sample.HelloDoubleEnded"
}
tasks.register<JavaExec>("HelloJoltJni") {
    description = "Runs the HelloJoltJni tutorial app."
    mainClass = "com.github.stephengold.sportjolt.javaapp.sample.console.HelloJoltJni"
}
tasks.register<JavaExec>("HelloKinematics") {
    description = "Runs the HelloKinematics tutorial app."
    mainClass = "com.github.stephengold.sportjolt.javaapp.sample.HelloKinematics"
}
tasks.register<JavaExec>("HelloLimit") {
    description = "Runs the HelloLimit tutorial app."
    mainClass = "com.github.stephengold.sportjolt.javaapp.sample.HelloLimit"
}
tasks.register<JavaExec>("HelloMotor") {
    description = "Runs the HelloMotor tutorial app."
    mainClass = "com.github.stephengold.sportjolt.javaapp.sample.HelloMotor"
}
tasks.register<JavaExec>("HelloNarrowPhase") {
    description = "Runs the HelloNarrowPhase tutorial app."
    mainClass = "com.github.stephengold.sportjolt.javaapp.sample.HelloNarrowPhase"
}
tasks.register<JavaExec>("HelloPin") {
    description = "Runs the HelloPin tutorial app."
    mainClass = "com.github.stephengold.sportjolt.javaapp.sample.HelloPin"
}
tasks.register<JavaExec>("HelloPivot") {
    description = "Runs the HelloPivot tutorial app."
    mainClass = "com.github.stephengold.sportjolt.javaapp.sample.HelloPivot"
}
tasks.register<JavaExec>("HelloRigidBody") {
    description = "Runs the HelloRigidBody tutorial app."
    mainClass = "com.github.stephengold.sportjolt.javaapp.sample.HelloRigidBody"
}
tasks.register<JavaExec>("HelloSensor") {
    description = "Runs the HelloSensor tutorial app."
    mainClass = "com.github.stephengold.sportjolt.javaapp.sample.HelloSensor"
}
tasks.register<JavaExec>("HelloSoftRope") {
    description = "Runs the HelloSoftRope tutorial app."
    mainClass = "com.github.stephengold.sportjolt.javaapp.sample.HelloSoftRope"
}
tasks.register<JavaExec>("HelloServo") {
    description = "Runs the HelloServo tutorial app."
    mainClass = "com.github.stephengold.sportjolt.javaapp.sample.HelloServo"
}
tasks.register<JavaExec>("HelloSoftBody") {
    description = "Runs the HelloSoftBody tutorial app."
    mainClass = "com.github.stephengold.sportjolt.javaapp.sample.HelloSoftBody"
}
tasks.register<JavaExec>("HelloSport") {
    description = "Runs the HelloSport tutorial app."
    mainClass = "com.github.stephengold.sportjolt.javaapp.sample.HelloSport"
}
tasks.register<JavaExec>("HelloSpring") {
    description = "Runs the HelloSpring tutorial app."
    mainClass = "com.github.stephengold.sportjolt.javaapp.sample.HelloSpring"
}
tasks.register<JavaExec>("HelloStaticBody") {
    description = "Runs the HelloStaticBody tutorial app."
    mainClass = "com.github.stephengold.sportjolt.javaapp.sample.HelloStaticBody"
}
tasks.register<JavaExec>("HelloVehicle") {
    description = "Runs the HelloVehicle tutorial app."
    mainClass = "com.github.stephengold.sportjolt.javaapp.sample.HelloVehicle"
}
tasks.register<JavaExec>("HelloWalk") {
    description = "Runs the HelloWalk tutorial app."
    mainClass = "com.github.stephengold.sportjolt.javaapp.sample.HelloWalk"
}

val assertions = providers.gradleProperty("assertions").get().equals("true")

val os = DefaultNativePlatform.getCurrentOperatingSystem()
val includeLinux = os.isLinux
val includeMacOsX = os.isMacOsX
val includeWindows = os.isWindows
val enableNativeAccess = JavaVersion.current().isCompatibleWith(JavaVersion.VERSION_17)

tasks.withType<JavaExec>().all { // JVM runtime options:
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
    runtimeOnly(libs.oshi.core)
    runtimeOnly(libs.log4j.impl)
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
