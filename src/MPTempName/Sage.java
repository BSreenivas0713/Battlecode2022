package MPTempName;

import battlecode.common.*;
import MPTempName.Debug.*;
import MPTempName.Util.*;

public class Sage extends Robot{
    static enum SageState {
        EXPLORING,
        GOING_TO_HEAL,
        HEALING,
    }

    static SageState currState;

    static MapLocation healTarget;
    static int healTargetIdx;
    static int healCounter;
    static boolean canHeal;

    static int homeFlagIdx;
    static MapLocation home;
    static int targetId;
    static boolean isRunning;
    static int runSemaphore;
    static Direction runDirection;
    static RobotInfo sensedArchon;
    static RobotInfo inRangeArchon;
    static boolean isTurret;
    static MapLocation attackTarget;
    static MapLocation closestSoldier;
    static int closestSoldierDist;
    static MapLocation closestSage;
    static int closestSageDist;
    static MapLocation closestWatchtower;
    static int closestWatchtowerDist;
    static int bestOverallSoldierHealth;
    static int predictedDamage;
    static int totalBuildingHealth;
    static int numVictims;

    static int overallAttackingEnemyDx;
    static int overallAttackingEnemyDy;
    static int numAttackingEnemies;
    static MapLocation averageAttackingEnemyLocation;
    static int[] chargeRounds;
    static int nextCharge;

    public Sage(RobotController r) throws GameActionException {
        this(r, Comms.firstArchonFlag);
    }

    public Sage(RobotController r, int homeFlagIndex) throws GameActionException {
        super(r);
        home = Comms.locationFromFlag(rc.readSharedArray(homeFlagIndex - Comms.mapLocToFlag));
        homeFlagIdx = homeFlagIndex;
        isRunning = false;
        currState = SageState.EXPLORING;
        canHeal = true;
        AnomalyScheduleEntry[] schedule = rc.getAnomalySchedule();
        int chargeCount = 0;
        for (int i = 0; i < schedule.length; i++) {
            AnomalyScheduleEntry entry = schedule[i];
            if (entry.anomalyType == AnomalyType.CHARGE) {
                chargeCount++;
            }
        }       
        chargeRounds = new int[chargeCount]; 
        int currentCharge = 0;
        for (int i = 0; i < schedule.length; i++) {
            AnomalyScheduleEntry entry = schedule[i];
            if (entry.anomalyType == AnomalyType.CHARGE) {
                chargeRounds[currentCharge] = entry.roundNumber;
                currentCharge++;
            }
        }
        nextCharge = 0;
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();
        Debug.printString("Cool: " + rc.getActionCooldownTurns());
        Comms.incrementSageCounter();
        if (isRunning) {
            runSemaphore--;
        }
        tryAttackArchon();
        scanEnemies();
        boolean almostReady = rc.getActionCooldownTurns() < GameConstants.COOLDOWN_LIMIT + GameConstants.COOLDOWNS_PER_TURN;
        trySwitchState();
        doSageAction(almostReady);
    }

    public void tryAttackArchon() throws GameActionException {
        if (!rc.isActionReady()) {
            return;
        }
        if (inRangeArchon != null && isTurret) {
            Debug.printString("Envisioning Fury");
            rc.envision(AnomalyType.FURY);
            return;
        } else if (sensedArchon != null) {
            Nav.move(sensedArchon.location);
            if (isTurret && rc.canAttack(sensedArchon.location)) {
                Debug.printString("Envisioning Fury");
                rc.envision(AnomalyType.FURY);
                return;
            }
        }
    }

    public void trySwitchState() throws GameActionException {
        switch(currState) {
            case EXPLORING:
                // Run away if 1/2 health left
                if(rc.getHealth() <= robotType.health / 2) {
                    if(canHeal && loadHealTarget()) {
                        currState = SageState.GOING_TO_HEAL;
                    }
                }
                break;
            case GOING_TO_HEAL:
                if(rc.getHealth() == robotType.health) {
                    currState = SageState.EXPLORING;
                } else if(needToReloadTarget()) {
                    if(!reloadTarget()) {
                        currState = SageState.EXPLORING;
                    }
                } else if(currLoc.isWithinDistanceSquared(healTarget, RobotType.ARCHON.actionRadiusSquared)) {
                    currState = SageState.HEALING;
                    healCounter = 0;
                }
                break;
            case HEALING:
                healCounter++;
                if (healCounter == Util.HealTimeout) {
                    currState = SageState.EXPLORING;
                    canHeal = false;
                } else if(rc.getHealth() == robotType.health) {
                    currState = SageState.EXPLORING;
                } else if(needToReloadTarget()) {
                    if(!reloadTarget()) {
                        currState = SageState.EXPLORING;
                    } else {
                        currState = SageState.GOING_TO_HEAL;
                    }
                }
                break;
        }
    }

    public void doSageAction(boolean almostReady) throws GameActionException {
        switch(currState) {
            case EXPLORING:
                if (numAttackingEnemies > 0) {
                    if (almostReady) {
                        Debug.printString("Fighting!");
                        isRunning = false;
                        if (closestSageDist <= RobotType.SAGE.actionRadiusSquared) {
                            tryAttack();
                            Direction dir = closestSage.directionTo(currLoc);
                            Direction newDir = makeDirBetter(dir);
                            if (canMoveRubble(newDir)) {
                                Direction[] dirs = Util.getInOrderDirections(newDir);
                                tryMoveDest(dirs);
                            }
                        } else if (closestSoldierDist <= RobotType.SOLDIER.actionRadiusSquared) {
                            tryAttack();
                            Direction dir = closestSoldier.directionTo(currLoc);
                            Direction newDir = makeDirBetter(dir);
                            if (canMoveRubble(newDir)) {
                                Direction[] dirs = Util.getInOrderDirections(newDir);
                                tryMoveDest(dirs);
                            }
                        } else if (closestSoldierDist <= RobotType.SOLDIER.visionRadiusSquared) {
                            if (tryAttack()) {
                                Direction dir = closestSoldier.directionTo(currLoc);
                                Direction newDir = makeDirBetter(dir);
                                if (canMoveRubble(newDir)) {
                                    Direction[] dirs = Util.getInOrderDirections(newDir);
                                    tryMoveDest(dirs);
                                }
                            } else {
                                Direction moveDir = makeDirBetter(currLoc.directionTo(averageAttackingEnemyLocation));
                                if (canMoveRubble(moveDir)) {
                                    Direction[] dirs = Util.getInOrderDirections(moveDir);
                                    tryMoveDest(dirs);
                                }
                                tryAttack();
                            }
                        } else if (closestSoldierDist <= RobotType.SAGE.actionRadiusSquared) {
                            tryAttack();
                        } else {
                            Direction moveDir = makeDirBetter(currLoc.directionTo(averageAttackingEnemyLocation));
                            if (canMoveRubble(moveDir)) {
                                Direction[] dirs = Util.getInOrderDirections(moveDir);
                                tryMoveDest(dirs);
                            }
                            tryAttack();
                        }
                    } else {
                        if (closestSoldierDist > RobotType.SOLDIER.visionRadiusSquared
                            && closestSageDist > RobotType.SAGE.visionRadiusSquared
                            && closestWatchtowerDist > RobotType.WATCHTOWER.visionRadiusSquared) {
                            return;
                        }
                        Debug.printString("Running!");
                        Direction dir = averageAttackingEnemyLocation.directionTo(currLoc);
                        if (closestSageDist <= RobotType.SAGE.visionRadiusSquared) {
                            dir = closestSage.directionTo(currLoc);
                        } else if (closestSoldierDist <= RobotType.SOLDIER.visionRadiusSquared) {
                            dir = closestSoldier.directionTo(currLoc);
                        } else if (closestWatchtowerDist <= RobotType.WATCHTOWER.actionRadiusSquared) {
                            dir = closestWatchtower.directionTo(currLoc);
                        }
                        Direction newDir = chooseBackupDirection(dir);
                        if (canMoveRubble(newDir)) {
                            Direction[] dirs = Util.getInOrderDirections(newDir);
                            tryMoveDest(dirs);
                        }
                        runSemaphore = 5;
                        isRunning = true;
                        runDirection = dir;
                    }
                } else {
                    if (!isRunning || runSemaphore <= 0) {
                        tryAttack();
                        checkAvoidCharge();
                        isRunning = false;
                        moveTowardsCluster();
                    } else {
                        tryAttack();
                        Debug.printString("Running!");
                        Direction newDir = chooseBackupDirection(runDirection);
                        if (canMoveRubble(newDir)) {
                            Direction[] dirs = Util.getInOrderDirections(newDir);
                            tryMoveDest(dirs);
                        }
                    }
                }
                break;
            case GOING_TO_HEAL:
                Comms.incrementNumTroopsHealingAt(healTargetIdx);
                Debug.setIndicatorDot(Debug.INDICATORS, healTarget, 0, 255, 0);
                Debug.printString("Going to heal");
                tryAttack();
                if(currLoc.isWithinDistanceSquared(healTarget, 4 * Util.HEAL_DIST_TO_HOME)) {
                    moveMoreSafely(healTarget, Util.HEAL_DIST_TO_HOME);
                } else {
                    Nav.move(healTarget);
                }
                break;
            case HEALING:
                Comms.incrementNumTroopsHealingAt(healTargetIdx);
                Debug.setIndicatorDot(Debug.INDICATORS, healTarget, 0, 255, 0);
                Debug.printString("Healing");
                tryAttack();
                moveMoreSafely(healTarget, Util.HEAL_DIST_TO_HOME);
                break;

        }
    }

    public int estimateWaitTimeAt(int targetIdx, int prioritizedArchon) throws GameActionException {
        MapLocation archonLoc = archonLocations[targetIdx];
        int numHealing = Comms.getNumTroopsHealingAt(targetIdx);
        int archonHealRate = -RobotType.ARCHON.damage;
        int waitTime = numHealing * Util.AVERAGE_HEALTH_TO_HEAL / archonHealRate / 2 + (int)Math.sqrt(currLoc.distanceSquaredTo(archonLoc));
        if(prioritizedArchon == targetIdx) waitTime += numHealing;
        return waitTime;
    }

    // Weight the prioritized archon less
    // Returns whether we found a target
    public boolean loadHealTarget() throws GameActionException {
        loadArchonLocations();
        int prioritizedArchon = Comms.getPrioritizedArchon() - 1;
        healTarget = null;
        int bestWait = Integer.MAX_VALUE;
        int bestWaitIdx = -1;
        for(int i = 0; i < Comms.friendlyArchonCount(); i++) {
            MapLocation archonLoc = archonLocations[i];
            if(archonLoc == null || Comms.isAtHealingCap(i)) continue;
            int waitTime = estimateWaitTimeAt(i, prioritizedArchon);
            if(waitTime < bestWait) {
                bestWait = waitTime;
                bestWaitIdx = i;
            }
        }

        if(bestWaitIdx != -1) {
            healTarget = archonLocations[bestWaitIdx];
            healTargetIdx = bestWaitIdx;
        }

        return healTarget != null;
    }

    public boolean needToReloadTarget() throws GameActionException {
        loadArchonLocations();
        for(MapLocation archonLoc : archonLocations) {
            if(archonLoc == null) continue;
            if(archonLoc.equals(healTarget)) return false;
        }
        return true;
    }

    public boolean reloadTarget() throws GameActionException {
        Comms.markArchonDead(healTarget);
        currState = SageState.GOING_TO_HEAL;
        Debug.printString("Reloading");
        return loadHealTarget();
    }

    public void scanEnemies() throws GameActionException {
        averageAttackingEnemyLocation = null;
        sensedArchon = null;
        inRangeArchon = null;
        isTurret = false;
        attackTarget = null;
        int totalHealth = 0;
        totalBuildingHealth = 0;
        MapLocation bestSoldier = null;
        int bestSoldierDamageScore = 0;
        MapLocation bestSage = null;
        int bestSageDamageScore = 0;
        MapLocation bestMiner = null;
        int bestMinerDamageScore = 0;
        MapLocation bestLab = null;
        int bestLabDamageScore = 0;
        MapLocation bestBuilder = null;
        int bestBuilderDamageScore = 0;
        overallAttackingEnemyDx = 0;
        overallAttackingEnemyDy = 0;
        numAttackingEnemies = 0;
        closestSoldier = null;
        closestSoldierDist = Util.MAP_MAX_DIST_SQUARED;
        closestSage = null;
        closestSageDist = Util.MAP_MAX_DIST_SQUARED;
        closestWatchtower = null;
        closestWatchtowerDist = Util.MAP_MAX_DIST_SQUARED;
        bestOverallSoldierHealth = 0;
        predictedDamage = 0;
        RobotInfo robot;
        MapLocation loc;
        int dist;
        numVictims = 0;

        for (int i = EnemySensable.length; --i >= 0;) {
            robot = EnemySensable[i];
            dist = robot.location.distanceSquaredTo(currLoc);
            switch(robot.type) {
                case SOLDIER:
                    if (dist <= RobotType.SAGE.actionRadiusSquared) {
                        int damageScore = Math.min(45, robot.health);
                        if (robot.health <= 45) {
                            damageScore += 20;
                        }
                        if (damageScore > bestSoldierDamageScore) {
                            bestSoldierDamageScore = damageScore;
                            bestSoldier = robot.location;
                        }
                        totalHealth += Math.min(11, robot.health);
                        if (robot.health <= 11) {
                            totalHealth += 20;
                        }
                        numVictims++;
                    }
                    if (robot.health > bestOverallSoldierHealth) {
                        bestOverallSoldierHealth = robot.health;
                    }
                    if (dist < closestSoldierDist) {
                        closestSoldier = robot.location;
                        closestSoldierDist = dist;
                    }
                    loc = robot.location;
                    overallAttackingEnemyDx += loc.x;
                    overallAttackingEnemyDy += loc.y;
                    numAttackingEnemies++;
                    break;
                case SAGE:
                    if (dist <= RobotType.SAGE.actionRadiusSquared) {
                        int damageScore = Math.min(45, robot.health);
                        if (robot.health <= 45) {
                            damageScore += 30;
                        }
                        if (damageScore > bestSageDamageScore) {
                            bestSageDamageScore = damageScore;
                            bestSage = robot.location;
                        }
                        totalHealth += Math.min(22, robot.health);
                        if (robot.health <= 22) {
                            totalHealth += 30;
                        }
                        numVictims++;
                    }
                    if (dist < closestSageDist) {
                        closestSage = robot.location;
                        closestSageDist = dist;
                    }
                    loc = robot.location;
                    overallAttackingEnemyDx += loc.x;
                    overallAttackingEnemyDy += loc.y;
                    numAttackingEnemies++;
                    break;
                case WATCHTOWER:
                    if (robot.mode != RobotMode.TURRET) {
                        break;
                    }
                    if (dist <= RobotType.SAGE.actionRadiusSquared) {
                        if (robot.level == 1) {
                            totalBuildingHealth += Math.min(15, robot.health);
                        } else if (robot.level == 2) {
                            totalBuildingHealth += Math.min(27, robot.health);
                        } else {
                            totalBuildingHealth += Math.min(48, robot.health);
                        }
                        
                    }
                    if (dist < closestWatchtowerDist) {
                        closestWatchtower = robot.location;
                        closestWatchtowerDist = dist;
                    }
                    loc = robot.location;
                    overallAttackingEnemyDx += loc.x;
                    overallAttackingEnemyDy += loc.y;
                    numAttackingEnemies++;
                    break;
                case ARCHON:
                    if (dist <= RobotType.SAGE.actionRadiusSquared) {
                        inRangeArchon = robot;
                    }
                    isTurret = robot.mode == RobotMode.TURRET;
                    sensedArchon = robot;
                    break;
                case MINER:
                    if (dist <= RobotType.SAGE.actionRadiusSquared) {
                        int damageScore = Math.min(45, robot.health);
                        if (robot.health <= 45) {
                            damageScore += 5;
                        }
                        if (damageScore > bestMinerDamageScore) {
                            bestMinerDamageScore = damageScore;
                            bestMiner = robot.location;
                        }
                        totalHealth += Math.min(8, robot.health);
                        if (robot.health <= 8) {
                            totalHealth += 5;
                        }
                        numVictims++;
                    }
                    break;
                case BUILDER:
                    if (dist <= RobotType.SAGE.actionRadiusSquared) {
                        int damageScore = Math.min(45, robot.health);
                        if (robot.health <= 45) {
                            damageScore += 5;
                        }
                        if (damageScore > bestBuilderDamageScore) {
                            bestBuilderDamageScore = damageScore;
                            bestBuilder = robot.location;
                        }
                        totalHealth += Math.min(6, robot.health);
                        if (robot.health <= 6) {
                            totalHealth += 5;
                        }
                        numVictims++;
                    }
                    break;
                case LABORATORY:
                    if (dist <= RobotType.SAGE.actionRadiusSquared) {
                        int damageScore = Math.min(45, robot.health);
                        if (robot.health <= 45) {
                            damageScore += 50;
                        }
                        if (damageScore > bestLabDamageScore) {
                            bestLabDamageScore = damageScore;
                            bestLab = robot.location;
                        }
                        if (robot.level == 1) {
                            totalBuildingHealth += Math.min(10, robot.health);
                        } else if (robot.level == 2) {
                            totalBuildingHealth += Math.min(18, robot.health);
                        } else {
                            totalBuildingHealth += Math.min(32, robot.health);
                        }
                        numVictims++;
                    }
                default:
                    break;
            }
        }
        if (numAttackingEnemies != 0) {
            predictedDamage = totalHealth;
            averageAttackingEnemyLocation = new MapLocation(overallAttackingEnemyDx / numAttackingEnemies, overallAttackingEnemyDy / numAttackingEnemies);
        }
        if (bestLabDamageScore > totalHealth) {
            predictedDamage = bestLabDamageScore;
            attackTarget = bestLab;
        } else if (bestSageDamageScore > totalHealth) {
            predictedDamage = bestSageDamageScore;
            attackTarget = bestSage;
        } else if (bestSoldierDamageScore > totalHealth) {
            predictedDamage = bestSoldierDamageScore;
            attackTarget = bestSoldier;
        } else if (bestMinerDamageScore > totalHealth) {
            predictedDamage = bestMinerDamageScore;
            attackTarget = bestMiner;
        } else if (bestBuilderDamageScore > totalHealth) {
            predictedDamage = bestBuilderDamageScore;
            attackTarget = bestBuilder;
        }
    }    

    public boolean tryAttack() throws GameActionException {
        if (totalBuildingHealth > predictedDamage
            && rc.canEnvision(AnomalyType.FURY)) {
            Debug.printString("Envisioning Fury");
            rc.envision(AnomalyType.FURY);
            return true;
        }
        if (numVictims == 0 || predictedDamage < bestOverallSoldierHealth * 2 / 3) {
            return false;
        }
        if (attackTarget == null && rc.canEnvision(AnomalyType.CHARGE)) {
            Debug.printString("Envisioning Charge");
            rc.envision(AnomalyType.CHARGE);
            return true;
        }
        if (attackTarget != null && rc.canAttack(attackTarget)) {
            Debug.printString("Attacking " + attackTarget);
            rc.attack(attackTarget);
            return true;
        }
        return false;
    }

    public void moveTowardsCluster() throws GameActionException {
        Debug.printString("Following clusters");
        MapLocation clusterLoc = Comms.getClosestCluster(currLoc);
        if (currLoc.distanceSquaredTo(clusterLoc) <= visionRadiusSquared) {
            boolean seeArchonInSensable = false;
            for (RobotInfo bot : EnemySensable) {
                if (bot.getType() == RobotType.ARCHON) {
                    seeArchonInSensable = true;
                }
            }
            if (!seeArchonInSensable) {
                Comms.broadcastSoldierNearClusterButNothingFound();
            }
        }
        Nav.move(clusterLoc);
    }

    // Copied from Soldier code
    public Direction chooseBackupDirection(Direction Dir) throws GameActionException {
        Direction[] dirsToConsider = Util.getInOrderDirections(Dir);
        Direction bestDirSoFar = null;
        int bestRubble = 101;
        int bestEnemiesStillSeen = Integer.MAX_VALUE;
        RobotInfo[] enemyAttackable = getEnemyAttackable();
        RobotInfo enemy;
        for(Direction newDir: dirsToConsider) {
            if (rc.canMove(newDir)) {
                MapLocation targetLoc = currLoc.add(newDir);
                int locRubble = rc.senseRubble(currLoc);
                int newDirRubble = rc.senseRubble(targetLoc);
                boolean notTooMuchRubble = newDirRubble < (10 + locRubble);
                int currEnemiesStillSeen = 0;
                for(int i = enemyAttackable.length; --i >= 0;) {
                    enemy = enemyAttackable[i];
                    MapLocation enemyLoc = enemy.getLocation();
                    int enemyActionRadius = enemy.getType().actionRadiusSquared;
                    if(enemyLoc.distanceSquaredTo(targetLoc) <= enemyActionRadius) {
                        currEnemiesStillSeen++;
                    }
                }
                if(notTooMuchRubble && currEnemiesStillSeen <= bestEnemiesStillSeen) {
                    if (currEnemiesStillSeen < bestEnemiesStillSeen) {
                        bestDirSoFar = newDir;
                        bestRubble = newDirRubble;
                        bestEnemiesStillSeen = currEnemiesStillSeen;
                    }
                    else if (currEnemiesStillSeen == bestEnemiesStillSeen && newDirRubble < bestRubble) {
                        bestDirSoFar = newDir;
                        bestRubble = newDirRubble;
                        bestEnemiesStillSeen = currEnemiesStillSeen;                    
                    }
                }                
            }
        }
        if (bestDirSoFar == null) {
            return Dir;
        } else {
            return bestDirSoFar;
        }
    }

    public void checkAvoidCharge() throws GameActionException {
        int round = rc.getRoundNum();
        for (int i = nextCharge; i < chargeRounds.length; i++) {
            if (round > chargeRounds[i]) {
                nextCharge++;
                continue;
            } else if (chargeRounds[i] - round <= 50) {
                Debug.printString("Preparing for Charge");
                RobotInfo[] friendlies = rc.senseNearbyRobots(-1, rc.getTeam());
                int totalFriendlyDx = 0;
                int totalFriendlyDy = 0;
                int numFriendlies = 0;
                for (RobotInfo robot : friendlies) {
                    MapLocation loc = robot.location;
                    totalFriendlyDx += loc.x;
                    totalFriendlyDy += loc.y;
                    numFriendlies++;
                }
                if (numFriendlies == 0) {
                    return;
                }
                MapLocation averageLoc = new MapLocation(totalFriendlyDx / numFriendlies, totalFriendlyDy / numFriendlies);
                Direction dir = averageLoc.directionTo(currLoc);
                Direction newDir = makeDirBetter(dir);
                if (canMoveRubble(newDir)) {
                    Direction[] dirs = Util.getInOrderDirections(newDir);
                    tryMoveDest(dirs);
                }
                return;
            } else {
                break;
            }
        }
    }

    public Direction makeDirBetter(Direction dir) throws GameActionException {
        Direction[] possibleDirs = new Direction[]{dir, dir.rotateRight(), dir.rotateLeft(), 
            dir.rotateRight().rotateRight(), dir.rotateLeft().rotateLeft()};
        Direction bestDirSoFar = null;
        int bestRubble = 101;
        for(Direction newDir: possibleDirs) {
            if (rc.canMove(newDir)) {
                MapLocation targetLoc = currLoc.add(newDir);
                int newDirRubble = rc.senseRubble(targetLoc);
                if (newDirRubble < bestRubble) {
                    bestDirSoFar = newDir;
                    bestRubble = newDirRubble;
                }  
            }
        }
        if (bestDirSoFar == null) {
            return dir;
        }
        return bestDirSoFar;
    }

    public boolean canMoveRubble(Direction dir) throws GameActionException {
        return Util.getRubble(currLoc.add(dir)) < Util.getRubble(currLoc) + 10;
    }
}