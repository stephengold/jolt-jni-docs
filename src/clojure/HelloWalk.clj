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

; HelloWalk application
;
; A simple example of character physics.
;
; Press the W key to walk. Press the space bar to jump.
;
; Builds upon HelloCharacter.
;
; author: Stephen Gold sgold@sonic.net

(ns clojure.HelloWalk
  (:gen-class)
  (:import
   [com.github.stephengold.joltjni
    BodyCreationSettings
    CapsuleShape
    CharacterSettings
    HeightFieldShapeSettings
    Quat
    RVec3
    Vec3]
   [com.github.stephengold.joltjni.enumerate
    EActivation
    EMotionType]
   [com.github.stephengold.sportjolt
    BaseApplication
    Constants
    Utils]
   [com.github.stephengold.sportjolt.input RotateMode]
   [com.github.stephengold.sportjolt.physics
    BasePhysicsApp
    FunctionalPhysicsApp]
   [org.joml Vector4f]
   [org.lwjgl.glfw GLFW]))

; variables:
(def jumpRequested false) ; true when the space bar is pressed, otherwise false
(def walkRequested false) ; true when the W key is pressed, otherwise false
(def character) ; character being tested

; Create the PhysicsSystem. Invoked once during initialization.
(defn createSystem [app]
  ; For simplicity, use a single broadphase layer:
  (let [maxBodies 2
        numBpLayers 1]
    (.createSystem app maxBodies numBpLayers)))

; Configure the projection and CIP during initialization.
(defn configureCamera []
  (.setRotationMode (BaseApplication/getCameraInputProcessor) RotateMode/DragLMB)
  (.setFovyDegrees (BaseApplication/getProjection) 30.)

  ; Bring the near plane closer to reduce clipping:
  (.setZClip (BaseApplication/getProjection) 0.1 1000.))

; Configure keyboard input during initialization.
(defn configureInput [fpa]
  (.addKeyboardListener
   fpa
   (fn [glfwKeyId isPressed]
     (if (= glfwKeyId GLFW/GLFW_KEY_SPACE)
       (do
         (def jumpRequested isPressed)
         true)
       (if (= glfwKeyId GLFW/GLFW_KEY_W)
         (do
           (def walkRequested isPressed)
           ; This overrides the CameraInputProcessor.
           true)
         false)))))

; Configure lighting and the background color during initialization.
(defn configureLighting []
  (BaseApplication/setLightColor 0.3 0.3 0.3)
  (BaseApplication/setLightDirection 7. 3. 5.)

  ; Set the background color to light blue:
  (BaseApplication/setBackgroundColor Constants/SKY_BLUE))

; Initialize the application. Invoked once.
(defn initialize [fpa]
  (BaseApplication/setVsync true)
  (configureCamera)
  (configureInput fpa)
  (configureLighting))

; Add a static heightfield rigid body to the system.
(defn addTerrain [bi]
  ; Generate an array of heights from a PNG image on the classpath:
  (let [resourceName "/Textures/Terrain/splat/mountains512.png"
        image (Utils/loadResourceAsImage resourceName)

        maxHeight 51.
        heightBuffer (Utils/toHeightBuffer image maxHeight)

        ; Construct a static rigid body based on the array of heights:
        numFloats (.capacity heightBuffer)

        offset (Vec3. -256. 0. -256.)
        scale (Vec3. 1. 1. 1.)
        sampleCount 512]
    (do
      (assert (== numFloats (* sampleCount sampleCount)) numFloats)
      (let [ss (HeightFieldShapeSettings. heightBuffer offset scale sampleCount)

            shapeRef (.get (.create ss))
            bcs (BodyCreationSettings.)]
        (do
          (.setMotionType bcs EMotionType/Static)
          (.setObjectLayer bcs BasePhysicsApp/objLayerNonMoving)
          (.setShape bcs shapeRef)

          (let [result (.createBody bi bcs)]
            (do
              (.addBody bi result EActivation/DontActivate)
              result)))))))

; Populate the PhysicsSystem with bodies. Invoked once during initialization.
(defn populateSystem [app]
  (let [physicsSystem (.getPhysicsSystem app)
        bi (.getBodyInterface physicsSystem)

        ; Create a character with a capsule shape and add it to the system:
        capsuleRadius 3. ; meters
        capsuleHeight 4. ; meters
        shape (CapsuleShape. (/ capsuleHeight 2.) capsuleRadius)
        settings (CharacterSettings.)]
    (do
      (.setShape settings shape)
      (let [startLocation (RVec3. -73.6 19.09 -45.58)
            userData 0]
        (do
          (def character (com.github.stephengold.joltjni.Character.
                          settings startLocation (Quat.) userData physicsSystem))
          (.addToPhysicsSystem character)))

      ; Add a static heightmap to represent the ground:
      (let [ground (addTerrain bi)]
        (do

          ; Visualize the shapes of both physics objects:
          (BasePhysicsApp/visualizeShape character)
          (let [darkGreen (Vector4f. 0. 0.3 0. 1.)
                geom (BasePhysicsApp/visualizeShape ground)]
            (do
              (.setColor geom darkGreen)
              (.setSpecularColor geom Constants/BLACK))))))))

(defn postPhysicsTick [app timeStep]
  ; Update the character:
  (let [maxSeparation 0.1] ; meters above the ground
    (.postSimulation character maxSeparation))

  (let [location (.getPosition character)]
    (.setLocation (BaseApplication/getCamera) location)))

(defn prePhysicsTick [app timeStep]
  (let [velocity (.getLinearVelocity character)]
    (do

      ; Clear any horizontal motion from the previous simulation step:
      (.setX velocity 0.)
      (.setZ velocity 0.)

      ; If the character is supported, make it respond to keyboard input:
      (if (.isSupported character)
        (do
          (if jumpRequested
            (.setY velocity 8.))
          (if walkRequested ; Walk in the camera's forward direction:
            (let [cam (BaseApplication/getCamera)
                  forward (.getDirection cam)
                  walkSpeed 7.]
              (do
                (.setX velocity (* walkSpeed (.getX forward)))
                (.setZ velocity (* walkSpeed (.getZ forward))))))))
      (.setLinearVelocity character velocity))))

(defn -main "main entry point for the HelloWalk application" [& arguments]
  (let [fpa (FunctionalPhysicsApp.)]
    (do
      (.setCreateSystem fpa createSystem)
      (.setInitialize fpa initialize)
      (.setPopulateSystem fpa populateSystem)
      (.setPostPhysicsTick fpa postPhysicsTick)
      (.setPrePhysicsTick fpa prePhysicsTick)
      (.start fpa "HelloWalk"))))
