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

package org.ksdfv.thelema

import org.ksdfv.thelema.math.IMat4
import org.ksdfv.thelema.math.IVec4
import org.ksdfv.thelema.math.Mat4
import org.ksdfv.thelema.mesh.IMesh
import org.ksdfv.thelema.mesh.IVertexBuffer
import org.ksdfv.thelema.mesh.VertexAttribute
import org.ksdfv.thelema.mesh.VertexAttributes
import org.ksdfv.thelema.shader.Shader


/** Immediate mode rendering class for GLES 2.0. The renderer will allow you to specify vertices on the fly and provides a default
 * shader for (unlit) rendering. *
 *
 * @author mzechner
 */
class ImmediateModeRenderer20(
        override val maxVertices: Int,
        hasNormals: Boolean,
        hasColors: Boolean,
        private val numTexCoords: Int,
        private var shader: Shader?) : ImmediateModeRenderer {
    private var primitiveType = 0
    private var vertexIdx = 0
    private var numSetTexCoords = 0
    override var numVertices = 0
    private val mesh: IMesh
    private var ownsShader = false
    private val vertexSize: Int
    private val normalOffset: Int
    private val colorOffset: Int
    private val texCoordOffset: Int
    private val projModelView = Mat4()
    private val vertices: FloatArray
    private val shaderUniformNames: Array<String>

    constructor(hasNormals: Boolean, hasColors: Boolean, numTexCoords: Int) : this(5000, hasNormals, hasColors, numTexCoords, createDefaultShader(hasNormals, hasColors, numTexCoords)) {
        ownsShader = true
    }

    constructor(maxVertices: Int, hasNormals: Boolean, hasColors: Boolean, numTexCoords: Int) : this(maxVertices, hasNormals, hasColors, numTexCoords, createDefaultShader(hasNormals, hasColors, numTexCoords)) {
        ownsShader = true
    }

    private fun buildVertexAttributes(hasNormals: Boolean, hasColor: Boolean, numTexCoords: Int): VertexAttributes {
        val vertexAttributes = VertexAttributes(VertexAttribute.Position)
        if (hasNormals) vertexAttributes.add(VertexAttribute.Normal)
        if (hasColor) vertexAttributes.add(VertexAttribute.ColorPacked)
        for (i in 0 until numTexCoords) {
            vertexAttributes.add(VertexAttribute.UV[0])
        }
        return vertexAttributes
    }

    fun setShader(shader: Shader?) {
        if (ownsShader) this.shader!!.destroy()
        this.shader = shader
        ownsShader = false
    }

    override fun begin(projModelView: IMat4, primitiveType: Int) {
        this.projModelView.set(projModelView)
        this.primitiveType = primitiveType
    }

    override fun color(color: IVec4) {
        vertices[vertexIdx + colorOffset] = Color.toFloatBits(color)
    }

    override fun color(r: Float, g: Float, b: Float, a: Float) {
        vertices[vertexIdx + colorOffset] = Color.toFloatBits(r, g, b, a)
    }

    override fun color(colorBits: Float) {
        vertices[vertexIdx + colorOffset] = colorBits
    }

    override fun texCoord(u: Float, v: Float) {
        val idx = vertexIdx + texCoordOffset
        vertices[idx + numSetTexCoords] = u
        vertices[idx + numSetTexCoords + 1] = v
        numSetTexCoords += 2
    }

    override fun normal(x: Float, y: Float, z: Float) {
        val idx = vertexIdx + normalOffset
        vertices[idx] = x
        vertices[idx + 1] = y
        vertices[idx + 2] = z
    }

    override fun vertex(x: Float, y: Float, z: Float) {
        val idx = vertexIdx
        vertices[idx] = x
        vertices[idx + 1] = y
        vertices[idx + 2] = z
        numSetTexCoords = 0
        vertexIdx += vertexSize
        numVertices++
    }

    override fun flush() {
        if (numVertices == 0) return
        shader!!.bind()
        shader!!["u_projModelView"] = projModelView
        for (i in 0 until numTexCoords) shader!!.setUniformi(shaderUniformNames[i], i)
        mesh.vertices?.set(vertices, 0, vertexIdx)
        mesh.render(shader!!, primitiveType)
        numSetTexCoords = 0
        vertexIdx = 0
        numVertices = 0
    }

    override fun end() {
        flush()
    }

    override fun dispose() {
        if (ownsShader && shader != null) shader!!.destroy()
        //mesh.dispose()
    }

    companion object {
        private fun createVertexShader(hasNormals: Boolean, hasColors: Boolean, numTexCoords: Int): String {
            var shader = ("attribute vec4 " + VertexAttribute.PositionName + ";\n"
                    + (if (hasNormals) "attribute vec3 " + VertexAttribute.NormalName + ";\n" else "")
                    + if (hasColors) "attribute vec4 " + VertexAttribute.ColorName + ";\n" else "")
            for (i in 0 until numTexCoords) {
                shader += "attribute vec2 " + VertexAttribute.UVName + i + ";\n"
            }
            shader += "uniform mat4 u_projModelView;\n"
            shader += if (hasColors) "varying vec4 v_col;\n" else ""
            for (i in 0 until numTexCoords) {
                shader += "varying vec2 v_tex$i;\n"
            }
            shader += ("void main() {\n" + "   gl_Position = u_projModelView * " + VertexAttribute.PositionName + ";\n"
                    + if (hasColors) "   v_col = " + VertexAttribute.ColorName + ";\n" else "")
            for (i in 0 until numTexCoords) {
                shader += "   v_tex" + i + " = " + VertexAttribute.UVName + i + ";\n"
            }
            shader += "   gl_PointSize = 1.0;\n"
            shader += "}\n"
            return shader
        }

        private fun createFragmentShader(hasNormals: Boolean, hasColors: Boolean, numTexCoords: Int): String {
            var shader = "#ifdef GL_ES\n" + "precision mediump float;\n" + "#endif\n"
            if (hasColors) shader += "varying vec4 v_col;\n"
            for (i in 0 until numTexCoords) {
                shader += "varying vec2 v_tex$i;\n"
                shader += "uniform sampler2D u_sampler$i;\n"
            }
            shader += "void main() {\n" + "   gl_FragColor = " + if (hasColors) "v_col" else "vec4(1, 1, 1, 1)"
            if (numTexCoords > 0) shader += " * "
            for (i in 0 until numTexCoords) {
                shader += if (i == numTexCoords - 1) {
                    " texture2D(u_sampler$i,  v_tex$i)"
                } else {
                    " texture2D(u_sampler$i,  v_tex$i) *"
                }
            }
            shader += ";\n}"
            return shader
        }

        /** Returns a new instance of the default shader used by SpriteBatch for GL2 when no shader is specified.  */
        fun createDefaultShader(hasNormals: Boolean, hasColors: Boolean, numTexCoords: Int): Shader {
            val vertexShader = createVertexShader(hasNormals, hasColors, numTexCoords)
            val fragmentShader = createFragmentShader(hasNormals, hasColors, numTexCoords)
            return Shader(vertexShader, fragmentShader)
        }
    }

    init {
        val vertexAttributes = buildVertexAttributes(hasNormals, hasColors, numTexCoords)
        vertices = FloatArray(maxVertices * (vertexAttributes.bytesPerVertex / 4))
        mesh = IMesh.Build()
        mesh.vertices = IVertexBuffer.build(vertices, vertexAttributes)
        vertexSize = vertexAttributes.bytesPerVertex / 4
        normalOffset = (vertexAttributes[VertexAttribute.Normal]?.byteOffset ?: 0) / 4
        colorOffset = (vertexAttributes[VertexAttribute.ColorPacked]?.byteOffset ?: 0) / 4
        texCoordOffset = (vertexAttributes[VertexAttribute.UV[0]]?.byteOffset ?: 0) / 4
        shaderUniformNames = Array(numTexCoords) { "u_sampler$it" }
    }
}