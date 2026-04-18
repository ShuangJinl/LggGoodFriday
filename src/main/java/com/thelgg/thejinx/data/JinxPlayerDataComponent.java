package com.thelgg.thejinx.data;

import java.util.Set;
import org.ladysnake.cca.api.v3.component.Component;

public interface JinxPlayerDataComponent extends Component {
    boolean isJinx();

    void setJinx(boolean value);

    int getJinxDeaths();

    void setJinxDeaths(int deaths);

    void incrementJinxDeaths();

    boolean hasUsedAssimilation();

    void setUsedAssimilation(boolean used);

    boolean shouldRegrantAssimilation();

    void setShouldRegrantAssimilation(boolean value);

    long getSprintProgressTicks();

    void setSprintProgressTicks(long ticks);

    long getRecoveryProgressTicks();

    void setRecoveryProgressTicks(long ticks);

    Set<LimbPart> getMissingParts();

    boolean hasMissingPart(LimbPart part);

    void setMissingPart(LimbPart part, boolean missing);

    boolean isLaunchedAsHumanTnt();

    void setLaunchedAsHumanTnt(boolean launched);

    boolean isScreenOpen();

    void setScreenOpen(boolean open);
}
