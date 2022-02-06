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

import CPRect.*
import impl.CPUtils

/*
   _________            ______________
   __  ____/_______________  __ \__  /_____ _____  __
   _  /    _  __ \_  ___/_  /_/ /_  /_  __ `/_  / / /
   / /___  / /_/ /(__  )_  ____/_  / / /_/ /_  /_/ /
   \____/  \____//____/ /_/     /_/  \__,_/ _\__, /
                                            /____/

          2D ASCII GAME ENGINE FOR SCALA3
            (C) 2021 Rowan Games, Inc.
               ALl rights reserved.
*/

/**
  * Rectangular shape.
  *
  * This class is used to define any arbitrary rectangular shape. Note that this class
  * only describes the rectangular shape but does not hold any content.
  *
  * @param x X-coordinate of the left top corner.
  * @param y Y-coordinate of the left top corner.
  * @param width Width of the rectangular.
  * @param height Height of the rectangular.
  * @see [[CPArray2D]] 2D content holder.
  */
final case class CPRect(x: Int, y: Int, width: Int, height: Int) extends CPInt4(x, y, width, height):
    if width < 0 || height < 0 then E(s"Rect dimension must be >= 0: [$width, $height]")

    /**
      * Area of this rectangle as `width * height`.
      */
    final val area: Int = width * height

    /**
      * Whether width or height equals to zero.
      */
    final val isEmpty: Boolean = area == 0

    /**
      * Whether width and height are greater than zero.
      */
    final val nonEmpty: Boolean = area > 0

    /**
      * Dimension of this rectangle.
      */
    final val dim: CPDim = CPDim(width, height)

    /**
      * Maximum X-coordinate. If width is zero this will equal to `x - 1`.
      */
    final val xMax = x + width - 1

    /**
      * Maximum Y-coordinate. If height is zero this will equal to `y - 1`.
      */
    final val yMax = y + height - 1

    /**
      * Minimal X-coordinate. It is equal to `x`.
      */
    final val xMin = x

    /**
      * Minimal Y-coordinate. It is equal to `y`.
      */
    final val yMin = y

    /**
      * Width of the rectangular (shortcut API).
      */
    final val w = width

    /**
      * Height of the rectangular (shortcut API).
      */
    final val h = height

    /**
      * X-coordinate of the center point.
      */
    final val centerX = if width == 0 then x else x + (xMax - x) / 2

    /**
      * Y-coordinate of the center point.
      */
    final val centerY = if height == 0 then y else y + (yMax - y) / 2

    /**
      * Creates rectangle from two tuples.
      *
      * @param p1 XY-coordinate of the top left corner.
      * @param p2 XY-coordinate of the bottom right corner.
      */
    def this(p1: CPInt2, p2: CPInt2) = 
        this(p1.x, p1.y, (p2.x - p1.x).abs + 1, (p2.y - p2.y).abs + 1)

    /**
      * Creates rectangle from two tuples.
      *
      * @param p1 XY-coordinate of the top left corner.
      * @param p2 XY-coordinate of the bottom right corner.
      */
    def this(p1: (Int, Int), p2: (Int, Int)) = 
        this(p1._1, p1._2, (p2._1 - p1._1).abs + 1, (p2._2 - p1._2).abs + 1)

    /**
      * Creates rectangle.
      *
      * @param x X-coordinate of the top left corner.
      * @param y Y-coordinate of the top left corner.
      * @param dim Dimension of the rectangle.
      */
    def this(x: Int, y: Int, dim: CPDim) = 
        this(x, y, dim.w, dim.h)

    /**
      * Creates rectangle with `(0,0)` left top corner and given dimension.
      *
      * @param dim Rectangle dimension.
      */
    def this(dim: CPDim) = 
        this(0, 0, dim.w, dim.h)

    /**
      * Gets random X-coordinate within this rectangle.
      */
    def randX(): Int = if width == 0 then x else CPRand.randInt(x, x + width - 1)

    /**
      * Gets random Y-coordinate within this rectangle.
      */
    def randY(): Int = if height == 0 then y else CPRand.randInt(y, y + height - 1)

    /**
      * Gets random XY-coordinate within this rectangle.
      */
    def randXY(): CPInt2 = CPInt2(randX(), randY())

    /**
      * Calls given XY-coordinate function on each point in this rectangle.
      *
      * @param f XY-coordinate function to call.
      */
    def loop(f: (Int, Int) => Unit): Unit =
        if nonEmpty then
            var a = x
            var b = y
            while (a <= xMax)
                b = y
                while (b <= yMax)
                    f(a, b)
                    b += 1
                a += 1

    /**
      * Tests whether this rectangle has at least one points satisfying given predicate.
      *
      * @param f Predicate to test.
      */
    def exists(f: (Int, Int) => Boolean): Boolean =
        if nonEmpty then
            var a = x
            var b = y
            var found = false
            while (a <= xMax && !found)
                b = y
                while (b <= yMax && !found)
                    found = f(a, b)
                    b += 1
                a += 1
            found
        else
            false
    
    /**
      * Tests whether this rectangle is position to the left of the given rectangle.
      *
      * @param r Rectangle to test.
      */
    inline def isLeftOf(r: CPRect): Boolean = width != 0 && xMax < r.x

    /**
      * Tests whether this rectangle is position to the right of the given rectangle.
      *
      * @param r Rectangle to test.
      */
    inline def isRightOf(r: CPRect): Boolean = width != 0 && x > r.xMax

    /**
      * Tests whether this rectangle is position above of the given rectangle.
      *
      * @param r Rectangle to test.
      */
    inline def isAboveOf(r: CPRect): Boolean = height != 0 && yMax < r.y

    /**
      * Tests whether this rectangle is position below of the given rectangle.
      *
      * @param r Rectangle to test.
      */
    inline def isBelowOf(r: CPRect): Boolean = height != 0 && y > r.yMax

    /**
      * Whether or not given point is contained within this rectangle. This includes a point
      * on the edge of the rectangle.
      *
      * @param a X coordinate.
      * @param b Y coordinate.
      */
    inline def contains(a: Int, b: Int): Boolean =
        nonEmpty && a >= x && b >= y && a <= xMax && b <= yMax

    /**
      * Tests whether this rectangle contains given rectangle.
      *
      * @param rect Rectangle to test.
      */
    inline def contains(rect: CPRect): Boolean =
        nonEmpty && !rect.isEmpty && x <= rect.x && y <= rect.y && xMax >= rect.xMax && yMax >= rect.yMax

    /**
      * Tests whether this rectangle contains given rectangle vertically.
      *
      * @param rect Rectangle to test.
      */
    inline def containsVer(rect: CPRect): Boolean =
        nonEmpty && rect.nonEmpty && y <= rect.y && yMax >= rect.yMax

    /**
      * Tests whether this rectangle contains given rectangle horizontally.
      *
      * @param rect Rectangle to test.
      */
    inline def containsHor(rect: CPRect): Boolean =
        nonEmpty && rect.nonEmpty && x <= rect.x && xMax >= rect.xMax

    /**
      * Creates new rectangle grown in size enough to contain a given point.
      * This method always creates new rectangle and its dimension will always be equal or larger
      * than the dimension of this rectangle.
      *
      * @param x2 X-coordinate of the point.
      * @param y2 Y-coordinate of the point.
      */
    inline def growTo(x2: Int, y2: Int): CPRect =
        if isEmpty then ZERO
        else
            CPRect(
                x.min(x2),
                y.min(y2),
                xMax.max(x2) + 1,
                yMax.max(y2) + 1
            )

    /**
      * Creates new rectangle with a minimal size to contain both this and given rectangle.
      *
      * @param rect Rectangle to combine with this rectangle.
      */
    inline def union(rect: CPRect): CPRect =
        if isEmpty then rect
        else if rect.isEmpty then this
        else
            CPRect(
                x.min(rect.x),
                y.min(rect.y),
                xMax.max(rect.xMax) + 1,
                yMax.max(rect.yMax) + 1
            )

    /**
      * Creates intersection rectangle between this rectangle and a given one. If rectangles don't
      * intersect, [[CPRect.ZERO]] is returned.
      *
      * @param rect Rectangle to intersect with.
      */
    inline def intersectWith(rect: CPRect): CPRect =
        if overlaps(rect) then
            val xx = x.max(rect.x)
            val yy = y.max(rect.y)
            CPRect(
                xx,
                yy,
                xMax.min(rect.xMax) - xx + 1,
                yMax.min(rect.yMax) - yy + 1
            )
        else
            CPRect.ZERO

    /**
      * Tests whether or not this rectangle overlaps with the given one.
      * Note that touching rectangles do not intersect as far as this method is concerned.
      *
      * @param rect Rectangle to test.
      */
    inline def overlaps(rect: CPRect): Boolean =
        nonEmpty && !rect.isEmpty && !(isAboveOf(rect) || isLeftOf(rect) || rect.isAboveOf(this) || rect.isLeftOf(this))

    override def equals(obj: Any): Boolean = super[CPInt4].equals(obj)
    override def toString: String = s"[x=$x,y=$y,w=$width,h=$height]"
end CPRect

/**
  * Companion object with utility functions.
  */
object CPRect:
    /**
      * Zero rectangle defines as `CPRect(0, 0, 0, 0)`.
      */
    final val ZERO = CPRect(0, 0, 0, 0)

