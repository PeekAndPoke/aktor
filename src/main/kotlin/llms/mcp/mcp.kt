package io.peekandpoke.aktor.llms.mcp

import io.modelcontextprotocol.kotlin.sdk.JSONRPCResponse
import io.modelcontextprotocol.kotlin.sdk.Progress

typealias McpProgressHandler = (Progress) -> Unit
typealias McpResponseHandler = (JSONRPCResponse?, Throwable?) -> Unit

