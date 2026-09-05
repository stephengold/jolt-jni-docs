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

; HelloSport class
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
      BodyInterface
      BroadPhaseLayerInterfaceTable
      Jolt
      ObjectLayerPairFilterTable
      ObjectVsBroadPhaseLayerFilterTable
      PhysicsSystem
      Plane
      PlaneShape
      SphereShape
      Vec3
    ]
    [com.github.stephengold.joltjni.enumerate
      EActivation
      EMotionType
    ]
    [com.github.stephengold.sportjolt BaseApplication]
    [com.github.stephengold.sportjolt.physics
      BasePhysicsApp
      FunctionalPhysicsApp
    ]
))

; fields
(def ball) ; falling rigid body

; Create the PhysicsSystem. Invoked once during initialization.
(defn createSystem [app]
  ; For simplicity, use a single broadphase layer:
  (def numBpLayers 1)

  (def ovoFilter (ObjectLayerPairFilterTable. BasePhysicsApp/numObjLayers))
  ; Enable collisions between 2 moving bodies:
  (.enableCollision ovoFilter BasePhysicsApp/objLayerMoving BasePhysicsApp/objLayerMoving)
  ; Enable collisions between a moving body and a non-moving one:
  (.enableCollision ovoFilter BasePhysicsApp/objLayerMoving BasePhysicsApp/objLayerNonMoving)
  ; Disable collisions between 2 non-moving bodies:
  (.disableCollision ovoFilter BasePhysicsApp/objLayerNonMoving BasePhysicsApp/objLayerNonMoving)

  ; Map both object layers to broadphase layer 0:
  (def layerMap (BroadPhaseLayerInterfaceTable. BasePhysicsApp/numObjLayers numBpLayers))
  (.mapObjectToBroadPhaseLayer layerMap BasePhysicsApp/objLayerMoving 0)
  (.mapObjectToBroadPhaseLayer layerMap BasePhysicsApp/objLayerNonMoving 0)

  ; Rules for colliding object layers with broadphase layers:
  (def ovbFilter (ObjectVsBroadPhaseLayerFilterTable. layerMap numBpLayers ovoFilter BasePhysicsApp/numObjLayers))

  (def result (PhysicsSystem.))

  ; Set high limits, even though this sample app uses only 2 bodies:
  (def maxBodies 5000)
  (def numBodyMutexes 0) ; 0 means "use the default number"
  (def maxBodyPairs 65536)
  (def maxContacts 20480)
  (.init result maxBodies numBodyMutexes maxBodyPairs maxContacts layerMap ovbFilter ovoFilter)
  result
)

; Initialize the application. Invoked once.
(defn initialize [app]
  (BaseApplication/setVsync true)
)

; Populate the PhysicsSystem with bodies. Invoked once during initialization.
(defn populateSystem [app]
  (println "populateSystem")
  (def physicsSystem (.getPhysicsSystem app))
  (def bi (.getBodyInterface physicsSystem))

  ; Add a static horizontal plane at y=-1:
  (def groundY -1.)
  (def normal (Vec3/sAxisY))
  (def plane (Plane. normal (- groundY)))
  (def floorShape (PlaneShape. plane))
  (def bcs (BodyCreationSettings.))
  (.setMotionType bcs EMotionType/Static)
  (.setObjectLayer bcs BasePhysicsApp/objLayerNonMoving)
  (.setShape bcs floorShape)
  (def floor (.createBody bi bcs))
  (.addBody bi floor EActivation/DontActivate)

  ; Add a sphere-shaped, dynamic, rigid body at the origin:
  (def ballRadius 0.3)
  (def ballShape (SphereShape. ballRadius))
  (.setMotionType bcs EMotionType/Dynamic)
  (.setObjectLayer bcs BasePhysicsApp/objLayerMoving)
  (.setShape bcs ballShape)
  (def ball (.createBody bi bcs))
  (.addBody bi ball EActivation/Activate)

  ; Visualize the shapes of both rigid bodies:
  (BasePhysicsApp/visualizeShape floor)
  (BasePhysicsApp/visualizeShape ball)
)

(defn -main "main entry point for the HelloSport application" [& arguments]
  (def fpa (FunctionalPhysicsApp.))
  (.setCreateSystem fpa createSystem)
  (.setInitialize fpa initialize)
  (.setPopulateSystem fpa populateSystem)
  (.start fpa "HelloSport")
  (println "done with main")
)