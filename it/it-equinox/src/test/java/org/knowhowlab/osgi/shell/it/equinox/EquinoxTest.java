package org.knowhowlab.osgi.shell.it.equinox;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import javax.inject.Inject;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.knowhowlab.osgi.testing.assertions.BundleAssert.assertBundleAvailable;
import static org.knowhowlab.osgi.testing.assertions.ServiceAssert.assertServiceAvailable;
import static org.knowhowlab.osgi.testing.assertions.ServiceAssert.assertServiceUnavailable;
import static org.knowhowlab.osgi.testing.utils.BundleUtils.*;
import static org.knowhowlab.osgi.testing.utils.ServiceUtils.getServiceReference;
import static org.ops4j.pax.exam.CoreOptions.*;

/**
 * @author dpishchukhin.
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class EquinoxTest {
    @Inject
    private BundleContext bc;

    @Configuration
    public Option[] config() {
        return options(
                mavenBundle().groupId("org.knowhowlab.osgi")
                        .artifactId("org.knowhowlab.osgi.testing.all")
                        .version("1.3.0"),

                mavenBundle().groupId("org.knowhowlab.osgi.shell")
                        .artifactId("equinox").
                        version(System.getProperty("project.version")).start(),

                mavenBundle().groupId("org.knowhowlab.osgi.shell.it")
                        .artifactId("it-test-bundle").
                        version(System.getProperty("project.version")).noStart(),
                junitBundles()
        );
    }

    @Test
    public void testCommandLineService() throws Exception {
        ServiceReference[] serviceReferences = bc.getServiceReferences("org.eclipse.osgi.framework.console.CommandProvider", null);
        int countOfConsoleServices = serviceReferences.length;

        assertServiceUnavailable("org.knowhowlab.osgi.shell.it.test.Activator");

        assertBundleAvailable("org.knowhowlab.osgi.shell.it.test-bundle");
        startBundleAsync(findBundle(bc, "org.knowhowlab.osgi.shell.it.test-bundle")).get();

        assertServiceAvailable("org.knowhowlab.osgi.shell.it.test.Activator");

        serviceReferences = bc.getServiceReferences("org.eclipse.osgi.framework.console.CommandProvider", null);
        assertThat(serviceReferences.length, is(countOfConsoleServices + 1));

        ServiceReference serviceReference = getServiceReference(bc,
                "org.knowhowlab.osgi.shell.it.test.Activator");

        assertThat((String) serviceReference.getProperty("org.knowhowlab.osgi.shell.group.id"), is("Test_commands"));
        assertThat((String) serviceReference.getProperty("org.knowhowlab.osgi.shell.group.name"), is("Test commands"));
        assertThat(((String[]) serviceReference.getProperty("org.knowhowlab.osgi.shell.commands"))[0], is("echo#echo - Echo"));
    }

    @Test
    public void testCommandLineServiceRestart() throws Exception {
        startBundleAsync(findBundle(bc, "org.knowhowlab.osgi.shell.it.test-bundle")).get();

        assertServiceAvailable("org.knowhowlab.osgi.shell.it.test.Activator");

        assertBundleAvailable("org.knowhowlab.osgi.shell.it.test-bundle");

        stopBundleAsync(findBundle(bc, "org.knowhowlab.osgi.shell.it.test-bundle")).get();

        assertServiceUnavailable("org.knowhowlab.osgi.shell.it.test.Activator");

        startBundleAsync(findBundle(bc, "org.knowhowlab.osgi.shell.it.test-bundle")).get();

        assertServiceAvailable("org.knowhowlab.osgi.shell.it.test.Activator");
    }

    @Test
    public void testShellBundleRestart() throws Exception {
        ServiceReference[] serviceReferences = bc.getServiceReferences("org.eclipse.osgi.framework.console.CommandProvider", null);
        int countOfConsoleServices = serviceReferences.length;

        startBundleAsync(findBundle(bc, "org.knowhowlab.osgi.shell.it.test-bundle")).get();

        assertServiceAvailable("org.knowhowlab.osgi.shell.it.test.Activator");
        serviceReferences = bc.getServiceReferences("org.eclipse.osgi.framework.console.CommandProvider", null);
        assertThat(serviceReferences.length, is(countOfConsoleServices + 1));

        assertBundleAvailable("org.knowhowlab.osgi.shell.it.test-bundle");

        stopBundleAsync(findBundle(bc, "org.knowhowlab.osgi.shell.equinox")).get();

        serviceReferences = bc.getServiceReferences("org.eclipse.osgi.framework.console.CommandProvider", null);
        assertThat(serviceReferences.length, is(countOfConsoleServices));

        startBundleAsync(findBundle(bc, "org.knowhowlab.osgi.shell.equinox")).get();

        serviceReferences = bc.getServiceReferences("org.eclipse.osgi.framework.console.CommandProvider", null);
        assertThat(serviceReferences.length, is(countOfConsoleServices + 1));
    }
}
