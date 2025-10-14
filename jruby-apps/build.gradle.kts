// Gradle script to build and run the "jruby-apps" subproject of jolt-jni-docs

import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform

plugins {
    application // to build JVM applications
}

sourceSets.main {
    resources {
        srcDir("scripts") // for NetBeans access
    }
}

application {
    mainClass = "com.github.stephengold.jsr223.RunScript"
}

// Register tasks to run specific applications:

// physics console apps (no graphics)
tasks.register<JavaExec>("HelloJoltJni") {
    args("jruby", "scripts/console/HelloJoltJni.rb",
         "../class-lists/jolt-jni-classes.txt",
         "../class-lists/snap-loader-classes.txt")
    description = "Runs the Jruby port of the HelloJoltJni console app."
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
    mainClass = "com.github.stephengold.jsr223.RunScript"
}

// which BTF (build type + flavor) of native physics libraries to include:
val btf = providers.gradleProperty("btf").get()

dependencies {
    implementation(project(":java-apps"))
    implementation(libs.sport.jolt)
    runtimeOnly(libs.jruby)

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
