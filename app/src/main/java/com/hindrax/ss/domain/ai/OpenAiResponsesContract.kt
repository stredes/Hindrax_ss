package com.hindrax.ss.domain.ai

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

object OpenAiResponsesPayloadBuilder {
    private val json = Json {
        encodeDefaults = true
    }

    fun build(
        model: String,
        userInput: String,
        appContext: String
    ): String {
        return buildRequest(
            model = model,
            input = JsonPrimitive(
                buildString {
                    appendLine(appContext.trim().ifBlank { "MODULE=HINDRAX_CORE" })
                    appendLine()
                    appendLine("USER_REQUEST:")
                    append(userInput.trim())
                }
            ),
            store = false
        )
    }

    fun buildWithTools(
        model: String,
        userInput: String,
        appContext: String
    ): String {
        return buildRequest(
            model = model,
            input = JsonPrimitive(
                buildString {
                    appendLine(appContext.trim().ifBlank { "MODULE=HINDRAX_CORE" })
                    appendLine()
                    appendLine("USER_REQUEST:")
                    append(userInput.trim())
                }
            ),
            tools = HindraxAiToolSchemas.tools,
            store = true
        )
    }

    fun buildToolOutputFollowUp(
        model: String,
        previousResponseId: String,
        outputs: List<OpenAiToolOutput>
    ): String {
        val input = buildJsonArray {
            outputs.forEach { output ->
                add(buildJsonObject {
                    put("type", "function_call_output")
                    put("call_id", output.callId)
                    put("output", output.output)
                })
            }
        }
        return buildRequest(
            model = model,
            input = input,
            tools = HindraxAiToolSchemas.tools,
            previousResponseId = previousResponseId,
            store = true
        )
    }

    private fun buildRequest(
        model: String,
        input: JsonElement,
        tools: JsonArray = JsonArray(emptyList()),
        previousResponseId: String? = null,
        store: Boolean
    ): String {
        return json.encodeToString(
            OpenAiResponsesRequest(
                model = model.ifBlank { DEFAULT_MODEL },
                instructions = HINDRAX_AI_INSTRUCTIONS,
                input = input,
                tools = tools,
                previous_response_id = previousResponseId,
                store = store
            )
        )
    }

    const val DEFAULT_MODEL = "gpt-5.5"

    private const val HINDRAX_AI_INSTRUCTIONS = """
You are Hindrax AI, an in-app operational assistant for a defensive, owner-authorized Android security lab.
Help the user operate Hindrax by explaining workflows, choosing safe modules, preparing checklists, interpreting local results, and drafting authorized lab commands.
Do not claim that a target is authorized. Ask the user to verify ownership or written permission when needed.
Use the provided Hindrax tools only for defensive, owner-authorized actions. If a user asks to run a tool, call a function instead of only describing the workflow.
You can inspect the full Hindrax catalog and launch allowlisted Termux catalog commands when Termux is installed.
For Termux commands, use only catalog command ids returned by list_hindrax_tools, pass exact argv in arguments, and never invent shell pipelines or hidden background behavior.
Do not hide activity, bypass consent, steal credentials, clone access cards, or provide destructive instructions.
Before invoking a tool against a public target or any HIGH risk catalog tool, require the user to confirm ownership or written permission in the request.
When working from net_disc results, suggest the next safe tool and a narrow port profile before scanning. Common profiles include WEB 80,443,8000,8080,8443; LAN 22,53,80,139,443,445,548,631,8080; IOT 23,80,443,554,1883,5683,8000,8080,8883; HINDRAX 80,443,8080,8443,9999; CYD 80,443,8080,8888.
Keep output concise, practical, and formatted in terminal-friendly ASCII sections.
Prefer defensive analysis, reporting, inventory, task planning, file analysis, APK review, and controlled lab diagnostics.
"""
}

object OpenAiResponseParser {
    private val json = Json { ignoreUnknownKeys = true }

    fun extractResponseId(responseBody: String): String? {
        val root = Json.parseToJsonElement(responseBody).jsonObject
        return root["id"]?.jsonPrimitive?.content
    }

    fun extractText(responseBody: String): String {
        val root = Json.parseToJsonElement(responseBody).jsonObject
        root["error"]?.jsonObject?.get("message")?.jsonPrimitive?.content?.let { message ->
            if (message.isNotBlank()) return "OPENAI_ERROR: $message"
        }

        val output = root["output"] as? JsonArray ?: return "EMPTY_OPENAI_RESPONSE"
        val parts = output.flatMap { item ->
            val content = item.jsonObject["content"]?.jsonArray.orEmpty()
            content.mapNotNull { part ->
                val obj = part.jsonObject
                if (obj["type"]?.jsonPrimitive?.content == "output_text") {
                    obj["text"]?.jsonPrimitive?.content
                } else {
                    null
                }
            }
        }
        return parts.joinToString("\n").trim().ifBlank { "EMPTY_OPENAI_RESPONSE" }
    }

    fun extractFunctionCalls(responseBody: String): List<OpenAiFunctionCall> {
        val root = Json.parseToJsonElement(responseBody).jsonObject
        val output = root["output"] as? JsonArray ?: return emptyList()
        return output.mapNotNull { item ->
            val obj = item.jsonObject
            if (obj["type"]?.jsonPrimitive?.content != "function_call") return@mapNotNull null
            OpenAiFunctionCall(
                callId = obj["call_id"]?.jsonPrimitive?.content.orEmpty(),
                name = obj["name"]?.jsonPrimitive?.content.orEmpty(),
                arguments = obj["arguments"]?.jsonPrimitive?.content.orEmpty()
            ).takeIf { it.callId.isNotBlank() && it.name.isNotBlank() }
        }
    }

    fun parseArguments(arguments: String): JsonObject {
        return runCatching { json.parseToJsonElement(arguments).jsonObject }
            .getOrDefault(JsonObject(emptyMap()))
    }
}

data class OpenAiFunctionCall(
    val callId: String,
    val name: String,
    val arguments: String
)

data class OpenAiToolOutput(
    val callId: String,
    val output: String
)

object HindraxAiToolSchemas {
    val tools: JsonArray = buildJsonArray {
        add(buildJsonObject {
            put("type", "function")
            put("name", "list_hindrax_tools")
            put("description", "List enabled Hindrax native tools and the full allowlisted Termux command catalog available to the assistant.")
            putJsonObject("parameters") {
                put("type", "object")
                putJsonObject("properties") {
                    putJsonObject("category") {
                        put("type", "string")
                        put("description", "Optional category filter such as NETWORK, WEB, OSINT, APK, network-recon, web-security, linux-utilities, or ALL.")
                    }
                }
                putJsonArray("required") { add(JsonPrimitive("category")) }
                put("additionalProperties", false)
            }
            put("strict", true)
        })
        add(buildJsonObject {
            put("type", "function")
            put("name", "run_hindrax_tool")
            put("description", "Launch a supported Hindrax defensive tool after the user has confirmed authorization for the target.")
            putJsonObject("parameters") {
                put("type", "object")
                putJsonObject("properties") {
                    putJsonObject("tool_id") {
                        put("type", "string")
                        put("description", "Tool id, for example ping, dns_lookup, web_headers, osint_discovery, port_scan, or net_disc.")
                    }
                    putJsonObject("target") {
                        put("type", "string")
                        put("description", "IP, domain, URL, APK path, or local file path to analyze.")
                    }
                    putJsonObject("ports") {
                        put("type", "string")
                        put("description", "Optional comma-separated TCP ports for port_scan, for example 22,80,443,8080,9999.")
                    }
                    putJsonObject("authorization_confirmed") {
                        put("type", "boolean")
                        put("description", "True only when the user explicitly confirms ownership or written permission for this target.")
                    }
                }
                putJsonArray("required") {
                    add(JsonPrimitive("tool_id"))
                    add(JsonPrimitive("target"))
                    add(JsonPrimitive("ports"))
                    add(JsonPrimitive("authorization_confirmed"))
                }
                put("additionalProperties", false)
            }
            put("strict", true)
        })
        add(buildJsonObject {
            put("type", "function")
            put("name", "run_termux_catalog_command")
            put("description", "Launch an allowlisted Hindrax catalog command in Termux using RUN_COMMAND_PATH and argv, after required authorization checks.")
            putJsonObject("parameters") {
                put("type", "object")
                putJsonObject("properties") {
                    putJsonObject("command_id") {
                        put("type", "string")
                        put("description", "Exact command id from termuxCatalogTools, for example nmap, dig, whois, curl, apktool, jadx, binwalk, python, or bash.")
                    }
                    putJsonObject("target") {
                        put("type", "string")
                        put("description", "Authorized target, local file, APK path, or empty string for local-only commands.")
                    }
                    putJsonObject("arguments") {
                        put("type", "string")
                        put("description", "Full command argv as a string. Include the target in this argument string when the command needs it, for example '-sV 192.168.1.10'.")
                    }
                    putJsonObject("authorization_confirmed") {
                        put("type", "boolean")
                        put("description", "True only when the user explicitly confirms ownership or written permission for public targets or HIGH risk tools.")
                    }
                }
                putJsonArray("required") {
                    add(JsonPrimitive("command_id"))
                    add(JsonPrimitive("target"))
                    add(JsonPrimitive("arguments"))
                    add(JsonPrimitive("authorization_confirmed"))
                }
                put("additionalProperties", false)
            }
            put("strict", true)
        })
    }
}

@Serializable
private data class OpenAiResponsesRequest(
    val model: String,
    val instructions: String,
    val input: JsonElement,
    val store: Boolean = false,
    val tools: JsonArray = JsonArray(emptyList()),
    val tool_choice: String = "auto",
    val previous_response_id: String? = null,
    val text: OpenAiTextConfig = OpenAiTextConfig(),
    val reasoning: OpenAiReasoningConfig = OpenAiReasoningConfig(),
    val max_output_tokens: Int = 900
)

@Serializable
private data class OpenAiTextConfig(
    val format: OpenAiTextFormat = OpenAiTextFormat(),
    val verbosity: String = "low"
)

@Serializable
private data class OpenAiTextFormat(
    val type: String = "text"
)

@Serializable
private data class OpenAiReasoningConfig(
    val effort: String = "low"
)
