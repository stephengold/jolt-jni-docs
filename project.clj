; Leiningen script to build the src/clojure subproject of jolt-jni-docs

(def joltjni "6.0.0")

(defproject jolt-jni-clojure "0.1.0-SNAPSHOT"
  :dependencies [
    [com.github.stephengold/jolt-jni-Linux64 ~joltjni :classifier "DebugSp"]
    [com.github.stephengold/jolt-jni-Linux_ARM32hf ~joltjni :classifier "DebugSp"]
    [com.github.stephengold/jolt-jni-Linux_ARM64 "6.0.2" :classifier "DebugSp"]
    [com.github.stephengold/jolt-jni-MacOSX64 ~joltjni :classifier "DebugSp"]
    [com.github.stephengold/jolt-jni-MacOSX_ARM64 ~joltjni :classifier "DebugSp"]
    [com.github.stephengold/jolt-jni-Windows64 ~joltjni :classifier "DebugSp"]
    [com.github.stephengold/jolt-jni-Windows_ARM64 ~joltjni :classifier "DebugSp"]
    [com.github.stephengold/sport-jolt "2.1.1"]
    [org.clojure/clojure "1.12.6"]
  ]
  :description "Sample applications (in Clojure) for the Jolt-JNI physics-simulation library"
  :license {:name "BSD 3-Clause License"
            :url "https://github.com/stephengold/jolt-jni-docs/blob/master/LICENSE"}
  :main ^:skip-aot clojure.HelloJoltJni
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}}
  :target-path "target/%s"
  :url "https://github.com/stephengold/jolt-jni-docs"
)
