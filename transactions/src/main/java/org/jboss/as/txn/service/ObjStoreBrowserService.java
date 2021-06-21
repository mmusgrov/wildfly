/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.txn.service;

import com.arjuna.ats.arjuna.common.arjPropertyManager;
import com.arjuna.ats.arjuna.tools.osb.mbean.ObjStoreBrowser;
import org.jboss.as.txn.logging.TransactionLogger;
import org.jboss.msc.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

import java.util.function.Consumer;

public class ObjStoreBrowserService implements Service {
    public static final String OBJ_STORE_BROWSER_BEAN_NAME = "narayana.logStore:type=ObjectStore";

    private volatile ObjStoreBrowser objStoreBrowser;
    private final Consumer<ObjStoreBrowser> objStoreBrowserConsumer;
    private final boolean jts;

    public ObjStoreBrowserService(final Consumer<ObjStoreBrowser> consumer, final boolean jts) {
        this.jts = jts;
        this.objStoreBrowserConsumer = consumer;
    }

    @Override
    public synchronized void start(final StartContext context) throws StartException {
        arjPropertyManager.getObjectStoreEnvironmentBean()
                .setJmxToolingMBeanName(OBJ_STORE_BROWSER_BEAN_NAME);
        objStoreBrowser = new ObjStoreBrowser();
        objStoreBrowserConsumer.accept(objStoreBrowser);

        try {
            objStoreBrowser.start();
        } catch (Exception e) {
            throw TransactionLogger.ROOT_LOGGER.objectStoreStartFailure(e);
        }

        // need to be configured after 'start()' method, redefining the type to beans linking
        objStoreBrowser.addType("StateManager/BasicAction/TwoPhaseCoordinator/AtomicAction",
                "com.arjuna.ats.arjuna.AtomicAction",
                "com.arjuna.ats.internal.jta.tools.osb.mbean.jta.JTAActionBean");
    }

    @Override
    public synchronized void stop(final StopContext context) {
        objStoreBrowser.stop();
    }

    public ObjStoreBrowser getObjectStoreBrowser() {
        return this.objStoreBrowser;
    }
}
