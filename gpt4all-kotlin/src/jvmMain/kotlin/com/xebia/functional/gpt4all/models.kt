package com.xebia.functional.gpt4all

data class Completion(val context: String)

data class Message(val role: Role, val content: String) {
    enum class Role {
        SYSTEM,
        USER,
        ASSISTANT
    }
}

data class CompletionResponse(
    val modelName: String,
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int,
    val choices: List<Completion>
)

data class ChatCompletionResponse(
    val modelName: String,
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int,
    val choices: List<Message>
)

data class GenerationConfig(
    val logitsSize: Int = 0,
    val tokensSize: Int = 0,
    val nPast: Int = 0,
    val nCtx: Int = 4096,
    val nPredict: Int = 128,
    val topK: Int = 40,
    val topP: Double = 0.95,
    val temp: Double = 0.28,
    val nBatch: Int = 8,
    val repeatPenalty: Double = 1.1,
    val repeatLastN: Int = 64,
    val contextErase: Double = 0.5
)
