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

class HelloContactResponse(BasePhysicsApp):
    """
    A simple demonstration of contact response.

    Press the E key to disable the ball's contact response. Once this happens,
    the gray (static) box no longer exerts any contact force on the ball. Gravity
    takes over, and the ball falls through.

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
        self.configure_input()

    def populateSystem(self):
        "Populate the PhysicsSystem with bodies. Invoked once during initialization."

        bi = self.physicsSystem.getBodyInterface()

        # Add a static box to the system, to serve as a horizontal platform:
        box_half_extent = 3.0
        box_shape = BoxShape(box_half_extent)
        bcs1 = BodyCreationSettings()
        bcs1.setMotionType(EMotionType.Static)
        bcs1.setObjectLayer(BasePhysicsApp.objLayerNonMoving)
        bcs1.setPosition(0.0, -4.0, 0.0)
        bcs1.setShape(box_shape)
        box = bi.createBody(bcs1)
        bi.addBody(box, EActivation.DontActivate)

        # Add a dynamic ball to the system:
        ball_radius = 1.0
        ball_shape = SphereShape(ball_radius)
        bcs2 = BodyCreationSettings()
        bcs2.getMassPropertiesOverride().setMass(2.0)
        bcs2.setAllowSleeping(False)
        bcs2.setOverrideMassProperties(EOverrideMassProperties.CalculateInertia)
        bcs2.setPosition(0.0, 4.0, 0.0);
        bcs2.setShape(ball_shape)
        global BALL
        BALL = bi.createBody(bcs2)
        bi.addBody(BALL, EActivation.Activate)

        # Visualize the shapes of both rigid bodies:
        BasePhysicsApp.visualizeShape(BALL)
        BasePhysicsApp.visualizeShape(box)

    def configure_input(self):
        "Configure keyboard input during initialization."

        processor = AnonymousInputProcessor()
        self.getInputManager().add(processor)


class AnonymousInputProcessor(InputProcessor):
    "When the E key is pressed, disable the ball's contact response."

    def onKeyboard(self, glfw_key_id, is_pressed):
        if glfw_key_id == GLFW.GLFW_KEY_E:
            if is_pressed:
                # Disable the ball's contact response:
                BALL.setIsSensor(True)
            return
        self.super__onKeyboard(glfw_key_id, is_pressed)


application = HelloContactResponse()
application.start("HelloContactResponse")
