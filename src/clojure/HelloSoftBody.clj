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

; HelloSoftBody application
;
; A simple example of a soft body colliding with a static rigid body.
;
; Builds upon HelloStaticBody.
;
; author: Stephen Gold sgold@sonic.net

(ns clojure.HelloSoftBody
  (:gen-class)
  (:import
   [com.github.stephengold.joltjni
    BodyCreationSettings
    BoxShape
    Face
    Quat
    RVec3
    SoftBodySharedSettings
    SoftBodyCreationSettings
    Vec3
    Vertex
    VertexAttributes]
   [com.github.stephengold.joltjni.enumerate
    EActivation
    EBendType
    EMotionType]
   [com.github.stephengold.sportjolt
    BaseApplication
    Mesh]
   [com.github.stephengold.sportjolt.mesh IcosphereMesh]
   [com.github.stephengold.sportjolt.physics
    BasePhysicsApp
    EdgesGeometry
    FacesGeometry
    FunctionalPhysicsApp]))

; Create the PhysicsSystem. Invoked once during initialization.
(defn createSystem [app]
  ; For simplicity, use a single broadphase layer:
  (let [maxBodies 2
        numBpLayers 1]
    (.createSystem app maxBodies numBpLayers)))

; Initialize the application. Invoked once.
(defn initialize [app]
  (BaseApplication/setVsync true)

  ; Relocate the camera:
  (.setLocation (BaseApplication/getCamera) 0. 1. 8.))

; Add a large static cube to serve as a platform.
(defn addBox [bi]
  (def halfExtent 3.)
  (def shape (BoxShape. (float halfExtent)))
  (def bcs (BodyCreationSettings.))
  (.setMotionType bcs EMotionType/Static)
  (.setObjectLayer bcs BasePhysicsApp/objLayerNonMoving)
  (.setPosition bcs 0. (- halfExtent) 0.)
  (.setShape bcs shape)

  (def body (.createBody bi bcs))
  (.addBody bi body EActivation/DontActivate)

  (BasePhysicsApp/visualizeShape body))

; Populate the PhysicsSystem with bodies. Invoked once during initialization.
(defn populateSystem [app]
  (def physicsSystem (.getPhysicsSystem app))
  (def bi (.getBodyInterface physicsSystem))
  (addBox bi)

  ; A mesh is used to generate the shape and topology of the body:
  (def numRefinementIterations 3)
  (def indexed true)
  (def mesh (IcosphereMesh. numRefinementIterations indexed))

  ; Create a soft ball and add it to the physics system:
  (def sbss (SoftBodySharedSettings.))

  (def locations (.getPositions mesh))
  (def numVertices (/ (.capacity locations) 3))
  (def tmpLocation (Vec3.))
  (def tmpVertex (Vertex.))
  (dotimes [i numVertices]
    (do
      (.get locations (* 3 i) tmpLocation)
      (.setPosition tmpVertex tmpLocation)
      (.addVertex sbss tmpVertex)))

  (def indices (.getIndexBuffer mesh))
  (def numFaces (/ (.capacity indices) Mesh/vpt))
  (def tmpFace (Face.))
  (dotimes [i numFaces]
    (do
      (dotimes [j Mesh/vpt]
        (do
          (def index (.get indices (+ (* Mesh/vpt i) j)))
          (.setVertex tmpFace j index)))
      (.addFace sbss tmpFace)))

  (def vertexAttributes (make-array VertexAttributes numVertices))
  (dotimes [i numVertices]
    (do
      (aset vertexAttributes i (VertexAttributes.))))
  (.createConstraints sbss vertexAttributes EBendType/Distance)
  (.optimize sbss)

  (def startLocation (RVec3. 0. 3. 0.))
  (def sbcs (SoftBodyCreationSettings.
             sbss startLocation (Quat.) BasePhysicsApp/objLayerMoving))

  ; Configure the ball to resist deformation:
  (.setPressure sbcs 30000.) ; default=0

  (def body (.createSoftBody bi sbcs))
  (.addBody bi body EActivation/Activate)

  ; Visualize the soft body:
  (FacesGeometry. body)
  (EdgesGeometry. body))

(defn -main "main entry point for the HelloSoftBody application" [& arguments]
  (def fpa (FunctionalPhysicsApp.))
  (.setCreateSystem fpa createSystem)
  (.setInitialize fpa initialize)
  (.setPopulateSystem fpa populateSystem)
  (.start fpa "HelloSoftBody"))
