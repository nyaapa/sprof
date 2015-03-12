package org.sprof;

import javassist.*;
import javassist.bytecode.BadBytecode;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.IllegalClassFormatException;
import java.net.URISyntaxException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

public class Profiler {
	static final String[] sprof = {
		"Call$CallPoint",
		"Call",
		"CallWatcher",
		"CallWatcher$1"
	};

    public static void main(String args[]) throws Exception {
		if ( args.length < 1 )
			throw new Exception("Need at least 1 argument");

		ClassPool pool = ClassPool.getDefault();
		pool.insertClassPath(CallWatcher.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());

        for ( int i = 0; i < args.length - 1; i++ )
			pool.insertClassPath(args[i]);

		String target = args[args.length - 1];
		JarFile jarFile = new JarFile(target);
		pool.insertClassPath(target);

		HashMap<String, byte[]> transformedClasses = new HashMap<>();

		for ( String className : sprof )
			transformedClasses.put("org/sprof/" + className + ".class", pool.getCtClass("org.sprof."  + className).toBytecode());

		for (Enumeration<JarEntry> classes = jarFile.entries(); classes.hasMoreElements(); ) {
			JarEntry je = classes.nextElement();
			if( je.isDirectory() || !je.getName().endsWith(".class") )
				continue;
			String className = je.getName();
			String normalizedName = className.replace(".class", "").replaceAll("/", ".");
			transformedClasses.put(
				je.getName(),
				Transformer.transform(normalizedName, pool)
			);
		}

		replaceJarFile(target, transformedClasses);
    }

	static private void replaceJarFile(String jarPathAndName, HashMap<String, byte[]> replaces) throws IOException,URISyntaxException {
		File jarFile = new File(jarPathAndName);
		File tempJarFile = new File(jarPathAndName + ".tmp");
		JarFile jar = new JarFile(jarFile);

		try {
			JarOutputStream tempJar = new JarOutputStream(new FileOutputStream(tempJarFile));

			byte[] buffer = new byte[1024];
			int bytesRead;

			try {
				InputStream entryStream = null;
				for (Enumeration<JarEntry> entries = jar.entries(); entries.hasMoreElements(); ) {
					JarEntry entry = entries.nextElement();
					if ( replaces.containsKey(entry.getName()) ) {
						tempJar.putNextEntry(new JarEntry(entry.getName()));
						tempJar.write(replaces.get(entry.getName()));
						replaces.remove(entry.getName());
					} else {
						entryStream = jar.getInputStream(entry);
						tempJar.putNextEntry(entry);
						while ((bytesRead = entryStream.read(buffer)) != -1) {
							tempJar.write(buffer, 0, bytesRead);
						}
						entryStream.close();
					}
				}
				for ( Map.Entry<String, byte[]> add : replaces.entrySet() ) {
					tempJar.putNextEntry(new JarEntry(add.getKey()));
					tempJar.write(add.getValue());
				}
			} catch (Exception ex) {
				System.err.println(ex);
				tempJar.putNextEntry(new JarEntry("stub"));
				throw ex;
			} finally {
				tempJar.close();
			}
		} finally {
			jar.close();
		}

		if ( jarFile.delete() ) {
			tempJarFile.renameTo(jarFile);
			System.out.println(jarPathAndName + " updated.");
		} else
			System.err.println("Could Not Delete JAR File");
	}
}
