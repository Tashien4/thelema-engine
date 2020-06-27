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

package org.ksdfv.thelema.g3d

/** Used in [IMaterial.shaderChannels] as key.
 * @author zeganstyl */
object ShaderChannel {
    private var idCounter = 0

    fun newId(): Int {
        val id = idCounter
        idCounter++
        return id
    }

    val DefaultShader = newId()
    val VelocityShader = newId()
}