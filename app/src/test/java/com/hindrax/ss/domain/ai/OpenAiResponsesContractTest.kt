package com.hindrax.ss.domain.ai

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenAiResponsesContractTest {
    @Test
    fun requestPayloadUsesResponsesApiShapeAndDisablesStorage() {
        val payload = OpenAiResponsesPayloadBuilder.build(
            model = "gpt-5.5",
            userInput = "Como analizo este APK propio?",
            appContext = "MODULE=APK"
        )

        val json = Json.parseToJsonElement(payload).jsonObject
        assertEquals("gpt-5.5", json.getValue("model").jsonPrimitive.content)
        assertEquals(false, json.getValue("store").jsonPrimitive.boolean)
        assertTrue(json.getValue("instructions").jsonPrimitive.content.contains("Hindrax"))
        assertTrue(json.getValue("input").jsonPrimitive.content.contains("MODULE=APK"))
    }

    @Test
    fun responseParserExtractsOutputText() {
        val response = """
            {
              "output": [
                {
                  "type": "message",
                  "content": [
                    { "type": "output_text", "text": "Paso 1: valida autorizacion." }
                  ]
                }
              ]
            }
        """.trimIndent()

        assertEquals("Paso 1: valida autorizacion.", OpenAiResponseParser.extractText(response))
    }
}
