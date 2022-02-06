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

package org.cosplay.games.pong

import org.cosplay.CPColor.*
import org.cosplay.*
import org.cosplay.CPArrayImage.*
import prefabs.shaders.*
import org.cosplay.CPFIGLetFont.*
import org.cosplay.CPCanvas.*
import org.cosplay.CPDim.*
import CPPixel.*
import CPKeyboardKey.*
import prefabs.images.*
import prefabs.scenes.*


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
  * Pong main scene.
  */
object CPPongGameScene extends CPScene("game", None, bgPx):
    private var playerScore = 0
    private var enemyScore = 0
    private var playerPosY = 30f
    private var enemyPosY = 30f
    private var ballX = 25
    private var ballY = 20
    private val paddleSpeed = 0.4f
    private var ballAngle = 45
    private var ballSpeed = 1f

    private var playerScoreImg = FIG_BIG.render(playerScore.toString, C_WHITE).trimBg()
    private val enemyScoreImg = FIG_BIG.render(enemyScore.toString, C_WHITE).trimBg()
    private val ballImg = CPArrayImage(
        prepSeq(
            """
              | _
              |(_)
            """
        ),
        (ch, _, _) => ch&C_DARK_GOLDEN_ROD
    ).trimBg()

    private final val ballW = ballImg.getWidth
    private final val ballH = ballImg.getHeight

    private val playerScoreSpr = new CPImageSprite("playerScoreSpr", 0, 0, 0, playerScoreImg, shaders = Seq(CPFadeInShader(true, 1500, bgPx))):
        override def update(ctx: CPSceneObjectContext): Unit =
            val canv = ctx.getCanvas
            setX((canv.dim.width - playerScoreImg.getWidth) / 4)

    private val enemyScoreSpr = new CPImageSprite("enemyScoreSpr", 0, 0, 0, enemyScoreImg):
        override def update(ctx: CPSceneObjectContext): Unit =
            val canv = ctx.getCanvas
            setX((canv.dim.width - enemyScoreImg.getWidth) - ((canv.dim.width / 4) - 1))

    private val ballSpr = new CPImageSprite("ballSpr", 0, 0, 0, ballImg):
        private def clip(v: Int, min: Int, max: Int): Int =
            if v < min then min
            else if v > max then max
            else v

        override def update(ctx: CPSceneObjectContext): Unit =
            val canv = ctx.getCanvas

            val rad = ballAngle * Math.PI / 180
            ballX = clip((ballX + ballSpeed * Math.cos(rad)).round.toInt, canv.xMin, canv.xMax - ballW + 1)
            ballY = clip((ballY + ballSpeed * Math.sin(rad)).round.toInt, canv.yMin, canv.yMax - ballH + 1)

            setX(ballX)
            setY(ballY)

            val rect = getRect

            ctx.getLog.info(s"ball=$rect, canv=${canv.rect}")

            def bounce(dx: Int, dy: Int): Unit =
                ballAngle = (ballAngle + 270) % 360
                ballX += dx
                ballY += dy
                setX(ballX)
                setY(ballY)
                ctx.getLog.trace(s"New angle: $ballAngle")

            if rect.xMax == canv.xMax then bounce(0, -1)
            else if rect.xMin == canv.xMin then bounce(1, 0)
            else if rect.yMax == canv.yMax then bounce(0, -1)
            else if rect.yMin == canv.yMin then bounce(0, 1)

    private val borderSpr = new CPCanvasSprite("border"):
        override def render(ctx: CPSceneObjectContext): Unit =
            val canv = ctx.getCanvas

            canv.drawPolyline(Seq(
                canv.dim.width / 2 -> 0,
                canv.dim.width / 2 -> canv.dim.height
            ), 100, '|'&C_AQUA)

    private val playerPx = CPPixel(' ', C_BLACK, Option(C_AQUA))
    private val enemyPx = CPPixel(' ', C_BLACK, Option(C_GREEN_YELLOW))

    private val playerSpr = new CPCanvasSprite("player"):
        override def render(ctx: CPSceneObjectContext): Unit =
            val canv = ctx.getCanvas

            def move(dy: Float): Unit =
                if dy > 0 && playerPosY < canv.height - 1 then playerPosY += dy
                else if dy < 0 && playerPosY > 5 then playerPosY += dy

            canv.drawLine(1, playerPosY.round, 1, (playerPosY - 5).round, 100, playerPx)
            ctx.getKbEvent match
                case Some(evt) =>
                    evt.key match
                        case KEY_LO_W | KEY_UP => move(if evt.isRepeated then -paddleSpeed else -1.0f)
                        case KEY_LO_S | KEY_DOWN => move(if evt.isRepeated then paddleSpeed else 1.0f)
                        case _ => ()
                case None => ()

    private val enemySpr = new CPCanvasSprite("enemy"):
        override def render(ctx: CPSceneObjectContext): Unit =
            val canv = ctx.getCanvas
            canv.drawLine(canv.dim.width - 2, enemyPosY.round, canv.dim.width - 2, (enemyPosY - 5).round, 100, enemyPx)

            if ballY > enemyPosY then enemyPosY += paddleSpeed
            else if ballY < enemyPosY then enemyPosY -= paddleSpeed

    addObjects(CPKeyboardSprite(KEY_LO_Q, _.exitGame()),
        playerScoreSpr,
        enemyScoreSpr,
        borderSpr,
        playerSpr,
        enemySpr,
        ballSpr
    )
