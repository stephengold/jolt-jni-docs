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


class HelloVehicle(BasePhysicsApp):
    """
    A simple example of vehicle physics.

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
        self.setVsync(True)

        self.getCameraInputProcessor().setRotationMode(RotateMode.DragLMB)
        cam = self.getCamera()
        cam.setLocation(-4.2, 6.8, 36.0)
        cam.setLookDirection(0.49, -0.36, -0.8)

        self.setBackgroundColor(Constants.SKY_BLUE)

    def populateSystem(self):
        "Populate the PhysicsSystem with bodies and constraints. Invoked once during initialization."

        # Add a static horizontal plane at y=-0.65 to represent the ground:
        ground_y = -0.65
        self.add_plane(ground_y)
        """
        Create a wedge-shaped body with a low center of gravity.
        The local forward direction is +Z.
        """
        nose_z = 1.4  # offset from body's center
        spoiler_y = 0.5  # offset from body's center
        tail_z = -0.7  # offset from body's center
        undercarriage_y = -0.1  # offset from body's center
        half_width = 0.4
        corner_locations = []
        corner_locations.append(Vec3(+half_width, undercarriage_y, nose_z))
        corner_locations.append(Vec3(-half_width, undercarriage_y, nose_z))
        corner_locations.append(Vec3(+half_width, undercarriage_y, tail_z))
        corner_locations.append(Vec3(-half_width, undercarriage_y, tail_z))
        corner_locations.append(Vec3(+half_width, spoiler_y, tail_z))
        corner_locations.append(Vec3(-half_width, spoiler_y, tail_z))
        ss = ConvexHullShapeSettings(corner_locations)
        wedge_shape = ss.create().get()

        bcs = BodyCreationSettings()
        bcs.getMassPropertiesOverride().setMass(200.0)
        bcs.setOverrideMassProperties(EOverrideMassProperties.CalculateInertia)
        bcs.setShape(wedge_shape)

        bi = self.physicsSystem.getBodyInterface()
        body = bi.createBody(bcs)
        bi.addBody(body, EActivation.Activate)

        # Configure 4 wheels, 2 in the front (for steering) and 2 in the rear:
        front_axle_z = 0.7 * nose_z  # offset from body's origin
        rear_axle_z = 0.8 * tail_z  # offset from body's origin
        x_offset = 0.9 * half_width
        wheels = []
        for i in range(4):
            wheels.append(WheelSettingsWv())
        wheels[0].setPosition(Vec3(-x_offset, 0.0, front_axle_z))  # left front
        wheels[1].setPosition(Vec3(x_offset, 0.0, front_axle_z))
        wheels[2].setPosition(Vec3(-x_offset, 0.0, rear_axle_z))
        wheels[3].setPosition(Vec3(x_offset, 0.0, rear_axle_z))  # right rear

        # The rear wheels aren't used for steering:
        wheels[2].setMaxSteerAngle(0.0)
        wheels[3].setMaxSteerAngle(0.0)
        """
        Configure a controller with a single differential,
        for rear-wheel drive:
        """
        wvcs = WheeledVehicleControllerSettings()
        wvcs.setNumDifferentials(1)
        vds = wvcs.getDifferential(0)
        vds.setLeftWheel(2)
        vds.setRightWheel(3)

        vcs = VehicleConstraintSettings()
        vcs.addWheels(wheels)
        vcs.setController(wvcs)
        vehicle = VehicleConstraint(body, vcs)
        object_layer = body.getObjectLayer()
        tester = VehicleCollisionTesterCastCylinder(object_layer)
        vehicle.setVehicleCollisionTester(tester)
        self.physicsSystem.addConstraint(vehicle)
        self.physicsSystem.addStepListener(vehicle.getStepListener())

        # Visualize the vehicle:
        self.visualizeShape(vehicle)
        self.visualizeWheels(vehicle)

        # Apply a steering angle of 6 degrees left (to both front wheels):
        right = -JphMath.degreesToRadians(6.0)

        # Apply the maximum forward acceleration:
        forward = 1.0
        brake = 0.0
        hand_brake = 0.0
        controller = vehicle.getController()
        controller.setDriverInput(forward, right, brake, hand_brake)

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


application = HelloVehicle()
application.start("HelloVehicle")
