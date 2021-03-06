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

package org.ksdfv.thelema.kxjs

import kotlinx.browser.document
import org.ksdfv.thelema.data.IByteData
import org.ksdfv.thelema.fs.IFile
import org.ksdfv.thelema.net.NET
import org.ksdfv.thelema.img.IImageData
import org.ksdfv.thelema.img.IImg
import org.w3c.dom.HTMLImageElement

/** @author zeganstyl */
object JsImg: IImg {
    override fun image(): IImageData =
        HtmlImage(document.createElement("img") as HTMLImageElement)

    override fun load(url: String, out: IImageData, sourceLocation: Int, response: (status: Int, img: IImageData) -> Unit): IImageData {
        val img = out.sourceObject as HTMLImageElement
        img.src = url

        // https://stackoverflow.com/questions/280049/how-to-create-a-javascript-callback-for-knowing-when-an-image-is-loaded
        if (img.complete) {
            response(NET.OK, out)
        } else {
            img.onload = { response(NET.OK, out) }
            img.onerror = { _, _, _, _, _ -> response(NET.NotFound, out) }
        }
        return out
    }

    override fun load(file: IFile, out: IImageData, response: (status: Int, img: IImageData) -> Unit): IImageData {
        val img = out.sourceObject as HTMLImageElement
        img.src = file.path
        if (img.complete) {
            response(NET.OK, out)
        } else {
            img.onload = { response(NET.OK, out) }
            img.onerror = { _, _, _, _, _ -> response(NET.NotFound, out) }
        }
        return out
    }

    override fun load(data: IByteData, out: IImageData, response: (status: Int, img: IImageData) -> Unit): IImageData {
        TODO("Not yet implemented")
    }
}
