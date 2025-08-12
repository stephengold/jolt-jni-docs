"""
 Copyright (c) 2025 Stephen Gold and Yanis Boudiaf

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


class HelloSport(BasePhysicsApp):
    """
    Drop a dynamic sphere onto a horizontal surface and visualize them both using
    Sport-Jolt graphics.

    Builds upon HelloJoltJni.

    author:  Stephen Gold sgold@sonic.net
    """

    def createSystem(self):
        "Create the PhysicsSystem. Invoked once during initialization."

        # For simplicity, use a single broadphase layer:
        num_bp_layers = 1

        ovo_filter = ObjectLayerPairFilterTable(BasePhysicsApp.numObjLayers)
        # Enable collisions between 2 moving bodies:
        ovo_filter.enableCollision(
            BasePhysicsApp.objLayerMoving, BasePhysicsApp.objLayerMoving
        )
        # Enable collisions between a moving body and a non-moving one:
        ovo_filter.enableCollision(
            BasePhysicsApp.objLayerMoving, BasePhysicsApp.objLayerNonMoving
        )
        # Disable collisions between 2 non-moving bodies:
        ovo_filter.disableCollision(
            BasePhysicsApp.objLayerNonMoving, BasePhysicsApp.objLayerNonMoving
        )

        # Map both object layers to broadphase layer 0:
        layer_map = BroadPhaseLayerInterfaceTable(
            BasePhysicsApp.numObjLayers, num_bp_layers
        )
        layer_map.mapObjectToBroadPhaseLayer(BasePhysicsApp.objLayerMoving, 0)
        layer_map.mapObjectToBroadPhaseLayer(BasePhysicsApp.objLayerNonMoving, 0)

        # Rules for colliding object layers with broadphase layers:
        ovb_filter = ObjectVsBroadPhaseLayerFilterTable(
            layer_map, num_bp_layers, ovo_filter, BasePhysicsApp.numObjLayers
        )

        result = PhysicsSystem()

        # Set high limits, even though this sample app uses only 2 bodies:
        max_bodies = 5000
        num_body_mutexes = 0  # 0 means "use the default number"
        max_body_pairs = 65536
        max_contacts = 20480
        result.init(
            max_bodies,
            num_body_mutexes,
            max_body_pairs,
            max_contacts,
            layer_map,
            ovb_filter,
            ovo_filter,
        )

        return result

    def populateSystem(self):
        "Populate the PhysicsSystem with bodies. Invoked once during initialization."

        bi = self.physicsSystem.getBodyInterface()

        # Add a static horizontal plane at y=-1:
        ground_y = -1
        normal = Vec3.sAxisY()
        plane = Plane(normal, -ground_y)
        floor_shape = PlaneShape(plane)
        bcs = BodyCreationSettings()
        bcs.setMotionType(EMotionType.Static)
        bcs.setObjectLayer(BasePhysicsApp.objLayerNonMoving)
        bcs.setShape(floor_shape)
        floor = bi.createBody(bcs)
        bi.addBody(floor, EActivation.DontActivate)

        # Add a sphere-shaped, dynamic, rigid body at the origin:
        ball_radius = 0.3
        ball_shape = SphereShape(ball_radius)
        bcs.setMotionType(EMotionType.Dynamic)
        bcs.setObjectLayer(BasePhysicsApp.objLayerMoving)
        bcs.setShape(ball_shape)
        global BALL
        BALL = bi.createBody(bcs)
        bi.addBody(BALL, EActivation.Activate)

        # Visualize the shapes of both bodies:
        BasePhysicsApp.visualizeShape(floor)
        BasePhysicsApp.visualizeShape(BALL)


application = HelloSport()
application.start("HelloSport", 0, 0, 0)
