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

package org.ksdfv.thelema.phys

import org.ksdfv.thelema.math.IVec3

/** @author zeganstyl */
interface IRay: IShape {
    var length: Float

    fun setRayDirection(vec: IVec3) = setRayDirection(vec.x, vec.y, vec.z)
    fun setRayDirection(x: Float, y: Float, z: Float)
    fun setRayDirection(x: Double, y: Double, z: Double) = setRayDirection(x.toFloat(), y.toFloat(), z.toFloat())

    fun getRayDirection(out: IVec3): IVec3
}