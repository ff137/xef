package com.xebia.functional.xef.reasoning.filesystem

import com.xebia.functional.xef.auto.CoreAIScope
import com.xebia.functional.xef.io.DEFAULT
import com.xebia.functional.xef.llm.ChatWithFunctions
import com.xebia.functional.xef.prompt.experts.ExpertSystem
import com.xebia.functional.xef.reasoning.tools.Tool
import kotlinx.uuid.UUID
import kotlinx.uuid.generateUUID
import okio.FileSystem

class ProduceTextFile(
  private val model: ChatWithFunctions,
  private val scope: CoreAIScope,
  private val instructions: List<String> = emptyList()
) : Tool {
  override val name: String = "Write output to Text file"

  override val description: String = "Convert output to Text files"

  override suspend fun invoke(input: String): String {
    val file: TxtFile =
      model.prompt(
        prompt =
          ExpertSystem(
            system = "Convert output for a Text File",
            query = input,
            instructions = instructions,
          ),
        context = scope.context,
        conversationId = scope.conversationId,
        serializer = TxtFile.serializer()
      )
    val uuid = UUID.generateUUID().toString(false)
    val data = FileSystem.SYSTEM_TEMPORARY_DIRECTORY.resolve(uuid)
    FileSystem.DEFAULT.createDirectories(data, true)
    val tempFile = data.resolve(file.name)
    FileSystem.DEFAULT.write(tempFile, mustCreate = true) { writeUtf8(file.content) }
    return tempFile.toString()
  }
}