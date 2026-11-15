package net.stackanvil.deep.module.impl;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.stackanvil.deep.module.Module;
import net.stackanvil.deep.module.ModuleCategory;
import net.stackanvil.deep.notebot.NotebotSong;
import net.stackanvil.deep.notebot.SongFiles;
import net.stackanvil.deep.notebot.SongNote;
import net.stackanvil.deep.rotation.Rotation;
import net.stackanvil.deep.rotation.RotationManager;
import net.stackanvil.deep.rotation.RotationRequest;
import net.stackanvil.deep.rotation.RotationTurnMode;
import net.stackanvil.deep.setting.BooleanSetting;
import net.stackanvil.deep.setting.NumberSetting;
import net.stackanvil.deep.setting.StringListSetting;
import net.stackanvil.deep.setting.StringSetting;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SequencedMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

public final class NotebotModule extends Module {

    private final StringSetting songFile = this.setting(StringSetting.from(StringSetting.builder()
            .id("song_file")
            .name("Song File")
            .defaultValue("")
            .description("Song file in config/deep/songs (.nbs, .mid, .txt, .mcsp). Leave empty to play the notes below.")
            .build()));
    private final StringSetting song = this.setting(StringSetting.from(StringSetting.builder()
            .id("song")
            .name("Song")
            .defaultValue("0 4 7 12")
            .description("Whitespace/comma separated notes. Use + for chords and tick:note for explicit ticks.")
            .build()));
    private final StringListSetting legacyNotes = this.setting(StringListSetting.from(StringListSetting.builder()
            .id("notes")
            .name("Legacy Notes")
            .addAllDefaultValue(List.of("0", "4", "7", "12"))
            .build()));
    private final NumberSetting delay = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("delay")
            .name("Play Delay")
            .defaultValue(6.0)
            .min(0.0)
            .max(40.0)
            .step(1.0)
            .description("Ticks between notes for typed songs. Song files keep their own tempo.")
            .build()));
    private final NumberSetting tuneDelay = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("tune_delay")
            .name("Tune Delay")
            .defaultValue(2.0)
            .min(0.0)
            .max(20.0)
            .step(1.0)
            .build()));
    private final NumberSetting range = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("range")
            .name("Range")
            .defaultValue(5.0)
            .min(1.0)
            .max(8.0)
            .step(0.5)
            .build()));
    private final BooleanSetting tune = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("tune")
            .name("Tune")
            .defaultValue(true)
            .build()));
    private final BooleanSetting rotate = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("rotate")
            .name("Rotate")
            .defaultValue(true)
            .build()));
    private final BooleanSetting swing = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("swing")
            .name("Swing")
            .defaultValue(true)
            .build()));
    private final BooleanSetting loop = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("loop")
            .name("Loop")
            .defaultValue(true)
            .build()));
    private final SongFiles songFiles = SongFiles.defaults();
    private Stage stage = Stage.PREPARE;
    private NotebotSong parsedSong = NotebotSong.EMPTY;
    private String loadedFile;
    private NotebotSong loadedFileSong = NotebotSong.EMPTY;
    private final Map<SongNote, List<BlockPos>> blocksByNote = new HashMap<>();
    private final Map<BlockPos, Integer> targetNotes = new HashMap<>();
    private final SequencedMap<BlockPos, Integer> tuneHits = new LinkedHashMap<>();
    private final Map<BlockPos, Integer> observedNotes = new HashMap<>();
    private int cooldownTicks;
    private int songTick;

    public NotebotModule() {
        super("notebot", "Notebot", ModuleCategory.MISC);
        this.legacyNotes.visibleWhen(() -> false);
        this.song.visibleWhen(() -> this.songFile.value().isBlank());
    }

    public SongFiles songFiles() {
        return this.songFiles;
    }

    /**
     * Selects a song file and restarts playback from the beginning.
     */
    public void selectSong(final String fileName) {
        this.songFile.value(fileName == null ? "" : fileName);
        this.loadedFile = null;
        this.resetRuntime();
    }

    public String selectedSong() {
        return this.songFile.value();
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null || client.gameMode == null || client.getConnection() == null) {
            this.debugStatus("Waiting for world");
            return;
        }
        if (this.cooldownTicks > 0) {
            this.cooldownTicks--;
            return;
        }
        if (this.stage == Stage.PREPARE && !this.prepare(client, player)) {
            this.cooldownTicks = 20;
            return;
        }
        if (this.stage == Stage.TUNE) {
            this.tuneNextBlock(client, player);
            return;
        }
        if (this.stage == Stage.PLAY) {
            this.playSongTick(client, player);
        }
    }

    @Override
    public void soundPacket(final Minecraft client, final ClientboundSoundPacket packet) {
        if (this.targetNotes.isEmpty()) {
            return;
        }
        if (!SoundScan.soundId(packet).toString().startsWith("minecraft:block.note_block.")) {
            return;
        }
        Vec3 position = new Vec3(packet.getX(), packet.getY(), packet.getZ());
        for (BlockPos pos : this.targetNotes.keySet()) {
            if (Vec3.atCenterOf(pos).distanceToSqr(position) <= 1.0) {
                this.observedNotes.put(pos, pitchToNote(packet.getPitch()));
                return;
            }
        }
    }

    @Override
    protected void onDisable() {
        this.resetRuntime();
        this.clearDebugValues();
    }

    private boolean prepare(final Minecraft client, final LocalPlayer player) {
        this.resetRuntime();
        this.parsedSong = this.loadSong();
        if (this.parsedSong.empty()) {
            if (this.songFile.value().isBlank()) {
                this.debugStatus("No notes configured");
            }
            return false;
        }
        List<ScannedNoteBlock> scannedBlocks = this.scanNoteBlocks(client, player);
        NoteBlockPlan plan = NoteBlockPlan.create(this.parsedSong, scannedBlocks);
        if (!plan.possible()) {
            this.debugStatus("Missing note blocks: " + describeShortfall(plan.shortfall()));
            return false;
        }
        this.blocksByNote.putAll(plan.blocksByNote());
        this.targetNotes.putAll(plan.targetNotes());
        this.observedNotes.putAll(plan.observedNotes());
        this.tuneHits.putAll(plan.tuneHits());
        this.songTick = 0;
        this.stage = this.tune.value() && !this.tuneHits.isEmpty() ? Stage.TUNE : Stage.PLAY;
        this.debugStatus(this.stage.name().toLowerCase(Locale.ROOT));
        return true;
    }

    private NotebotSong loadSong() {
        String file = this.songFile.value().trim();
        if (file.isEmpty()) {
            return parseSong(this.songText());
        }
        if (!file.equals(this.loadedFile)) {
            this.loadedFile = file;
            try {
                this.loadedFileSong = this.songFiles.load(file);
            } catch (Exception exception) {
                this.loadedFileSong = NotebotSong.EMPTY;
                this.debugStatus("Could not load " + file + ": " + exception.getMessage());
                return NotebotSong.EMPTY;
            }
        }
        if (this.loadedFileSong.empty()) {
            this.debugStatus(file + " has no playable notes");
        }
        return this.loadedFileSong;
    }

    private void tuneNextBlock(final Minecraft client, final LocalPlayer player) {
        if (this.tuneHits.isEmpty()) {
            this.stage = Stage.PLAY;
            this.cooldownTicks = this.parsedSong.gameTickTiming() ? 0 : this.delay.value().intValue();
            this.debugStatus("playing");
            return;
        }
        Map.Entry<BlockPos, Integer> entry = this.tuneHits.firstEntry();
        BlockPos pos = entry.getKey();
        Integer targetNote = this.targetNotes.get(pos);
        if (targetNote == null || !this.isUsableNoteBlock(client, player, pos)) {
            this.tuneHits.pollFirstEntry();
            return;
        }
        Vec3 hitLocation = Vec3.atCenterOf(pos);
        if (this.rotate.value()) {
            rotateToward(this, player, hitLocation);
        }
        InteractionResult result = client.gameMode.useItemOn(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(hitLocation, Direction.UP, pos, false));
        if (!result.consumesAction()) {
            this.cooldownTicks = 2;
            return;
        }
        if (this.swing.value()) {
            player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, false);
        }
        int current = this.currentNote(client, pos);
        this.observedNotes.put(pos, (current + 1) % 25);
        int remaining = entry.getValue() - 1;
        if (remaining <= 0) {
            this.tuneHits.pollFirstEntry();
        } else {
            entry.setValue(remaining);
        }
        this.debugValue("stage", "tuning");
        this.debugValue("remaining", String.valueOf(this.tuneHits.values().stream().mapToInt(Integer::intValue).sum()));
        this.debugValue("target", targetNote);
        this.cooldownTicks = this.tuneDelay.value().intValue();
    }

    private void playSongTick(final Minecraft client, final LocalPlayer player) {
        if (this.songTick > this.parsedSong.lastTick()) {
            if (!this.loop.value()) {
                this.enabled(false);
                return;
            }
            this.songTick = 0;
        }
        List<SongNote> notes = this.parsedSong.notesByTick().getOrDefault(this.songTick, List.of());
        Set<BlockPos> usedBlocks = new HashSet<>();
        for (SongNote note : notes) {
            BlockPos pos = this.nextBlockForNote(note, usedBlocks);
            if (pos == null) {
                continue;
            }
            if (this.rotate.value() && usedBlocks.isEmpty()) {
                rotateToward(this, player, Vec3.atCenterOf(pos));
            }
            client.getConnection().getConnection().send(new ServerboundPlayerActionPacket(
                    ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,
                    pos,
                    Direction.UP
            ));
            usedBlocks.add(pos);
        }
        if (!usedBlocks.isEmpty() && this.swing.value()) {
            player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, false);
        }
        this.debugValue("stage", "playing");
        this.debugValue("tick", this.songTick + "/" + this.parsedSong.lastTick());
        this.debugValue("notes", notes.size());
        this.songTick++;
        this.cooldownTicks = this.parsedSong.gameTickTiming() ? 0 : this.delay.value().intValue();
    }

    private BlockPos nextBlockForNote(final SongNote note, final Set<BlockPos> usedBlocks) {
        List<BlockPos> blocks = this.blocksByNote.getOrDefault(note, List.of());
        for (BlockPos block : blocks) {
            if (!usedBlocks.contains(block)) {
                return block;
            }
        }
        return null;
    }

    private List<ScannedNoteBlock> scanNoteBlocks(final Minecraft client, final LocalPlayer player) {
        List<ScannedNoteBlock> blocks = new ArrayList<>();
        int radius = (int) Math.ceil(this.range.value());
        BlockPos center = player.blockPosition();
        for (int y = -radius; y <= radius; y++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = center.offset(x, y, z);
                    if (!this.isUsableNoteBlock(client, player, pos)) {
                        continue;
                    }
                    BlockState state = client.level.getBlockState(pos);
                    NoteBlockInstrument instrument = state.getValue(NoteBlock.INSTRUMENT);
                    if (!instrument.isTunable()) {
                        continue;
                    }
                    blocks.add(new ScannedNoteBlock(pos.immutable(), instrument, state.getValue(NoteBlock.NOTE),
                            player.distanceToSqr(Vec3.atCenterOf(pos))));
                }
            }
        }
        blocks.sort(Comparator.comparingDouble(ScannedNoteBlock::distanceSq));
        return blocks;
    }

    private boolean isUsableNoteBlock(final Minecraft client, final LocalPlayer player, final BlockPos pos) {
        return client.level.isLoaded(pos)
                && client.level.getBlockState(pos).is(Blocks.NOTE_BLOCK)
                && client.level.getBlockState(pos.above()).isAir()
                && player.isWithinBlockInteractionRange(pos, 1.0);
    }

    private int currentNote(final Minecraft client, final BlockPos pos) {
        Integer observed = this.observedNotes.get(pos);
        if (observed != null) {
            return observed;
        }
        if (client.level != null && client.level.isLoaded(pos) && client.level.getBlockState(pos).is(Blocks.NOTE_BLOCK)) {
            return client.level.getBlockState(pos).getValue(NoteBlock.NOTE);
        }
        return 0;
    }

    private String songText() {
        if (!this.song.value().isBlank()) {
            return this.song.value();
        }
        return String.join(" ", this.legacyNotes.value());
    }

    private void resetRuntime() {
        this.stage = Stage.PREPARE;
        this.parsedSong = NotebotSong.EMPTY;
        this.blocksByNote.clear();
        this.targetNotes.clear();
        this.tuneHits.clear();
        this.observedNotes.clear();
        this.cooldownTicks = 0;
        this.songTick = 0;
    }

    private void debugStatus(final String status) {
        this.debugValue("stage", this.stage.name().toLowerCase(Locale.ROOT));
        this.debugValue("status", status);
        this.debugValue("blocks", this.targetNotes.size());
    }

    static String describeShortfall(final Map<String, Integer> shortfall) {
        return new TreeMap<>(shortfall).entrySet().stream()
                .map(entry -> entry.getValue() + " " + entry.getKey())
                .collect(Collectors.joining(", "));
    }

    static int parseNote(final String value) {
        try {
            return parseNoteToken(value);
        } catch (RuntimeException exception) {
            return 0;
        }
    }

    static float pitch(final int note) {
        return (float) Math.pow(2.0, (Math.max(0, Math.min(24, note)) - 12) / 12.0);
    }

    static int pitchToNote(final float pitch) {
        if (pitch <= 0.0F || !Float.isFinite(pitch)) {
            return 0;
        }
        return Math.max(0, Math.min(24, Math.round(12.0F + 12.0F * (float) (Math.log(pitch) / Math.log(2.0)))));
    }

    static int tuneClicks(final int from, final int to) {
        int safeFrom = Math.max(0, Math.min(24, from));
        int safeTo = Math.max(0, Math.min(24, to));
        return safeFrom <= safeTo ? safeTo - safeFrom : 25 - safeFrom + safeTo;
    }

    static NotebotSong parseSong(final String text) {
        if (text == null || text.isBlank()) {
            return NotebotSong.EMPTY;
        }
        Map<Integer, List<SongNote>> notes = new LinkedHashMap<>();
        int nextTick = 0;
        for (String token : text.split("[,;\\s]+")) {
            if (token.isBlank()) {
                continue;
            }
            String noteToken = token.trim();
            int tick = nextTick;
            int colon = noteToken.indexOf(':');
            if (colon >= 0) {
                try {
                    tick = Math.max(0, Integer.parseInt(noteToken.substring(0, colon).trim()));
                    noteToken = noteToken.substring(colon + 1);
                } catch (RuntimeException exception) {
                    continue;
                }
            }
            List<SongNote> tickNotes = notes.computeIfAbsent(tick, ignored -> new ArrayList<>());
            for (String part : noteToken.split("\\+")) {
                if (!part.isBlank()) {
                    tickNotes.add(SongNote.any(parseNote(part)));
                }
            }
            nextTick = tick + 1;
        }
        return NotebotSong.of(notes, false);
    }

    private static int parseNoteToken(final String value) {
        String token = value.trim().toLowerCase(Locale.ROOT);
        if (token.isEmpty()) {
            return 0;
        }
        try {
            return Math.max(0, Math.min(24, Integer.parseInt(token)));
        } catch (NumberFormatException ignored) {
            return switch (token) {
                case "c", "c4" -> 6;
                case "c#", "db", "c#4", "db4" -> 7;
                case "d", "d4" -> 8;
                case "d#", "eb", "d#4", "eb4" -> 9;
                case "e", "e4" -> 10;
                case "f", "f4" -> 11;
                case "f#", "gb", "f#4", "gb4" -> 12;
                case "g", "g4" -> 13;
                case "g#", "ab", "g#4", "ab4" -> 14;
                case "a", "a4" -> 15;
                case "a#", "bb", "a#4", "bb4" -> 16;
                case "b", "b4" -> 17;
                default -> 0;
            };
        }
    }

    private static void rotateToward(final Module owner, final LocalPlayer player, final Vec3 target) {
        RotationManager.request(new RotationRequest(
                owner.id(),
                Rotation.lookingAt(target, player.getEyePosition()),
                65,
                90.0F,
                1,
                2.0F,
                RotationTurnMode.STEPPED,
                true
        ));
        RotationManager.apply(player);
    }

    private enum Stage {
        PREPARE,
        TUNE,
        PLAY
    }

    record ScannedNoteBlock(BlockPos pos, NoteBlockInstrument instrument, int note, double distanceSq) {
    }

    /**
     * Assigns nearby note blocks to the notes a song needs. Specific instruments are matched first so that
     * instrument-agnostic notes cannot take blocks that a specific instrument needs.
     */
    record NoteBlockPlan(Map<String, Integer> shortfall, Map<SongNote, List<BlockPos>> blocksByNote,
                         Map<BlockPos, Integer> targetNotes, Map<BlockPos, Integer> observedNotes,
                         SequencedMap<BlockPos, Integer> tuneHits) {

        boolean possible() {
            return this.shortfall.isEmpty();
        }

        static NoteBlockPlan create(final NotebotSong song, final List<ScannedNoteBlock> scannedBlocks) {
            Map<SongNote, List<BlockPos>> blocksByNote = new HashMap<>();
            Map<BlockPos, Integer> targetNotes = new HashMap<>();
            Map<BlockPos, Integer> observedNotes = new HashMap<>();
            SequencedMap<BlockPos, Integer> tuneHits = new LinkedHashMap<>();
            Map<String, Integer> shortfall = new HashMap<>();
            ArrayDeque<ScannedNoteBlock> availableBlocks = new ArrayDeque<>(scannedBlocks);

            List<SongNote> notes = song.requiredBlocks().keySet().stream()
                    .sorted(Comparator.comparing((SongNote note) -> note.instrument() == null)
                            .thenComparing(note -> note.instrument() == null ? "" : note.instrument().getSerializedName())
                            .thenComparingInt(SongNote::key))
                    .toList();
            for (SongNote note : notes) {
                int required = song.requiredBlocks().get(note);
                for (int index = 0; index < required; index++) {
                    ScannedNoteBlock block = removeBestBlock(availableBlocks, note);
                    if (block == null) {
                        shortfall.merge(instrumentName(note), required - index, Integer::sum);
                        break;
                    }
                    blocksByNote.computeIfAbsent(note, ignored -> new ArrayList<>()).add(block.pos());
                    targetNotes.put(block.pos(), note.key());
                    observedNotes.put(block.pos(), block.note());
                    int clicks = tuneClicks(block.note(), note.key());
                    if (clicks > 0) {
                        tuneHits.put(block.pos(), clicks);
                    }
                }
            }
            if (!shortfall.isEmpty()) {
                return new NoteBlockPlan(Map.copyOf(shortfall), Map.of(), Map.of(), Map.of(), new LinkedHashMap<>());
            }
            Map<SongNote, List<BlockPos>> immutableBlocksByNote = new HashMap<>();
            blocksByNote.forEach((note, blocks) -> immutableBlocksByNote.put(note, List.copyOf(blocks)));
            return new NoteBlockPlan(Map.of(), Map.copyOf(immutableBlocksByNote), Map.copyOf(targetNotes),
                    Map.copyOf(observedNotes), new LinkedHashMap<>(tuneHits));
        }

        private static String instrumentName(final SongNote note) {
            return note.instrument() == null ? "any" : note.instrument().getSerializedName();
        }

        private static ScannedNoteBlock removeBestBlock(final ArrayDeque<ScannedNoteBlock> blocks, final SongNote note) {
            ScannedNoteBlock best = null;
            for (ScannedNoteBlock block : blocks) {
                if (!note.accepts(block.instrument())) {
                    continue;
                }
                int blockClicks = tuneClicks(block.note(), note.key());
                int bestClicks = best == null ? Integer.MAX_VALUE : tuneClicks(best.note(), note.key());
                if (best == null || blockClicks < bestClicks
                        || blockClicks == bestClicks && block.distanceSq() < best.distanceSq()) {
                    best = block;
                }
            }
            if (best != null) {
                blocks.remove(best);
            }
            return best;
        }
    }
}
