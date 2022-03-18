package org.cosplay.games.pong

import org.cosplay.CPFIGLetFont.FIG_BIG
import org.cosplay.CPScene

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

import org.cosplay.*
import CPColor.*
import CPPixel.*
import CPArrayImage.*
import CPFIGLetFont.*
import prefabs.shaders.*
import CPKeyboardKey.*
import org.cosplay.games.pong.shaders.*

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
  * Pong main gameplay scene.
  */
object CPPongPlayScene extends CPScene("play", None, BG_PX):
    private def randAngle(): Int = if CPRand.between(0, 2) == 1 then CPRand.between(135, 160) else CPRand.between(200, 225)

    private final val INIT_VAL = -1f
    private final val MAX_SCORE = 10
    private var playerScore = 0
    private var enemyScore = 0
    private final val playerSpeed = 1.2f
    private var enemySpeed = .8f
    private var ballSpeed = 1.2f
    private var playing = false
    private var gameOver = false
    private var ballAngle = randAngle()

    private val ballImg = CPArrayImage(
        prepSeq(
            """
              |  _
              | (_)
            """
        ),
        (ch, _, _) => ch&C1
    ).trimBg()

    private def mkPaddleImage(c: CPColor): CPImage =
        CPArrayImage(
            prepSeq(
                """
                  |X
                  |X
                  |X
                  |X
                  |X
                """
            ),
            (ch, _, _) => ' '&&(C_BLACK, c)
        )

    private val playerImg = mkPaddleImage(C5)
    private val enemyImg = mkPaddleImage(C3)
    require(playerImg.h == enemyImg.h)
    require(playerImg.w == enemyImg.w)
    private val paddleH = playerImg.h
    private val paddleW = playerImg.w

    private val playerShdr = CPPongPaddleShader()
    private val enemyShdr = CPPongPaddleShader()

    private final val ballW = ballImg.getWidth
    private final val ballH = ballImg.getHeight

    private final val paddleSnd = CPSound(s"sounds/games/pong/bounce1.wav", 0.2f)
    private final val wallSnd = CPSound(s"sounds/games/pong/bounce2.wav", 0.6f)
    private final val missSnd = CPSound(s"sounds/games/pong/miss.wav")
    private final val youLostSnd = CPSound(s"sounds/games/pong/you_lost.wav", 0.5f)
    private final val youWonSnd = CPSound(s"sounds/games/pong/you_won.wav", 0.5f)

    private val serveImg = CPArrayImage(
        prepSeq(
            """
              |+------------------------------+
              ||                              |
              ||    PLAYER 1 READY            |
              ||    ==============            |
              ||                              |
              ||    [SPACE]   Serve Ball      |
              ||    [Q]       Quit            |
              ||                              |
              |+______________________________+
            """),
        (ch, _, _) => ch match
            case c if c.isLetter => c&C4
            case '+' => ch&C1
            case _ => ch&C2
    ).trimBg()

    private val youLostImg = CPArrayImage(
        prepSeq(
            """
              |+--------------------------+
              ||                          |
              ||    YOU LOST :-(          |
              ||    ============          |
              ||                          |
              ||    [SPACE]   Continue    |
              ||    [Q]       Quit        |
              ||                          |
              |+__________________________+
            """),
        (ch, _, _) => ch match
            case c if c.isLetter => c&C4
            case '+' => ch&C1
            case _ => ch&C2
    ).trimBg()

    private val youWonImg = CPArrayImage(
        prepSeq(
            """
              |+--------------------------+
              ||                          |
              ||    YOU WON :-)           |
              ||    ===========           |
              ||                          |
              ||    [SPACE]   Continue    |
              ||    [Q]       Quit        |
              ||                          |
              |+__________________________+
            """),
        (ch, _, _) => ch match
            case c if c.isLetter => c&C4
            case '+' => ch&C1
            case _ => ch&C2
    ).trimBg()

    /**
      *
      * @param score Score value.
      * @return
      */
    private def mkScoreImage(score: Int): CPImage = FIG_BIG.render(score.toString, C4).trimBg()

    /**
      *
      * @param xf X-coordinate producer.
      * @return
      */
    private def mkScoreSprite(xf: (CPCanvas, CPImageSprite) ⇒ Int): CPImageSprite =
        new CPImageSprite(x = 0, y = 0, z = 0, mkScoreImage(0)): // Initial score is zero.
            override def update(ctx: CPSceneObjectContext): Unit = setX(xf(ctx.getCanvas, this))

    // Score sprites.
    private val playerScoreSpr = mkScoreSprite((canv, spr) ⇒ (canv.dim.w - spr.getImage.w) / 4)
    private val enemyScoreSpr = mkScoreSprite((canv, spr) ⇒ (canv.dim.w - spr.getImage.h) - ((canv.dim.w / 4) - 1))

    // Net in the middle.
    private val netSpr = new CPCanvasSprite("net"):
        override def render(ctx: CPSceneObjectContext): Unit =
            val canv = ctx.getCanvas
            canv.drawLine(canv.dim.w / 2, 0, canv.dim.w / 2, canv.dim.h, 5, '|'&C2)

    // Player paddle.
    private val playerSpr = new CPImageSprite(x = 0, y = 0, z = 0, playerImg, false, Seq(playerShdr)):
        private var y = INIT_VAL

        override def reset(): Unit =
            super.reset()
            y = INIT_VAL

        override def update(ctx: CPSceneObjectContext): Unit =
            super.update(ctx)
            val canv = ctx.getCanvas
            if playing then
                if y == INIT_VAL then y = getY.toFloat

                def move(dy: Float): Unit =
                    y = clipPaddleY(canv, y, dy)
                    setY(y.round)

                ctx.getKbEvent match
                    case Some(evt) =>
                        evt.key match
                            case KEY_LO_W | KEY_UP => move(if evt.isRepeated then -playerSpeed else -1.0f)
                            case KEY_LO_S | KEY_DOWN => move(if evt.isRepeated then playerSpeed else 1.0f)
                            case _ => ()
                    case None => ()

    /**
      *
      * @param canv
      * @param currY
      * @param dy
      */
    private def clipPaddleY(canv: CPCanvas, currY: Float, dy: Float): Float =
        val maxY = canv.hF - paddleH
        if dy > 0 then Math.min(maxY, currY + dy)
        else if dy < 0 then Math.max(currY + dy, 0)
        else currY

    // Computer paddle.
    private val enemySpr: CPImageSprite = new CPImageSprite(x = 1, y = 0, z = 0, enemyImg, false, Seq(enemyShdr)):
        private var y = INIT_VAL

        override def reset(): Unit =
            super.reset()
            y = INIT_VAL

        override def update(ctx: CPSceneObjectContext): Unit =
            super.update(ctx)
            val canv = ctx.getCanvas
            setX(canv.w - paddleW)

            if playing then
                if y == INIT_VAL then y = getY.toFloat
                val ballY = ballSpr.getY.toFloat
                val dy = if y > ballY then -enemySpeed else if (y + paddleH / 2) < ballY then enemySpeed else 0f
                y = clipPaddleY(canv, y, dy)
                setY(y.round)

    // Ball sprite.
    private val ballSpr = new CPImageSprite("bs", 0, 0, 1, ballImg, false, Seq(CPPongBallShader)):
        private var x, y = INIT_VAL

        override def reset(): Unit =
            super.reset()
            x = INIT_VAL
            y = INIT_VAL

        override def update(ctx: CPSceneObjectContext): Unit =
            super.update(ctx)
            val canv = ctx.getCanvas
            if playing then
                if x == INIT_VAL && y == INIT_VAL then
                    x = getX.toFloat
                    y = getY.toFloat

                // Adjust ball speed based on the canvas dimensions.
                ballSpeed = canv.wF / canv.hF * 1.1f

                val rad = ballAngle * (Math.PI / 180)
                val xMax = canv.xMax - ballImg.w + 1f
                val yMax = canv.yMax - ballImg.h + 1f

                x += (ballSpeed * Math.cos(rad)).toFloat
                y += (ballSpeed * 0.7 * -Math.sin(rad)).toFloat

                def paddleReturn(isPlayer: Boolean): Unit =
                    x = if isPlayer then paddleW.toFloat else canv.wF - paddleW - ballW - 2
                    ballAngle = -ballAngle + 180
                    paddleSnd.playOnce()
                    if isPlayer then playerShdr.start() else enemyShdr.start()

                def score(plyScr: Int, enyScr: Int): Unit =
                    playerScore += plyScr
                    enemyScore += enyScr
                    missSnd.playOnce()
                    enemyScoreSpr.setImage(mkScoreImage(enemyScore))
                    playerScoreSpr.setImage(mkScoreImage(playerScore))

                    def finishGame(spr: CPImageSprite, snd: CPSound): Unit =
                        playing = false
                        gameOver = true
                        spr.setVisible(true)
                        snd.play(3000)

                    if playerScore == MAX_SCORE then finishGame(youWonSpr, youWonSnd)
                    else if enemyScore == MAX_SCORE then finishGame(youLostSpr, youLostSnd)
                    else playing = false

                if x < 1 && y > playerSpr.getY - ballH && y < playerSpr.getY + paddleH then paddleReturn(true)
                else if x > xMax - 1 && y > enemySpr.getY - ballH && y < enemySpr.getY + paddleH then paddleReturn(false)
                else if y > yMax || y < 0 then
                    ballAngle = -ballAngle
                    wallSnd.playOnce()
                else if x < 0 then score(0, 1)
                else if x > canv.xMax then score(1, 0)

                setX(Math.min(Math.max(x, 0f), xMax).round)
                setY(Math.min(Math.max(y, 0f), yMax).round)

    class CenteredImageSprite(img: CPImage) extends CPImageSprite(x = 0, y = 0, z = 6, img = img):
        override def update(ctx: CPSceneObjectContext): Unit =
            super.update(ctx)
            val canv = ctx.getCanvas
            // Center itself.
            setX((canv.dim.w - getImage.getWidth) / 2)
            setY((canv.dim.h - getImage.getHeight) / 2)

    // Announcements.
    private val serveSpr = new CenteredImageSprite(serveImg)
    private val youLostSpr = new CenteredImageSprite(youLostImg)
    private val youWonSpr = new CenteredImageSprite(youWonImg)

    private val gameCtrlSpr = new CPOffScreenSprite():
        override def update(ctx: CPSceneObjectContext): Unit =
            super.update(ctx)
            val canv = ctx.getCanvas
            if !playing then
                if gameOver then
                    if ctx.isKbKey(KEY_SPACE) then ctx.switchScene("title")
                    end if
                else // Not playing and not game over - first serve.
                    serveSpr.setVisible(true)

                    // Reset positions.
                    playerSpr.reset()
                    enemySpr.reset()
                    ballSpr.reset()
                    playerSpr.setXY(0, canv.dim.h / 2 - paddleH / 2)
                    enemySpr.setXY(canv.w - paddleW, canv.dim.h / 2 - paddleH / 2)
                    ballSpr.setXY(canv.dim.w - paddleW - ballImg.w - 3, canv.dim.h / 2)

                    if ctx.isKbKey(KEY_SPACE) then
                        serveSpr.setVisible(false)
                        playing = true

    addObjects(
        // Scene-wide keyboard handlers.
        CPKeyboardSprite(KEY_LO_Q, _.exitGame()), // Handle 'Q' press globally for this scene.
        // Score sprites.
        playerScoreSpr,
        enemyScoreSpr,
        // Game elements.
        netSpr,
        enemySpr,
        playerSpr,
        ballSpr,
        // Game controller.
        gameCtrlSpr,
        // Announcements.
        serveSpr,
        youLostSpr,
        youWonSpr,
        // Scene-wide shader holder.
        new CPOffScreenSprite(shaders = Seq(CPFadeInShader(true, 1000, BG_PX)))
    )

    override def onActivate(): Unit =
        super.onActivate()

        // All anouncements are invisible initially.
        serveSpr.setVisible(false)
        youLostSpr.setVisible(false)
        youWonSpr.setVisible(false)

        playing = false
        gameOver = false

        playerScore = 0
        enemyScore = 0
        enemySpeed = .8f
        ballSpeed = 1.2f
        ballAngle = randAngle()

        enemyScoreSpr.setImage(mkScoreImage(enemyScore))
        playerScoreSpr.setImage(mkScoreImage(playerScore))





