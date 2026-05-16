"""
 Copyright (c) 2025-2026 Stephen Gold and Yanis Boudiaf

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:

 1. Redistributions of source code must retain the above copyright notice, this
    list of conditions and the following disclaimer.

 2. Redistributions in binary form must reproduce the above copyright notice,
    this list of conditions and the following disclaimer in the documentation
    and/or other materials provided with the distribution.

 3. Neither the name of the copyright holder nor the names of its
    contributors may be used to endorse or promote products derived from
    this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
"""


class HelloSoftBody(BasePhysicsApp):
    """
    A simple example of a soft body colliding with a static rigid body.

    Builds upon HelloStaticBody.

    author:  Stephen Gold sgold@sonic.net
    """

    def createSystem(self):
        "Create the PhysicsSystem. Invoked once during initialization."

        # For simplicity, use a single broadphase layer:
        max_bodies = 2
        num_bp_layers = 1
        result = self.super__createSystem(max_bodies, num_bp_layers)

        return result

    def initialize(self):
        "Initialize the application. Invoked once."

        self.super__initialize()
        BaseApplication.setVsync(True)

        # Relocate the camera:
        BaseApplication.getCamera().setLocation(0.0, 1.0, 8.0)

    def populateSystem(self):
        "Populate the PhysicsSystem with bodies. Invoked once during initialization."

        self.add_box()

        # A mesh is used to generate the shape and topology of the soft body:
        num_refinement_iterations = 3
        indexed = True
        mesh = IcosphereMesh(num_refinement_iterations, indexed)

        # Create a soft ball and add it to the physics system:
        sbss = SoftBodySharedSettings()

        locations = mesh.getPositions()
        num_vertices = locations.capacity() / 3
        tmp_location = Vec3()
        tmp_vertex = Vertex()
        for i in range(num_vertices):
            locations.get(3 * i, tmp_location)
            tmp_vertex.setPosition(tmp_location)
            sbss.addVertex(tmp_vertex)

        indices = mesh.getIndexBuffer()
        num_faces = indices.capacity() / Mesh.vpt
        tmp_face = Face()
        for i in range(num_faces):
            for j in range(Mesh.vpt):
                index = indices.get(Mesh.vpt * i + j)
                tmp_face.setVertex(j, index)
            sbss.addFace(tmp_face)

        vertex_attributes = []
        for i in range(num_vertices):
            vertex_attributes.append(VertexAttributes())
        sbss.createConstraints(vertex_attributes, EBendType.Distance)
        sbss.optimize()

        start_location = RVec3(0.0, 3.0, 0.0)
        sbcs = SoftBodyCreationSettings(
            sbss, start_location, Quat(), BasePhysicsApp.objLayerMoving
        )

        # Configure the ball to resist deformation:
        sbcs.setPressure(30000.0)
        # default=0

        bi = self.physicsSystem.getBodyInterface()
        body = bi.createSoftBody(sbcs)
        bi.addBody(body, EActivation.Activate)

        # Visualize the soft body:
        FacesGeometry(body)
        EdgesGeometry(body)

    def add_box(self):
        "Add a large static cube to serve as a platform."

        half_extent = 3.0
        shape = BoxShape(half_extent)
        bcs = BodyCreationSettings()
        bcs.setMotionType(EMotionType.Static)
        bcs.setObjectLayer(BasePhysicsApp.objLayerNonMoving)
        bcs.setPosition(0.0, -half_extent, 0.0)
        bcs.setShape(shape)

        bi = self.physicsSystem.getBodyInterface()
        body = bi.createBody(bcs)
        bi.addBody(body, EActivation.DontActivate)

        BasePhysicsApp.visualizeShape(body)


application = HelloSoftBody()
application.start("HelloSoftBody")
