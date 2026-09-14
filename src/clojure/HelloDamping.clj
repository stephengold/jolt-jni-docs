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

; HelloDamping application
;
; A simple example illustrating the effect of damping on dynamic rigid bodies.
;
; Builds upon HelloRigidBody.
;
; author: Stephen Gold sgold@sonic.net

(ns clojure.HelloDamping
  (:gen-class)
  (:import
   [com.github.stephengold.joltjni
    Body
    BodyCreationSettings
    BodyInterface
    BoxShape
    PhysicsSystem
    RVec3
    Vec3]
   [com.github.stephengold.joltjni.enumerate
    EActivation
    EOverrideMassProperties]
   [com.github.stephengold.joltjni.operator Op]
   [com.github.stephengold.sportjolt BaseApplication]
   [com.github.stephengold.sportjolt.physics
    BasePhysicsApp
    FunctionalPhysicsApp])

  (:require [clojure.string :as str]))

; Create the PhysicsSystem. Invoked once during initialization.
(defn createSystem [app]
  ; For simplicity, use a single broadphase layer:
  (def maxBodies 4)
  (def numBpLayers 1)
  (def result (.createSystem app maxBodies numBpLayers))

  (.setGravity result 0. 0. 0.)

  result)

; Initialize the application. Invoked once.
(defn initialize [app]
  (BaseApplication/setVsync true))

; Populate the PhysicsSystem with bodies. Invoked once during initialization.
(defn populateSystem [app]
  (def physicsSystem (.getPhysicsSystem app))
  (def bi (.getBodyInterface physicsSystem))

  ; Create a collision shape for unit cubes:
  (def cubeHalfExtent 0.5)
  (def cubeShape (BoxShape. (float cubeHalfExtent)))

  (def bcs (BodyCreationSettings.))
  (.setAllowSleeping bcs false)
  (.setMass (.getMassPropertiesOverride bcs) 2.)
  (.setOverrideMassProperties bcs EOverrideMassProperties/CalculateInertia)
  (.setObjectLayer bcs BasePhysicsApp/objLayerMoving)
  (.setShape bcs cubeShape)

  ; Create 4 cubes (dynamic rigid bodies) and add them to the system.
  ; Give each cube its own set of damping parameters.
  ; Locate the cubes 4 meters apart, center to center.
  (def numCubes 4)
  (def cube (make-array Body numCubes))

  (.setAngularDamping bcs 0.)
  (.setLinearDamping bcs 0.)
  (.setPosition bcs 0. +2. 0.)
  (aset cube 0 (.createBody bi bcs))
  (.addBody bi (aget cube 0) EActivation/Activate)

  (.setAngularDamping bcs 0.9)
  (.setLinearDamping bcs 0.)
  (.setPosition bcs 4. +2. 0.)
  (aset cube 1 (.createBody bi bcs))
  (.addBody bi (aget cube 1) EActivation/Activate)

  (.setAngularDamping bcs 0.)
  (.setLinearDamping bcs 0.9)
  (.setPosition bcs 0. -2. 0.)
  (aset cube 2 (.createBody bi bcs))
  (.addBody bi (aget cube 2) EActivation/Activate)

  (.setAngularDamping bcs 0.9)
  (.setLinearDamping bcs 0.9)
  (.setPosition bcs 4. -2. 0.)
  (aset cube 3 (.createBody bi bcs))
  (.addBody bi (aget cube 3) EActivation/Activate)

  (def angDamping (.getAngularDamping (.getMotionProperties (aget cube 2))))
  (assert (== angDamping 0.)
          (str/join "" ["angDamping = " (String/valueOf angDamping)]))
  (def linDamping (.getLinearDamping (.getMotionProperties (aget cube 2))))
  (assert (== linDamping (float 0.9))
          (str/join "" ["linDamping = " (String/valueOf linDamping)]))

  ; Apply an off-center impulse to each cube,
  ; causing it to drift and spin:
  (def impulse (Vec3. -1. 0. 0.))
  (def offset (RVec3. 0. 1. 1.))
  (dotimes [cubeIndex numCubes]
    (let [center (.getCenterOfMassPosition (aget cube cubeIndex))]
      (.addImpulse (aget cube cubeIndex) impulse (Op/plus center offset))))

  ; Visualize the shapes of all 4 cubes:
  (dotimes [cubeIndex numCubes]
    (BasePhysicsApp/visualizeShape (aget cube cubeIndex))))

(defn -main "main entry point for the HelloDamping application" [& arguments]
  (def fpa (FunctionalPhysicsApp.))
  (.setCreateSystem fpa createSystem)
  (.setInitialize fpa initialize)
  (.setPopulateSystem fpa populateSystem)
  (.start fpa "HelloDamping"))
