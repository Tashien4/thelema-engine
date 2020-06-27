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

/** @author zeganstyl */
object APP: IApp {
    const val Desktop = 0
    const val Android = 1
    const val WebGL = 2

    const val ArrowCursor = 0
    const val IBeamCursor = 1
    const val CrosshairCursor = 2
    const val HandCursor = 3
    const val HorizontalResizeCursor = 4
    const val VerticalResizeCursor = 5

    lateinit var api: IApp

    override val platformType: Int
        get() = api.platformType

    override val deltaTime: Float
        get() = api.deltaTime

    override val rawDeltaTime: Float
        get() = api.rawDeltaTime

    override val fps: Int
        get() = api.fps

    override val width: Int
        get() = api.width

    override val height: Int
        get() = api.height

    override var clipboardString: String
        get() = api.clipboardString
        set(value) { api.clipboardString = value }

    override var cursor: Int
        get() = api.cursor
        set(value) { api.cursor = value }

    override var defaultCursor: Int
        get() = api.defaultCursor
        set(value) { api.defaultCursor = value }

    /** It can be used to check if new iteration started. Minimal states is 0 and 1.
     * If [maxMainLoopIterationCounter] is less than this, iteration will be set to 0 */
    var mainLoopIteration: Int = 0

    /** See [mainLoopIteration] */
    var maxMainLoopIterationCounter: Int = 1

    override var cache: String
        get() = api.cache
        set(value) { api.cache = value }

    override fun destroy() = api.destroy()

    override fun startLoop() = api.startLoop()
}