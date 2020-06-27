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

package org.ksdfv.thelema.test

import org.ksdfv.thelema.input.IMouseListener
import org.ksdfv.thelema.input.MOUSE

/** @author zeganstyl */
object MouseTest: Test("Mouse") {
    override fun testMain() {
        MOUSE.addListener(object: IMouseListener {
            override fun buttonDown(button: Int, screenX: Int, screenY: Int, pointer: Int) {
                println("buttonDown: $button")
            }

            override fun buttonUp(button: Int, screenX: Int, screenY: Int, pointer: Int) {
                println("buttonUp: $button")
            }

            override fun dragged(screenX: Int, screenY: Int, pointer: Int) {
                println("dragged: $screenX, $screenY")
            }

            override fun moved(screenX: Int, screenY: Int) {
                println("moved: $screenX, $screenY")
            }

            override fun scrolled(amount: Int) {
                println("scrolled: $amount")
            }
        })
    }
}