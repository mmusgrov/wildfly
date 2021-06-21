/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.txn.subsystem;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.txn.logging.TransactionLogger;
import org.jboss.as.txn.service.ObjStoreBrowserService;
import org.jboss.dmr.ModelNode;

import javax.management.AttributeList;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.JMException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Handler for exposing transaction logs
 *
 * @author <a href="stefano.maestri@redhat.com">Stefano Maestri</a> (c) 2011 Red Hat Inc.
 * @author <a href="mmusgrove@redhat.com">Mike Musgrove</a> (c) 2012 Red Hat Inc.
 */
public class LogStoreProbeHandler implements OperationStepHandler {

    static final LogStoreProbeHandler INSTANCE = new LogStoreProbeHandler();

    private Map<String, String> getMBeanValues(MBeanServerConnection cnx, ObjectName on, String ... attributeNames)
            throws InstanceNotFoundException, IOException, ReflectionException, IntrospectionException {

        if (attributeNames == null) {
            MBeanInfo info = cnx.getMBeanInfo( on );
            MBeanAttributeInfo[] attributeArray = info.getAttributes();
            int i = 0;
            attributeNames = new String[attributeArray.length];

            for (MBeanAttributeInfo ai : attributeArray)
                attributeNames[i++] = ai.getName();
        }

        AttributeList attributes = cnx.getAttributes(on, attributeNames);
        Map<String, String> values = new HashMap<>();

        for (javax.management.Attribute attribute : attributes.asList()) {
            Object value = attribute.getValue();

            values.put(attribute.getName(), value == null ? "" : value.toString());
        }

        return values;
    }

    private void addAttributes(ModelNode node, Map<String, String> model2JmxNames, Map<String, String> attributes) {
        for (Map.Entry<String, String> e : model2JmxNames.entrySet()) {
            String attributeValue = attributes.get(e.getValue());

            if (attributeValue != null)
                node.get(e.getKey()).set(attributeValue);
        }
    }

    private void addParticipants(final JmxDataResolver resolver, final Resource parent, Set<ObjectInstance> participants, MBeanServer mbs)
            throws IntrospectionException, InstanceNotFoundException, IOException, ReflectionException {
        int i = 1;

        for (ObjectInstance participant : participants) {
            final Resource resource = new LogStoreResource.LogStoreRuntimeResource(participant.getObjectName());
            final ModelNode model = resource.getModel();
            Map<String, String> pAttributes = getMBeanValues(mbs,  participant.getObjectName(),
                    resolver.getParticipantJmxNames());
            String pAddress = pAttributes.get(resolver.getParticipantJMXPropertyForAddress());

            if (pAddress == null || pAddress.length() == 0) {
                pAttributes.put(resolver.getParticipantJMXPropertyForAddress(), String.valueOf(i++));
                pAddress = pAttributes.get(resolver.getParticipantJMXPropertyForAddress());
            }

            addAttributes(model, resolver.getParticipantJmxMapper(), pAttributes);

            final PathElement element = PathElement.pathElement(LogStoreConstants.PARTICIPANTS, pAddress);
            parent.registerChild(element, resource);
        }
    }

    private void addObjectStoreRecords(final Resource parent, Set<ObjectInstance> jmxLogRecords, MBeanServer mbs)
            throws IntrospectionException, InstanceNotFoundException, IOException,
            ReflectionException, MalformedObjectNameException {

        for (ObjectInstance oi : jmxLogRecords) {
            String jmxCanonicalName = oi.getObjectName().getCanonicalName();

            if (!jmxCanonicalName.contains("puid") && jmxCanonicalName.contains("itype")) {
                final Resource logStoreRecord = new LogStoreResource.LogStoreRuntimeResource(oi.getObjectName());
                final ModelNode model = logStoreRecord.getModel();

                JmxDataResolver resolver = new JmxDataResolver(jmxCanonicalName);

                Map<String, String> recordAttributes = getMBeanValues(
                        mbs,  oi.getObjectName(), resolver.getJmxNames());
                String logRecordId = recordAttributes.get(resolver.getJMXPropertyForAddress());
                addAttributes(model, resolver.getJmxMapper(), recordAttributes);

                String participantQuery =  jmxCanonicalName + ",puid=*";
                Set<ObjectInstance> participants = mbs.queryMBeans(new ObjectName(participantQuery), null);
                addParticipants(resolver, logStoreRecord, participants, mbs);

                final PathElement element = PathElement.pathElement(resolver.getRootPath(), logRecordId);
                parent.registerChild(element, logStoreRecord);
            }
        }
    }

    private Resource probeObjectStoreRecords(MBeanServer mbs, boolean exposeAllLogs)
            throws OperationFailedException {
        try {
            ObjectName on = new ObjectName(ObjStoreBrowserService.OBJ_STORE_BROWSER_BEAN_NAME);

            mbs.setAttribute(on, new javax.management.Attribute("ExposeAllRecordsAsMBeans", exposeAllLogs));
            mbs.invoke(on, "probe", null, null);

            Set<ObjectInstance> objBrowserRecords = mbs.queryMBeans(new ObjectName(ObjStoreBrowserService.OBJ_STORE_BROWSER_BEAN_NAME +  ",*"), null);

            final Resource resource = Resource.Factory.create();
            addObjectStoreRecords(resource, objBrowserRecords, mbs);
            return resource;

        } catch (JMException | IOException e) {
            throw TransactionLogger.ROOT_LOGGER.transactionDiscoveryError(e);
        }
    }

    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        if(! context.isNormalServer()) {
            context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
            return;
        }
        final MBeanServer mbs = TransactionExtension.getMBeanServer(context);
        if (mbs != null) {
            // Get the log-store resource
            final Resource resource = context.readResource(PathAddress.EMPTY_ADDRESS);
            assert resource instanceof LogStoreResource;
            final LogStoreResource logStore = (LogStoreResource) resource;
            // Get the expose-all-logs parameter value
            final ModelNode subModel = context.readResource(PathAddress.EMPTY_ADDRESS).getModel();
            final boolean exposeAllLogs = LogStoreConstants.EXPOSE_ALL_LOGS.resolveModelAttribute(context, subModel).asBoolean();
            final Resource storeModel = probeObjectStoreRecords(mbs, exposeAllLogs);
            // Replace the current model with an updated one
            context.acquireControllerLock();
            // WFLY-3020 -- don't drop the root model
            storeModel.writeModel(logStore.getModel());
            logStore.update(storeModel);
        }
        context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
    }

    /**
     * Mapping class that resolves the runtime log-store content data.
     * In current time we have following types to be listed via cli operations
     * <ul>
     *     <li>JTA/JTS transactions: <code>/subsystems=transactions/log-store=log-store/transactions=uid/participants=id</code></li>
     *     <li>LRA records: <code>/subsystems=transactions/log-store=log-store/lras=uid/participants=id</code></li>
     * </ul>
     */
    private static class JmxDataResolver {
        private final boolean isLRA;

        JmxDataResolver(String canonicalName) {
            isLRA = LogStoreLRAConstants.isLRAType(canonicalName);
        }
        String getJMXPropertyForAddress() {
            return LogStoreConstants.ID_JMX_PROPERTY_NAME;
        }
        String getParticipantJMXPropertyForAddress() {
            return isLRA ? LogStoreConstants.ID_JMX_PROPERTY_NAME : LogStoreConstants.JNDI_JXM_PROPERTY_NAME;
        }
        String[] getJmxNames() {
            return isLRA ? LogStoreLRAConstants.LRA_JMX_NAMES : LogStoreConstants.TXN_JMX_NAMES;
        }
        Map<String,String> getJmxMapper() {
            return isLRA ? LogStoreLRAConstants.MODEL_TO_JMX_LRA_NAMES : LogStoreConstants.MODEL_TO_JMX_TXN_NAMES;
        }
        String[] getParticipantJmxNames() {
            return isLRA ? LogStoreLRAConstants.LRA_PARTICIPANT_JMX_NAMES : LogStoreConstants.PARTICIPANT_JMX_NAMES;
        }
        Map<String,String> getParticipantJmxMapper() {
            return isLRA ? LogStoreLRAConstants.MODEL_TO_JMX_LRA_PARTICIPANT_NAMES : LogStoreConstants.MODEL_TO_JMX_PARTICIPANT_NAMES;
        }
        String getRootPath() {
            return isLRA ? LogStoreLRAConstants.LRAS : LogStoreConstants.TRANSACTIONS;
        }
    }

}
