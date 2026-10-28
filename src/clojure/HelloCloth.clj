; Copyright (c) 2019-2026 Stephen Gold and Yanis Boudiaf
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

; HelloCloth application
;
; A simple cloth simulation using a soft body.
;
; Builds upon HelloSoftBody.
;
; author: Stephen Gold sgold@sonic.net

(ns clojure.HelloCloth
  (:gen-class)
  (:import
   [com.github.stephengold.joltjni
    BodyCreationSettings
    Face
    Quat
    RVec3
    SoftBodySharedSettings
    SoftBodyCreationSettings
    SphereShape
    Vec3
    Vertex
    VertexAttributes]
   [com.github.stephengold.joltjni.enumerate
    EActivation
    EBendType
    EMotionType]
   [com.github.stephengold.sportjolt
    BaseApplication
    Mesh
    Topology]
   [com.github.stephengold.sportjolt.mesh ClothGrid]
   [com.github.stephengold.sportjolt.physics
    BasePhysicsApp
    EdgesGeometry
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

; Add a static, rigid sphere to serve as an obstacle.
(defn addBall [bi]
  (let [bcs (BodyCreationSettings.)]
    (do
      (.setMotionType bcs EMotionType/Static)
      (.setObjectLayer bcs BasePhysicsApp/objLayerNonMoving)
      (.setShape bcs (SphereShape. 1.))

      (let [body (.createBody bi bcs)]
        (do
          (.addBody bi body EActivation/DontActivate)
          (BasePhysicsApp/visualizeShape body))))))

; Generate a shared-settings object using the positions and faces in the
; specified TriangleList mesh.
(defn generateSharedSettings [mesh]
  (assert (= (.topology mesh) Topology/TriangleList))

  (let [result (SoftBodySharedSettings.)
        locations (.getPositions mesh)
        numVertices (/ (.capacity locations) 3)
        tmpLocation (Vec3.)
        tmpVertex (Vertex.)]
    (do
      (dotimes [i numVertices]
        (do
          (.get locations (* 3 i) tmpLocation)
          (.setPosition tmpVertex tmpLocation)
          (.addVertex result tmpVertex)))

      (let [indices (.getIndexBuffer mesh)
            numFaces (/ (.capacity indices) Mesh/vpt)
            tmpFace (Face.)]
        (dotimes [i numFaces]
          (do
            (dotimes [j Mesh/vpt]
              (let [index (.get indices (+ (* Mesh/vpt i) j))]
                (.setVertex tmpFace j index)))
            (.addFace result tmpFace))))

      result)))

; Populate the PhysicsSystem with bodies. Invoked once during initialization.
(defn populateSystem [app]
  (let [physicsSystem (.getPhysicsSystem app)
        bi (.getBodyInterface physicsSystem)]
    (do
      (addBall bi)

  ; Generate a subdivided square mesh with alternating diagonals:
      (let [numLines 41
            lineSpacing 0.1
            squareGrid (ClothGrid. numLines numLines lineSpacing)

       ; Create a compliant soft square and add it to the physics system:
            sbss (generateSharedSettings squareGrid)

            locations (.getPositions squareGrid)
            numVertices (.countVertices sbss)
            vertexAttributes (make-array VertexAttributes numVertices)]
        (dotimes [i numVertices]
          (do
            (aset vertexAttributes i (VertexAttributes.)))

      ; Make the cloth flexible by increasing the shear compliance of its edges:
          (.setShearCompliance (aget vertexAttributes i) 0.0002))
        (.createConstraints sbss vertexAttributes EBendType/Distance)
        (.optimize sbss)

        (let [startLocation (RVec3. 0. 3. 0.)
              sbcs (SoftBodyCreationSettings.
                    sbss startLocation (Quat.) BasePhysicsApp/objLayerMoving)

              cloth (.createSoftBody bi sbcs)]
          (do
            (.addBody bi cloth EActivation/Activate)

  ; Visualize the soft-body edges:
            (EdgesGeometry. cloth)))))))

(defn -main "main entry point for the HelloCloth application" [& arguments]
  (let [fpa (FunctionalPhysicsApp.)]
    (do
      (.setCreateSystem fpa createSystem)
      (.setInitialize fpa initialize)
      (.setPopulateSystem fpa populateSystem)
      (.start fpa "HelloCloth"))))
