; Leiningen script to build the src/clojure subproject of jolt-jni-docs

(defproject jolt-jni-clojure "0.1.0-SNAPSHOT"
  :dependencies [
    [com.github.stephengold/jolt-jni-Linux64 "4.0.1"]
    [com.github.stephengold/jolt-jni-Linux64 "4.0.1" :classifier "DebugSp"]
    [com.github.stephengold/jolt-jni-Linux_ARM32hf "4.0.1" :classifier "DebugSp"]
    [com.github.stephengold/jolt-jni-Linux_ARM64 "4.0.1" :classifier "DebugSp"]
    [com.github.stephengold/jolt-jni-MacOSX64 "4.0.1" :classifier "DebugSp"]
    [com.github.stephengold/jolt-jni-MacOSX_ARM64 "4.0.1" :classifier "DebugSp"]
    [com.github.stephengold/jolt-jni-Windows64 "4.0.1" :classifier "DebugSp"]
    [com.github.stephengold/sport-jolt "1.2.0"]
    [io.github.electrostat-lab/snaploader "1.1.1-stable"]
    [org.clojure/clojure "1.12.4"]
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
