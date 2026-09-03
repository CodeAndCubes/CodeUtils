package com.mrleonardos.codeutils.internal.broadcast;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ColorCodesTest {

    @Test
    void anAmpersandBeforeACodeBecomesTheSignOfTheGame() {
        assertEquals("§aзелёный §lжирный §rобычный", ColorCodes.paint("&aзелёный &lжирный &rобычный"));
    }

    @Test
    void theCodeLetterKeepsItsCase() {
        assertEquals("§Aвверх", ColorCodes.paint("&Aвверх"));
    }

    @Test
    void twoAmpersandsGiveTheSignItself() {
        assertEquals("&a это не цвет", ColorCodes.paint("&&a это не цвет"));
        assertEquals("Ben && Jerry", ColorCodes.paint("Ben &&&& Jerry"));
    }

    @Test
    void anAmpersandBeforeSomethingElseStaysWhereItIs() {
        assertEquals("Ben & Jerry", ColorCodes.paint("Ben & Jerry"));
        assertEquals("конец &", ColorCodes.paint("конец &"));
        assertEquals("&я", ColorCodes.paint("&я"));
    }

    @Test
    void aTextWithoutAmpersandsComesBackAsItIs() {
        assertEquals("просто текст", ColorCodes.paint("просто текст"));
        assertEquals("", ColorCodes.paint(null));
    }

    @Test
    void strippingTakesTheColoursOutForTheLog() {
        assertEquals("зелёный жирный обычный", ColorCodes.strip(ColorCodes.paint("&aзелёный &lжирный &rобычный")));
        assertEquals("&a это не цвет", ColorCodes.strip(ColorCodes.paint("&&a это не цвет")));
        assertEquals("", ColorCodes.strip(null));
    }
}
