--[[
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
]]

BodyCreationSettings = java.import("com.github.stephengold.joltjni.BodyCreationSettings")
BroadPhaseLayerInterfaceTable = java.import("com.github.stephengold.joltjni.BroadPhaseLayerInterfaceTable")
Jolt = java.import("com.github.stephengold.joltjni.Jolt")
JoltPhysicsObject = java.import("com.github.stephengold.joltjni.JoltPhysicsObject")
JobSystemThreadPool = java.import("com.github.stephengold.joltjni.JobSystemThreadPool")
ObjectLayerPairFilterTable = java.import("com.github.stephengold.joltjni.ObjectLayerPairFilterTable")
ObjectVsBroadPhaseLayerFilterTable = java.import("com.github.stephengold.joltjni.ObjectVsBroadPhaseLayerFilterTable")
PhysicsSystem = java.import("com.github.stephengold.joltjni.PhysicsSystem")
Plane = java.import("com.github.stephengold.joltjni.Plane")
PlaneShape = java.import("com.github.stephengold.joltjni.PlaneShape")
SphereShape = java.import("com.github.stephengold.joltjni.SphereShape")
TempAllocatorMalloc = java.import("com.github.stephengold.joltjni.TempAllocatorMalloc")
Vec3 = java.import("com.github.stephengold.joltjni.Vec3")

EActivation = java.import("com.github.stephengold.joltjni.enumerate.EActivation")
EMotionType = java.import("com.github.stephengold.joltjni.enumerate.EMotionType")

DirectoryPath = java.import("electrostatic4j.snaploader.filesystem.DirectoryPath")
LibraryInfo = java.import("electrostatic4j.snaploader.LibraryInfo")
LoadingCriterion = java.import("electrostatic4j.snaploader.LoadingCriterion")
NativeBinaryLoader = java.import("electrostatic4j.snaploader.NativeBinaryLoader")
NativeDynamicLibrary = java.import("electrostatic4j.snaploader.platform.NativeDynamicLibrary")
PlatformPredicate = java.import("electrostatic4j.snaploader.platform.util.PlatformPredicate")

Runtime = java.import("java.lang.Runtime")

--[[
   Drop a dynamic sphere onto a horizontal surface (non-graphical illustrative
   example).

   author:  Stephen Gold sgold@sonic.net
]]

-- number of object layers:
NUM_OBJ_LAYERS = 2

-- object layer for moving objects:
OBJ_LAYER_MOVING = 0

-- object layer for non-moving objects:
OBJ_LAYER_NONMOVING = 1

-- falling rigid body:
BALL = nil


-- Create the PhysicsSystem. Invoked once during initialization.
function create_system ()

    -- For simplicity, use a single broadphase layer:
    num_bp_layers = 1

    ovo_filter = java.new(ObjectLayerPairFilterTable, NUM_OBJ_LAYERS)
    -- Enable collisions between 2 moving bodies:
    ovo_filter:enableCollision(OBJ_LAYER_MOVING, OBJ_LAYER_MOVING)
    -- Enable collisions between a moving body and a non-moving one:
    ovo_filter:enableCollision(OBJ_LAYER_MOVING, OBJ_LAYER_NONMOVING)
    -- Disable collisions between 2 non-moving bodies:
    ovo_filter:disableCollision(OBJ_LAYER_NONMOVING, OBJ_LAYER_NONMOVING)

    -- Map both object layers to broadphase layer 0:
    layer_map = java.new(BroadPhaseLayerInterfaceTable, NUM_OBJ_LAYERS, num_bp_layers)
    layer_map:mapObjectToBroadPhaseLayer(OBJ_LAYER_MOVING, 0)
    layer_map:mapObjectToBroadPhaseLayer(OBJ_LAYER_NONMOVING, 0)

    -- Rules for colliding object layers with broadphase layers:
    ovb_filter = java.new(ObjectVsBroadPhaseLayerFilterTable,
        layer_map, num_bp_layers, ovo_filter, NUM_OBJ_LAYERS
    )

    result = java.new(PhysicsSystem)

    -- Set high limits, even though this sample app uses only 2 bodies:
    max_bodies = 5000
    num_body_mutexes = 0  -- 0 means "use the default number"
    max_body_pairs = 65536
    max_contacts = 20480
    result:init(
        max_bodies,
        num_body_mutexes,
        max_body_pairs,
        max_contacts,
        layer_map,
        ovb_filter,
        ovo_filter
    )

    return result
end


-- Populate the PhysicsSystem with bodies. Invoked once during initialization.
function populate_system ()

    bi = physics_system:getBodyInterface()

    -- Add a static horizontal plane at y=-1:
    ground_y = -1
    normal = Vec3:sAxisY()
    plane = java.new(Plane, normal, -ground_y)
    floor_shape = java.new(PlaneShape, plane)
    bcs = java.new(BodyCreationSettings)
    bcs:setMotionType(EMotionType.Static)
    bcs:setObjectLayer(OBJ_LAYER_NONMOVING)
    bcs:setShape(floor_shape)
    floor = bi:createBody(bcs)
    bi:addBody(floor, EActivation.DontActivate)

    -- Add a sphere-shaped, dynamic, rigid body at the origin:
    ball_radius = 0.3
    ball_shape = java.new(SphereShape, ball_radius)
    bcs:setMotionType(EMotionType.Dynamic)
    bcs:setObjectLayer(OBJ_LAYER_MOVING)
    bcs:setShape(ball_shape)
    BALL = bi:createBody(bcs)
    bi:addBody(BALL, EActivation.Activate)
end

info = java.new(LibraryInfo, nil, "joltjni", DirectoryPath.USER_DIR)
loader = java.new(NativeBinaryLoader, info)

libraries = java.array(NativeDynamicLibrary, 6)
libraries[1] = java.new(NativeDynamicLibrary, "linux/aarch64/com/github/stephengold",
        PlatformPredicate.LINUX_ARM_64)
libraries[2] = java.new(NativeDynamicLibrary, "linux/armhf/com/github/stephengold",
        PlatformPredicate.LINUX_ARM_32)
libraries[3] = java.new(NativeDynamicLibrary, "linux/x86-64/com/github/stephengold",
        PlatformPredicate.LINUX_X86_64)
libraries[4] = java.new(NativeDynamicLibrary, "osx/aarch64/com/github/stephengold",
        PlatformPredicate.MACOS_ARM_64)
libraries[5] = java.new(NativeDynamicLibrary, "osx/x86-64/com/github/stephengold",
        PlatformPredicate.MACOS_X86_64)
libraries[6] = java.new(NativeDynamicLibrary, "windows/x86-64/com/github/stephengold",
        PlatformPredicate.WIN_X86_64)

loader:registerNativeLibraries(libraries):initPlatformLibrary()
loader:loadLibrary(LoadingCriterion.CLEAN_EXTRACTION)

-- Jolt:setTraceAllocations(true) -- to log Jolt-JNI heap allocations
JoltPhysicsObject:startCleaner()  -- to reclaim native memory
Jolt:registerDefaultAllocator()  -- tell Jolt Physics to use malloc/free
Jolt:installDefaultAssertCallback()
Jolt:installDefaultTraceCallback()
Jolt:newFactory()
Jolt:registerTypes()

physics_system = create_system()
populate_system()
physics_system:optimizeBroadPhase()

temp_allocator = java.new(TempAllocatorMalloc)
num_worker_threads = Runtime:getRuntime():availableProcessors()
job_system = java.new(JobSystemThreadPool,
    Jolt.cMaxPhysicsJobs, Jolt.cMaxPhysicsBarriers, num_worker_threads
)

TIME_PER_STEP = 0.02  -- seconds
for iteration=1,50 do
    COLLISION_STEPS = 1
    physics_system:update(TIME_PER_STEP, COLLISION_STEPS, temp_allocator, job_system)

    location = BALL:getPosition()
    print(location:toString())
end
