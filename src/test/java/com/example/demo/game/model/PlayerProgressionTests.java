package com.example.demo.game.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerProgressionTests {

    @Test
    void baseAttributesDefineDerivedHeroStats() {
        Player player = new Player();

        player.initializeAttributes(HeroAttribute.AGILITY);

        assertEquals(12, player.strength);
        assertEquals(12, player.agility);
        assertEquals(12, player.intelligence);
        assertEquals(120, player.maxHp);
        assertEquals(120, player.hp);
        assertEquals(100, player.maxMana);
        assertEquals(100, player.mana);
        assertEquals(2, player.defense);
        assertEquals(4, player.attackDamageBonus);
        assertEquals(1.12, player.attackSpeedMultiplier, 0.0001);
        assertEquals(1.4, player.hpRegenPerSecond, 0.0001);
        assertEquals(1.32, player.manaRegenPerSecond, 0.0001);
    }

    @Test
    void levelUpsAndRegenerationIncreaseStatsAndResources() {
        Player player = new Player();
        player.initializeAttributes(HeroAttribute.AGILITY);
        player.hp = 100;
        player.mana = 80;

        player.applyLevelUpAttributes();
        player.applyLevelUpAttributes();
        player.regenerate(1.0);

        assertEquals(16, player.strength);
        assertEquals(16, player.agility);
        assertEquals(16, player.intelligence);
        assertEquals(144, player.maxHp);
        assertEquals(120, player.maxMana);
        assertEquals(2, player.defense);
        assertEquals(5, player.attackDamageBonus);
        assertEquals(1.16, player.attackSpeedMultiplier, 0.0001);
        assertEquals(101, player.hp);
        assertEquals(81, player.mana);
    }
}
