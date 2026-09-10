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

; HelloCcd application
;
; A simple example of continuous collision detection (CCD).
;
; Builds upon HelloStaticBody.
;
; author: Stephen Gold sgold@sonic.net

(ns clojure.HelloCcd
  (:gen-class)
  (:import
   [com.github.stephengold.joltjni
    BodyCreationSettings
    BodyInterface
    CylinderShape
    PhysicsSystem
    SphereShape]
   [com.github.stephengold.joltjni.enumerate
    EActivation
    EMotionQuality
    EMotionType
    EOverrideMassProperties]
   [com.github.stephengold.sportjolt BaseApplication]
   [com.github.stephengold.sportjolt.physics
    BasePhysicsApp
    FunctionalPhysicsApp]))

; Create the PhysicsSystem. Invoked once during initialization.
(defn createSystem [app]
  ; For simplicity, use a single broadphase layer:
  (def maxBodies 3)
  (def numBpLayers 1)
  (def result (.createSystem app maxBodies numBpLayers))

  ; Increase gravity to make the balls fall faster:
  (.setGravity result 0. -100. 0.)

  result)

; Initialize the application. Invoked once.
(defn initialize [app]
  (BaseApplication/setVsync true))

; Add a thin static disc to serve as an obstacle.
(defn addDisc [bi]
  (def discRadius 2.)
  (def discThickness 0.05)
  (def discConvexRadius 0.02)
  (def discShape (CylinderShape. (/ discThickness 2.) discRadius discConvexRadius))

  (def bcs (BodyCreationSettings.))
  (.setMotionType bcs EMotionType/Static)
  (.setObjectLayer bcs BasePhysicsApp/objLayerNonMoving)
  (.setShape bcs discShape)

  (def result (.createBody bi bcs))
  (.addBody bi result EActivation/DontActivate)

  result)

; Populate the PhysicsSystem with bodies. Invoked once during initialization.
(defn populateSystem [app]
  (def physicsSystem (.getPhysicsSystem app))
  (def bi (.getBodyInterface physicsSystem))

  ; Create a collision shape for balls:
  (def ballRadius 0.1)
  (def ballShape (SphereShape. ballRadius))

  (def bcs (BodyCreationSettings.))
  (.setMass (.getMassPropertiesOverride bcs) 2.)
  (.setOverrideMassProperties bcs EOverrideMassProperties/CalculateInertia)
  (.setShape bcs ballShape)

  ; Create 2 dynamic balls, one with LinearCast CCD and one without,
  ; and add them to the system:
  (.setMotionQuality bcs EMotionQuality/LinearCast)
  (.setPosition bcs -1. 4. 0.)
  (def ccdBall (.createBody bi bcs))
  (.addBody bi ccdBall EActivation/Activate)

  (.setMotionQuality bcs EMotionQuality/Discrete)
  (.setPosition bcs 1. 4. 0.)
  (def controlBall (.createBody bi bcs))
  (.addBody bi controlBall EActivation/Activate)

  ; Verify the motion quality of each ball:
  (def ccdProperties (.getMotionProperties ccdBall))
  (assert (= (.getMotionQuality ccdProperties) EMotionQuality/LinearCast))
  (def controlProperties (.getMotionProperties controlBall))
  (assert (= (.getMotionQuality controlProperties) EMotionQuality/Discrete))

  ; Add an obstacle:
  (def disc (addDisc bi))

  ; Visualize the shapes of all 3 rigid bodies:
  (BasePhysicsApp/visualizeShape ccdBall)
  (BasePhysicsApp/visualizeShape controlBall)
  (.setProgram (BasePhysicsApp/visualizeShape disc) "Unshaded/Monochrome"))

; Advance the physics simulation by the specified amount.
; Invoked during each update.
(defn advanceAmount [app wallClockSeconds]
  ; For clarity, simulate at 1/10th normal speed:
  (def result (* 0.1 wallClockSeconds))
  result)

(defn -main "main entry point for the HelloCcd application" [& arguments]
  (def fpa (FunctionalPhysicsApp.))
  (.setCreateSystem fpa createSystem)
  (.setInitialize fpa initialize)
  (.setPopulateSystem fpa populateSystem)
  (.setAdvanceAmount fpa advanceAmount)
  (.start fpa "HelloCcd"))
