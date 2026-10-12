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

; HelloSport application
;
; Drop a dynamic sphere onto a horizontal surface and visualize them both using
; Sport-Jolt graphics.
;
; Builds upon HelloJoltJni.
;
; author: Stephen Gold sgold@sonic.net

(ns clojure.HelloSport
  (:gen-class)
  (:import
   [com.github.stephengold.joltjni
    BodyCreationSettings
    BroadPhaseLayerInterfaceTable
    ObjectLayerPairFilterTable
    ObjectVsBroadPhaseLayerFilterTable
    PhysicsSystem
    Plane
    PlaneShape
    SphereShape
    Vec3]
   [com.github.stephengold.joltjni.enumerate
    EActivation
    EMotionType]
   [com.github.stephengold.sportjolt BaseApplication]
   [com.github.stephengold.sportjolt.physics
    BasePhysicsApp
    FunctionalPhysicsApp]))

; Create the PhysicsSystem. Invoked once during initialization.
(defn createSystem [app]
  ; For simplicity, use a single broadphase layer:
  (let [numBpLayers 1
        ovoFilter (ObjectLayerPairFilterTable. BasePhysicsApp/numObjLayers)]
    (do
      ; Enable collisions between 2 moving bodies:
      (.enableCollision ovoFilter BasePhysicsApp/objLayerMoving BasePhysicsApp/objLayerMoving)
      ; Enable collisions between a moving body and a non-moving one:
      (.enableCollision ovoFilter BasePhysicsApp/objLayerMoving BasePhysicsApp/objLayerNonMoving)
      ; Disable collisions between 2 non-moving bodies:
      (.disableCollision ovoFilter BasePhysicsApp/objLayerNonMoving BasePhysicsApp/objLayerNonMoving)

      ; Map both object layers to broadphase layer 0:
      (let [layerMap (BroadPhaseLayerInterfaceTable. BasePhysicsApp/numObjLayers numBpLayers)]
        (do
          (.mapObjectToBroadPhaseLayer layerMap BasePhysicsApp/objLayerMoving 0)
          (.mapObjectToBroadPhaseLayer layerMap BasePhysicsApp/objLayerNonMoving 0)

          ; Rules for colliding object layers with broadphase layers:
          (let [ovbFilter (ObjectVsBroadPhaseLayerFilterTable. layerMap numBpLayers ovoFilter BasePhysicsApp/numObjLayers)
                result (PhysicsSystem.)

                ; Set high limits, even though this sample app uses only 2 bodies:
                maxBodies 5000
                numBodyMutexes 0 ; 0 means "use the default number"
                maxBodyPairs 65536
                maxContacts 20480]
            (.init result maxBodies numBodyMutexes maxBodyPairs maxContacts layerMap ovbFilter ovoFilter)))))))

; Initialize the application. Invoked once.
(defn initialize [app]
  (BaseApplication/setVsync true))

; Populate the PhysicsSystem with bodies. Invoked once during initialization.
(defn populateSystem [app]
  (let [physicsSystem (.getPhysicsSystem app)
        bi (.getBodyInterface physicsSystem)

        ; Add a static horizontal plane at y=-1:
        groundY -1.
        normal (Vec3/sAxisY)
        plane (Plane. normal (- groundY))
        floorShape (PlaneShape. plane)
        bcs (BodyCreationSettings.)]
    (do
      (.setMotionType bcs EMotionType/Static)
      (.setObjectLayer bcs BasePhysicsApp/objLayerNonMoving)
      (.setShape bcs floorShape)
      (let [floor (.createBody bi bcs)]
        (do
          (.addBody bi floor EActivation/DontActivate)

          ; Add a sphere-shaped, dynamic, rigid body at the origin:
          (let [ballRadius 0.3
                ballShape (SphereShape. ballRadius)]
            (do
              (.setMotionType bcs EMotionType/Dynamic)
              (.setObjectLayer bcs BasePhysicsApp/objLayerMoving)
              (.setShape bcs ballShape)
              (let [ball (.createBody bi bcs)]
                (do
                  (.addBody bi ball EActivation/Activate)

                  ; Visualize the shapes of both rigid bodies:
                  (BasePhysicsApp/visualizeShape floor)
                  (BasePhysicsApp/visualizeShape ball))))))))))

(defn -main "main entry point for the HelloSport application" [& arguments]
  (let [fpa (FunctionalPhysicsApp.)]
    (do
      (.setCreateSystem fpa createSystem)
      (.setInitialize fpa initialize)
      (.setPopulateSystem fpa populateSystem)
      (.start fpa "HelloSport"))))
