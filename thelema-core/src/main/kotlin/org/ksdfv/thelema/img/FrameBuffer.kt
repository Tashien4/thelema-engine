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

import org.ksdfv.thelema.gl.GL

/**
 * Default frame buffer. You need manually add attachments and build them using [buildAttachments].
 * For creating attachments, probably better you already existing functions from [Attachments].
 *
 * For simple frame buffer you can use [SimpleFrameBuffer].
 *
 * @author zeganstyl
 */
class FrameBuffer(
    width: Int = GL.mainFrameBufferWidth,
    height: Int = GL.mainFrameBufferHeight,
    override var frameBufferHandle: Int = GL.glGenFramebuffer()
): IFrameBuffer {
    private var widthInternal = width
    private var heightInternal = height

    override val width: Int
        get() = widthInternal

    override val height: Int
        get() = heightInternal

    override var isBound: Boolean = false

    override val attachments = ArrayList<IFrameBufferAttachment>()

    override fun setResolution(width: Int, height: Int) {
        widthInternal = width
        heightInternal = height
        super.setResolution(width, height)
    }
}
