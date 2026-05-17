import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

/**
 * Controls class manages all input bindings for the game.
 * Supports both keyboard and mouse controls with customizable rebinding.
 * Actions can be dynamically reassigned to different keys or mouse buttons.
 */
public class Controls {
    // --- Action Constants ---
    // These constants represent different game actions, indexed 0-6
    public static final int MOVE_LEFT = 0;
    public static final int MOVE_RIGHT = 1;
    public static final int JUMP = 2;
    public static final int DUCK = 3;
    public static final int DASH = 4;
    public static final int MELEE = 5;
    public static final int SHOOT = 6;

    // --- Display Names ---
    // User-friendly names for each action (used in UI menus)
    private static final String[] ACTION_NAMES = {
        "Move Left",
        "Move Right",
        "Jump",
        "Duck",
        "Dash",
        "Melee",
        "Shoot"
    };

    // --- Binding Type Constants ---
    // Define whether a binding is for keyboard or mouse input
    private static final int KEY_BINDING = 0;
    private static final int MOUSE_BINDING = 1;

    // --- Default Control Mappings ---
    // All actions default to keyboard bindings
    private static final int[] DEFAULT_TYPES = {
        KEY_BINDING,
        KEY_BINDING,
        KEY_BINDING,
        KEY_BINDING,
        KEY_BINDING,
        KEY_BINDING,
        KEY_BINDING
    };

    // Default key codes: A, D, W, S, Shift, F, E
    private static final int[] DEFAULT_CODES = {
        KeyEvent.VK_A,
        KeyEvent.VK_D,
        KeyEvent.VK_W,
        KeyEvent.VK_S,
        KeyEvent.VK_SHIFT,
        KeyEvent.VK_F,
        KeyEvent.VK_E
    };

    // --- Current Bindings (Modifiable at Runtime) ---
    // These arrays store the current control configuration and can be changed by the player
    private static final int[] bindingTypes = DEFAULT_TYPES.clone();
    private static final int[] bindingCodes = DEFAULT_CODES.clone();

    // --- Query Methods ---
    // Methods to get information about actions and current bindings

    /** Returns the total number of actions */
    public static int getActionCount() {
        return ACTION_NAMES.length;
    }

    /** Returns the display name for a given action */
    public static String getActionName(int action) {
        return ACTION_NAMES[action];
    }

    /** Returns a human-readable string of what key/button is bound to an action */
    public static String getBindingText(int action) {
        if (bindingTypes[action] == MOUSE_BINDING) {
            return getMouseButtonText(bindingCodes[action]);
        }
        return KeyEvent.getKeyText(bindingCodes[action]);
    }

    // --- Input Matching Methods ---
    // These methods check if a key/mouse event matches a specific action

    /** Checks if a key code matches any action (delegates to matchesKey) */
    public static boolean matches(int action, int keyCode) {
        return matchesKey(action, keyCode);
    }

    /** Checks if a keyboard key matches a specific action */
    public static boolean matchesKey(int action, int keyCode) {
        return bindingTypes[action] == KEY_BINDING && bindingCodes[action] == keyCode;
    }

    /** Checks if a mouse button matches a specific action */
    public static boolean matchesMouse(int action, int mouseButton) {
        return bindingTypes[action] == MOUSE_BINDING && bindingCodes[action] == mouseButton;
    }

    // --- Rebinding Methods ---
    // These methods allow players to customize their control scheme

    /** Generic setter - binds an action to a keyboard key */
    public static void setBinding(int action, int keyCode) {
        setKeyBinding(action, keyCode);
    }

    /** Rebinds an action to a specific keyboard key */
    public static void setKeyBinding(int action, int keyCode) {
        bindingTypes[action] = KEY_BINDING;
        bindingCodes[action] = keyCode;
    }

    /** Rebinds an action to a specific mouse button */
    public static void setMouseBinding(int action, int mouseButton) {
        bindingTypes[action] = MOUSE_BINDING;
        bindingCodes[action] = mouseButton;
    }

    /** Resets all bindings to the default configuration */
    public static void resetDefaults() {
        System.arraycopy(DEFAULT_TYPES, 0, bindingTypes, 0, DEFAULT_TYPES.length);
        System.arraycopy(DEFAULT_CODES, 0, bindingCodes, 0, DEFAULT_CODES.length);
    }

    // --- UI Helper Methods ---
    /** Returns a formatted string of all current bindings for display in UI */
    public static String getShortSummary() {
        return getBindingText(MOVE_LEFT) + "/" + getBindingText(MOVE_RIGHT)
            + " move | " + getBindingText(JUMP) + " jump | "
            + getBindingText(DUCK) + " duck | " + getBindingText(DASH)
            + " dash | " + getBindingText(MELEE) + " melee | "
            + getBindingText(SHOOT) + " shoot | ESC pause";
    }

    // --- Private Helper Methods ---
    /** Converts mouse button constants to readable text */
    private static String getMouseButtonText(int button) {
        if (button == MouseEvent.BUTTON1) return "Mouse Left";
        if (button == MouseEvent.BUTTON2) return "Mouse Middle";
        if (button == MouseEvent.BUTTON3) return "Mouse Right";
        return "Mouse " + button;
    }
} // End of Controls class
