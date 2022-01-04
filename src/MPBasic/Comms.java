package MPBasic;

import battlecode.common.*;
import MPBasic.Debug.*;
import MPBasic.Util.*;

public class Comms {
    static final int mapLocToFlag = 8;

    static final int numArchons = 4;
    static final int firstArchon = 1;
    static final int lastArchon = 4;
    static final int firstEnemy = 5;
    static final int lastEnemy = 8;
    static final int idList = 13;

    static final int COORD_MASK = 63;
    static final int HEALTH_MASK = 15;
    static final int X_COORD_OFFSET = 0;
    static final int Y_COORD_OFFSET = 6;
    static final int HEALTH_OFFSET = 12;
    static final int HEALTH_BUCKET_SIZE = 40;

    static final int ID_MASK = 15;
    static final int ID_OFFSET_1 = 0;
    static final int ID_OFFSET_2 = 4;
    static final int ID_OFFSET_3 = 8;
    static final int ID_OFFSET_4 = 12;

    private static RobotController rc;

    static void init(RobotController r) {
        rc = r;
    }

    public static int friendlyArchonCount() throws GameActionException {
        int flag = rc.readSharedArray(0);
        return flag & 3;
    }
    public static int enemyArchonCount() throws GameActionException {
        int flag = rc.readSharedArray(0);
        return (flag & 12) >> 2;
    }
    // Returns array index to put current friendly Archon location in
    public static int incrementFriendly() throws GameActionException {
        int oldFlag = rc.readSharedArray(0);
        //updating num friendly archons discovered
        int newFlag = oldFlag + 1;
        rc.writeSharedArray(0, newFlag);
        return 1 + (oldFlag & 3);
    }
    // Returns array index to put current enemy Archon location in
    public static void incrementEnemy(int id) throws GameActionException {
        int oldFlag = rc.readSharedArray(0);
        int oldCount = (oldFlag >> 2) & 3;
        int newFlag = oldFlag + 4;
        rc.writeSharedArray(0, newFlag);
        storeID(id, oldCount);
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
}