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

package org.ksdfv.thelema.gl

abstract class AbstractGL: IGL {
    /** See [call] */
    val singleCalls = ArrayList<() -> Unit>()

    /** See [render] */
    val renderCalls = ArrayList<() -> Unit>()

    var maxAnisotropicFilterLevelInternal = 1f

    /** Maximum supported anisotropic filtering level supported by the device. */
    override val maxAnisotropicFilterLevel
        get() = maxAnisotropicFilterLevelInternal

    protected val textureUnitsInternal = ArrayList<Int>(16).apply { for (i in 0 until 16) { add(0) } }

    /** Contains texture handles of all texture image units. */
    override val textureUnits: List<Int>
        get() = textureUnitsInternal

    override var activeTexture: Int = 0
        set(value) {
            if (field != value) {
                field = value
                glActiveTextureBase(value + GL_TEXTURE0)
            }
        }

    override var program: Int = 0
        set(value) {
            if (field != value) {
                field = value
                glUseProgramBase(value)
            }
        }

    override var arrayBuffer: Int = 0
        set(value) {
            if (field != value) {
                field = value
                glBindBufferBase(GL_ARRAY_BUFFER, value)
            }
        }

    override var elementArrayBuffer: Int = 0
        set(value) {
            if (field != value) {
                field = value
                glBindBufferBase(GL_ELEMENT_ARRAY_BUFFER, value)
            }
        }

    override var vertexArray: Int = 0
        set(value) {
            if (field != value) {
                field = value
                glBindVertexArrayBase(value)
            }
        }

    override var isCullFaceEnabled: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                if (value) glEnableBase(GL_CULL_FACE) else glEnableBase(GL_CULL_FACE)
            }
        }

    override var cullFaceMode: Int = GL_BACK
        set(value) {
            if (field != value) {
                field = value
                glCullFaceBase(value)
            }
        }

    override var isBlendEnabled: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                if (value) glEnableBase(GL_BLEND) else glDisableBase(GL_BLEND)
            }
        }

    override var blendFactorS: Int = GL_ONE
        set(value) {
            if (field != value) {
                field = value
                if (glCallsEnabled) glBlendFuncBase(value, blendFactorD)
            }
        }

    override var blendFactorD: Int = GL_ZERO
        set(value) {
            if (field != value) {
                field = value
                if (glCallsEnabled) glBlendFuncBase(blendFactorS, value)
            }
        }

    override var isDepthTestEnabled: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                if (value) glEnableBase(GL_DEPTH_TEST) else glDisableBase(GL_DEPTH_TEST)
            }
        }

    override var depthFunc: Int = GL_LESS
        set(value) {
            if (field != value) {
                field = value
                glDepthFuncBase(value)
            }
        }

    override var isDepthMaskEnabled: Boolean = true
        set(value) {
            if (field != value) {
                field = value
                glDepthMaskBase(value)
            }
        }

    private var glCallsEnabled = true

    protected var textureUnitCounter = 0

    protected abstract fun glActiveTextureBase(value: Int)
    protected abstract fun glUseProgramBase(value: Int)
    protected abstract fun glBindBufferBase(target: Int, buffer: Int)
    protected abstract fun glBindVertexArrayBase(value: Int)
    protected abstract fun glCullFaceBase(value: Int)
    protected abstract fun glBlendFuncBase(factorS: Int, factorD: Int)
    protected abstract fun glEnableBase(value: Int)
    protected abstract fun glDisableBase(value: Int)
    protected abstract fun glDepthFuncBase(value: Int)
    protected abstract fun glDepthMaskBase(value: Boolean)
    protected abstract fun glBindTextureBase(target: Int, texture: Int)

    override fun initGL() {
        // check texture units
        val buffer1 = IntArray(1)
        glGetIntegerv(GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS, buffer1)
        val units = buffer1[0]

        if (textureUnitsInternal.size < units) {
            val num = units - textureUnitsInternal.size
            for (i in 0 until num) {
                textureUnitsInternal.add(0)
            }
        } else if (textureUnitsInternal.size > units) {
            val num = textureUnitsInternal.size - units
            for (i in 0 until num) {
                textureUnitsInternal.removeAt(textureUnitsInternal.lastIndex)
            }
        }

        if (isExtensionSupported("GL_EXT_texture_filter_anisotropic")) {
            val buffer2 = FloatArray(16)
            glGetFloatv(GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT, buffer2)
            maxAnisotropicFilterLevelInternal = buffer2[0]
        }
    }

    override fun getNextTextureUnit(): Int {
        val unit = textureUnitCounter
        textureUnitCounter++
        if (textureUnitCounter > textureUnits.size) {
            throw IllegalStateException("Texture unit exceeds maximum number of texture units")
        }
        return unit
    }

    override fun resetTextureUnitCounter() {
        textureUnitCounter = 0
    }

    override fun glDisable(cap: Int) {
        when (cap) {
            GL_BLEND -> isBlendEnabled = false
            GL_CULL_FACE -> isCullFaceEnabled = false
            GL_DEPTH_TEST -> isDepthTestEnabled = false
            else -> glDisableBase(cap)
        }
    }

    override fun glEnable(cap: Int) {
        when (cap) {
            GL_BLEND -> isBlendEnabled = true
            GL_CULL_FACE -> isCullFaceEnabled = true
            GL_DEPTH_TEST -> isDepthTestEnabled = true
            else -> glEnableBase(cap)
        }
    }

    override fun glBindBuffer(target: Int, buffer: Int) {
        when (target) {
            GL_ARRAY_BUFFER -> arrayBuffer = buffer
            GL_ELEMENT_ARRAY_BUFFER -> elementArrayBuffer = buffer
            else -> glBindBufferBase(target, buffer)
        }
    }

    override fun glCullFace(mode: Int) {
        cullFaceMode = mode
    }

    override fun glDepthFunc(func: Int) {
        depthFunc = func
    }

    override fun glDepthMask(flag: Boolean) {
        isDepthMaskEnabled = flag
    }

    override fun glActiveTexture(texture: Int) {
        activeTexture = texture - GL_TEXTURE0
    }

    override fun glBindTexture(target: Int, texture: Int) {
        if (textureUnitsInternal[activeTexture] != texture) {
            textureUnitsInternal[activeTexture] = texture
            glBindTextureBase(target, texture)
        }
    }

    override fun glBindVertexArray(array: Int) {
        vertexArray = array
    }

    override fun glBlendFunc(sfactor: Int, dfactor: Int) {
        glCallsEnabled = false
        glBlendFuncBase(sfactor, dfactor)
        blendFactorS = sfactor
        blendFactorD = sfactor
        glCallsEnabled = true
    }

    override fun glUseProgram(program: Int) {
        this.program = program
    }

    /** Simple blending function in most cases.
     * sfactor = GL_SRC_ALPHA; dfactor = GL_ONE_MINUS_SRC_ALPHA */
    override fun setSimpleAlphaBlending() {
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
    }

    override fun call(function: () -> Unit) {
        singleCalls.add(function)
    }

    override fun render(function: () -> Unit) {
        renderCalls.add(function)
    }

    override fun removeSingleCall(function: () -> Unit) {
        singleCalls.remove(function)
    }

    override fun removeRenderCall(function: () -> Unit) {
        renderCalls.remove(function)
    }

    override fun clearSingleCalls() {
        singleCalls.clear()
    }

    override fun clearRenderCalls() {
        renderCalls.clear()
    }

    /** Must be called on OpenGL thread by platform backend */
    override fun runSingleCalls() {
        if (singleCalls.size > 0) {
            for (i in singleCalls.indices) {
                singleCalls[i]()
            }
            singleCalls.clear()
        }
    }

    /** Must be called on OpenGL thread by platform backend */
    override fun runRenderCalls() {
        if (renderCalls.size > 0) {
            for (i in renderCalls.indices) {
                renderCalls[i]()
            }
        }
    }
}
