package MPBasic;

import battlecode.common.*;
import MPBasic.Debug.*;
import MPBasic.Util.*;

public class Comms {
    static final int mapLocToFlag = 8;

    static final int firstArchon = 1;
    static final int lastArchon = 4;
    static final int firstEnemy = 5;
    static final int lastEnemy = 8;
    static final int firstArchonFlag = 9;
    static final int idList = 13;
    static final int MINER_COUNTER_IDX = 14;
    static final int MINER_MINING_COUNTER_IDX = 15;

    static final int COUNT_MASK = 7;
    static final int COORD_MASK = 63;
    static final int HEALTH_MASK = 15;
    static final int COUNT_OFFSET = 3;
    static final int X_COORD_OFFSET = 0;
    static final int Y_COORD_OFFSET = 6;
    static final int HEALTH_OFFSET = 12;
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

    public enum InformationCategory {
        EMPTY,
        RUSH_SOLDIERS,
    }

    static void init(RobotController r) {
        rc = r;
    }
    
    public static int encodeArchonFlag(InformationCategory cat) {
        return cat.ordinal();
    }

    public static InformationCategory getICFromFlag(int flag) {
        return InformationCategory.values()[flag];
    }

    public static int friendlyArchonCount() throws GameActionException {
        int flag = rc.readSharedArray(0);
        return flag & COUNT_MASK;
    }
    public static int enemyArchonCount() throws GameActionException {
        int flag = rc.readSharedArray(0);
        return (flag >> COUNT_OFFSET) & COUNT_MASK;
    }
    // Returns array index to put current friendly Archon location in
    public static int incrementFriendly() throws GameActionException {
        int oldFlag = rc.readSharedArray(0);
        //updating num friendly archons discovered
        int newFlag = oldFlag + 1;
        rc.writeSharedArray(0, newFlag);
        return firstArchon + (oldFlag & COUNT_MASK);
    }
    // Returns array index to put current enemy Archon location in
    public static int incrementEnemy(int id) throws GameActionException {
        int oldFlag = rc.readSharedArray(0);
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

    public static void storeID(int id, int number) throws GameActionException {
        int oldFlag = rc.readSharedArray(idList);
        int newFlag = (id << (4 * number)) | oldFlag;
        rc.writeSharedArray(idList, newFlag);
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
}
