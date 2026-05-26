public enum Difficulty {

    // Each difficulty stores the text shown in the menu and the gameplay values
    // used when that difficulty is selected.
    EASY("Easy", 1, "Relaxed play", new int[]{3, 1, 5}),
    NORMAL("Normal", 2, "Normal challenge", new int[]{5, 2, 3}),
    HARD("Hard", 3, "For experts only", new int[]{8, 3, 1});

    // Display values for the menu.
    private final String displayName;
    private final int level;
    private final String description;

    // Gameplay values tied to this difficulty.
    private final int lives;
    private final int speed;
    private final int timeBonus;

    // The values array stores lives, speed, and time bonus in that order.
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

    // Builds a simple difficulty rating, such as "**-" for normal difficulty.
    public String getStars() {
        return "*".repeat(level) + "-".repeat(3 - level);
    }

    // Gives a readable summary when the difficulty is printed for debugging.
    @Override
    public String toString() {
        return String.format("[%s] Level %d | Lives: %d | Speed: %d | Time bonus: %ds | %s",
                displayName, level, lives, speed, timeBonus, description);
    }
}
