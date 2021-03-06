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

package org.ksdfv.thelema.test.shader.post


import org.ksdfv.thelema.app.APP
import org.ksdfv.thelema.g3d.cam.Camera
import org.ksdfv.thelema.g3d.cam.OrbitCameraControl
import org.ksdfv.thelema.gl.GL
import org.ksdfv.thelema.gl.GL_COLOR_BUFFER_BIT
import org.ksdfv.thelema.gl.GL_DEPTH_BUFFER_BIT
import org.ksdfv.thelema.img.ITexture
import org.ksdfv.thelema.img.SimpleFrameBuffer
import org.ksdfv.thelema.input.IMouseListener
import org.ksdfv.thelema.input.MOUSE
import org.ksdfv.thelema.math.Vec2
import org.ksdfv.thelema.mesh.ScreenQuad
import org.ksdfv.thelema.mesh.gen.BoxMeshBuilder
import org.ksdfv.thelema.shader.Shader
import org.ksdfv.thelema.shader.post.PostShader
import org.ksdfv.thelema.test.Test
import kotlin.math.pow

/** @author zeganstyl */
class BloomBaseTest: Test {
    override val name: String
        get() = "Bloom base"

    override fun testMain() {

        val cubeShader = Shader(
            vertCode = """
attribute vec3 aPosition;
varying vec3 vPosition;
uniform mat4 viewProj;

void main() {
    vPosition = aPosition;
    gl_Position = viewProj * vec4(aPosition, 1.0);
}""",
            fragCode = """
varying vec3 vPosition;
                
void main() {
    gl_FragColor = vec4(vPosition, 1.0);
}"""
        )


        val blurDownShader = PostShader(
            fragCode = """
varying vec2 uv;

uniform sampler2D uTexture;

uniform vec2 uTexelSize;
uniform float uDelta;

void main() {
    vec4 o = uTexelSize.xyxy * vec2(-uDelta, uDelta).xxyy;

    vec4 s =
    texture2D(uTexture, uv + o.xy) +
    texture2D(uTexture, uv + o.zy) +
    texture2D(uTexture, uv + o.xw) +
    texture2D(uTexture, uv + o.zw);

    gl_FragColor = vec4(s.rgb * 0.25, 1.0);
}"""
        )

        blurDownShader.bind()
        blurDownShader["uTexture"] = 0


        val blurUpShader = PostShader(
            fragCode = """
varying vec2 uv;

uniform sampler2D uSourceTexture;
uniform sampler2D uTexture;

uniform float uDelta;
uniform vec2 uTexelSize;
uniform float uIntensity;

void main() {
    vec4 o = uTexelSize.xyxy * vec2(-uDelta, uDelta).xxyy;

    vec4 s =
    texture2D(uTexture, uv + o.xy) +
    texture2D(uTexture, uv + o.zy) +
    texture2D(uTexture, uv + o.xw) +
    texture2D(uTexture, uv + o.zw);

    gl_FragColor = vec4(s.rgb * 0.25 + texture2D(uSourceTexture, uv).rgb, 1.0);
    gl_FragColor.rgb *= uIntensity;
}"""
        )

        blurUpShader.bind()
        blurUpShader["uSourceTexture"] = 0
        blurUpShader["uTexture"] = 1
        blurUpShader["uIntensity"] = 1f

        val sceneColorBuffer = SimpleFrameBuffer()

        val iterations = 8

        val downScaleBuffers = Array(iterations) {
            val div = 2f.pow(it + 1).toInt()
            SimpleFrameBuffer(sceneColorBuffer.width / div, sceneColorBuffer.height / div)
        }

        val upScaleBuffers = Array(iterations) {
            val div = 2f.pow(it + 1).toInt()
            SimpleFrameBuffer(sceneColorBuffer.width / div, sceneColorBuffer.height / div)
        }

        val texelSizes = Array(iterations) { Vec2(1f / downScaleBuffers[it].width, 1f / downScaleBuffers[it].height) }

        val screenQuad = ScreenQuad.TextureRenderer()

        val box = BoxMeshBuilder().build()

        val camera = Camera()
        camera.far = 1000f

        val control = OrbitCameraControl(camera = camera)
        control.listenToMouse()
        println(control.help)

        // maps to debug
        val debugMaps = ArrayList<ITexture>()
        debugMaps.addAll(downScaleBuffers)
        debugMaps.addAll(upScaleBuffers)
        var mapIndex = 0

        MOUSE.addListener(object : IMouseListener {
            override fun buttonUp(button: Int, screenX: Int, screenY: Int, pointer: Int) {
                if (button == MOUSE.LEFT) {
                    mapIndex = (mapIndex + 1) % debugMaps.size
                }
            }
        })

        GL.isDepthTestEnabled = true

        GL.glClearColor(0f, 0f, 0f, 1f)

        GL.render {
            GL.glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

            control.update(APP.deltaTime)
            camera.update()

            // draw scene to buffer
            sceneColorBuffer.render {
                GL.glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

                cubeShader.bind()
                cubeShader["viewProj"] = camera.viewProjectionMatrix
                box.render(cubeShader)
            }

            // downscale
            blurDownShader.bind()
            blurDownShader["uDelta"] = 1f

            var prevMap = sceneColorBuffer.getTexture(0) // brightness map
            for (i in downScaleBuffers.indices) {
                val buffer = downScaleBuffers[i]
                screenQuad.render(blurDownShader, buffer) {
                    GL.glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)
                    blurDownShader["uTexelSize"] = texelSizes[i]
                    prevMap.bind(0)
                }
                prevMap = buffer.getTexture(0)
            }

            // upscale
            blurUpShader.bind()
            blurUpShader["uDelta"] = 0.5f

            var i = iterations - 1
            while (i > 0) {
                screenQuad.render(blurUpShader, upScaleBuffers[i-1]) {
                    blurUpShader["uTexelSize"] = texelSizes[i-1]
                    downScaleBuffers[i].texture.bind(0)
                    upScaleBuffers[i].texture.bind(1)
                }

                i--
            }

            // bloom output
            screenQuad.render(blurUpShader, null) {
                blurUpShader.set("uTexelSize", 1f / APP.width, 1f / APP.height)
                upScaleBuffers[0].texture.bind(0)
                sceneColorBuffer.texture.bind(1)
            }

            // maps debug rendering
            //screenQuad.render(debugMaps[mapIndex])
        }
    }
}