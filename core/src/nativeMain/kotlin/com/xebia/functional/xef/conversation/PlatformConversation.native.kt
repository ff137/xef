package com.xebia.functional.xef.conversation

import com.xebia.functional.xef.store.ConversationId
import com.xebia.functional.xef.store.VectorStore

actual abstract class PlatformConversation
actual constructor(store: VectorStore, conversationId: ConversationId?) :
  Conversation, AutoClose, AutoCloseable {
  actual companion object {
    actual fun create(store: VectorStore, conversationId: ConversationId?): PlatformConversation =
      NativeConversation(store, conversationId)
  }
}
