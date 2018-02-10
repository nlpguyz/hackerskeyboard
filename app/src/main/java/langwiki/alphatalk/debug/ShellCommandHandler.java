package org.langwiki.alphatalk.debug;

import org.langwiki.alphatalk.debug.cli.Command;
import org.langwiki.alphatalk.debug.cli.HelpCommandHandler;
import org.langwiki.alphatalk.debug.cli.Shell;
import org.langwiki.alphatalk.debug.cli.ShellDependent;
import org.langwiki.alphatalk.script.ScriptManager;

import javax.script.ScriptEngine;

/**
 * Shell commands for debug console.
 */
public class ShellCommandHandler implements ShellDependent {
    protected Shell mShell;
    protected HelpCommandHandler mHelpHandler = new HelpCommandHandler();

    protected ScriptHandler mScriptHandler;

    public ShellCommandHandler() {
    }

    //@Command
    public Object lua() {
        return enterLanguage(ScriptManager.LANG_LUA);
    }

    @Command
    public Object js() {
        return enterLanguage(ScriptManager.LANG_JAVASCRIPT);
    }

    private Object enterLanguage(String language) {
        ScriptEngine engine = ScriptManager.getInstance().getEngine(language);

        if (engine == null) {
            return "Cannot find the language engine for " + language;
        }

        mScriptHandler = new ScriptHandler(language, engine);
        mShell.setLineProcessor(mScriptHandler);

        return null;
    }

    @Command
    public Object version() {
        return "1.0";
    }

    @Command
    public Object help() {
        return mHelpHandler.help();
    }

    @Command
    public void exit() {
        // empty
    }

    @Override
    public void cliSetShell(Shell theShell) {
        mShell = theShell;
        mHelpHandler.cliSetShell(mShell);
    }
}
