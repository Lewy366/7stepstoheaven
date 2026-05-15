public enum Difficulty {

    EASY("Easy", 1, "Relaxed play", new int[]{3, 1, 5}),
    NORMAL("Normal", 2, "Standard challenge", new int[]{5, 2, 3}),
    HARD("Hard", 3, "For experienced players", new int[]{8, 3, 1});

    private final String displayName;
    private final int level;
    private final String description;
    private final int lives;
    private final int speed;
    private final int timeBonus;

    Difficulty(String displayName, int level, String description, int[] values) {
        this.displayName = displayName;
        this.level = level;
        this.description = description;
        this.lives = values[0];
        this.speed = values[1];
        this.timeBonus = values[2];
    }

    public String getDisplayName() { return displayName; }
    public int getLevel() { return level; }
    public String getDescription() { return description; }
    public int getLives() { return lives; }
    public int getSpeed() { return speed; }
    public int getTimeBonus() { return timeBonus; }

    public String getStars() {
        return "★".repeat(level) + "☆".repeat(3 - level);
    }

    @Override
    public String toString() {
        return String.format("[%s] Level %d | Lives: %d | Speed: %d | Time bonus: %ds | %s",
                displayName, level, lives, speed, timeBonus, description);
    }
}
