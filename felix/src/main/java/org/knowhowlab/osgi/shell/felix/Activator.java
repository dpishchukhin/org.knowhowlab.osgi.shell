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
import org.osgi.framework.*;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import java.io.PrintWriter;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Felix shell adapter activator
 *
 * @author dmytro.pishchukhin
 */
public class Activator implements BundleActivator {
    private static final Logger LOG = Logger.getLogger(Activator.class.getName());

    private static final String COMMANDS_DESCRIPTION_PROPERTY = "org.knowhowlab.osgi.shell.commands";
    /**
     * Felix shell API does not support commands grouping. Filter requires only shell commands description
     */
    private static final String SHELL_COMMANDS_SERVICE_FILTER = "(" + COMMANDS_DESCRIPTION_PROPERTY + "=*)";

    /**
     * Bundle Context instance
     */
    private BundleContext bc;
    /**
     * Command provides service tracker
     */
    private ServiceTracker shellCommandsTracker;

    private Map<ServiceReference, List<ServiceRegistration>> commandRegistrations = new HashMap<ServiceReference, List<ServiceRegistration>>();

    public void start(BundleContext context) throws Exception {
        bc = context;
        shellCommandsTracker = new ServiceTracker(bc, bc.createFilter(SHELL_COMMANDS_SERVICE_FILTER),
                new ShellCommandsCustomizer());
        shellCommandsTracker.open();
    }

    public void stop(BundleContext context) throws Exception {
        shellCommandsTracker.close();
        shellCommandsTracker = null;

        bc = null;
    }

    /**
     * Validate Command method
     *
     * @param service     service instance
     * @param commandName command method name
     * @return <code>true</code> if method is peresent in service, <code>public</code> and
     *         has params <code>PrintStream</code> and <code>String[]</code>, otherwise - <code>false</code>
     */
    private boolean isValidCommandMethod(Object service, String commandName) {
        try {
            service.getClass().getMethod(commandName, PrintWriter.class, String[].class);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }


    /**
     * Command provides service tracker customizer
     */
    private class ShellCommandsCustomizer implements ServiceTrackerCustomizer {
        public Object addingService(ServiceReference reference) {
            // get commands description property
            Object commandsDescription = reference.getProperty(COMMANDS_DESCRIPTION_PROPERTY);
            // if property value null or not String[][] - ignore service
            if (commandsDescription == null) {
                LOG.warning(COMMANDS_DESCRIPTION_PROPERTY + " property is null. Ignore service");
                return null;
            } else if (!(commandsDescription instanceof String[][] || commandsDescription instanceof String[])) {
                LOG.warning(COMMANDS_DESCRIPTION_PROPERTY + " property has wrong format: not String[][] or String[]");
                return null;
            } else {
                Object service = bc.getService(reference);
                // get service ranking propety. if not null - use it on Command services registration
                Integer ranking = (Integer) reference.getProperty(Constants.SERVICE_RANKING);
                String[][] commands = parseCommands(commandsDescription);
                List<ServiceRegistration> registrations = new ArrayList<ServiceRegistration>();
                for (String[] commandInfo : commands) {
                    // if command info is invalid - ignore command
                    if (commandInfo != null) {
                        if (commandInfo.length != 2) {
                            LOG.warning(COMMANDS_DESCRIPTION_PROPERTY + " property has wrong format: not String[][]");
                            continue;
                        }
                        String commandName = commandInfo[0];
                        String commandHelp = commandInfo[1];
                        // if command info links to wrong method - ignore command
                        if (isValidCommandMethod(service, commandName)) {
                            Dictionary<String, Object> props = new Hashtable<String, Object>();
                            if (ranking != null) {
                                props.put(Constants.SERVICE_RANKING, ranking);
                            }
                            try {
                                // create command and register it
                                FelixCommand command = new FelixCommand(commandName, commandHelp, service);
                                registrations.add(bc.registerService(Command.class.getName(), command, props));
                            } catch (Exception e) {
                                LOG.log(Level.WARNING, "Unable to initialize command: " + commandName, e);
                            }
                        }
                    }
                }
                if (!registrations.isEmpty()) {
                    commandRegistrations.put(reference, registrations);
                    return service;
                }
                return null;
            }
        }

        private String[][] parseCommands(Object commandsDescription) {
            if (commandsDescription == null) {
                return null;
            } else if (commandsDescription instanceof String[][]) {
                return (String[][]) commandsDescription;
            } else if (commandsDescription instanceof String[]) {
                String[] commands = (String[]) commandsDescription;
                String[][] result = new String[commands.length][];
                for (int i = 0; i < commands.length; i++) {
                    String command = commands[i];
                    result[i] = command.split("#");
                }
                return result;
            } else {
                return null;
            }
        }

        public void modifiedService(ServiceReference reference, Object service) {
            // ignore
        }

        public void removedService(ServiceReference reference, Object service) {
            // unregister all Command services that belong to this service registration
            List<ServiceRegistration> registrations = commandRegistrations.remove(reference);
            if (registrations != null) {
                for (ServiceRegistration registration : registrations) {
                    registration.unregister();
                }
            }
            bc.ungetService(reference);
        }
    }
}
