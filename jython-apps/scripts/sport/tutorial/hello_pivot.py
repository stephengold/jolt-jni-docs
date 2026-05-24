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

# latest X-Z plane location indicated by the mouse cursor
MOUSE_LOCATION = Vector3f()


class HelloPivot(BasePhysicsApp, PhysicsTickListener):
    """
    A simple example of a constraint that isn't centered on a body.

    Builds upon HelloConstraint.

    author:  Stephen Gold sgold@sonic.net
    """

    def createSystem(self):
        "Create the PhysicsSystem. Invoked once during initialization."

        # For simplicity, use a single broadphase layer:
        max_bodies = 2
        num_bp_layers = 1
        result = self.super__createSystem(max_bodies, num_bp_layers)

        # To enable the callbacks, register the application as a tick listener:
        self.addTickListener(self)

        # Reduce the time step for better accuracy:
        self.timePerStep = 0.005  # seconds

        return result

    def initialize(self):
        "Initialize the application. Invoked once."

        self.super__initialize()

        self.configure_camera()
        self.setLightDirection(7.0, 3.0, 5.0)

        # Disable VSync for more frequent mouse-position updates:
        self.setVsync(False)

    def populateSystem(self):
        "Populate the PhysicsSystem with bodies and constraints. Invoked once during initialization."

        # Add a mouse-controlled kinematic ball:
        global KINE_BALL
        KINE_BALL = self.add_kine_ball()

        # Add a dynamic box:
        rotor = self.add_box()

        # Constrain the box to rotate around an offset Y axis:
        settings = SixDofConstraintSettings()
        settings.makeFixedAxis(EAxis.RotationX)
        # Y-axis rotation remains free.
        settings.makeFixedAxis(EAxis.RotationZ)
        settings.makeFixedAxis(EAxis.TranslationX)
        settings.makeFixedAxis(EAxis.TranslationY)
        settings.makeFixedAxis(EAxis.TranslationZ)
        pivot_in_world = RVec3(0.0, 0.0, 2.0)
        settings.setPosition1(pivot_in_world)
        settings.setPosition2(pivot_in_world)

        fixed_to_world = Body.sFixedToWorld()
        constraint = settings.create(fixed_to_world, rotor)
        self.physicsSystem.addConstraint(constraint)

        # Visualize the constraint:
        ConstraintGeometry(constraint, 2)  # rotor is its 2nd end

    def render(self):
        "Callback invoked during each iteration of the render loop."

        screen_xy = self.getInputManager().locateCursor()
        if screen_xy != None:
            """
            Calculate the X-Z plane location (if any)
            indicated by the mouse cursor:
            """
            cam = self.getCamera()
            near_location = cam.clipToWorld(screen_xy, Projection.nearClipZ, None)
            far_location = cam.clipToWorld(screen_xy, Projection.farClipZ, None)
            near_y = near_location.y()
            far_y = far_location.y()
            if near_y > 0.0 and far_y < 0.0:
                dy = near_y - far_y
                t = near_y / dy
                Utils.lerp(t, near_location, far_location, MOUSE_LOCATION)

        self.super__render()

    def physicsTick(self, system, time_step):
        "Callback invoked (by Sport-Jolt, not by Jolt Physics) after the system has been stepped."

    def prePhysicsTick(self, system, time_step):
        "Callback invoked (by Sport-Jolt, not by Jolt Physics) before the system is stepped."

        # Relocate the kinematic ball based on the mouse location:
        body_location = Utils.toLocationVector(MOUSE_LOCATION)
        KINE_BALL.moveKinematic(body_location, Quat(), time_step)

    def add_box(self):
        "Create a dynamic rigid body with a box shape and add it to the system."

        shape = BoxShape(0.3, 1.0, 1.0)

        bcs = BodyCreationSettings()
        bcs.setAllowSleeping(False)  # Disable sleep (deactivation).
        bcs.getMassPropertiesOverride().setMass(0.2)
        bcs.setOverrideMassProperties(EOverrideMassProperties.CalculateInertia)
        bcs.setShape(shape)

        bi = self.physicsSystem.getBodyInterface()
        result = bi.createBody(bcs)
        bi.addBody(result, EActivation.Activate)

        self.visualizeShape(result)

        return result

    def add_kine_ball(self):
        "Create a kinematic body with a sphere shape and add it to the system."

        ball_radius = 1.0
        shape = SphereShape(ball_radius)

        bcs = BodyCreationSettings()
        bcs.setAllowSleeping(False)  # Disable sleep (deactivation).
        bcs.setMotionType(EMotionType.Kinematic)  # default=Dynamic
        bcs.setShape(shape)

        bi = self.physicsSystem.getBodyInterface()
        result = bi.createBody(bcs)
        bi.addBody(result, EActivation.Activate)

        self.visualizeShape(result)

        return result

    def configure_camera(self):
        "Configure the Camera and CIP during initialization."

        cip = self.getCameraInputProcessor()
        cip.setRotationMode(RotateMode.None)

        cam = self.getCamera()
        cam.setAzimuth(-1.6)
        cam.setLocation(0.0, 5.0, 10.0)
        cam.setUpAngle(-0.6)


application = HelloPivot()
application.start("HelloPivot")
