package ch.logixisland.anuto.entity.enemy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import ch.logixisland.anuto.engine.logic.GameEngine;
import ch.logixisland.anuto.engine.logic.entity.Entity;
import ch.logixisland.anuto.entity.Types;
import ch.logixisland.anuto.entity.tower.Tower;
import ch.logixisland.anuto.util.container.KeyValueStore;
import ch.logixisland.anuto.util.iterator.Function;
import ch.logixisland.anuto.util.iterator.Predicate;
import ch.logixisland.anuto.util.math.Vector2;


public abstract class Enemy extends Entity {

    public static Predicate<Enemy> beingTeleported(final boolean value) {
        return new Predicate<Enemy>() {
            @Override
            public boolean apply(Enemy enemy) {
                return enemy.isBeingTeleported() == value;
            }
        };
    }

    public static Function<Enemy, Float> health() {
        return new Function<Enemy, Float>() {
            @Override
            public Float apply(Enemy input) {
                return input.mHealth;
            }
        };
    }

    public static Function<Enemy, Float> distanceRemaining() {
        return new Function<Enemy, Float>() {
            @Override
            public Float apply(Enemy input) {
                return input.getDistanceRemaining();
            }
        };
    }

    private int mWaveNumber;
    private boolean mBeingTeleported;
    private int mReward;
    private float mHealth;
    private float mMaxHealth;
    private float mSpeed;
    private Collection<WeaponType> mWeakAgainst;
    private Collection<WeaponType> mStrongAgainst;
    private float mSpeedModifier;
    private float mMinSpeedModifier;
    private float mWeakAgainstModifier;
    private float mStrongAgainstModifier;
    private List<Vector2> mWayPoints;
    private int mWayPointIndex;

    private HealthBar mHealthBar;

    private final List<EnemyListener> mListeners = new CopyOnWriteArrayList<>();

    Enemy(GameEngine gameEngine, KeyValueStore enemySettings) {
        super(gameEngine);

        mBeingTeleported = false;
        mSpeedModifier = 1f;

        mReward = enemySettings.getInt("reward");
        mHealth = enemySettings.getFloat("health");
        mMaxHealth = enemySettings.getFloat("health");
        mSpeed = enemySettings.getFloat("speed");
        mMinSpeedModifier = enemySettings.getFloat("minSpeedModifier");
        mWeakAgainstModifier = enemySettings.getFloat("weakAgainstModifier");
        mStrongAgainstModifier = enemySettings.getFloat("strongAgainstModifier");
        mWeakAgainst = new ArrayList<>();
        mStrongAgainst = new ArrayList<>();

        for (String name : enemySettings.getStringList("weakAgainst")) {
            mWeakAgainst.add(WeaponType.valueOf(name));
        }

        for (String name : enemySettings.getStringList("strongAgainst")) {
            mStrongAgainst.add(WeaponType.valueOf(name));
        }

        mHealthBar = new HealthBar(getTheme(), this);
    }

    @Override
    public final int getEntityType() {
        return Types.ENEMY;
    }

    @Override
    public void init() {
        super.init();
        getGameEngine().add(mHealthBar);
    }

    @Override
    public void clean() {
        super.clean();
        getGameEngine().remove(mHealthBar);

        for (EnemyListener listener : mListeners) {
            listener.enemyRemoved(this);
        }

        mListeners.clear();
    }

    @Override
    public void tick() {
        super.tick();

        if (mBeingTeleported) {
            return;
        }

        if (!hasWayPoint()) {
            for (EnemyListener listener : mListeners) {
                listener.enemyFinished(this);
            }
            remove();
            return;
        }

        float stepSize = getSpeed() / GameEngine.TARGET_FRAME_RATE;
        if (getDistanceTo(getCurrentWayPoint()) >= stepSize) {
            move(getDirection().mul(stepSize));
        } else {
            setPosition(getCurrentWayPoint());
            mWayPointIndex++;
        }
    }

    public boolean isBeingTeleported() {
        return mBeingTeleported;
    }

    public void setBeingTeleported(boolean beingTeleported) {
        mBeingTeleported = beingTeleported;
    }

    public int getWaveNumber() {
        return mWaveNumber;
    }

    public void setWaveNumber(int waveNumber) {
        mWaveNumber = waveNumber;
    }

    public void setupPath(List<Vector2> wayPoints) {
        setupPath(wayPoints, 0);
    }

    void setupPath(List<Vector2> wayPoints, int wayPointIndex) {
        mWayPoints = wayPoints;
        mWayPointIndex = wayPointIndex;
    }

    private Vector2 getCurrentWayPoint() {
        return mWayPoints.get(mWayPointIndex);
    }

    List<Vector2> getWayPoints() {
        return mWayPoints;
    }

    int getWayPointIndex() {
        return mWayPointIndex;
    }

    boolean hasWayPoint() {
        return mWayPoints != null && mWayPointIndex < mWayPoints.size();
    }

    Vector2 getDirection() {
        if (!hasWayPoint()) {
            return null;
        }

        return getDirectionTo(getCurrentWayPoint());
    }

    public float getSpeed() {
        return mSpeed * Math.max(mMinSpeedModifier, mSpeedModifier);
    }

    public void modifySpeed(float f) {
        mSpeedModifier = mSpeedModifier * f;
    }

    private float getDistanceRemaining() {
        if (!hasWayPoint()) {
            return 0;
        }

        float dist = getDistanceTo(getCurrentWayPoint());

        for (int i = mWayPointIndex + 1; i < mWayPoints.size(); i++) {
            Vector2 wThis = mWayPoints.get(i);
            Vector2 wLast = mWayPoints.get(i - 1);

            dist += wLast.to(wThis).len();
        }

        return dist;
    }

    public Vector2 getPositionAfter(float sec) {
        if (mWayPoints == null) {
            return getPosition();
        }

        float distance = sec * getSpeed();
        int index = mWayPointIndex;
        Vector2 position = getPosition();

        while (index < mWayPoints.size()) {
            Vector2 toWaypoint = position.to(mWayPoints.get(index));
            float toWaypointDist = toWaypoint.len();

            if (distance < toWaypointDist) {
                return position.add(toWaypoint.mul(distance / toWaypointDist));
            } else {
                distance -= toWaypointDist;
                mWayPoints.get(index);
                index++;
            }
        }

        return position;
    }

    public void sendBack(float dist) {
        int index = mWayPointIndex - 1;
        Vector2 pos = getPosition();

        while (index > 0) {
            Vector2 wp = mWayPoints.get(index);
            Vector2 toWp = pos.to(wp);
            float toWpLen = toWp.len();

            if (dist > toWpLen) {
                dist -= toWpLen;
                pos = wp;
                index--;
            } else {
                pos = toWp.norm().mul(dist).add(pos);
                setPosition(pos);
                mWayPointIndex = index + 1;
                return;
            }
        }

        setPosition(mWayPoints.get(0));
        mWayPointIndex = 1;
    }

    float getHealth() {
        return mHealth;
    }

    public float getMaxHealth() {
        return mMaxHealth;
    }

    public void damage(float amount, Entity origin) {
        if (origin != null && origin instanceof Tower) {
            Tower originTower = (Tower) origin;

            if (mWeakAgainst.contains(originTower.getWeaponType())) {
                amount *= mWeakAgainstModifier;
            }

            if (mStrongAgainst.contains(originTower.getWeaponType())) {
                amount *= mStrongAgainstModifier;
            }

            originTower.reportDamageInflicted(amount);
        }

        mHealth -= amount;

        if (mHealth <= 0) {
            for (EnemyListener listener : mListeners) {
                listener.enemyKilled(this);
            }

            remove();
        }
    }

    public void modifyHealth(float f) {
        mHealth *= f;
        mMaxHealth *= f;
    }

    void setHealth(float health, float maxHealth) {
        mHealth = health;
        mMaxHealth = maxHealth;
    }

    public void heal(float amount) {
        mHealth += amount;

        if (mHealth > mMaxHealth) {
            mHealth = mMaxHealth;
        }
    }

    public int getReward() {
        return mReward;
    }

    public void modifyReward(float f) {
        mReward = Math.round(mReward * f);
    }

    void setReward(int reward) {
        mReward = reward;
    }

    public void addListener(EnemyListener listener) {
        mListeners.add(listener);
    }

    public void removeListener(EnemyListener listener) {
        mListeners.remove(listener);
    }

}
