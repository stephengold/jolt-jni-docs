(defproject jolt-jni-clojure "0.1.0-SNAPSHOT"
  :dependencies [
    [com.github.stephengold/jolt-jni-Linux64 "2.1.0"]
    [com.github.stephengold/jolt-jni-Linux64 "2.1.0" :classifier "DebugSp"]
    [com.github.stephengold/jolt-jni-Linux_ARM32hf "2.1.0" :classifier "DebugSp"]
    [com.github.stephengold/jolt-jni-Linux_ARM64 "2.1.0" :classifier "DebugSp"]
    [com.github.stephengold/jolt-jni-MacOSX64 "2.1.0" :classifier "DebugSp"]
    [com.github.stephengold/jolt-jni-MacOSX_ARM64 "2.1.0" :classifier "DebugSp"]
    [com.github.stephengold/jolt-jni-Windows64 "2.1.0" :classifier "DebugSp"]
    [com.github.stephengold/sport-jolt "0.9.6"]
    [io.github.electrostat-lab/snaploader "1.1.1-stable"]
    [org.clojure/clojure "1.12.1"]
  ]
  :description "Sample applications (in Clojure) for the Jolt-JNI physics-simulation library"
  :license {:name "BSD 3-Clause License"
            :url "https://github.com/stephengold/jolt-jni-clojure/blob/master/LICENSE"}
  :main ^:skip-aot clojure.HelloJoltJni
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}}
  :target-path "target/%s"
  :url "https://github.com/stephengold/jolt-jni-docs"
)
