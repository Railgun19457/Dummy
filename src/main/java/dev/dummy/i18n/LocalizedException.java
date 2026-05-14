package dev.dummy.i18n;

public final class LocalizedException extends IllegalArgumentException {
    private final String key;
    private final Object[] args;

    public LocalizedException(String key, Object... args) {
        super(key);
        this.key = key;
        this.args = args;
    }

    public String key() {
        return key;
    }

    public Object[] args() {
        return args;
    }
}
