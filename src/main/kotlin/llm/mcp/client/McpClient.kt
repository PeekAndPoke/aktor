package io.peekandpoke.aktor.llm.mcp.client

import io.ktor.client.*
import io.ktor.client.plugins.sse.*
import io.modelcontextprotocol.kotlin.sdk.CallToolResultBase
import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.ListToolsResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.peekandpoke.aktor.llm.Llm
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.time.Duration.Companion.seconds

class McpClient(
    val name: String,
    val version: String,
    val toolNamespace: String,
    val connector: McpConnector = SseMcpConnector(
        baseUrl = "http://localhost:8001",
        connectUri = "/sse",
        httpClient = HttpClient {
            install(SSE) {
                reconnectionTime = 1.seconds
                maxReconnectionAttempts = Int.MAX_VALUE
            }
        }
    ),
) {
    companion object {
        fun ListToolsResult.asLlmTools(client: McpClient) = tools.map { tool ->

            println("Tool: ${tool.name}")
            println("inputSchema: ${tool.inputSchema}")

            val required = tool.inputSchema.required?.toSet() ?: emptySet()

            val parameters = tool.inputSchema.properties.map { (name, data) ->
                val type = data.jsonObject["type"]?.jsonPrimitive?.content ?: "string"
                val description = data.jsonObject["description"]?.jsonPrimitive?.content ?: ""
                val required = required.contains(name)

                when (type.lowercase()) {
                    "boolean" ->
                        Llm.Tool.BooleanParam(name = name, description = description, required = required)

                    "integer" ->
                        Llm.Tool.IntegerParam(name = name, description = description, required = required)

                    "number" ->
                        Llm.Tool.NumberParam(name = name, description = description, required = required)

                    "string" ->
                        Llm.Tool.StringParam(name = name, description = description, required = required)

                    else ->
                        Llm.Tool.StringParam(name = name, description = description, required = required)
                }
            }

            Llm.Tool.Function(
                name = client.toolNamespace + "_" + tool.name,
                description = tool.description ?: "",
                parameters = parameters,
                fn = { input ->
                    val args = parameters.associate { param ->
                        param.name to when (param) {
                            is Llm.Tool.BooleanParam -> input.getBoolean(param.name)
                            is Llm.Tool.IntegerParam -> input.getInt(param.name)
                            is Llm.Tool.NumberParam -> input.getDouble(param.name)
                            is Llm.Tool.StringParam -> input.getString(param.name)
                        }
                    }

                    println("Calling tool: ${tool.name} with args: $args")

                    val result = client.callTool(name = tool.name, args = args)

                    println("Result: $result")

                    when {
                        // Check for errors
                        result == null || result.isError == true -> {
                            "Error: no response from tool"
                        }

                        else -> {
                            result.content
                                .filterIsInstance<TextContent>()
                                .joinToString("") { it.text ?: "" }
                        }
                    }
                }
            )
        }
    }

    suspend fun connect(): McpClient = apply {
        // https://github.com/modelcontextprotocol/kotlin-sdk

        val client = Client(
            clientInfo = Implementation(name = name, version = version)
        )

        // TODO: send client info on connection

        connector.connect()
    }

    suspend fun listTools(): ListToolsResult? {
        return connector.listTools()
    }

    suspend fun listToolsBound(): List<Llm.Tool.Function>? {
        return listTools()?.asLlmTools(this)
    }

    suspend fun callTool(name: String, args: Map<String, Any?>): CallToolResultBase? {
        return connector.callTool(name, args)
    }
}
