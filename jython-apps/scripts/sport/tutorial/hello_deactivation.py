"""
 Copyright (c) 2026 Stephen Gold and Yanis Boudiaf

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


class HelloDeactivation(BasePhysicsApp, PhysicsTickListener):
    """
    A simple example of rigid-body deactivation.

    Builds upon HelloStaticBody.

    author:  Stephen Gold sgold@sonic.net
    """

    def createSystem(self):
        "Create the PhysicsSystem. Invoked once during initialization."

        # For simplicity, use a single broadphase layer:
        max_bodies = 3
        num_bp_layers = 1
        result = self.super__createSystem(max_bodies, num_bp_layers)

        # To enable the callbacks, register the application as a tick listener:
        self.addTickListener(self)

        return result

    def initialize(self):
        "Initialize the application. Invoked once."

        self.super__initialize()
        BaseApplication.setVsync(True)
        self.configure_input()

    def populateSystem(self):
        "Populate the PhysicsSystem with bodies. Invoked once during initialization."

        global BI
        BI = self.physicsSystem.getBodyInterface()

        # Create a dynamic cube and add it to the system:
        box_half_extent = 0.5
        small_cube_shape = BoxShape(box_half_extent)
        bcs = BodyCreationSettings()
        bcs.getMassPropertiesOverride().setMass(2.0)
        bcs.setOverrideMassProperties(EOverrideMassProperties.CalculateInertia)
        bcs.setPosition(0.0, 4.0, 0.0)
        bcs.setShape(small_cube_shape)
        global DYNAMIC_CUBE
        DYNAMIC_CUBE = BI.createBody(bcs)
        BI.addBody(DYNAMIC_CUBE, EActivation.Activate)
        """
        Create 2 static bodies and add them to the system.
        The top body serves as a temporary support.
        """
        cube_half_extent = 1.0
        large_cube_shape = BoxShape(cube_half_extent)
        bcs.setMotionType(EMotionType.Static)
        bcs.setObjectLayer(BasePhysicsApp.objLayerNonMoving)
        bcs.setPosition(0.0, 0.0, 0.0)
        bcs.setShape(large_cube_shape)
        global SUPPORT_CUBE
        SUPPORT_CUBE = BI.createBody(bcs)
        BI.addBody(SUPPORT_CUBE, EActivation.DontActivate)

        # The bottom body serves as a visual reference point:
        ball_radius = 0.5
        ball_shape = SphereShape(ball_radius)
        bcs.setPosition(0.0, -2.0, 0.0)
        bcs.setShape(ball_shape)
        bottom_body = BI.createBody(bcs)
        BI.addBody(bottom_body, EActivation.DontActivate)

        # Visualize the shapes of all 3 bodies:
        BasePhysicsApp.visualizeShape(DYNAMIC_CUBE)
        BasePhysicsApp.visualizeShape(SUPPORT_CUBE)
        BasePhysicsApp.visualizeShape(bottom_body)

    def physicsTick(self, system, time_step):
        "Callback invoked (by Sport-Jolt, not by Jolt Physics) after the system has been stepped."

        # Once the dynamic cube gets deactivated, remove the support cube from the system:
        bi = system.getBodyInterface()
        support_id = SUPPORT_CUBE.getId()
        if bi.isAdded(support_id) and not DYNAMIC_CUBE.isActive():
            bi.removeBody(support_id)

    def prePhysicsTick(self, system, time_step):
        "Callback invoked (by Sport-Jolt, not by Jolt Physics) before the system is stepped."

        # do nothing

    def configure_input(self):
        "Configure keyboard input during initialization."

        processor = AnonymousInputProcessor()
        self.getInputManager().add(processor)


class AnonymousInputProcessor(InputProcessor):
    "When the E key is pressed, reactivate the dynamic cube."

    def onKeyboard(self, glfw_key_id, is_pressed):
        if glfw_key_id == GLFW.GLFW_KEY_E:
            if is_pressed:
                # Reactivate the dynamic cube:
                BI.activateBody(DYNAMIC_CUBE.getId())
            return
        self.super__onKeyboard(glfw_key_id, is_pressed)


application = HelloDeactivation()
application.start("HelloDeactivation")
