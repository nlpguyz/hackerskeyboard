package org.langwiki.alphatalk.script;

@SuppressWarnings("serial")
public class ScriptError extends Exception {
    private Exception cause;

    public ScriptError(String exceptionString) {
        this(exceptionString, null);
    }

    public ScriptError(String exceptionString, Exception cause) {
        super(exceptionString);
        this.cause = cause;
    }

    @Override
    public String toString() {
        return "ScriptError{" +
                super.toString() +
                ", cause=" + cause +
                '}';
    }
}
