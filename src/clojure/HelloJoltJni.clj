; Copyright (c) 2020-2025 Stephen Gold and Yanis Boudiaf
;
; Redistribution and use in source and binary forms, with or without
; modification, are permitted provided that the following conditions are met:
;
; 1. Redistributions of source code must retain the above copyright notice, this
;    list of conditions and the following disclaimer.
;
; 2. Redistributions in binary form must reproduce the above copyright notice,
;    this list of conditions and the following disclaimer in the documentation
;    and/or other materials provided with the distribution.
;
; 3. Neither the name of the copyright holder nor the names of its
;    contributors may be used to endorse or promote products derived from
;    this software without specific prior written permission.
;
; THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
; ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
; WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
; DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
; FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
; DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
; SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
; CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
; OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
; OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

; HelloJoltJni class
:
; Drop a dynamic sphere onto a horizontal surface (non-graphical illustrative example).
;
; author: Stephen Gold sgold@sonic.net

(ns clojure.HelloJoltJni
  (:gen-class)
  (:import
    [com.github.stephengold.joltjni
      BodyCreationSettings
      BodyInterface
      BroadPhaseLayerInterfaceTable
      JobSystemThreadPool
      Jolt
      JoltPhysicsObject
      ObjectLayerPairFilterTable
      ObjectVsBroadPhaseLayerFilterTable
      PhysicsSystem
      Plane
      PlaneShape
      SphereShape
      TempAllocatorMalloc
      Vec3]
    [com.github.stephengold.joltjni.enumerate
      EActivation
      EMotionType
      EPhysicsUpdateError]
    [electrostatic4j.snaploader
      LibraryInfo
      LoadingCriterion
      NativeBinaryLoader]
    [electrostatic4j.snaploader.filesystem DirectoryPath]
    [electrostatic4j.snaploader.platform NativeDynamicLibrary]
    [electrostatic4j.snaploader.platform.util PlatformPredicate]
    [java.lang Runtime]))

; constants
(def numObjLayers 2) ; number of object layers
(def objLayerMoving 0) ; object layer for moving objects
(def objLayerNonMoving 1) ; object layer for non-moving objects

; fields
(def ball) ; falling rigid body
(def physicsSystem) ; system to simulate

; Create the PhysicsSystem. Invoked once during initialization.
(defn createSystem []
  ; For simplicity, use a single broadphase layer:
  (def numBpLayers 1)

  (def ovoFilter (ObjectLayerPairFilterTable. numObjLayers))
  ; Enable collisions between 2 moving bodies:
  (.enableCollision ovoFilter objLayerMoving objLayerMoving)
  ; Enable collisions between a moving body and a non-moving one:
  (.enableCollision ovoFilter objLayerMoving objLayerNonMoving)
  ; Disable collisions between 2 non-moving bodies:
  (.disableCollision ovoFilter objLayerNonMoving objLayerNonMoving)

  ; Map both object layers to broadphase layer 0:
  (def layerMap (BroadPhaseLayerInterfaceTable. numObjLayers numBpLayers))
  (.mapObjectToBroadPhaseLayer layerMap objLayerMoving 0)
  (.mapObjectToBroadPhaseLayer layerMap objLayerNonMoving 0)

  ; Rules for colliding object layers with broadphase layers:
  (def ovbFilter (ObjectVsBroadPhaseLayerFilterTable. layerMap numBpLayers ovoFilter numObjLayers))

  (def result (PhysicsSystem.))

  ; Set high limits, even though this sample app uses only 2 bodies:
  (def maxBodies 5000)
  (def numBodyMutexes 0) ; 0 means "use the default number"
  (def maxBodyPairs 65536)
  (def maxContacts 20480)
  (.init result maxBodies numBodyMutexes maxBodyPairs maxContacts layerMap ovbFilter ovoFilter)
  result
)

; Populate the PhysicsSystem with bodies. Invoked once during initialization.
(defn populateSystem []
  (def bi (.getBodyInterface physicsSystem))

  ; Add a static horizontal plane at y=-1:
  (def groundY -1.)
  (def normal (Vec3/sAxisY))
  (def plane (Plane. normal (- groundY)))
  (def floorShape (PlaneShape. plane))
  (def bcs (BodyCreationSettings.))
  (.setMotionType bcs EMotionType/Static)
  (.setObjectLayer bcs objLayerNonMoving)
  (.setShape bcs floorShape)
  (def floor (.createBody bi bcs))
  (.addBody bi floor EActivation/DontActivate)

  ; Add a sphere-shaped, dynamic, rigid body at the origin:
  (def ballRadius 0.3)
  (def ballShape (SphereShape. ballRadius))
  (.setMotionType bcs EMotionType/Dynamic)
  (.setObjectLayer bcs objLayerMoving)
  (.setShape bcs ballShape)
  (def ball (.createBody bi bcs))
  (.addBody bi ball EActivation/Activate)
)

(defn -main "main entry point for the HelloJoltJni application" [& arguments]
  (def info (LibraryInfo. nil "joltjni" DirectoryPath/USER_DIR))
  (def loader (NativeBinaryLoader. info))
  (def libraries (into-array NativeDynamicLibrary [
        (NativeDynamicLibrary. "linux/aarch64/com/github/stephengold" PlatformPredicate/LINUX_ARM_64)
        (NativeDynamicLibrary. "linux/armhf/com/github/stephengold" PlatformPredicate/LINUX_ARM_32)
        (NativeDynamicLibrary. "linux/x86-64/com/github/stephengold" PlatformPredicate/LINUX_X86_64)
        (NativeDynamicLibrary. "osx/aarch64/com/github/stephengold" PlatformPredicate/MACOS_ARM_64)
        (NativeDynamicLibrary. "osx/x86-64/com/github/stephengold" PlatformPredicate/MACOS_X86_64)
        (NativeDynamicLibrary. "windows/x86-64/com/github/stephengold" PlatformPredicate/WIN_X86_64)]))
  (.initPlatformLibrary (.registerNativeLibraries loader libraries))
  (.loadLibrary loader LoadingCriterion/CLEAN_EXTRACTION)

  ;(Jolt/setTraceAllocations true) ; to log Jolt-JNI heap allocations
  (JoltPhysicsObject/startCleaner) ; to reclaim native memory
  (Jolt/registerDefaultAllocator) ; tell Jolt Physics to use malloc/free
  (Jolt/installDefaultAssertCallback)
  (Jolt/installDefaultTraceCallback)
  (def success (Jolt/newFactory))
  (assert success)
  (Jolt/registerTypes)

  (def physicsSystem (createSystem))
  (populateSystem)
  (.optimizeBroadPhase physicsSystem)

  (def tempAllocator (TempAllocatorMalloc.))
  (def numWorkerThreads (.availableProcessors (Runtime/getRuntime)))
  (def jobSystem (JobSystemThreadPool. Jolt/cMaxPhysicsJobs Jolt/cMaxPhysicsBarriers numWorkerThreads))
  (def timePerStep 0.02) ; seconds
  (dotimes [iteration 50] (do
      (def collisionSteps 1)
      (def errors (.update physicsSystem timePerStep collisionSteps tempAllocator jobSystem))
      (assert (= errors EPhysicsUpdateError/None))
      (def location (.getPosition ball))
      (println (.toString location)))))
