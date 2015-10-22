# OSGi Shell Adapters (OSGi Universal shell)

Adapts shell commands to all popular OSGi shell runtimes (Equinox, Felix Native, Felix GoGo, Knopflerfish)

[![Build Status](https://travis-ci.org/knowhowlab/org.knowhowlab.osgi.shell.svg?branch=master)](https://travis-ci.org/knowhowlab/org.knowhowlab.osgi.shell)

### Blog with more samples and tutorials

[http://knowhowlab.org](http://knowhowlab.org)

## Usage

### Maven artifacts:

```xml
    <dependency>
        <groupId>org.knowhowlab.osgi.shell</groupId>
        <artifactId>org.knowhowlab.osgi.shell.equinox</artifactId>
        <version>1.3.0</version>
    </dependency>

    <dependency>
        <groupId>org.knowhowlab.osgi.shell</groupId>
        <artifactId>org.knowhowlab.osgi.shell.felix</artifactId>
        <version>1.3.0</version>
    </dependency>

    <dependency>
        <groupId>org.knowhowlab.osgi.shell</groupId>
        <artifactId>org.knowhowlab.osgi.shell.felix-gogo</artifactId>
        <version>1.3.0</version>
    </dependency>

    <dependency>
        <groupId>org.knowhowlab.osgi.shell</groupId>
        <artifactId>org.knowhowlab.osgi.shell.knopflerfish</artifactId>
        <version>1.3.0</version>
    </dependency>
```

### Implement shell command 

    public void <command_name>(java.io.PrintWriter out, java.lang.String[] args);

### Register services with BundleContext

- service property **"org.knowhowlab.osgi.shell.group.id" (mandatory, java.lang.String)** - unique commands group id. Used by some framework specific shell API that support command groups;
- service property **"org.knowhowlab.osgi.shell.group.name" (mandatory, java.lang.String)** - commands group name. Used by some framework specific shell API that support command groups;
- service property **"org.knowhowlab.osgi.shell.commands" (mandatory, java.lang.String)** - commands definition "command_name" -> "command_help";
- service object class - could be used any value. It's very useful when commands are included into interface implementation.

### Register with Declarative services description

- service property **"org.knowhowlab.osgi.shell.group.id" (mandatory, java.lang.String)** - unique commands group id. Used by some framework specific shell API that support command groups;
- service property **"org.knowhowlab.osgi.shell.group.name" (mandatory, java.lang.String)** - commands group name. Used by some framework specific shell API that support command groups;
- service property **"org.knowhowlab.osgi.shell.commands" (mandatory, java.lang.String)** - commands definition "command_name#command_help";
- service object class - could be used any value. It's very useful when commands are included into interface implementation.

## Examples

### Command implementation

```java
    public class ShellTestService {
    ................
        public void bndinfo(PrintWriter out, String... args) {
            if (args == null || args.length != 1) {
                out.println("Bundle id argument is missed");
                return;
            }
            try {
                int bundleId = Integer.parseInt(args[0]);
                Bundle bundle = bc.getBundle(bundleId);
                if (bundle == null) {
                    out.println("Bundle id is invalid: " + bundleId);
                    return;
                }
                printBundleInfo(bundle, out);
            } catch (NumberFormatException e) {
                out.println("Bundle id has wrong format: " + args[0]);
            }
        }
    
        public void bndsinfo(PrintWriter out, String... args) {
            Bundle[] bundles = bc.getBundles();
            for (Bundle bundle : bundles) {
                printBundleInfo(bundle, out);
            }
        }
    ......................
    }
```

### Command service registration

```java    
    ShellTestService shellTestService = ....;
    Dictionary<String, Object> props = new Hashtable<String, Object>();
    props.put("org.knowhowlab.osgi.shell.group.id", "test_group_id");
    props.put("org.knowhowlab.osgi.shell.group.name", "Test commands");
    String[][] commandsArray = new String[2][2];
    commandsArray[0] = new String[]{"bndinfo", "bndinfo <bundleId> - Print information for bundle with <bundleId>"};
    commandsArray[1] = new String[]{"bndsinfo", "bndsinfo - Print information for all bundles"};
    props.put("org.knowhowlab.osgi.shell.commands", commandsArray);
    bc.registerService(ShellTestService.class.getName(), shellTestService, props);
```    
### Command service registration with DS

```xml    
    <?xml version="1.0" encoding="UTF-8"?>
    <component name="shell_test.component">
        <implementation class="...ShellTestService"/>
    
        <service>
            <provide interface="...ShellTestService"/>
        </service>
    
        <property name="org.knowhowlab.osgi.shell.group.id" type="String" value="test_group_id"/>
        <property name="org.knowhowlab.osgi.shell.group.name" type="String" value="Test commands"/>
        <property name="org.knowhowlab.osgi.shell.commands" type="String">
            bndinfo#bndinfo - Print information for bundle with bundleId
            bndsinfo#bndsinfo - Print information for all bundles
        </property>
    ...
    </component>
```
