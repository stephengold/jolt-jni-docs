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

; HelloContactResponse application
;
; Press the E key to disable the ball's contact response. Once this happens,
; the gray (static) box no longer exerts any contact force on the ball. Gravity
; takes over, and the ball falls through.
;
; Builds upon HelloStaticBody.
;
; author: Stephen Gold sgold@sonic.net

(ns clojure.HelloContactResponse
  (:gen-class)
  (:import
   [com.github.stephengold.joltjni
    BodyCreationSettings
    BoxShape
    SphereShape]
   [com.github.stephengold.joltjni.enumerate
    EActivation
    EMotionType
    EOverrideMassProperties]
   [com.github.stephengold.sportjolt BaseApplication]
   [com.github.stephengold.sportjolt.physics
    BasePhysicsApp
    FunctionalPhysicsApp]
   [org.lwjgl.glfw GLFW]))

; variables:
(def ball) ; falling ball

; Create the PhysicsSystem. Invoked once during initialization.
(defn createSystem [app]
  ; For simplicity, use a single broadphase layer:
  (def maxBodies 2)
  (def numBpLayers 1)
  (def result (.createSystem app maxBodies numBpLayers))

  result)

; Initialize the application. Invoked once.
(defn initialize [fpa]
  (BaseApplication/setVsync true)
  (.addKeyboardListener
   fpa
   (fn [glfwKeyId isPressed]
     (if (= glfwKeyId GLFW/GLFW_KEY_E)
       (do
         (if isPressed
           (.setIsSensor ball true)) ; Disable the ball's contact response.
         true)
       false))))

; Populate the PhysicsSystem with bodies. Invoked once during initialization.
(defn populateSystem [app]
  (def physicsSystem (.getPhysicsSystem app))
  (def bi (.getBodyInterface physicsSystem))

  ; Add a static box to the system, to serve as a platform:
  (def boxHalfExtent 3.)
  (def boxShape (BoxShape. (float boxHalfExtent)))
  (def bcs1 (BodyCreationSettings.))
  (.setMotionType bcs1 EMotionType/Static)
  (.setPosition bcs1 0. -4. 0.)
  (.setShape bcs1 boxShape)
  (def box (.createBody bi bcs1))
  (.addBody bi box EActivation/DontActivate)

  ; Add a dynamic ball to the system:
  (def ballRadius 1.)
  (def ballShape (SphereShape. ballRadius))
  (def bcs2 (BodyCreationSettings.))
  (.setMass (.getMassPropertiesOverride bcs2) 2.)
  (.setAllowSleeping bcs2 false) ; Disable sleeping for clarity.
  (.setOverrideMassProperties bcs2 EOverrideMassProperties/CalculateInertia)
  (.setPosition bcs2 0. 4. 0.)
  (.setShape bcs2 ballShape)
  (def ball (.createBody bi bcs2))
  (.addBody bi ball EActivation/Activate)

  ; Visualize the shapes of both rigid bodies:
  (BasePhysicsApp/visualizeShape ball)
  (BasePhysicsApp/visualizeShape box))

(defn -main "main entry point for the HelloContactResponse application" [& arguments]
  (def fpa (FunctionalPhysicsApp.))
  (.setCreateSystem fpa createSystem)
  (.setInitialize fpa initialize)
  (.setPopulateSystem fpa populateSystem)
  (.start fpa "HelloContactResponse"))
