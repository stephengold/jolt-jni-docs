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


class HelloMotor(BasePhysicsApp, PhysicsTickListener):
    """
    A simple example of a constraint with a motor.

    Builds upon HelloLimit.

    author:  Stephen Gold sgold@sonic.net
    """

    def createSystem(self):
        "Create the PhysicsSystem. Invoked once during initialization."

        # For simplicity, use a single broadphase layer:
        max_bodies = 2
        num_bp_layers = 1
        result = self.super__createSystem(max_bodies, num_bp_layers)

        result.setGravity(0.0, 0.0, 0.0)
        return result

    def initialize(self):
        "Initialize the application. Invoked once."

        self.super__initialize()

        self.setVsync(True)
        self.configure_camera()
        self.configure_input()
        self.configure_lighting()

    def populateSystem(self):
        "Populate the PhysicsSystem with bodies and constraints. Invoked once during initialization."

        # Add a dynamic, green doorframe:
        frame_body = self.add_frame()

        # Add a dynamic, yellow box for the door:
        door_body = self.add_door()

        # Add a double-ended hinge to join the door to the frame:
        settings = SixDofConstraintSettings()
        # Fix all 3 translation DOFs:
        settings.makeFixedAxis(EAxis.TranslationX)
        settings.makeFixedAxis(EAxis.TranslationY)
        settings.makeFixedAxis(EAxis.TranslationZ)
        # Fix the X- and Z-rotation DOFs:
        settings.makeFixedAxis(EAxis.RotationX)
        settings.makeFixedAxis(EAxis.RotationZ)
        # Limit the Y-rotation DOF:
        settings.setLimitedAxis(EAxis.RotationY, 0.0, 1.2)
        pivot_location = RVec3(-1.0, 0.0, 0.0)
        settings.setPosition1(pivot_location)
        settings.setPosition2(pivot_location)
        settings.setSwingType(ESwingType.Pyramid)  # default=Cone
        # ESwingType.Cone would result in symmetrical rotation limits!
        global CONSTRAINT
        CONSTRAINT = settings.create(door_body, frame_body)
        self.physicsSystem.addConstraint(CONSTRAINT)

        # Enable the motor for Y rotation and drive it to a target velocity:
        CONSTRAINT.setMotorState(EAxis.RotationY, EMotorState.Velocity)

        ConstraintGeometry(CONSTRAINT, 1).setDepthTest(False)
        ConstraintGeometry(CONSTRAINT, 2).setDepthTest(False)

    def add_door(self):
        "Create a dynamic rigid body with a box shape and add it to the system."

        shape = BoxShape(0.8, 0.8, 0.1)

        bcs = BodyCreationSettings()
        bcs.setAllowSleeping(False)  # Disable sleep (deactivation).
        bcs.getMassPropertiesOverride().setMass(0.2)
        bcs.setOverrideMassProperties(EOverrideMassProperties.CalculateInertia)
        bcs.setShape(shape)

        bi = self.physicsSystem.getBodyInterface()
        result = bi.createBody(bcs)
        bi.addBody(result, EActivation.Activate)

        self.visualizeShape(result).setColor(Constants.YELLOW)

        return result

    def add_frame(self):
        "Create a dynamic body with a square-frame shape and add it to the system."

        half_length = 1.0
        radius = 0.1
        y_shape = CapsuleShapeSettings(half_length, radius)

        y2x = Quat.sEulerAngles(0.0, 0.0, JphMath.JPH_PI / 2.0)
        frame_settings = StaticCompoundShapeSettings()
        frame_settings.addShape(Vec3(0.0, +1.0, 0.0), y2x, y_shape)
        frame_settings.addShape(Vec3(0.0, -1.0, 0.0), y2x, y_shape)
        frame_settings.addShape(+1.0, 0.0, 0.0, y_shape)
        frame_settings.addShape(-1.0, 0.0, 0.0, y_shape)
        frame_shape = frame_settings.create().get()

        bcs = BodyCreationSettings()
        bcs.setAllowSleeping(False)  # Disable sleep (deactivation).
        bcs.getMassPropertiesOverride().setMass(1.0)
        bcs.setOverrideMassProperties(EOverrideMassProperties.CalculateInertia)
        bcs.setShape(frame_shape)

        bi = self.physicsSystem.getBodyInterface()
        result = bi.createBody(bcs)
        bi.addBody(result, EActivation.Activate)

        self.visualizeShape(result).setColor(Constants.GREEN)

        return result

    def configure_camera(self):
        "Configure the Camera and CIP during initialization."

        cip = self.getCameraInputProcessor()
        cip.setMoveSpeed(5.0)
        cip.setRotationMode(RotateMode.DragLMB)

        cam = self.getCamera()
        cam.setAzimuth(-1.56)
        cam.setLocation(0.0, 1.5, 4.0)
        cam.setUpAngle(-0.45)

    def configure_input(self):
        "Configure keyboard input during initialization."

        self.getInputManager().add(AnonymousInputProcessor())

    def configure_lighting(self):
        self.setLightDirection(7.0, 3.0, 5.0)

        # Set the background color to light blue:
        self.setBackgroundColor(Constants.SKY_BLUE)


class AnonymousInputProcessor(InputProcessor):
    "When the SPACE key is pressed, reverse the motor's direction."

    def onKeyboard(self, glfw_key_id, is_pressed):
        if glfw_key_id == GLFW.GLFW_KEY_SPACE:
            if is_pressed:  # Reverse the motor's direction:
                global CONSTRAINT
                target_velocity = CONSTRAINT.getTargetAngularVelocityCs()
                if target_velocity.length() < 0.1:  # not moving
                    target_velocity = Vec3(0.0, 1.0, 0.0)
                else:
                    target_velocity = Op.minus(target_velocity)
                CONSTRAINT.setTargetAngularVelocityCs(target_velocity)
            return
        self.super__onKeyboard(glfw_key_id, is_pressed)


application = HelloMotor()
application.start("HelloMotor")
