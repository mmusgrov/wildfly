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

package org.jboss.as.txn.subsystem;

import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 * LRA participant, loaded after {@code :probe} and created as model node under {@link LogStoreLRADefinition}.
 */
public class LogStoreLRAParticipantDefinition extends SimpleResourceDefinition {
    static final SimpleAttributeDefinition[] LRA_PARTICIPANT_ATTRIBUTES = new SimpleAttributeDefinition[]{
            LogStoreConstants.JMX_NAME, LogStoreConstants.PARTICIPANT_JNDI_NAME,
            LogStoreConstants.PARTICIPANT_STATUS, LogStoreConstants.RECORD_TYPE,
            LogStoreLRAConstants.LRA_STATUS, LogStoreLRAConstants.COMPENSATOR,
            LogStoreLRAConstants.END_NOTIFICATION_URI, LogStoreLRAConstants.PARTICIPANT_PATH,
            LogStoreLRAConstants.RECOVERY_URI,
    };

    static final LogStoreLRAParticipantDefinition INSTANCE = new LogStoreLRAParticipantDefinition();

    private LogStoreLRAParticipantDefinition() {
        super(new Parameters(TransactionExtension.PARTICIPANT_PATH,
                TransactionExtension.getResourceDescriptionResolver(LogStoreConstants.LOG_STORE, CommonAttributes.LRA, CommonAttributes.PARTICIPANT))
                .setRuntime()
        );
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);

        final LogStoreParticipantRefreshHandler refreshHandler = LogStoreParticipantRefreshHandler.INSTANCE;
        final LogStoreProbeHandler probeHandler = LogStoreProbeHandler.INSTANCE;

        resourceRegistration.registerOperationHandler(
                SimpleOperationDefinitionBuilder.of(LogStoreConstants.REFRESH, getResourceDescriptionResolver()).build(), refreshHandler);
        resourceRegistration.registerOperationHandler(
                SimpleOperationDefinitionBuilder.of(LogStoreConstants.RECOVER, getResourceDescriptionResolver()).build(), new LogStoreParticipantRecoveryHandler(refreshHandler));
        resourceRegistration.registerOperationHandler(
                SimpleOperationDefinitionBuilder.of(LogStoreConstants.DELETE, getResourceDescriptionResolver()).build(), new LogStoreParticipantDeleteHandler(probeHandler));
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        for (final SimpleAttributeDefinition attribute : LRA_PARTICIPANT_ATTRIBUTES) {
            resourceRegistration.registerReadOnlyAttribute(attribute, null);
        }
    }
}
