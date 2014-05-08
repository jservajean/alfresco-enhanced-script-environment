package org.nabucco.alfresco.enhScriptEnv.experimental.repo.script;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collections;
import java.util.HashMap;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.SimpleScriptContext;

public class Test
{

    public static class TestCallee
    {
        public void test(final String param)
        {
            System.out.println(param);
        }
    }

    public static class TestObj
    {
        private final String name;

        public TestObj(final String name)
        {
            this.name = name;
        }

        public String getName()
        {
            return this.name;
        }

        public void doStuff(final Object obj)
        {
            final Class<?> cls = obj.getClass();
            System.out.println(cls);
        }
    }

    public static void main(final String[] args) throws Exception
    {
        final ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");

        final Bindings globalBindings = engine.createBindings();
        final ScriptContext myContext = new SimpleScriptContext();
        myContext.setBindings(globalBindings, ScriptContext.ENGINE_SCOPE);

        // re-initialize some of the global functions with scope-access to those globals we need to remove
        InputStream is = Test.class.getResource("resources/alt-engine.js").openStream();
        Reader isReader = new InputStreamReader(is);
        engine.eval(isReader, myContext);

        is = Test.class.getResource("resources/alt-mozilla-compat.js").openStream();
        isReader = new InputStreamReader(is);
        engine.eval(isReader, myContext);

        globalBindings.remove("java");
        globalBindings.remove("Packages");
        globalBindings.remove("load");
        globalBindings.remove("loadWithNewGlobal");
        globalBindings.remove("exit");
        globalBindings.remove("quit");
        globalBindings.remove("JavaAdapter");
        globalBindings.remove("importPackage");
        globalBindings.remove("importClass");

        engine.eval("for each(var prop in this){ print(prop + \" - \" + this[prop]);} print(__FILE__);", myContext);

        globalBindings.put("testVar", new TestCallee());

        engine.eval("testVar.test(\"TestCall\");", myContext);

        globalBindings.put("testObj", new TestObj("TestName"));

        engine.eval("print(testObj.name);", myContext);

        engine.eval("print(testObj.getName());", myContext);

        engine.eval("testObj.doStuff({test: 123});", myContext);

        engine.eval("testObj.doStuff(new Object());", myContext);

        globalBindings.put("map", new HashMap<String, String>(Collections.singletonMap("Test", "Map")));
        is = Test.class.getResource("resources/nashorn-object-to-rhino.js").openStream();
        isReader = new InputStreamReader(is);
        engine.eval("nashornObj = [\"Test\", \"Test2\", { test : map, test2: false, test3: 12.12} ];", myContext);
        engine.eval(isReader, myContext);
        engine.eval("testObj.doStuff(rhinoObj);", myContext);

        final Object result = engine.eval("function main(){ var obj = new Object(); obj.prototype = this; return obj;} main();");
        System.out.println(result instanceof Bindings);
        // engine.eval("importClass(java.util.HashMap);", myContext);

        // engine.eval("print(new HashMap());", myContext);
    }

}
