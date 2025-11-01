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
import com.github.stephengold.joltjni.Quat;
import com.github.stephengold.joltjni.RVec3;
import com.github.stephengold.joltjni.SixDofConstraintSettings;
import com.github.stephengold.joltjni.SphereShape;
import com.github.stephengold.joltjni.TwoBodyConstraint;
import com.github.stephengold.joltjni.enumerate.EActivation;
import com.github.stephengold.joltjni.enumerate.EAxis;
import com.github.stephengold.joltjni.enumerate.EMotionType;
import com.github.stephengold.joltjni.enumerate.EOverrideMassProperties;
import com.github.stephengold.joltjni.readonly.ConstShape;
import com.github.stephengold.joltjni.readonly.RVec3Arg;
import com.github.stephengold.sportjolt.BaseApplication;
import com.github.stephengold.sportjolt.Camera;
import com.github.stephengold.sportjolt.Constants;
import com.github.stephengold.sportjolt.Projection;
import com.github.stephengold.sportjolt.Utils;
import com.github.stephengold.sportjolt.input.RotateMode;
import com.github.stephengold.sportjolt.physics.BasePhysicsApp;
import com.github.stephengold.sportjolt.physics.FunctionalPhysicsApp;
import org.joml.Vector2fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * A simple example of a constraint with limits.
 * <p>
 * Builds upon HelloConstraint.
 *
 * @author Stephen Gold sgold@sonic.net
 */
final public class HelloLimit {
    // *************************************************************************
    // constants

    /**
     * system Y coordinate of the ground plane
     */
    final private static float groundY = -0.5f;
    /**
     * half the height of the paddle (in meters)
     */
    final private static float paddleHalfHeight = 0.5f;
    // *************************************************************************
    // fields

    /**
     * mouse-controlled kinematic paddle
     */
    private static Body paddleBody;
    /**
     * latest ground location indicated by the mouse cursor
     */
    final private static Vector3f mouseLocation = new Vector3f();
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private HelloLimit() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Main entry point for the HelloLimit application.
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
            configureLighting();

            // Disable VSync for more frequent mouse-position updates:
            BaseApplication.setVsync(false);
        });

        fpa.setPopulateSystem((app) -> {
            PhysicsSystem physicsSystem = app.getPhysicsSystem();
            BodyInterface bi = physicsSystem.getBodyInterface();

            // Add a static, green square to represent the ground:
            float halfExtent = 3f;
            Body ground = addSquare(bi, halfExtent, groundY);

            // Add a mouse-controlled kinematic paddle:
            paddleBody = addBox(bi);

            // Add a dynamic ball:
            Body ballBody = addBall(bi);

            // Constrain the ball to a square in the X-Z plane:
            SixDofConstraintSettings settings = new SixDofConstraintSettings();
            settings.setLimitedAxis(
                    EAxis.TranslationX, -halfExtent, +halfExtent);
            settings.makeFixedAxis(EAxis.TranslationY);
            settings.setLimitedAxis(
                    EAxis.TranslationZ, -halfExtent, +halfExtent);
            Body fixedToWorld = Body.sFixedToWorld();
            TwoBodyConstraint constraint
                    = settings.create(fixedToWorld, ballBody);
            physicsSystem.addConstraint(constraint);

            // Visualize the ground:
            BasePhysicsApp.visualizeShape(ground)
                    .setColor(Constants.GREEN)
                    .setSpecularColor(Constants.DARK_GRAY);
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

        fpa.setPrePhysicsTick((app, system, timeStep) -> {
            // Relocate the kinematic paddle based on the mouse location:
            float y = groundY + paddleHalfHeight;
            RVec3Arg bodyLocation
                    = new RVec3(mouseLocation.x, y, mouseLocation.z);
            paddleBody.moveKinematic(bodyLocation, new Quat(), timeStep);
        });

        fpa.start("HelloLimit");
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

        // Apply angular damping to reduce the ball's tendency to spin:
        bcs.setAngularDamping(2f);

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
        ConstShape shape = new BoxShape(0.2f, paddleHalfHeight, 0.2f);

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
     * Add a static horizontal box to the system.
     *
     * @param bi the system's body interface (not {@code null})
     * @param halfExtent half of the desired side length (in meters)
     * @param y the desired elevation of the body's top face (in system
     * coordinates)
     * @return the new body (not null)
     */
    private static Body addSquare(BodyInterface bi, float halfExtent, float y) {
        float halfThickness = 0.1f;
        ConstShape shape = new BoxShape(halfExtent, halfThickness, halfExtent);
        BodyCreationSettings bcs = new BodyCreationSettings();
        bcs.setMotionType(EMotionType.Static);
        bcs.setObjectLayer(BasePhysicsApp.objLayerNonMoving);
        bcs.setPosition(0., y - halfThickness, 0.);
        bcs.setShape(shape);

        Body result = bi.createBody(bcs);
        bi.addBody(result, EActivation.DontActivate);

        assert result != null;
        return result;
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

    /**
     * Configure lighting and the background color.
     */
    private static void configureLighting() {
        BaseApplication.setLightDirection(7f, 3f, 5f);

        // Set the background color to light blue:
        BaseApplication.setBackgroundColor(Constants.SKY_BLUE);
    }
}
