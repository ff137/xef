package com.xebia.functional.xef.scala.conversation.serialization

import com.xebia.functional.xef.prompt.Prompt
import com.xebia.functional.xef.scala.conversation.*
import io.circe.Decoder

private final case class ASCIIArt(art: String) derives SerialDescriptor, Decoder

@main def runASCIIArt: Unit =
  lazy val asciiArt = conversation {
    prompt[ASCIIArt](Prompt("ASCII art of a cat dancing"))
  }
  println(asciiArt.art)
