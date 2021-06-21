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
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Constants used to list LRA data in the log-store.
 */
class LogStoreLRAConstants {
    static final String LRAS = "lras";

    private static final String lraTypeIdentificationSubstring = "LongRunningAction";

    static final Map<String, String> MODEL_TO_JMX_LRA_NAMES =
            Collections.unmodifiableMap(new HashMap<String, String>() {
                private static final long serialVersionUID = 1L;
            {
                put(LogStoreConstants.JMX_ON_ATTRIBUTE, null);
                put(LogStoreConstants.ID_ATTRIBUTE, LogStoreConstants.ID_JMX_PROPERTY_NAME);
                put("age-in-seconds", "AgeInSeconds");
                put("type", "Type");
                put("lra-id", "LRAId");
                put("lra-client-id", "LRAClientId");
                put("parent-lra-id", "ParentLRAId");
                put("creation-time", "CreationTime");
                put("start-time", "StartTime");
                put("finish-time", "FinishTime");
                put("lra-status", "LRAStatus");
            }});

    static final Map<String, String> MODEL_TO_JMX_LRA_PARTICIPANT_NAMES =
            Collections.unmodifiableMap(new HashMap<String, String>() {
                private static final long serialVersionUID = 1L;
            {
                put(LogStoreConstants.JMX_ON_ATTRIBUTE, null);
                put(LogStoreConstants.ID_ATTRIBUTE, LogStoreConstants.ID_JMX_PROPERTY_NAME);
                put("type", "Type");
                put("status", "Status");
                put("lra-status", "LRAStatus");
                put("compensator", "Compensator");
                put("end-notification-uri", "EndNotificationUri");
                put("participant-path", "ParticipantPath");
                put("recovery-uri", "RecoveryURI");
            }});

    static final String[] LRA_JMX_NAMES = MODEL_TO_JMX_LRA_NAMES.values().toArray(new String[]{});
    static final String[] LRA_PARTICIPANT_JMX_NAMES = MODEL_TO_JMX_LRA_PARTICIPANT_NAMES.values().toArray(new String[]{});

    static boolean isLRAType(String type) {
        return type.contains(lraTypeIdentificationSubstring);
    }

    static SimpleAttributeDefinition LRA_ID = (new SimpleAttributeDefinitionBuilder("lra-id", ModelType.STRING))
            .setAllowExpression(false)
            .setRequired(false)
            .setDefaultValue(new ModelNode())
            .build();
    static SimpleAttributeDefinition LRA_CLIENT_ID = (new SimpleAttributeDefinitionBuilder("lra-client-id", ModelType.STRING))
            .setAllowExpression(false)
            .setRequired(false)
            .setDefaultValue(new ModelNode())
            .build();
    static SimpleAttributeDefinition PARENT_LRA_ID = (new SimpleAttributeDefinitionBuilder("parent-lra-id", ModelType.STRING))
            .setAllowExpression(false)
            .setRequired(false)
            .setDefaultValue(new ModelNode())
            .build();
    static SimpleAttributeDefinition CREATION_TIME = (new SimpleAttributeDefinitionBuilder("creation-time", ModelType.STRING))
            .setAllowExpression(false)
            .setRequired(false)
            .setDefaultValue(new ModelNode())
            .build();
    static SimpleAttributeDefinition START_TIME = (new SimpleAttributeDefinitionBuilder("start-time", ModelType.STRING))
            .setAllowExpression(false)
            .setRequired(false)
            .setDefaultValue(new ModelNode())
            .build();
    static SimpleAttributeDefinition FINISH_TIME = (new SimpleAttributeDefinitionBuilder("finish-time", ModelType.STRING))
            .setAllowExpression(false)
            .setRequired(false)
            .setDefaultValue(new ModelNode())
            .build();
    static SimpleAttributeDefinition LRA_STATUS = (new SimpleAttributeDefinitionBuilder("lra-status", ModelType.STRING))
            .setAllowExpression(false)
            .setRequired(false)
            .setDefaultValue(new ModelNode())
            .build();
    static SimpleAttributeDefinition COMPENSATOR = (new SimpleAttributeDefinitionBuilder("compensator", ModelType.STRING))
            .setAllowExpression(false)
            .setRequired(false)
            .setDefaultValue(new ModelNode())
            .build();
    static SimpleAttributeDefinition END_NOTIFICATION_URI = (new SimpleAttributeDefinitionBuilder("end-notification-uri", ModelType.STRING))
            .setAllowExpression(false)
            .setRequired(false)
            .setDefaultValue(new ModelNode())
            .build();
    static SimpleAttributeDefinition PARTICIPANT_PATH = (new SimpleAttributeDefinitionBuilder("participant-path", ModelType.STRING))
            .setAllowExpression(false)
            .setRequired(false)
            .setDefaultValue(new ModelNode())
            .build();
    static SimpleAttributeDefinition RECOVERY_URI = (new SimpleAttributeDefinitionBuilder("recovery-uri", ModelType.STRING))
            .setAllowExpression(false)
            .setRequired(false)
            .setDefaultValue(new ModelNode())
            .build();
}


