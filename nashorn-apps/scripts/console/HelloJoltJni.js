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

/*
 * Drop a dynamic sphere onto a horizontal surface (non-graphical illustrative
 * example).
 *
 * author:  Stephen Gold sgold@sonic.net
 */

// Import Java classes:
var Runtime = Java.type('java.lang.Runtime');

// number of object layers:
var numObjLayers = 2;

// object layer for moving objects:
var objLayerMoving = 0;

// object layer for non-moving objects:
var objLayerNonMoving = 1;

// falling rigid body
var ball;

// system to simulate
var physicsSystem;

var info = new LibraryInfo(null, "joltjni", DirectoryPath.USER_DIR);
var loader = new NativeBinaryLoader(info);
var libraries = new Array(
        new NativeDynamicLibrary("linux/aarch64/com/github/stephengold", PlatformPredicate.LINUX_ARM_64),
        new NativeDynamicLibrary("linux/armhf/com/github/stephengold", PlatformPredicate.LINUX_ARM_32),
        new NativeDynamicLibrary("linux/x86-64/com/github/stephengold", PlatformPredicate.LINUX_X86_64),
        new NativeDynamicLibrary("osx/aarch64/com/github/stephengold", PlatformPredicate.MACOS_ARM_64),
        new NativeDynamicLibrary("osx/x86-64/com/github/stephengold", PlatformPredicate.MACOS_X86_64),
        new NativeDynamicLibrary("windows/x86-64/com/github/stephengold", PlatformPredicate.WIN_X86_64)
        );
loader.registerNativeLibraries(libraries).initPlatformLibrary();
loader.loadLibrary(LoadingCriterion.CLEAN_EXTRACTION);

//Jolt.setTraceAllocations(true); // to log Jolt-JNI heap allocations
JoltPhysicsObject.startCleaner(); // to reclaim native memory
Jolt.registerDefaultAllocator(); // tell Jolt Physics to use malloc/free
Jolt.installDefaultAssertCallback();
Jolt.installDefaultTraceCallback();
Jolt.newFactory();
Jolt.registerTypes();

var physicsSystem = createSystem();
populateSystem();
physicsSystem.optimizeBroadPhase();

var tempAllocator = new TempAllocatorMalloc();
var numWorkerThreads = Runtime.getRuntime().availableProcessors();
var jobSystem = new JobSystemThreadPool(Jolt.cMaxPhysicsJobs, Jolt.cMaxPhysicsBarriers, numWorkerThreads);

var timePerStep = 0.02; // seconds
for (var iteration = 0; iteration < 50; ++iteration) {
    var collisionSteps = 1;
    physicsSystem.update(timePerStep, collisionSteps, tempAllocator, jobSystem);

    var location = ball.getPosition();
    print(location);
}

// *************************************************************************
// functions

/*
 * Create the PhysicsSystem. Invoked once during initialization.
 */
function createSystem() {
    // For simplicity, use a single broadphase layer:
    var numBpLayers = 1;

    var ovoFilter = new ObjectLayerPairFilterTable(numObjLayers);
    // Enable collisions between 2 moving bodies:
    ovoFilter.enableCollision(objLayerMoving, objLayerMoving);
    // Enable collisions between a moving body and a non-moving one:
    ovoFilter.enableCollision(objLayerMoving, objLayerNonMoving);
    // Disable collisions between 2 non-moving bodies:
    ovoFilter.disableCollision(objLayerNonMoving, objLayerNonMoving);

    // Map both object layers to broadphase layer 0:
    var layerMap = new BroadPhaseLayerInterfaceTable(numObjLayers, numBpLayers);
    layerMap.mapObjectToBroadPhaseLayer(objLayerMoving, 0);
    layerMap.mapObjectToBroadPhaseLayer(objLayerNonMoving, 0);

    // Rules for colliding object layers with broadphase layers:
    var ovbFilter = new ObjectVsBroadPhaseLayerFilterTable(
            layerMap, numBpLayers, ovoFilter, numObjLayers);

    var result = new PhysicsSystem();

    // Set high limits, even though this sample app uses only 2 bodies:
    var maxBodies = 5000;
    var numBodyMutexes = 0; // 0 means "use the default number"
    var maxBodyPairs = 65536;
    var maxContacts = 20480;
    result.init(maxBodies, numBodyMutexes, maxBodyPairs, maxContacts, layerMap, ovbFilter, ovoFilter);

    return result;
}

/*
 * Populate the PhysicsSystem with bodies. Invoked once during initialization.
 */
function populateSystem() {
    var bi = physicsSystem.getBodyInterface();

    // Add a static horizontal plane at y=-1:
    var groundY = -1;
    var normal = Vec3.sAxisY();
    var plane = new Plane(normal, -groundY);
    var floorShape = new PlaneShape(plane);
    var bcs = new BodyCreationSettings();
    bcs.setMotionType(EMotionType.Static);
    bcs.setObjectLayer(objLayerNonMoving);
    bcs.setShape(floorShape);
    var floor = bi.createBody(bcs);
    bi.addBody(floor, EActivation.DontActivate);

    // Add a sphere-shaped, dynamic, rigid body at the origin:
    var ballRadius = 0.3;
    var ballShape = new SphereShape(ballRadius);
    bcs.setMotionType(EMotionType.Dynamic);
    bcs.setObjectLayer(objLayerMoving);
    bcs.setShape(ballShape);
    ball = bi.createBody(bcs);
    bi.addBody(ball, EActivation.Activate);
}
