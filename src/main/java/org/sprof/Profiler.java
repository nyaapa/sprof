package org.sprof;

import java.lang.instrument.Instrumentation;

public class Profiler {
    public static void premain(String argument, Instrumentation instrumentation) {
        Transformer transformer = new Transformer();
        instrumentation.addTransformer(transformer);
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                System.out.println("Shutdown");
                try {
                    CallWatcher.instance.dump();
                } catch ( Exception e ) {
                    System.err.println("Got " + e);
                }
            }
        });
    }
}
