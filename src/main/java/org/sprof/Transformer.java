package org.sprof;

import javassist.*;
import javassist.bytecode.BadBytecode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Transformer {

    private static final Set<String> excludes = getExcludes();
    private static final List<String> excludeMasks = getExcludeMasks();

	static public byte[] transform(String className, ClassPool classPool) throws IllegalClassFormatException, NotFoundException, CannotCompileException, BadBytecode, IOException {
        if (!isExclude(className)) {
            try {
                return transformClass(className, classPool);
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("fail [" + className + "]");
				throw e;
            }
        }

        return classPool.get(className).toBytecode();
    }

	static private byte[] transformClass(String className, ClassPool classPool) throws NotFoundException, CannotCompileException, BadBytecode, IOException {
        CtClass ctClass = classPool.get(className);
        if (ctClass.isAnnotation()) {
            return classPool.get(className).toBytecode();
        } else if (ctClass.isInterface()) {
            return classPool.get(className).toBytecode();
        } else
        if ( ctClass.isFrozen() )
            ctClass.defrost();
        try {
            CtMethod[] methods = ctClass.getDeclaredMethods();
            for (CtMethod ctMethod : methods) {
                if ((ctMethod.getModifiers() & Modifier.ABSTRACT) > 0) {
                    continue;
                } else if ((ctMethod.getModifiers() & Modifier.NATIVE) > 0) {
                    continue;
                }
                String methodId = CallWatcher.getMethodName(ctMethod);
                ctMethod.insertBefore("org.sprof.CallWatcher.instance.push(\"" + methodId + "\", java.lang.System.nanoTime());");
                ctMethod.insertAfter("org.sprof.CallWatcher.instance.pop(\"" + methodId + "\", java.lang.System.nanoTime());", true);
                ctMethod.getMethodInfo().rebuildStackMap(classPool);
            }
            return ctClass.toBytecode();
        } catch ( Exception e ) {
			System.err.println("Can't transform " + className + ": " + e);
			return ctClass.toBytecode();
		} finally {
            ctClass.detach();
        }
    }

	static private boolean isExclude(String className) {
        if (excludes.contains(className)) {
            return true;
        }
        for (String excludeMask : excludeMasks) {
            if (className.startsWith(excludeMask)) {
                return true;
            }
        }
        return false;
    }

	static private Set<String> getExcludes() {
        Set<String> excludes = new HashSet<>();
        InputStream stream = null;
        BufferedReader reader = null;
        try {
            // from https://github.com/mitallast/simple-java-profiler.git
            stream = Transformer.class.getResourceAsStream("/org/sprof/excludes");
            reader = new BufferedReader(new InputStreamReader(stream));
            String className;
            while ((className = reader.readLine()) != null) {
                excludes.add(className);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return excludes;
    }

	static private List<String> getExcludeMasks() {
        List<String> excludes = new ArrayList<>();
        InputStream stream = null;
        BufferedReader reader = null;
        try {
            // from https://github.com/mitallast/simple-java-profiler.git
            stream = Transformer.class.getResourceAsStream("/org/sprof/exclude_masks");
            reader = new BufferedReader(new InputStreamReader(stream));
            String className;
            while ((className = reader.readLine()) != null) {
                excludes.add(className);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return excludes;
    }
}
