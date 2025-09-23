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
package com.github.stephengold.sportjolt.scala.console

import com.github.stephengold.joltjni.Body
import com.github.stephengold.joltjni.BodyCreationSettings
import com.github.stephengold.joltjni.BroadPhaseLayerInterfaceTable
import com.github.stephengold.joltjni.JobSystemThreadPool
import com.github.stephengold.joltjni.Jolt
import com.github.stephengold.joltjni.JoltPhysicsObject
import com.github.stephengold.joltjni.ObjectLayerPairFilterTable
import com.github.stephengold.joltjni.ObjectVsBroadPhaseLayerFilterTable
import com.github.stephengold.joltjni.PhysicsSystem
import com.github.stephengold.joltjni.Plane
import com.github.stephengold.joltjni.PlaneShape
import com.github.stephengold.joltjni.SphereShape
import com.github.stephengold.joltjni.TempAllocatorMalloc
import com.github.stephengold.joltjni.Vec3
import com.github.stephengold.joltjni.enumerate.EActivation
import com.github.stephengold.joltjni.enumerate.EMotionType
import com.github.stephengold.joltjni.enumerate.EPhysicsUpdateError
import com.github.stephengold.joltjni.readonly.ConstBody
import electrostatic4j.snaploader.LibraryInfo
import electrostatic4j.snaploader.LoadingCriterion
import electrostatic4j.snaploader.NativeBinaryLoader
import electrostatic4j.snaploader.filesystem.DirectoryPath
import electrostatic4j.snaploader.platform.NativeDynamicLibrary
import electrostatic4j.snaploader.platform.util.PlatformPredicate

/**
 * Drop a dynamic sphere onto a horizontal surface (non-graphical illustrative
 * example).
 *
 * @author Stephen Gold sgold@sonic.net
 */
object HelloJoltJni {
    // *************************************************************************
    // constants

    /**
     * number of object layers
     */
    private val numObjLayers = 2
    /**
     * object layer for moving objects
     */
    private val objLayerMoving = 0
    /**
     * object layer for non-moving objects
     */
    private val objLayerNonMoving = 1
    // *************************************************************************
    // fields

    /**
     * falling rigid body
     */
    private var ball: ConstBody = null
    /**
     * system to simulate
     */
    private var physicsSystem: PhysicsSystem = null
    // *************************************************************************
    // new methods exposed

    /**
     * Main entry point for the HelloJoltJni application.
     *
     * @param arguments array of command-line arguments (not null)
     */
    def main(arguments: Array[String]): Unit = {
        val info = new LibraryInfo(null, "joltjni", DirectoryPath.USER_DIR)
        val loader = new NativeBinaryLoader(info)

        val libraries = Array(
            new NativeDynamicLibrary("linux/aarch64/com/github/stephengold",
                    PlatformPredicate.LINUX_ARM_64),
            new NativeDynamicLibrary("linux/armhf/com/github/stephengold",
                    PlatformPredicate.LINUX_ARM_32),
            new NativeDynamicLibrary("linux/x86-64/com/github/stephengold",
                    PlatformPredicate.LINUX_X86_64),
            new NativeDynamicLibrary("osx/aarch64/com/github/stephengold",
                    PlatformPredicate.MACOS_ARM_64),
            new NativeDynamicLibrary("osx/x86-64/com/github/stephengold",
                    PlatformPredicate.MACOS_X86_64),
            new NativeDynamicLibrary("windows/x86-64/com/github/stephengold",
                    PlatformPredicate.WIN_X86_64)
        )
        loader.registerNativeLibraries(libraries).initPlatformLibrary
        try
            loader.loadLibrary(LoadingCriterion.CLEAN_EXTRACTION)
        catch {
            case exception: Exception =>
                throw new IllegalStateException(
                    "Failed to load a Jolt-JNI native library!")
        }

        //Jolt.setTraceAllocations(true) // to log Jolt-JNI heap allocations
        JoltPhysicsObject.startCleaner // to reclaim native memory
        Jolt.registerDefaultAllocator // tell Jolt Physics to use malloc/free
        Jolt.installDefaultAssertCallback
        Jolt.installDefaultTraceCallback
        val success = Jolt.newFactory
        assert(success)
        Jolt.registerTypes

        physicsSystem = createSystem
        populateSystem
        physicsSystem.optimizeBroadPhase

        val tempAllocator = new TempAllocatorMalloc
        val numWorkerThreads = Runtime.getRuntime.availableProcessors
        val jobSystem = new JobSystemThreadPool(Jolt.cMaxPhysicsJobs,
                Jolt.cMaxPhysicsBarriers, numWorkerThreads)

        val timePerStep = 0.02f // in seconds
        for (iteration <- 0 to 49) {
            val collisionSteps = 1
            val errors = physicsSystem.update(
                    timePerStep, collisionSteps, tempAllocator, jobSystem)
            assert(errors == EPhysicsUpdateError.None)

            val location = ball.getPosition
            println(location)
        }
    }
    // *************************************************************************
    // private methods

    /**
     * Create the PhysicsSystem. Invoked once during initialization.
     *
     * @return a new object
     */
    private def createSystem: PhysicsSystem = {
        // For simplicity, use a single broadphase layer:
        val numBpLayers = 1

        val ovoFilter = new ObjectLayerPairFilterTable(numObjLayers)
        // Enable collisions between 2 moving bodies:
        ovoFilter.enableCollision(objLayerMoving, objLayerMoving)
        // Enable collisions between a moving body and a non-moving one:
        ovoFilter.enableCollision(objLayerMoving, objLayerNonMoving)
        // Disable collisions between 2 non-moving bodies:
        ovoFilter.disableCollision(objLayerNonMoving, objLayerNonMoving)

        // Map both object layers to broadphase layer 0:
        val layerMap = new BroadPhaseLayerInterfaceTable(numObjLayers, numBpLayers)
        layerMap.mapObjectToBroadPhaseLayer(objLayerMoving, 0)
        layerMap.mapObjectToBroadPhaseLayer(objLayerNonMoving, 0)

        // Rules for colliding object layers with broadphase layers:
        val ovbFilter = new ObjectVsBroadPhaseLayerFilterTable(
                        layerMap, numBpLayers, ovoFilter, numObjLayers)

        val result = new PhysicsSystem

        // Set high limits, even though this sample app uses only 2 bodies:
        val maxBodies = 5_000
        val numBodyMutexes = 0 // 0 means "use the default number"
        val maxBodyPairs = 65_536
        val maxContacts = 20_480
        result.init(maxBodies, numBodyMutexes, maxBodyPairs, maxContacts,
                layerMap, ovbFilter, ovoFilter)
        result
    }

    /**
     * Populate the PhysicsSystem with bodies. Invoked once during
     * initialization.
     */
    private def populateSystem: Unit = {
        val bi = physicsSystem.getBodyInterface

        // Add a static horizontal plane at y=-1:
        val groundY = -1f
        val normal = Vec3.sAxisY
        val plane = new Plane(normal, -groundY)
        val floorShape = new PlaneShape(plane)
        val bcs = new BodyCreationSettings
        bcs.setMotionType(EMotionType.Static)
        bcs.setObjectLayer(objLayerNonMoving)
        bcs.setShape(floorShape)
        val floor = bi.createBody(bcs)
        bi.addBody(floor, EActivation.DontActivate)

        // Add a sphere-shaped, dynamic, rigid body at the origin:
        val ballRadius = 0.3f
        val ballShape = new SphereShape(ballRadius)
        bcs.setMotionType(EMotionType.Dynamic)
        bcs.setObjectLayer(objLayerMoving)
        bcs.setShape(ballShape)
        ball = bi.createBody(bcs)
        bi.addBody(ball, EActivation.Activate)
    }
}
