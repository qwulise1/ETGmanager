package com.noobexon.xposedfakelocation.manager.localization
 
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
 
class LanguageOptionTest {
    @Test
    fun fromTagReturnsRussianForRussianTag() {
        assertEquals(LanguageOption.RUSSIAN, LanguageOption.fromTag("ru-RU"))
    }
 
    @Test
    fun russianAutonymIsCapitalizedRussian() {
        val autonym = LanguageOption.RUSSIAN.autonym
        assertNotNull(autonym)
        assertEquals("Русский", autonym)
    }
 
    @Test
    fun fromTagFallsBackToSystemForUnknownTag() {
        assertEquals(LanguageOption.SYSTEM, LanguageOption.fromTag("unknown-tag"))
    }
}
