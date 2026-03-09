package com.example.demo.game.model;

public class Player implements CombatUnit {
    public static final int BASE_STRENGTH = 12;
    public static final int BASE_AGILITY = 12;
    public static final int BASE_INTELLIGENCE = 12;
    public static final int ATTRIBUTE_GAIN_PER_LEVEL = 2;
    public static final int BASE_MAX_HP = 48;
    public static final int BASE_MAX_MANA = 40;
    public static final int BASE_DEFENSE = 1;
    public static final double BASE_HP_REGEN = 0.8;
    public static final double BASE_MANA_REGEN = 0.6;
    public static final int HP_PER_STRENGTH = 6;
    public static final int MANA_PER_INTELLIGENCE = 5;
    public static final double HP_REGEN_PER_STRENGTH = 0.05;
    public static final double MANA_REGEN_PER_INTELLIGENCE = 0.06;
    public static final double ATTACK_SPEED_PER_AGILITY = 0.01;
    public static final int AGILITY_PER_DEFENSE = 12;
    public static final int PRIMARY_ATTRIBUTE_PER_DAMAGE = 3;

    public Team team = Team.LIGHT;
    public double x;
    public double y;
    public double spawnX;
    public double spawnY;
    public double radius = 12.0;

    public int hp;
    public int maxHp = 100;
    public int mana;
    public int maxMana = 100;
    public int defense = 2;
    public double hpRegenPerSecond;
    public double manaRegenPerSecond;
    public double attackSpeedMultiplier = 1.0;
    public int attackDamageBonus;

    public HeroAttribute primaryAttribute = HeroAttribute.AGILITY;
    public int strength = BASE_STRENGTH;
    public int agility = BASE_AGILITY;
    public int intelligence = BASE_INTELLIGENCE;
    public int strengthGainPerLevel = ATTRIBUTE_GAIN_PER_LEVEL;
    public int agilityGainPerLevel = ATTRIBUTE_GAIN_PER_LEVEL;
    public int intelligenceGainPerLevel = ATTRIBUTE_GAIN_PER_LEVEL;

    public int level = 1;
    public int xp = 0;
    public int xpToNextLevel = 40;

    public boolean moving;
    public double animPhase;
    public double aimAngle;
    public double hitCooldown;
    public double respawnTimer;
    public double attackTimer;
    public double attackAnimationTimer;
    public double deathAnimationTimer;
    public AnimationState state = AnimationState.IDLE;

    private double hpRegenRemainder;
    private double manaRegenRemainder;

    public void initializeAttributes(HeroAttribute attribute) {
        primaryAttribute = attribute;
        strength = BASE_STRENGTH;
        agility = BASE_AGILITY;
        intelligence = BASE_INTELLIGENCE;
        strengthGainPerLevel = ATTRIBUTE_GAIN_PER_LEVEL;
        agilityGainPerLevel = ATTRIBUTE_GAIN_PER_LEVEL;
        intelligenceGainPerLevel = ATTRIBUTE_GAIN_PER_LEVEL;
        recalculateDerivedStats();
        restoreFullResources();
    }

    public void applyLevelUpAttributes() {
        strength += strengthGainPerLevel;
        agility += agilityGainPerLevel;
        intelligence += intelligenceGainPerLevel;
        recalculateDerivedStats();
    }

    public void recalculateDerivedStats() {
        maxHp = BASE_MAX_HP + strength * HP_PER_STRENGTH;
        maxMana = BASE_MAX_MANA + intelligence * MANA_PER_INTELLIGENCE;
        defense = BASE_DEFENSE + agility / AGILITY_PER_DEFENSE;
        hpRegenPerSecond = BASE_HP_REGEN + strength * HP_REGEN_PER_STRENGTH;
        manaRegenPerSecond = BASE_MANA_REGEN + intelligence * MANA_REGEN_PER_INTELLIGENCE;
        attackSpeedMultiplier = 1.0 + agility * ATTACK_SPEED_PER_AGILITY;
        attackDamageBonus = primaryAttributeValue() / PRIMARY_ATTRIBUTE_PER_DAMAGE;
        hp = Math.min(hp, maxHp);
        mana = Math.min(mana, maxMana);
    }

    public int attributeValue(HeroAttribute attribute) {
        return switch (attribute) {
            case STRENGTH -> strength;
            case AGILITY -> agility;
            case INTELLIGENCE -> intelligence;
        };
    }

    public int primaryAttributeValue() {
        return attributeValue(primaryAttribute);
    }

    public void restoreFullResources() {
        hp = maxHp;
        mana = maxMana;
        hpRegenRemainder = 0.0;
        manaRegenRemainder = 0.0;
    }

    public void regenerate(double dt) {
        if (hp > 0 && hp < maxHp) {
            hpRegenRemainder += hpRegenPerSecond * dt;
            int recoveredHp = (int) hpRegenRemainder;
            if (recoveredHp > 0) {
                hp = Math.min(maxHp, hp + recoveredHp);
                hpRegenRemainder -= recoveredHp;
            }
        } else {
            hpRegenRemainder = 0.0;
        }

        if (hp > 0 && mana < maxMana) {
            manaRegenRemainder += manaRegenPerSecond * dt;
            int recoveredMana = (int) manaRegenRemainder;
            if (recoveredMana > 0) {
                mana = Math.min(maxMana, mana + recoveredMana);
                manaRegenRemainder -= recoveredMana;
            }
        } else {
            manaRegenRemainder = 0.0;
        }
    }

    public boolean hasMana(int amount) {
        return mana >= Math.max(0, amount);
    }

    public boolean spendMana(int amount) {
        int manaCost = Math.max(0, amount);
        if (!hasMana(manaCost)) {
            return false;
        }
        mana -= manaCost;
        return true;
    }

    @Override
    public Team getTeam() {
        return team;
    }

    @Override
    public double getX() {
        return x;
    }

    @Override
    public double getY() {
        return y;
    }

    @Override
    public void setX(double x) {
        this.x = x;
    }

    @Override
    public void setY(double y) {
        this.y = y;
    }

    @Override
    public double getRadius() {
        return radius;
    }

    @Override
    public int getHp() {
        return hp;
    }

    @Override
    public void setHp(int hp) {
        this.hp = hp;
    }

    @Override
    public int getMaxHp() {
        return maxHp;
    }

    @Override
    public int getDamage() {
        return 0;
    }

    @Override
    public int getDefense() {
        return defense;
    }

    @Override
    public double getAttackRange() {
        return 0.0;
    }

    @Override
    public double getAttackCooldown() {
        return 0.0;
    }

    @Override
    public double getAttackTimer() {
        return attackTimer;
    }

    @Override
    public void setAttackTimer(double attackTimer) {
        this.attackTimer = attackTimer;
    }

    @Override
    public boolean isMoving() {
        return moving;
    }

    @Override
    public void setMoving(boolean moving) {
        this.moving = moving;
    }

    @Override
    public double getAnimPhase() {
        return animPhase;
    }

    @Override
    public void setAnimPhase(double animPhase) {
        this.animPhase = animPhase;
    }

    @Override
    public double getLookAngle() {
        return aimAngle;
    }

    @Override
    public void setLookAngle(double lookAngle) {
        this.aimAngle = lookAngle;
    }

    @Override
    public double getAttackAnimationTimer() {
        return attackAnimationTimer;
    }

    @Override
    public void setAttackAnimationTimer(double attackAnimationTimer) {
        this.attackAnimationTimer = attackAnimationTimer;
    }

    @Override
    public AnimationState getAnimationState() {
        return state;
    }

    @Override
    public void setAnimationState(AnimationState animationState) {
        this.state = animationState;
    }
}
