import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

public class Controls {
    public static final int MOVE_LEFT = 0;
    public static final int MOVE_RIGHT = 1;
    public static final int JUMP = 2;
    public static final int DUCK = 3;
    public static final int DASH = 4;
    public static final int MELEE = 5;
    public static final int SHOOT = 6;

    private static final String[] ACTION_NAMES = {
        "Move Left",
        "Move Right",
        "Jump",
        "Duck",
        "Dash",
        "Melee",
        "Shoot"
    };

    private static final int KEY_BINDING = 0;
    private static final int MOUSE_BINDING = 1;

    private static final int[] DEFAULT_TYPES = {
        KEY_BINDING,
        KEY_BINDING,
        KEY_BINDING,
        KEY_BINDING,
        KEY_BINDING,
        KEY_BINDING,
        KEY_BINDING
    };

    private static final int[] DEFAULT_CODES = {
        KeyEvent.VK_A,
        KeyEvent.VK_D,
        KeyEvent.VK_W,
        KeyEvent.VK_S,
        KeyEvent.VK_SHIFT,
        KeyEvent.VK_F,
        KeyEvent.VK_E
    };

    private static final int[] bindingTypes = DEFAULT_TYPES.clone();
    private static final int[] bindingCodes = DEFAULT_CODES.clone();

    public static int getActionCount() {
        return ACTION_NAMES.length;
    }

    public static String getActionName(int action) {
        return ACTION_NAMES[action];
    }

    public static String getBindingText(int action) {
        if (bindingTypes[action] == MOUSE_BINDING) {
            return getMouseButtonText(bindingCodes[action]);
        }
        return KeyEvent.getKeyText(bindingCodes[action]);
    }

    public static boolean matches(int action, int keyCode) {
        return matchesKey(action, keyCode);
    }

    public static boolean matchesKey(int action, int keyCode) {
        return bindingTypes[action] == KEY_BINDING && bindingCodes[action] == keyCode;
    }

    public static boolean matchesMouse(int action, int mouseButton) {
        return bindingTypes[action] == MOUSE_BINDING && bindingCodes[action] == mouseButton;
    }

    public static void setBinding(int action, int keyCode) {
        setKeyBinding(action, keyCode);
    }

    public static void setKeyBinding(int action, int keyCode) {
        bindingTypes[action] = KEY_BINDING;
        bindingCodes[action] = keyCode;
    }

    public static void setMouseBinding(int action, int mouseButton) {
        bindingTypes[action] = MOUSE_BINDING;
        bindingCodes[action] = mouseButton;
    }

    public static void resetDefaults() {
        System.arraycopy(DEFAULT_TYPES, 0, bindingTypes, 0, DEFAULT_TYPES.length);
        System.arraycopy(DEFAULT_CODES, 0, bindingCodes, 0, DEFAULT_CODES.length);
    }

    public static String getShortSummary() {
        return getBindingText(MOVE_LEFT) + "/" + getBindingText(MOVE_RIGHT)
            + " move | " + getBindingText(JUMP) + " jump | "
            + getBindingText(DUCK) + " duck | " + getBindingText(DASH)
            + " dash | " + getBindingText(MELEE) + " melee | "
            + getBindingText(SHOOT) + " shoot | ESC pause";
    }

    private static String getMouseButtonText(int button) {
        if (button == MouseEvent.BUTTON1) return "Mouse Left";
        if (button == MouseEvent.BUTTON2) return "Mouse Middle";
        if (button == MouseEvent.BUTTON3) return "Mouse Right";
        return "Mouse " + button;
    }
}
