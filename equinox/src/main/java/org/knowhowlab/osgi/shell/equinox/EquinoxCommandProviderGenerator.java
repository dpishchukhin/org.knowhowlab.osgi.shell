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

package org.knowhowlab.osgi.shell.equinox;

import javassist.*;
import org.eclipse.osgi.framework.console.CommandInterpreter;

import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * CommandProvider class generator. It uses {@link AbstractEquinoxCommandProvider} as super class and
 * adds public methods for each command name and in method body redirects call to original commands service
 *
 * @author dmytro.pishchukhin
 */
public class EquinoxCommandProviderGenerator {
    private static final Logger LOG = Logger.getLogger(EquinoxCommandProviderGenerator.class.getName());

    /**
     * Classes pool
     */
    private static final ClassPool POOL = ClassPool.getDefault();

    /**
     * Initialization of Classes pool
     */
    static {
        POOL.appendClassPath(new ClassClassPath(AbstractEquinoxCommandProvider.class));
        POOL.appendClassPath(new ClassClassPath(CommandInterpreter.class));
    }

    /**
     * Generate CommandProvider class and instance for this class based on parameters
     *
     * @param service   commands service
     * @param groupName group name
     * @param commands  commands map (name=help)
     * @param suffix    unique class suffix
     * @return generated CommandProvider instance
     *
     * @throws Exception if something went wrong
     */
    public static Object generate(Object service, String groupName, Map<String, String> commands, String suffix) throws Exception {
        // generate class with unique name
        CtClass ctClass = POOL.makeClass(AbstractEquinoxCommandProvider.class.getName() + suffix);
        try {
            Set<String> commandHelps = new HashSet<String>();
            if (!ctClass.isFrozen()) {
                // set superclass
                CtClass abstractCtClass = POOL.getCtClass(AbstractEquinoxCommandProvider.class.getName());
                ctClass.setSuperclass(abstractCtClass);

                // add constructor
                CtClass serviceCtClass = POOL.getCtClass(Object.class.getName());
                CtClass stringCtClass = POOL.getCtClass(String.class.getName());
                CtClass setCtClass = POOL.getCtClass(Set.class.getName());
                CtConstructor ctConstructor = new CtConstructor(new CtClass[]{
                        serviceCtClass, stringCtClass, setCtClass
                }, ctClass);
                ctConstructor.setModifiers(Modifier.PUBLIC);
                ctConstructor.setBody("super($1, $2, $3);");
                ctClass.addConstructor(ctConstructor);

                // add method for each command
                CtClass interpreterCtClass = POOL.getCtClass(CommandInterpreter.class.getName());
                Set<String> names = commands.keySet();
                for (String name : names) {
                    if (isMethodAvailable(service, name)) {
                        CtMethod ctMethod = new CtMethod(CtClass.voidType, "_" + name, new CtClass[]{
                                interpreterCtClass
                        }, ctClass);
                        ctMethod.setModifiers(Modifier.PUBLIC);
                        ctMethod.setBody("runCommand(\"" + name + "\", $1);");
                        ctClass.addMethod(ctMethod);

                        commandHelps.add(commands.get(name));
                    }
                }
            }

            // create new instance
            Class<?> aClass = ctClass.toClass(EquinoxCommandProviderGenerator.class.getClassLoader());
            Constructor<?> constructor = aClass.getConstructor(Object.class, String.class, Set.class);
            return constructor.newInstance(service, groupName, commandHelps);
        } catch (Exception e) {
            ctClass.detach();
            throw e;
        }
    }

    private static boolean isMethodAvailable(Object commandsProvider, String methodName) {
        try {
            commandsProvider.getClass().getMethod(methodName, PrintWriter.class, String[].class);
            return true;
        } catch (NoSuchMethodException e) {
            LOG.log(Level.WARNING, "Unable to find Console Command: " + methodName, e);
            return false;
        }
    }

    /**
     * Detach generated class
     *
     * @param suffix unique class suffix
     */
    public static void clean(String suffix) {
        try {
            CtClass ctClass = POOL.getCtClass(AbstractEquinoxCommandProvider.class.getName() + suffix);
            ctClass.defrost();
            ctClass.detach();
        } catch (NotFoundException e) {
            LOG.log(Level.WARNING, "Unable to clean Console Service. " + e.getMessage(), e);
        }
    }

}
