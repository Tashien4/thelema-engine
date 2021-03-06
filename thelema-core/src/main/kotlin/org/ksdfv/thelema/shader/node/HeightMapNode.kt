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

package org.ksdfv.thelema.shader.node

import org.ksdfv.thelema.img.ITexture
import org.ksdfv.thelema.math.IVec3
import org.ksdfv.thelema.math.Vec3
import org.ksdfv.thelema.mesh.IMesh

/** @author zeganstyl */
class HeightMapNode(
    vertexPosition: IShaderData,
    var texture: ITexture? = null
): ShaderNode() {
    override val name: String
        get() = "Height Map"

    override val classId: String
        get() = ClassId

    override val inputForm: Map<String, Int>
        get() = TODO("Not yet implemented")

    /** Component from [vertexPosition] will be used as U texture coordinate */
    var u = "x"
    /** Component from [vertexPosition] will be used as V texture coordinate */
    var v = "z"

    var uvScale: Float = 1f

    var vertexPosition: IShaderData
        get() = input["vertexPosition"] ?: GLSL.zeroFloat
        set(value) = setInput("vertexPosition", value)

    var mapWorldSize: IVec3 = Vec3(1f, 1f, 1f)

    val sampler = defOut(GLSLFloat("tex"))
    val textureValue = defOut(GLSLVec4("texValue"))
    val height = defOut(GLSLFloat("height"))

    var unit: Int = -1

    init {
        setInput("vertexPosition", vertexPosition)
    }

    override fun prepareToDrawMesh(mesh: IMesh) {
        super.prepareToDrawMesh(mesh)
        if (unit >= 0) texture?.bind(unit)
    }

    override fun declarationVert(out: StringBuilder) {
        super.declarationVert(out)
        out.append("uniform sampler2D ${sampler.ref};\n")
        out.append("varying vec4 ${textureValue.ref};\n")
        out.append("varying float ${height.ref};\n")
    }

    override fun declarationFrag(out: StringBuilder) {
        super.declarationFrag(out)
        out.append("varying vec4 ${textureValue.ref};\n")
        out.append("varying float ${height.ref};\n")
    }

    override fun executionVert(out: StringBuilder) {
        super.executionVert(out)
        val pos = vertexPosition.ref
        out.append("${textureValue.ref} = texture2D(${sampler.ref}, vec2($pos.$u / ${mapWorldSize.x} + 0.5, $pos.$v / ${mapWorldSize.z} + 0.5) * $uvScale);\n")
        out.append("${height.ref} = ${textureValue.ref}.x * ${mapWorldSize.y};\n")
        out.append("${vertexPosition.ref}.y = ${height.ref};\n")
    }

    companion object {
        const val ClassId = "heightMap"
    }
}