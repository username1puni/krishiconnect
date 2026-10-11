package com.krishiconnect.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CropRuleTest {

    @Test
    void normalWindow() {
        CropRule kharifHarvest = rule(11, 12);
        assertThat(kharifHarvest.covers(11)).isTrue();
        assertThat(kharifHarvest.covers(12)).isTrue();
        assertThat(kharifHarvest.covers(1)).isFalse();
        assertThat(kharifHarvest.covers(10)).isFalse();
    }

    @Test
    void windowAcrossTheNewYear() {
        CropRule caneCrushing = rule(11, 4);
        assertThat(caneCrushing.covers(1)).isTrue();
        assertThat(caneCrushing.covers(4)).isTrue();
        assertThat(caneCrushing.covers(11)).isTrue();
        assertThat(caneCrushing.covers(7)).isFalse();
    }

    @Test
    void wholeYear() {
        CropRule coconut = rule(1, 12);
        for (int m = 1; m <= 12; m++) {
            assertThat(coconut.covers(m)).isTrue();
        }
    }

    private static CropRule rule(int from, int to) {
        CropRule r = new CropRule();
        r.setMonthFrom(from);
        r.setMonthTo(to);
        return r;
    }
}
