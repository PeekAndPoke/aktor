package io.peekandpoke.aktor.mcpclient

import io.ktor.client.*
import io.ktor.client.plugins.sse.*
import io.modelcontextprotocol.kotlin.sdk.CallToolResultBase
import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.ListToolsResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.shared.RequestOptions
import io.modelcontextprotocol.kotlin.sdk.shared.Transport
import io.peekandpoke.aktor.llm.Llm
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.time.Duration.Companion.seconds

class McpClient(
    val name: String,
    val version: String,
    val toolNamespace: String,
    private val transport: Transport = CustomSSEClientTransport(
        client = HttpClient {
            install(SSE)
        },
        baseUrl = "http://localhost:8000",
        connectUrl = "http://localhost:8000/sse",
    ),
) {
    companion object {
        fun ListToolsResult.asLlmTools(client: Connected) = tools.map { tool ->

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
                name = client.definition.toolNamespace + "_" + tool.name,
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

    class Connected(
        val definition: McpClient,
        val client: Client,
    ) {
        suspend fun listTools(): ListToolsResult? {
            return client.listTools()
        }

        suspend fun listToolsBound(): List<Llm.Tool.Function>? {
            return listTools()?.asLlmTools(this)
        }

        suspend fun callTool(name: String, args: Map<String, Any?>): CallToolResultBase? {
            return client.callTool(
                name = name,
                arguments = args,
                options = RequestOptions(
                    timeout = 10.seconds
                )
            )
        }
    }

    suspend fun connect(): Connected {
        // https://github.com/modelcontextprotocol/kotlin-sdk

        val client = Client(
            clientInfo = Implementation(name = name, version = version)
        )

        // Connect to server
        client.connect(transport)

        return Connected(
            definition = this,
            client = client,
        )
    }
}
