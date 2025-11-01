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
import com.github.stephengold.joltjni.BroadPhaseLayerInterfaceTable;
import com.github.stephengold.joltjni.ObjectLayerPairFilterTable;
import com.github.stephengold.joltjni.ObjectVsBroadPhaseLayerFilter;
import com.github.stephengold.joltjni.ObjectVsBroadPhaseLayerFilterTable;
import com.github.stephengold.joltjni.PhysicsSystem;
import com.github.stephengold.joltjni.Plane;
import com.github.stephengold.joltjni.PlaneShape;
import com.github.stephengold.joltjni.SphereShape;
import com.github.stephengold.joltjni.Vec3;
import com.github.stephengold.joltjni.enumerate.EActivation;
import com.github.stephengold.joltjni.enumerate.EMotionType;
import com.github.stephengold.joltjni.readonly.ConstBody;
import com.github.stephengold.joltjni.readonly.ConstPlane;
import com.github.stephengold.joltjni.readonly.ConstShape;
import com.github.stephengold.joltjni.readonly.Vec3Arg;
import com.github.stephengold.sportjolt.BaseApplication;
import com.github.stephengold.sportjolt.physics.BasePhysicsApp;
import com.github.stephengold.sportjolt.physics.FunctionalPhysicsApp;

/**
 * Drop a dynamic sphere onto a horizontal surface and visualize them both using
 * Sport-Jolt graphics.
 * <p>
 * Builds upon HelloJoltJni.
 *
 * @author Stephen Gold sgold@sonic.net
 */
final public class HelloSport {
    // *************************************************************************
    // fields

    /**
     * falling rigid body
     */
    private static ConstBody ball;
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private HelloSport() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Main entry point for the HelloSport application.
     *
     * @param arguments array of command-line arguments (not null)
     */
    public static void main(String[] arguments) {
        FunctionalPhysicsApp fpa = new FunctionalPhysicsApp();

        fpa.setCreateSystem((app) -> {
            // For simplicity, use a single broadphase layer:
            int numBpLayers = 1;

            ObjectLayerPairFilterTable ovoFilter
                    = new ObjectLayerPairFilterTable(
                            BasePhysicsApp.numObjLayers);
            // Enable collisions between 2 moving bodies:
            ovoFilter.enableCollision(BasePhysicsApp.objLayerMoving,
                    BasePhysicsApp.objLayerMoving);
            // Enable collisions between a moving body and a non-moving one:
            ovoFilter.enableCollision(BasePhysicsApp.objLayerMoving,
                    BasePhysicsApp.objLayerNonMoving);
            // Disable collisions between 2 non-moving bodies:
            ovoFilter.disableCollision(BasePhysicsApp.objLayerNonMoving,
                    BasePhysicsApp.objLayerNonMoving);

            // Map both object layers to broadphase layer 0:
            BroadPhaseLayerInterfaceTable layerMap
                    = new BroadPhaseLayerInterfaceTable(
                            BasePhysicsApp.numObjLayers, numBpLayers);
            layerMap.mapObjectToBroadPhaseLayer(
                    BasePhysicsApp.objLayerMoving, 0);
            layerMap.mapObjectToBroadPhaseLayer(
                    BasePhysicsApp.objLayerNonMoving, 0);
            /*
             * Pre-compute the rules for colliding object layers
             * with broadphase layers:
             */
            ObjectVsBroadPhaseLayerFilter ovbFilter
                    = new ObjectVsBroadPhaseLayerFilterTable(
                            layerMap, numBpLayers, ovoFilter,
                            BasePhysicsApp.numObjLayers);

            PhysicsSystem result = new PhysicsSystem();

            // Set high limits, even though this sample app uses only 2 bodies:
            int maxBodies = 5_000;
            int numBodyMutexes = 0; // 0 means "use the default number"
            int maxBodyPairs = 65_536;
            int maxContacts = 20_480;
            result.init(maxBodies, numBodyMutexes, maxBodyPairs, maxContacts,
                    layerMap, ovbFilter, ovoFilter);

            return result;
        });

        fpa.setInitialize((app) -> {
            BaseApplication.setVsync(true);
        });

        fpa.setPopulateSystem((app) -> {
            BodyInterface bi = app.getPhysicsSystem().getBodyInterface();

            // Add a static horizontal plane at y=-1:
            float groundY = -1f;
            Vec3Arg normal = Vec3.sAxisY();
            ConstPlane plane = new Plane(normal, -groundY);
            ConstShape floorShape = new PlaneShape(plane);
            BodyCreationSettings bcs = new BodyCreationSettings();
            bcs.setMotionType(EMotionType.Static);
            bcs.setObjectLayer(BasePhysicsApp.objLayerNonMoving);
            bcs.setShape(floorShape);
            Body floor = bi.createBody(bcs);
            bi.addBody(floor, EActivation.DontActivate);

            // Add a sphere-shaped, dynamic, rigid body at the origin:
            float ballRadius = 0.3f;
            ConstShape ballShape = new SphereShape(ballRadius);
            bcs.setMotionType(EMotionType.Dynamic);
            bcs.setObjectLayer(BasePhysicsApp.objLayerMoving);
            bcs.setShape(ballShape);
            ball = bi.createBody(bcs);
            bi.addBody(ball, EActivation.Activate);

            // Visualize the shapes of both rigid bodies:
            BasePhysicsApp.visualizeShape(floor);
            BasePhysicsApp.visualizeShape(ball);
        });

        fpa.start("HelloSport");
    }
}
