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

package org.ksdfv.thelema.mesh

import org.ksdfv.thelema.data.DATA
import org.ksdfv.thelema.data.IByteData
import org.ksdfv.thelema.data.IFloatData
import org.ksdfv.thelema.gl.GL_STATIC_DRAW
import org.ksdfv.thelema.mesh.IVertexBuffer.Companion.Build
import org.ksdfv.thelema.shader.IShader

/** @author zeganstyl */
interface IVertexBuffer {
    var handle: Int

    /** @return the number of vertices this VertexData stores */
    var size
        get() = if (attributes.bytesPerVertex > 0) bytes.size / attributes.bytesPerVertex else 0
        set(_) {}

    /** @return the [VertexAttributes] as specified during construction.
     */
    var attributes: VertexAttributes

    /** Returns the underlying FloatBuffer and marks it as dirty, causing the buffer contents to be uploaded on the next call to
     * bind. If you need immediate uploading use [.setVertices]; Any modifications made to the Buffer
     * *after* the call to bind will not automatically be uploaded.
     * @return the underlying FloatBuffer holding the vertex data.
     */
    var floatBuffer: IFloatData

    var bytes: IByteData

    var isBufferNeedReload: Boolean
    var isBound: Boolean
    var usage: Int

    /** Number of instances for instanced buffers or particle system buffers.
     * For simple vertex buffers it is unused */
    var instancesToRenderCount: Int

    /** Initialize handles */
    fun initGpuObjects()

    /** If buffer is not bound [isBufferNeedReload] will set to true and buffer will be loaded on next bind call */
    fun loadBufferToGpu()

    /** Sets the vertices of this VertexData, discarding the old vertex data. The count must equal the number of floats per vertex
     * times the number of vertices to be copied to this VertexData. The order of the vertex attributes must be the same as
     * specified at construction time via [VertexAttributes].
     *
     *
     * This can be called in between calls to bind and unbind. The vertex data will be updated instantly.
     * @param data the vertex data
     * @param offset the offset to start copying the data from
     * @param count the number of floats to copy
     */
    fun set(data: FloatArray, offset: Int = 0, count: Int = data.size) {
        bytes.position = 0
        bytes.size = count * 4
        floatBuffer.position = 0
        floatBuffer.size = count
        floatBuffer.put(data, 0, count)
        bytes.position = 0
        floatBuffer.position = 0
        isBufferNeedReload = true
        loadBufferToGpu()
    }

    fun bind(shader: IShader? = null)
    fun unbind(shader: IShader? = null)

    /** Disposes vertex data of this object and all its associated OpenGL resources.  */
    fun destroy()

    companion object {
        /** Wrapper for [Build] */
        fun build(
            byteBuffer: IByteData,
            attributes: VertexAttributes,
            usage: Int = GL_STATIC_DRAW,
            initGpuObjects: Boolean = true
        ) = Build(byteBuffer, attributes, usage, initGpuObjects)

        /** Wrapper for [Build] */
        @Deprecated("")
        fun build(
            data: FloatArray,
            attributes: VertexAttributes,
            usage: Int = GL_STATIC_DRAW,
            initGpuObjects: Boolean = true
        ): IVertexBuffer {
            val vertices = Build(DATA.bytes(data.size * 4), attributes, usage, initGpuObjects)
            vertices.set(data)
            return vertices
        }

        /** Wrapper for [Build] */
        fun build(
            verticesNum: Int,
            attributes: VertexAttributes,
            usage: Int = GL_STATIC_DRAW,
            initGpuObjects: Boolean = true,
            context: IFloatData.() -> Unit
        ): IVertexBuffer {
            val bytes = DATA.bytes(verticesNum * attributes.bytesPerVertex)
            context(bytes.floatView())
            return Build(bytes, attributes, usage, initGpuObjects)
        }

        /** Wrapper for [Build]. With position attribute */
        fun buildPos(
            verticesNum: Int,
            usage: Int = GL_STATIC_DRAW,
            initGpuObjects: Boolean = true,
            context: IFloatData.() -> Unit
        ): IVertexBuffer {
            val attributes = VertexAttributes(VertexAttribute.Position)
            val bytes = DATA.bytes(verticesNum * attributes.bytesPerVertex)
            context(bytes.floatView())
            return Build(bytes, attributes, usage, initGpuObjects)
        }

        /** Wrapper for [Build]. With position, texture coordinates and normal attributes */
        fun buildPosTexNor(
            verticesNum: Int,
            usage: Int = GL_STATIC_DRAW,
            initGpuObjects: Boolean = true,
            context: IFloatData.() -> Unit
        ): IVertexBuffer {
            val attributes = VertexAttributes(
                VertexAttribute.Position,
                VertexAttribute.UV[0],
                VertexAttribute.Normal
            )
            val bytes = DATA.bytes(verticesNum * attributes.bytesPerVertex)
            context(bytes.floatView())
            return Build(bytes, attributes, usage, initGpuObjects)
        }

        /** Default builder */
        var Build: (
            data: IByteData,
            attributes: VertexAttributes,
            usage: Int,
            initGpuObjects: Boolean
        ) -> IVertexBuffer = { data, attributes, usage, initGpuObjects ->
            VertexArrayObject(attributes, data, usage, initGpuObjects)
        }
    }
}