/*
 Copyright (c) 2025 Stephen Gold and Yanis Boudiaf

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
package com.github.stephengold.sportjolt.ktapps.console

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
import electrostatic4j.snaploader.LibraryInfo
import electrostatic4j.snaploader.LoadingCriterion
import electrostatic4j.snaploader.NativeBinaryLoader
import electrostatic4j.snaploader.filesystem.DirectoryPath
import electrostatic4j.snaploader.platform.NativeDynamicLibrary
import electrostatic4j.snaploader.platform.util.PlatformPredicate

/*
 * Drop a dynamic sphere onto a horizontal surface (non-graphical illustrative
 * example).
 *
 * author:  Stephen Gold sgold@sonic.net
 */

private const val BALL_RADIUS = 0.3f
private const val GROUND_Y = -1f

private const val MAX_BODIES = 5_000
private const val NUM_BODY_MUTEXES = 0  // 0 means "use the default number"
private const val MAX_BODY_PAIRS = 65_536
private const val MAX_CONTACTS = 20_480

// For simplicity, use a single broadphase layer:
private const val NUM_BP_LAYERS = 1

private const val NUM_OBJECT_LAYERS = 2
private const val OBJ_LAYER_MOVING = 0
private const val OBJ_LAYER_NON_MOVING = 1
private const val TIME_PER_STEP = 0.02f  // seconds

private var ball: Body? = null
private var physicsSystem: PhysicsSystem? = null

/*
 * Main entry point for the HelloJoltJniKt application.
 */
fun main() {
    val info = LibraryInfo(null, "joltjni", DirectoryPath.USER_DIR)
    val loader = NativeBinaryLoader(info)

    val libraries = arrayOf(
        NativeDynamicLibrary("linux/aarch64/com/github/stephengold", PlatformPredicate.LINUX_ARM_64),
        NativeDynamicLibrary("linux/armhf/com/github/stephengold", PlatformPredicate.LINUX_ARM_32),
        NativeDynamicLibrary("linux/x86-64/com/github/stephengold", PlatformPredicate.LINUX_X86_64),
        NativeDynamicLibrary("osx/aarch64/com/github/stephengold", PlatformPredicate.MACOS_ARM_64),
        NativeDynamicLibrary("osx/x86-64/com/github/stephengold", PlatformPredicate.MACOS_X86_64),
        NativeDynamicLibrary("windows/x86-64/com/github/stephengold", PlatformPredicate.WIN_X86_64)
    )
    loader.registerNativeLibraries(libraries).initPlatformLibrary()
    try {
        loader.loadLibrary(LoadingCriterion.CLEAN_EXTRACTION)
    } catch (exception: Exception) {
        throw IllegalStateException("Failed to load a Jolt-JNI native library!")
    }

    //Jolt.setTraceAllocations(true) // to log Jolt-JNI heap allocations
    JoltPhysicsObject.startCleaner() // to reclaim native memory automatically
    Jolt.registerDefaultAllocator() // tell Jolt Physics to use malloc/free
    Jolt.installDefaultAssertCallback()
    Jolt.installDefaultTraceCallback()
    val success = Jolt.newFactory()
    assert(success)
    Jolt.registerTypes()

    physicsSystem = createSystem()
    populateSystem()
    physicsSystem!!.optimizeBroadPhase()

    val tempAllocator = TempAllocatorMalloc()
    val numWorkerThreads = Runtime.getRuntime().availableProcessors()
    val jobSystem = JobSystemThreadPool(
            Jolt.cMaxPhysicsJobs, Jolt.cMaxPhysicsBarriers, numWorkerThreads)

    for (iteration in 0 ..< 50) {
        val collisionSteps = 1
        val errors = physicsSystem!!.update(
            TIME_PER_STEP, collisionSteps, tempAllocator, jobSystem)
        assert(errors == EPhysicsUpdateError.None)
        val location = ball!!.getPosition()
        println(location)
    }
}

/*
 * Create the PhysicsSystem. Invoked once during initialization.
 */
private fun createSystem(): PhysicsSystem {
    val ovoFilter = ObjectLayerPairFilterTable(NUM_OBJECT_LAYERS)
    // Enable collisions between 2 moving bodies:
    ovoFilter.enableCollision(OBJ_LAYER_MOVING, OBJ_LAYER_MOVING)
    // Enable collisions between a moving body and a non-moving one:
    ovoFilter.enableCollision(OBJ_LAYER_MOVING, OBJ_LAYER_NON_MOVING)
    // Disable collisions between 2 non-moving bodies:
    ovoFilter.disableCollision(OBJ_LAYER_NON_MOVING, OBJ_LAYER_NON_MOVING)

    // Map both object layers to broadphase layer 0:
    val layerMap = BroadPhaseLayerInterfaceTable(NUM_OBJECT_LAYERS, NUM_BP_LAYERS)
    layerMap.mapObjectToBroadPhaseLayer(OBJ_LAYER_MOVING, 0)
    layerMap.mapObjectToBroadPhaseLayer(OBJ_LAYER_NON_MOVING, 0)

    // Rules for colliding object layers with broadphase layers:
    val ovbFilter = ObjectVsBroadPhaseLayerFilterTable(
        layerMap, NUM_BP_LAYERS, ovoFilter, NUM_OBJECT_LAYERS)

    val result = PhysicsSystem()

    // Set high limits, even though this sample app uses only 2 bodies:
    result.init(MAX_BODIES, NUM_BODY_MUTEXES, MAX_BODY_PAIRS, MAX_CONTACTS,
        layerMap, ovbFilter, ovoFilter)

    return result
}

/*
 * Populate the PhysicsSystem with bodies. Invoked once during initialization.
 */
private fun populateSystem() {
    val bi = physicsSystem!!.getBodyInterface()

    // Add a static horizontal plane at y=-1:
    val normal = Vec3.sAxisY()
    val plane = Plane(normal, GROUND_Y)
    val floorShape = PlaneShape(plane)
    val bcs = BodyCreationSettings()
    bcs.setMotionType(EMotionType.Static)
    bcs.setObjectLayer(OBJ_LAYER_NON_MOVING)
    bcs.setShape(floorShape)
    val floor = bi.createBody(bcs)
    bi.addBody(floor, EActivation.DontActivate)

    // Add a sphere-shaped, dynamic, rigid body at the origin:
    val ballShape = SphereShape(BALL_RADIUS)
    bcs.setMotionType(EMotionType.Dynamic)
    bcs.setObjectLayer(OBJ_LAYER_MOVING)
    bcs.setShape(ballShape)
    ball = bi.createBody(bcs)
    bi.addBody(ball, EActivation.Activate)
}
