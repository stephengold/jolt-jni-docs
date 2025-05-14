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
import com.github.stephengold.joltjni.TwoBodyConstraint;
import com.github.stephengold.joltjni.enumerate.EActivation;
import com.github.stephengold.joltjni.enumerate.EAxis;
import com.github.stephengold.joltjni.enumerate.EMotionType;
import com.github.stephengold.joltjni.enumerate.EOverrideMassProperties;
import com.github.stephengold.joltjni.readonly.ConstShape;
import com.github.stephengold.joltjni.readonly.RVec3Arg;
import com.github.stephengold.sportjolt.Projection;
import com.github.stephengold.sportjolt.Utils;
import com.github.stephengold.sportjolt.input.RotateMode;
import com.github.stephengold.sportjolt.physics.BasePhysicsApp;
import com.github.stephengold.sportjolt.physics.LocalAxisGeometry;
import com.github.stephengold.sportjolt.physics.PhysicsTickListener;
import org.joml.Vector2fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * A simple example of a single-ended SixDofConstraint simulating a hinge
 * constraint.
 * <p>
 * Builds upon HelloKinematics.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class HelloConstraint
        extends BasePhysicsApp
        implements PhysicsTickListener {
    // *************************************************************************
    // fields

    /**
     * mouse-controlled kinematic ball
     */
    private static Body kineBall;
    /**
     * latest X-Z plane location indicated by the mouse cursor
     */
    final private static Vector3f mouseLocation = new Vector3f();
    // *************************************************************************
    // constructors

    /**
     * Instantiate the HelloConstraint application.
     * <p>
     * This no-arg constructor was made explicit to avoid javadoc warnings from
     * JDK 18+.
     */
    public HelloConstraint() {
        // do nothing
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Main entry point for the HelloConstraint application.
     *
     * @param arguments array of command-line arguments (not null)
     */
    public static void main(String[] arguments) {
        HelloConstraint application = new HelloConstraint();
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
        int numBpLayers = 1;
        int maxBodies = 2;
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
        setLightDirection(7f, 3f, 5f);

        // Disable VSync for more frequent mouse-position updates:
        setVsync(false);
    }

    /**
     * Populate the PhysicsSystem with bodies and constraints. Invoked once
     * during initialization.
     */
    @Override
    protected void populateSystem() {
        // Add a mouse-controlled kinematic ball:
        kineBall = addKineBall();

        // Add a dynamic box:
        Body rotor = addBox();

        // Constrain the box to rotate around its Y axis:
        SixDofConstraintSettings settings = new SixDofConstraintSettings();
        settings.makeFixedAxis(EAxis.RotationX);
        // Y-axis rotation remains free.
        settings.makeFixedAxis(EAxis.RotationZ);
        settings.makeFixedAxis(EAxis.TranslationX);
        settings.makeFixedAxis(EAxis.TranslationY);
        settings.makeFixedAxis(EAxis.TranslationZ);

        Body fixedToWorld = Body.sFixedToWorld();
        TwoBodyConstraint constraint = settings.create(fixedToWorld, rotor);
        physicsSystem.addConstraint(constraint);

        // Visualize the axis of rotation:
        int axisIndex = 1; // +Y axis
        float axisLength = 2f; // meters
        new LocalAxisGeometry(rotor, axisIndex, axisLength);
    }

    /**
     * Callback invoked during each iteration of the render loop.
     */
    @Override
    protected void render() {
        Vector2fc screenXy = getInputManager().locateCursor();
        if (screenXy != null) {
            /*
             * Calculate the X-Z plane location (if any)
             * indicated by the mouse cursor:
             */
            Vector3fc nearLocation
                    = cam.clipToWorld(screenXy, Projection.nearClipZ, null);
            Vector3fc farLocation
                    = cam.clipToWorld(screenXy, Projection.farClipZ, null);
            float nearY = nearLocation.y();
            float farY = farLocation.y();
            if (nearY > 0f && farY < 0f) {
                float dy = nearY - farY;
                float t = nearY / dy;
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
        // Relocate the kinematic ball based on the mouse location:
        RVec3Arg bodyLocation
                = new RVec3(mouseLocation.x, mouseLocation.y, mouseLocation.z);
        kineBall.moveKinematic(bodyLocation, new Quat(), timeStep);
    }
    // *************************************************************************
    // private methods

    /**
     * Create a dynamic rigid body with a box shape and add it to the system.
     *
     * @return the new body
     */
    private Body addBox() {
        ConstShape shape = new BoxShape(0.3f, 1f, 3f);

        BodyCreationSettings bcs = new BodyCreationSettings();
        bcs.setAllowSleeping(false); // Disable sleep (deactivation).
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
     * Create a kinematic body with a sphere shape and add it to the system.
     *
     * @return the new body
     */
    private Body addKineBall() {
        float ballRadius = 1f;
        ConstShape shape = new SphereShape(ballRadius);

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
     * Configure the Camera and CIP during initialization.
     */
    private static void configureCamera() {
        getCameraInputProcessor().setRotationMode(RotateMode.None);

        cam.setLocation(0f, 5f, 10f);
        cam.setUpAngle(-0.6f);
        cam.setAzimuth(-1.6f);
    }
}
