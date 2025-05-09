/*
 Copyright (c) 2021-2025 Stephen Gold and Yanis Boudiaf

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
import com.github.stephengold.joltjni.CapsuleShapeSettings;
import com.github.stephengold.joltjni.Jolt;
import com.github.stephengold.joltjni.PhysicsSystem;
import com.github.stephengold.joltjni.Quat;
import com.github.stephengold.joltjni.RVec3;
import com.github.stephengold.joltjni.SixDofConstraint;
import com.github.stephengold.joltjni.SixDofConstraintSettings;
import com.github.stephengold.joltjni.StaticCompoundShapeSettings;
import com.github.stephengold.joltjni.Vec3;
import com.github.stephengold.joltjni.enumerate.EActivation;
import com.github.stephengold.joltjni.enumerate.EAxis;
import com.github.stephengold.joltjni.enumerate.EMotorState;
import com.github.stephengold.joltjni.enumerate.EOverrideMassProperties;
import com.github.stephengold.joltjni.enumerate.ESwingType;
import com.github.stephengold.joltjni.operator.Op;
import com.github.stephengold.joltjni.readonly.ConstShape;
import com.github.stephengold.joltjni.readonly.QuatArg;
import com.github.stephengold.joltjni.readonly.RVec3Arg;
import com.github.stephengold.joltjni.readonly.Vec3Arg;
import com.github.stephengold.sportjolt.Constants;
import com.github.stephengold.sportjolt.input.CameraInputProcessor;
import com.github.stephengold.sportjolt.input.InputProcessor;
import com.github.stephengold.sportjolt.input.RotateMode;
import com.github.stephengold.sportjolt.physics.BasePhysicsApp;
import com.github.stephengold.sportjolt.physics.ConstraintGeometry;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

/**
 * A simple example of a constraint with a motor.
 * <p>
 * Builds upon HelloLimit.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class HelloMotor extends BasePhysicsApp {
    // *************************************************************************
    // fields

    /**
     * constraint being tested
     */
    private static SixDofConstraint constraint;
    // *************************************************************************
    // constructors

    /**
     * Instantiate the HelloMotor application.
     * <p>
     * This no-arg constructor was made explicit to avoid javadoc warnings from
     * JDK 18+.
     */
    public HelloMotor() {
        // do nothing
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Main entry point for the HelloMotor application.
     *
     * @param arguments array of command-line arguments (not null)
     */
    public static void main(String[] arguments) {
        HelloMotor application = new HelloMotor();
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

        result.setGravity(0f, 0f, 0f);
        return result;
    }

    /**
     * Initialize the application. Invoked once.
     */
    @Override
    protected void initialize() {
        super.initialize();

        setVsync(true);
        configureCamera();
        configureInput();
        configureLighting();
    }

    /**
     * Populate the PhysicsSystem with bodies and constraints. Invoked once
     * during initialization.
     */
    @Override
    protected void populateSystem() {
        // Add a dynamic, green doorframe:
        Body frameBody = addFrame();

        // Add a dynamic, yellow box for the door:
        Body doorBody = addDoor();

        // Add a double-ended hinge to join the door to the frame:
        SixDofConstraintSettings settings = new SixDofConstraintSettings();
        // Fix all 3 translation DOFs:
        settings.makeFixedAxis(EAxis.TranslationX);
        settings.makeFixedAxis(EAxis.TranslationY);
        settings.makeFixedAxis(EAxis.TranslationZ);
        // Fix the X and Z rotation DOFs:
        settings.makeFixedAxis(EAxis.RotationX);
        settings.makeFixedAxis(EAxis.RotationZ);
        // Limit the Y rotation DOF:
        settings.setLimitedAxis(EAxis.RotationY, 0f, 1.2f);
        RVec3Arg pivotLocation = new RVec3(-1., 0., 0.);
        settings.setPosition1(pivotLocation);
        settings.setPosition2(pivotLocation);
        settings.setSwingType(ESwingType.Pyramid); // default=Cone
        // ESwingType.Cone would result in symmetrical rotation limits!
        constraint = (SixDofConstraint) settings.create(doorBody, frameBody);
        physicsSystem.addConstraint(constraint);

        // Enable the motor for Y rotation and drive it to a target velocity:
        constraint.setMotorState(EAxis.RotationY, EMotorState.Velocity);

        new ConstraintGeometry(constraint, 1).setDepthTest(false);
        new ConstraintGeometry(constraint, 2).setDepthTest(false);
    }
    // *************************************************************************
    // private methods

    /**
     * Create a dynamic rigid body with a box shape and add it to the system.
     *
     * @return the new body
     */
    private Body addDoor() {
        BoxShape shape = new BoxShape(0.8f, 0.8f, 0.1f);

        BodyCreationSettings bcs = new BodyCreationSettings();
        bcs.setAllowSleeping(false); // Disable sleep (deactivation).
        bcs.getMassPropertiesOverride().setMass(0.2f);
        bcs.setOverrideMassProperties(EOverrideMassProperties.CalculateInertia);
        bcs.setShape(shape);

        BodyInterface bi = physicsSystem.getBodyInterface();
        Body result = bi.createBody(bcs);
        bi.addBody(result, EActivation.Activate);

        visualizeShape(result).setColor(Constants.YELLOW);

        return result;
    }

    /**
     * Create a dynamic body with a square-frame shape and add it to the system.
     *
     * @return the new body
     */
    private Body addFrame() {
        float halfLength = 1f;
        float radius = 0.1f;
        CapsuleShapeSettings yShape
                = new CapsuleShapeSettings(halfLength, radius);

        QuatArg y2x = Quat.sEulerAngles(0f, 0f, Jolt.JPH_PI / 2f);
        StaticCompoundShapeSettings frameSettings
                = new StaticCompoundShapeSettings();
        frameSettings.addShape(new Vec3(0f, +1f, 0f), y2x, yShape);
        frameSettings.addShape(new Vec3(0f, -1f, 0f), y2x, yShape);
        frameSettings.addShape(+1f, 0f, 0f, yShape);
        frameSettings.addShape(-1f, 0f, 0f, yShape);
        ConstShape frameShape = frameSettings.create().get();

        BodyCreationSettings bcs = new BodyCreationSettings();
        bcs.setAllowSleeping(false); // Disable sleep (deactivation).
        bcs.getMassPropertiesOverride().setMass(1f);
        bcs.setOverrideMassProperties(EOverrideMassProperties.CalculateInertia);
        bcs.setShape(frameShape);

        BodyInterface bi = physicsSystem.getBodyInterface();
        Body result = bi.createBody(bcs);
        bi.addBody(result, EActivation.Activate);

        visualizeShape(result).setColor(Constants.GREEN);

        return result;
    }

    /**
     * Configure the Camera and CIP during startup.
     */
    private static void configureCamera() {
        CameraInputProcessor cip = getCameraInputProcessor();
        cip.setRotationMode(RotateMode.DragLMB);
        cip.setMoveSpeed(5f);

        cam.setLocation(new Vector3f(0f, 1.5f, 4f));
        cam.setAzimuth(-1.56f);
        cam.setUpAngle(-0.45f);
    }

    /**
     * Configure keyboard input during startup.
     */
    private void configureInput() {
        getInputManager().add(new InputProcessor() {
            @Override
            public void onKeyboard(int glfwKeyId, boolean isPressed) {
                if (glfwKeyId == GLFW.GLFW_KEY_SPACE) {
                    if (isPressed) { // Reverse the motor's direction:
                        Vec3Arg targetVelocity
                                = constraint.getTargetAngularVelocityCs();
                        if (targetVelocity.length() < 0.1f) { // not moving
                            targetVelocity = new Vec3(0f, 1f, 0f);
                        } else {
                            targetVelocity = Op.minus(targetVelocity);
                        }
                        constraint.setTargetAngularVelocityCs(targetVelocity);
                    }
                    return;
                }
                super.onKeyboard(glfwKeyId, isPressed);
            }
        });
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
