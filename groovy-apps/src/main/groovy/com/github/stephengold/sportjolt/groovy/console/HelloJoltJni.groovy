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
package com.github.stephengold.sportjolt.groovy.console

import com.github.stephengold.joltjni.Body
import com.github.stephengold.joltjni.BodyCreationSettings
import com.github.stephengold.joltjni.BodyInterface
import com.github.stephengold.joltjni.BroadPhaseLayerInterfaceTable
import com.github.stephengold.joltjni.JobSystem
import com.github.stephengold.joltjni.JobSystemThreadPool
import com.github.stephengold.joltjni.Jolt
import com.github.stephengold.joltjni.JoltPhysicsObject
import com.github.stephengold.joltjni.ObjectLayerPairFilterTable
import com.github.stephengold.joltjni.ObjectVsBroadPhaseLayerFilter
import com.github.stephengold.joltjni.ObjectVsBroadPhaseLayerFilterTable
import com.github.stephengold.joltjni.PhysicsSystem
import com.github.stephengold.joltjni.Plane
import com.github.stephengold.joltjni.PlaneShape
import com.github.stephengold.joltjni.SphereShape
import com.github.stephengold.joltjni.TempAllocator
import com.github.stephengold.joltjni.TempAllocatorMalloc
import com.github.stephengold.joltjni.Vec3
import com.github.stephengold.joltjni.enumerate.EActivation
import com.github.stephengold.joltjni.enumerate.EMotionType
import com.github.stephengold.joltjni.enumerate.EPhysicsUpdateError
import com.github.stephengold.joltjni.readonly.ConstBody
import com.github.stephengold.joltjni.readonly.ConstPlane
import com.github.stephengold.joltjni.readonly.ConstShape
import com.github.stephengold.joltjni.readonly.RVec3Arg
import com.github.stephengold.joltjni.readonly.Vec3Arg
import electrostatic4j.snaploader.LibraryInfo
import electrostatic4j.snaploader.LoadingCriterion
import electrostatic4j.snaploader.NativeBinaryLoader
import electrostatic4j.snaploader.filesystem.DirectoryPath
import electrostatic4j.snaploader.platform.NativeDynamicLibrary
import electrostatic4j.snaploader.platform.util.PlatformPredicate
import groovy.transform.CompileStatic

/**
 * Drop a dynamic sphere onto a horizontal surface (non-graphical illustrative
 * example).
 *
 * @author Stephen Gold sgold@sonic.net
 */
@CompileStatic
final public class HelloJoltJni {

    // *************************************************************************
    // constants

    /**
     * number of object layers
     */
    final private static int NUM_OBJ_LAYERS = 2
    /**
     * object layer for moving objects
     */
    final private static int OBJ_LAYER_MOVING = 0
    /**
     * object layer for non-moving objects
     */
    final private static int OBJ_LAYER_NONMOVING = 1
    // *************************************************************************
    // fields

    /**
     * falling rigid body
     */
    private static ConstBody ball
    /**
     * system to simulate
     */
    private static PhysicsSystem physicsSystem
    // *************************************************************************
    // new methods exposed

    /**
     * Main entry point for the HelloJoltJni application.
     *
     * @param arguments array of command-line arguments (not null)
     */
    static void main(String[] arguments) {
        var info = new LibraryInfo(null, 'joltjni', DirectoryPath.USER_DIR)
        var loader = new NativeBinaryLoader(info)

        NativeDynamicLibrary[] libraries = [
            new NativeDynamicLibrary('linux/aarch64/com/github/stephengold',
                PlatformPredicate.LINUX_ARM_64),
            new NativeDynamicLibrary('linux/armhf/com/github/stephengold',
                PlatformPredicate.LINUX_ARM_32),
            new NativeDynamicLibrary('linux/x86-64/com/github/stephengold',
                PlatformPredicate.LINUX_X86_64),
            new NativeDynamicLibrary('osx/aarch64/com/github/stephengold',
                PlatformPredicate.MACOS_ARM_64),
            new NativeDynamicLibrary('osx/x86-64/com/github/stephengold',
                PlatformPredicate.MACOS_X86_64),
            new NativeDynamicLibrary('windows/x86-64/com/github/stephengold',
                PlatformPredicate.WIN_X86_64)
        ]
        loader.registerNativeLibraries(libraries).initPlatformLibrary()
        try {
            loader.loadLibrary(LoadingCriterion.CLEAN_EXTRACTION)
        } catch (Exception exception) {
            throw new IllegalStateException(
                    'Failed to load a Jolt-JNI native library!')
        }

        //Jolt.setTraceAllocations(true) // to log Jolt-JNI heap allocations
        JoltPhysicsObject.startCleaner() // to reclaim native memory
        Jolt.registerDefaultAllocator() // tell Jolt Physics to use malloc/free
        Jolt.installDefaultAssertCallback()
        Jolt.installDefaultTraceCallback()
        var success = Jolt.newFactory()
        assert success
        Jolt.registerTypes()

        physicsSystem = createSystem()
        populateSystem()
        physicsSystem.optimizeBroadPhase()

        var tempAllocator = new TempAllocatorMalloc()
        var numWorkerThreads = Runtime.runtime.availableProcessors()
        var jobSystem = new JobSystemThreadPool(Jolt.cMaxPhysicsJobs,
            Jolt.cMaxPhysicsBarriers, numWorkerThreads)

        var timePerStep = 0.02f // in seconds
        for (int iteration = 0; iteration < 50; ++iteration) {
            var collisionSteps = 1
            var errors = physicsSystem.update(
                timePerStep, collisionSteps, tempAllocator, jobSystem)
            assert errors == EPhysicsUpdateError.None : errors

            var location = ball.position
            System.out.println(location)
        }
    }
    // *************************************************************************
    // private methods

    /**
     * Create the PhysicsSystem. Invoked once during initialization.
     *
     * @return a new object
     */
    private static PhysicsSystem createSystem() {
        // For simplicity, use a single broadphase layer:
        var numBpLayers = 1

        var ovoFilter = new ObjectLayerPairFilterTable(NUM_OBJ_LAYERS)
        // Enable collisions between 2 moving bodies:
        ovoFilter.enableCollision(OBJ_LAYER_MOVING, OBJ_LAYER_MOVING)
        // Enable collisions between a moving body and a non-moving one:
        ovoFilter.enableCollision(OBJ_LAYER_MOVING, OBJ_LAYER_NONMOVING)
        // Disable collisions between 2 non-moving bodies:
        ovoFilter.disableCollision(OBJ_LAYER_NONMOVING, OBJ_LAYER_NONMOVING)

        // Map both object layers to broadphase layer 0:
        var layerMap = new BroadPhaseLayerInterfaceTable(NUM_OBJ_LAYERS, numBpLayers)
        layerMap.mapObjectToBroadPhaseLayer(OBJ_LAYER_MOVING, 0)
        layerMap.mapObjectToBroadPhaseLayer(OBJ_LAYER_NONMOVING, 0)

        // Rules for colliding object layers with broadphase layers:
        var ovbFilter = new ObjectVsBroadPhaseLayerFilterTable(
            layerMap, numBpLayers, ovoFilter, NUM_OBJ_LAYERS)

        var result = new PhysicsSystem()

        // Set high limits, even though this sample app uses only 2 bodies:
        var maxBodies = 5_000
        var numBodyMutexes = 0 // 0 means "use the default number"
        var maxBodyPairs = 65_536
        var maxContacts = 20_480
        result.init(maxBodies, numBodyMutexes, maxBodyPairs, maxContacts,
            layerMap, ovbFilter, ovoFilter)

        return result
    }

    /**
     * Populate the PhysicsSystem with bodies. Invoked once during
     * initialization.
     */
    private static void populateSystem() {
        var bi = physicsSystem.bodyInterface

        // Add a static horizontal plane at y=-1:
        var groundY = -1f
        var normal = Vec3.sAxisY()
        var plane = new Plane(normal, -groundY)
        var floorShape = new PlaneShape(plane)
        var bcs = new BodyCreationSettings()
        bcs.motionType = EMotionType.Static
        bcs.objectLayer = OBJ_LAYER_NONMOVING
        bcs.shape = floorShape
        var floor = bi.createBody(bcs)
        bi.addBody(floor, EActivation.DontActivate)

        // Add a sphere-shaped, dynamic, rigid body at the origin:
        var ballRadius = 0.3f
        var ballShape = new SphereShape(ballRadius)
        bcs.motionType = EMotionType.Dynamic
        bcs.objectLayer = OBJ_LAYER_MOVING
        bcs.shape = ballShape
        ball = bi.createBody(bcs)
        bi.addBody(ball, EActivation.Activate)
    }

}
