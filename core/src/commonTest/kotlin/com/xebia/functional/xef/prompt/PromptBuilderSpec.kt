package com.xebia.functional.xef.prompt

import com.xebia.functional.xef.data.Question
import com.xebia.functional.xef.llm.models.chat.Role
import com.xebia.functional.xef.prompt.templates.assistant
import com.xebia.functional.xef.prompt.templates.steps
import com.xebia.functional.xef.prompt.templates.system
import com.xebia.functional.xef.prompt.templates.user
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class PromptBuilderSpec :
  StringSpec({
    "buildPrompt should return the expected messages" {
      val messages =
        Prompt {
            +system("Test System")
            +user("Test Query")
            +assistant("Test Assistant")
          }
          .messages

      val messagesExpected =
        listOf(
          "Test System".message(Role.SYSTEM),
          "Test Query".message(Role.USER),
          "Test Assistant".message(Role.ASSISTANT)
        )

      messages shouldBe messagesExpected
    }

    "buildPrompt should return the expected messages when using forEach" {
      val instructions = listOf("instruction 1", "instruction 2")

      val messages =
        Prompt {
            +system("Test System")
            +user("Test Query")
            instructions.forEach { +assistant(it) }
          }
          .messages

      val messagesExpected =
        listOf(
          "Test System".message(Role.SYSTEM),
          "Test Query".message(Role.USER),
          "instruction 1".message(Role.ASSISTANT),
          "instruction 2".message(Role.ASSISTANT)
        )

      messages shouldBe messagesExpected
    }

    "buildPrompt should return the expected messages when using steps with the number for every step" {
      val instructions = listOf("instruction 1", "instruction 2")

      val messages =
        Prompt {
            +system("Test System")
            +user("Test Query")
            +steps { instructions.forEach { +assistant(it) } }
          }
          .messages

      val messagesExpected =
        listOf(
          "Test System".message(Role.SYSTEM),
          "Test Query".message(Role.USER),
          "1 - instruction 1".message(Role.ASSISTANT),
          "2 - instruction 2".message(Role.ASSISTANT)
        )

      messages shouldBe messagesExpected
    }

    "buildPrompt should return the expected messages when using serializable objects" {
      val question = Question("Test Question")

      val messages =
        Prompt {
            +system("Test System")
            +user(question)
          }
          .messages

      val messagesExpected =
        listOf(
          "Test System".message(Role.SYSTEM),
          question.message(Role.USER),
        )

      messages shouldBe messagesExpected
    }
  })
