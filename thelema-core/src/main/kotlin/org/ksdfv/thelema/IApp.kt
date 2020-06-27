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

import org.ksdfv.thelema.gl.GL

/** @author zeganstyl */
interface IApp {
    val platformType: Int

    /** Main window/canvas width in logical units. If you want frame buffer size, see [GL.mainFrameBufferWidth] */
    val width: Int

    /** Main window/canvas height in logical units. If you want frame buffer size, see [GL.mainFrameBufferHeight] */
    val height: Int

    /** Time span between the current frame and the last frame in seconds. Might be smoothed over n frames. */
    val deltaTime: Float

    /** @return the time span between the current frame and the last frame in seconds, without smoothing */
    val rawDeltaTime: Float

    /** @return the average number of frames per second */
    val fps: Int

    var clipboardString: String

    /** Local cache. For desktop, it is file content in user directory, for HTML it is browser cache. */
    var cache: String

    /** Current cursor */
    var cursor: Int
    var defaultCursor: Int

    fun destroy()

    fun startLoop()
}