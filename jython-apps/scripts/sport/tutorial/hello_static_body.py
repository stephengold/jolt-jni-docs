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


class HelloStaticBody(BasePhysicsApp):
    """
    A simple example combining static and dynamic rigid bodies.

    Builds upon HelloRigidBody.

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

    def populateSystem(self):
        "Populate the PhysicsSystem with bodies. Invoked once during initialization."

        bi = self.physicsSystem.getBodyInterface()

        # Create a collision shape for balls:
        ball_radius = 1.0
        ball_shape = SphereShape(ball_radius)

        bcs = BodyCreationSettings()
        bcs.getMassPropertiesOverride().setMass(2.0)
        bcs.setOverrideMassProperties(EOverrideMassProperties.CalculateInertia)
        bcs.setShape(ball_shape)

        # Create a dynamic body and add it to the system:
        bcs.setPosition(0.0, 4.0, 0.0)
        dyna_ball = bi.createBody(bcs)
        bi.addBody(dyna_ball, EActivation.Activate)

        # Create a static body and add it to the system:
        bcs.setMotionType(EMotionType.Static)
        bcs.setObjectLayer(BasePhysicsApp.objLayerNonMoving)
        bcs.setPosition(0.1, 0.0, 0.0)
        stat_ball = bi.createBody(bcs)
        bi.addBody(stat_ball, EActivation.DontActivate)

        assert stat_ball.isStatic()

        # Visualize the shapes of both rigid bodies:
        BasePhysicsApp.visualizeShape(dyna_ball)
        BasePhysicsApp.visualizeShape(stat_ball)


application = HelloStaticBody()
application.start("HelloStaticBody", 0, 0, 0)
