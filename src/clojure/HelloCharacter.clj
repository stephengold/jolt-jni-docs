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

; HelloCharacter application
;
; A simple example of character physics.
;
; Builds upon HelloStaticBody.
;
; author: Stephen Gold sgold@sonic.net

(ns clojure.HelloCharacter
  (:gen-class)
  (:import
   [com.github.stephengold.joltjni
    BodyCreationSettings
    BodyInterface
    BoxShape
    CapsuleShape
    CharacterSettings
    PhysicsSystem
    Quat
    RVec3
    Vec3]
   [com.github.stephengold.joltjni.enumerate
    EActivation
    EMotionType]
   [com.github.stephengold.sportjolt
    BaseApplication
    Constants]
   [com.github.stephengold.sportjolt.input RotateMode]
   [com.github.stephengold.sportjolt.physics
    BasePhysicsApp
    FunctionalPhysicsApp]))

; Create the PhysicsSystem. Invoked once during initialization.
(defn createSystem [app]
  ; For simplicity, use a single broadphase layer:
  (def maxBodies 2)
  (def numBpLayers 1)
  (def result (.createSystem app maxBodies numBpLayers))

  result)

; Initialize the application. Invoked once.
(defn initialize [app]
  (BaseApplication/setVsync true)
  (.setRotationMode (BaseApplication/getCameraInputProcessor) RotateMode/DragLMB)
  (BaseApplication/setBackgroundColor Constants/SKY_BLUE))

; Add a static horizontal-square rigid body to the system.
(defn addSquare [bi halfExtent y]
  ; Create a static rigid body with a square shape:
  (def halfThickness 0.1)
  (def shape (BoxShape. (Vec3. halfExtent halfThickness halfExtent)))
  (def bcs (BodyCreationSettings.))
  (.setMotionType bcs EMotionType/Static)
  (.setObjectLayer bcs BasePhysicsApp/objLayerNonMoving)
  (.setPosition bcs 0. (- y halfThickness) 0.)
  (.setShape bcs shape)

  (def result (.createBody bi bcs))
  (.addBody bi result EActivation/DontActivate)

  result)

; Populate the PhysicsSystem with bodies. Invoked once during initialization.
(defn populateSystem [app]
  (def physicsSystem (.getPhysicsSystem app))
  (def bi (.getBodyInterface physicsSystem))

  ; Create a character with a capsule shape and add it to the system:
  (def capsuleRadius 0.5) ; meters
  (def capsuleHeight 1.) ; meters
  (def shape (CapsuleShape. (/ capsuleHeight 2.) capsuleRadius))

  (def settings (CharacterSettings.))
  (.setShape settings shape)

  (def startLocation (RVec3. 0. 2. 0.))
  (def userData 0)
  (def character (com.github.stephengold.joltjni.Character. settings startLocation (Quat.) userData physicsSystem))
  (.addToPhysicsSystem character)

  ; Add a static square to represent the ground:
  (def halfExtent 4.)
  (def y -2.)
  (def ground (addSquare bi halfExtent y))

  ; Visualize the shapes of both physics objects:
  (BasePhysicsApp/visualizeShape character)
  (BasePhysicsApp/visualizeShape ground))

(defn postPhysicsTick [app timeStep]
  ; Update the character:
  (def maxSeparation 0.1) ; meters above the floor
  (.postSimulation character maxSeparation))

(defn prePhysicsTick [app timeStep]
  ; If the character is supported, cause it to jump:
  (if (.isSupported character)
    (.setLinearVelocity character 0. 8. 0.)))

(defn -main "main entry point for the HelloCharacter application" [& arguments]
  (def fpa (FunctionalPhysicsApp.))
  (.setCreateSystem fpa createSystem)
  (.setInitialize fpa initialize)
  (.setPopulateSystem fpa populateSystem)
  (.setPostPhysicsTick fpa postPhysicsTick)
  (.setPrePhysicsTick fpa prePhysicsTick)
  (.start fpa "HelloCharacter"))
