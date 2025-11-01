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
package com.github.stephengold.sportjolt.javaapp.sample.fun;

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
import com.github.stephengold.sportjolt.BaseApplication;
import com.github.stephengold.sportjolt.Constants;
import com.github.stephengold.sportjolt.TextureKey;
import com.github.stephengold.sportjolt.input.RotateMode;
import com.github.stephengold.sportjolt.physics.BasePhysicsApp;
import com.github.stephengold.sportjolt.physics.FunctionalPhysicsApp;
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
final public class HelloVehicle {
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private HelloVehicle() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Main entry point for the HelloVehicle application.
     *
     * @param arguments array of command-line arguments (not null)
     */
    public static void main(String[] arguments) {
        FunctionalPhysicsApp fpa = new FunctionalPhysicsApp();

        fpa.setCreateSystem((app) -> {
            // For simplicity, use a single broadphase layer:
            int maxBodies = 2;
            int numBpLayers = 1;
            PhysicsSystem result = app.createSystem(maxBodies, numBpLayers);

            return result;
        });

        fpa.setInitialize((app) -> {
            BaseApplication.setVsync(true);

            BaseApplication.getCameraInputProcessor()
                    .setRotationMode(RotateMode.DragLMB);
            BaseApplication.getCamera().setLocation(-4.2f, 6.8f, 36f)
                    .setLookDirection(new Vector3f(0.49f, -0.36f, -0.8f));

            BaseApplication.setBackgroundColor(Constants.SKY_BLUE);
        });

        fpa.setPopulateSystem((app) -> {
            PhysicsSystem physicsSystem = app.getPhysicsSystem();
            BodyInterface bi = physicsSystem.getBodyInterface();

            // Add a static horizontal plane at y=-0.65 to represent the ground:
            float groundY = -0.65f;
            addPlane(bi, groundY);
            /*
             * Create a wedge-shaped body with a low center of gravity.
             * The local forward direction is +Z.
             */
            float noseZ = 1.4f;           // offset from body's center
            float spoilerY = 0.5f;        // offset from body's center
            float tailZ = -0.7f;          // offset from body's center
            float undercarriageY = -0.1f; // offset from body's center
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
            bcs.setOverrideMassProperties(
                    EOverrideMassProperties.CalculateInertia);
            bcs.setShape(wedgeShape);

            Body body = bi.createBody(bcs);
            bi.addBody(body, EActivation.Activate);

            // Configure 4 wheels, 2 in front (for steering) and 2 in rear:
            float frontAxleZ = 0.7f * noseZ; // offset from body's origin
            float rearAxleZ = 0.8f * tailZ; // offset from body's origin
            float xOffset = 0.9f * halfWidth;
            WheelSettingsWv[] wheels = new WheelSettingsWv[4];
            for (int i = 0; i < 4; ++i) {
                wheels[i] = new WheelSettingsWv();
            }
            wheels[0].setPosition(new Vec3(-xOffset, 0f, frontAxleZ)); // LF
            wheels[1].setPosition(new Vec3(xOffset, 0f, frontAxleZ));
            wheels[2].setPosition(new Vec3(-xOffset, 0f, rearAxleZ));
            wheels[3].setPosition(new Vec3(xOffset, 0f, rearAxleZ)); // RR

            // The rear wheels aren't used for steering:
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
            int objectLayer = body.getObjectLayer();
            VehicleCollisionTester tester
                    = new VehicleCollisionTesterRay(objectLayer);
            vehicle.setVehicleCollisionTester(tester);
            physicsSystem.addConstraint(vehicle);
            physicsSystem.addStepListener(vehicle.getStepListener());

            // Visualize the vehicle:
            BasePhysicsApp.visualizeShape(vehicle);
            BasePhysicsApp.visualizeWheels(vehicle);

            // Apply a steering angle of 4 degrees left (to both front wheels):
            float right = -Jolt.degreesToRadians(4f);

            // Apply a constant forward acceleration:
            float forward = 1f;
            float brake = 0f;
            float handBrake = 0f;
            WheeledVehicleController controller
                    = (WheeledVehicleController) vehicle.getController();
            controller.setDriverInput(forward, right, brake, handBrake);
        });

        fpa.start("HelloVehicle");
    }
    // *************************************************************************
    // private methods

    /**
     * Add a static horizontal plane body to the system.
     *
     * @param bi the system's body interface (not {@code null})
     * @param y the desired elevation (in system coordinates)
     */
    private static void addPlane(BodyInterface bi, float y) {
        ConstPlane plane = new Plane(0f, 1f, 0f, -y);
        ConstShape shape = new PlaneShape(plane);
        BodyCreationSettings bcs = new BodyCreationSettings();
        bcs.setMotionType(EMotionType.Static);
        bcs.setObjectLayer(BasePhysicsApp.objLayerNonMoving);
        bcs.setShape(shape);

        Body body = bi.createBody(bcs);
        bi.addBody(body, EActivation.DontActivate);

        // Visualize the body:
        String resourceName = "/Textures/greenTile.png";
        float maxAniso = 16f;
        TextureKey textureKey
                = new TextureKey("classpath://" + resourceName, maxAniso);
        BasePhysicsApp.visualizeShape(body, 0.1f)
                .setSpecularColor(Constants.DARK_GRAY)
                .setTexture(textureKey);
    }
}
