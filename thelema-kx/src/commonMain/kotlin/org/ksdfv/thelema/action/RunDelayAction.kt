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

package org.ksdfv.thelema.action

/** @author zeganstyl */
class RunDelayAction(var delay: Float, var call: () -> Unit): IAction {
    var remain: Float = delay

    var isNotCalled: Boolean = true

    override val isRunning: Boolean
        get() = remain > 0f

    override fun reset() {
        remain = delay
        isNotCalled = true
    }

    override fun update(delta: Float) {
        if (isRunning) {
            if (isNotCalled) {
                call()
                isNotCalled = false
            }

            if (remain > 0f) {
                remain -= delta
            }
        }
    }
}