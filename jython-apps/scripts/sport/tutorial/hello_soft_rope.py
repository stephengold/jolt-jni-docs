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

# Import an additional Java class:
from org.joml import Vector3f


class HelloSoftRope(BasePhysicsApp):
    """
    A simple rope simulation using a soft body.

    Builds upon HelloPin.

    author:  Stephen Gold sgold@sonic.net
    """

    def createSystem(self):
        "Create the PhysicsSystem. Invoked once during initialization."

        # For simplicity, use a single broadphase layer:
        max_bodies = 1
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

        # Generate a subdivided line-segment mesh:
        num_segments = 40
        end_point1 = Vector3f(0.0, 4.0, 0.0)
        end_point2 = Vector3f(2.0, 4.0, 2.0)
        line_mesh = DividedLine(end_point1, end_point2, num_segments)

        # Create a soft body and add it to the physics system:
        sbss = self.generate_shared_settings(line_mesh)

        # Pin one of the end vertices by zeroing its inverse mass:
        vertex_index = 0
        end_vertex = sbss.getVertex(vertex_index)
        end_vertex.setInvMass(0.0)

        sbss.optimize()

        start_location = RVec3(0.0, 0.0, 0.0)
        sbcs = SoftBodyCreationSettings(
            sbss, start_location, Quat(), self.objLayerMoving
        )

        bi = self.physicsSystem.getBodyInterface()
        rope = bi.createSoftBody(sbcs)
        bi.addBody(rope, EActivation.Activate)

        # Visualize the soft-body edges and the pin:
        EdgesGeometry(rope)
        PinsGeometry(rope)

    def generate_shared_settings(self, mesh):
        """
        Generate a shared-settings object using the positions and lines in the
        specified LineList mesh.
        """
        assert mesh.topology() == Topology.LineList

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
        num_edges = indices.capacity() / Mesh.vpe
        tmp_edge = Edge()
        tmp_location2 = Vec3()
        for i in range(num_edges):
            vertex_index1 = indices.get(Mesh.vpe * i)
            tmp_edge.setVertex(0, vertex_index1)

            vertex_index2 = indices.get(Mesh.vpe * i + 1)
            tmp_edge.setVertex(1, vertex_index2)

            locations.get(3 * vertex_index1, tmp_location)
            locations.get(3 * vertex_index2, tmp_location2)
            offset = Op.minus(tmp_location2, tmp_location)
            length = offset.length()
            tmp_edge.setRestLength(length)

            result.addEdgeConstraint(tmp_edge)

        return result


application = HelloSoftRope()
application.start("HelloSoftRope")
