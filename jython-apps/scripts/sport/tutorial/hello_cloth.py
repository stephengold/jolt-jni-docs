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


class HelloCloth(BasePhysicsApp):
    """
    A simple cloth simulation using a soft body.

    Builds upon HelloSoftBody.

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
        self.setVsync(True)

        # Relocate the camera:
        self.getCamera().setLocation(0.0, 1.0, 8.0)

    def populateSystem(self):
        "Populate the PhysicsSystem with bodies. Invoked once during initialization."

        self.add_ball()

        # Generate a subdivided square mesh with alternating diagonals:
        num_lines = 41
        line_spacing = 0.1
        square_grid = ClothGrid(num_lines, num_lines, line_spacing)

        # Create a compliant soft square and add it to the physics system:
        sbss = self.generate_shared_settings(square_grid)
        num_vertices = sbss.countVertices()
        vertex_attributes = []
        for i in range(num_vertices):
            vertex_attributes.append(VertexAttributes())
            # Make the cloth flexible by increasing
            # the shear compliance of its edges:
            vertex_attributes[i].setShearCompliance(2.0e-4)
        sbss.createConstraints(vertex_attributes, EBendType.Distance)
        sbss.optimize()

        start_location = RVec3(0.0, 3.0, 0.0)
        sbcs = SoftBodyCreationSettings(
            sbss, start_location, Quat(), self.objLayerMoving
        )

        bi = self.physicsSystem.getBodyInterface()
        cloth = bi.createSoftBody(sbcs)
        bi.addBody(cloth, EActivation.Activate)

        # Visualize the soft-body edges:
        EdgesGeometry(cloth)

    def add_ball(self):
        "Add a static, rigid sphere to serve as an obstacle."

        radius = 1.0
        shape = SphereShape(radius)
        bcs = BodyCreationSettings()
        bcs.setMotionType(EMotionType.Static)
        bcs.setObjectLayer(self.objLayerNonMoving)
        bcs.setShape(shape)

        bi = self.physicsSystem.getBodyInterface()
        body = bi.createBody(bcs)
        bi.addBody(body, EActivation.DontActivate)

        self.visualizeShape(body)

    def generate_shared_settings(self, mesh):
        """
        Generate a shared-settings object using the positions and faces in the
        specified TriangleList mesh.
        """
        assert mesh.topology() == Topology.TriangleList

        result = SoftBodySharedSettings()

        locations = mesh.getPositions()
        num_vertices = locations.capacity() / 3
        tmp_location = Vec3()
        tmp_vertex = Vertex()
        for i in range(num_vertices):
            locations.get(3 * i, tmp_location)
            tmp_vertex.setPosition(tmp_location)
            result.addVertex(tmp_vertex)

        indices = mesh.getIndexBuffer()
        num_faces = indices.capacity() / Mesh.vpt
        tmp_face = Face()
        for i in range(num_faces):
            for j in range(Mesh.vpt):
                index = indices.get(Mesh.vpt * i + j)
                tmp_face.setVertex(j, index)
            result.addFace(tmp_face)

        return result


application = HelloCloth()
application.start("HelloCloth")
