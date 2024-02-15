package me.nologic.castlewars.util;

import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.Player;

import java.util.List;

@RequiredArgsConstructor
public class BossBar {

    private final org.bukkit.boss.BossBar bar;

    public static BossBar bossBar(final String title, final double progress, final BarColor color, final BarStyle style) {
        return new BossBar(Bukkit.createBossBar(title, color, style)).progress(progress);
    }

    public double progress() {
        return this.bar.getProgress();
    }

    public BossBar progress(final double progress) {
        this.bar.setProgress(progress);
        return this;
    }

    public String title() {
        return this.bar.getTitle();
    }

    public BossBar title(final String title) {
        this.bar.setTitle(title);
        return this;
    }

    public BossBar color(final BarColor color) {
        this.bar.setColor(color);
        return this;
    }

    public BossBar style(final BarStyle style) {
        this.bar.setStyle(style);
        return this;
    }

    public BossBar addViewer(final Player player) {
        this.bar.addPlayer(player);
        return this;
    }

    public BossBar addViewers(final List<Player> players) {
        players.forEach(this.bar::addPlayer);
        return this;
    }

    public BossBar removeViewer(final Player player) {
        this.bar.removePlayer(player);
        return this;
    }

    public BossBar removeViewers(final List<Player> players) {
        players.forEach(this.bar::removePlayer);
        return this;
    }

    public BossBar cleanViewers() {
        this.bar.removeAll();
        return this;
    }

    public BossBar visible(final boolean visible) {
        this.bar.setVisible(visible);
        return this;
    }

}