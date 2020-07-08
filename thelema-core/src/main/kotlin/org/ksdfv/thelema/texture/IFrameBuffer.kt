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

package org.ksdfv.thelema.texture

import org.ksdfv.thelema.gl.*

/**
 * @author zeganstyl
 * */
interface IFrameBuffer {
    val width: Int
    val height: Int

    val attachments: MutableList<IFrameBufferAttachment>

    var glHandle: Int

    var isBound: Boolean

    /** @param index attachment index */
    fun getTexture(index: Int) = attachments[index].texture!!

    fun buildAttachments() {
        bind {
            attachments.forEach { attachment ->
                if (attachment.glHandle == 0) {
                    if (attachment.target == GL_RENDERBUFFER) {
                        attachment.glHandle = GL.glGenRenderbuffer()
                        GL.glBindRenderbuffer(GL_RENDERBUFFER, attachment.glHandle)
                        GL.glRenderbufferStorage(GL_RENDERBUFFER, attachment.internalformat, width, height)
                    } else {
                        val texture = Texture2D()
                        texture.glHandle = GL.glGenTexture()
                        texture.glTarget = attachment.textarget
                        texture.load(
                            width,
                            height,
                            null,
                            attachment.mipmapLevel,
                            attachment.internalformat,
                            attachment.pixelFormat,
                            attachment.type,
                            GL_LINEAR,
                            GL_LINEAR,
                            GL_CLAMP_TO_EDGE,
                            GL_CLAMP_TO_EDGE
                        )
                        attachment.glHandle = texture.glHandle
                        attachment.texture = texture
                    }
                }

                if (attachment.target == GL_RENDERBUFFER) {
                    GL.glFramebufferRenderbuffer(GL_FRAMEBUFFER, attachment.attachment, GL_RENDERBUFFER, attachment.glHandle)
                    GL.glBindRenderbuffer(GL_RENDERBUFFER, 0)
                } else {
                    GL.glFramebufferTexture2D(GL_FRAMEBUFFER, attachment.attachment, attachment.textarget, attachment.glHandle, attachment.mipmapLevel)
                }
            }

            // if second attachment is not depth or stencil, that means it is MRT
            if (attachments.size > 1) {
                val attachment = attachments[1].attachment
                if (attachment != GL_DEPTH_ATTACHMENT &&
                        attachment != GL_STENCIL_ATTACHMENT &&
                        attachment != GL_DEPTH_STENCIL_ATTACHMENT) {
                    initBuffersOrder()
                }
            }
        }
    }

    /** You have to reimplement this and set width and height fields */
    fun setResolution(width: Int, height: Int) {
        destroy(true)
        glHandle = GL.glGenFramebuffer()
        buildAttachments()
    }

    /** Indices of [attachments]. For example (0, 1, 2)
     * Used for multi render target.
     * If no indices specified, all color attachments will be used in added order. */
    fun initBuffersOrder(vararg indices: Int) {
        if (glHandle == 0) throw RuntimeException("GL Object is not created, may be you must call createGLObject()")
        if (!isBound) throw RuntimeException("Frame buffer it not bound. You must use bind {}")

        if (indices.isEmpty()) {
            val indices2 = IntArray(attachments.size)
            var count = 0

            for (i in attachments.indices) {
                val attachment = attachments[i].attachment

                if (attachment != GL_DEPTH_ATTACHMENT && attachment != GL_STENCIL_ATTACHMENT && attachment != GL_DEPTH_STENCIL_ATTACHMENT) {
                    indices2[count] = attachment
                    count++
                }
            }

            if (count > 0) {
                GL.glDrawBuffers(count, indices2)
            }
        } else {
            val indicesBuffer = IntArray(indices.size)

            for (i in indices.indices) {
                indicesBuffer[i] = attachments[indices[i]].attachment
            }

            GL.glDrawBuffers(indices.size, indicesBuffer)
        }
    }

    /** Use for debug. */
    fun checkErrors() {
        val result = GL.glCheckFramebufferStatus(GL_FRAMEBUFFER)
        if (result != GL_FRAMEBUFFER_COMPLETE) {
            destroy()
            val errorText = "Frame buffer couldn't be constructed: ${when (result) {
                GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT -> "incomplete attachment"
                GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS -> "incomplete dimensions"
                GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT -> "missing attachment"
                GL_FRAMEBUFFER_UNSUPPORTED -> "unsupported combination of formats"
                else -> "unknown error $result"
            }}"
            println(errorText)
        }
    }

    /** Releases all resources associated with the FrameBuffer.  */
    fun destroy(destroyTextures: Boolean = true) {
        for (i in attachments.indices) {
            val attachment = attachments[i]

            if (attachment.isRenderBuffer) {
                GL.glDeleteRenderbuffer(attachment.glHandle)
            } else {
                val texture = attachment.texture
                if (destroyTextures && texture != null) {
                    texture.destroy()
                }
            }

            attachment.glHandle = 0
        }

        GL.glDeleteFramebuffer(glHandle)
    }

    /** Bind this, do [block] and unbind this.
     * Usually used for setting framebuffer parameters, attaching textures, etc.
     * For rendering you can use [render] */
    fun bind(block: IFrameBuffer.() -> Unit) {
        isBound = true
        GL.glBindFramebuffer(GL_FRAMEBUFFER, glHandle)
        block(this)
        GL.glBindFramebuffer(GL_FRAMEBUFFER,
            defaultFramebufferHandle
        )
        isBound = false
    }

    /** Like [bind], but also sets viewport */
    fun render(block: IFrameBuffer.() -> Unit) {
        GL.glBindFramebuffer(GL_FRAMEBUFFER, glHandle)
        GL.glViewport(0, 0, width, height)
        block(this)
        GL.glBindFramebuffer(GL_FRAMEBUFFER,
            defaultFramebufferHandle
        )
        GL.glViewport(0, 0, GL.mainFrameBufferWidth, GL.mainFrameBufferHeight)
    }

    companion object {
        /** the default framebuffer handle, a.k.a screen.  */
        var defaultFramebufferHandle = 0
    }
}