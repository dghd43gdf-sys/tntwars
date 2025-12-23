package com.example.tntwars.game;

import com.example.tntwars.model.TeamColor;
import com.example.tntwars.util.FormatUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ScoreboardService {
    private static final String OBJECTIVE_ID = "tntwars";

    private final Map<String, Scoreboard> scoreboards = new HashMap<>();

    public void update(GameSession session, List<Player> participants) {
        if (participants.isEmpty()) {
            return;
        }
        Scoreboard scoreboard = ensureScoreboard(session);
        if (scoreboard == null) {
            return;
        }
        Objective objective = scoreboard.getObjective(OBJECTIVE_ID);
        if (objective == null) {
            objective = scoreboard.registerNewObjective(OBJECTIVE_ID, "dummy", FormatUtil.tntTitleLegacy("TNT Wars"));
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        } else {
            objective.setDisplayName(FormatUtil.tntTitleLegacy("TNT Wars"));
        }

        clearEntries(scoreboard);
        applyLines(session, objective);

        for (Player participant : participants) {
            if (participant.isOnline()) {
                participant.setScoreboard(scoreboard);
            }
        }
    }

    public void clear(GameSession session, List<Player> participants) {
        String key = session.getArena().getWorldName().toLowerCase();
        Scoreboard scoreboard = scoreboards.remove(key);
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        Scoreboard mainBoard = manager != null ? manager.getMainScoreboard() : null;
        for (Player participant : participants) {
            if (participant.isOnline() && mainBoard != null) {
                participant.setScoreboard(mainBoard);
            }
        }
        if (scoreboard != null) {
            Objective objective = scoreboard.getObjective(OBJECTIVE_ID);
            if (objective != null) {
                objective.unregister();
            }
            scoreboard.clearSlot(DisplaySlot.SIDEBAR);
        }
    }

    private Scoreboard ensureScoreboard(GameSession session) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) {
            return null;
        }
        String key = session.getArena().getWorldName().toLowerCase();
        return scoreboards.computeIfAbsent(key, ignored -> {
            Scoreboard scoreboard = manager.getNewScoreboard();
            Objective objective = scoreboard.registerNewObjective(OBJECTIVE_ID, "dummy", FormatUtil.tntTitleLegacy("TNT Wars"));
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
            return scoreboard;
        });
    }

    private void applyLines(GameSession session, Objective objective) {
        setLine(objective, 4, FormatUtil.blankLine(0));
        setLine(objective, 3, FormatUtil.teamCountLine(TeamColor.RED, onlineCount(session, TeamColor.RED)));
        setLine(objective, 2, FormatUtil.teamCountLine(TeamColor.BLUE, onlineCount(session, TeamColor.BLUE)));
        setLine(objective, 1, FormatUtil.blankLine(1));
    }

    private void setLine(Objective objective, int score, String entry) {
        Score line = objective.getScore(entry);
        line.setScore(score);
    }

    private int onlineCount(GameSession session, TeamColor team) {
        int count = 0;
        for (UUID uuid : session.getTeamMembers(team)) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                count++;
            }
        }
        return count;
    }

    private void clearEntries(Scoreboard scoreboard) {
        for (String entry : new HashSet<>(scoreboard.getEntries())) {
            scoreboard.resetScores(entry);
        }
    }
}
