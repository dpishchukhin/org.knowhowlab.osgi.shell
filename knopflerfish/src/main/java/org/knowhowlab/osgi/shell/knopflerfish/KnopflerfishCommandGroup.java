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

package org.knowhowlab.osgi.shell.knopflerfish;

import org.knopflerfish.service.console.CommandGroup;
import org.knopflerfish.service.console.Session;

import java.io.PrintWriter;
import java.io.Reader;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Knopflerfish CommandGroup service implementation
 *
 * @author dpishchukhin
 */
public class KnopflerfishCommandGroup implements CommandGroup {
    private static final Logger LOG = Logger.getLogger(KnopflerfishCommandGroup.class.getName());

    private String groupId;
    private String groupName;
    private Object service;

    private SortedSet<String> commandHelps = new TreeSet<String>();

    /**
     * Command Group constructor
     *
     * @param groupId   group id
     * @param groupName group name
     * @param service   commands provider service instance
     */
    public KnopflerfishCommandGroup(String groupId, String groupName, Object service) {
        this.groupId = groupId;
        this.groupName = groupName;
        this.service = service;
    }

    public String getGroupName() {
        return groupId;
    }

    public String getShortHelp() {
        return groupName;
    }

    public String getLongHelp() {
        StringBuilder builder = new StringBuilder();
        for (String command : commandHelps) {
            builder.append(command).append('\n');
        }
        return builder.toString();
    }

    public int execute(String[] lineArgs, Reader in, PrintWriter out, Session session) {
        if (lineArgs != null && lineArgs.length >= 1) {
            String commandName = lineArgs[0];
            String[] args;
            // if no additional agruments - do nothing
            if (lineArgs.length <= 1) {
                args = new String[0];
            } else {
                // remove command name from command line arguments
                args = new String[lineArgs.length - 1];
                System.arraycopy(lineArgs, 1, args, 0, args.length);
            }
            try {
                // invoke command method
                Method method = service.getClass().getMethod(commandName, PrintWriter.class, String[].class);
                method.invoke(service, out, args);
                out.flush();
            } catch (NoSuchMethodException e) {
                out.println("No such command: " + commandName);
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Unable to execute command: " + commandName + " with args: " + Arrays.toString(args), e);
                e.printStackTrace(out);
            }
        }
        return 0;
    }

    /**
     * Add command to group
     *
     * @param commandHelp command help
     */
    public void addCommandHelp(String commandHelp) {
        commandHelps.add(commandHelp);
    }

    /**
     * Get commands count in the group
     *
     * @return commands count
     */
    public int getCommandsCount() {
        return commandHelps.size();
    }
}
