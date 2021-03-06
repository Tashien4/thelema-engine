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

package org.ksdfv.thelema.g3d.gltf

import org.ksdfv.thelema.gl.GL_LINEAR
import org.ksdfv.thelema.gl.GL_LINEAR_MIPMAP_LINEAR

/** @author zeganstyl */
class GLTFConf {
    /** Store file buffers in RAM */
    var saveFileBuffersInMem: Boolean = false
    /** Store texture buffers in RAM */
    var saveTexturesInMem: Boolean = false
    /** Store mesh buffers in RAM */
    var saveMeshesInMem: Boolean = false

    /** If separate thread used, gl calls will be deferred to be called on gl thread.
     * User must manually create thread and execute loading there. */
    var separateThread: Boolean = true

    /** Velocity rendering may be used for motion blur */
    var setupVelocityShader: Boolean = true

    var receiveShadows: Boolean = false

    var setupDepthRendering: Boolean = true

    /** Setup pipeline for deferred shading */
    var setupGBufferShader: Boolean = false

    /** Default shader version */
    var shaderVersion: Int = if (setupGBufferShader) 330 else 110

    var defaultTextureMinFilter = GL_LINEAR_MIPMAP_LINEAR
    var defaultTextureMagFilter = GL_LINEAR
}