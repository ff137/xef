package com.xebia.functional.auto

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.toNonEmptyListOrNull
import com.xebia.functional.llm.openai.ChatCompletionRequest
import com.xebia.functional.llm.openai.ChatCompletionResponse
import com.xebia.functional.llm.openai.Message
import com.xebia.functional.llm.openai.OpenAIClient
import com.xebia.functional.llm.openai.Role
import com.xebia.functional.vectorstores.VectorStore

const val COMPLETED = "%COMPLETED%"
const val FAILED = "%FAILED%"

class AutoAI(
  private val model: LLM,
  private val user: User,
  private val openAIClient: OpenAIClient,
  private val vectorStore: VectorStore
) {

  private object TaskCompleted

  private fun messageToTaskAsStrings(firstMessage: String?): List<String> =
    firstMessage?.split("\n") ?: emptyList()

  private suspend fun prioritizationAgent(objective: Objective, tasks: List<Task>): List<Task> {
    val tasksString = tasks.joinToString("\n") { "${it.id.id}. ${it.objective.value}" }
    val prompt = """
            |You are a task prioritization AI tasked with cleaning the formatting of and re-prioritizing the following tasks:
            |$tasksString
            |Consider the ultimate objective of your team: ${objective.value}.
            |Do not remove any tasks. Return the result as a numbered list, like:
            |#. First task
            |#. Second task""".trimMargin()

    val response = chatCompletionResponse(prompt)
    val newTasks = messageToTaskAsStrings(response.firstChoiceOrNull())

    return newTasks.mapNotNull {
      val taskParts = it.trim().split(".", limit = 2)
      if (taskParts.size == 2) {
        val taskId = taskParts[0].trim()
        val taskName = taskParts[1].trim()
        Task(TaskId(taskId.toInt()), Objective(taskName))
      } else null
    }
  }

  private fun List<TaskWithResult>.print(): String =
    joinToString("; ") { "${it.task.id.id}. ${it.task.objective.value} -> result: ${it.result.value()}" }

  /**
   * The execution agent is the AI that performs the task
   */
  private suspend fun executionAgent(objective: Objective, task: Task): ChatCompletionResponse {
    val context = vectorStore.similaritySearch(objective.value, 5).map { TaskWithResult.fromJson(it.content) }
    val prompt = """
            |You are an AI who performs one task based on the following objective: 
            |${objective.value}
            |Take into account these previously completed tasks:
            |${context.print()}.
            |If you think your response is correct given the objective, 
            |return the Response ending with the delimiter: $COMPLETED
            |
            |Your task:
            |${task.objective.value}
            |Response:""".trimMargin()
    return chatCompletionResponse(prompt)
  }

  /** Call the remote Open AI API to complete the task */
  private suspend fun chatCompletionResponse(prompt: String): ChatCompletionResponse {
    val completionRequest = ChatCompletionRequest(
      model = model.value,
      messages = listOf(Message(Role.system.name, prompt, user.name)),
      user = user.name
    )
    return openAIClient.createChatCompletion(completionRequest).also {
      logger.debug {
        when {
          it.isCompleted() -> "CreateChatCompletion SUCCESS ${it.firstChoiceOrNull()}"
          it.isFailed() -> "CreateChatCompletion FAILED ${it.firstChoiceOrNull()}"
          else -> "CreateChatCompletion No choices $it"
        }
      }
    }
  }

  suspend operator fun invoke(objective: Objective): TaskResult? =
    invoke(objective, nonEmptyListOf(Task(TaskId(1), objective)))

  tailrec suspend operator fun invoke(objective: Objective, tasks: NonEmptyList<Task>): TaskResult? {
    logger.debug { tasks.joinToString(separator = "\n") { "${it.id.id}. ${it.objective.value}" } }
    val task = executeAndStoreTask(objective, tasks.head)
    return if (task.isCompleted()) task.result
    // Otherwise, send the result to the task creation agent to create new tasks
    else when (val newPrompts = getNewTasksOrComplete(objective, tasks.tail, task)) {
      // If the task creation agent determines the objective has been completed, return the results
      is Either.Left -> task.result
      // Otherwise, add the new tasks to the storage and send to the prioritization agent
      is Either.Right -> {
        var taskCounter = tasks.last().id.id
        val newTasks = newPrompts.value.map { content -> Task(TaskId(taskCounter++), Objective(content)) }
        val tasksOrNull = prioritizationAgent(objective, newTasks).toNonEmptyListOrNull()
        if (tasksOrNull != null) invoke(objective, tasksOrNull) else null
      }
    }
  }

  /**
   * If the task has been completed, return the results.
   * Otherwise, send the result to the task creation agent to create new tasks
   */
  private suspend fun getNewTasksOrComplete(
    objective: Objective,
    tasks: List<Task>,
    taskWithResult: TaskWithResult
  ): Either<TaskCompleted, List<String>> = either {
    val prompt = """
            You are a task creation AI that uses the result of an execution agent to create new tasks with the following objective: ${objective.value},
            The last completed task has the result: ${taskWithResult.value()}.
            This result was based on this task description: ${taskWithResult.task.objective}.
            These are incomplete tasks:
            ${tasks.joinToString(separator = "\n")}.
            Based on the result, create new tasks to be completed by the AI system that do not overlap with incomplete tasks.
            Return the tasks as an array.
            IMPORTANT!!! : If there are no new tasks to complete and you determine the original objective:[${objective.value}] has been accomplished simply return:$COMPLETED""".trimIndent()
    val response = chatCompletionResponse(prompt)
    ensure(!response.isCompleted()) { TaskCompleted }
    messageToTaskAsStrings(response.firstChoiceOrNull())
  }

  /** Execute the task and store the result */
  private suspend fun executeAndStoreTask(objective: Objective, task: Task): TaskWithResult {
    val result = executionAgent(objective = objective, task = task)
    val taskResult = requireNotNull(result.toTaskResultOrNull()) { "No message returned" }
    return TaskWithResult(task, taskResult).also { vectorStore.addText(it.toJson()) }
  }
}