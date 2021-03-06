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

package org.ksdfv.thelema.g2d

import org.ksdfv.thelema.app.APP
import org.ksdfv.thelema.data.DATA
import org.ksdfv.thelema.g2d.SpriteBatch.Companion.createDefaultShader
import org.ksdfv.thelema.gl.*
import org.ksdfv.thelema.img.ITexture2D
import org.ksdfv.thelema.kx.Language
import org.ksdfv.thelema.math.*
import org.ksdfv.thelema.mesh.MSH
import org.ksdfv.thelema.mesh.VertexInput
import org.ksdfv.thelema.mesh.VertexInputs
import org.ksdfv.thelema.shader.IShader
import org.ksdfv.thelema.shader.Shader
import org.ksdfv.thelema.utils.Color
import kotlin.math.min

/** Draws batched quads using indices.
 *
 * Constructs a new SpriteBatch. Sets the projection matrix to an orthographic projection with y-axis point upwards, x-axis
 * point to the right and the origin being in the bottom left corner of the screen. The projection will be pixel perfect with
 * respect to the current screen resolution.
 *
 *
 * The defaultShader specifies the shader to use. Note that the names for uniforms for this default shader are different than
 * the ones expect for shaders set with [shader]. See [createDefaultShader].
 * @param size The max number of sprites in a single batch. Max of 8191.
 * @param defaultShader The default shader to use. This is not owned by the SpriteBatch and must be disposed separately.
 *
 * @author mzechner, Nathan Sweet, zeganstyl
 */
open class SpriteBatch (size: Int = 1000, defaultShader: Shader = createDefaultShader(), var ownsShader: Boolean = true) : Batch {
    val verts = MSH.vertexBuffer(
        DATA.bytes(size * (2 + 4 + 2)),
        VertexInputs(
            VertexInput(2, "POSITION", GL_FLOAT, false),
            VertexInput(4, "COLOR", GL_UNSIGNED_BYTE, true),
            VertexInput(2, "UV", GL_FLOAT, false)
        )
    )

    val vertices = verts.bytes.floatView()

    private val mesh = MSH.mesh().apply {
        this.vertices = verts

        indices = MSH.indexBuffer(DATA.bytes(size * 6 * 2).apply {
            shortView().apply {
                val len = size * 6
                var j = 0
                var i = 0
                while (i < len) {
                    this[i] = j.toShort()
                    this[i + 1] = (j + 1).toShort()
                    this[i + 2] = (j + 2).toShort()
                    this[i + 3] = (j + 2).toShort()
                    this[i + 4] = (j + 3).toShort()
                    this[i + 5] = this[i]
                    i += 6
                    j += 4
                }
            }
        }, GL_UNSIGNED_SHORT)
    }

    var idx = 0
    var lastTexture: ITexture2D? = null
    var invTexWidth = 0f
    var invTexHeight = 0f
    var drawing = false
    override var transformMatrix: IMat4 = Mat4()
        set(value) {
            if (drawing) flush()
            field.set(value)
            if (drawing) setupMatrices()
        }
    override var projectionMatrix = Mat4().setToOrtho(0f, APP.width.toFloat(), 0f, APP.height.toFloat())
        set(value) {
            if (drawing) flush()
            field.set(value)
            if (drawing) setupMatrices()
        }
    override val combinedMatrix = Mat4()
    private var blendingDisabled = false
    override var blendSrcFunc = GL_SRC_ALPHA
    override var blendDstFunc = GL_ONE_MINUS_SRC_ALPHA
    override var blendSrcFuncAlpha = GL_SRC_ALPHA
    override var blendDstFuncAlpha = GL_ONE_MINUS_SRC_ALPHA
    override var shader: IShader = defaultShader

    override var color: IVec4 = Vec4(1f, 1f, 1f, 1f)
        set(value) {
            field.set(value)
            colorPacked = Color.toFloatBits(value)
        }

    private var colorPacked = Color.toFloatBits(1f, 1f, 1f, 1f)

    override val isDrawing: Boolean
        get() = drawing

    override var packedColor: Float
        get() = colorPacked
        set(value) {
            colorPacked = value
        }

    /** Number of render calls since the last [begin].  */
    var renderCalls = 0
    /** Number of rendering calls, ever. Will not be reset unless set manually.  */
    var totalRenderCalls = 0
    /** The maximum number of sprites rendered in one batch so far.  */
    var maxSpritesInBatch = 0

    override fun begin() {
        check(!drawing) { "SpriteBatch.end must be called before begin." }
        renderCalls = 0
        GL.glDepthMask(false)
        shader.bind()
        setupMatrices()
        drawing = true
    }

    override fun end() {
        check(drawing) { "SpriteBatch.begin must be called before end." }
        if (idx > 0) flush()
        lastTexture = null
        drawing = false
        val gl = GL
        gl.glDepthMask(true)
        if (isBlendingEnabled) gl.glDisable(GL_BLEND)
    }

    override fun setColor(r: Float, g: Float, b: Float, a: Float) {
        color.set(r, g, b, a)
        colorPacked = Color.toFloatBits(color)
    }

    override fun draw(texture: ITexture2D, x: Float, y: Float, originX: Float, originY: Float, width: Float, height: Float, scaleX: Float,
                      scaleY: Float, rotation: Float, srcX: Int, srcY: Int, srcWidth: Int, srcHeight: Int, flipX: Boolean, flipY: Boolean) {
        check(drawing) { "SpriteBatch.begin must be called before draw." }
        val vertices = vertices
        if (texture !== lastTexture) switchTexture(texture) else if (idx == vertices.size) //
            flush()
        // bottom left and top right corner points relative to origin
        val worldOriginX = x + originX
        val worldOriginY = y + originY
        var fx = -originX
        var fy = -originY
        var fx2 = width - originX
        var fy2 = height - originY
        // scale
        if (scaleX != 1f || scaleY != 1f) {
            fx *= scaleX
            fy *= scaleY
            fx2 *= scaleX
            fy2 *= scaleY
        }
        // construct corner points, start from top left and go counter clockwise
        val p1x = fx
        val p1y = fy
        val p2x = fx
        val p2y = fy2
        val p3x = fx2
        val p3y = fy2
        val p4x = fx2
        val p4y = fy
        var x1: Float
        var y1: Float
        var x2: Float
        var y2: Float
        var x3: Float
        var y3: Float
        var x4: Float
        var y4: Float
        // rotate
        if (rotation != 0f) {
            val cos = MATH.cos(rotation)
            val sin = MATH.sin(rotation)
            x1 = cos * p1x - sin * p1y
            y1 = sin * p1x + cos * p1y
            x2 = cos * p2x - sin * p2y
            y2 = sin * p2x + cos * p2y
            x3 = cos * p3x - sin * p3y
            y3 = sin * p3x + cos * p3y
            x4 = x1 + (x3 - x2)
            y4 = y3 - (y2 - y1)
        } else {
            x1 = p1x
            y1 = p1y
            x2 = p2x
            y2 = p2y
            x3 = p3x
            y3 = p3y
            x4 = p4x
            y4 = p4y
        }
        x1 += worldOriginX
        y1 += worldOriginY
        x2 += worldOriginX
        y2 += worldOriginY
        x3 += worldOriginX
        y3 += worldOriginY
        x4 += worldOriginX
        y4 += worldOriginY
        var u = srcX * invTexWidth
        var v = (srcY + srcHeight) * invTexHeight
        var u2 = (srcX + srcWidth) * invTexWidth
        var v2 = srcY * invTexHeight
        if (flipX) {
            val tmp = u
            u = u2
            u2 = tmp
        }
        if (flipY) {
            val tmp = v
            v = v2
            v2 = tmp
        }
        val color = colorPacked
        val idx = idx
        vertices[idx] = x1
        vertices[idx + 1] = y1
        vertices[idx + 2] = color
        vertices[idx + 3] = u
        vertices[idx + 4] = v
        vertices[idx + 5] = x2
        vertices[idx + 6] = y2
        vertices[idx + 7] = color
        vertices[idx + 8] = u
        vertices[idx + 9] = v2
        vertices[idx + 10] = x3
        vertices[idx + 11] = y3
        vertices[idx + 12] = color
        vertices[idx + 13] = u2
        vertices[idx + 14] = v2
        vertices[idx + 15] = x4
        vertices[idx + 16] = y4
        vertices[idx + 17] = color
        vertices[idx + 18] = u2
        vertices[idx + 19] = v
        this.idx = idx + 20
    }

    override fun draw(texture: ITexture2D, x: Float, y: Float, width: Float, height: Float, srcX: Int, srcY: Int, srcWidth: Int,
                      srcHeight: Int, flipX: Boolean, flipY: Boolean) {
        check(drawing) { "SpriteBatch.begin must be called before draw." }
        val vertices = vertices
        if (texture !== lastTexture) switchTexture(texture) else if (idx == vertices.size) //
            flush()
        var u = srcX * invTexWidth
        var v = (srcY + srcHeight) * invTexHeight
        var u2 = (srcX + srcWidth) * invTexWidth
        var v2 = srcY * invTexHeight
        val fx2 = x + width
        val fy2 = y + height
        if (flipX) {
            val tmp = u
            u = u2
            u2 = tmp
        }
        if (flipY) {
            val tmp = v
            v = v2
            v2 = tmp
        }
        val color = colorPacked
        val idx = idx
        vertices[idx] = x
        vertices[idx + 1] = y
        vertices[idx + 2] = color
        vertices[idx + 3] = u
        vertices[idx + 4] = v
        vertices[idx + 5] = x
        vertices[idx + 6] = fy2
        vertices[idx + 7] = color
        vertices[idx + 8] = u
        vertices[idx + 9] = v2
        vertices[idx + 10] = fx2
        vertices[idx + 11] = fy2
        vertices[idx + 12] = color
        vertices[idx + 13] = u2
        vertices[idx + 14] = v2
        vertices[idx + 15] = fx2
        vertices[idx + 16] = y
        vertices[idx + 17] = color
        vertices[idx + 18] = u2
        vertices[idx + 19] = v
        this.idx = idx + 20
    }

    override fun draw(texture: ITexture2D, x: Float, y: Float, srcX: Int, srcY: Int, srcWidth: Int, srcHeight: Int) {
        check(drawing) { "SpriteBatch.begin must be called before draw." }
        val vertices = vertices
        if (texture !== lastTexture) switchTexture(texture) else if (idx == vertices.size) //
            flush()
        val u = srcX * invTexWidth
        val v = (srcY + srcHeight) * invTexHeight
        val u2 = (srcX + srcWidth) * invTexWidth
        val v2 = srcY * invTexHeight
        val fx2 = x + srcWidth
        val fy2 = y + srcHeight
        val color = colorPacked
        val idx = idx
        vertices[idx] = x
        vertices[idx + 1] = y
        vertices[idx + 2] = color
        vertices[idx + 3] = u
        vertices[idx + 4] = v
        vertices[idx + 5] = x
        vertices[idx + 6] = fy2
        vertices[idx + 7] = color
        vertices[idx + 8] = u
        vertices[idx + 9] = v2
        vertices[idx + 10] = fx2
        vertices[idx + 11] = fy2
        vertices[idx + 12] = color
        vertices[idx + 13] = u2
        vertices[idx + 14] = v2
        vertices[idx + 15] = fx2
        vertices[idx + 16] = y
        vertices[idx + 17] = color
        vertices[idx + 18] = u2
        vertices[idx + 19] = v
        this.idx = idx + 20
    }

    override fun draw(texture: ITexture2D, x: Float, y: Float, width: Float, height: Float, u: Float, v: Float, u2: Float, v2: Float) {
        check(drawing) { "SpriteBatch.begin must be called before draw." }
        val vertices = vertices
        if (texture !== lastTexture) switchTexture(texture) else if (idx == vertices.size) //
            flush()
        val fx2 = x + width
        val fy2 = y + height
        val color = colorPacked
        val idx = idx
        vertices[idx] = x
        vertices[idx + 1] = y
        vertices[idx + 2] = color
        vertices[idx + 3] = u
        vertices[idx + 4] = v
        vertices[idx + 5] = x
        vertices[idx + 6] = fy2
        vertices[idx + 7] = color
        vertices[idx + 8] = u
        vertices[idx + 9] = v2
        vertices[idx + 10] = fx2
        vertices[idx + 11] = fy2
        vertices[idx + 12] = color
        vertices[idx + 13] = u2
        vertices[idx + 14] = v2
        vertices[idx + 15] = fx2
        vertices[idx + 16] = y
        vertices[idx + 17] = color
        vertices[idx + 18] = u2
        vertices[idx + 19] = v
        this.idx = idx + 20
    }

    override fun draw(texture: ITexture2D, x: Float, y: Float, width: Float, height: Float) {
        check(drawing) { "SpriteBatch.begin must be called before draw." }
        val vertices = vertices
        if (texture !== lastTexture) switchTexture(texture) else if (idx == vertices.size) {
            flush()
        }
        val fx2 = x + width
        val fy2 = y + height
        val u = 0f
        val v = 1f
        val u2 = 1f
        val v2 = 0f
        val color = colorPacked

        val idx = idx
        vertices[idx] = x
        vertices[idx+1] = y
        vertices[idx+2] = color
        vertices[idx+3] = u
        vertices[idx+4] = v
        vertices[idx+5] = x
        vertices[idx+6] = fy2
        vertices[idx+7] = color
        vertices[idx+8] = u
        vertices[idx+9] = v2
        vertices[idx+10] = fx2
        vertices[idx+11] = fy2
        vertices[idx+12] = color
        vertices[idx+13] = u2
        vertices[idx+14] = v2
        vertices[idx+15] = fx2
        vertices[idx+16] = y
        vertices[idx+17] = color
        vertices[idx+18] = u2
        vertices[idx+19] = v
        this.idx = idx + 20
    }

    override fun draw(texture: ITexture2D, spriteVertices: FloatArray, offset: Int, count: Int) {
        var offset2 = offset
        var count2 = count
        check(drawing) { "SpriteBatch.begin must be called before draw." }
        val verticesLength = vertices.size
        var remainingVertices = verticesLength
        if (texture !== lastTexture) switchTexture(texture) else {
            remainingVertices -= idx
            if (remainingVertices == 0) {
                flush()
                remainingVertices = verticesLength
            }
        }
        var copyCount = min(remainingVertices, count2)
        vertices.position = idx
        vertices.put(spriteVertices, copyCount, offset2)
        idx += copyCount
        count2 -= copyCount
        while (count2 > 0) {
            offset2 += copyCount
            flush()
            copyCount = min(verticesLength, count2)
            vertices.position = 0
            vertices.put(spriteVertices, copyCount, offset2)
            idx += copyCount
            count2 -= copyCount
        }
    }

    override fun draw(region: TextureRegion, x: Float, y: Float, width: Float, height: Float) {
        check(drawing) { "SpriteBatch.begin must be called before draw." }
        val vertices = vertices
        val texture = region.texture
        if (texture !== lastTexture) {
            switchTexture(texture)
        } else if (idx == vertices.size) //
            flush()
        val fx2 = x + width
        val fy2 = y + height
        val u = region.left
        val v = region.top
        val u2 = region.right
        val v2 = region.bottom
        val color = colorPacked
        val idx = idx
        vertices[idx] = x
        vertices[idx + 1] = y
        vertices[idx + 2] = color
        vertices[idx + 3] = u
        vertices[idx + 4] = v
        vertices[idx + 5] = x
        vertices[idx + 6] = fy2
        vertices[idx + 7] = color
        vertices[idx + 8] = u
        vertices[idx + 9] = v2
        vertices[idx + 10] = fx2
        vertices[idx + 11] = fy2
        vertices[idx + 12] = color
        vertices[idx + 13] = u2
        vertices[idx + 14] = v2
        vertices[idx + 15] = fx2
        vertices[idx + 16] = y
        vertices[idx + 17] = color
        vertices[idx + 18] = u2
        vertices[idx + 19] = v
        this.idx = idx + 20
    }

    override fun draw(region: TextureRegion, x: Float, y: Float, originX: Float, originY: Float, width: Float, height: Float,
                      scaleX: Float, scaleY: Float, rotation: Float) {
        check(drawing) { "SpriteBatch.begin must be called before draw." }
        val vertices = vertices
        val texture = region.texture
        if (texture !== lastTexture) {
            switchTexture(texture)
        } else if (idx == vertices.size) //
            flush()
        // bottom left and top right corner points relative to origin
        val worldOriginX = x + originX
        val worldOriginY = y + originY
        var fx = -originX
        var fy = -originY
        var fx2 = width - originX
        var fy2 = height - originY
        // scale
        if (scaleX != 1f || scaleY != 1f) {
            fx *= scaleX
            fy *= scaleY
            fx2 *= scaleX
            fy2 *= scaleY
        }
        // construct corner points, start from top left and go counter clockwise
        val p1x = fx
        val p1y = fy
        val p2x = fx
        val p2y = fy2
        val p3x = fx2
        val p3y = fy2
        val p4x = fx2
        val p4y = fy
        var x1: Float
        var y1: Float
        var x2: Float
        var y2: Float
        var x3: Float
        var y3: Float
        var x4: Float
        var y4: Float
        // rotate
        if (rotation != 0f) {
            val cos = MATH.cos(rotation)
            val sin = MATH.sin(rotation)
            x1 = cos * p1x - sin * p1y
            y1 = sin * p1x + cos * p1y
            x2 = cos * p2x - sin * p2y
            y2 = sin * p2x + cos * p2y
            x3 = cos * p3x - sin * p3y
            y3 = sin * p3x + cos * p3y
            x4 = x1 + (x3 - x2)
            y4 = y3 - (y2 - y1)
        } else {
            x1 = p1x
            y1 = p1y
            x2 = p2x
            y2 = p2y
            x3 = p3x
            y3 = p3y
            x4 = p4x
            y4 = p4y
        }
        x1 += worldOriginX
        y1 += worldOriginY
        x2 += worldOriginX
        y2 += worldOriginY
        x3 += worldOriginX
        y3 += worldOriginY
        x4 += worldOriginX
        y4 += worldOriginY
        val u = region.left
        val v = region.top
        val u2 = region.right
        val v2 = region.bottom
        val color = colorPacked
        val idx = idx
        vertices[idx] = x1
        vertices[idx + 1] = y1
        vertices[idx + 2] = color
        vertices[idx + 3] = u
        vertices[idx + 4] = v
        vertices[idx + 5] = x2
        vertices[idx + 6] = y2
        vertices[idx + 7] = color
        vertices[idx + 8] = u
        vertices[idx + 9] = v2
        vertices[idx + 10] = x3
        vertices[idx + 11] = y3
        vertices[idx + 12] = color
        vertices[idx + 13] = u2
        vertices[idx + 14] = v2
        vertices[idx + 15] = x4
        vertices[idx + 16] = y4
        vertices[idx + 17] = color
        vertices[idx + 18] = u2
        vertices[idx + 19] = v
        this.idx = idx + 20
    }

    override fun draw(region: TextureRegion, x: Float, y: Float, originX: Float, originY: Float, width: Float, height: Float,
                      scaleX: Float, scaleY: Float, rotation: Float, clockwise: Boolean) {
        check(drawing) { "SpriteBatch.begin must be called before draw." }
        val vertices = vertices
        val texture = region.texture
        if (texture !== lastTexture) {
            switchTexture(texture)
        } else if (idx == vertices.size) //
            flush()
        // bottom left and top right corner points relative to origin
        val worldOriginX = x + originX
        val worldOriginY = y + originY
        var fx = -originX
        var fy = -originY
        var fx2 = width - originX
        var fy2 = height - originY
        // scale
        if (scaleX != 1f || scaleY != 1f) {
            fx *= scaleX
            fy *= scaleY
            fx2 *= scaleX
            fy2 *= scaleY
        }
        // construct corner points, start from top left and go counter clockwise
        val p1x = fx
        val p1y = fy
        val p2x = fx
        val p2y = fy2
        val p3x = fx2
        val p3y = fy2
        val p4x = fx2
        val p4y = fy
        var x1: Float
        var y1: Float
        var x2: Float
        var y2: Float
        var x3: Float
        var y3: Float
        var x4: Float
        var y4: Float
        // rotate
        if (rotation != 0f) {
            val cos = MATH.cos(rotation)
            val sin = MATH.sin(rotation)
            x1 = cos * p1x - sin * p1y
            y1 = sin * p1x + cos * p1y
            x2 = cos * p2x - sin * p2y
            y2 = sin * p2x + cos * p2y
            x3 = cos * p3x - sin * p3y
            y3 = sin * p3x + cos * p3y
            x4 = x1 + (x3 - x2)
            y4 = y3 - (y2 - y1)
        } else {
            x1 = p1x
            y1 = p1y
            x2 = p2x
            y2 = p2y
            x3 = p3x
            y3 = p3y
            x4 = p4x
            y4 = p4y
        }
        x1 += worldOriginX
        y1 += worldOriginY
        x2 += worldOriginX
        y2 += worldOriginY
        x3 += worldOriginX
        y3 += worldOriginY
        x4 += worldOriginX
        y4 += worldOriginY
        val u1: Float
        val v1: Float
        val u2: Float
        val v2: Float
        val u3: Float
        val v3: Float
        val u4: Float
        val v4: Float
        if (clockwise) {
            u1 = region.right
            v1 = region.top
            u2 = region.left
            v2 = region.top
            u3 = region.left
            v3 = region.bottom
            u4 = region.right
            v4 = region.bottom
        } else {
            u1 = region.left
            v1 = region.bottom
            u2 = region.right
            v2 = region.bottom
            u3 = region.right
            v3 = region.top
            u4 = region.left
            v4 = region.top
        }
        val color = colorPacked
        val idx = idx
        vertices[idx] = x1
        vertices[idx + 1] = y1
        vertices[idx + 2] = color
        vertices[idx + 3] = u1
        vertices[idx + 4] = v1
        vertices[idx + 5] = x2
        vertices[idx + 6] = y2
        vertices[idx + 7] = color
        vertices[idx + 8] = u2
        vertices[idx + 9] = v2
        vertices[idx + 10] = x3
        vertices[idx + 11] = y3
        vertices[idx + 12] = color
        vertices[idx + 13] = u3
        vertices[idx + 14] = v3
        vertices[idx + 15] = x4
        vertices[idx + 16] = y4
        vertices[idx + 17] = color
        vertices[idx + 18] = u4
        vertices[idx + 19] = v4
        this.idx = idx + 20
    }

    override fun draw(region: TextureRegion, width: Float, height: Float, transform: Affine2) {
        check(drawing) { "SpriteBatch.begin must be called before draw." }
        val vertices = vertices
        val texture = region.texture
        if (texture !== lastTexture) {
            switchTexture(texture)
        } else if (idx == vertices.size) {
            flush()
        }
        // construct corner points
        val x1 = transform.m02
        val y1 = transform.m12
        val x2 = transform.m01 * height + transform.m02
        val y2 = transform.m11 * height + transform.m12
        val x3 = transform.m00 * width + transform.m01 * height + transform.m02
        val y3 = transform.m10 * width + transform.m11 * height + transform.m12
        val x4 = transform.m00 * width + transform.m02
        val y4 = transform.m10 * width + transform.m12
        val u = region.left
        val v = region.top
        val u2 = region.right
        val v2 = region.bottom
        val color = colorPacked
        val idx = idx

        vertices[idx] = x1
        vertices[idx + 1] = y1
        vertices[idx + 2] = color
        vertices[idx + 3] = u
        vertices[idx + 4] = v
        vertices[idx + 5] = x2
        vertices[idx + 6] = y2
        vertices[idx + 7] = color
        vertices[idx + 8] = u
        vertices[idx + 9] = v2
        vertices[idx + 10] = x3
        vertices[idx + 11] = y3
        vertices[idx + 12] = color
        vertices[idx + 13] = u2
        vertices[idx + 14] = v2
        vertices[idx + 15] = x4
        vertices[idx + 16] = y4
        vertices[idx + 17] = color
        vertices[idx + 18] = u2
        vertices[idx + 19] = v
        this.idx = idx + 20
    }

    override fun flush() {
        if (idx == 0) return
        renderCalls++
        totalRenderCalls++
        val spritesInBatch = idx / 20
        if (spritesInBatch > maxSpritesInBatch) maxSpritesInBatch = spritesInBatch
        val count = spritesInBatch * 6
        lastTexture?.bind(0)
        val mesh = mesh
        verts.bytes.size = idx * 4
        vertices.size = idx
        verts.isBufferNeedReload = true
        if (blendingDisabled) {
            GL.glDisable(GL_BLEND)
        } else {
            GL.glEnable(GL_BLEND)
            if (blendSrcFunc != -1) GL.glBlendFuncSeparate(blendSrcFunc, blendDstFunc, blendSrcFuncAlpha, blendDstFuncAlpha)
        }

        mesh.render(shader, count = count)
        idx = 0

        verts.bytes.size = verts.bytes.capacity
        vertices.size = vertices.capacity
    }

    override fun disableBlending() {
        if (blendingDisabled) return
        flush()
        blendingDisabled = true
    }

    override fun enableBlending() {
        if (!blendingDisabled) return
        flush()
        blendingDisabled = false
    }

    override fun setBlendFunction(srcFunc: Int, dstFunc: Int) {
        setBlendFunctionSeparate(srcFunc, dstFunc, srcFunc, dstFunc)
    }

    override fun setBlendFunctionSeparate(srcFuncColor: Int, dstFuncColor: Int, srcFuncAlpha: Int, dstFuncAlpha: Int) {
        if (blendSrcFunc == srcFuncColor && blendDstFunc == dstFuncColor && blendSrcFuncAlpha == srcFuncAlpha && blendDstFuncAlpha == dstFuncAlpha) return
        flush()
        blendSrcFunc = srcFuncColor
        blendDstFunc = dstFuncColor
        blendSrcFuncAlpha = srcFuncAlpha
        blendDstFuncAlpha = dstFuncAlpha
    }

    fun dispose() {
        mesh.destroy()
        if (ownsShader) shader.destroy()
    }

    private fun setupMatrices() {
        combinedMatrix.set(projectionMatrix).mul(transformMatrix)
        shader["u_projTrans"] = combinedMatrix
        shader.setUniformi("u_texture", 0)
    }

    protected fun switchTexture(texture: ITexture2D) {
        flush()
        lastTexture = texture
        invTexWidth = 1.0f / texture.width
        invTexHeight = 1.0f / texture.height
    }

    override val isBlendingEnabled: Boolean
        get() = !blendingDisabled

    companion object {
        /** Returns a new instance of the default shader used by SpriteBatch for GL2 when no shader is specified.  */
        fun createDefaultShader(): Shader {
            @Language("GLSL")
            val vertexShader = """
attribute vec2 POSITION;
attribute vec4 COLOR;
attribute vec2 UV;
uniform mat4 u_projTrans;
varying vec4 v_color;
varying vec2 uv;

void main() {
    v_color = COLOR;
    v_color.a = v_color.a * (255.0/254.0);
    uv = UV;
    gl_Position =  u_projTrans * vec4(POSITION, 0.0, 1.0);
}"""

            @Language("GLSL")
            val fragmentShader = """
#ifdef GL_ES
    #define LOWP lowp
    precision mediump float;
#else
    #define LOWP
#endif

varying LOWP vec4 v_color;
varying vec2 uv;
uniform sampler2D u_texture;

void main() {
    gl_FragColor = v_color * texture2D(u_texture, uv);
}"""
            return Shader(vertexShader, fragmentShader)
        }
    }
}
