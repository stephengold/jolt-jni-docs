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


class HelloCharacter(BasePhysicsApp, PhysicsTickListener):
    """
    A simple example combining of character physics.

    Builds upon HelloStaticBody.

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
        BaseApplication.setVsync(True)
        BaseApplication.getCameraInputProcessor().setRotationMode(RotateMode.DragLMB)
        BaseApplication.setBackgroundColor(Constants.SKY_BLUE)

    def populateSystem(self):
        "Populate the PhysicsSystem with bodies. Invoked once during initialization."

        # Create a character with a capsule shape and add it to the system:
        capsule_radius = 0.5  # meters
        capsule_height = 1.0  # meters
        shape = CapsuleShape(capsule_height / 2.0, capsule_radius)

        settings = CharacterSettings()
        settings.setShape(shape)

        start_location = RVec3(0.0, 2.0, 0.0)
        user_data = 0
        global CHARACTER
        CHARACTER = Character(
            settings, start_location, Quat(), user_data, self.physicsSystem
        )
        CHARACTER.addToPhysicsSystem()

        # Add a static square to represent the ground:
        half_extent = 4.0
        y = -2.0
        ground = self.add_square(half_extent, y)

        # Visualize the shapes of both physics objects:
        BasePhysicsApp.visualizeShape(CHARACTER)
        BasePhysicsApp.visualizeShape(ground)

    def physicsTick(self, system, time_step):
        "Callback invoked (by Sport-Jolt, not by Jolt Physics) after the system has been stepped."

        # Update the character:
        max_separation = 0.1  # meters above the floor
        CHARACTER.postSimulation(max_separation)

    def prePhysicsTick(self, system, time_step):
        "Callback invoked (by Sport-Jolt, not by Jolt Physics) before the system is stepped."

        # If the character is supported, cause it to jump:
        if CHARACTER.isSupported():
            CHARACTER.setLinearVelocity(0.0, 8.0, 0.0)

    def add_square(self, half_extent, y):
        "Add a static horizontal-square rigid body to the system."

        # Create a static rigid body with a square shape:
        half_thickness = 0.1
        shape = BoxShape(half_extent, half_thickness, half_extent)
        bcs = BodyCreationSettings()
        bcs.setMotionType(EMotionType.Static)
        bcs.setObjectLayer(BasePhysicsApp.objLayerNonMoving)
        bcs.setPosition(0.0, y - half_thickness, 0.0)
        bcs.setShape(shape)

        bi = self.physicsSystem.getBodyInterface()
        result = bi.createBody(bcs)
        bi.addBody(result, EActivation.DontActivate)

        return result


application = HelloCharacter()
application.start("HelloCharacter")
