/*
 Copyright (c) 2020-2025 Stephen Gold and Yanis Boudiaf

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
 */
package com.github.stephengold.sportjolt.javaapp.sample;

import com.github.stephengold.joltjni.Body;
import com.github.stephengold.joltjni.BodyCreationSettings;
import com.github.stephengold.joltjni.BodyInterface;
import com.github.stephengold.joltjni.ConvexHullShapeSettings;
import com.github.stephengold.joltjni.Jolt;
import com.github.stephengold.joltjni.PhysicsSystem;
import com.github.stephengold.joltjni.Plane;
import com.github.stephengold.joltjni.PlaneShape;
import com.github.stephengold.joltjni.ShapeRefC;
import com.github.stephengold.joltjni.ShapeSettings;
import com.github.stephengold.joltjni.Vec3;
import com.github.stephengold.joltjni.VehicleCollisionTester;
import com.github.stephengold.joltjni.VehicleCollisionTesterRay;
import com.github.stephengold.joltjni.VehicleConstraint;
import com.github.stephengold.joltjni.VehicleConstraintSettings;
import com.github.stephengold.joltjni.VehicleDifferentialSettings;
import com.github.stephengold.joltjni.WheelSettingsWv;
import com.github.stephengold.joltjni.WheeledVehicleController;
import com.github.stephengold.joltjni.WheeledVehicleControllerSettings;
import com.github.stephengold.joltjni.enumerate.EActivation;
import com.github.stephengold.joltjni.enumerate.EMotionType;
import com.github.stephengold.joltjni.enumerate.EOverrideMassProperties;
import com.github.stephengold.joltjni.readonly.ConstPlane;
import com.github.stephengold.joltjni.readonly.ConstShape;
import com.github.stephengold.joltjni.readonly.Vec3Arg;
import com.github.stephengold.sportjolt.Constants;
import com.github.stephengold.sportjolt.TextureKey;
import com.github.stephengold.sportjolt.input.RotateMode;
import com.github.stephengold.sportjolt.physics.BasePhysicsApp;
import java.util.ArrayList;
import java.util.List;
import org.joml.Vector3f;

/**
 * A simple example of vehicle physics.
 * <p>
 * Builds upon HelloStaticBody.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class HelloVehicle extends BasePhysicsApp {
    // *************************************************************************
    // constructors

    /**
     * Instantiate the HelloVehicle application.
     * <p>
     * This no-arg constructor was made explicit to avoid javadoc warnings from
     * JDK 18+.
     */
    public HelloVehicle() {
        // do nothing
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Main entry point for the HelloVehicle application.
     *
     * @param arguments array of command-line arguments (not null)
     */
    public static void main(String[] arguments) {
        HelloVehicle application = new HelloVehicle();
        application.start();
    }
    // *************************************************************************
    // BasePhysicsApp methods

    /**
     * Create the PhysicsSystem. Invoked once during initialization.
     *
     * @return a new instance
     */
    @Override
    protected PhysicsSystem createSystem() {
        // For simplicity, use a single broadphase layer:
        int numBpLayers = 1;
        int maxBodies = 3; // TODO 2
        PhysicsSystem result = createSystem(maxBodies, numBpLayers);

        return result;
    }

    /**
     * Initialize the application. Invoked once.
     */
    @Override
    protected void initialize() {
        super.initialize();

        getCameraInputProcessor().setRotationMode(RotateMode.DragLMB);
        cam.setLocation(new Vector3f(-4.17558f, 6.82825f, 35.9108f))
                .setLookDirection(new Vector3f(0.49f, -0.36f, -0.80f));

        setBackgroundColor(Constants.SKY_BLUE);
    }

    /**
     * Populate the PhysicsSystem. Invoked once during initialization.
     */
    @Override
    protected void populateSystem() {
        // Add a static horizontal plane at y=-0.65 to represent the ground:
        float groundY = -0.65f;
        addPlane(groundY);
        /*
         * Create a wedge-shaped vehicle with a low center of gravity.
         * The local forward direction is +Z.
         */
        float noseZ = 1.4f;           // offset from chassis center
        float spoilerY = 0.5f;        // offset from chassis center
        float tailZ = -0.7f;          // offset from chassis center
        float undercarriageY = -0.1f; // offset from chassis center
        float halfWidth = 0.4f;
        List<Vec3Arg> cornerLocations = new ArrayList<>(6);
        cornerLocations.add(new Vec3(+halfWidth, undercarriageY, noseZ));
        cornerLocations.add(new Vec3(-halfWidth, undercarriageY, noseZ));
        cornerLocations.add(new Vec3(+halfWidth, undercarriageY, tailZ));
        cornerLocations.add(new Vec3(-halfWidth, undercarriageY, tailZ));
        cornerLocations.add(new Vec3(+halfWidth, spoilerY, tailZ));
        cornerLocations.add(new Vec3(-halfWidth, spoilerY, tailZ));
        ShapeSettings ss = new ConvexHullShapeSettings(cornerLocations);
        ShapeRefC wedgeShape = ss.create().get();

        BodyCreationSettings bcs = new BodyCreationSettings();
        bcs.getMassPropertiesOverride().setMass(200f);
        bcs.setOverrideMassProperties(EOverrideMassProperties.CalculateInertia);
        bcs.setShape(wedgeShape);

        BodyInterface bi = physicsSystem.getBodyInterface();
        Body body = bi.createBody(bcs);
        bi.addBody(body.getId(), EActivation.Activate);

        // Configure 4 wheels, 2 in the front (for steering) and 2 in the rear:
        float frontAxleZ = 0.7f * noseZ; // offset from chassis center
        float rearAxleZ = 0.8f * tailZ; // offset from chassis center
        float xOffset = 0.9f * halfWidth;
        WheelSettingsWv[] wheels = new WheelSettingsWv[4];
        for (int i = 0; i < 4; ++i) {
            wheels[i] = new WheelSettingsWv();
        }
        wheels[0].setPosition(new Vec3(-xOffset, 0f, frontAxleZ)); // left front
        wheels[1].setPosition(new Vec3(xOffset, 0f, frontAxleZ));
        wheels[2].setPosition(new Vec3(-xOffset, 0f, rearAxleZ));
        wheels[3].setPosition(new Vec3(xOffset, 0f, rearAxleZ)); // right rear

        // The rear wheels aren't used for steering.
        wheels[2].setMaxSteerAngle(0f);
        wheels[3].setMaxSteerAngle(0f);
        /*
         * Configure a controller with a single differential,
         * for rear-wheel drive:
         */
        WheeledVehicleControllerSettings wvcs
                = new WheeledVehicleControllerSettings();
        wvcs.setNumDifferentials(1);
        VehicleDifferentialSettings vds = wvcs.getDifferential(0);
        vds.setLeftWheel(2);
        vds.setRightWheel(3);

        VehicleConstraintSettings vcs = new VehicleConstraintSettings();
        vcs.addWheels(wheels);
        vcs.setController(wvcs);
        VehicleConstraint vehicle = new VehicleConstraint(body, vcs);
        VehicleCollisionTester tester = new VehicleCollisionTesterRay(0);
        vehicle.setVehicleCollisionTester(tester);
        physicsSystem.addConstraint(vehicle);
        physicsSystem.addStepListener(vehicle.getStepListener());

        // Visualize the vehicle.
        visualizeShape(vehicle);
        visualizeWheels(vehicle);

        // Apply a steering angle of 4 degrees left (to both front wheels):
        float right = -Jolt.degreesToRadians(4f);

        // Apply a constant forward acceleration:
        float forward = 1f;
        float brake = 0f;
        float handBrake = 0f;
        WheeledVehicleController controller
                = (WheeledVehicleController) vehicle.getController();
        controller.setDriverInput(forward, right, brake, handBrake);
    }
    // *************************************************************************
    // private methods

    /**
     * Add a horizontal plane body to the system.
     *
     * @param y the desired elevation (in system coordinates)
     */
    private void addPlane(float y) {
        ConstPlane plane = new Plane(0f, 1f, 0f, -y);
        ConstShape shape = new PlaneShape(plane);
        BodyCreationSettings bcs = new BodyCreationSettings();
        bcs.setMotionType(EMotionType.Static);
        bcs.setShape(shape);

        BodyInterface bi = physicsSystem.getBodyInterface();
        Body body = bi.createBody(bcs);
        bi.addBody(body, EActivation.DontActivate);

        // visualization
        String resourceName = "/Textures/greenTile.png";
        float maxAniso = 16f;
        TextureKey textureKey
                = new TextureKey("classpath://" + resourceName, maxAniso);
        visualizeShape(body, 0.1f)
                .setSpecularColor(Constants.DARK_GRAY)
                .setTexture(textureKey);
    }
}
