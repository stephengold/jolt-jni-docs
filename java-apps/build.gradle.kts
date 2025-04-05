// Gradle script to build and run the "java-apps" subproject of jolt-jni-docs

import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform

plugins {
    `application` // to build JVM applications
    `checkstyle`  // to analyze Java sourcecode for style violations
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
    options.release = 11
    options.setDeprecation(true) // to provide detailed deprecation warnings
}

// Register tasks to run specific applications:

tasks.register<JavaExec>("HelloCcd") {
    description = "Runs the HelloCcd tutorial app."
    mainClass = "com.github.stephengold.sportjolt.javaapp.sample.HelloCcd"
}
tasks.register<JavaExec>("HelloCharacter") {
    description = "Runs the HelloCharacter tutorial app."
    mainClass = "com.github.stephengold.sportjolt.javaapp.sample.HelloCharacter"
}
tasks.register<JavaExec>("HelloConstraint") {
    description = "Runs the HelloConstraint tutorial app."
    mainClass = "com.github.stephengold.sportjolt.javaapp.sample.HelloConstraint"
}
tasks.register<JavaExec>("HelloDamping") {
    description = "Runs the HelloDamping tutorial app."
    mainClass = "com.github.stephengold.sportjolt.javaapp.sample.HelloDamping"
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
tasks.register<JavaExec>("HelloRigidBody") {
    description = "Runs the HelloRigidBody tutorial app."
    mainClass = "com.github.stephengold.sportjolt.javaapp.sample.HelloRigidBody"
}
tasks.register<JavaExec>("HelloSoftBody") {
    description = "Runs the HelloSoftBody tutorial app."
    mainClass = "com.github.stephengold.sportjolt.javaapp.sample.HelloSoftBody"
}
tasks.register<JavaExec>("HelloStaticBody") {
    description = "Runs the HelloStaticBody tutorial app."
    mainClass = "com.github.stephengold.sportjolt.javaapp.sample.HelloStaticBody"
}
tasks.register<JavaExec>("HelloVehicle") {
    description = "Runs the HelloVehicle tutorial app."
    mainClass = "com.github.stephengold.sportjolt.javaapp.sample.HelloVehicle"
}

val assertions = providers.gradleProperty("assertions").get().equals("true")

val os = DefaultNativePlatform.getCurrentOperatingSystem()
val includeLinux = os.isLinux()
val includeMacOsX = os.isMacOsX()
val includeWindows = os.isWindows()

tasks.withType<JavaExec>().all { // JVM runtime options:
    if (os.isMacOsX()) {
        jvmArgs("-XstartOnFirstThread") // required for GLFW on macOS
    }
    classpath = sourceSets.main.get().getRuntimeClasspath()
    enableAssertions = assertions
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
