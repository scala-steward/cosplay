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

package org.cosplay.games.mir.station

import java.text.SimpleDateFormat

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
  *
  */
object CPMirStation:
    /**
      *
      */
    def apply(): CPMirStation =
        val fmt = SimpleDateFormat("dd MMMM yyyy")
        val modules = Seq(
            CPMirModule("Core Module", "cor", fmt.parse("19 Feb 1986")),
            CPMirModule("Kvant-1", "kv1", fmt.parse("31 Mar 1987")),
            CPMirModule("Kvant-2", "kv2", fmt.parse("26 Nov 1989")),
            CPMirModule("Kristal", "krs", fmt.parse("31 May 1990")),
            CPMirModule("Spektr", "spk", fmt.parse("20 May 1995")),
            CPMirModule("Docking Module", "dck", fmt.parse("15 Nov 1995")),
            CPMirModule("Priroda", "prd", fmt.parse("26 Apr 1996"))
        )

        new CPMirStation:
            override def allModules: Seq[CPMirModule] = modules

/**
  *
  */
trait CPMirStation:
    /**
      *
      */
    def allModules: Seq[CPMirModule]

