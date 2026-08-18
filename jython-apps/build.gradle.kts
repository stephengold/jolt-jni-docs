// Gradle script to build and run the "jython-apps" subproject of jolt-jni-docs

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
    args("jython", "scripts/console/hello_jolt_jni.py",
         "../class-lists/jolt-jni-classes.txt",
         "../class-lists/snap-loader-classes.txt")
    description = "Runs the Jython port of the HelloJoltJni console app."
}

// physics tutorial apps (very simple)
tasks.register<JavaExec>("HelloBroadPhase") {
    args("jython", "scripts/sport/tutorial/hello_broad_phase.py",
         "../class-lists/jolt-jni-classes.txt",
         "../class-lists/sport-jolt-classes.txt")
    description = "Runs the Jython port of the HelloBroadPhase tutorial app."
}
tasks.register<JavaExec>("HelloCcd") {
    args("jython", "scripts/sport/tutorial/hello_ccd.py",
         "../class-lists/jolt-jni-classes.txt",
         "../class-lists/sport-jolt-classes.txt")
    description = "Runs the Jython port of the HelloCcd tutorial app."
}
tasks.register<JavaExec>("HelloCharacter") {
    args("jython", "scripts/sport/tutorial/hello_character.py",
         "../class-lists/jolt-jni-classes.txt",
         "../class-lists/sport-jolt-classes.txt")
    description = "Runs the Jython port of the HelloCharacter tutorial app."
}
tasks.register<JavaExec>("HelloCharacterVirtual") {
    args("jython", "scripts/sport/tutorial/hello_character_virtual.py",
         "../class-lists/jolt-jni-classes.txt",
         "../class-lists/sport-jolt-classes.txt")
    description = "Runs the Jython port of the HelloCharacterVirtual tutorial app."
}
tasks.register<JavaExec>("HelloCloth") {
    args("jython", "scripts/sport/tutorial/hello_cloth.py",
         "../class-lists/jolt-jni-classes.txt",
         "../class-lists/sport-jolt-classes.txt")
    description = "Runs the Jython port of the HelloCloth tutorial app."
}
tasks.register<JavaExec>("HelloConstraint") {
    args("jython", "scripts/sport/tutorial/hello_constraint.py",
         "../class-lists/jolt-jni-classes.txt",
         "../class-lists/sport-jolt-classes.txt")
    description = "Runs the Jython port of the HelloConstraint tutorial app."
}
tasks.register<JavaExec>("HelloContactResponse") {
    args("jython", "scripts/sport/tutorial/hello_contact_response.py",
         "../class-lists/jolt-jni-classes.txt",
         "../class-lists/sport-jolt-classes.txt")
    description = "Runs the Jython port of the HelloContactResponse tutorial app."
}
tasks.register<JavaExec>("HelloDamping") {
    args("jython", "scripts/sport/tutorial/hello_damping.py",
         "../class-lists/jolt-jni-classes.txt",
         "../class-lists/sport-jolt-classes.txt")
    description = "Runs the Jython port of the HelloDamping tutorial app."
}
tasks.register<JavaExec>("HelloDeactivation") {
    args("jython", "scripts/sport/tutorial/hello_deactivation.py",
         "../class-lists/jolt-jni-classes.txt",
         "../class-lists/sport-jolt-classes.txt")
    description = "Runs the Jython port of the HelloDeactivation tutorial app."
}
tasks.register<JavaExec>("HelloDoubleEnded") {
    args("jython", "scripts/sport/tutorial/hello_double_ended.py",
         "../class-lists/jolt-jni-classes.txt",
         "../class-lists/sport-jolt-classes.txt")
    description = "Runs the Jython port of the HelloDoubleEnded tutorial app."
}
tasks.register<JavaExec>("HelloKinematics") {
    args("jython", "scripts/sport/tutorial/hello_kinematics.py",
         "../class-lists/jolt-jni-classes.txt",
         "../class-lists/sport-jolt-classes.txt")
    description = "Runs the Jython port of the HelloKinematics tutorial app."
}
tasks.register<JavaExec>("HelloLimit") {
    args("jython", "scripts/sport/tutorial/hello_limit.py",
         "../class-lists/jolt-jni-classes.txt",
         "../class-lists/sport-jolt-classes.txt")
    description = "Runs the Jython port of the HelloLimit tutorial app."
}
tasks.register<JavaExec>("HelloMotor") {
    args("jython", "scripts/sport/tutorial/hello_motor.py",
         "../class-lists/jolt-jni-classes.txt",
         "../class-lists/sport-jolt-classes.txt")
    description = "Runs the Jython port of the HelloMotor tutorial app."
}
tasks.register<JavaExec>("HelloNarrowPhase") {
    args("jython", "scripts/sport/tutorial/hello_narrow_phase.py",
         "../class-lists/jolt-jni-classes.txt",
         "../class-lists/sport-jolt-classes.txt")
    description = "Runs the Jython port of the HelloNarrowPhase tutorial app."
}
tasks.register<JavaExec>("HelloPin") {
    args("jython", "scripts/sport/tutorial/hello_pin.py",
         "../class-lists/jolt-jni-classes.txt",
         "../class-lists/sport-jolt-classes.txt")
    description = "Runs the Jython port of the HelloPin tutorial app."
}
tasks.register<JavaExec>("HelloPivot") {
    args("jython", "scripts/sport/tutorial/hello_pivot.py",
         "../class-lists/jolt-jni-classes.txt",
         "../class-lists/sport-jolt-classes.txt")
    description = "Runs the Jython port of the HelloPivot tutorial app."
}
tasks.register<JavaExec>("HelloRigidBody") {
    args("jython", "scripts/sport/tutorial/hello_rigid_body.py",
         "../class-lists/jolt-jni-classes.txt",
         "../class-lists/sport-jolt-classes.txt")
    description = "Runs the Jython port of the HelloRigidBody tutorial app."
}
tasks.register<JavaExec>("HelloSensor") {
    args("jython", "scripts/sport/tutorial/hello_sensor.py",
         "../class-lists/jolt-jni-classes.txt",
         "../class-lists/sport-jolt-classes.txt")
    description = "Runs the Jython port of the HelloSensor tutorial app."
}
tasks.register<JavaExec>("HelloSoftBody") {
    args("jython", "scripts/sport/tutorial/hello_soft_body.py",
         "../class-lists/jolt-jni-classes.txt",
         "../class-lists/sport-jolt-classes.txt")
    description = "Runs the Jython port of the HelloSoftBody tutorial app."
}
tasks.register<JavaExec>("HelloSport") {
    args("jython", "scripts/sport/tutorial/hello_sport.py",
         "../class-lists/jolt-jni-classes.txt",
         "../class-lists/sport-jolt-classes.txt")
    description = "Runs the Jython port of the HelloSport tutorial app."
}
tasks.register<JavaExec>("HelloStaticBody") {
    args("jython", "scripts/sport/tutorial/hello_static_body.py",
         "../class-lists/jolt-jni-classes.txt",
         "../class-lists/sport-jolt-classes.txt")
    description = "Runs the Jython port of the HelloStaticBody tutorial app."
}
tasks.register<JavaExec>("HelloWalk") {
    args("jython", "scripts/sport/tutorial/hello_walk.py",
         "../class-lists/jolt-jni-classes.txt",
         "../class-lists/sport-jolt-classes.txt")
    description = "Runs the Jython port of the HelloWalk tutorial app."
}

val assertions = providers.gradleProperty("assertions").get().equals("true")

val os = DefaultNativePlatform.getCurrentOperatingSystem()
val includeLinux = os.isLinux
val includeMacOsX = os.isMacOsX
val includeWindows = os.isWindows
val enableNativeAccess = JavaVersion.current().isCompatibleWith(JavaVersion.VERSION_17)

tasks.withType<JavaExec>().configureEach { // JVM runtime options:
    if (os.isLinux) {
        environment("__GL_THREADED_OPTIMIZATIONS", "0") // see lwjgl3 issue #1071
    }
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
    runtimeOnly(libs.jython)

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

        runtimeOnly(variantOf(libs.jolt.jni.windowsarm64){classifier(btf)})

        runtimeOnly(variantOf(libs.lwjgl){classifier("natives-windows-arm64")})
        runtimeOnly(variantOf(libs.lwjgl.assimp){classifier("natives-windows-arm64")})
        runtimeOnly(variantOf(libs.lwjgl.glfw){classifier("natives-windows-arm64")})
        runtimeOnly(variantOf(libs.lwjgl.opengl){classifier("natives-windows-arm64")})
    }
}
