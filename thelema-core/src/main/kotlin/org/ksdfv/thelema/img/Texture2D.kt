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

package org.ksdfv.thelema.img

import org.ksdfv.thelema.data.DATA
import org.ksdfv.thelema.data.IByteData
import org.ksdfv.thelema.fs.IFile
import org.ksdfv.thelema.gl.*
import org.ksdfv.thelema.net.NET
import org.ksdfv.thelema.utils.LOG

/** 2D texture object
 * @author zeganstyl */
open class Texture2D(textureHandle: Int = GL.glGenTexture()) : ITexture2D, Texture(GL_TEXTURE_2D, textureHandle) {
    override var width: Int = 0
    override var height: Int = 0
    override val depth: Int
        get() = 0

    /** Image from which texture was loaded, may be null, if texture was generated by frame buffer */
    override var image: IImageData? = null

    override fun load(
        uri: String,
        minFilter: Int,
        magFilter: Int,
        sWrap: Int,
        tWrap: Int,
        anisotropicFilter: Float,
        generateMipmaps: Boolean
    ): ITexture2D {
        initTexture()
        IMG.load(uri) { status, img ->
            if (NET.isSuccess(status)) {
                this.image = img
                name = img.name

                initParameters(img.width, img.height, minFilter, magFilter, sWrap, tWrap, anisotropicFilter)
                GL.glTexImage2D(glTarget, 0, img.glInternalFormat, width, height, 0, img.glPixelFormat, img.glType, img)
                if (generateMipmaps) generateMipmapsGPU()
                GL.glBindTexture(glTarget, 0)
            } else {
                LOG.info("can't read $uri, status $status")
            }
        }

        return this
    }

    override fun load(
        file: IFile,
        minFilter: Int,
        magFilter: Int,
        sWrap: Int,
        tWrap: Int,
        anisotropicFilter: Float,
        generateMipmaps: Boolean
    ): ITexture2D {
        initTexture()
        IMG.load(file) { status, img ->
            if (NET.isSuccess(status)) {
                this.image = img
                name = img.name

                initParameters(img.width, img.height, minFilter, magFilter, sWrap, tWrap, anisotropicFilter)
                GL.glTexImage2D(glTarget, 0, img.glInternalFormat, width, height, 0, img.glPixelFormat, img.glType, img)
                if (generateMipmaps) generateMipmapsGPU()
                GL.glBindTexture(glTarget, 0)
            } else {
                LOG.info("can't read ${file.path}, status $status")
            }
        }

        return this
    }

    /** Texture must be bound */
    override fun load(
        image: IImageData,
        minFilter: Int,
        magFilter: Int,
        sWrap: Int,
        tWrap: Int,
        anisotropicFilter: Float,
        generateMipmaps: Boolean
    ): ITexture2D {
        this.image = image
        name = image.name

        initTexture()
        initParameters(image.width, image.height, minFilter, magFilter, sWrap, tWrap, anisotropicFilter)
        GL.glTexImage2D(glTarget, 0, image.glInternalFormat, width, height, 0, image.glPixelFormat, image.glType, image)
        if (generateMipmaps) generateMipmapsGPU()
        GL.glBindTexture(glTarget, 0)

        return this
    }

    override fun load(
        width: Int,
        height: Int,
        pixels: IByteData?,
        mipmapLevel: Int,
        internalFormat: Int,
        pixelFormat: Int,
        type: Int,
        minFilter: Int,
        magFilter: Int,
        sWrap: Int,
        tWrap: Int,
        anisotropicFilter: Float,
        generateMipmaps: Boolean
    ): ITexture2D {
        initParameters(width, height, minFilter, magFilter, sWrap, tWrap, anisotropicFilter)
        GL.glTexImage2D(glTarget, mipmapLevel, internalFormat, width, height, 0, pixelFormat, type, pixels)
        if (generateMipmaps) generateMipmapsGPU()
        GL.glBindTexture(glTarget, 0)

        return this
    }

    override fun initTexture() {
        if (textureHandle == 0) {
            textureHandle = GL.glGenTexture()
        }

        bind()
        minFilter = GL_NEAREST
        magFilter = GL_NEAREST
        val pixels = DATA.bytes(1)
        pixels[0] = 127
        GL.glTexImage2D(glTarget, 0, GL_LUMINANCE, 1, 1, 0, GL_LUMINANCE, GL_UNSIGNED_BYTE, pixels)
        pixels.destroy()
    }

    private fun initParameters(
        width: Int,
        height: Int,
        minFilter: Int,
        magFilter: Int,
        sWrap: Int,
        tWrap: Int,
        anisotropicFilter: Float
    ) {
        this.width = width
        this.height = height

        if (textureHandle == 0) {
            textureHandle = GL.glGenTexture()
        }

        bind()
        this.minFilter = minFilter
        this.magFilter = magFilter
        this.sWrap = sWrap
        this.tWrap = tWrap
        GL.glTexParameteri(glTarget, GL_TEXTURE_MIN_FILTER, minFilter)
        GL.glTexParameteri(glTarget, GL_TEXTURE_MAG_FILTER, magFilter)
        GL.glTexParameteri(glTarget, GL_TEXTURE_WRAP_S, sWrap)
        GL.glTexParameteri(glTarget, GL_TEXTURE_WRAP_T, tWrap)
        this.anisotropicFilter = anisotropicFilter
    }
}
