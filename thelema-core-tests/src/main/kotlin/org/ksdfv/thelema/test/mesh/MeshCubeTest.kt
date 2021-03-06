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
import org.ksdfv.thelema.g3d.cam.Camera
import org.ksdfv.thelema.gl.*
import org.ksdfv.thelema.math.MATH
import org.ksdfv.thelema.math.Mat4
import org.ksdfv.thelema.math.Vec3
import org.ksdfv.thelema.mesh.MSH
import org.ksdfv.thelema.mesh.Mesh
import org.ksdfv.thelema.mesh.VertexInput
import org.ksdfv.thelema.mesh.VertexInputs
import org.ksdfv.thelema.shader.Shader
import org.ksdfv.thelema.test.Test

/** @author zeganstyl */
class MeshCubeTest: Test {
    override val name: String
        get() = "Mesh Cube"

    override fun testMain() {
        val mesh = Mesh()

        val vertexInputs = VertexInputs(
            VertexInput(3, "POSITION", GL_FLOAT, true)
        )

        mesh.vertices = MSH.vertexBuffer(DATA.bytes(8 * 3 * 4).apply {
            floatView().apply {
                put(
                        // front
                        -1f, -1f,  1f,
                        1f, -1f,  1f,
                        1f,  1f,  1f,
                        -1f,  1f,  1f,
                        // back
                        -1f, -1f, -1f,
                        1f, -1f, -1f,
                        1f,  1f, -1f,
                        -1f,  1f, -1f
                )
            }
        }, vertexInputs)

        mesh.indices = MSH.indexBuffer(DATA.bytes(6 * 6 * 2).apply {
            shortView().apply {
                put(
                        // front
                        0, 1, 2,
                        2, 3, 0,
                        // right
                        1, 5, 6,
                        6, 2, 1,
                        // back
                        7, 6, 5,
                        5, 4, 7,
                        // left
                        4, 0, 3,
                        3, 7, 4,
                        // bottom
                        4, 5, 1,
                        1, 0, 4,
                        // top
                        3, 2, 6,
                        6, 7, 3
                )
            }
        }, GL_UNSIGNED_SHORT)


        val shader = Shader(
                vertCode = """
attribute vec3 POSITION;
varying vec3 position;
uniform mat4 projViewModelTrans;

void main() {
    position = POSITION.xyz;
    gl_Position = projViewModelTrans * vec4(POSITION, 1.0);
}""",
                fragCode = """
varying vec3 position;

void main() {
    gl_FragColor = vec4(position, 1.0);
}""")

        val camera = Camera().apply {
            lookAt(Vec3(0f, 3f, -3f), MATH.Zero3)
            near = 0.1f
            far = 100f
            update()
        }

        val cubeMatrix4 = Mat4()
        val temp = Mat4()

        GL.isDepthTestEnabled = true

        GL.glClearColor(0f, 0f, 0f, 1f)
        GL.render {
            GL.glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

            cubeMatrix4.rotate(0f, 1f, 0f, 0.01f)

            shader.bind()
            shader["projViewModelTrans"] = temp.set(cubeMatrix4).mulLeft(camera.viewProjectionMatrix)
            mesh.render(shader)
        }
    }
}