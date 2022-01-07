package MPFrankenstein;

import battlecode.common.*;
import MPFrankenstein.Debug.*;
import MPFrankenstein.Util.*;

public class Comms {
    static final int mapLocToFlag = 8;

    static final int setupFlag = 0;
    static final int firstArchon = 1;
    static final int lastArchon = 4;
    static final int firstEnemy = 5;
    static final int lastEnemy = 8;
    static final int firstArchonFlag = 9;
    static final int idList = 13;
    static final int MINER_COUNTER_IDX = 14;
    static final int MINER_MINING_COUNTER_IDX = 15;
    static final int STATE_STORAGE_IDX = 16; // NOT USED
    static final int SOLDIER_COUNTER_IDX = 17;
    static final int BUILDER_REQUEST_IDX = 18;
    static final int BUILDER_COUNTER_IDX = 19;
    static final int FIRST_ROUNDS_BUILD_COUNTER_IDX = 20;
    static final int ARCHON_COMM_IDX = 21;
    static final int SOLDIER_STATE_IDX = 22;
    static final int FIRST_HELPER_COUNTER = 23;
    static final int SECOND_HELPER_COUNTER = 24;
    static final int LAST_ROUND_AVG_ENEMY_LOC_IDX_1 = 25;
    static final int CURR_ROUND_AVG_ENEMY_LOC_IDX_1 = 26;
    static final int CURR_ROUND_NUM_ENEMIES_IDX_1 = 27;
    static final int LAST_ROUND_AVG_ENEMY_LOC_IDX_2 = 28;
    static final int CURR_ROUND_AVG_ENEMY_LOC_IDX_2 = 29;
    static final int CURR_ROUND_NUM_ENEMIES_IDX_2 = 30;
    static final int LAST_ROUND_AVG_ENEMY_LOC_IDX_3 = 31;
    static final int CURR_ROUND_AVG_ENEMY_LOC_IDX_3 = 32;
    static final int CURR_ROUND_NUM_ENEMIES_IDX_3 = 33;

    static final int COUNT_MASK = 7;
    static final int COORD_MASK = 63;
    static final int HEALTH_MASK = 15;
    static final int STATE_MASK = 15;
    static final int HELPER_MASK = 255;
    static final int MAX_HELPER_MASK = 63;
    static final int COUNT_OFFSET = 3;
    static final int MAX_HELPER_OFFSET = 6;
    static final int X_COORD_OFFSET = 0;
    static final int Y_COORD_OFFSET = 6;
    static final int HEALTH_OFFSET = 12;
    static final int HELPER_OFFSET = 8;
    static final int HEALTH_BUCKET_SIZE = 80;
    static final int NUM_HEALTH_BUCKETS = 16;
    static final int DEAD_ARCHON_FLAG = 65535;

    static final int ID_MASK = 15;
    static final int ID_OFFSET_1 = 0;
    static final int ID_OFFSET_2 = 4;
    static final int ID_OFFSET_3 = 8;
    static final int ID_OFFSET_4 = 12;
    static final int MINER_COUNTER_OFFSET = 8;
    static final int MINER_MASK = 0xFF;

    private static RobotController rc;
    public static boolean foundEnemy;

    public enum InformationCategory {
        EMPTY,
        DEFENSE_SOLDIERS,
        UNDER_ATTACK,
    }

    public enum SoldierStateCategory {
        EMPTY,
        RUSH_SOLDIERS,
    }

    // Categories of information to tell the Archons
    public enum ArchonInfo {
        FOUND_ENEMY,
    }

    static final int FOUND_ENEMY_ARCHON_INFO = (1 << ArchonInfo.FOUND_ENEMY.ordinal());

    static void init(RobotController r) {
        rc = r;
        foundEnemy = false;
    }
    
    public static int encodeArchonFlag(InformationCategory cat) {
        return cat.ordinal();
    }

    public static int encodeSoldierStateFlag(SoldierStateCategory cat) {
        return cat.ordinal();
    }

    public static InformationCategory getICFromFlag(int flag) {
        return InformationCategory.values()[flag];
    }

    public static SoldierStateCategory getSoldierCatFromFlag(int flag) {
        return SoldierStateCategory.values()[flag];
    }

    public static int friendlyArchonCount() throws GameActionException {
        int flag = rc.readSharedArray(setupFlag);
        return flag & COUNT_MASK;
    }
    public static int enemyArchonCount() throws GameActionException {
        int flag = rc.readSharedArray(setupFlag);
        return (flag >> COUNT_OFFSET) & COUNT_MASK;
    }
    public static void setMaxHelper(int maximum) throws GameActionException {
        int flag = rc.readSharedArray(setupFlag);
        int clearedFlag = (~(MAX_HELPER_MASK << MAX_HELPER_OFFSET)) & flag;
        int newFlag = clearedFlag | (maximum << MAX_HELPER_OFFSET);
        Comms.writeIfChanged(setupFlag, newFlag);
    }
    public static int readMaxHelper() throws GameActionException {
        int flag = rc.readSharedArray(setupFlag);
        return (flag >> MAX_HELPER_OFFSET) & MAX_HELPER_MASK;
    }
    public static int aliveEnemyArchonCount() throws GameActionException {
        int res = 0;
        for (int i = 0; i < Comms.enemyArchonCount(); i++) {
            int enemyLocFlag = rc.readSharedArray(Comms.firstEnemy + i);
            if (enemyLocFlag != DEAD_ARCHON_FLAG) {
                res += 1;
            }
        }
        return res;
    }
    // Returns array index to put current friendly Archon location in
    public static int incrementFriendly() throws GameActionException {
        int oldFlag = rc.readSharedArray(setupFlag);
        //updating num friendly archons discovered
        int newFlag = oldFlag + 1;
        rc.writeSharedArray(0, newFlag);
        return firstArchon + (oldFlag & COUNT_MASK);
    }
    // Returns array index to put current enemy Archon location in
    public static int incrementEnemy(int id) throws GameActionException {
        int oldFlag = rc.readSharedArray(setupFlag);
        int oldCount = (oldFlag >> COUNT_OFFSET) & COUNT_MASK;
        int newFlag = oldFlag + (1 << COUNT_OFFSET);
        rc.writeSharedArray(0, newFlag);
        storeID(id, oldCount);
        return firstEnemy + oldCount;
    }

    public static int xcoord(int flag) {
        return flag & COORD_MASK;
    }
    public static int ycoord(int flag) {
        return (flag >> Y_COORD_OFFSET) & COORD_MASK;
    }
    
    public static int getHealthBucket(int flag) {
        return (flag >> HEALTH_OFFSET) & HEALTH_MASK;
    }
    public static int getHealth(int flag) {
        return getHealthBucket(flag) * HEALTH_BUCKET_SIZE;
    }

    public static MapLocation locationFromFlag(int flag) {
        return new MapLocation(xcoord(flag), ycoord(flag));
    }

    public static int encodeLocation() {
        return rc.getLocation().x + (rc.getLocation().y << Y_COORD_OFFSET);
    }
    public static int encodeLocation(RobotInfo robot) {
        return robot.getLocation().x + (robot.getLocation().y << Y_COORD_OFFSET);
    }
    public static int encodeLocation(MapLocation loc, int healthBucket) {
        return loc.x + (loc.y << Y_COORD_OFFSET) + (healthBucket << HEALTH_OFFSET);
    }

    public static int encodeLocation(MapLocation loc) {
        return loc.x + (loc.y << Y_COORD_OFFSET);
    }

    public static void storeID(int id, int number) throws GameActionException {
        int oldFlag = rc.readSharedArray(idList);
        int newFlag = (id << (4 * number)) | oldFlag;
        Comms.writeIfChanged(idList, newFlag);
    }
    public static int[] getIDs() throws GameActionException {
        int idFlag = rc.readSharedArray(idList);
        int id1 = (idFlag >> ID_OFFSET_1) & ID_MASK;
        int id2 = (idFlag >> ID_OFFSET_2) & ID_MASK;
        int id3 = (idFlag >> ID_OFFSET_3) & ID_MASK;
        int id4 = (idFlag >> ID_OFFSET_4) & ID_MASK;
        return new int[]{id1, id2, id3, id4};
    }

    public static MapLocation[] getEnemyArchonLocations() throws GameActionException {
        MapLocation[] res = new MapLocation[enemyArchonCount()];
        for (int i = firstEnemy; i < firstEnemy + enemyArchonCount(); i++) {
            res[i - firstEnemy] = locationFromFlag(rc.readSharedArray(i));
        }
        return res;
    }

    public static MapLocation[] getFriendlyArchonLocations() throws GameActionException {
        MapLocation[] res = new MapLocation[friendlyArchonCount()];
        for (int i = firstArchon; i < firstArchon + friendlyArchonCount(); i++) {
            res[i - firstArchon] = locationFromFlag(rc.readSharedArray(i));
        }
        return res;
    }


    // The upper half of 16 bits hold the robot count for the last turn.
    // Archons move the lower half into the upper half if the upper is 0.
    // Archons also zero the lower half.
    public static int getMinerCount() throws GameActionException {
        int minerFlag = rc.readSharedArray(MINER_COUNTER_IDX);
        int lastCount = (minerFlag >> MINER_COUNTER_OFFSET) & MINER_MASK;
        int currCount = minerFlag & MINER_MASK;

        if(lastCount == 0) {
            rc.writeSharedArray(MINER_COUNTER_IDX, currCount << MINER_COUNTER_OFFSET);
            return currCount;
        }
        
        return lastCount;
    }

    // The lower half holds the running count updated by the miners.
    // Miners zero out the upper half if they see it's not 0.
    public static void incrementMinerCounter() throws GameActionException {
        int minerFlag = rc.readSharedArray(MINER_COUNTER_IDX);
        int lastCount = (minerFlag >> MINER_COUNTER_OFFSET) & MINER_MASK;
        int currCount = minerFlag & MINER_MASK;

        if(lastCount != 0) {
            rc.writeSharedArray(MINER_COUNTER_IDX, 1);
        } else {
            rc.writeSharedArray(MINER_COUNTER_IDX, currCount + 1);
        }
    }

    public static int getMinerMiningCount() throws GameActionException {
        int minerFlag = rc.readSharedArray(MINER_MINING_COUNTER_IDX);
        int lastCount = (minerFlag >> MINER_COUNTER_OFFSET) & MINER_MASK;
        int currCount = minerFlag & MINER_MASK;

        if(lastCount == 0) {
            rc.writeSharedArray(MINER_MINING_COUNTER_IDX, currCount << MINER_COUNTER_OFFSET);
            return currCount;
        }
        
        return lastCount;
    }

    public static void incrementMinerMiningCounter() throws GameActionException {
        int minerFlag = rc.readSharedArray(MINER_MINING_COUNTER_IDX);
        int lastCount = (minerFlag >> MINER_COUNTER_OFFSET) & MINER_MASK;
        int currCount = minerFlag & MINER_MASK;

        if(lastCount != 0) {
            rc.writeSharedArray(MINER_MINING_COUNTER_IDX, 1);
        } else {
            rc.writeSharedArray(MINER_MINING_COUNTER_IDX, currCount + 1);
        }
    }
    
    // The upper half of 16 bits hold the robot count for the last turn.
    // Archons move the lower half into the upper half if the upper is 0.
    // Archons also zero the lower half.
    public static int getRushSoldierCount() throws GameActionException {
        int soldierFlag = rc.readSharedArray(SOLDIER_COUNTER_IDX);
        int lastCount = (soldierFlag >> MINER_COUNTER_OFFSET) & MINER_MASK;
        int currCount = soldierFlag & MINER_MASK;

        if(lastCount == 0) {
            rc.writeSharedArray(SOLDIER_COUNTER_IDX, currCount << MINER_COUNTER_OFFSET);
            return currCount;
        }
        
        return lastCount;
    }

    // The lower half holds the running count updated by the miners.
    // Miners zero out the upper half if they see it's not 0.
    public static void incrementRushSoldierCounter() throws GameActionException {
        int soldierFlag = rc.readSharedArray(SOLDIER_COUNTER_IDX);
        int lastCount = (soldierFlag >> MINER_COUNTER_OFFSET) & MINER_MASK;
        int currCount = soldierFlag & MINER_MASK;

        if(lastCount != 0) {
            rc.writeSharedArray(SOLDIER_COUNTER_IDX, 1);
        } else if (currCount < 255) {
            rc.writeSharedArray(SOLDIER_COUNTER_IDX, currCount + 1);
        }
    }

    public static int getBuilderCount() throws GameActionException {
        int builderFlag = rc.readSharedArray(BUILDER_COUNTER_IDX);
        int lastCount = (builderFlag >> MINER_COUNTER_OFFSET) & MINER_MASK;
        int currCount = builderFlag & MINER_MASK;
        if (lastCount == 0) {
            rc.writeSharedArray(BUILDER_COUNTER_IDX, currCount << MINER_COUNTER_OFFSET);
            return currCount;
        }
        return lastCount;
    }
    public static void incrementBuilderCount() throws GameActionException {
        int builderFlag = rc.readSharedArray(BUILDER_COUNTER_IDX);
        int currCount = builderFlag & MINER_MASK;

        rc.writeSharedArray(BUILDER_COUNTER_IDX, currCount + 1);
    }

    public static void incrementBuiltRobots(int archonTurnNum, int robotCounter) throws GameActionException {
        int currFlag = rc.readSharedArray(FIRST_ROUNDS_BUILD_COUNTER_IDX);
        int thisArchonsBuilt = (currFlag >> (4 * (archonTurnNum - 1))) & STATE_MASK;
        int newBucket = robotCounter / 3;
        if (thisArchonsBuilt != newBucket && newBucket < 15) {
            int newFlag = currFlag + (1 << (4 * (archonTurnNum - 1)));
            rc.writeSharedArray(FIRST_ROUNDS_BUILD_COUNTER_IDX, newFlag);
        }
    }

    public static int getArchonWithLeastFirstRoundBuilt() throws GameActionException {
        int currFlag = rc.readSharedArray(FIRST_ROUNDS_BUILD_COUNTER_IDX);
        int minTowersBuilt = Integer.MAX_VALUE;
        int archonCount = rc.getArchonCount();
        for (int i = 0; i < archonCount; i++) {
            int thisArchonsBuilt = (currFlag >> (4 * i)) & STATE_MASK;
            if (thisArchonsBuilt < minTowersBuilt) {
                minTowersBuilt = thisArchonsBuilt;
            }
        }
        int numTied = 0;
        int[] tiedIds = new int[archonCount];
        for (int i = 0; i < archonCount; i++) {
            int thisArchonsBuilt = (currFlag >> (4 * i)) & STATE_MASK;
            if (thisArchonsBuilt == minTowersBuilt) {
                tiedIds[numTied] = i + 1;
                numTied++;
            }
        }
        return tiedIds[Util.rng.nextInt(numTied)];
    }

    // Lets Archons know when the first enemy has been found
    public static void broadcastEnemyFound() throws GameActionException {
        if(!foundEnemy) {
            int archonInfo = rc.readSharedArray(ARCHON_COMM_IDX);
            foundEnemy = (archonInfo & FOUND_ENEMY_ARCHON_INFO) == 1;
            if(rc.senseNearbyRobots(rc.getType().visionRadiusSquared, 
                rc.getTeam().opponent()).length > 0) {
                foundEnemy = true;
                Comms.writeIfChanged(ARCHON_COMM_IDX, archonInfo | FOUND_ENEMY_ARCHON_INFO);
            }
        }
    }

    public static boolean isArchonDead(int idx) throws GameActionException {
        if (rc.readSharedArray(idx + firstEnemy) == DEAD_ARCHON_FLAG) {
            return true;
        }
        return false;
    }

    public static int getHelpersForArchon(int archonNumber) throws GameActionException {
        int counter;
        if (archonNumber == 1 || archonNumber == 2) {
            counter = FIRST_HELPER_COUNTER;
        } else {
            counter = SECOND_HELPER_COUNTER;
        }
        int oldFlag = rc.readSharedArray(counter);
        if (archonNumber == 1 || archonNumber == 3) {
            return oldFlag & HELPER_MASK;
        } else {
            return (oldFlag >> HELPER_OFFSET) & HELPER_MASK;
        }
    }
    public static void incrementHelpersForArchon(int archonNumber) throws GameActionException {
        int counter;
        if (archonNumber == 1 || archonNumber == 2) {
            counter = FIRST_HELPER_COUNTER;
        } else {
            counter = SECOND_HELPER_COUNTER;
        }
        int oldFlag = rc.readSharedArray(counter);
        int lowerCount = oldFlag & HELPER_MASK;
        int upperCount = (oldFlag >> HELPER_OFFSET) & HELPER_MASK;
        if (archonNumber == 1 || archonNumber == 3) {
            lowerCount++;
        } else {
            upperCount++;
        }
        int newFlag = (upperCount << HELPER_OFFSET) | lowerCount;
        rc.writeSharedArray(counter, newFlag);
    }

    public static void writeIfChanged(int index, int flag) throws GameActionException {
        int oldFlag = rc.readSharedArray(index);
        if (flag == oldFlag) {
            return;
        } else {
            rc.writeSharedArray(index, flag);
        }
    }

    public static MapLocation getClosestCluster(MapLocation currLoc) throws GameActionException {
        MapLocation closestCluster = null;
        int closestClusterDist = Integer.MAX_VALUE;
        int numClusters = 0;
        for (int i = 0; i < 3; i++) {
            if (rc.readSharedArray(i * 3 + LAST_ROUND_AVG_ENEMY_LOC_IDX_1) != 0) {
                numClusters++;
            }
        }
        MapLocation currAvgLoc1 = locationFromFlag(rc.readSharedArray(LAST_ROUND_AVG_ENEMY_LOC_IDX_1));
        MapLocation currAvgLoc2 = locationFromFlag(rc.readSharedArray(LAST_ROUND_AVG_ENEMY_LOC_IDX_2));
        MapLocation currAvgLoc3 = locationFromFlag(rc.readSharedArray(LAST_ROUND_AVG_ENEMY_LOC_IDX_3));
        MapLocation[] currAvgLocs = new MapLocation[]{currAvgLoc1, currAvgLoc2, currAvgLoc3};
        for (int i = 0; i < numClusters; i++) {
            MapLocation currCluster = currAvgLocs[i];
            int dist = currLoc.distanceSquaredTo(currCluster);
            if (dist < closestClusterDist) {
                closestClusterDist = dist;
                closestCluster = currCluster;
            }
        }
        return closestCluster;
    }

    /**
     * update avg enemy loc (MapLocation enemyLoc)
     * update curr round running avg
     * also add 1 to curr num enemies
     */
    public static void updateAvgEnemyLoc(MapLocation enemyLoc) throws GameActionException {
        int numClusters = 0;
        for (int i = 0; i < 3; i++) {
            if (rc.readSharedArray(i * 3 + CURR_ROUND_AVG_ENEMY_LOC_IDX_1) != 0) {
                numClusters++;
            }
        }
        MapLocation currAvgLoc1 = locationFromFlag(rc.readSharedArray(CURR_ROUND_AVG_ENEMY_LOC_IDX_1));
        MapLocation currAvgLoc2 = locationFromFlag(rc.readSharedArray(CURR_ROUND_AVG_ENEMY_LOC_IDX_2));
        MapLocation currAvgLoc3 = locationFromFlag(rc.readSharedArray(CURR_ROUND_AVG_ENEMY_LOC_IDX_3));
        MapLocation[] currAvgLocs = new MapLocation[]{currAvgLoc1, currAvgLoc2, currAvgLoc3};

        int closestClusterDist = Integer.MAX_VALUE;
        int closestClusterIdx = -1;
        for (int i = 0; i < numClusters; i++) {
            MapLocation clusterLoc = currAvgLocs[i];
            int distToCluster = clusterLoc.distanceSquaredTo(enemyLoc);
            if (distToCluster < closestClusterDist) {
                closestClusterDist = distToCluster;
                closestClusterIdx = i;
            }
        }
        if (closestClusterDist > Util.MAP_MAX_DIST_SQUARED / 9 && numClusters < 3) {
            closestClusterIdx = numClusters;
        }
        int numEnemies = rc.readSharedArray(closestClusterIdx * 3 + CURR_ROUND_NUM_ENEMIES_IDX_1);
        int currX = currAvgLocs[closestClusterIdx].x;
        int currY = currAvgLocs[closestClusterIdx].y;
        int newX = (currX * numEnemies + enemyLoc.x) / (numEnemies + 1);
        int newY = (currY * numEnemies + enemyLoc.y) / (numEnemies + 1);
        int encodedNewLoc = encodeLocation(new MapLocation(newX, newY));
        writeIfChanged(CURR_ROUND_AVG_ENEMY_LOC_IDX_1 + closestClusterIdx * 3, encodedNewLoc);
        rc.writeSharedArray(CURR_ROUND_NUM_ENEMIES_IDX_1 + closestClusterIdx * 3, numEnemies + 1);
    }

    /**
     * first archon will move CURR_ROUND_AVG_ENEMY_LOC_IDX to LAST_ROUND_AVG_ENEMY_LOC_IDX
     * will also zero out CURR_ROUND_AVG_ENEMY_LOC_IDX
     * will also zero out CURR_ROUND_NUM_ENEMIES_IDX
     */
    public static void resetAvgEnemyLoc(int archonNum) throws GameActionException {
        if (archonNum == 1) {
            int currAvgLoc1 = rc.readSharedArray(CURR_ROUND_AVG_ENEMY_LOC_IDX_1);
            writeIfChanged(LAST_ROUND_AVG_ENEMY_LOC_IDX_1, currAvgLoc1);
            writeIfChanged(CURR_ROUND_AVG_ENEMY_LOC_IDX_1, 0);
            writeIfChanged(CURR_ROUND_NUM_ENEMIES_IDX_1, 0);
            int currAvgLoc2 = rc.readSharedArray(CURR_ROUND_AVG_ENEMY_LOC_IDX_2);
            writeIfChanged(LAST_ROUND_AVG_ENEMY_LOC_IDX_2, currAvgLoc2);
            writeIfChanged(CURR_ROUND_AVG_ENEMY_LOC_IDX_2, 0);
            writeIfChanged(CURR_ROUND_NUM_ENEMIES_IDX_2, 0);
            int currAvgLoc3 = rc.readSharedArray(CURR_ROUND_AVG_ENEMY_LOC_IDX_3);
            writeIfChanged(LAST_ROUND_AVG_ENEMY_LOC_IDX_3, currAvgLoc3);
            writeIfChanged(CURR_ROUND_AVG_ENEMY_LOC_IDX_3, 0);
            writeIfChanged(CURR_ROUND_NUM_ENEMIES_IDX_3, 0);
        }
    }
}
