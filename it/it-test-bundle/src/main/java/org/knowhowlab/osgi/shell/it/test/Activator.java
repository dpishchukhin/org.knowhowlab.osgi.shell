package org.knowhowlab.osgi.shell.it.test;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Hashtable;

/**
 * @author dpishchukhin.
 */
public class Activator implements BundleActivator {
    public void start(BundleContext bundleContext) throws Exception {
        bundleContext.registerService(Activator.class.getName(), this, new Hashtable<String, Object>() {{
            put("org.knowhowlab.osgi.shell.group.id", "Test_commands");
            put("org.knowhowlab.osgi.shell.group.name", "Test commands");
            put("org.knowhowlab.osgi.shell.commands", new String[]{
                    "echo#echo - Echo"
            });
        }});
    }

    public void stop(BundleContext bundleContext) throws Exception {

    }

    public void echo(final PrintWriter out, final String... args) {
        out.write(Arrays.toString(args));
    }
}
