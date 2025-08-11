"""
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
"""

"""
 * Drop a dynamic sphere onto a horizontal surface (non-graphical illustrative
 * example).
 *
 * author:  Stephen Gold sgold@sonic.net
"""

# Import Java classes:
from java.lang import Runtime

# number of object layers:
numObjLayers = 2

# object layer for moving objects:
objLayerMoving = 0

# object layer for non-moving objects:
objLayerNonMoving = 1

# Create the PhysicsSystem. Invoked once during initialization.
def createSystem():
    # For simplicity, use a single broadphase layer:
    numBpLayers = 1

    ovoFilter = ObjectLayerPairFilterTable(numObjLayers)
    # Enable collisions between 2 moving bodies:
    ovoFilter.enableCollision(objLayerMoving, objLayerMoving)
    # Enable collisions between a moving body and a non-moving one:
    ovoFilter.enableCollision(objLayerMoving, objLayerNonMoving)
    # Disable collisions between 2 non-moving bodies:
    ovoFilter.disableCollision(objLayerNonMoving, objLayerNonMoving)

    # Map both object layers to broadphase layer 0:
    layerMap = BroadPhaseLayerInterfaceTable(numObjLayers, numBpLayers)
    layerMap.mapObjectToBroadPhaseLayer(objLayerMoving, 0)
    layerMap.mapObjectToBroadPhaseLayer(objLayerNonMoving, 0)

    # Rules for colliding object layers with broadphase layers:
    ovbFilter = ObjectVsBroadPhaseLayerFilterTable(layerMap, numBpLayers, ovoFilter, numObjLayers)

    result = PhysicsSystem()

    # Set high limits, even though this sample app uses only 2 bodies:
    maxBodies = 5000
    numBodyMutexes = 0 # 0 means "use the default number"
    maxBodyPairs = 65536
    maxContacts = 20480
    result.init(maxBodies, numBodyMutexes, maxBodyPairs, maxContacts, layerMap, ovbFilter, ovoFilter)

    return result

# Populate the PhysicsSystem with bodies. Invoked once during initialization.
def populateSystem():
    bi = physicsSystem.getBodyInterface()

    # Add a static horizontal plane at y=-1:
    groundY = -1
    normal = Vec3.sAxisY()
    plane = Plane(normal, -groundY)
    floorShape = PlaneShape(plane)
    bcs = BodyCreationSettings()
    bcs.setMotionType(EMotionType.Static)
    bcs.setObjectLayer(objLayerNonMoving)
    bcs.setShape(floorShape)
    floor = bi.createBody(bcs)
    bi.addBody(floor, EActivation.DontActivate)

    # Add a sphere-shaped, dynamic, rigid body at the origin:
    ballRadius = 0.3
    ballShape = SphereShape(ballRadius)
    bcs.setMotionType(EMotionType.Dynamic)
    bcs.setObjectLayer(objLayerMoving)
    bcs.setShape(ballShape)
    global ball
    ball = bi.createBody(bcs)
    bi.addBody(ball, EActivation.Activate)


info = LibraryInfo(None, "joltjni", DirectoryPath.USER_DIR)
loader = NativeBinaryLoader(info)

libraries = [
    NativeDynamicLibrary("linux/aarch64/com/github/stephengold", PlatformPredicate.LINUX_ARM_64),
    NativeDynamicLibrary("linux/armhf/com/github/stephengold", PlatformPredicate.LINUX_ARM_32),
    NativeDynamicLibrary("linux/x86-64/com/github/stephengold", PlatformPredicate.LINUX_X86_64),
    NativeDynamicLibrary("osx/aarch64/com/github/stephengold", PlatformPredicate.MACOS_ARM_64),
    NativeDynamicLibrary("osx/x86-64/com/github/stephengold", PlatformPredicate.MACOS_X86_64),
    NativeDynamicLibrary("windows/x86-64/com/github/stephengold", PlatformPredicate.WIN_X86_64)
]

loader.registerNativeLibraries(libraries).initPlatformLibrary()
loader.loadLibrary(LoadingCriterion.CLEAN_EXTRACTION)

#Jolt.setTraceAllocations(true) # to log Jolt-JNI heap allocations
JoltPhysicsObject.startCleaner() # to reclaim native memory
Jolt.registerDefaultAllocator() # tell Jolt Physics to use malloc/free
Jolt.installDefaultAssertCallback()
Jolt.installDefaultTraceCallback()
Jolt.newFactory()
Jolt.registerTypes()

physicsSystem = createSystem()
populateSystem()
physicsSystem.optimizeBroadPhase()

tempAllocator = TempAllocatorMalloc()
numWorkerThreads = Runtime.getRuntime().availableProcessors()
jobSystem = JobSystemThreadPool(Jolt.cMaxPhysicsJobs, Jolt.cMaxPhysicsBarriers, numWorkerThreads)

timePerStep = 0.02 # seconds
for iteration in range(0, 49):
    collisionSteps = 1
    physicsSystem.update(timePerStep, collisionSteps, tempAllocator, jobSystem)

    location = ball.getPosition()
    print(location)
