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

; HelloBroadPhase application
;
; A simple example of a broadphase query.
;
; Press the arrow keys to walk. Press the space bar to jump.
;
; Builds upon HelloSensor.
;
; author: Stephen Gold sgold@sonic.net

(ns clojure.HelloBroadPhase
  (:gen-class)
  (:import
   [com.github.stephengold.joltjni
    AaBox
    AllHitCollideShapeBodyCollector
    BodyCreationSettings
    BroadPhaseLayerFilter
    CapsuleShape
    CharacterSettings
    Plane
    PlaneShape
    Quat
    RVec3
    SpecifiedObjectLayerFilter
    Vec3]
   [com.github.stephengold.joltjni.enumerate
    EActivation
    EMotionType]
   [com.github.stephengold.joltjni.operator Op]
   [com.github.stephengold.sportjolt
    BaseApplication
    Constants
    Geometry
    TextureKey]
   [com.github.stephengold.sportjolt.input RotateMode]
   [com.github.stephengold.sportjolt.mesh BoxMesh]
   [com.github.stephengold.sportjolt.physics
    AabbGeometry
    BasePhysicsApp
    FunctionalPhysicsApp]
   [org.lwjgl.glfw GLFW]))

; variables:
(def collector) ; reusable hit collector
(def jumpRequested false) ; true when the space bar is pressed, otherwise false
(def walkBackward false) ; true when the DOWN key is pressed, otherwise false
(def walkForward false) ; true when the UP key is pressed, otherwise false
(def walkLeft false) ; true when the LEFT key is pressed, otherwise false
(def walkRight false) ; true when the RIGHT key is pressed, otherwise false
(def filterBpLayerNoOp) ; broadphase-layer filter that has no effect
(def character) ; character to trigger the sensor
(def ghost) ; ghost box for detecting intrusions
(def ghostGeometry) ; visualize the ghost box
(def filterObjLayerMoving) ; object-layer filter that accepts only moving bodies

; Create the PhysicsSystem. Invoked once during initialization.
(defn createSystem [app]
  ; For simplicity, use a single broadphase layer:
  (def maxBodies 2)
  (def numBpLayers 1)
  (def result (.createSystem app maxBodies numBpLayers))

  result)

; Configure the projection and CIP during initialization.
(defn configureCamera []
  (.setRotationMode (BaseApplication/getCameraInputProcessor) RotateMode/DragLMB)
  (def cam (BaseApplication/getCamera))
  (.setAzimuth cam -1.9)
  (.setLocation cam 35. 35. 60.)
  (.setUpAngle cam -0.5)
  (.setFovyDegrees (BaseApplication/getProjection) 30.))

; Configure keyboard input during initialization.
(defn configureInput [fpa]
  (.addKeyboardListener
   fpa
   (fn [glfwKeyId isPressed]
     (if (= glfwKeyId GLFW/GLFW_KEY_SPACE)
       (do
         (def jumpRequested isPressed)
         true)

       (if (= glfwKeyId GLFW/GLFW_KEY_DOWN)
         (do
           (def walkBackward isPressed)
           true)
         (if (= glfwKeyId GLFW/GLFW_KEY_LEFT)
           (do
             (def walkLeft isPressed)
             true)
           (if (= glfwKeyId GLFW/GLFW_KEY_RIGHT)
             (do
               (def walkRight isPressed)
               true)
             (if (= glfwKeyId GLFW/GLFW_KEY_UP)
               (do
                 (def walkForward isPressed)
                 true)
               false))))))))

; Configure lighting and the background color during initialization.
(defn configureLighting []
  (BaseApplication/setLightDirection 7. 3. 5.)

  ; Set the background color to light blue:
  (BaseApplication/setBackgroundColor Constants/SKY_BLUE))

; Initialize the application. Invoked once.
(defn initialize [fpa]
  (BaseApplication/setVsync true)
  (configureCamera)
  (configureInput fpa)
  (configureLighting)

  ; Initialize collector and filters:
  (def collector (AllHitCollideShapeBodyCollector.))
  (def filterBpLayerNoOp (BroadPhaseLayerFilter.))
  (def filterObjLayerMoving (SpecifiedObjectLayerFilter. BasePhysicsApp/objLayerMoving))

  ; Create a ghost box:
  (def center (Vec3. 15. 0. -13.))
  (def radius 10.)
  (def ghost (AaBox. center (float radius)))

  ; Visualize the ghost box:
  (def boxMesh (BoxMesh/getMesh))
  (def ghostGeometry (Geometry. boxMesh))
  (.setLocation ghostGeometry center)
  (.setScale ghostGeometry (float radius)))

; Add a static horizontal plane body to the system.
(defn addPlane [bi y]
  (def plane (Plane. 0. 1. 0. (- y)))
  (def shape (PlaneShape. plane))
  (def bcs (BodyCreationSettings.))
  (.setMotionType bcs EMotionType/Static)
  (.setObjectLayer bcs BasePhysicsApp/objLayerNonMoving)
  (.setShape bcs shape)

  (def body (.createBody bi bcs))
  (.addBody bi body EActivation/DontActivate)

  ; Visualize the body:
  (def resourceName "/Textures/greenTile.png")
  (def maxAniso 16.)
  (def textureKey (TextureKey. (str "classpath://" resourceName) maxAniso))
  (def geom (BasePhysicsApp/visualizeShape body 0.1))
  (.setSpecularColor geom Constants/DARK_GRAY)
  (.setTexture geom textureKey))

; Populate the PhysicsSystem with bodies. Invoked once during initialization.
(defn populateSystem [app]
  ; Create a character with a capsule shape and add it to the system:
  (def capsuleRadius 3.) ; meters
  (def capsuleHeight 4.) ; meters
  (def shape (CapsuleShape. (/ capsuleHeight 2.) capsuleRadius))

  (def settings (CharacterSettings.))
  (.setShape settings shape)

  (def startLocation (RVec3. 0. 3. 0.))
  (def userData 0)
  (def physicsSystem (.getPhysicsSystem app))
  (def character (com.github.stephengold.joltjni.Character.
                  settings startLocation (Quat.) userData physicsSystem))
  (.addToPhysicsSystem character)

  ; Visualize the character and sensor:
  (BasePhysicsApp/visualizeShape character)
  (AabbGeometry. character) ; outline the character's bounding box in white

  ; Add a plane to represent the ground:
  (def bi (.getBodyInterface physicsSystem))
  (def groundY -2.)
  (addPlane bi groundY))

(defn postPhysicsTick [app timeStep]
  ; Update the character:
  (def maxSeparation 0.1) ; meters above the ground
  (.postSimulation character maxSeparation)

  ; Collect all movable bodies with AABBs overlapping the ghost box:
  (def system (.getPhysicsSystem app))
  (def query (.getBroadPhaseQuery system))
  (.reset collector)
  (.collideAaBox query ghost collector filterBpLayerNoOp filterObjLayerMoving)

  ; Update the color of the ghost:
  (def numHits (.countHits collector))
  (if (> numHits 0) ; Intruder detected!
    (.setColor ghostGeometry Constants/RED)
    (.setColor ghostGeometry Constants/YELLOW)))

(defn prePhysicsTick [app timeStep]
  (def velocity (.getLinearVelocity character))

  ; Clear any horizontal motion from the previous simulation step:
  (.setX velocity 0.)
  (.setZ velocity 0.)

  ; If the character is supported, make it respond to keyboard input:
  (if (.isSupported character)
    (do
      (if jumpRequested ; Cause the character to jump:
        (.setY velocity 18.)
        (do
          (def cam (BaseApplication/getCamera))

          ; Walk as directed by the arrow keys:
          (def component1 (.getDirection cam))
          (def backward (if walkBackward 1. 0.))
          (def forward (if walkForward 1. 0.))
          (.scaleInPlace component1 (- forward backward))

          (def right (if walkRight 1. 0.))
          (def left (if walkLeft 1. 0.))
          (def component2 (.getRight cam))
          (.scaleInPlace component2 (- right left))
          (Op/assign velocity (Op/plus component1 component2))

          (.setY velocity 0.)
          (if (> (.length velocity) 0.)
            (do
              (def scale (/ 7. (.length velocity)))
              (.scaleInPlace velocity scale)))))))
  (.setLinearVelocity character velocity))

(defn -main "main entry point for the HelloBroadPhase application" [& arguments]
  (def fpa (FunctionalPhysicsApp.))
  (.setCreateSystem fpa createSystem)
  (.setInitialize fpa initialize)
  (.setPopulateSystem fpa populateSystem)
  (.setPostPhysicsTick fpa postPhysicsTick)
  (.setPrePhysicsTick fpa prePhysicsTick)
  (.start fpa "HelloBroadPhase"))
