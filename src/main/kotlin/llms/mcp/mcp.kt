package io.peekandpoke.aktor.llm.mcp

import io.modelcontextprotocol.kotlin.sdk.JSONRPCResponse
import io.modelcontextprotocol.kotlin.sdk.Progress

typealias McpProgressHandler = (Progress) -> Unit
typealias McpResponseHandler = (JSONRPCResponse?, Throwable?) -> Unit

