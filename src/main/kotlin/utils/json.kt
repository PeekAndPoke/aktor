package io.peekandpoke.aktor.utils

import kotlinx.serialization.json.*

fun JsonObject.append(other: JsonObject?): JsonObject =
    if (other == null) this else JsonObject(this + other)

fun JsonObject.append(builderAction: JsonObjectBuilder.() -> Unit): JsonObject =
    this.append(buildJsonObject(builderAction))

fun JsonObject.unwrap(): Map<String, Any?> {
    return entries.associate { (k, v) ->
        k to v.unwrap()
    }
}

fun JsonElement.unwrap(): Any? = when (this) {
    is JsonObject -> unwrap()
    is JsonArray -> map { it.unwrap() }
    is JsonPrimitive -> when {
        isString -> content
        longOrNull != null -> long
        doubleOrNull != null -> double
        booleanOrNull != null -> boolean
        else -> null
    }
}

fun Map<String, Any?>.toJsonObject(): JsonObject {
    return buildJsonObject {
        forEach { (key, value) ->
            put(key.toString(), value.toJsonElement())
        }
    }
}

// Helper function to convert any value to JsonElement
private fun Any?.toJsonElement(): JsonElement {
    @Suppress("UNCHECKED_CAST")
    return when (this) {
        null -> JsonNull
        is Map<*, *> -> (this as Map<String, Any?>).toJsonObject()
        is List<*> -> JsonArray(map { it.toJsonElement() })
        is Boolean -> JsonPrimitive(this)
        is Number -> JsonPrimitive(this)
        is String -> JsonPrimitive(this)
        else -> JsonPrimitive(toString())
    }
}

