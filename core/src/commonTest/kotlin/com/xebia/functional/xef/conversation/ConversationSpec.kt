package com.xebia.functional.xef.conversation

import com.xebia.functional.tokenizer.ModelType
import com.xebia.functional.xef.data.*
import com.xebia.functional.xef.llm.models.chat.Message
import com.xebia.functional.xef.llm.models.chat.Role
import com.xebia.functional.xef.prompt.Prompt
import com.xebia.functional.xef.prompt.templates.user
import com.xebia.functional.xef.store.ConversationId
import com.xebia.functional.xef.store.LocalVectorStore
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.uuid.UUID
import kotlinx.uuid.generateUUID

class ConversationSpec :
  StringSpec({
    "memories should have the correct size in the vector store" {
      val conversationId = ConversationId(UUID.generateUUID().toString())

      val model = TestModel(modelType = ModelType.ADA, name = "fake-model")

      val scope = Conversation(LocalVectorStore(TestEmbeddings()), conversationId = conversationId)

      val vectorStore = scope.store

      model.promptMessages(prompt = Prompt("question 1"), scope = scope)

      model.promptMessages(prompt = Prompt("question 2"), scope = scope)

      val memories = vectorStore.memories(conversationId, 10000)

      memories.size shouldBe 4
    }

    """"
      | ADA model has 2049 max context length
      | when the number of token in the conversation is greater than
      | the space allotted for the message history in the prompt configuration
      | the number of messages in the request must have fewer messages than
      | the total number of messages in the conversation
      |""" {
      val messages = generateRandomMessages(50, 40, 60)
      val conversationId = ConversationId(UUID.generateUUID().toString())
      val scope = Conversation(LocalVectorStore(TestEmbeddings()), conversationId = conversationId)
      val vectorStore = scope.store

      val modelAda = TestModel(modelType = ModelType.ADA, name = "fake-model", responses = messages)

      val totalTokens =
        modelAda.tokensFromMessages(
          messages.flatMap {
            listOf(
              Message(role = Role.USER, content = it.key, name = Role.USER.name),
              Message(role = Role.ASSISTANT, content = it.value, name = Role.USER.name),
            )
          }
        )

      messages.forEach { message ->
        modelAda.promptMessages(prompt = Prompt(message.key), scope = scope)
      }

      val lastRequest = modelAda.requests.last()

      val memories = vectorStore.memories(conversationId, totalTokens)

      // The messages in the request doesn't contain the message response
      val messagesSizePlusMessageResponse = lastRequest.messages.size + 1

      messagesSizePlusMessageResponse shouldBeLessThan memories.size
    }

    """"
      | GPT Turbo 16K model has 16388 max context length
      | when the number of token in the conversation is less than
      | the space allotted for the message history in the prompt configuration
      | the request must send all messages in the conversation
      |""" {
      val messages = generateRandomMessages(50, 40, 60)
      val conversationId = ConversationId(UUID.generateUUID().toString())
      val scope = Conversation(LocalVectorStore(TestEmbeddings()), conversationId = conversationId)
      val vectorStore = scope.store

      val modelGPTTurbo16K =
        TestModel(
          modelType = ModelType.GPT_3_5_TURBO_16_K,
          name = "fake-model",
          responses = messages
        )

      val totalTokens =
        modelGPTTurbo16K.tokensFromMessages(
          messages.flatMap {
            listOf(
              Message(role = Role.USER, content = it.key, name = Role.USER.name),
              Message(role = Role.ASSISTANT, content = it.value, name = Role.ASSISTANT.name),
            )
          }
        )

      messages.forEach { message ->
        modelGPTTurbo16K.promptMessages(prompt = Prompt(message.key), scope = scope)
      }

      val lastRequest = modelGPTTurbo16K.requests.last()

      val memories = vectorStore.memories(conversationId, totalTokens)

      // The messages in the request doesn't contain the message response
      val messagesSizePlusMessageResponse = lastRequest.messages.size + 1

      messagesSizePlusMessageResponse shouldBe memories.size
    }

    "functionCall should be null when the model doesn't support functions" {
      val messages = generateRandomMessages(1, 40, 60)
      val conversationId = ConversationId(UUID.generateUUID().toString())
      val scope = Conversation(LocalVectorStore(TestEmbeddings()), conversationId = conversationId)

      val model = TestModel(modelType = ModelType.ADA, name = "fake-model")

      model.promptMessage(prompt = Prompt(messages.keys.first()), scope = scope)

      val lastRequest = model.requests.last()

      lastRequest.functionCall shouldBe null
    }

    "functionCall should be null when the model support functions and the prompt doesn't contain a function" {
      val messages = generateRandomMessages(1, 40, 60)
      val conversationId = ConversationId(UUID.generateUUID().toString())
      val scope = Conversation(LocalVectorStore(TestEmbeddings()), conversationId = conversationId)

      val model =
        TestFunctionsModel(modelType = ModelType.GPT_3_5_TURBO_FUNCTIONS, name = "fake-model")

      model.promptMessage(prompt = Prompt(messages.keys.first()), scope = scope)

      val lastRequest = model.requests.last()

      lastRequest.functionCall shouldBe null
    }

    "functionCall shouldn't be null when the model support functions and the prompt contain a function" {
      val question = "fake-question"
      val answer = Answer("fake-answer")

      val message = mapOf(question to Json.encodeToString(answer))

      val conversationId = ConversationId(UUID.generateUUID().toString())
      val scope = Conversation(LocalVectorStore(TestEmbeddings()), conversationId = conversationId)

      val model =
        TestFunctionsModel(
          modelType = ModelType.GPT_3_5_TURBO_FUNCTIONS,
          name = "fake-model",
          responses = message
        )

      val response: Answer =
        model.prompt(prompt = Prompt(question), scope = scope, serializer = Answer.serializer())

      val lastRequest = model.requests.last()

      response shouldBe answer

      lastRequest.functionCall shouldNotBe null
    }

    "the message of the request should be the JSON string of the question when the prompt contains serializable object" {
      val question = Question("fake-question")
      val questionJsonString = Json.encodeToString(question)
      val answer = Answer("fake-answer")
      val answerJsonString = Json.encodeToString(answer)

      val message = mapOf(questionJsonString to answerJsonString)

      val conversationId = ConversationId(UUID.generateUUID().toString())
      val scope = Conversation(LocalVectorStore(TestEmbeddings()), conversationId = conversationId)

      val model =
        TestFunctionsModel(
          modelType = ModelType.GPT_3_5_TURBO_FUNCTIONS,
          name = "fake-model",
          responses = message
        )

      val response: Answer =
        model.prompt(
          prompt = Prompt { +user(question) },
          scope = scope,
          serializer = Answer.serializer()
        )

      val lastRequest = model.requests.last()

      lastRequest.messages.last().content shouldBe questionJsonString

      response shouldBe answer
    }
  })
