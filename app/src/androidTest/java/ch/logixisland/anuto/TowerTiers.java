package ch.logixisland.anuto;

import java.util.HashMap;

import ch.logixisland.anuto.business.tower.TowerInserter;
import ch.logixisland.anuto.engine.logic.entity.EntityRegistry;
import ch.logixisland.anuto.entity.tower.Tower;
import ch.logixisland.anuto.util.iterator.Function;
import ch.logixisland.anuto.util.iterator.StreamIterator;

public class TowerTiers {

    private final GameFactory mGameFactory;
    private final HashMap<String, Integer> mTowerTierCache = new HashMap<>();

    public TowerTiers(GameFactory gameFactory) {
        mGameFactory = gameFactory;
    }

    protected StreamIterator<Tower> getBuildableTowers() {
        final TowerInserter towerInserter = mGameFactory.getTowerInserter();

        return StreamIterator.fromIterable(towerInserter.getAssignedSlots())
                .map(new Function<Integer, Tower>() {
                    @Override
                    public Tower apply(Integer slot) {
                        return towerInserter.createPreviewTower(slot);
                    }
                });
    }

    protected int getTowerTier(Tower tower) {
        if (mTowerTierCache.isEmpty()) {
            initTowerTierCache();
        }

        return mTowerTierCache.get(tower.getEntityName());
    }

    private void initTowerTierCache() {
        EntityRegistry entityRegistry = mGameFactory.getEntityRegistry();
        StreamIterator<Tower> iterator = getBuildableTowers();
        mTowerTierCache.clear();

        while (iterator.hasNext()) {
            int tier = 1;
            Tower tower = iterator.next();
            mTowerTierCache.put(tower.getEntityName(), tier);

            while (tower.isUpgradeable()) {
                tower = (Tower) entityRegistry.createEntity(tower.getUpgradeName());
                tier += 1;
                mTowerTierCache.put(tower.getEntityName(), tier);
            }
        }
    }

}
