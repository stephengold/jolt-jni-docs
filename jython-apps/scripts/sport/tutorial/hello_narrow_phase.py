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

# location of the ghost's center
GHOST_CENTER = Vec3(15.0, 0.0, -13.0)


class HelloNarrowPhase(BasePhysicsApp, PhysicsTickListener):
    """
    A simple example of a narrow-phase query.

    Press the arrow keys to walk. Press the space bar to jump.

    Builds upon HelloBroadPhase.

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

        # Initialize collector and filters:
        global COLLECTOR
        COLLECTOR = AllHitCollideShapeCollector()

        global FILTER_BP_LAYER_NO_OP
        FILTER_BP_LAYER_NO_OP = BroadPhaseLayerFilter()

        global FILTER_OBJ_LAYER_MOVING
        FILTER_OBJ_LAYER_MOVING = SpecifiedObjectLayerFilter(self.objLayerMoving)

        # Create a ghost shape:
        radius = 10.0
        ss = ConvexHullShapeSettings(
            Vec3(radius, 0.0, 0.0),
            Vec3(-radius, 0.0, 0.0),
            Vec3(0.0, radius, 0.0),
            Vec3(0.0, -radius, 0.0),
            Vec3(0.0, 0.0, radius),
            Vec3(0.0, 0.0, -radius),
        )
        global GHOST
        GHOST = ss.create().get()

        # Visualize the ghost shape:
        summary = ShapeSummary(GHOST, "low/Facet")
        mesh = self.meshForShape(GHOST, summary)
        global GHOST_GEOMETRY
        GHOST_GEOMETRY = Geometry(mesh)
        GHOST_GEOMETRY.setLocation(GHOST_CENTER)

        global JUMP_REQUESTED
        JUMP_REQUESTED = False

        global WALK_BACKWARD
        WALK_BACKWARD = False

        global WALK_FORWARD
        WALK_FORWARD = False

        global WALK_LEFT
        WALK_LEFT = False

        global WALK_RIGHT
        WALK_RIGHT = False

    def populateSystem(self):
        "Populate the PhysicsSystem. Invoked once during initialization."

        # Create a character with a capsule shape and add it to the system:
        capsule_radius = 3.0  # meters
        capsule_height = 4.0  # meters
        shape = CapsuleShape(capsule_height / 2.0, capsule_radius)

        settings = CharacterSettings()
        settings.setShape(shape)

        start_location = RVec3(0.0, 3.0, 0.0)
        user_data = 0
        global CHARACTER
        CHARACTER = Character(
            settings, start_location, Quat(), user_data, self.physicsSystem
        )
        CHARACTER.addToPhysicsSystem()

        # Visualize the character:
        self.visualizeShape(CHARACTER)

        # Add a plane to represent the ground:
        ground_y = -2.0
        self.add_plane(ground_y)

    def physicsTick(self, system, time_step):
        "Callback invoked (by Sport-Jolt, not by Jolt Physics) after the system has been stepped."

        # Update the character:
        max_separation = 0.1  # meters above the floor
        CHARACTER.postSimulation(max_separation)

        # Collect all movable bodies intersecting the ghost shape:
        query = self.physicsSystem.getNarrowPhaseQuery()
        scale = Vec3.sOne()
        transform = RMat44.sTranslation(GHOST_CENTER.toRVec3())
        settings = CollideShapeSettings()
        base = GHOST_CENTER.toRVec3()
        COLLECTOR.reset()
        query.collideShape(
            GHOST,
            scale,
            transform,
            settings,
            base,
            COLLECTOR,
            FILTER_BP_LAYER_NO_OP,
            FILTER_OBJ_LAYER_MOVING,
        )

        # Update the color of the ghost:
        num_hits = COLLECTOR.countHits()
        if num_hits > 0:
            # Intruder detected!
            GHOST_GEOMETRY.setColor(Constants.RED)
        else:
            GHOST_GEOMETRY.setColor(Constants.YELLOW)

    def prePhysicsTick(self, system, time_step):
        "Callback invoked (by Sport-Jolt, not by Jolt Physics) before the system is stepped."

        velocity = CHARACTER.getLinearVelocity()

        # Clear any horizontal motion from the previous simulation step:
        velocity.setX(0.0).setZ(0.0)

        # If the character is supported, make it respond to keyboard input:
        if CHARACTER.isSupported():
            if JUMP_REQUESTED:
                # Cause the character to jump:
                velocity.setY(18.0)

            else:
                # Walk as directed by the arrow keys:
                cam = self.getCamera()
                component1 = cam.getDirection()
                backward = 1.0 if WALK_BACKWARD else 0.0
                forward = 1.0 if WALK_FORWARD else 0.0
                component1.scaleInPlace(forward - backward)

                right = 1.0 if WALK_RIGHT else 0.0
                left = 1.0 if WALK_LEFT else 0.0
                component2 = cam.getRight()
                component2.scaleInPlace(right - left)
                Op.assign(velocity, Op.plus(component1, component2))

                velocity.setY(0.0)
                if velocity.length() > 0.0:
                    scale = 7.0 / velocity.length()
                    velocity.scaleInPlace(scale)

        CHARACTER.setLinearVelocity(velocity)

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
        "Configure the camera, projection, and CIP during initialization."

        self.getCameraInputProcessor().setRotationMode(RotateMode.DragLMB)

        cam = self.getCamera()
        cam.setAzimuth(-1.9)
        cam.setLocation(35.0, 35.0, 60.0)
        cam.setUpAngle(-0.5)

        self.getProjection().setFovyDegrees(30.0)

    def configure_input(self):
        "Configure keyboard input during initialization."

        processor = AnonymousInputProcessor()
        self.getInputManager().add(processor)

    def configure_lighting(self):
        "Configure lighting and the background color."

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

        elif glfw_key_id == GLFW.GLFW_KEY_DOWN:
            global WALK_BACKWARD
            WALK_BACKWARD = is_pressed
            return

        elif glfw_key_id == GLFW.GLFW_KEY_LEFT:
            global WALK_LEFT
            WALK_LEFT = is_pressed
            return

        elif glfw_key_id == GLFW.GLFW_KEY_RIGHT:
            global WALK_RIGHT
            WALK_RIGHT = is_pressed
            return

        elif glfw_key_id == GLFW.GLFW_KEY_UP:
            global WALK_FORWARD
            WALK_FORWARD = is_pressed
            return

        self.super__onKeyboard(glfw_key_id, is_pressed)


application = HelloNarrowPhase()
application.start("HelloNarrowPhase")
