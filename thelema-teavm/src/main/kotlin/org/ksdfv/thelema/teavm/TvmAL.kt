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

package org.ksdfv.thelema.teavm

import org.ksdfv.thelema.audio.*
import org.ksdfv.thelema.fs.IFile
import org.teavm.jso.webaudio.AudioContext

/** @author zeganstyl */
class TvmAL: IAL {
    val context = AudioContext.create()

    override fun newAudioDevice(samplingRate: Int, channelsNum: Int): IAudioDevice =
        TvmAudioDevice(this, samplingRate.toFloat(), channelsNum)

    override fun newAudioRecorder(samplingRate: Int, isMono: Boolean): IAudioRecorder {
        TODO("Not yet implemented")
    }

    override fun newSound(file: IFile): ISound = TvmSound(this, file.path)

    override fun newMusic(file: IFile): IMusic = TvmSound(this, file.path)

    override fun getVersion(param: Int): String = "Web Audio API"

    override fun update() {}

    override fun destroy() {
        context.close()
    }
}
