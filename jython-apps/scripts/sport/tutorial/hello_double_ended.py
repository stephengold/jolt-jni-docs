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

# system Y coordinate of the ground plane
GROUND_Y = -4.0

# half the height of the paddle (in meters)
PADDLE_HALF_HEIGHT = 1.0

# latest X-Z plane location indicated by the mouse cursor
MOUSE_LOCATION = Vector3f()


class HelloDoubleEnded(BasePhysicsApp, PhysicsTickListener):
    """
    A simple example of a double-ended SixDofBodyConstraint.

    Builds upon HelloPivot.

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

        # Add a static plane to represent the ground:
        self.add_plane(GROUND_Y)

        # Add a mouse-controlled kinematic paddle:
        global PADDLE_BODY
        PADDLE_BODY = self.add_box()

        # Add a dynamic ball:
        ball_body = self.add_ball()

        # Add a double-ended constraint to connect the ball to the paddle:
        pivot_in_ball = RVec3(0.0, 3.0, 0.0)
        pivot_in_paddle = RVec3(0.0, 3.0, 0.0)

        settings = SixDofConstraintSettings()
        settings.makeFixedAxis(EAxis.TranslationX)
        settings.makeFixedAxis(EAxis.TranslationY)
        settings.makeFixedAxis(EAxis.TranslationZ)
        settings.setPosition1(pivot_in_paddle)
        settings.setPosition2(pivot_in_ball)

        constraint = settings.create(PADDLE_BODY, ball_body)
        self.physicsSystem.addConstraint(constraint)

        # Visualize the constraint:
        ConstraintGeometry(constraint, 1)  # PADDLE_BODY is its 1st end
        ConstraintGeometry(constraint, 2)  # ball_body is its 2nd end

    def render(self):
        "Callback invoked during each iteration of the render loop."

        screen_xy = self.getInputManager().locateCursor()
        if screen_xy is not None:
            """
            Calculate the ground-plane location (if any)
            indicated by the mouse cursor:
            """
            cam = self.getCamera()
            near_location = cam.clipToWorld(screen_xy, Projection.nearClipZ, None)
            far_location = cam.clipToWorld(screen_xy, Projection.farClipZ, None)
            near_y = near_location.y()
            far_y = far_location.y()
            if near_y > GROUND_Y and far_y < GROUND_Y:
                dy = near_y - far_y
                t = (near_y - GROUND_Y) / dy
                Utils.lerp(t, near_location, far_location, MOUSE_LOCATION)

        self.super__render()

    def physicsTick(self, system, time_step):
        "Callback invoked (by Sport-Jolt, not by Jolt Physics) after the system has been stepped."

    def prePhysicsTick(self, system, time_step):
        "Callback invoked (by Sport-Jolt, not by Jolt Physics) before the system is stepped."

        # Relocate the kinematic paddle based on the mouse location:
        mouse = Utils.toJoltVector(MOUSE_LOCATION)
        body_location = Op.plus(mouse, RVec3(0.0, PADDLE_HALF_HEIGHT, 0.0))
        PADDLE_BODY.moveKinematic(body_location, Quat(), time_step)

    def add_box(self):
        "Create a kinematic body with a box shape and add it to the system."

        shape = BoxShape(0.3, PADDLE_HALF_HEIGHT, 1.0)

        bcs = BodyCreationSettings()
        bcs.setAllowSleeping(False)  # Disable sleep (deactivation).
        bcs.setMotionType(EMotionType.Kinematic)  # default=Dynamic
        bcs.setShape(shape)

        bi = self.physicsSystem.getBodyInterface()
        result = bi.createBody(bcs)
        bi.addBody(result, EActivation.Activate)

        self.visualizeShape(result)

        return result

    def add_ball(self):
        "Create a dynamic rigid body with a sphere shape and add it to the system."

        ball_radius = 0.4
        shape = SphereShape(ball_radius)

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

    def add_plane(self, y):
        "Add a static horizontal plane body to the system."

        plane = Plane(0.0, 1.0, 0.0, -y)
        shape = PlaneShape(plane)
        bcs = BodyCreationSettings()
        bcs.setMotionType(EMotionType.Static)
        bcs.setObjectLayer(self.objLayerNonMoving)
        bcs.setShape(shape)

        bi = self.physicsSystem.getBodyInterface()
        body = bi.createBody(bcs)
        bi.addBody(body, EActivation.DontActivate)

        # Visualize the body:
        resourceName = "/Textures/greenTile.png"
        maxAniso = 16.0
        textureKey = TextureKey("classpath://" + resourceName, maxAniso)
        geometry = self.visualizeShape(body, 0.1)
        geometry.setSpecularColor(Constants.DARK_GRAY)
        geometry.setTexture(textureKey)

    def configure_camera(self):
        "Configure the Camera and CIP during initialization."

        cip = self.getCameraInputProcessor()
        cip.setRotationMode(RotateMode.Off)

        cam = self.getCamera()
        cam.setAzimuth(-1.6)
        cam.setLocation(0.0, 5.0, 10.0)
        cam.setUpAngle(-0.6)


application = HelloDoubleEnded()
application.start("HelloDoubleEnded")
