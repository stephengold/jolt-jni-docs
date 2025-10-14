/*
 Copyright (c) 2025 Stephen Gold and Yanis Boudiaf

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:

 1. Redistributions of source code must retain the above copyright notice, this
    list of conditions and the following disclaimer.

 2. Redistributions in binary form must reproduce the above copyright notice,
    this list of conditions and the following disclaimer in the documentation
    and/or other materials provided with the distribution.

 3. Neither the name of the copyright holder nor the names of its
    contributors may be used to endorse or promote products derived from
    this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.github.stephengold.jsr223;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 * A console application to run the specified script using the specified JSR-223
 * script engine, after importing all Java classes named in specified text
 * files.
 *
 * @author Stephen Gold sgold@sonic.net
 */
final public class RunScript {
    // *************************************************************************
    // fields

    /**
     * engine for evaluating scripts
     */
    private static ScriptEngine scriptEngine;
    /**
     * short name for the script-engine factory
     */
    private static String factoryName;
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private RunScript() {
        // do nothing
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Main entry point for the RunScript application.
     *
     * @param arguments array of command-line arguments (not null)
     */
    public static void main(String[] arguments) {
        // Parse the command-line arguments:
        int numArgs = arguments.length;
        if (numArgs < 2) {
            System.err.println(
                    "Usage:  RunScript <engine> <script> [ <classList> ... ]");

            System.out.println();
            printFactories();

            System.exit(0);
        }

        factoryName = arguments[0];
        String scriptFilePath = arguments[1];
        String[] classListPaths = Arrays.copyOfRange(arguments, 2, numArgs);

        // Acquire a script engine via the Java Scripting API (aka JSR 223):
        ScriptEngineManager manager = new ScriptEngineManager();
        scriptEngine = manager.getEngineByName(factoryName);
        if (scriptEngine == null) {
            System.err.println("Script-engine factory not found:  \""
                    + factoryName + "\"");

            System.out.println();
            printFactories();

            System.exit(0);
        }

        for (String classListFilePath : classListPaths) {
            importClassesFromFile(classListFilePath);
        }

        evaluateScriptFromFile(scriptFilePath);
    }
    // *************************************************************************
    // private methods

    /**
     * Read and evaluate the script in the specified file.
     *
     * @param scriptFilePath path to the script file (not null)
     */
    private static void evaluateScriptFromFile(String scriptFilePath) {
        // Create a reader for the script file:
        FileReader scriptReader = null;
        try {
            scriptReader = new FileReader(scriptFilePath);
        } catch (FileNotFoundException exception) {
            System.err.println(
                    "Script file not found:  \"" + scriptFilePath + "\"");
            System.exit(1);
        }

        // Evaluate the script:
        try {
            scriptEngine.eval(scriptReader);
        } catch (ScriptException exception) {
            exception.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Convert the first character of the specified text to upper case.
     *
     * @param input the input text to convert (not null)
     * @return the converted text (not null)
     */
    private static String firstToUpper(String input) {
        String result = input;
        if (!input.isEmpty()) {
            String first = input.substring(0, 1);
            first = first.toUpperCase(Locale.ROOT);
            String rest = input.substring(1);
            result = first + rest;
        }

        return result;
    }

    /**
     * Import the specified class into the script engine.
     *
     * @param fullName the full name of the class to import (not null)
     */
    private static void importClass(String fullName) {
        int lastDotPos = fullName.lastIndexOf('.');
        assert lastDotPos >= 0 : lastDotPos;
        String packageName = fullName.substring(0, lastDotPos);
        String simpleName = fullName.substring(lastDotPos + 1);

        String codeSnippet;
        switch (factoryName) {
            case "jruby":
                if (simpleName.equals("Mutex")) { // conflicting constant
                    return;
                }
                codeSnippet = String.format(
                        "java_import Java::%s", firstToUpper(fullName));
                break;

            case "jython":
                codeSnippet = String.format(
                        "from %s import %s", packageName, simpleName);
                break;

            case "lua54":
                return;

            case "luaj":
                codeSnippet = String.format(
                        "%s = luajava.bindClass(\"%s\")", simpleName, fullName);
                break;

            case "nashorn":
                codeSnippet = String.format(
                        "var %s = Java.type('%s');", simpleName, fullName);
                break;

            default:
                throw new IllegalStateException("factoryName = " + factoryName);
        }

        //System.out.println(codeSnippet);

        try {
            scriptEngine.eval(codeSnippet);
        } catch (ScriptException exception) {
            exception.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Read a list of Java classes and import each one into the script engine.
     *
     * @param classListFilePath the name of the input file
     */
    private static void importClassesFromFile(String classListFilePath) {
        // Create an input stream for the class list:
        InputStream inStream;
        try {
            inStream = new FileInputStream(classListFilePath);
        } catch (FileNotFoundException exception) {
            System.err.println("Class list file not found:  \""
                    + classListFilePath + "\"");
            System.exit(1);
            return;
        }

        // Read the list and import each class into the engine:
        try (Scanner scanner = new Scanner(inStream, StandardCharsets.UTF_8)) {
            while (scanner.hasNextLine()) {
                String fullName = scanner.nextLine();
                importClass(fullName);
            }
        }
    }

    /**
     * Print a description of each available script-engine factory.
     */
    private static void printFactories() {
        ScriptEngineManager manager = new ScriptEngineManager();
        List<ScriptEngineFactory> factories = manager.getEngineFactories();

        int numFactories = factories.size();
        System.out.println(
                "Number of script-engine factories found:  " + numFactories);

        for (int i = 0; i < numFactories; ++i) {
            ScriptEngineFactory factory = factories.get(i);
            System.out.printf("  Factory#%d%n", i + 1);

            String engineName = factory.getEngineName();
            String engineVersion = factory.getEngineVersion();

            System.out.printf("    engine:  \"%s\"   version \"%s\"%n",
                    engineName, engineVersion);

            List<String> aliases = factory.getNames();
            System.out.printf("    aliases:  %s%n", aliases);

            String lanuage = factory.getLanguageName();
            String languageVersion = factory.getLanguageVersion();
            System.out.printf("    language:  \"%s\"   version \"%s\"%n",
                    lanuage, languageVersion);
        }
    }
}
