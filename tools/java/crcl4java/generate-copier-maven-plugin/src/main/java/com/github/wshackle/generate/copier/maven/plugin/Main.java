/*
 * This software is public domain software, however it is preferred
 * that the following disclaimers be attached.
 * Software Copywrite/Warranty Disclaimer
 * 
 * This software was developed at the National Institute of Standards and
 * Technology by employees of the Federal Government in the course of their
 * official duties. Pursuant to title 17 Section 105 of the United States
 * Code this software is not subject to copyright protection and is in the
 * public domain.
 * 
 * This software is experimental. NIST assumes no responsibility whatsoever 
 * for its use by other parties, and makes no guarantees, expressed or 
 * implied, about its quality, reliability, or any other characteristic. 
 * We would appreciate acknowledgement if the software is used. 
 * This software can be redistributed and/or modified freely provided 
 * that any derivative works bear some notice that they are derived from it, 
 * and any modified versions bear some notice that they have been modified.
 * 
 *  See http://www.copyright.gov/title17/92chap1.html#105
 * 
 */
package com.github.wshackle.generate.copier.maven.plugin;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class Main {

    public static boolean verbose = false;

    public static boolean main_completed = false;
    private static final int DEFAULT_LIMIT = 200;

    public static String getCurrentDir() throws IOException {
        String userDirProp = System.getProperty("user.dir");
        if (verbose) {
            logString("userDirProp = " + userDirProp);
        }
        String currentDir = new File(userDirProp).getCanonicalPath();
        if (verbose) {
            logString("currentDir = " + currentDir);
        }
        return currentDir.replace("\\", "\\\\");
    }

    public static String getHomeDir() throws IOException {
        String userHomeProp = System.getProperty("user.home");
        if (verbose) {
            logString("userDirProp = " + userHomeProp);
        }
        String homeDir = new File(userHomeProp).getCanonicalPath();
        if (verbose) {
            logString("homeDir = " + homeDir);
        }
        return homeDir;
    }

    private static final Set<String> badNames = getBadNames();

    private static Set<String> getBadNames() {
        Set<String> badNamesSet = new TreeSet<>();
        badNamesSet.addAll(Arrays.asList("and", "and_eq", "bitand",
                "bitor", "compl", "not", "not_eq", "or",
                "not_eq", "or", "or_eq", "xor", "xor_eq",
                "delete", "namespace", "union", "cast"));
        return badNamesSet;
    }

    public static boolean isBadName(String nameToCheck) {
        return badNames.contains(nameToCheck);
    }

    public static boolean isAddableClass(Class<?> clss, Set<String> excludedClassNames) {
        if (clss.isArray()
                || clss.isSynthetic()
                || clss.isAnnotation()
                || clss.isPrimitive()) {
            return false;
        }
//        if(clss.getCanonicalName().contains("Dialog") || clss.getName().contains("ModalExlusionType")) {
//            if(verbose)  logString("clss = " + clss);
//        }
//        if (clss.getEnclosingClass() != null) {
//            return false;
//        }
        String canonicalName = null;
        try {
            canonicalName = clss.getCanonicalName();
        } catch (Throwable t) {
            // leaving canonicalName null is enough
        }
        if (null == canonicalName) {
            return false;
        }
        if (excludedClassNames.contains(canonicalName)) {
            return false;
        }
        if (excludedClassNames.contains(clss.getName())) {
            return false;
        }
        if (canonicalName.indexOf('$') >= 0) {
            return false;
        }
        String pkgNames[] = canonicalName.split("\\.");
        for (int i = 0; i < pkgNames.length; i++) {
            String pkgName = pkgNames[i];
            if (badNames.contains(pkgName)) {
                return false;
            }
        }
        Method ma[] = null;
        try {
            ma = clss.getDeclaredMethods();
        } catch (Throwable t) {
            // leaving canonicalName null is enough
        }
        if (null == ma) {
            return false;
        }
        return !excludedClassNames.contains(clss.getName());
    }

    public static @Nullable Log log = null;

    private static final StringBuilder logStringBuilder = new StringBuilder();

    private static void logString(String s) {
        System.out.println(s);
        logStringBuilder.append(s).append("\n");
        if (null != log) {
            log.info(s);
        }
    }

    private static void findFilesFromDir(File dir, List<File> files) {
        File fa[] = dir.listFiles();
        if (null != fa) {
            for (int i = 0; i < fa.length; i++) {
                File file = fa[i];
                if (file.isDirectory()) {
                    findFilesFromDir(file, files);
                } else {
                    files.add(file);
                }
            }
        }
    }

    private static class LocalParams {

        @Nullable String output = null;
        @Nullable String header = null;
        @Nullable String jar = null;
        @Nullable Set<String> classnamesToFind = null;
        @Nullable Set<String> packageprefixes = null;
        @Nullable String loadlibname = null;
        @Nullable String javacloner = null;

//        Map<String, String> nativesNameMap = null;
//        Map<String, Class> nativesClassMap = null;
final int limit = DEFAULT_LIMIT;
        int classes_per_file = 10;

        String limitstring = Integer.toString(limit);
        URL extraclassurls[] = new URL[0];
        String extraclassnames[] = new String[0];
        String excludedclassnames[] = new String[0];
        String nocopyclassnames[] = new String[0];
        final Set<String> nocopyclassnamesSet = new TreeSet<>();
        String namespace = "javaforcpp";
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException, MojoExecutionException {
        main_completed = false;

        Options options = setupOptions();
        LocalParams localParams = new LocalParams();
        try {
            // parse the command line arguments
            logString("args = " + Arrays.toString(args));
            CommandLine line = new DefaultParser().parse(options, args);
            localParams.loadlibname = line.getOptionValue("loadlibname");
            localParams.javacloner = line.getOptionValue("javacloner");
            verbose = line.hasOption("verbose");
            if (line.hasOption("extraclassurls")) {
                String extraclassurlstrings[] = line.getOptionValues("extraclassurls");
                logString("extraclassurlstrings = " + Arrays.toString(extraclassurlstrings));
                localParams.extraclassurls = new URL[extraclassurlstrings.length];
                for (int i = 0; i < extraclassurlstrings.length; i++) {
                    String extraclassurlstring = extraclassurlstrings[i];
                    localParams.extraclassurls[i] = new URL(extraclassurlstring);
                }
                logString("extraclassurls = " + Arrays.toString(localParams.extraclassurls));
            }

            if (line.hasOption("extraclassnames")) {
                localParams.extraclassnames = line.getOptionValues("extraclassnames");
                logString("extraclassnames = " + Arrays.toString(localParams.extraclassnames));
            }

            if (line.hasOption("excludedclassnames")) {
                localParams.excludedclassnames = line.getOptionValues("excludedclassnames");
                logString("excludedclassnames = " + Arrays.toString(localParams.excludedclassnames));
            }

            if (line.hasOption("nocopyclassnames")) {
                localParams.nocopyclassnames = line.getOptionValues("nocopyclassnames");
                logString("nocopyclassnames = " + Arrays.toString(localParams.nocopyclassnames));
                for (int i = 0; i < localParams.nocopyclassnames.length; i++) {
                    String nocopyclassname = localParams.nocopyclassnames[i];
                    localParams.nocopyclassnamesSet.add(nocopyclassname);
                }
            }

            final String initJarString = line.getOptionValue("jar");
            if (verbose) {
                logString("initJarString = " + initJarString);
            }
            if (null != initJarString) {
                if (initJarString.startsWith("~/")) {
                    localParams.jar = new File(new File(getHomeDir()), initJarString.substring(2)).getCanonicalPath();
                } else if (initJarString.startsWith("./")) {
                    localParams.jar = new File(new File(getCurrentDir()), initJarString.substring(2)).getCanonicalPath();
                } else if (initJarString.startsWith("../")) {
                    localParams.jar = new File(new File(getCurrentDir()).getParentFile(), initJarString.substring(3)).getCanonicalPath();
                } else {
                    localParams.jar = new File(initJarString).getCanonicalPath();
                }
            }
            if (verbose) {
                logString("localParams.jar = " + localParams.jar);
            }
            if (line.hasOption("classes")) {
                String classStrings[] = line.getOptionValues("classes");
                if (verbose) {
                    logString("classStrings = " + Arrays.toString(classStrings));
                }
                localParams.classnamesToFind = new TreeSet<>(Arrays.asList(classStrings));
                if (verbose) {
                    logString("classnamesToFind = " + localParams.classnamesToFind);
                }
            }

            localParams.output = line.getOptionValue("output");
            if (verbose) {
                logString("output = " + localParams.output);
            }

            if (line.hasOption("packages")) {
                final String[] packages = line.getOptionValues("packages");
                localParams.packageprefixes = new TreeSet<>(Arrays.asList(packages));
            }

            if (line.hasOption("help")) {
                printHelpAndExit(options, args);
            }
        } catch (ParseException exp) {
            if (verbose) {
                logString("Unexpected exception:" + exp.getMessage());
            }
            printHelpAndExit(options, args);
        }

        List<Class<?>> classesList = new ArrayList<>();
//        Set<Class<?>> excludedClasses = new TreeSet<>();
        Set<String> excludedClassNames = new TreeSet<>();
        for (int i = 0; i < localParams.excludedclassnames.length; i++) {
            excludedClassNames.add(localParams.excludedclassnames[i]);
        }

        Set<String> foundClassNames = new TreeSet<>();
        excludedClassNames.add(Object.class.getName());
        excludedClassNames.add(String.class.getName());
        excludedClassNames.add(void.class.getName());
        excludedClassNames.add(Void.class.getName());
        excludedClassNames.add(Class.class.getName());
        excludedClassNames.add(Enum.class.getName());
        Set<String> packagesSet = new TreeSet<>();
        List<URL> urlsList = new ArrayList<>();
        String cp = System.getProperty("java.class.path");
        if (verbose) {
            logString("System.getProperty(\"java.class.path\") = " + cp);
        }
        if (null != cp) {
            for (String cpe : cp.split(File.pathSeparator)) {
                if (verbose) {
                    logString("class path element = " + cpe);
                }
                File f = new File(cpe);
                if (f.isDirectory()) {
                    urlsList.add(new URL("file:" + f.getCanonicalPath() + File.separator));
                } else if (cpe.endsWith(".jar")) {
                    urlsList.add(new URL("jar:file:" + f.getCanonicalPath() + "!/"));
                }
            }
        }
        cp = System.getenv("CLASSPATH");
        if (verbose) {
            logString("System.getenv(\"CLASSPATH\") = " + cp);
        }
        if (null != cp) {
            for (String cpe : cp.split(File.pathSeparator)) {
                if (verbose) {
                    logString("class path element = " + cpe);
                }
                File f = new File(cpe);
                if (f.isDirectory()) {
                    urlsList.add(new URL("file:" + f.getCanonicalPath() + File.separator));
                } else if (cpe.endsWith(".jar")) {
                    urlsList.add(new URL("jar:file:" + f.getCanonicalPath() + "!/"));
                }
            }
        }
        if (verbose) {
            logString("urlsList = " + urlsList);
        }

        Path jarPath = getPath(localParams.jar);
        if (jarPath != null) {
            File jarFile = jarPath.toFile();
            final URL jarUrl;
            if (!jarFile.isDirectory()) {
                jarUrl = new URL("jar:file:" + jarPath.toFile().getCanonicalPath() + "!/");
            } else {
                jarUrl = new URL("file:" + jarPath.toFile().getCanonicalPath() + "/");
            }
            urlsList.add(jarUrl);
        }
        urlsList.add(new URL("file://" + System.getProperty("user.dir") + "/"));
        for (int i = 0; i < localParams.extraclassurls.length; i++) {
            urlsList.add(localParams.extraclassurls[i]);
        }
        URL[] urls = urlsList.toArray(new URL[urlsList.size()]);
        if (verbose) {
            logString("urls = " + Arrays.toString(urls));
        }
        URLClassLoader cl = URLClassLoader.newInstance(urls);
        if (null != jarPath) {
            final File jarFile = jarPath.toFile();
            if (jarFile.isDirectory()) {
                List<File> filesList = new ArrayList<>();
                findFilesFromDir(jarFile, filesList);
                for (File clssFile : filesList) {
                    final String name = clssFile.getName();
                    if (!name.endsWith(".class")) {
                        continue;
                    }
                    String relPath = clssFile.getCanonicalPath().substring(jarFile.getCanonicalPath().length());
                    if (relPath.startsWith("/") || relPath.startsWith("\\")) {
                        relPath = relPath.substring(1);
                    }
                    System.out.println("relPath = " + relPath);
                    String className = relPath
                            .replace('/', '.')
                            .replace('\\', '.')
                            .substring(0, relPath.length() - ".class".length());
                    System.out.println("className = " + className);
                    Class<?> clss = cl.loadClass(className);
                    System.out.println("clss = " + clss);
                    try {
                        addClass(cl, className, localParams, packagesSet, classesList, excludedClassNames, foundClassNames);
                    } catch (ClassNotFoundException | NoClassDefFoundError ex) {
                        System.err.println("Caught " + ex.getClass().getName() + ":" + ex.getMessage() + " for className=" + className + ", jarPath=" + jarPath);
                    }
                    System.out.println("classesList = " + classesList);
                }
            } else {
                if (!jarFile.exists()) {
                    System.out.println("jarFile = " + jarFile + " does not exist.");
                    System.out.println("System.getProperty(\"user.dir\") = " + System.getProperty("user.dir"));
                    File jarFileParent = jarFile.getParentFile();
                    System.out.println("jarFileParent = " + jarFileParent);
                    if (null != jarFileParent) {
                        if (jarFileParent.exists()) {
                            System.out.println("jarFileParent.listFiles() = " + Arrays.toString(jarFileParent.listFiles()));
                        } else {
                            File jarFileGrandParent = jarFileParent.getParentFile();
                            System.out.println("jarFileGrandParent = " + jarFileGrandParent);
                            if (null != jarFileGrandParent && jarFileGrandParent.exists()) {
                                System.out.println("jarFileGrandParent.listFiles() = " + Arrays.toString(jarFileGrandParent.listFiles()));
                            }
                        }
                    }
                }

                if (!jarFile.exists()) {
                    System.out.println("jarPath.toFile().exists() = " + jarFile.exists());
                    System.out.println("FileSystems.getDefault() = " + FileSystems.getDefault());
                }
                ZipInputStream zip = new ZipInputStream(Files.newInputStream(jarPath, StandardOpenOption.READ));
                for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                    // This ZipEntry represents a class. Now, what class does it represent?
                    String entryName = entry.getName();
                    if (verbose) {
                        logString("entryName = " + entryName);
                    }

                    if (!entry.isDirectory() && entryName.endsWith(".class")) {

                        if (entryName.indexOf('$') >= 0) {
                            continue;
                        }
                        String classFileName = entry.getName()
                                .replace('/', '.')
                                .replace('\\', '.');
                        String className = classFileName
                                .substring(0, classFileName.length() - ".class".length());
                        if (localParams.classnamesToFind != null
                                && localParams.classnamesToFind.size() > 0
                                && !localParams.classnamesToFind.contains(className)) {
                            if (verbose) {
                                logString("skipping className=" + className + " because it does not found in=" + localParams.classnamesToFind);
                            }
                            continue;
                        }
                        try {
                            addClass(cl, className, localParams, packagesSet, classesList, excludedClassNames, foundClassNames);
                        } catch (ClassNotFoundException | NoClassDefFoundError ex) {
                            System.err.println("Caught " + ex.getClass().getName() + ":" + ex.getMessage() + " for className=" + className + ", entryName=" + entryName + ", jarPath=" + jarPath);
                        }
                    }
                }
            }
        }
        Comparator<Class<?>> classNameComparator = new Comparator<Class<?>>() {
            @Override
            public int compare(Class o1, Class o2) {
                if (o1 == o2) {
                    return 0;
                } else if (o1 == null) {
                    return -1;
                } else if (o2 == null) {
                    return 1;
                } else {
                    return o1.getName().compareTo(o2.getName());
                }
            }
        };
        Collections.sort(classesList, classNameComparator);

        if (null != localParams.classnamesToFind) {
            for (String classname : localParams.classnamesToFind) {
                if (verbose) {
                    logString("classname = " + classname);
                }
                if (foundClassNames.contains(classname)) {
                    if (verbose) {
                        logString("foundClassNames.contains(" + classname + ")");
                    }
                    continue;
                }
                try {
                    if (classesList.contains(Class.forName(classname))) {
                        if (verbose) {
                            logString("Classes list already contains:  " + classname);
                        }
                        continue;
                    }
                } catch (Exception ignored) {

                }

                if (null != classname && classname.length() > 0) {

                    Class<?> c = null;
                    try {
                        c = cl.loadClass(classname);
                    } catch (ClassNotFoundException e) {
                        System.err.println("Class " + classname + " not found ");
                    }
                    if (verbose) {
                        logString("c = " + c);
                    }
                    if (null == c) {
                        try {
                            c = ClassLoader.getSystemClassLoader().loadClass(classname);
                        } catch (ClassNotFoundException e) {
                            if (verbose) {
                                logString("System ClassLoader failed to find " + classname);
                            }
                        }
                    }
                    if (null != c) {
                        if (!classesList.contains(c)) {
                            classesList.add(c);
                        }
                    } else {
                        System.err.println("Class " + classname + " not found");
                    }
                }
            }
            if (verbose) {
                logString("Finished checking classnames arguments");
            }
        }
        if (verbose) {
            logString("Classes found = " + classesList.size());
        }
        List<Class<?>> newClasses = new ArrayList<Class<?>>();
        logString("Before adding extras : classesList.size() = " + classesList.size());
        if (localParams.extraclassnames.length > 0) {
            for (int i = 0; i < localParams.extraclassnames.length; i++) {
                String extraclassname = localParams.extraclassnames[i];
                Class<?> clzz = cl.loadClass(extraclassname);
                if (!classesList.contains(clzz)) {
                    checkClass(clzz);
                    classesList.add(clzz);
                }
            }
        }
        logString("Before adding supers : classesList.size() = " + classesList.size());

        for (Class<?> clss : classesList) {
            Class<?> superClass = clss.getSuperclass();
            while (null != superClass
                    && !classesList.contains(superClass)
                    && !newClasses.contains(superClass)
                    && isAddableClass(superClass, excludedClassNames)) {
                checkClass(superClass);
                newClasses.add(superClass);
                superClass = superClass.getSuperclass();
            }
            try {
                Field fa[] = clss.getDeclaredFields();
                for (Field f : fa) {
                    if (Modifier.isPublic(f.getModifiers())) {
                        Class<?> fClass = f.getType();
                        if (!classesList.contains(fClass)
                                && !newClasses.contains(fClass)
                                && isAddableClass(fClass, excludedClassNames)) {
                            checkClass(fClass);
                            newClasses.add(fClass);
                        }
                    }
                }
            } catch (NoClassDefFoundError e) {
                e.printStackTrace();
            }
            final Method[] classDeclaredMethods = clss.getDeclaredMethods();
//            Arrays.sort(classDeclaredMethods, Comparator.comparing((Method m) -> m.getName()));
            for (Method m : classDeclaredMethods) {
                if (m.isSynthetic()) {
                    continue;
                }
                if (!Modifier.isPublic(m.getModifiers())
                        || Modifier.isAbstract(m.getModifiers())) {
                    continue;
                }
                Class<?> retType = m.getReturnType();
                if (verbose) {
                    logString("Checking dependancies for Method = " + m);
                }
                if (!classesList.contains(retType)
                        && !newClasses.contains(retType)
                        && isAddableClass(retType, excludedClassNames)) {
                    if (retType.getName().contains("JAXB")) {
                        System.out.println("excludedClassNames = ");
                        throw new RuntimeException("retType=" + retType + ", m=" + m + ", clss=" + clss);
                    }
                    checkClass(retType);
                    newClasses.add(retType);
                    if (verbose) {
                        logString("Added retType = " + retType);
                    }
                    superClass = retType.getSuperclass();
                    while (null != superClass
                            && !classesList.contains(superClass)
                            && !newClasses.contains(superClass)
                            && isAddableClass(superClass, excludedClassNames)) {
                        checkClass(superClass);
                        newClasses.add(superClass);
                        if (verbose) {
                            logString("Added retType.getSuperclass() = " + superClass);
                        }
                        superClass = superClass.getSuperclass();
                    }
                }
                for (Class<?> paramType : m.getParameterTypes()) {
                    if (!classesList.contains(paramType)
                            && !newClasses.contains(paramType)
                            && isAddableClass(paramType, excludedClassNames)) {
                        checkClass(paramType);
                        newClasses.add(paramType);
                        if (verbose) {
                            logString("Added paramType = " + superClass);
                        }
                        superClass = paramType.getSuperclass();
                        while (null != superClass
                                && !classesList.contains(superClass)
                                && !newClasses.contains(superClass)
                                && !excludedClassNames.contains(superClass.getName())) {
                            if (verbose) {
                                logString("Added paramType.superClass = " + superClass);
                            }
                            checkClass(superClass);
                            newClasses.add(superClass);
                            superClass = superClass.getSuperclass();
                        }
                    }
                }
            }
        }
//        if (null != nativesClassMap) {
//            for (Class clss : nativesClassMap.values()) {
//                if (null != clss) {
//                    Class superClass = clss.getSuperclass();
//                    while (null != superClass
//                            && !classesList.contains(superClass)
//                            && !newClasses.contains(superClass)
//                            && !excludedClasses.contains(superClass)) {
//                        newClasses.add(superClass);
//                        superClass = superClass.getSuperclass();
//                    }
//                }
//            }
//        }
        if (verbose) {
            logString("Dependency classes needed = " + newClasses.size());
            logString("newClasses = " + newClasses);
        }
        classesList.addAll(newClasses);
        List<Class<?>> newOrderClasses = new ArrayList<>();
        for (Class<?> clss : classesList) {
            if (newOrderClasses.contains(clss)) {
                continue;
            }
            Class<?> superClass = clss.getSuperclass();
            Stack<Class<?>> stack = new Stack<>();
            while (null != superClass
                    && !newOrderClasses.contains(superClass)
                    && !superClass.equals(java.lang.Object.class)) {
                stack.push(superClass);
                superClass = superClass.getSuperclass();
            }
            while (!stack.empty()) {
                final Class<?> poppedClass = stack.pop();
                if (!newOrderClasses.contains(poppedClass)) {
                    newOrderClasses.add(poppedClass);
                }
            }
            if (!newOrderClasses.contains(clss)) {
                newOrderClasses.add(clss);
            }
        }
        classesList = newOrderClasses;
        if (verbose) {
            logString("Total number of classes = " + classesList.size());
            logString("classes = " + classesList);
        }
        if (null != localParams.javacloner && localParams.javacloner.length() > 1) {
            JavaCloneUtilGenerator generator = new JavaCloneUtilGenerator();
            File dir;
            if (null != localParams.output && localParams.output.length() > 0) {
                File outFile = new File(localParams.output);
                if (outFile.isDirectory()) {
                    dir = outFile;
                } else {
                    File outFileParent = outFile.getParentFile();
                    if (null != outFileParent) {
                        dir = outFileParent;
                    } else {
                        throw new MojoExecutionException("output directory not given: localParams.output=" + localParams.output);
                    }
                }
            } else {
                throw new MojoExecutionException("output directory not given: localParams.output=" + localParams.output);
            }
            JavaCloneUtilOptions javaCloneUtilOptions = new JavaCloneUtilOptions();
            javaCloneUtilOptions.classname = localParams.javacloner;
            javaCloneUtilOptions.dir = dir;
            javaCloneUtilOptions.classes = classesList;
            javaCloneUtilOptions.nocopyclassnamesSet = localParams.nocopyclassnamesSet;
            javaCloneUtilOptions.logString = logStringBuilder.toString();
            logString("javaCloneUtilOptions = " + javaCloneUtilOptions);
            generator.generateCloneUtil(javaCloneUtilOptions);
        }
        main_completed = true;
    }

    @SuppressWarnings("nullness")
    private static @Nullable
    Path getPath(@Nullable String fname) {
        if (null != fname && fname.length() > 0) {
            return FileSystems.getDefault().getPath(fname);
        } else {
            return null;
        }
    }

    private static void addClass(URLClassLoader cl, String className, LocalParams localParams, Set<String> packagesSet, List<Class<?>> classesList, Set<String> excludedClassNames, Set<String> foundClassNames) throws ClassNotFoundException {
        Class<?> clss = cl.loadClass(className);
        final Set<String> packageprefixes = localParams.packageprefixes;
        if (packageprefixes != null
                && packageprefixes.size() > 0) {
            if (null == clss.getPackage()) {
                return;
            }
            final String pkgName = clss.getPackage().getName();
            boolean matchFound = false;
            for (String prefix : packageprefixes) {
                if (pkgName.startsWith(prefix)) {
                    matchFound = true;
                    break;
                }
            }
            if (!matchFound) {
                return;
            }
        }
        Package p = clss.getPackage();
        if (null != p) {
            packagesSet.add(p.getName());
        }
        if (!classesList.contains(clss)
                && isAddableClass(clss, excludedClassNames)) {
            final Set<String> classnamesToFind = localParams.classnamesToFind;
            if (null != classnamesToFind
                    && classnamesToFind.contains(className)
                    && !foundClassNames.contains(className)) {
                foundClassNames.add(className);
                if (verbose) {
                    logString("foundClassNames = " + foundClassNames);
                }
            }
            classesList.add(clss);
        }
    }

    private static void checkClass(Class<?> clzz) throws RuntimeException {
        if (clzz.getName().contains("JAXB")) {
            throw new RuntimeException("clzz=" + clzz);
        }
    }

    private static Options setupOptions() {
        Options options = new Options();
        options.addOption(Option.builder("?")
                .desc("Print this message")
                .longOpt("help")
                .build());
        options.addOption(Option.builder("n")
                .hasArg()
                .desc("C++ namespace for newly generated classes.")
                .longOpt("namespace")
                .build());
        options.addOption(Option.builder("c")
                .hasArgs()
                .desc("Single Java class to extract.")
                .longOpt("classes")
                .build());
        options.addOption(Option.builder("p")
                .hasArgs()
                .desc("Java Package prefix to extract")
                .longOpt("packages")
                .build());
        options.addOption(Option.builder("o")
                .hasArg()
                .desc("Output C++ source file.")
                .longOpt("output")
                .build());
        options.addOption(Option.builder("j")
                .hasArg()
                .desc("Input jar file")
                .longOpt("jar")
                .build());
        options.addOption(Option.builder("h")
                .hasArg()
                .desc("Output C++ header file.")
                .longOpt("header")
                .build());
        options.addOption(Option.builder("l")
                .hasArg()
                .desc("Maximum limit on classes to extract from jars.[default=200]")
                .longOpt("limit")
                .build());
        options.addOption(Option.builder("v")
                .desc("enable verbose output")
                .longOpt("verbose")
                .build());
        options.addOption(Option.builder()
                .hasArg()
                .desc("Classes per output file.[default=10]")
                .longOpt("classes-per-output")
                .build());
        options.addOption(Option.builder()
                .hasArgs()
                .desc("Comma seperated list of nativeclass=javaclass native where nativeclass will be generated as an extension/implementation of the java class.")
                .longOpt("natives")
                .build());
        options.addOption(Option.builder()
                .hasArg()
                .desc("library name for System.loadLibrary(...) for native extension classes")
                .longOpt("loadlibname")
                .build());
        options.addOption(Option.builder()
                .hasArg()
                .desc("Extra class urls.")
                .longOpt("extraclassurls")
                .build());
        options.addOption(Option.builder()
                .hasArg()
                .desc("Extra class names.")
                .longOpt("extraclassnames")
                .build());
        options.addOption(Option.builder()
                .hasArg()
                .desc("Classes excluded from analysis.")
                .longOpt("excludedclassnames")
                .build());
        options.addOption(Option.builder()
                .hasArg()
                .desc("No copy class names.")
                .longOpt("nocopyclassnames")
                .build());
        options.addOption(Option.builder()
                .hasArg()
                .desc("Generate a utility java class for cloning.")
                .longOpt("javacloner")
                .build());
        return options;
    }

    private static void printHelpAndExit(Options options, String args[]) {
        logString("args = " + Arrays.toString(args));
        new HelpFormatter().printHelp("java4cpp", options);
        System.exit(1);
    }
}
