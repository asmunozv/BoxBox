package view;

import java.time.LocalDate;

public final class SeasonContext {

    private static int activeSeason = LocalDate.now().getYear();

    private SeasonContext() {
    }

    public static int getActiveSeason() {
        return activeSeason;
    }

    public static void setActiveSeason(int season) {
        activeSeason = season;
    }
}
