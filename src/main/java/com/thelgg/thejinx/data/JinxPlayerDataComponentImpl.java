package com.thelgg.thejinx.data;

import com.mojang.serialization.Codec;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;

public class JinxPlayerDataComponentImpl implements JinxPlayerDataComponent, AutoSyncedComponent {
    private static final String TAG_IS_JINX = "IsJinx";
    private static final String TAG_JINX_DEATHS = "JinxDeaths";
    private static final String TAG_USED_ASSIMILATION = "UsedAssimilation";
    private static final String TAG_REGRANT_ASSIMILATION = "RegrantAssimilation";
    private static final String TAG_SPRINT_TICKS = "SprintTicks";
    private static final String TAG_RECOVERY_TICKS = "RecoveryTicks";
    private static final String TAG_MISSING_PARTS = "MissingParts";
    private static final String TAG_LAUNCHED_TNT = "LaunchedHumanTnt";
    private static final String TAG_SCREEN_OPEN = "ScreenOpen";

    private final PlayerEntity owner;
    private boolean isJinx;
    private int jinxDeaths;
    private boolean usedAssimilation;
    private boolean regrantAssimilation;
    private long sprintProgressTicks;
    private long recoveryProgressTicks;
    private final EnumSet<LimbPart> missingParts = EnumSet.noneOf(LimbPart.class);
    private boolean launchedAsHumanTnt;
    private boolean screenOpen;

    public JinxPlayerDataComponentImpl(PlayerEntity owner) {
        this.owner = owner;
    }

    @Override
    public boolean isJinx() {
        return isJinx;
    }

    @Override
    public void setJinx(boolean value) {
        this.isJinx = value;
        sync();
    }

    @Override
    public int getJinxDeaths() {
        return jinxDeaths;
    }

    @Override
    public void setJinxDeaths(int deaths) {
        this.jinxDeaths = Math.max(0, deaths);
        sync();
    }

    @Override
    public void incrementJinxDeaths() {
        this.jinxDeaths++;
        sync();
    }

    @Override
    public boolean hasUsedAssimilation() {
        return usedAssimilation;
    }

    @Override
    public void setUsedAssimilation(boolean used) {
        this.usedAssimilation = used;
        sync();
    }

    @Override
    public boolean shouldRegrantAssimilation() {
        return regrantAssimilation;
    }

    @Override
    public void setShouldRegrantAssimilation(boolean value) {
        this.regrantAssimilation = value;
        sync();
    }

    @Override
    public long getSprintProgressTicks() {
        return sprintProgressTicks;
    }

    @Override
    public void setSprintProgressTicks(long ticks) {
        this.sprintProgressTicks = Math.max(0L, ticks);
        sync();
    }

    @Override
    public long getRecoveryProgressTicks() {
        return recoveryProgressTicks;
    }

    @Override
    public void setRecoveryProgressTicks(long ticks) {
        this.recoveryProgressTicks = Math.max(0L, ticks);
        sync();
    }

    @Override
    public Set<LimbPart> getMissingParts() {
        return Collections.unmodifiableSet(missingParts);
    }

    @Override
    public boolean hasMissingPart(LimbPart part) {
        return missingParts.contains(part);
    }

    @Override
    public void setMissingPart(LimbPart part, boolean missing) {
        if (missing) {
            missingParts.add(part);
        } else {
            missingParts.remove(part);
        }
        sync();
    }

    @Override
    public boolean isLaunchedAsHumanTnt() {
        return launchedAsHumanTnt;
    }

    @Override
    public void setLaunchedAsHumanTnt(boolean launched) {
        this.launchedAsHumanTnt = launched;
        sync();
    }

    @Override
    public boolean isScreenOpen() {
        return screenOpen;
    }

    @Override
    public void setScreenOpen(boolean open) {
        this.screenOpen = open;
        sync();
    }

    @Override
    public void readData(ReadView readView) {
        isJinx = readView.getBoolean(TAG_IS_JINX, false);
        jinxDeaths = readView.getInt(TAG_JINX_DEATHS, 0);
        usedAssimilation = readView.getBoolean(TAG_USED_ASSIMILATION, false);
        regrantAssimilation = readView.getBoolean(TAG_REGRANT_ASSIMILATION, false);
        sprintProgressTicks = Math.max(0L, readView.getLong(TAG_SPRINT_TICKS, 0L));
        recoveryProgressTicks = Math.max(0L, readView.getLong(TAG_RECOVERY_TICKS, 0L));
        launchedAsHumanTnt = readView.getBoolean(TAG_LAUNCHED_TNT, false);
        screenOpen = readView.getBoolean(TAG_SCREEN_OPEN, false);

        missingParts.clear();
        readView.getOptionalTypedListView(TAG_MISSING_PARTS, Codec.STRING).ifPresent(list ->
                list.stream()
                        .map(LimbPart::fromId)
                        .filter(part -> part != null)
                        .forEach(missingParts::add)
        );
    }

    @Override
    public void writeData(WriteView writeView) {
        writeView.putBoolean(TAG_IS_JINX, isJinx);
        writeView.putInt(TAG_JINX_DEATHS, jinxDeaths);
        writeView.putBoolean(TAG_USED_ASSIMILATION, usedAssimilation);
        writeView.putBoolean(TAG_REGRANT_ASSIMILATION, regrantAssimilation);
        writeView.putLong(TAG_SPRINT_TICKS, sprintProgressTicks);
        writeView.putLong(TAG_RECOVERY_TICKS, recoveryProgressTicks);
        writeView.putBoolean(TAG_LAUNCHED_TNT, launchedAsHumanTnt);
        writeView.putBoolean(TAG_SCREEN_OPEN, screenOpen);

        WriteView.ListAppender listAppender = writeView.getListAppender(TAG_MISSING_PARTS, Codec.STRING);
        for (LimbPart part : missingParts) {
            listAppender.add(part.id());
        }
    }

    private void sync() {
        if (owner instanceof ServerPlayerEntity serverPlayer) {
            JinxEntityComponents.JINX_DATA.sync(serverPlayer);
        }
    }
}
