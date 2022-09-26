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

package org.cosplay.games.mir.os.progs.mash.compiler

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

import org.cosplay.*
import games.mir.*
import os.progs.mash.*
import os.progs.asm.compiler.*
import os.progs.mash.compiler.antlr4.*
import org.antlr.v4.runtime.tree.*
import org.antlr.v4.runtime.*
import org.cosplay.games.mir.os.progs.asm.compiler.MirAsmException

import MirMashParser as MMP
import ParserRuleContext as PRC

import java.util.UUID
import scala.collection.mutable

/**
  * A container for translated assembler code and global declarations.
  *
  * @param asm
  * @param global
  */
case class MirMashModule(asm: IndexedSeq[MirMashAsm], global: MirMashScope):
    require(global.isGlobal)

/**
  *
  */
enum MirMashDeclarationKind:
    case VAR, FUN, VAL, ALS, NAT

/**
  * A mash declaration of some kind.
  *
  * @param kind
  * @param name Identifier of the declaration.
  */
class MirMashDecl(val kind: MirMashDeclarationKind, val name: String)

/**
  *
  * @param params Function declaration site parameters.
  * @param kind
  * @param name Identifier of the function.
  */
class MirMashDefDecl(val params: Seq[String], kind: MirMashDeclarationKind, name: String) extends MirMashDecl(kind, name)

/**
  * A namespace.
  *
  * @param parent Optional parent scope (`None` for global scope).
  */
case class MirMashScope(parent: Option[MirMashScope] = None):
    private val decls = mutable.HashMap.empty[String, MirMashDecl]
    private val children = mutable.ArrayBuffer.empty[MirMashScope]

    def isGlobal: Boolean = parent.isEmpty
    def hasDecl(name: String): Boolean = decls.contains(name)
    def addDecl(decl: MirMashDecl): Unit = decls += decl.name -> decl
    def getDecl(name: String): Option[MirMashDecl] = decls.get(name)
    def getDecls: Seq[MirMashDecl] = decls.values.toSeq
    def getChildren: Seq[MirMashScope] = children.toSeq
    def mkSubScope: MirMashScope =
        val scope = MirMashScope(Option(this))
        decls.values.foreach(scope.addDecl)
        children += scope
        scope

/**
  * Portable assembler block.
  *
  * @param origin Name of the original source (i.e. file name).
  * @param startLbl Block start ASM label.
  * @param parent Optional parent of this compilation block.
  */
case class AsmBlock(origin: String, startLbl: String, parent: Option[AsmBlock]):
    private val asm = mutable.ArrayBuffer.empty[MirMashAsm]
    private val children = mutable.ArrayBuffer.empty[AsmBlock]

    /**
      *
      */
    def getChildren: Seq[AsmBlock] = children.toSeq

    /**
      *
      * @param lbl
      * @param instr
      * @param cmt
      * @param ctx
      */
    def addAsm(lbl: Option[String], instr: Option[String], cmt: Option[String])(using ctx: PRC): Unit =
        val tok = ctx.start
        val line = tok.getLine
        val pos = tok.getCharPositionInLine
        val dbg = MirMashAsmDebug(line, pos, origin)
        asm += MirMashAsm(lbl, instr, cmt, dbg)

/**
  *
  * @param line Original source line name.
  * @param pos In-line character position.
  * @param origin Name of the original source (i.e. file name).
  */
case class MirMashAsmDebug(line: Int, pos: Int, origin: String)

/**
  *
  * @param label Optional asm instruction label.
  * @param instr Optional instruction (if line contains only label and/or comment).
  * @param comment Optional comment.
  */
case class MirMashAsm(
    label: Option[String],
    instr: Option[String],
    comment: Option[String], // Without leading ';'.
    dbg: MirMashAsmDebug
):
    def toAsmString(useDbg: Boolean): String =
        val lbl = label match
            case Some(s) => s"$s: "
            case None => ""
        val cmt = comment match
            case Some(s) => s"; $s"
            case None => ""
        if instr.isEmpty then
            s"$lbl $cmt"
        else
            val ins = s"${instr.getOrElse("")}${if useDbg then s" @${dbg.line},${dbg.pos},\"${dbg.origin}\"" else ""}"
            f"$lbl%-10s $ins%-15s $cmt"

/**
  *
  */
object MirMashCompiler:
    final val VER = "1.0.1"

    /**
      *
      */
    enum StrKind:
        case NUM, VAR, VAL, ALS, FUN, NAT, UNDEF

    /**
      *
      * @param kind
      * @param str
      */
    case class StrEntity(kind: StrKind, str: String)

/**
  *
  */
class MirMashCompiler:
    import MirMashDeclarationKind.*
    import MirMashCompiler.*
    
    // Not very unique, of course - but good enough.
    private val lblBase = UUID.randomUUID().hashCode().toString
    private var lblCnt = 0L

    /**
      *
      */
    private def genLabel(): String =
        val lbl = s"L$lblBase-$lblCnt"
        lblCnt += 1
        lbl

    /**
      *
      * @param code
      * @param origin
      */
    private class FiniteStateMachine(code: String, origin: String) extends MirMashBaseListener:
        private val asm = mutable.ArrayBuffer.empty[MirMashAsm]
        private var scope = MirMashScope() // Initially global scope.
        private var funParams: mutable.ArrayBuffer[String] = _

        /**
          *
          */
        def getCompileModule: MirMashModule =
            require(scope.isGlobal)
            MirMashModule(asm.toIndexedSeq, scope)

        private def addAsm(instr: String)(using ctx: PRC): Unit = addAsmLoC(None, Option(instr), None)
        private def addAsmComment(cmt: String)(using ctx: PRC): Unit = addAsmLoC(None, None, Option(cmt)) // NOTE: comment without leading ';'.

        private def parseStr(s: String)(using ctx: PRC): StrEntity =
            scope.getDecl(s) match
                case Some(decl) => decl.kind match
                    case VAL => StrEntity(StrKind.VAL, s)
                    case VAR => StrEntity(StrKind.VAR, s)
                    case ALS => StrEntity(StrKind.ALS, s)
                    case FUN => StrEntity(StrKind.FUN, s)
                    case NAT => StrEntity(StrKind.NAT, s)
                case None =>
                    val num = s.replaceAll("_", "")
                    try
                        java.lang.Long.parseLong(num) // Try 'long'.
                        StrEntity(StrKind.NUM, s)
                    catch case _: NumberFormatException =>
                        try
                            java.lang.Double.parseDouble(num) // Try 'double'.
                            StrEntity(StrKind.NUM, s)
                        catch case _: NumberFormatException =>
                            StrEntity(StrKind.UNDEF, s)

        override def exitNatDefDecl(using ctx: MMP.NatDefDeclContext): Unit =
            val strEnt = parseStr(ctx.STR().getText)
            strEnt.kind match
                case StrKind.UNDEF => scope.addDecl(new MirMashDefDecl(funParams.toSeq, NAT, strEnt.str))
                case _ => throw error(s"Non-unique native function name (${strEnt.str})")
        override def enterNatDefDecl(ctx: MMP.NatDefDeclContext): Unit =
            funParams = mutable.ArrayBuffer.empty[String]
        override def enterDefDecl(ctx: MMP.DefDeclContext): Unit =
            funParams = mutable.ArrayBuffer.empty[String]
        override def exitFunParamList(using ctx: MMP.FunParamListContext): Unit =
            val strEnt = parseStr(ctx.STR().getText)
            strEnt.kind match
                case StrKind.UNDEF =>
                    val param = strEnt.str
                    if funParams.contains(param) then throw error(s"Duplicate function parameter ($param)")
                    else funParams += param
                case _ => throw error(s"Function parameter (${strEnt.str}) overrides existing identifier")
        override def enterCompoundExpr(using ctx: MMP.CompoundExprContext): Unit =
            scope = scope.mkSubScope
        override def exitCompoundExpr(using ctx: MMP.CompoundExprContext): Unit =
            scope = scope.parent match
                case Some(s) => s
                case None => assert(false, "Exit global scope.")
        override def exitMash(using ctx: MMP.MashContext): Unit =
            addAsm("exit")
        override def enterMash(using ctx: MMP.MashContext): Unit =
            addAsmComment(s"Generated by mash compiler ver. $VER on ${MirClock.formatNowTimeDate()}")
        override def exitUnaryExpr(using ctx: MMP.UnaryExprContext): Unit =
            if ctx.MINUS() != null then addAsm("neg") else addAsm("not")
        override def exitAndOrExpr(using ctx: MMP.AndOrExprContext): Unit =
            if ctx.AND() != null then addAsm("and") else addAsm("or")
        override def exitEqNeqExpr(using ctx: MMP.EqNeqExprContext): Unit =
            if ctx.EQ() != null then addAsm("eq") else addAsm("neq")
        override def exitCompExpr(using ctx: MMP.CompExprContext): Unit =
            if ctx.LT() != null then addAsm("lt")
            else if ctx.LTEQ() != null then addAsm("lte")
            else if ctx.GT() != null then addAsm("gt")
            else addAsm("gte")
        override def exitPlusMinusExpr(using ctx: MMP.PlusMinusExprContext): Unit =
            if ctx.PLUS() != null then addAsm("add") else addAsm("sub")
        override def exitMultDivModExpr(using ctx: MMP.MultDivModExprContext): Unit =
            if ctx.MOD() != null then addAsm("mod")
            else if ctx.MULT() != null then addAsm("mul")
            else addAsm("div")
        override def exitAtom(using ctx: MMP.AtomContext): Unit =
            if ctx.NULL() != null then addAsm("push null")
            else if ctx.BOOL() != null then addAsm(s"push ${if ctx.getText == "true" then 1 else 0}")
            else if ctx.qstring() != null then
                val qStr = ctx.qstring()
                if qStr.SQSTRING() != null then
                    val s = MirUtils.dequote(qStr.SQSTRING().getText)
                    addAsm(s"push \"$s\"")
                else if qStr.DQSTRING() != null then
                    // TODO: handle variable expansion for double-quoted strings.
                    val s = MirUtils.dequote(qStr.DQSTRING().getText)
                    addAsm(s"push \"$s\"")
            else
                require(ctx.STR() != null)
                val strEnt = parseStr(ctx.STR().getText)
                strEnt.kind match
                    case StrKind.VAL | StrKind.VAR | StrKind.NUM => addAsm(s"push ${strEnt.str}")
                    case _ => throw error("Undefined identifier")

        private def addVar(kind: MirMashDeclarationKind, s: String)(using ctx: PRC): Unit =
            val strEnt = parseStr(s)
            val name = strEnt.str
            strEnt.kind match
                case StrKind.UNDEF =>
                    addAsm(s"pop $name")
                    scope.addDecl(MirMashDecl(kind, name))
                case StrKind.NAT | StrKind.ALS | StrKind.FUN | StrKind.VAL | StrKind.VAR => throw error(s"Duplicate '$name' declaration.")
                case _ => throw error("Unexpected expression")

        override def exitValDecl(using ctx: MMP.ValDeclContext): Unit = addVar(VAL, ctx.STR().getText)
        override def exitVarDecl(using ctx: MMP.VarDeclContext): Unit = addVar(VAR, ctx.STR().getText)

        /**
          *
          * @param errMsg
          * @param ctx
          */
        private def error(errMsg: String)(using ctx: PRC): CPException =
            val tok = ctx.start
            new MirMashException(mkErrorMessage(errMsg, tok.getLine, tok.getCharPositionInLine, code, origin))

    /**
      *
      * @param msg
      * @param line
      * @param charPos
      * @param code
      * @param origin Code origin (file name, etc.).
      */
    private def mkErrorMessage(
        msg: String,
        line: Int, // 1, 2, ...
        charPos: Int, // 0, 1, 2, ...
        code: String,
        origin: String): String =
        def errorPointer(in: String, charPos: Int): (String, String) =
            val in0 = in.strip()
            if in0.isEmpty || charPos < 0 then "<empty>" -> "<empty>"
            else
                var charPos0 = charPos - (in.length - in.stripLeading().length)
                if msg.contains("extraneous input") || msg.contains("mismatched input") then charPos0 += 1
                val len = in0.length
                val pos = Math.min(Math.max(0, charPos0), len)

                if pos == len then
                    s"${"-" * len}^" -> in0
                else
                    val dash = "-" * len
                    val ptrStr = s"${dash.substring(0, pos)}^${dash.substring(pos + 1)}"
                    ptrStr -> in0

        val codeLine = code.split("\n")(line - 1)
        val (ptrStr, origStr) = errorPointer(codeLine, charPos)
        var aMsg = MirUtils.decapitalize(msg) match
            case s: String if s.last == '.' => s
            case s: String => s"$s."

        // Cut the long syntax error.
        aMsg = aMsg.indexOf("expecting") match
            case idx if idx >= 0 => s"${aMsg.substring(0, idx).trim}."
            case _ => aMsg

        s"""# Mesh syntax error in '$origin' at line $line - $aMsg
            #   |-- Line:  $origStr
            #   +-- Error: $ptrStr
            #""".stripMargin('#')

    /**
      * Custom error handler.
      *
      * @param code
      */
    class CompilerErrorListener(code: String) extends BaseErrorListener:
        /**
          *
          * @param recog
          * @param badSymbol
          * @param line
          * @param charPos
          * @param msg
          * @param e
          */
        override def syntaxError(
            recog: Recognizer[_, _],
            badSymbol: scala.Any,
            line: Int, // 1, 2, ...
            charPos: Int, // 1, 2, ...
            msg: String,
            e: RecognitionException): Unit =
            throw new MirMashException(mkErrorMessage(msg, line, charPos - 1, code, recog.getInputStream.getSourceName))

    /**
      *
      * @param code
      * @param origin
      */
    private def antlr4Setup(code: String, origin: String): (FiniteStateMachine, MirMashParser) =
        val lexer = new MirMashLexer(CharStreams.fromString(code, origin))
        val parser = new MirMashParser(new CommonTokenStream(lexer))

        // Set custom error handlers.
        lexer.removeErrorListeners()
        parser.removeErrorListeners()
        lexer.addErrorListener(new CompilerErrorListener(code))
        parser.addErrorListener(new CompilerErrorListener(code))

        // State automata + it's parser.
        new FiniteStateMachine(code, origin) -> parser

    /**
      *
      * @param code
      * @param origin
      * @throws MirMashException
      */
    def compileToAsm(code: String, origin: String): MirMashModule =
        val (fsm, parser) = antlr4Setup(code, origin)

        // Parse the input IDL and walk the built AST.
        (new ParseTreeWalker).walk(fsm, parser.mash())

        fsm.getCompileModule

    /**
      *
      * @param code Mash source code to compile.
      * @param origin Origin of the source code.
      * @throws MirMashException Thrown in case of syntax or runtime errors.
      */
    def compile(code: String, origin: String): MirMashExecutable =
        MirMashExecutable(compileToAsm(code, origin).asm.map(_.toAsmString(true)).mkString("\n"), origin)
