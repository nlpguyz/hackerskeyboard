package org.mozilla.javascript;

import org.langwiki.alphatalk.script.ScriptManager;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

public class FunctionProxy {
    private static int sProxySerial = 0;

    InterpretedFunction interFunc;
    int proxySerial;

    private FunctionProxy(Object func) {
        interFunc = (InterpretedFunction) func;
        synchronized (FunctionProxy.class) {
            proxySerial = sProxySerial++;
        }
    }

    public void invoke(Object... args) {
        if (interFunc == null) {
            return;
        }

        // Pass to javascript helper $invoke_cb to execute.
        String tmpFunc = "__cb_" + proxySerial;
        ScriptManager sm = ScriptManager.getInstance();

        StringBuilder argList = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            if (argList.length() > 0) {
                argList.append(", ");
            }
            String varName = tmpFunc + "_" + i;
            argList.append(varName);

            sm.addVariable(varName, args[i], false);
        }

        sm.addVariable(tmpFunc, interFunc, true);

        ScriptEngine engine = ScriptManager.getInstance().getEngine(ScriptManager.LANG_JAVASCRIPT);
        try {
            // $invoke_cb is defined in JavascriptPreamble.java
            engine.eval(String.format("%s(%s)", tmpFunc, argList.toString()));
        } catch (ScriptException e) {
            e.printStackTrace();
        } finally {
            for (int i = 0; i < args.length; i++) {
                String varName = tmpFunc + "_" + i;
                sm.removeVariable(varName, false);
            }

            ScriptManager.getInstance().removeVariable(tmpFunc, true);
        }
    }

    public static FunctionProxy getFunctionProxy(Object func) {
        if (func instanceof InterpretedFunction) {
            return new FunctionProxy(func);
        } else {
            return null;
        }
    }
}
