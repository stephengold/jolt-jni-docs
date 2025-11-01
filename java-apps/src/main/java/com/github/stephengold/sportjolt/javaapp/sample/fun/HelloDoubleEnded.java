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
import com.github.stephengold.joltjni.BoxShape;
import com.github.stephengold.joltjni.PhysicsSystem;
import com.github.stephengold.joltjni.Plane;
import com.github.stephengold.joltjni.PlaneShape;
import com.github.stephengold.joltjni.Quat;
import com.github.stephengold.joltjni.RVec3;
import com.github.stephengold.joltjni.SixDofConstraintSettings;
import com.github.stephengold.joltjni.SphereShape;
import com.github.stephengold.joltjni.TwoBodyConstraint;
import com.github.stephengold.joltjni.enumerate.EActivation;
import com.github.stephengold.joltjni.enumerate.EAxis;
import com.github.stephengold.joltjni.enumerate.EMotionType;
import com.github.stephengold.joltjni.enumerate.EOverrideMassProperties;
import com.github.stephengold.joltjni.operator.Op;
import com.github.stephengold.joltjni.readonly.ConstPlane;
import com.github.stephengold.joltjni.readonly.ConstShape;
import com.github.stephengold.joltjni.readonly.RVec3Arg;
import com.github.stephengold.joltjni.readonly.Vec3Arg;
import com.github.stephengold.sportjolt.BaseApplication;
import com.github.stephengold.sportjolt.Camera;
import com.github.stephengold.sportjolt.Constants;
import com.github.stephengold.sportjolt.Projection;
import com.github.stephengold.sportjolt.TextureKey;
import com.github.stephengold.sportjolt.Utils;
import com.github.stephengold.sportjolt.input.RotateMode;
import com.github.stephengold.sportjolt.physics.BasePhysicsApp;
import com.github.stephengold.sportjolt.physics.ConstraintGeometry;
import com.github.stephengold.sportjolt.physics.FunctionalPhysicsApp;
import org.joml.Vector2fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * A simple example of a double-ended SixDofBodyConstraint.
 * <p>
 * Builds upon HelloPivot.
 *
 * @author Stephen Gold sgold@sonic.net
 */
final public class HelloDoubleEnded {
    // *************************************************************************
    // constants

    /**
     * system Y coordinate of the ground plane
     */
    final private static float groundY = -4f;
    /**
     * half the height of the paddle (in meters)
     */
    final private static float paddleHalfHeight = 1f;
    // *************************************************************************
    // fields

    /**
     * mouse-controlled kinematic paddle
     */
    private static Body paddleBody;
    /**
     * latest ground-plane location indicated by the mouse cursor
     */
    final private static Vector3f mouseLocation = new Vector3f();
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private HelloDoubleEnded() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Main entry point for the HelloDoubleEnded application.
     *
     * @param arguments array of command-line arguments (not null)
     */
    public static void main(String[] arguments) {
        FunctionalPhysicsApp fpa = new FunctionalPhysicsApp();

        fpa.setCreateSystem((app) -> {
            // For simplicity, use a single broadphase layer:
            int maxBodies = 3;
            int numBpLayers = 1;
            PhysicsSystem result = app.createSystem(maxBodies, numBpLayers);

            // Reduce the time step for better accuracy:
            app.setTimePerStep(0.005f); // seconds

            return result;
        });

        fpa.setInitialize((app) -> {
            configureCamera();
            BaseApplication.setLightDirection(7f, 3f, 5f);

            // Disable VSync for more frequent mouse-position updates:
            BaseApplication.setVsync(false);
        });

        fpa.setPopulateSystem((app) -> {
            PhysicsSystem physicsSystem = app.getPhysicsSystem();
            BodyInterface bi = physicsSystem.getBodyInterface();

            // Add a static plane to represent the ground:
            addPlane(bi, groundY);

            // Add a mouse-controlled kinematic paddle:
            paddleBody = addBox(bi);

            // Add a dynamic ball:
            Body ballBody = addBall(bi);

            // Add a double-ended constraint to connect the ball to the paddle:
            RVec3Arg pivotInBall = new RVec3(0., 3., 0.);
            RVec3Arg pivotInPaddle = new RVec3(0., 3., 0.);

            SixDofConstraintSettings settings = new SixDofConstraintSettings();
            settings.makeFixedAxis(EAxis.TranslationX);
            settings.makeFixedAxis(EAxis.TranslationY);
            settings.makeFixedAxis(EAxis.TranslationZ);
            settings.setPosition1(pivotInPaddle);
            settings.setPosition2(pivotInBall);

            TwoBodyConstraint constraint
                    = settings.create(paddleBody, ballBody);
            physicsSystem.addConstraint(constraint);

            // Visualize the constraint:
            new ConstraintGeometry(constraint, 1); // paddleBody is 1st end
            new ConstraintGeometry(constraint, 2); // ballBody is 2nd end
        });

        fpa.setPrePhysicsTick((app, system, timeStep) -> {
            // Relocate the kinematic paddle based on the mouse location:
            Vec3Arg mouse = Utils.toJoltVector(mouseLocation);
            RVec3Arg bodyLocation
                    = Op.plus(mouse, new RVec3(0., paddleHalfHeight, 0.));
            paddleBody.moveKinematic(bodyLocation, new Quat(), timeStep);
        });

        fpa.setPreRender((app) -> {
            Vector2fc screenXy
                    = BaseApplication.getInputManager().locateCursor();
            if (screenXy != null) {
                /*
                 * Calculate the ground-plane location (if any)
                 * indicated by the mouse cursor:
                 */
                Camera cam = BaseApplication.getCamera();
                Vector3fc nearLocation
                        = cam.clipToWorld(screenXy, Projection.nearClipZ, null);
                Vector3fc farLocation
                        = cam.clipToWorld(screenXy, Projection.farClipZ, null);
                float nearY = nearLocation.y();
                float farY = farLocation.y();
                if (nearY > groundY && farY < groundY) {
                    float dy = nearY - farY;
                    float t = (nearY - groundY) / dy;
                    Utils.lerp(t, nearLocation, farLocation, mouseLocation);
                }
            }
        });

        fpa.start("HelloDoubleEnded");
    }
    // *************************************************************************
    // private methods

    /**
     * Create a dynamic rigid body with a sphere shape and add it to the system.
     *
     * @param bi the system's body interface (not {@code null})
     * @return the new body
     */
    private static Body addBall(BodyInterface bi) {
        float radius = 0.4f;
        ConstShape shape = new SphereShape(radius);

        BodyCreationSettings bcs = new BodyCreationSettings();
        bcs.setAllowSleeping(false); // Disable sleep (deactivation).
        bcs.getMassPropertiesOverride().setMass(0.2f);
        bcs.setOverrideMassProperties(EOverrideMassProperties.CalculateInertia);
        bcs.setShape(shape);

        Body result = bi.createBody(bcs);
        bi.addBody(result, EActivation.Activate);

        BasePhysicsApp.visualizeShape(result);

        return result;
    }

    /**
     * Create a kinematic body with a box shape and add it to the system.
     *
     * @param bi the system's body interface (not {@code null})
     * @return the new body
     */
    private static Body addBox(BodyInterface bi) {
        ConstShape shape = new BoxShape(0.3f, paddleHalfHeight, 1f);

        BodyCreationSettings bcs = new BodyCreationSettings();
        bcs.setAllowSleeping(false); // Disable sleep (deactivation).
        bcs.setMotionType(EMotionType.Kinematic); // default=Dynamic
        bcs.setShape(shape);

        Body result = bi.createBody(bcs);
        bi.addBody(result, EActivation.Activate);

        BasePhysicsApp.visualizeShape(result);

        return result;
    }

    /**
     * Add a static horizontal plane body to the physics system.
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

    /**
     * Configure the Camera and CIP during initialization.
     */
    private static void configureCamera() {
        BasePhysicsApp.getCameraInputProcessor()
                .setRotationMode(RotateMode.None);

        Camera cam = BasePhysicsApp.getCamera();
        cam.setLocation(0f, 5f, 10f);
        cam.setUpAngle(-0.6f);
        cam.setAzimuth(-1.6f);
    }
}
