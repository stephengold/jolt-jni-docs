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
import com.github.stephengold.joltjni.Plane;
import com.github.stephengold.joltjni.PlaneShape;
import com.github.stephengold.joltjni.Quat;
import com.github.stephengold.joltjni.RVec3;
import com.github.stephengold.joltjni.SixDofConstraintSettings;
import com.github.stephengold.joltjni.SphereShape;
import com.github.stephengold.joltjni.TwoBodyConstraint;
import com.github.stephengold.joltjni.Vec3;
import com.github.stephengold.joltjni.enumerate.EActivation;
import com.github.stephengold.joltjni.enumerate.EAxis;
import com.github.stephengold.joltjni.enumerate.EMotionType;
import com.github.stephengold.joltjni.enumerate.EOverrideMassProperties;
import com.github.stephengold.joltjni.operator.Op;
import com.github.stephengold.joltjni.readonly.ConstPlane;
import com.github.stephengold.joltjni.readonly.ConstShape;
import com.github.stephengold.joltjni.readonly.RVec3Arg;
import com.github.stephengold.sportjolt.Constants;
import com.github.stephengold.sportjolt.Projection;
import com.github.stephengold.sportjolt.TextureKey;
import com.github.stephengold.sportjolt.Utils;
import com.github.stephengold.sportjolt.input.RotateMode;
import com.github.stephengold.sportjolt.physics.BasePhysicsApp;
import com.github.stephengold.sportjolt.physics.ConstraintGeometry;
import com.github.stephengold.sportjolt.physics.PhysicsTickListener;
import org.joml.Vector2fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * A simple example of a double-ended TwoBodyConstraint.
 * <p>
 * Builds upon HelloConstraint.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class HelloDoubleEnded
        extends BasePhysicsApp
        implements PhysicsTickListener {
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
     * Instantiate the HelloDoubleEnded application.
     * <p>
     * This no-arg constructor was made explicit to avoid javadoc warnings from
     * JDK 18+.
     */
    public HelloDoubleEnded() {
        // do nothing
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Main entry point for the HelloDoubleEnded application.
     *
     * @param arguments array of command-line arguments (not null)
     */
    public static void main(String[] arguments) {
        HelloDoubleEnded application = new HelloDoubleEnded();
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
        int maxBodies = 3;
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
        // Add a static plane to represent the ground:
        addPlane(groundY);

        // Add a mouse-controlled kinematic paddle:
        addPaddle();

        // Add a dynamic ball:
        Body ballBody = addBall();

        // Add a double-ended constraint to connect the ball to the paddle:
        RVec3Arg pivotInBall = new RVec3(0f, 3f, 0f);
        RVec3Arg pivotInPaddle = new RVec3(0f, 3f, 0f);

        SixDofConstraintSettings settings = new SixDofConstraintSettings();
        settings.makeFixedAxis(EAxis.TranslationX);
        settings.makeFixedAxis(EAxis.TranslationY);
        settings.makeFixedAxis(EAxis.TranslationZ);
        settings.setPosition1(pivotInPaddle);
        settings.setPosition2(pivotInBall);

        TwoBodyConstraint constraint = settings.create(paddleBody, ballBody);
        physicsSystem.addConstraint(constraint);

        // Visualize the constraint:
        new ConstraintGeometry(constraint, 1); // paddleBody is at the 1st end
        new ConstraintGeometry(constraint, 2); // ballBody is at the 2nd end
    }

    /**
     * Callback invoked during each iteration of the render loop.
     */
    @Override
    protected void render() {
        // Calculate the ground location (if any) indicated by the mouse cursor.
        Vector2fc screenXy = getInputManager().locateCursor();
        if (screenXy != null) {
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
    public void prePhysicsTick(PhysicsSystem system, float timeStep) {
        // Reposition the paddle based on the mouse location.
        Vec3 mouse = Utils.toJoltVector(mouseLocation);
        RVec3 bodyLocation
                = Op.plus(mouse, new RVec3(0f, paddleHalfHeight, 0f));
        paddleBody.moveKinematic(bodyLocation, new Quat(), timeStep);
    }

    /**
     * Callback invoked (by Sport-Jolt, not by Jolt Physics) before the system
     * is stepped.
     *
     * @param system the system that's about to to be stepped (not null)
     * @param timeStep the duration of the simulation step (in seconds, &ge;0)
     */
    @Override
    public void physicsTick(PhysicsSystem system, float timeStep) {
        // do nothing
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
     */
    private void addPaddle() {
        ConstShape shape = new BoxShape(0.3f, paddleHalfHeight, 1f);

        BodyCreationSettings bcs = new BodyCreationSettings();
        bcs.setAllowSleeping(false);
        bcs.setMotionType(EMotionType.Kinematic);
        bcs.setShape(shape);

        BodyInterface bi = physicsSystem.getBodyInterface();
        paddleBody = bi.createBody(bcs);
        bi.addBody(paddleBody, EActivation.Activate);

        visualizeShape(paddleBody);
    }

    /**
     * Add a horizontal plane body to the system.
     *
     * @param y the desired elevation (in system coordinates)
     */
    private void addPlane(float y) {
        ConstPlane plane = new Plane(0f, 1f, 0f, -y);
        PlaneShape shape = new PlaneShape(plane);
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
