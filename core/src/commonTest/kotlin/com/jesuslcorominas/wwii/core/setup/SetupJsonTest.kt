package com.jesuslcorominas.wwii.core.setup

import com.jesuslcorominas.wwii.core.catalog.TestCatalogs
import com.jesuslcorominas.wwii.core.setup.json.SetupJson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SetupJsonTest {
    private val catalog = TestCatalogs.valid()

    private fun issuesOf(text: String): List<SetupIssue> =
        assertFailsWith<InvalidSetupException> { SetupJson.decode(text, catalog) }.issues

    @Test
    fun roundTrip() {
        val setup = TestSetups.valid()
        assertEquals(setup, SetupJson.decode(SetupJson.encode(setup), catalog))
        assertEquals(setup, SetupJson.decode(SetupJson.encode(setup, prettyPrint = true), catalog))
    }

    @Test
    fun encodingIsStableAndOmitsDefaults() {
        val setup = TestSetups.valid()
        val text = SetupJson.encode(setup)
        assertEquals(text, SetupJson.encode(SetupJson.decode(text, catalog)))
        assertFalse(""""naval":0""" in text)
        assertTrue(""""type":"key_city_bonus"""" in text)
        assertTrue(""""type":"controls_city"""" in text)
    }

    @Test
    fun unsupportedVersion() {
        assertEquals(
            listOf<SetupIssue>(SetupIssue.UnsupportedFormatVersion(2, 1)),
            issuesOf(TestSetups.VALID.replace("\"formatVersion\": 1", "\"formatVersion\": 2")),
        )
    }

    @Test
    fun missingVersion() {
        assertIs<SetupIssue.MalformedDocument>(issuesOf("""{ "id": "x" }""").single())
    }

    @Test
    fun notAnObject() {
        assertIs<SetupIssue.MalformedDocument>(issuesOf("[]").single())
        assertIs<SetupIssue.MalformedDocument>(issuesOf("{ nope").single())
    }

    @Test
    fun unknownKeysAreRejected() {
        val text = TestSetups.VALID.replace("\"turns\": 4,", "\"turns\": 4, \"extra\": true,")
        assertIs<SetupIssue.MalformedDocument>(issuesOf(text).single())
    }

    @Test
    fun unknownSpecialTypeIsRejected() {
        val text = TestSetups.VALID.replace("\"type\": \"join_side\"", "\"type\": \"teleport\"")
        assertIs<SetupIssue.MalformedDocument>(issuesOf(text).single())
    }

    @Test
    fun invalidValuesAreMalformed() {
        val blankNation =
            TestSetups.VALID.replace(
                "{ \"nation\": \"BB\", \"territory\": \"BB\"",
                "{ \"nation\": \" \", \"territory\": \"BB\"",
            )
        assertIs<SetupIssue.MalformedDocument>(issuesOf(blankNation).single())
        val blankCity = TestSetups.VALID.replace("\"cityId\": \"colony-town\"", "\"cityId\": \"\"")
        assertIs<SetupIssue.MalformedDocument>(issuesOf(blankCity).single())
    }

    @Test
    fun validationIssuesPassThrough() {
        val text = TestSetups.VALID.replace("\"catalog\": \"test-small\"", "\"catalog\": \"other\"")
        assertEquals(listOf<SetupIssue>(SetupIssue.CatalogMismatch("other", "test-small")), issuesOf(text))
    }

    @Test
    fun issueMessagesAreReadable() {
        val e = assertFailsWith<InvalidSetupException> { SetupJson.decode("[]", catalog) }
        assertTrue(e.message!!.startsWith("Invalid game setup (1 issue(s))"))
    }
}
