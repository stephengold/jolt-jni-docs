/*
 Copyright (c) 2026 Stephen Gold and Yanis Boudiaf

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
package com.github.stephengold.sportjolt.scala.tutorial

import com.github.stephengold.joltjni.Edge
import com.github.stephengold.joltjni.PhysicsSystem
import com.github.stephengold.joltjni.Quat
import com.github.stephengold.joltjni.RVec3
import com.github.stephengold.joltjni.SoftBodyCreationSettings
import com.github.stephengold.joltjni.SoftBodySharedSettings
import com.github.stephengold.joltjni.Vec3
import com.github.stephengold.joltjni.Vertex
import com.github.stephengold.joltjni.enumerate.EActivation
import com.github.stephengold.joltjni.operator.Op
import com.github.stephengold.sportjolt.BaseApplication
import com.github.stephengold.sportjolt.Mesh
import com.github.stephengold.sportjolt.Topology
import com.github.stephengold.sportjolt.mesh.DividedLine
import com.github.stephengold.sportjolt.physics.BasePhysicsApp
import com.github.stephengold.sportjolt.physics.EdgesGeometry
import com.github.stephengold.sportjolt.physics.PinsGeometry
import org.joml.Vector3f

/**
 * A simple rope simulation using a soft body.
 * <p>
 * Builds upon HelloPin.
 *
 * @author Stephen Gold sgold@sonic.net
 */
object HelloSoftRope {
    // *************************************************************************
    // new methods exposed

    /**
     * Main entry point for the HelloSoftRope application.
     *
     * @param arguments array of command-line arguments (not null)
     */
    def main(arguments: Array[String]): Unit = {
        val application = new HelloSoftRope
        application.start
    }
}

class HelloSoftRope extends BasePhysicsApp {
    /**
     * Create the PhysicsSystem. Invoked once during initialization.
     *
     * @return a new object
     */
    override def createSystem: PhysicsSystem = {
        // For simplicity, use a single broadphase layer:
        val maxBodies = 2
        val numBpLayers = 1
        val result = createSystem(maxBodies, numBpLayers)

        return result
    }

    /**
     * Initialize the application. Invoked once.
     */
    override def initialize: Unit = {
        super.initialize
        BaseApplication.setVsync(true)

        // Relocate the camera:
        BaseApplication.cam.setLocation(0f, 1f, 8f)
    }

    /**
     * Populate the PhysicsSystem with bodies. Invoked once during
     * initialization.
     */
    override def populateSystem: Unit = {
        // Generate a subdivided line-segment mesh:
        val numSegments = 40
        val endPoint1 = new Vector3f(0f, 4f, 0f)
        val endPoint2 = new Vector3f(2f, 4f, 2f)
        val lineMesh = new DividedLine(endPoint1, endPoint2, numSegments)

        // Create a soft body and add it to the physics system:
        val sbss = generateSharedSettings(lineMesh)

        // Pin one of the end vertices by zeroing its inverse mass:
        val vertexIndex = 0
        val cornerVertex = sbss.getVertex(vertexIndex)
        cornerVertex.setInvMass(0f)

        sbss.optimize();

        val startLocation = new RVec3(0.0, 0.0, 0.0)
        val sbcs = new SoftBodyCreationSettings(
                sbss, startLocation, new Quat, BasePhysicsApp.objLayerMoving)

        val bi = physicsSystem.getBodyInterface
        val rope = bi.createSoftBody(sbcs)
        bi.addBody(rope, EActivation.Activate)

        // Visualize the soft-body edges and the pin:
        new EdgesGeometry(rope)
        new PinsGeometry(rope)
    }
    // *************************************************************************
    // private methods

    /**
     * Generate a shared-settings object using the positions and lines in the
     * specified LineList mesh.
     *
     * @param mesh the mesh to use (not null, unaffected)
     * @return a new object
     */
    private def generateSharedSettings(mesh: Mesh): SoftBodySharedSettings = {
        assert (mesh.topology == Topology.LineList)

        val result = new SoftBodySharedSettings

        val locations = mesh.getPositions
        val numVertices = locations.capacity / 3
        val tmpLocation = new Vec3
        val tmpVertex = new Vertex
        for (i <- 0 to numVertices - 1) {
            locations.get(3 * i, tmpLocation)
            tmpVertex.setPosition(tmpLocation)
            result.addVertex(tmpVertex)
        }

        val indices = mesh.getIndexBuffer
        val numEdges = indices.capacity / Mesh.vpe
        val tmpEdge = new Edge
        val tmpLocation2 = new Vec3
        for (i <- 0 to numEdges - 1) {
            val vertexIndex1 = indices.get(Mesh.vpe * i)
            tmpEdge.setVertex(0, vertexIndex1)

            val vertexIndex2 = indices.get(Mesh.vpe * i + 1)
            tmpEdge.setVertex(1, vertexIndex2)

            locations.get(3 * vertexIndex1, tmpLocation)
            locations.get(3 * vertexIndex2, tmpLocation2)
            val offset = Op.minus(tmpLocation2, tmpLocation)
            val length = offset.length
            tmpEdge.setRestLength(length)

            result.addEdgeConstraint(tmpEdge)
        }

        return result
    }
}
