package com.xebia.functional.xef.conversation.llm.openai

import com.xebia.functional.xef.conversation.AiDsl
import com.xebia.functional.xef.conversation.Conversation
import com.xebia.functional.xef.llm.Chat
import com.xebia.functional.xef.llm.ChatWithFunctions
import com.xebia.functional.xef.llm.StreamedFunction
import com.xebia.functional.xef.prompt.Prompt
import com.xebia.functional.xef.prompt.templates.user
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.serializer

@AiDsl
suspend fun Conversation.promptMessage(
  prompt: Prompt,
  model: Chat = OpenAI().DEFAULT_CHAT
): String = model.promptMessage(prompt, this)

@AiDsl
suspend fun Conversation.promptStreaming(
  prompt: Prompt,
  model: Chat = OpenAI().DEFAULT_CHAT
): Flow<String> = model.promptStreaming(prompt, this)

@AiDsl
inline fun <reified A> Conversation.promptStreaming(
  prompt: Prompt,
  model: ChatWithFunctions = OpenAI().DEFAULT_SERIALIZATION
): Flow<StreamedFunction<A>> = model.promptStreaming(prompt, this, serializer())

@AiDsl
suspend inline fun <reified A> Conversation.prompt(
  input: String,
  model: ChatWithFunctions =
    if (A::class == String::class) OpenAI().DEFAULT_CHAT else OpenAI().DEFAULT_SERIALIZATION
): A =
  if (A::class == String::class)
    model.promptMessage(prompt = Prompt { +user(input) }, scope = conversation) as A
  else
    model.prompt(
      prompt = Prompt { +user(input) },
      scope = conversation,
      serializer = serializer<A>()
    )

@AiDsl
suspend inline fun <reified A> Conversation.prompt(
  prompt: Prompt,
  model: ChatWithFunctions =
    if (A::class == String::class) OpenAI().DEFAULT_CHAT else OpenAI().DEFAULT_SERIALIZATION
): A =
  if (A::class == String::class) model.promptMessage(prompt = prompt, scope = conversation) as A
  else model.prompt(prompt = prompt, scope = conversation, serializer = serializer<A>())
