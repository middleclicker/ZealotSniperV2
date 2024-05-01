package cf.vaccat.catclient.module.misc;

import cf.vaccat.catclient.module.Category;
import cf.vaccat.catclient.module.Module;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

import java.util.List;
import java.util.Random;

public class ZealotSniper extends Module {
    public ZealotSniper() {
        super("Zealot Sniper", "snipes zealots", Category.MISC);
        this.setKey(Keyboard.KEY_R);
    }

    // Math variables
    Random random;

    // Targeting variables
    Entity targetZealot;
    Vec3d aimOffset;
    float yaw;
    float pitch;
    List<Entity> loadedEntityList;

    // Smooth aim variables
    int step;

    // Control variables
    int keybindSneak;
    int keybindJump;
    int keybindForward;
    int keybindUseItem;

    // Misc variables
    String[] rewarpMessages;

    @Override
    public void onEnable() {
        super.onEnable();

        random = new Random();

        targetZealot = null;
        aimOffset = null;
        yaw = mc.player.rotationYaw;
        pitch = mc.player.rotationPitch;
        loadedEntityList = null;

        step = 2;

        keybindSneak = mc.gameSettings.keyBindSneak.getKeyCode();
        keybindJump = mc.gameSettings.keyBindJump.getKeyCode();
        keybindForward = mc.gameSettings.keyBindForward.getKeyCode();
        keybindUseItem = mc.gameSettings.keyBindUseItem.getKeyCode();

        rewarpMessages = new String[]{
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

        keyDown(keybindSneak);
        keyDown(keybindJump);
    }

    @Override
    public void onDisable() {
        keyUp(keybindSneak);
        keyUp(keybindJump);
        keyUp(keybindUseItem);
        super.onDisable();
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (targetZealot == null || !targetZealot.isEntityAlive() || !mc.player.canEntityBeSeen(targetZealot)) {
            // Locating part is kinda slow for some reason.
            keyDown(keybindUseItem);
            aimOffset = null;
            findZealot(0);
        } else {
            generateYawPitch();
            setYawPitch(); // TODO: silent aim
            keyDown(keybindUseItem);
        }
    }

    @SubscribeEvent
    public void onMessage(ClientChatReceivedEvent event) {
        String message = event.getMessage().getUnformattedText();
        if (containsIgnoreCase(message, "A special Zealot has spawned nearby")) {
            findZealot(1);
        } else if (checkMessageCases(message)) {
            mc.player.sendChatMessage("/warp home");
            this.setToggled(false);
        }
    }

    private void findZealot(int mode) {
        switch (mode) {
            case 0:
                findLeastMouseMovementZealot();
            case 1:
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

    private boolean containsIgnoreCase(String string1, String string2) {
        return string1.toLowerCase().contains(string2.toLowerCase());
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

    private void keyDown(int keybind) {
        KeyBinding.setKeyBindState(keybind, true);
    }

    private void keyUp(int keybind) {
        KeyBinding.setKeyBindState(keybind, false);
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
}

