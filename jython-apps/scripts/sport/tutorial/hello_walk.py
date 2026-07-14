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

from org.joml import Vector4f


class HelloWalk(BasePhysicsApp, PhysicsTickListener):
    """
    A simple example of character physics.

    Press the W key to walk. Press the space bar to jump.

    Builds upon HelloCharacter.

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

        return result

    def initialize(self):
        "Initialize the application. Invoked once."

        self.super__initialize()
        self.setVsync(True)
        self.configure_camera()
        self.configure_input()
        self.configure_lighting()

        global JUMP_REQUESTED
        JUMP_REQUESTED = False

        global WALK_REQUESTED
        WALK_REQUESTED = False

    def populateSystem(self):
        "Populate the PhysicsSystem. Invoked once during initialization."

        # Create a character with a capsule shape and add it to the system:
        capsule_radius = 3.0  # meters
        capsule_height = 4.0  # meters
        shape = CapsuleShape(capsule_height / 2.0, capsule_radius)

        settings = CharacterSettings()
        settings.setShape(shape)

        start_location = RVec3(-73.6, 19.09, -45.58)
        user_data = 0
        global CHARACTER
        CHARACTER = Character(
            settings, start_location, Quat(), user_data, self.physicsSystem
        )
        CHARACTER.addToPhysicsSystem()

        # Add a static heightmap to represent the ground:
        ground = self.add_terrain()

        # Visualize the shapes of both physics objects:
        self.visualizeShape(CHARACTER)
        dark_green = Vector4f(0.0, 0.3, 0.0, 1.0)
        geometry = self.visualizeShape(ground)
        geometry.setColor(dark_green)
        geometry.setSpecularColor(Constants.BLACK)

    def physicsTick(self, system, time_step):
        "Callback invoked (by Sport-Jolt, not by Jolt Physics) after the system has been stepped."

        # Update the character:
        max_separation = 0.1  # meters above the floor
        CHARACTER.postSimulation(max_separation)

        location = CHARACTER.getPosition()
        self.getCamera().setLocation(location)

    def prePhysicsTick(self, system, time_step):
        "Callback invoked (by Sport-Jolt, not by Jolt Physics) before the system is stepped."

        velocity = CHARACTER.getLinearVelocity()

        # Clear any horizontal motion from the previous simulation step:
        velocity.setX(0.0).setZ(0.0)

        # If the character is supported, make it respond to keyboard input:
        if CHARACTER.isSupported():
            if JUMP_REQUESTED:
                # Cause the character to jump:
                velocity.setY(8.0)

            elif WALK_REQUESTED:
                # Walk in the camera's forward direction:
                forward = self.getCamera().getDirection()
                walk_speed = 7.0
                velocity.setX(walk_speed * forward.getX())
                velocity.setZ(walk_speed * forward.getZ())

        CHARACTER.setLinearVelocity(velocity)

    def add_terrain(self):
        "Add a static heightfield rigid body to the system."

        # Generate an array of heights from a PNG image on the classpath:
        resource_name = "/Textures/Terrain/splat/mountains512.png"
        image = Utils.loadResourceAsImage(resource_name)

        max_height = 51.0
        height_buffer = Utils.toHeightBuffer(image, max_height)

        # Construct a static rigid body based on the array of heights:
        num_floats = height_buffer.capacity()

        offset = Vec3(-256.0, 0.0, -256.0)
        scale = Vec3(1.0, 1.0, 1.0)
        sample_count = 512
        assert num_floats == sample_count * sample_count
        ss = HeightFieldShapeSettings(height_buffer, offset, scale, sample_count)

        shape_ref = ss.create().get()
        bcs = BodyCreationSettings()
        bcs.setMotionType(EMotionType.Static)
        bcs.setObjectLayer(self.objLayerNonMoving)
        bcs.setShape(shape_ref)

        bi = self.physicsSystem.getBodyInterface()
        result = bi.createBody(bcs)
        bi.addBody(result, EActivation.DontActivate)

        return result

    def configure_camera(self):
        "Configure the projection and CIP during initialization."

        self.getCameraInputProcessor().setRotationMode(RotateMode.DragLMB)
        self.getProjection().setFovyDegrees(30.0)

        # Bring the near plane closer to reduce clipping:
        self.getProjection().setZClip(0.1, 1000.0)

    def configure_input(self):
        "Configure keyboard input during initialization."

        processor = AnonymousInputProcessor()
        self.getInputManager().add(processor)

    def configure_lighting(self):
        "Configure lighting and the background color."

        self.setLightColor(0.3, 0.3, 0.3)
        self.setLightDirection(7.0, 3.0, 5.0)

        # Set the background color to light blue:
        self.setBackgroundColor(Constants.SKY_BLUE)


class AnonymousInputProcessor(InputProcessor):
    "When the SPACE key is pressed, jump.  When the W key is pressed, walk forward."

    def onKeyboard(self, glfw_key_id, is_pressed):
        if glfw_key_id == GLFW.GLFW_KEY_SPACE:
            global JUMP_REQUESTED
            JUMP_REQUESTED = is_pressed
            return

        elif glfw_key_id == GLFW.GLFW_KEY_W:
            global WALK_REQUESTED
            WALK_REQUESTED = is_pressed
            # This overrides the CameraInputProcessor.
            return

        self.super__onKeyboard(glfw_key_id, is_pressed)


application = HelloWalk()
application.start("HelloWalk")
