; Copyright (c) 2020-2026 Stephen Gold and Yanis Boudiaf
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

; HelloJoltJni application
;
; Drop a dynamic sphere onto a horizontal surface (non-graphical illustrative example).
;
; author: Stephen Gold sgold@sonic.net

(ns clojure.HelloJoltJni
  (:gen-class)
  (:import
   [com.github.stephengold.joltjni
    BodyCreationSettings
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

; constants:
(def numObjLayers 2) ; number of object layers
(def objLayerMoving 0) ; object layer for moving objects
(def objLayerNonMoving 1) ; object layer for non-moving objects

; variables:
(def physicsSystem) ; system to simulate

; Create the PhysicsSystem. Invoked once during initialization.
(defn createSystem []
  ; For simplicity, use a single broadphase layer:
  (let [numBpLayers 1
        ovoFilter (ObjectLayerPairFilterTable. numObjLayers)]
    (do
      ; Enable collisions between 2 moving bodies:
      (.enableCollision ovoFilter objLayerMoving objLayerMoving)
      ; Enable collisions between a moving body and a non-moving one:
      (.enableCollision ovoFilter objLayerMoving objLayerNonMoving)
      ; Disable collisions between 2 non-moving bodies:
      (.disableCollision ovoFilter objLayerNonMoving objLayerNonMoving)

      ; Map both object layers to broadphase layer 0:
      (let [layerMap (BroadPhaseLayerInterfaceTable. numObjLayers numBpLayers)]
        (do
          (.mapObjectToBroadPhaseLayer layerMap objLayerMoving 0)
          (.mapObjectToBroadPhaseLayer layerMap objLayerNonMoving 0)

          ; Rules for colliding object layers with broadphase layers:
          (let [ovbFilter (ObjectVsBroadPhaseLayerFilterTable. layerMap numBpLayers ovoFilter numObjLayers)
                result (PhysicsSystem.)]
            ; Set high limits, even though this sample app uses only 2 bodies:
            (let [maxBodies 5000
                  numBodyMutexes 0 ; 0 means "use the default number"
                  maxBodyPairs 65536
                  maxContacts 20480]
              (.init result maxBodies numBodyMutexes maxBodyPairs maxContacts layerMap ovbFilter ovoFilter))))))))

; Populate the PhysicsSystem with bodies. Invoked once during initialization.
(defn populateSystem []
  (let [bi (.getBodyInterface physicsSystem)

        ; Add a static horizontal plane at y=-1:
        groundY -1.
        normal (Vec3/sAxisY)
        plane (Plane. normal (- groundY))
        floorShape (PlaneShape. plane)
        bcs (BodyCreationSettings.)]
    (do
      (.setMotionType bcs EMotionType/Static)
      (.setObjectLayer bcs objLayerNonMoving)
      (.setShape bcs floorShape)
      (let [floor (.createBody bi bcs)]
        (.addBody bi floor EActivation/DontActivate))

      ; Add a sphere-shaped, dynamic, rigid body at the origin:
      (let [ballRadius 0.3
            ballShape (SphereShape. ballRadius)]
        (do
          (.setMotionType bcs EMotionType/Dynamic)
          (.setObjectLayer bcs objLayerMoving)
          (.setShape bcs ballShape)
          (def ball (.createBody bi bcs))
          (.addBody bi ball EActivation/Activate))))))

(defn -main "main entry point for the HelloJoltJni application" [& arguments]
  (let [info (LibraryInfo. nil "joltjni" DirectoryPath/USER_DIR)
        loader (NativeBinaryLoader. info)
        libraries (into-array NativeDynamicLibrary
                              [(NativeDynamicLibrary. "linux/aarch64/com/github/stephengold" PlatformPredicate/LINUX_ARM_64)
                               (NativeDynamicLibrary. "linux/armhf/com/github/stephengold" PlatformPredicate/LINUX_ARM_32)
                               (NativeDynamicLibrary. "linux/x86-64/com/github/stephengold" PlatformPredicate/LINUX_X86_64)
                               (NativeDynamicLibrary. "osx/aarch64/com/github/stephengold" PlatformPredicate/MACOS_ARM_64)
                               (NativeDynamicLibrary. "osx/x86-64/com/github/stephengold" PlatformPredicate/MACOS_X86_64)
                               (NativeDynamicLibrary. "windows/aarch64/com/github/stephengold" PlatformPredicate/WIN_ARM_64)
                               (NativeDynamicLibrary. "windows/x86-64/com/github/stephengold" PlatformPredicate/WIN_X86_64)])]
    (do
      (.initPlatformLibrary (.registerNativeLibraries loader libraries))
      (.loadLibrary loader LoadingCriterion/CLEAN_EXTRACTION)

      ;(Jolt/setTraceAllocations true) ; to log Jolt-JNI heap allocations
      (JoltPhysicsObject/startCleaner) ; to reclaim native memory
      (Jolt/registerDefaultAllocator) ; tell Jolt Physics to use malloc/free
      (Jolt/installDefaultAssertCallback)
      (Jolt/installDefaultTraceCallback)
      (let [success (Jolt/newFactory)]
        (assert success))
      (Jolt/registerTypes)

      (def physicsSystem (createSystem))
      (populateSystem)
      (.optimizeBroadPhase physicsSystem)

      (let [tempAllocator (TempAllocatorMalloc.)
            numWorkerThreads (.availableProcessors (Runtime/getRuntime))
            jobSystem (JobSystemThreadPool. Jolt/cMaxPhysicsJobs Jolt/cMaxPhysicsBarriers numWorkerThreads)
            timePerStep 0.02] ; seconds
        (dotimes [iteration 50]
          (do
            (let [collisionSteps 1
                  errors (.update physicsSystem timePerStep collisionSteps tempAllocator jobSystem)]
              (do
                (assert (= errors EPhysicsUpdateError/None))
                (println (.toString (.getPosition ball)))))))))))
