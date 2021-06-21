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

package org.wildfly.extension.microprofile.lra.participant;

import static org.jboss.as.controller.OperationContext.Stage.RUNTIME;
import static org.jboss.as.server.deployment.Phase.DEPENDENCIES;
import static org.jboss.as.server.deployment.Phase.STRUCTURE;
import static org.wildfly.extension.microprofile.lra.participant.MicroProfileLRAParticipantExtension.SUBSYSTEM_NAME;
import static org.wildfly.extension.microprofile.lra.participant.MicroProfileLRAParticipantExtension.SUBSYSTEM_PATH;

import java.util.Collection;
import java.util.Arrays;

import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.jaxrs.JaxrsExtension;
import org.jboss.as.jaxrs.deployment.JaxrsScanningProcessor;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.extension.microprofile.lra.participant._private.MicroProfileLRAParticipantLogger;
import org.wildfly.extension.microprofile.lra.participant.deployment.LRAJaxrsScanningProcessor;
import org.wildfly.extension.microprofile.lra.participant.deployment.LRAParticipantDeploymentDependencyProcessor;
import org.wildfly.extension.microprofile.lra.participant.deployment.LRAParticipantDeploymentSetupProcessor;

public class MicroProfileLRAParticipantSubsystemDefinition  extends PersistentResourceDefinition {

    private static final String LRA_PARTICIPANT_CAPABILITY_NAME = "org.wildfly.microprofile.lra.participant";

    private static final RuntimeCapability<Void> LRA_PARTICIPANT_CAPABILITY = RuntimeCapability.Builder
            .of(LRA_PARTICIPANT_CAPABILITY_NAME)
            .addRequirements(LRA_PARTICIPANT_CAPABILITY_NAME)
            .build();

    protected static final SimpleAttributeDefinition URL =
            new SimpleAttributeDefinitionBuilder(CommonAttributes.URL, ModelType.STRING, true)
                    .setRequired(false)
                    .setDefaultValue(new ModelNode(CommonAttributes.DEFAULT_URL))
                    .setAllowExpression(true)
                    .setXmlName(CommonAttributes.URL)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .build();

    public MicroProfileLRAParticipantSubsystemDefinition() {
        super(
                new SimpleResourceDefinition.Parameters(
                        SUBSYSTEM_PATH,
                        MicroProfileLRAParticipantExtension.getResourceDescriptionResolver(SUBSYSTEM_NAME))
                        .setAddHandler(AddHandler.INSTANCE)
                        .setRemoveHandler(new ReloadRequiredRemoveStepHandler())
                        .setCapabilities(LRA_PARTICIPANT_CAPABILITY)
        );
    }

    static final AttributeDefinition[] ATTRIBUTES = {URL};

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerReadWriteAttribute(URL, null, new ReloadRequiredWriteAttributeHandler(URL));
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Arrays.asList(ATTRIBUTES);
    }

    static class AddHandler extends AbstractBoottimeAddStepHandler {

        static AddHandler INSTANCE = new AddHandler();

        private AddHandler() {
            super(Arrays.asList(ATTRIBUTES));
        }

        @Override
        protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
            URL.validateAndSet(operation, model);
        }

        @Override
        protected void performBoottime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
            super.performBoottime(context, operation, model);
            final String url = URL.resolveModelAttribute(context, model).asString();
            System.setProperty("lra.coordinator.url", url);

            context.addStep(new AbstractDeploymentChainStep() {
                public void execute(DeploymentProcessorTarget processorTarget) {

                    // org.jboss.as.server.deployment.Phase.java, it defines the order within the phase (https://issues.redhat.com/browse/WFCORE-5559)
                    final int STRUCTURE_MICROPROFILE_LRA_PARTICIPANT = 0x2400;
                    final int DEPENDENCIES_MICROPROFILE_LRA_PARTICIPANT = 0x18D0;

                    processorTarget.addDeploymentProcessor(SUBSYSTEM_NAME, STRUCTURE, STRUCTURE_MICROPROFILE_LRA_PARTICIPANT, new LRAParticipantDeploymentSetupProcessor());
                    processorTarget.addDeploymentProcessor(SUBSYSTEM_NAME, DEPENDENCIES, DEPENDENCIES_MICROPROFILE_LRA_PARTICIPANT, new LRAParticipantDeploymentDependencyProcessor());
                    processorTarget.addDeploymentProcessor(SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_JAXRS_SCANNING, new LRAJaxrsScanningProcessor());
                }
            }, RUNTIME);

            MicroProfileLRAParticipantLogger.LOGGER.activatingSubsystem(url);
        }
    }
}
