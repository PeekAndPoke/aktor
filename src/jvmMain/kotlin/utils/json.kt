package io.peekandpoke.aktor.utils

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.buildJsonObject

fun JsonObject.append(other: JsonObject?): JsonObject =
    if (other == null) this else JsonObject(this + other)

fun JsonObject.append(builderAction: JsonObjectBuilder.() -> Unit): JsonObject =
    this.append(buildJsonObject(builderAction))
