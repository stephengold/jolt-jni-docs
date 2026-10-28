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

; HelloSensor application
;
; A simple example of a sensor body with a contact listener.
;
; Press the arrow keys to walk. Press the space bar to jump.
;
; Builds upon HelloWalk.
;
; author: Stephen Gold sgold@sonic.net

(ns clojure.HelloSensor
  (:gen-class)
  (:import
   [com.github.stephengold.joltjni
    Body
    BodyCreationSettings
    CapsuleShape
    CharacterSettings
    Plane
    PlaneShape
    Quat
    RVec3
    SphereShape]
   [com.github.stephengold.joltjni.enumerate
    EActivation
    EMotionType]
   [com.github.stephengold.joltjni.operator Op]
   [com.github.stephengold.sportjolt
    BaseApplication
    Constants
    TextureKey]
   [com.github.stephengold.sportjolt.input RotateMode]
   [com.github.stephengold.sportjolt.physics
    BasePhysicsApp
    FunctionalPhysicsApp]
   [org.lwjgl.glfw GLFW]))

; variables:
(def sensor) ; sensor body for detecting intrusions
(def hadContact false) ; record contact between the sensor and a moving body
(def jumpRequested false) ; true when the space bar is pressed, otherwise false
(def walkBackward false) ; true when the DOWN key is pressed, otherwise false
(def walkForward false) ; true when the UP key is pressed, otherwise false
(def walkLeft false) ; true when the LEFT key is pressed, otherwise false
(def walkRight false) ; true when the RIGHT key is pressed, otherwise false
(def character) ; character to trigger the sensor

; Process a new contact point.
(defn addContact [system body1Va body2Va]
  (let [ghostVa (.va sensor)]
    (if (= body1Va ghostVa)
      (let [other (Body. system body2Va)]
        (if (not (.isStatic other))
          (def hadContact true)))
      (if (= body2Va ghostVa)
        (let [other (Body. system body1Va)]
          (if (not (.isStatic other))
            (def hadContact true)))))))

; Create the PhysicsSystem. Invoked once during initialization.
(defn createSystem [app]
  ; For simplicity, use a single broadphase layer:
  (let [maxBodies 3
        numBpLayers 1
        result (.createSystem app maxBodies numBpLayers)]
    (do
      ; To enable callbacks, set a simple contact listener:
      (.addContactAddedListener
       app result
       (fn [ap vaArray]
         (addContact result (nth vaArray 0) (nth vaArray 1))))

      result)))

; Configure the camera, projection, and CIP during initialization.
(defn configureCamera []
  (.setRotationMode (BaseApplication/getCameraInputProcessor) RotateMode/DragLMB)
  (let [cam (BaseApplication/getCamera)]
    (do
      (.setAzimuth cam -1.9)
      (.setLocation cam 35. 35. 60.)
      (.setUpAngle cam -0.5)))
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
  (configureLighting))

; Add a static horizontal plane body to the system.
(defn addPlane [bi y]
  (let [plane (Plane. 0. 1. 0. (- y))
        shape (PlaneShape. plane)
        bcs (BodyCreationSettings.)]
    (do
      (.setMotionType bcs EMotionType/Static)
      (.setObjectLayer bcs BasePhysicsApp/objLayerNonMoving)
      (.setShape bcs shape)

      (let [body (.createBody bi bcs)]
        (do
          (.addBody bi body EActivation/DontActivate)

          ; Visualize the body:
          (let [resourceName "/Textures/greenTile.png"
                maxAniso 16.
                textureKey (TextureKey. (str "classpath://" resourceName) maxAniso)
                geom (BasePhysicsApp/visualizeShape body 0.1)]
            (do
              (.setSpecularColor geom Constants/DARK_GRAY)
              (.setTexture geom textureKey))))))))

; Populate the PhysicsSystem with bodies. Invoked once during initialization.
(defn populateSystem [app]
  (let [physicsSystem (.getPhysicsSystem app)

        ; Create a character with a capsule shape and add it to the system:
        capsuleRadius 3. ; meters
        capsuleHeight 4. ; meters
        shape (CapsuleShape. (/ capsuleHeight 2.) capsuleRadius)
        settings (CharacterSettings.)]
    (do
      (.setShape settings shape)
      (let [startLocation (RVec3. 0. 3. 0.)
            userData 0]
        (do
          (def character (com.github.stephengold.joltjni.Character.
                          settings startLocation (Quat.) userData physicsSystem))
          (.addToPhysicsSystem character)

          ; Create a spherical sensor bubble:
          (let [sensorRadius 10.
                sensorShape (SphereShape. sensorRadius)
                bcs (BodyCreationSettings.)]
            (do
              (.setIsSensor bcs true)
              (.setMotionType bcs EMotionType/Static)
              (.setObjectLayer bcs BasePhysicsApp/objLayerNonMoving)
              (.setPosition bcs 15. 0. -13.)
              (.setShape bcs sensorShape)
              (let [bi (.getBodyInterface physicsSystem)]
                (do
                  (def sensor (.createBody bi bcs))
                  (.addBody bi sensor EActivation/DontActivate)

                  ; Visualize the character and sensor:
                  (BasePhysicsApp/visualizeShape character)
                  (BasePhysicsApp/visualizeShape sensor)

                  ; Add a plane to represent the ground:
                  (let [groundY -2.]
                    (addPlane bi groundY)))))))))))

(defn postPhysicsTick [app timeStep]
  ; Update the character:
  (let [maxSeparation 0.1] ; meters above the ground
    (.postSimulation character maxSeparation))

  (if hadContact ; Intruder detected! Pop the sensor bubble:
    (do
      (def system (.getPhysicsSystem app))
      (def bi (.getBodyInterface system))
      (def bodyId (.getId sensor))
      (.removeBody bi bodyId)
      (def hadContact false))))

(defn prePhysicsTick [app timeStep]
  (let [velocity (.getLinearVelocity character)]
    (do

      ; Clear any horizontal motion from the previous simulation step:
      (.setX velocity 0.)
      (.setZ velocity 0.)

      ; If the character is supported, make it respond to keyboard input:
      (if (.isSupported character)
        (if jumpRequested ; Cause the character to jump:
          (.setY velocity 18.)

          (let [cam (BaseApplication/getCamera)
                ; Walk as directed by the arrow keys:
                component1 (.getDirection cam)
                backward (if walkBackward 1. 0.)
                forward (if walkForward 1. 0.)]
            (do
              (.scaleInPlace component1 (- forward backward))
              (let [right (if walkRight 1. 0.)
                    left (if walkLeft 1. 0.)
                    component2 (.getRight cam)]
                (do
                  (.scaleInPlace component2 (- right left))
                  (Op/assign velocity (Op/plus component1 component2))))

              (.setY velocity 0.)
              (if (> (.length velocity) 0.)
                (let [scale (/ 7. (.length velocity))]
                  (.scaleInPlace velocity scale)))))))
      (.setLinearVelocity character velocity))))

(defn -main "main entry point for the HelloSensor application" [& arguments]
  (let [fpa (FunctionalPhysicsApp.)]
    (do
      (.setCreateSystem fpa createSystem)
      (.setInitialize fpa initialize)
      (.setPopulateSystem fpa populateSystem)
      (.setPostPhysicsTick fpa postPhysicsTick)
      (.setPrePhysicsTick fpa prePhysicsTick)
      (.start fpa "HelloSensor"))))
