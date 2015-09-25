package org.knowhowlab.osgi.shell.felixgogo;

import org.apache.felix.service.command.CommandSession;

import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Abstract class of Felix GoGo Commands Service
 *
 * @author dpishchukhin
 */
public class AbstractFelixCommandsService {
    private static final Logger LOG = Logger.getLogger(AbstractFelixCommandsService.class.getName());

    private Object service;

    public AbstractFelixCommandsService(Object service) {
        this.service = service;
    }

    /**
     * Run command method
     *
     * @param commandName command name
     * @param params command parameters
     */
    protected void runCommand(String commandName, CommandSession session, String[] params) {
        try {
            Method method = service.getClass().getMethod(commandName, PrintWriter.class, String[].class);
            PrintWriter printWriter = new PrintWriter(session.getConsole());
            method.invoke(service, printWriter, params);
            printWriter.flush();
        } catch (NoSuchMethodException e) {
            session.getConsole().println("No such command: " + commandName);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Unable to execute command: " + commandName + " with args: " + Arrays.toString(params), e);
            e.printStackTrace(session.getConsole());
        }
    }

}
