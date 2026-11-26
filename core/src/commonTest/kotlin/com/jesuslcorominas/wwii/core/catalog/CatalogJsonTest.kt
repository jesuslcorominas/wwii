package com.jesuslcorominas.wwii.core.catalog

import com.jesuslcorominas.wwii.core.catalog.json.CatalogJson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class CatalogJsonTest {
    @Test
    fun loadsTheTestCatalog() {
        val catalog = CatalogJson.decode(TestCatalogs.VALID)
        assertEquals("test-small", catalog.id)
        assertEquals("Small test catalog", catalog.name)
        assertEquals(3, catalog.nations.size)
        assertEquals(5, catalog.entries.size)
    }

    @Test
    fun encodeThenDecodeGivesAnEqualCatalog() {
        val catalog = TestCatalogs.valid()
        assertEquals(catalog, CatalogJson.decode(CatalogJson.encode(catalog)))
        assertEquals(catalog, CatalogJson.decode(CatalogJson.encode(catalog, prettyPrint = true)))
    }

    @Test
    fun encodingIsStable() {
        val once = CatalogJson.encode(TestCatalogs.valid())
        assertEquals(once, CatalogJson.encode(CatalogJson.decode(once)))
    }

    @Test
    fun encodeOmitsDefaultValuesAndWritesTheFormatVersion() {
        val text = CatalogJson.encode(TestCatalogs.valid())
        assertTrue(text.startsWith("{\"formatVersion\":1,"))
        assertTrue("{\"id\":\"AA\",\"name\":\"Alfa\",\"role\":\"major\",\"side\":\"axis\"}" in text)
        assertTrue("\"side\":\"western_allies\",\"lateEntry\":true,\"offMap\":true" in text)
        assertTrue("{\"nation\":\"CC\",\"type\":\"naval\",\"count\":2}" in text)
        assertFalse("false" in text)
    }

    @Test
    fun rejectsUnknownFormatVersion() {
        val text = TestCatalogs.VALID.replace("\"formatVersion\": 1", "\"formatVersion\": 2")
        val e = assertFailsWith<InvalidCatalogException> { CatalogJson.decode(text) }
        assertEquals(listOf<CatalogIssue>(CatalogIssue.UnsupportedFormatVersion(2, 1)), e.issues)
    }

    @Test
    fun rejectsMissingFormatVersion() {
        val text = TestCatalogs.VALID.replace("\"formatVersion\": 1,", "")
        val e = assertFailsWith<InvalidCatalogException> { CatalogJson.decode(text) }
        assertIs<CatalogIssue.MalformedDocument>(e.issues.single())
    }

    @Test
    fun rejectsUnknownKeys() {
        val text = TestCatalogs.VALID.replace("\"lateEntry\"", "\"lateEntri\"")
        val e = assertFailsWith<InvalidCatalogException> { CatalogJson.decode(text) }
        assertIs<CatalogIssue.MalformedDocument>(e.issues.single())
    }

    @Test
    fun rejectsUnknownEnumValues() {
        val text = TestCatalogs.VALID.replace("\"type\": \"air\"", "\"type\": \"space\"")
        val e = assertFailsWith<InvalidCatalogException> { CatalogJson.decode(text) }
        assertIs<CatalogIssue.MalformedDocument>(e.issues.single())
    }

    @Test
    fun rejectsBlankNationIdAsMalformed() {
        val text = TestCatalogs.VALID.replace("{ \"id\": \"BB\"", "{ \"id\": \" \"")
        val e = assertFailsWith<InvalidCatalogException> { CatalogJson.decode(text) }
        assertIs<CatalogIssue.MalformedDocument>(e.issues.single())
    }

    @Test
    fun rejectsNonObjectRoot() {
        val e = assertFailsWith<InvalidCatalogException> { CatalogJson.decode("[]") }
        assertIs<CatalogIssue.MalformedDocument>(e.issues.single())
    }

    @Test
    fun validationProblemsSurviveDecoding() {
        val text = TestCatalogs.VALID.replace("\"role\": \"minor\"", "\"role\": \"neutral\"")
        val e = assertFailsWith<InvalidCatalogException> { CatalogJson.decode(text) }
        assertIs<CatalogIssue.RoleSideMismatch>(e.issues.single())
    }
}
