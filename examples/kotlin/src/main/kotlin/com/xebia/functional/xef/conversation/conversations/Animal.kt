package com.xebia.functional.xef.conversation.conversations

import com.xebia.functional.xef.conversation.llm.openai.OpenAI
import com.xebia.functional.xef.conversation.llm.openai.prompt
import com.xebia.functional.xef.prompt.Prompt
import com.xebia.functional.xef.prompt.templates.system
import com.xebia.functional.xef.prompt.templates.user
import kotlinx.serialization.Serializable

@Serializable data class Animal(val name: String, val habitat: String, val diet: String)

@Serializable
data class Invention(val name: String, val inventor: String, val year: Int, val purpose: String)

suspend fun main() {
  OpenAI.conversation {
    val animal: Animal = prompt("A unique animal species.")
    val invention: Invention = prompt("A groundbreaking invention from the 20th century.")

    println("Animal: $animal")
    println("Invention: $invention")

    val storyPrompt = Prompt {
      +system("You are a writer for a science fiction magazine.")
      +user("Write a short story of 200 words that involves the animal and the invention")
    }

    val story: String = prompt(storyPrompt)

    println(story)
  }
}
