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
import com.github.stephengold.joltjni.BoxShape;
import com.github.stephengold.joltjni.PhysicsSystem;
import com.github.stephengold.joltjni.Quat;
import com.github.stephengold.joltjni.RVec3;
import com.github.stephengold.joltjni.SixDofConstraintSettings;
import com.github.stephengold.joltjni.SphereShape;
import com.github.stephengold.joltjni.SpringSettings;
import com.github.stephengold.joltjni.TwoBodyConstraint;
import com.github.stephengold.joltjni.enumerate.EActivation;
import com.github.stephengold.joltjni.enumerate.EAxis;
import com.github.stephengold.joltjni.enumerate.EMotionType;
import com.github.stephengold.joltjni.enumerate.EOverrideMassProperties;
import com.github.stephengold.joltjni.readonly.ConstShape;
import com.github.stephengold.joltjni.readonly.RVec3Arg;
import com.github.stephengold.sportjolt.Constants;
import com.github.stephengold.sportjolt.Projection;
import com.github.stephengold.sportjolt.Utils;
import com.github.stephengold.sportjolt.input.RotateMode;
import com.github.stephengold.sportjolt.physics.BasePhysicsApp;
import com.github.stephengold.sportjolt.physics.PhysicsTickListener;
import org.joml.Vector2fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * A simple example of a constraint with springs.
 * <p>
 * Builds upon HelloLimit.
 *
 * @author Stephen Gold sgold@sonic.net
 */
final public class HelloSpring
        extends BasePhysicsApp
        implements PhysicsTickListener {
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
     * Instantiate the HelloSpring application.
     * <p>
     * This no-arg constructor was made explicit to avoid javadoc warnings from
     * JDK 18+.
     */
    public HelloSpring() {
        // do nothing
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Main entry point for the HelloSpring application.
     *
     * @param arguments array of command-line arguments (not null)
     */
    public static void main(String[] arguments) {
        HelloSpring application = new HelloSpring();
        application.start();
    }
    // *************************************************************************
    // BasePhysicsApp methods

    /**
     * Create the PhysicsSystem. Invoked once during initialization.
     *
     * @return a new object
     */
    @Override
    protected PhysicsSystem createSystem() {
        // For simplicity, use a single broadphase layer:
        int maxBodies = 3;
        int numBpLayers = 1;
        PhysicsSystem result = createSystem(maxBodies, numBpLayers);

        // To enable the callbacks, register this app as a tick listener:
        addTickListener(this);

        // Reduce the time step for better accuracy:
        this.timePerStep = 0.005f; // seconds

        return result;
    }

    /**
     * Initialize the application. Invoked once.
     */
    @Override
    protected void initialize() {
        super.initialize();

        configureCamera();
        configureLighting();

        // Disable VSync for more frequent mouse-position updates:
        setVsync(false);
    }

    /**
     * Populate the PhysicsSystem with bodies and constraints. Invoked once
     * during initialization.
     */
    @Override
    protected void populateSystem() {
        // Add a static, green square to represent the ground:
        float halfExtent = 3f;
        Body ground = addSquare(halfExtent, groundY);

        // Add a mouse-controlled kinematic paddle:
        paddleBody = addBox();

        // Add a dynamic ball:
        Body ballBody = addBall();

        // Constrain the ball to the origin:
        SixDofConstraintSettings settings = new SixDofConstraintSettings();
        settings.makeFixedAxis(EAxis.TranslationX);
        settings.makeFixedAxis(EAxis.TranslationY);
        settings.makeFixedAxis(EAxis.TranslationZ);
        // Configure 2 springs to soften the horizontal limits:
        SpringSettings xSpring
                = settings.getLimitsSpringSettings(EAxis.TranslationX);
        xSpring.setDamping(0.2f);
        xSpring.setFrequency(2f); // in Hertz
        SpringSettings zSpring
                = settings.getLimitsSpringSettings(EAxis.TranslationZ);
        zSpring.setDamping(0.2f);
        zSpring.setFrequency(2f); // in Hertz
        Body fixedToWorld = Body.sFixedToWorld();
        TwoBodyConstraint constraint = settings.create(fixedToWorld, ballBody);
        physicsSystem.addConstraint(constraint);

        // Visualize the ground:
        visualizeShape(ground)
                .setColor(Constants.GREEN)
                .setSpecularColor(Constants.DARK_GRAY);
    }

    /**
     * Callback invoked during each iteration of the render loop.
     */
    @Override
    protected void render() {
        Vector2fc screenXy = getInputManager().locateCursor();
        if (screenXy != null) {
            /*
             * Calculate the ground-plane location (if any)
             * indicated by the mouse cursor:
             */
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

        super.render();
    }
    // *************************************************************************
    // PhysicsTickListener methods

    /**
     * Callback invoked (by Sport-Jolt, not by Jolt Physics) after the system
     * has been stepped.
     *
     * @param system the system that was just stepped (not null)
     * @param timeStep the duration of the simulation step (in seconds, &ge;0)
     */
    @Override
    public void physicsTick(PhysicsSystem system, float timeStep) {
        // do nothing
    }

    /**
     * Callback invoked (by Sport-Jolt, not by Jolt Physics) before the system
     * is stepped.
     *
     * @param system the system that's about to be stepped (not null)
     * @param timeStep the duration of the simulation step (in seconds, &ge;0)
     */
    @Override
    public void prePhysicsTick(PhysicsSystem system, float timeStep) {
        // Relocate the kinematic paddle based on the mouse location:
        float y = groundY + paddleHalfHeight;
        RVec3Arg bodyLocation = new RVec3(mouseLocation.x, y, mouseLocation.z);
        paddleBody.moveKinematic(bodyLocation, new Quat(), timeStep);
    }
    // *************************************************************************
    // private methods

    /**
     * Create a dynamic rigid body with a sphere shape and add it to the system.
     *
     * @return the new body
     */
    private Body addBall() {
        float radius = 0.4f;
        ConstShape shape = new SphereShape(radius);

        BodyCreationSettings bcs = new BodyCreationSettings();
        bcs.setAllowSleeping(false); // Disable sleep (deactivation).

        // Apply angular damping to reduce the ball's tendency to spin:
        bcs.setAngularDamping(2f);

        bcs.getMassPropertiesOverride().setMass(0.2f);
        bcs.setOverrideMassProperties(EOverrideMassProperties.CalculateInertia);
        bcs.setShape(shape);

        BodyInterface bi = physicsSystem.getBodyInterface();
        Body result = bi.createBody(bcs);
        bi.addBody(result, EActivation.Activate);

        visualizeShape(result);

        return result;
    }

    /**
     * Create a kinematic body with a box shape and add it to the system.
     *
     * @return the new body
     */
    private Body addBox() {
        ConstShape shape = new BoxShape(0.2f, paddleHalfHeight, 0.2f);

        BodyCreationSettings bcs = new BodyCreationSettings();
        bcs.setAllowSleeping(false); // Disable sleep (deactivation).
        bcs.setMotionType(EMotionType.Kinematic); // default=Dynamic
        bcs.setShape(shape);

        BodyInterface bi = physicsSystem.getBodyInterface();
        Body result = bi.createBody(bcs);
        bi.addBody(result, EActivation.Activate);

        visualizeShape(result);

        return result;
    }

    /**
     * Add a static horizontal box to the system.
     *
     * @param halfExtent half of the desired side length (in meters)
     * @param y the desired elevation of the body's top face (in system
     * coordinates)
     * @return the new body (not null)
     */
    private Body addSquare(float halfExtent, float y) {
        float halfThickness = 0.1f;
        ConstShape shape = new BoxShape(halfExtent, halfThickness, halfExtent);
        BodyCreationSettings bcs = new BodyCreationSettings();
        bcs.setMotionType(EMotionType.Static);
        bcs.setObjectLayer(objLayerNonMoving);
        bcs.setPosition(0., y - halfThickness, 0.);
        bcs.setShape(shape);

        BodyInterface bi = physicsSystem.getBodyInterface();
        Body result = bi.createBody(bcs);
        bi.addBody(result, EActivation.DontActivate);

        assert result != null;
        return result;
    }

    /**
     * Configure the Camera and CIP during initialization.
     */
    private static void configureCamera() {
        getCameraInputProcessor().setRotationMode(RotateMode.None);

        cam.setLocation(0f, 5f, 10f);
        cam.setUpAngle(-0.6f);
        cam.setAzimuth(-1.6f);
    }

    /**
     * Configure lighting and the background color.
     */
    private void configureLighting() {
        setLightDirection(7f, 3f, 5f);

        // Set the background color to light blue:
        setBackgroundColor(Constants.SKY_BLUE);
    }
}
