package ru.kekih.vulcan.model;

public enum VulcanType {
    NORMAL("normal", "Обычный вулкан"),
    LEGENDARY("legendary", "Легендарный вулкан"),
    MYTHICAL("mythical", "Мифический вулкан");

    private final String key;
    private final String defaultName;

    VulcanType(String key, String defaultName) {
        this.key = key;
        this.defaultName = defaultName;
    }

    public String getKey() {
        return key;
    }

    public String getDefaultName() {
        return defaultName;
    }

    public static VulcanType fromString(String input) {
        if (input == null) return null;
        String lower = input.toLowerCase();
        for (VulcanType type : values()) {
            if (type.key.equals(lower) || type.name().equalsIgnoreCase(lower)) {
                return type;
            }
        }
        switch (lower) {
            case "обычный":
            case "normal":
                return NORMAL;
            case "легендарный":
            case "legendary":
                return LEGENDARY;
            case "мифический":
            case "mythical":
                return MYTHICAL;
            default:
                return null;
        }
    }
}
