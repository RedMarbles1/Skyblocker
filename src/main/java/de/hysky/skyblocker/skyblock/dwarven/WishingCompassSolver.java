package de.hysky.skyblocker.skyblock.dwarven;

import de.hysky.skyblocker.config.SkyblockerConfigManager;
import de.hysky.skyblocker.skyblock.tabhud.util.PlayerListMgr;
import de.hysky.skyblocker.utils.Constants;
import de.hysky.skyblocker.utils.Utils;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

public class WishingCompassSolver {
    private static final MinecraftClient CLIENT = MinecraftClient.getInstance();

    enum SolverStates {
        NOT_STARTED,
        PROCESSING_FIRST_USE,
        WAITING_FOR_SECOND,
        PROCESSING_SECOND_USE,
    }

    enum ZONE {
        CRYSTAL_NUCLEUS,
        JUNGLE,
        MITHRIL_DEPOSITS,
        GOBLIN_HOLDOUT,
        PRECURSOR_REMNANTS,
        MAGMA_FIELDS,
    }

    private static final HashMap<ZONE, Box> ZONE_BOUNDING_BOXES = Util.make(new HashMap<>(), map -> {
        map.put(ZONE.CRYSTAL_NUCLEUS, new Box(462, 63, 461, 564, 181, 565));
        map.put(ZONE.JUNGLE, new Box(201, 63, 201, 513, 189, 513));
        map.put(ZONE.MITHRIL_DEPOSITS, new Box(512, 63, 201, 824, 189, 513));
        map.put(ZONE.GOBLIN_HOLDOUT, new Box(201, 63, 512, 513, 189, 824));
        map.put(ZONE.PRECURSOR_REMNANTS, new Box(512, 63, 512, 824, 189, 824));
        map.put(ZONE.MAGMA_FIELDS, new Box(201, 30, 201, 824, 64, 824));
    });
    private static final Vec3d JUNGLE_TEMPLE_DOOR_OFFSET = new Vec3d(-57, 36, -21);
    /**
     * how many particles to use to get direction of a line
     */
    private static final long PARTICLES_PER_LINE = 25;
    /**
     * the amount of milliseconds to wait for the next particle until assumed failed
     */
    private static final long PARTICLES_MAX_DELAY = 500;
    /**
     * The maximum distance a particle can be from the last to be considered part of the line
     */
    private static final double PARTICLES_MAX_DISTANCE = 0.9;
    /**
     * the distance the player has to be from where they used the first compass to where they use the second
     */
    private static final long DISTANCE_BETWEEN_USES = 8;

    private static SolverStates currentState = SolverStates.NOT_STARTED;
    private static Vec3d startPosOne = Vec3d.ZERO;
    private static Vec3d startPosTwo = Vec3d.ZERO;
    private static Vec3d directionOne = Vec3d.ZERO;
    private static Vec3d directionTwo = Vec3d.ZERO;
    private static long particleUsedCountOne = 0;
    private static long particleUsedCountTwo = 0;
    private static long particleLastUpdate = System.currentTimeMillis();
    private static Vec3d particleLastPos = Vec3d.ZERO;


    public static void init() {
        UseItemCallback.EVENT.register(WishingCompassSolver::onItemInteract);
        UseBlockCallback.EVENT.register(WishingCompassSolver::onBlockInteract);
        ClientPlayConnectionEvents.JOIN.register((_handler, _sender, _client) -> reset());
    }

    private static void reset() {
        currentState = SolverStates.NOT_STARTED;
        startPosOne = Vec3d.ZERO;
        startPosTwo = Vec3d.ZERO;
        directionOne = Vec3d.ZERO;
        directionTwo = Vec3d.ZERO;
        particleUsedCountOne = 0;
        particleUsedCountTwo = 0;
        particleLastUpdate = System.currentTimeMillis();
        Vec3d particleLastPos = Vec3d.ZERO;
    }

    private static boolean isKingsScentPresent() {
        String footer = PlayerListMgr.getFooter();
        if (footer == null) {
            return false;
        }
        return footer.contains("King's Scent I");
    }

    private static boolean isKeyInInventory() {
        if (CLIENT.player == null) {
            return false;
        }
        for (ItemStack item : CLIENT.player.getInventory().main) {
            if (item != null && Objects.equals(item.getSkyblockId(), "JUNGLE_KEY")) {
                return true;
            }
        }
        return false;
    }

    private static ZONE getZoneOfLocation(Vec3d location) {
        for (Map.Entry<ZONE, Box> zone : ZONE_BOUNDING_BOXES.entrySet()) {
            if (zone.getValue().contains(location)) {
                return zone.getKey();
            }
        }

        //default to nucleus if somehow not in another zone
        return ZONE.CRYSTAL_NUCLEUS;
    }

    private static Boolean isZoneComplete(ZONE zone) {
        if (CLIENT.getNetworkHandler() == null || CLIENT.player == null) {
            return false;
        }
        //creates cleaned stream of all the entry's in tab list
        Stream<PlayerListEntry> playerListStream = CLIENT.getNetworkHandler().getPlayerList().stream();
        Stream<String> displayNameStream = playerListStream.map(PlayerListEntry::getDisplayName).filter(Objects::nonNull).map(Text::getString).map(String::strip);

        //make sure the data is in tab and if not tell the user
        if (displayNameStream.noneMatch(entry -> entry.equals("Crystals:"))) {
            CLIENT.player.sendMessage(Constants.PREFIX.get().append(Text.translatable("skyblocker.config.mining.crystalsWaypoints.wishingCompassSolver.enableTabMessage")), false);
            return false;
        }

        //return if the crystal for a zone is found
        playerListStream = CLIENT.getNetworkHandler().getPlayerList().stream();
        displayNameStream = playerListStream.map(PlayerListEntry::getDisplayName).filter(Objects::nonNull).map(Text::getString).map(String::strip);
        return switch (zone) {
            case JUNGLE -> displayNameStream.noneMatch(entry -> entry.equals("Amethyst: ✖ Not Found"));
            case MITHRIL_DEPOSITS -> displayNameStream.noneMatch(entry -> entry.equals("Jade: ✖ Not Found"));
            case GOBLIN_HOLDOUT -> displayNameStream.noneMatch(entry -> entry.equals("Amber: ✖ Not Found"));
            case PRECURSOR_REMNANTS -> displayNameStream.noneMatch(entry -> entry.equals("Sapphire: ✖ Not Found"));
            case MAGMA_FIELDS -> displayNameStream.noneMatch(entry -> entry.equals("Topaz: ✖ Not Found"));
            default -> false;
        };
    }

    private static MiningLocationLabel.CrystalHollowsLocationsCategory getTargetLocation(ZONE startingZone) {
        //if the zone is complete return null
        if (isZoneComplete(startingZone)) {
            return MiningLocationLabel.CrystalHollowsLocationsCategory.UNKNOWN;
        }
        return switch (startingZone) {
            case JUNGLE ->
                    isKeyInInventory() ? MiningLocationLabel.CrystalHollowsLocationsCategory.JUNGLE_TEMPLE : MiningLocationLabel.CrystalHollowsLocationsCategory.ODAWA;
            case MITHRIL_DEPOSITS -> MiningLocationLabel.CrystalHollowsLocationsCategory.MINES_OF_DIVAN;
            case GOBLIN_HOLDOUT ->
                    isKingsScentPresent() ? MiningLocationLabel.CrystalHollowsLocationsCategory.GOBLIN_QUEENS_DEN : MiningLocationLabel.CrystalHollowsLocationsCategory.KING_YOLKAR;
            case PRECURSOR_REMNANTS -> MiningLocationLabel.CrystalHollowsLocationsCategory.LOST_PRECURSOR_CITY;
            case MAGMA_FIELDS -> MiningLocationLabel.CrystalHollowsLocationsCategory.KHAZAD_DUM;
            default -> MiningLocationLabel.CrystalHollowsLocationsCategory.UNKNOWN;
        };
    }

    /**
     * Verify that a location could be correct and not to far out of zone. This is a problem when areas sometimes do not exist and is not a perfect solution
     * @param startingZone zone player is searching in
     * @param pos location where the area should be
     * @return corrected location
     */
    private static Boolean verifyLocation(ZONE startingZone, Vec3d pos) {
        return ZONE_BOUNDING_BOXES.get(startingZone).expand(100, 0, 100).contains(pos);
    }

    public static void onParticle(ParticleS2CPacket packet) {
        if (!Utils.isInCrystalHollows() || !ParticleTypes.HAPPY_VILLAGER.equals(packet.getParameters().getType())) {
            return;
        }
        //get location of particle
        Vec3d particlePos = new Vec3d(packet.getX(), packet.getY(), packet.getZ());
        //update particle used time
        particleLastUpdate = System.currentTimeMillis();
        //ignore particle not in the line
        if (particlePos.distanceTo(particleLastPos) > PARTICLES_MAX_DISTANCE) {
            return;
        }
        particleLastPos = particlePos;

        switch (currentState) {
            case PROCESSING_FIRST_USE -> {
                Vec3d particleDirection = particlePos.subtract(startPosOne).normalize();
                //move direction to fit with particle
                directionOne = directionOne.add(particleDirection.multiply((double) 1 / PARTICLES_PER_LINE));
                particleUsedCountOne += 1;
                //if used enough particle go to next state
                if (particleUsedCountOne >= PARTICLES_PER_LINE) {
                    currentState = SolverStates.WAITING_FOR_SECOND;
                    if (CLIENT.player != null) {
                        CLIENT.player.sendMessage(Constants.PREFIX.get().append(Text.translatable("skyblocker.config.mining.crystalsWaypoints.wishingCompassSolver.wishingCompassUsedMessage").formatted(Formatting.GREEN)), false);
                    }
                }
            }
            case PROCESSING_SECOND_USE -> {
                Vec3d particleDirection = particlePos.subtract(startPosTwo).normalize();
                //move direction to fit with particle
                directionTwo = directionTwo.add(particleDirection.multiply((double) 1 / PARTICLES_PER_LINE));
                particleUsedCountTwo += 1;
                //if used enough particle go to next state
                if (particleUsedCountTwo >= PARTICLES_PER_LINE) {
                    processSolution();
                }
            }
        }
    }

    private static void processSolution() {
        if (CLIENT.player == null) {
            reset();
            return;
        }
        Vec3d targetLocation = solve(startPosOne, startPosTwo, directionOne, directionTwo);
        if (targetLocation == null) {
            CLIENT.player.sendMessage(Constants.PREFIX.get().append(Text.translatable("skyblocker.config.mining.crystalsWaypoints.wishingCompassSolver.somethingWentWrongMessage").formatted(Formatting.RED)), false);
        } else {
            //send message to player with location and name
            ZONE playerZone = getZoneOfLocation(startPosOne);
            MiningLocationLabel.CrystalHollowsLocationsCategory location = getTargetLocation(playerZone);
            if (!verifyLocation(playerZone, targetLocation)) {
                location = MiningLocationLabel.CrystalHollowsLocationsCategory.UNKNOWN;
            }
            //offset the jungle location to its doors
            if (location == MiningLocationLabel.CrystalHollowsLocationsCategory.JUNGLE_TEMPLE) {
                targetLocation = targetLocation.add(JUNGLE_TEMPLE_DOOR_OFFSET);
            }

            CLIENT.player.sendMessage(Constants.PREFIX.get()
                            .append(Text.translatable("skyblocker.config.mining.crystalsWaypoints.wishingCompassSolver.foundMessage").formatted(Formatting.GREEN))
                            .append(Text.literal(location.getName()).withColor(location.getColor()))
                            .append(Text.literal(": " + (int) targetLocation.getX() + " " + (int) targetLocation.getY() + " " + (int) targetLocation.getZ())),
                    false);

            //add waypoint
            CrystalsLocationsManager.addCustomWaypoint(location.getName(), BlockPos.ofFloored(targetLocation));
        }

        //reset ready for another go
        reset();
    }

    /**
     * using the stating locations and line direction solve for where the location must be
     */
    protected static Vec3d solve(Vec3d startPosOne, Vec3d startPosTwo, Vec3d directionOne, Vec3d directionTwo) {
        Vec3d crossProduct = directionOne.crossProduct(directionTwo);
        if (crossProduct.equals(Vec3d.ZERO)) {
            //lines are parallel or coincident
            return null;
        }
        // Calculate the difference vector between startPosTwo and startPosOne
        Vec3d diff = startPosTwo.subtract(startPosOne);
        // projecting 'diff' onto the plane defined by 'directionTwo' and 'crossProduct'. then scaling by the inverse squared length of 'crossProduct'
        double intersectionScalar = diff.dotProduct(directionTwo.crossProduct(crossProduct)) / crossProduct.lengthSquared();
        // if intersectionScalar is a negative number the lines are meeting in the opposite direction and giving incorrect cords
        if (intersectionScalar < 0) {
            return null;
        }
        //get final target location
        return startPosOne.add(directionOne.multiply(intersectionScalar));
    }

    private static ActionResult onBlockInteract(PlayerEntity playerEntity, World world, Hand hand, BlockHitResult blockHitResult) {
        if (CLIENT.player == null) {
            return null;
        }
        ItemStack stack = CLIENT.player.getStackInHand(hand);
        //make sure the user is in the crystal hollows and holding the wishing compass
        if (!Utils.isInCrystalHollows() || !SkyblockerConfigManager.get().mining.crystalsWaypoints.WishingCompassSolver || !Objects.equals(stack.getSkyblockId(), "WISHING_COMPASS")) {
            return ActionResult.PASS;
        }
        if (useCompass()) {
            return ActionResult.FAIL;
        }

        return ActionResult.PASS;
    }

    private static TypedActionResult<ItemStack> onItemInteract(PlayerEntity playerEntity, World world, Hand hand) {
        if (CLIENT.player == null) {
            return null;
        }
        ItemStack stack = CLIENT.player.getStackInHand(hand);
        //make sure the user is in the crystal hollows and holding the wishing compass
        if (!Utils.isInCrystalHollows() || !SkyblockerConfigManager.get().mining.crystalsWaypoints.WishingCompassSolver || !Objects.equals(stack.getSkyblockId(), "WISHING_COMPASS")) {
            return TypedActionResult.pass(stack);
        }
        if (useCompass()) {
            return TypedActionResult.fail(stack);
        }

        return TypedActionResult.pass(stack);
    }

    /**
     * Computes what to do next when a compass is used.
     *
     * @return if the use event should be canceled
     */
    private static boolean useCompass() {
        if (CLIENT.player == null) {
            return true;
        }
        Vec3d playerPos = CLIENT.player.getEyePos();
        ZONE currentZone = getZoneOfLocation(playerPos);

        switch (currentState) {
            case NOT_STARTED -> {
                //do not start if the player is in nucleus as this does not work well
                if (currentZone == ZONE.CRYSTAL_NUCLEUS) {
                    CLIENT.player.sendMessage(Constants.PREFIX.get().append(Text.translatable("skyblocker.config.mining.crystalsWaypoints.wishingCompassSolver.useOutsideNucleusMessage")), false);
                    return true;
                }
                startNewState(SolverStates.PROCESSING_FIRST_USE);
            }

            case WAITING_FOR_SECOND -> {
                //only continue if the player is far enough away from the first position to get a better reading
                if (startPosOne.distanceTo(playerPos) < DISTANCE_BETWEEN_USES) {
                    CLIENT.player.sendMessage(Constants.PREFIX.get().append(Text.translatable("skyblocker.config.mining.crystalsWaypoints.wishingCompassSolver.moveFurtherMessage")), false);
                    return true;
                } else {
                    //make sure the player is in the same zone as they used to first or restart
                    if (currentZone != getZoneOfLocation(startPosOne)) {
                        CLIENT.player.sendMessage(Constants.PREFIX.get().append(Text.translatable("skyblocker.config.mining.crystalsWaypoints.wishingCompassSolver.changingZoneMessage")), false);
                        reset();
                        startNewState(SolverStates.PROCESSING_FIRST_USE);
                    } else {
                        startNewState(SolverStates.PROCESSING_SECOND_USE);
                    }
                }
            }

            case PROCESSING_FIRST_USE, PROCESSING_SECOND_USE -> {
                //if still looking for particles for line tell the user to wait
                //else tell the use something went wrong and its starting again
                if (System.currentTimeMillis() - particleLastUpdate < PARTICLES_MAX_DELAY) {
                    CLIENT.player.sendMessage(Constants.PREFIX.get().append(Text.translatable("skyblocker.config.mining.crystalsWaypoints.wishingCompassSolver.waitLongerMessage").formatted(Formatting.RED)), false);
                    return true;
                } else {
                    CLIENT.player.sendMessage(Constants.PREFIX.get().append(Text.translatable("skyblocker.config.mining.crystalsWaypoints.wishingCompassSolver.couldNotDetectLastUseMessage").formatted(Formatting.RED)), false);
                    reset();
                    startNewState(SolverStates.PROCESSING_FIRST_USE);
                }
            }
        }

        return false;
    }

    private static void startNewState(SolverStates newState) {
        if (CLIENT.player == null) {
            return;
        }
        //get where eye pos independent of if player is crouching
        Vec3d playerPos = CLIENT.player.getPos().add(0, 1.62, 0);

        if (newState == SolverStates.PROCESSING_FIRST_USE) {
            currentState = SolverStates.PROCESSING_FIRST_USE;
            startPosOne = playerPos;
            particleLastUpdate = System.currentTimeMillis();
            particleLastPos = playerPos;
        } else if (newState == SolverStates.PROCESSING_SECOND_USE) {
            currentState = SolverStates.PROCESSING_SECOND_USE;
            startPosTwo = playerPos;
            particleLastUpdate = System.currentTimeMillis();
            particleLastPos = playerPos;
        }
    }
}
