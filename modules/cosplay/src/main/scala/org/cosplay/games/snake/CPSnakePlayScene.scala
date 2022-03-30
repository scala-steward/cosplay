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

package org.cosplay.games.snake

import org.cosplay.*
import games.*
import prefabs.shaders.*
import CPFIGLetFont.*
import CPPixel.*
import CPColor.*
import CPKeyboardKey.*

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
  * @param dim Dimension for this scene.
  */
class CPSnakePlayScene(dim: CPDim) extends CPScene("play", Option(dim), BG_PX):
    private var score = 0
    private val scoreSpr = new CPImageSprite(x = 0, y = 0, z = 1, img = mkScoreImage):
        override def update(ctx: CPSceneObjectContext): Unit =
            val canv = ctx.getCanvas
            setX((canv.w - getImage.w) / 2)
    private val borderSpr = new CPCanvasSprite:
        private val px = ' '&&(C1, C1)
        private val px2 = ' '&&(C2, C2)
        override def render(ctx: CPSceneObjectContext): Unit =
            val canv = ctx.getCanvas
            val scoreH = scoreSpr.getHeight
            canv.drawRect(0, scoreH, CPDim(canv.w, canv.h - scoreH), 0, (_, _) ⇒ px)
            canv.drawLine(1, scoreH + 1, 1, canv.h, 0, px)
            canv.drawLine(canv.w - 2, scoreH + 1, canv.w - 2, canv.h, 0, px)

            // Border rect fill.
            canv.fillRect(0, 0, canv.w, scoreH - 1, 0, (_, _) ⇒ px2)

    /**
      * Creates score image.
      */
    private def mkScoreImage: CPImage = FIG_RECTANGLES.render(s"SCORE : $score", C_BLACK).trimBg()

    addObjects(
        new CPOffScreenSprite(CPFadeInShader(true, 1000, BG_PX)),
        // Scene-wide keyboard handlers.
        CPKeyboardSprite(KEY_LO_Q, _.exitGame()), // Handle 'Q' press globally for this scene.
        scoreSpr,
        borderSpr
    )