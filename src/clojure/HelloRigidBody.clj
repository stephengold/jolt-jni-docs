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

; HelloRigidBody class
;
; A simple example of 2 colliding balls, illustrating the 5 basic features of
; responsive, dynamic, rigid bodies:
; + rigidity (fixed shape),
; + inertia (resistance to changes of motion),
; + dynamics (motion determined by forces, torques, and impulses),
; + gravity (continual downward force), and
; + contact response (avoid intersecting other bodies).
;
; Builds upon HelloSport.
;
; author: Stephen Gold sgold@sonic.net

(ns clojure.HelloRigidBody
  (:gen-class)
  (:import
    [com.github.stephengold.joltjni
      Body
      BodyCreationSettings
      BodyInterface
      PhysicsSystem
      SphereShape
    ]
    [com.github.stephengold.joltjni.enumerate
      EActivation
      EOverrideMassProperties
    ]
    [com.github.stephengold.sportjolt BaseApplication]
    [com.github.stephengold.sportjolt.physics
      BasePhysicsApp
      FunctionalPhysicsApp
    ]
))

; Create the PhysicsSystem. Invoked once during initialization.
(defn createSystem [app]
  ; For simplicity, use a single broadphase layer:
  (def maxBodies 2)
  (def numBpLayers 1)
  (def result (.createSystem app maxBodies numBpLayers))

  result
)

; Initialize the application. Invoked once.
(defn initialize [app]
  (BaseApplication/setVsync true)
)

; Populate the PhysicsSystem with bodies. Invoked once during initialization.
(defn populateSystem [app]
  (def physicsSystem (.getPhysicsSystem app))
  (def bi (.getBodyInterface physicsSystem))

  ; Create a collision shape for balls:
  (def ballRadius 1.)
  (def ballShape (SphereShape. ballRadius))

  (def bcs (BodyCreationSettings.))
  (.setMass (.getMassPropertiesOverride bcs) 2.)
  (.setOverrideMassProperties bcs EOverrideMassProperties/CalculateInertia)
  (.setShape bcs ballShape)

  ; Create 2 balls (dynamic rigid bodies) and add them to the system:
  (.setPosition bcs 1. 1. 0.)
  (def ball1 (.createBody bi bcs))
  (.addBody bi ball1 EActivation/Activate)

  (.setPosition bcs 5. 1. 0.)
  (def ball2 (.createBody bi bcs))
  (.addBody bi ball2 EActivation/Activate)

  (assert (.isDynamic ball2))
  (def actualMass (/ 1. (.getInverseMass (.getMotionProperties ball2))))

  ; Apply an impulse to ball2 to put it on a collision course:
  (.addImpulse ball2 -25. 0. 0.)

  ; Visualize the shapes of both rigid bodies:
  (BasePhysicsApp/visualizeShape ball1)
  (BasePhysicsApp/visualizeShape ball2)
)

; Advance the physics simulation by the specified amount.
; Invoked during each update.
(defn advanceAmount [app wallClockSeconds]
  ; For clarity, simulate at 1/10th normal speed:
  (def result (* 0.1 wallClockSeconds))
  result
)

(defn -main "main entry point for the HelloSport application" [& arguments]
  (def fpa (FunctionalPhysicsApp.))
  (.setCreateSystem fpa createSystem)
  (.setInitialize fpa initialize)
  (.setPopulateSystem fpa populateSystem)
  (.setAdvanceAmount fpa advanceAmount)
  (.start fpa "HelloRigidBody")
)