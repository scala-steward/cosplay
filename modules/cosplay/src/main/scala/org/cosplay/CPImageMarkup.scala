/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.cosplay

/*
   _________            ______________
   __  ____/_______________  __ \__  /_____ _____  __
   _  /    _  __ \_  ___/_  /_/ /_  /_  __ `/_  / / /
   / /___  / /_/ /(__  )_  ____/_  / / /_/ /_  /_/ /
   \____/  \____//____/ /_/     /_/  \__,_/ _\__, /
                                            /____/

          2D ASCII JVM GAME ENGINE FOR SCALA3
              (C) 2021 Rowan Games, Inc.
                ALl rights reserved.
*/

/**
  *
  * @param openTag
  * @param closeTag
  * @param skin
  * @see [[CPImage.markupImage()]]
  * @see [[CPImageMarkup]]
  */
case class CPImageMarkupElement(openTag: String, closeTag: String, skin: Char => CPPixel)

/**
  *
  * @param fg
  * @param bg
  * @param elements
  * @see [[CPImage.markupImage()]]
  * @see [[CPImageMarkupElement]]
  */
case class CPImageMarkup(fg: CPColor, bg: Option[CPColor], elements: Seq[CPImageMarkupElement]):
    require(elements.exists(x ⇒ x._1.isEmpty || x._2.isEmpty), "Markup element cannot have empty opening or closing tags.")
    require(!elements.exists(elm ⇒ elements.exists(x ⇒
        x != elm &&
        (
            x.openTag.contains(elm.openTag) ||
            x.openTag.contains(elm.closeTag) ||
            x.closeTag.contains(elm.openTag) ||
            x.closeTag.contains(elm.closeTag) ||
            elm.openTag.contains(x.closeTag) ||
            elm.closeTag.contains(x.closeTag) ||
            elm.openTag.contains(x.openTag) ||
            elm.closeTag.contains(x.openTag)
        )
    )), "Markup elements cannot have intersecting open or close tags.")

    /**
      *
      * @param fg
      * @param bg
      * @param elms
      */
    def this(fg: CPColor, bg: Option[CPColor], elms: List[(String, String, Char ⇒ CPPixel)]) =
        this(fg, bg, elms.map(elm ⇒ CPImageMarkupElement(elm._1, elm._2, elm._3)))

    /**
      *
      * @param fg
      * @param bg
      * @param openTag
      * @param closeTag
      * @param skin
      */
    def this(fg: CPColor, bg: Option[CPColor], openTag: String, closeTag: String, skin: Char ⇒ CPPixel) =
        this(fg, bg, Seq(CPImageMarkupElement(openTag, closeTag, skin)))