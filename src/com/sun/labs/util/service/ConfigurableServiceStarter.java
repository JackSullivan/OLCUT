/*
 * Copyright 2008-2009 Sun Microsystems, Inc. All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER
 * 
 * This code is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * only, as published by the Free Software Foundation.
 * 
 * This code is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License version 2 for more details (a copy is
 * included in the LICENSE file that accompanied this code).
 * 
 * You should have received a copy of the GNU General Public License
 * version 2 along with this work; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA
 * 
 * Please contact Sun Microsystems, Inc., 16 Network Circle, Menlo
 * Park, CA 94025 or visit www.sun.com if you need additional
 * information or have any questions.
 */

package com.sun.labs.util.service;

import com.sun.labs.util.LabsLogFormatter;
import com.sun.labs.util.props.ConfigComponentList;
import com.sun.labs.util.props.Configurable;
import com.sun.labs.util.props.ConfigurationManager;
import com.sun.labs.util.props.PropertyException;
import com.sun.labs.util.props.PropertySheet;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A class that will start and stop a configurable service.  The configuration for
 * this class will point to a configuration that can be used to create 
 * and start the service.
 */
public class ConfigurableServiceStarter implements Configurable {

    private static final Logger logger = Logger.getLogger(ConfigurableServiceStarter.class.getName());

    private List<ConfigurableService> services;

    private List<Thread> serviceThreads;

    private ConfigurationManager cm;

    /**
     * A configuration property for the services that we will be starting and
     * stopping.
     */
    @ConfigComponentList(type = com.sun.labs.util.service.ConfigurableService.class)
    public static final String PROP_SERVICE_COMPONENTS = "serviceComponents";

    private void waitForServices() {
        for (Thread serviceThread : serviceThreads) {
            try {
                serviceThread.join();
            } catch (InterruptedException ex) {

            }
        }
    }

    public void stopServices() {
        for (ConfigurableService service : services) {
            service.stop();
        }
    }

    @Override
    public void newProperties(PropertySheet ps) throws PropertyException {

        cm = ps.getConfigurationManager();

        //
        // Get the names of the components we're to start, then start them.
        services = (List<ConfigurableService>) ps.getComponentList(PROP_SERVICE_COMPONENTS);
        serviceThreads = new ArrayList<Thread>();
        for (ConfigurableService service : services) {
            service.setStarter(this);
            Thread st = new Thread(service);
            st.setDaemon(true);
            st.start();
            serviceThreads.add(st);
        }
    }

    public static void usage() {
        System.err.println(
                "Usage: com.sun.labs.aura.ConfigurableServiceStarter <config> <component name> [<file handler pattern>]");
        System.err.println(
                "  Some useful global properties are auraHome and auraDistDir");
        System.err.println("  auraHome defaults to /aura.");
        System.err.println(
                "  auraDistDir defaults to the current working directory");
    }

    /**
     * A main program to read the configuration for the service starter and
     * start the service.
     *
     * @param args
     */
    public static void main(String[] args) {
        if (args.length < 2) {
            usage();
            return;
        }

        Logger rl = Logger.getLogger("");
        if (args.length > 2) {

            //
            // If a file handler pattern was specified, then use that to startup,
            // removing all of the other handlers.
            try {
                FileHandler fh
                        = new FileHandler(args[2], 30000000, 5, true);
                for (Handler h : rl.getHandlers()) {
                    rl.removeHandler(h);
                }
                rl.addHandler(fh);
            } catch (IOException ex) {
                System.err.format("Error opening log file handler: " + ex);
                usage();
                return;
            } catch (SecurityException ex) {
                System.err.format("Error opening log file handler: " + ex);
                usage();
                return;
            }
        }

        //
        // Use the labs format logging.
        for (Handler h : rl.getHandlers()) {
            h.setLevel(Level.ALL);
            h.setFormatter(new LabsLogFormatter());
            try {
                h.setEncoding("utf-8");
            } catch (Exception ex) {
                rl.severe("Error setting output encoding");
            }
        }

        final ConfigurableServiceStarter starter;
        String configFile = args[0];
        try {
            //
            // See if we can get a resource for the configuration file first.
            // This is mostly a convenience.
            URL cu = ConfigurableServiceStarter.class.getResource(configFile);
            if (cu == null) {
                cu = (new File(configFile)).toURI().toURL();
            }
            final ConfigurationManager cm = new ConfigurationManager(cu);
            starter = (ConfigurableServiceStarter) cm.lookup(args[1]);

            if (starter == null) {
                System.err.println("Unknown starter: " + args[1]);
                return;
            }

            //
            // Add a shutdown hook to stop the services.
            Runtime.getRuntime().addShutdownHook(new Thread() {

                @Override
                public void run() {
                    starter.stopServices();
                    cm.shutdown();
                }
            });

            starter.waitForServices();
        } catch (IOException ex) { 
            logger.log(Level.SEVERE, "Error parsing configuration file: " + configFile, ex);
        } catch (PropertyException ex) {
            logger.log(Level.SEVERE, "Error parsing configuration file: " + configFile, ex);
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Other error", ex);
            usage();
        }
    }
}
