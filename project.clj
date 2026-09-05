; Leiningen script to build the src/clojure subproject of jolt-jni-docs

(def joltjni "6.0.0")
(def lwjgl "3.4.2")

(defproject jolt-jni-clojure "0.1.0-SNAPSHOT"
  :aliases {
    "HelloJoltJni" ["run" "-m" "clojure.HelloJoltJni"]
  }
  :dependencies [
    [com.github.stephengold/jolt-jni-Linux64 ~joltjni :classifier "DebugSp"]
    [com.github.stephengold/jolt-jni-Linux64_fma ~joltjni :classifier "DebugSp"]
    [com.github.stephengold/jolt-jni-Linux_ARM32hf ~joltjni :classifier "DebugSp"]
    [com.github.stephengold/jolt-jni-Linux_ARM64 "6.0.2" :classifier "DebugSp"]
    [com.github.stephengold/jolt-jni-MacOSX64 ~joltjni :classifier "DebugSp"]
    [com.github.stephengold/jolt-jni-MacOSX_ARM64 ~joltjni :classifier "DebugSp"]
    [com.github.stephengold/jolt-jni-Windows64 ~joltjni :classifier "DebugSp"]
    [com.github.stephengold/jolt-jni-Windows64_avx2 ~joltjni :classifier "DebugSp"]
    [com.github.stephengold/jolt-jni-Windows_ARM64 ~joltjni :classifier "DebugSp"]
    [com.github.stephengold/sport-jolt "2.1.1"]
    [org.clojure/clojure "1.12.6"]
    [org.lwjgl/lwjgl ~lwjgl :classifier "natives-linux"]
    [org.lwjgl/lwjgl ~lwjgl :classifier "natives-linux-arm32"]
    [org.lwjgl/lwjgl ~lwjgl :classifier "natives-linux-arm64"]
    [org.lwjgl/lwjgl ~lwjgl :classifier "natives-macos"]
    [org.lwjgl/lwjgl ~lwjgl :classifier "natives-macos-arm64"]
    [org.lwjgl/lwjgl ~lwjgl :classifier "natives-windows"]
    [org.lwjgl/lwjgl ~lwjgl :classifier "natives-windows-arm64"]
    [org.lwjgl/lwjgl-assimp ~lwjgl :classifier "natives-linux"]
    [org.lwjgl/lwjgl-assimp ~lwjgl :classifier "natives-linux-arm32"]
    [org.lwjgl/lwjgl-assimp ~lwjgl :classifier "natives-linux-arm64"]
    [org.lwjgl/lwjgl-assimp ~lwjgl :classifier "natives-macos"]
    [org.lwjgl/lwjgl-assimp ~lwjgl :classifier "natives-macos-arm64"]
    [org.lwjgl/lwjgl-assimp ~lwjgl :classifier "natives-windows"]
    [org.lwjgl/lwjgl-assimp ~lwjgl :classifier "natives-windows-arm64"]
    [org.lwjgl/lwjgl-glfw ~lwjgl :classifier "natives-linux"]
    [org.lwjgl/lwjgl-glfw ~lwjgl :classifier "natives-linux-arm32"]
    [org.lwjgl/lwjgl-glfw ~lwjgl :classifier "natives-linux-arm64"]
    [org.lwjgl/lwjgl-glfw ~lwjgl :classifier "natives-macos"]
    [org.lwjgl/lwjgl-glfw ~lwjgl :classifier "natives-macos-arm64"]
    [org.lwjgl/lwjgl-glfw ~lwjgl :classifier "natives-windows"]
    [org.lwjgl/lwjgl-glfw ~lwjgl :classifier "natives-windows-arm64"]
    [org.lwjgl/lwjgl-opengl ~lwjgl :classifier "natives-linux"]
    [org.lwjgl/lwjgl-opengl ~lwjgl :classifier "natives-linux-arm32"]
    [org.lwjgl/lwjgl-opengl ~lwjgl :classifier "natives-linux-arm64"]
    [org.lwjgl/lwjgl-opengl ~lwjgl :classifier "natives-macos"]
    [org.lwjgl/lwjgl-opengl ~lwjgl :classifier "natives-macos-arm64"]
    [org.lwjgl/lwjgl-opengl ~lwjgl :classifier "natives-windows"]
    [org.lwjgl/lwjgl-opengl ~lwjgl :classifier "natives-windows-arm64"]
  ]
  :description "Sample applications (in Clojure) for the Jolt-JNI physics-simulation library"
  :license {
    :name "BSD 3-Clause License"
    :url "https://github.com/stephengold/jolt-jni-docs/blob/master/LICENSE"
  }
  :main ^:skip-aot clojure.HelloJoltJni
  :profiles {
    :uberjar {
      :aot :all
      :jvm-opts ["-Dclojure.compiler.direct-linking=true"]
    }
  }
  :target-path "target/%s"
  :url "https://github.com/stephengold/jolt-jni-docs"
)
