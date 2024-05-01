package cf.vaccat.catclient.module.misc;

import cf.vaccat.catclient.module.Category;
import cf.vaccat.catclient.module.Module;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.biome.BiomeEnd;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ZealotSniper extends Module {
    public ZealotSniper() {
        super("Zealot Sniper", "snipes zealots", Category.MISC);
        this.setKey(Keyboard.KEY_R);
    }

    // Targeting
    Entity targetZealot;
    Vec3d aimOffset;
    float yaw;
    float pitch;
    TargetingMode targetingMode;
    List<Entity> loadedEntityList;
    // Movement
    int keybindSneak, keybindJump, keybindForward, keybindUseItem;
    MovementMode movementMode;
    Timer currentMoveModeTimer;
    long moveModeSwitchTime;
    int minModeTime, maxModeTime;
    // Safety
    String[] rewarpMessages;
    List<EntityPlayer> loadedPlayerList;
    float maxPlayerLookTime;
    List<Player> playerTracker;
    String[] NPCList;
    String[] AdminList;
    // Misc
    Random random;
    int step;

    @Override
    public void onEnable() {
        super.onEnable();
        random = new Random();
        targetZealot = null;
        aimOffset = null;
        yaw = mc.player.rotationYaw;
        pitch = mc.player.rotationPitch;
        targetingMode = TargetingMode.ROTATION;
        loadedEntityList = null;
        loadedPlayerList = null;
        NPCList = new String[]{
                "Lone Adventurer"
        };
        AdminList = new String[]{
                "Minikloon"
        };
        step = 2;
        keybindSneak = mc.gameSettings.keyBindSneak.getKeyCode();
        keybindJump = mc.gameSettings.keyBindJump.getKeyCode();
        keybindForward = mc.gameSettings.keyBindForward.getKeyCode();
        keybindUseItem = mc.gameSettings.keyBindUseItem.getKeyCode();
        movementMode = MovementMode.JUMP_SNEAK;
        currentMoveModeTimer = new Timer();
        minModeTime = 30;
        maxModeTime = 60;
        maxPlayerLookTime = 10*1000;
        playerTracker = new ArrayList<>();
        moveModeSwitchTime = (long) (generateRandom(minModeTime, maxModeTime) * 1000L);
        rewarpMessages = new String[]{
                mc.player.getName(), // In case you are mentioned in chat
                "This server will restart soon",
                "has sent you a trade request",
                "you have sent a trade request",
                "fell into the void",
                "was knocked into the void by",
                "was slain by", "burned to death",
                "fell to death",
                "fell to their death with help from",
                "suffocated",
                "drowned",
                "was pricked to death by a cactus",
                "died"
        };
    }

    @Override
    public void onDisable() {
        KeyBinding.unPressAllKeys();
        super.onDisable();
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (!basicChecks()) return;
        playerProximityCheck();
        doMovement();
        doTargeting();
    }

    private void playerProximityCheck() {
        loadedPlayerList = mc.world.playerEntities;
        for (EntityPlayer player : loadedPlayerList) {
            // 1. Filter out NPCs and Admins
            String playerName = player.getName();
            if (stringInList(player.getName(), NPCList)) {
                continue;
            } else if (stringInList(player.getName(), AdminList)) {
                // You're fucked
                mc.player.sendChatMessage("/warp home");
                setToggled(false);
                return;
            }

            // 2. Update watch list for players looking at you
            Player registeredPlayer = findInWatchList(player);
            if (registeredPlayer != null) {
                if (!isPlayerLooking(player)) {
                    playerTracker.remove(registeredPlayer);
                } else if (registeredPlayer.playerLookTimer.getElapsedTime() >= maxPlayerLookTime) {
                    mc.player.sendChatMessage("/warp home");
                    setToggled(false);
                    return;
                }
            } else {
                // Add to watch list if looking at you.
                if (isPlayerLooking(player)) {
                    playerTracker.add(new Player(player));
                }
            }
        }
    }

    private Player findInWatchList(EntityPlayer player) {
        for (Player registeredPlayer : playerTracker) {
            if (registeredPlayer.player == player) {
                return registeredPlayer;
            }
        }
        return null;
    }

    // GPT generated, I have no idea if it works.
    private boolean isPlayerLooking(EntityPlayer player) {
        Vec3d playerEyePos = player.getPositionEyes(mc.getRenderPartialTicks());
        Vec3d playerLookVec = player.getLook(mc.getRenderPartialTicks());
        Vec3d myEyePos = player.getPositionEyes(mc.getRenderPartialTicks());
        Vec3d toEntity = new Vec3d(
                myEyePos.x - playerEyePos.x,
                myEyePos.y - playerEyePos.y,
                myEyePos.z - playerEyePos.z
        ); // Vector from player to the entity

        double dotProduct = toEntity.dotProduct(playerLookVec); // Dot product between the look vector and vector to the entity
        double lengthSquared = toEntity.lengthSquared(); // Squared length of the vector to the entity

        // Check if the dot product is positive and if the angle corresponds to the player looking at the entity
        if (dotProduct > 0) {
            double lookVecLengthSquared = playerLookVec.lengthSquared();
            double angleCos = dotProduct / (Math.sqrt(lookVecLengthSquared * lengthSquared));

            // Check if the angle is within a certain threshold (e.g., corresponding to about 30 degrees field of view)
            if (angleCos > Math.cos(Math.toRadians(30))) {
                return true; // Player is looking at the entity
            }
        }
        return false;
    }

    private boolean basicChecks() {
        // Most essential checks. I don't know if the last one works.
        return mc.player != null && mc.world != null && mc.world.getBiome(mc.player.getPosition()) instanceof BiomeEnd;
    }

    private void doTargeting() {
        if (verifyTargetZealot()) {
            generateYawPitch();
            setYawPitch(); // TODO: Silent aim, smooth aim
            keyDown(keybindUseItem);
        } else {
            keyDown(keybindUseItem);
            aimOffset = null;
            findZealot(TargetingMode.ROTATION);
        }
    }

    private void doMovement() {
        if (movementMode == MovementMode.JUMP_SNEAK) {
            keyUp(keybindSneak, keybindForward);
            keyDown(keybindJump, keybindSneak);
        } else if (movementMode == MovementMode.SNEAK_FORWARD) {
            keyUp(keybindJump, keybindSneak);
            keyDown(keybindSneak, keybindForward);
        }

        if (currentMoveModeTimer.getElapsedTime() >= moveModeSwitchTime) {
            movementMode = movementMode.nextMode();
            moveModeSwitchTime = (long) (generateRandom(minModeTime, maxModeTime) * 1000L);
            currentMoveModeTimer.reset();
        }
    }

    private boolean verifyTargetZealot() {
        return targetZealot != null && targetZealot.isEntityAlive() && mc.player.canEntityBeSeen(targetZealot);
    }

    @SubscribeEvent
    public void onMessage(ClientChatReceivedEvent event) {
        String message = event.getMessage().getUnformattedText();
        if (containsIgnoreCase(message, "A special Zealot has spawned nearby")) {
            findZealot(TargetingMode.DISTANCE);
        } else if (checkMessageCases(message)) {
            mc.player.sendChatMessage("/warp home");
            setToggled(false);
        }
    }

    private void findZealot(TargetingMode targetingMode) {
        switch (targetingMode) {
            case ROTATION:
                findLeastMouseMovementZealot();
            case DISTANCE:
                findClosestDistanceZealot();
        }
    }

    private void findLeastMouseMovementZealot() {
        // Find Zealot which requires the least mouse movement
        loadedEntityList = mc.world.getLoadedEntityList();
        double lowestRotDegree = 10000d;
        for (Entity entity : loadedEntityList) {
            if (entity.getName().contains("Enderman") && mc.player.canEntityBeSeen(entity)) {
                Vec3d playerEyePos = mc.player.getPositionEyes(mc.getRenderPartialTicks());
                Vec3d targetEyePos = entity.getPositionEyes(mc.getRenderPartialTicks());
                double currentRotDegree = calcRotationCost(calcAngle(playerEyePos, targetEyePos));
                if (currentRotDegree < lowestRotDegree) {
                    lowestRotDegree = currentRotDegree;
                    targetZealot = entity;
                }
            }
        }
    }

    private void findClosestDistanceZealot() {
        loadedEntityList = mc.world.getLoadedEntityList();
        double closestDistance = 10000d;
        for (Entity entity : loadedEntityList) {
            if (entity.getName().contains("Enderman") && mc.player.canEntityBeSeen(entity)) {
                double distance = entity.getDistance(mc.player.posX, mc.player.posY, mc.player.posZ);
                if (distance < closestDistance) {
                    closestDistance = distance;
                    targetZealot = entity;
                }
            }
        }
    }

    private boolean checkMessageCases(String message) {
        for (String rewarpMessage : rewarpMessages) {
            if (containsIgnoreCase(message, rewarpMessage)) {
                return true;
            }
        }
        return false;
    }

    private static float[] calcAngle(Vec3d from, Vec3d to) {
        final double difX = to.x - from.x;
        final double difY = (to.y - from.y) * -1.0F;
        final double difZ = to.z - from.z;
        final double dist = MathHelper.sqrt(difX * difX + difZ * difZ);
        return new float[] {
                ( float ) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(difZ, difX)) - 90.0f),
                ( float ) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(difY, dist)))
        };
    }

    private double generateRandom(double min, double max) {
        return min + random.nextDouble() * (max - min);
    }

    private void setYawPitch() {
        mc.player.rotationYaw = yaw;
        mc.player.rotationYawHead = yaw;
        mc.player.rotationPitch = pitch;
    }

    private void generateYawPitch() {
        Vec3d playerEyePos = mc.player.getPositionEyes(mc.getRenderPartialTicks());
        Vec3d targetEyePos = targetZealot.getPositionEyes(mc.getRenderPartialTicks());

        if (aimOffset == null) generateAimOffset();

        Vec3d targetPos = targetEyePos.add(aimOffset);
        float[] targetYawPitch = calcAngle(playerEyePos, targetPos);

        yaw = targetYawPitch[0];
        pitch = targetYawPitch[1];
    }

    private void generateAimOffset() {
        aimOffset = new Vec3d(
                generateRandom(-targetZealot.width/2, targetZealot.width/2),
                -generateRandom(0.1, targetZealot.height),
                generateRandom(-targetZealot.width/2, targetZealot.width/2)
        );
    }

    private double calcRotationCost(float[] targetYawPitch) {
        // Yaw:Pitch weighting 1:2
        return Math.abs(targetYawPitch[0] - mc.player.rotationYaw) + Math.abs(targetYawPitch[1] - mc.player.rotationPitch)*2;
    }

    public void keyDown(int keybind) {
        KeyBinding.setKeyBindState(keybind, true);
    }

    public void keyUp(int keybind) {
        KeyBinding.setKeyBindState(keybind, false);
    }

    public void keyDown(int... keybinds) {
        for (int keybind : keybinds) {
            keyDown(keybind);
        }
    }

    public void keyUp(int... keybinds) {
        for (int keybind : keybinds) {
            keyUp(keybind);
        }
    }

    private boolean stringInList(String target, String[] strings) {
        for (String string : strings) {
            if (string.equals(target)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsIgnoreCase(String string1, String string2) {
        return string1.toLowerCase().contains(string2.toLowerCase());
    }
}

enum MovementMode {
    JUMP_SNEAK,
    SNEAK_FORWARD;

    private static final MovementMode[] movementModes = values();

    public MovementMode nextMode() {
        return movementModes[(this.ordinal() + 1) % movementModes.length];
    }
}

enum TargetingMode {
    DISTANCE,
    ROTATION
}

class Timer {
    long startTime;

    Timer() {
        startTime = System.currentTimeMillis();
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public void reset() {
        setStartTime(System.currentTimeMillis());
    }

    public long getElapsedTime() { // In milliseconds. 1000 ms = 1 s
        return System.currentTimeMillis()-startTime;
    }
}

class Player {
    Timer playerLookTimer;
    EntityPlayer player;

    Player(EntityPlayer player) {
        this.player = player;
        playerLookTimer = new Timer();
    }

    public void resetTimer() {
        playerLookTimer.reset();
    }

    public Timer getTimer() {
        return playerLookTimer;
    }
}