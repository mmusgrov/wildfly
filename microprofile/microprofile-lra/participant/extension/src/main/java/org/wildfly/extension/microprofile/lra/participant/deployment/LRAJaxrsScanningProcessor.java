package org.wildfly.extension.microprofile.lra.participant.deployment;

import io.narayana.lra.client.internal.proxy.nonjaxrs.LRAParticipantResource;
import org.jboss.as.jaxrs.deployment.JaxrsAttachments;
import org.jboss.as.jaxrs.deployment.ResteasyDeploymentData;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;

public class LRAJaxrsScanningProcessor implements DeploymentUnitProcessor {
    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        ResteasyDeploymentData attachment = deploymentUnit.getAttachment(JaxrsAttachments.RESTEASY_DEPLOYMENT_DATA);
        attachment.getScannedResourceClasses().add(LRAParticipantResource.class.getName());
    }

    @Override
    public void undeploy(DeploymentUnit context) {

    }
}
