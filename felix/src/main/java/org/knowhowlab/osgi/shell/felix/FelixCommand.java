/*
 * Copyright (c) 2010 Dmytro Pishchukhin (http://knowhowlab.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.knowhowlab.osgi.shell.felix;

import org.apache.felix.shell.Command;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Felix Command service implementation
 *
 * @author dmytro.pishchukhin
 */
public class FelixCommand implements Command {
    private static final Logger LOG = Logger.getLogger(FelixCommand.class.getName());

    private String name;
    private String usage;
    private Object service;
    private Method method;

    /**
     * Command constructor
     *
     * @param name    command name
     * @param usage   command usage help
     * @param service commands provider service instance
     * @throws NoSuchMethodException if unable to find method in service that links to command name
     */
    public FelixCommand(String name, String usage, Object service) throws NoSuchMethodException {
        this.name = name;
        this.usage = usage;
        this.service = service;
        method = service.getClass().getMethod(name, PrintWriter.class, String[].class);
    }

    public String getName() {
        return name;
    }

    public String getUsage() {
        return usage;
    }

    public String getShortDescription() {
        return usage;
    }

    public void execute(String line, PrintStream out, PrintStream err) {
        // parse command line
        String[] parsedLine = line.split(" ");
        String[] args;
        // if no additional agruments - do nothing
        if (parsedLine.length <= 1) {
            args = new String[0];
        } else {
            // remove command name from command line arguments
            args = new String[parsedLine.length - 1];
            System.arraycopy(parsedLine, 1, args, 0, args.length);
        }
        try {
            // invoke command method
            PrintWriter writer = new PrintWriter(out, true);
            method.invoke(service, writer, args);
            writer.flush();
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Unable to execute command: " + name + " with args: " + Arrays.toString(args), e);
            // print error to error stream
            e.printStackTrace(err);
        }
    }
}
