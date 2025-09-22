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


class HelloDamping(BasePhysicsApp):
    """
    A simple example illustrating the effect of damping on dynamic rigid bodies.

    Builds upon HelloRigidBody.

    author:  Stephen Gold sgold@sonic.net
    """

    def createSystem(self):
        "Create the PhysicsSystem. Invoked once during initialization."

        # For simplicity, use a single broadphase layer:
        max_bodies = 4
        num_bp_layers = 1
        result = self.super__createSystem(max_bodies, num_bp_layers)

        # For clarity, disable gravity:
        result.setGravity(0.0, 0.0, 0.0)

        return result

    def initialize(self):
        "Initialize the application. Invoked once."

        self.super__initialize()
        BaseApplication.setVsync(True)

    def populateSystem(self):
        "Populate the PhysicsSystem with bodies. Invoked once during initialization."

        bi = self.physicsSystem.getBodyInterface()

        # Create a collision shape for unit cubes:
        cube_half_extent = 0.5
        cube_shape = BoxShape(cube_half_extent)

        bcs = BodyCreationSettings().setAllowSleeping(
            False
        )  # Disable sleeping for clarity.
        bcs.setOverrideMassProperties(
            EOverrideMassProperties.CalculateInertia
        ).getMassPropertiesOverride().setMass(2)
        bcs.setObjectLayer(BasePhysicsApp.objLayerMoving).setShape(cube_shape)
        """
        Create 4 cubes (dynamic rigid bodies) and add them to the system.
        Give each cube its own set of damping parameters.
        Locate the cubes 4 meters apart, center to center.
        """
        num_cubes = 4
        cube = []

        bcs.setAngularDamping(0.0)
        bcs.setLinearDamping(0.0)
        bcs.setPosition(0.0, +2.0, 0.0)
        cube.append(bi.createBody(bcs))
        bi.addBody(cube[0], EActivation.Activate)

        bcs.setAngularDamping(0.9)
        bcs.setLinearDamping(0.0)
        bcs.setPosition(4.0, +2.0, 0.0)
        cube.append(bi.createBody(bcs))
        bi.addBody(cube[1], EActivation.Activate)

        bcs.setAngularDamping(0.0)
        bcs.setLinearDamping(0.9)
        bcs.setPosition(0.0, -2.0, 0.0)
        cube.append(bi.createBody(bcs))
        bi.addBody(cube[2], EActivation.Activate)

        bcs.setAngularDamping(0.9)
        bcs.setLinearDamping(0.9)
        bcs.setPosition(4.0, -2.0, 0.0)
        cube.append(bi.createBody(bcs))
        bi.addBody(cube[3], EActivation.Activate)

        ang_damping = cube[2].getMotionProperties().getAngularDamping()
        assert ang_damping == 0.0, "ang_damping = " + str(ang_damping)
        lin_damping = cube[2].getMotionProperties().getLinearDamping()
        assert abs(lin_damping - 0.9) < 1e-6, "lin_damping = " + str(lin_damping)
        """
        Apply an off-center impulse to each cube,
        causing it to drift and spin:
        """
        impulse = Vec3(-1.0, 0.0, 0.0)
        offset = RVec3(0.0, 1.0, 1.0)
        for cube_index in range(num_cubes):
            center = cube[cube_index].getCenterOfMassPosition()
            cube[cube_index].addImpulse(impulse, Op.plus(center, offset))

        # Visualize the shapes of all 4 cubes:
        for cube_index in range(num_cubes):
            BasePhysicsApp.visualizeShape(cube[cube_index])


application = HelloDamping()
application.start("HelloDamping")
