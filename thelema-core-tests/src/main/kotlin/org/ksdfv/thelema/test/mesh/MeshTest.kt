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

package org.ksdfv.thelema.test.mesh


import org.ksdfv.thelema.data.DATA
import org.ksdfv.thelema.gl.GL
import org.ksdfv.thelema.gl.GL_FLOAT
import org.ksdfv.thelema.gl.GL_UNSIGNED_SHORT
import org.ksdfv.thelema.mesh.*
import org.ksdfv.thelema.shader.Shader
import org.ksdfv.thelema.test.Test

/** @author zeganstyl */
class MeshTest: Test {
    override val name: String
        get() = "Mesh"

    override fun testMain() {
        val mesh = Mesh()

        val vertexInputs = VertexInputs(
            VertexInput(3, "POSITION", GL_FLOAT, true),
            VertexInput(2, "UV", GL_FLOAT, true)
        )

        val vertexBytes = DATA.bytes(20 * 4).apply {
            floatView().apply {
                // x, y, z,   u, v
                put(-1f, -1f, 0f,   0f, 0f)
                put(1f, -1f, 0f,   1f, 0f)
                put(1f, 1f, 0f,   1f, 1f)
                put(-1f, 1f, 0f,   0f, 1f)
            }
        }

        mesh.vertices = VertexBufferObject(vertexBytes, vertexInputs)

        mesh.indices = IndexBufferObject(DATA.bytes(12).apply {
            shortView().apply {
                put(0, 1, 2)
                put(0, 2, 3)
            }
        }, GL_UNSIGNED_SHORT)


        val shader = Shader(
                vertCode = """          
attribute vec4 POSITION;
attribute vec2 UV;
varying vec2 uv;

void main() {
    uv = UV;
    gl_Position = POSITION;
}""",
                fragCode = """                
varying vec2 uv;

void main() {
    gl_FragColor = vec4(uv, 1, 1);
}""")

        GL.render {
            shader.bind()
            mesh.render(shader)
        }
    }
}
