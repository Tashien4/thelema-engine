/*
 * Copyright 2020 Anton Trushkov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ksdfv.thelema.mesh.build

import org.ksdfv.thelema.data.DATA
import org.ksdfv.thelema.data.IByteData
import org.ksdfv.thelema.data.IFloatData
import org.ksdfv.thelema.data.IShortData
import org.ksdfv.thelema.g3d.IMaterial
import org.ksdfv.thelema.gl.GL_UNSIGNED_SHORT
import org.ksdfv.thelema.math.Vec3
import org.ksdfv.thelema.mesh.*

/** @author zeganstyl */
open class MeshBuilder {
    open var textureCoordinates: Boolean = true
    open var normals: Boolean = true

    open var textureCoordinatesScale: Float = 1f

    var material: IMaterial = IMaterial.Default

    lateinit var currentFloatBuffer: IFloatData

    fun createAttributes(): VertexAttributes {
        val attributes = VertexAttributes(VertexAttribute.Position)
        if (textureCoordinates) attributes.add(VertexAttribute.UV[0])
        if (normals) attributes.add(VertexAttribute.Normal)
        return attributes
    }

    fun createVertices(verticesNum: Int, block: IByteData.() -> Unit): IVertexBuffer {
        val attributes = createAttributes()
        return VertexArrayObject(
            attributes,
            DATA.bytes(verticesNum * attributes.bytesPerVertex).apply(block)
        )
    }

    fun createVerticesFloat(verticesNum: Int, block: IFloatData.() -> Unit) =
        createVertices(verticesNum) {
            currentFloatBuffer = floatView()
            currentFloatBuffer.apply(block)
        }

    fun createIndices(indicesNum: Int, type: Int, block: IByteData.() -> Unit): IndexBufferObject {
        return IndexBufferObject(
            DATA.bytes(indicesNum * 2).apply(block), type
        )
    }

    fun createIndicesShort(indicesNum: Int, block: IShortData.() -> Unit) =
        createIndices(indicesNum, GL_UNSIGNED_SHORT) { shortView().apply(block) }

    open fun build(out: IMesh = IMesh.Build()): IMesh {
        out.material = material
        return out
    }

    companion object {
        val normal = Vec3()
    }
}