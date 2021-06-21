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

package org.wildfly.test.integration.microprofile.lra;

import java.util.List;

import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.shared.CLIServerSetupTask;
import org.jboss.dmr.ModelNode;

public class EnableLRAExtensionsSetupTask extends CLIServerSetupTask {
    private static final String MODULE_LRA_PARTICIPANT = "org.wildfly.extension.microprofile.lra-participant";
    private static final String MODULE_LRA_COORDINATOR = "org.wildfly.extension.microprofile.lra-coordinator";
    private static final String SUBSYSTEM_LRA_PARTICIPANT = "microprofile-lra-participant";
    private static final String SUBSYSTEM_LRA_COORDINATOR = "microprofile-lra-coordinator";

    public EnableLRAExtensionsSetupTask() {
    }

    @Override
    public void setup(ManagementClient managementClient, String containerId) throws Exception {
        boolean lcExt = !containsChild(managementClient, "extension", MODULE_LRA_COORDINATOR);
        boolean lcSs = !containsChild(managementClient, "extension", SUBSYSTEM_LRA_COORDINATOR);
        boolean lpExt = !containsChild(managementClient, "extension", MODULE_LRA_PARTICIPANT);
        boolean lpSs = !containsChild(managementClient, "extension", SUBSYSTEM_LRA_PARTICIPANT);

        NodeBuilder nodeBuilder = this.builder.node(containerId);
        if (lpExt) {
            nodeBuilder.setup("/extension=%s:add", MODULE_LRA_PARTICIPANT);
        }
        if (lpSs) {
            nodeBuilder.setup("/subsystem=%s:add", SUBSYSTEM_LRA_PARTICIPANT);
        }
        if (lpSs) {
            nodeBuilder.teardown("/subsystem=%s:remove", SUBSYSTEM_LRA_PARTICIPANT);
        }
        if (lpExt) {
            nodeBuilder.teardown("/extension=%s:remove", MODULE_LRA_PARTICIPANT);
        }

        if (lcExt) {
            nodeBuilder.setup("/extension=%s:add", MODULE_LRA_COORDINATOR);
        }
        if (lcSs) {
            nodeBuilder.setup("/subsystem=%s:add", SUBSYSTEM_LRA_COORDINATOR);
        }
        if (lcSs) {
            nodeBuilder.teardown("/subsystem=%s:remove", SUBSYSTEM_LRA_COORDINATOR);
        }
        if (lcExt) {
            nodeBuilder.teardown("/extension=%s:remove", MODULE_LRA_COORDINATOR);
        }
        super.setup(managementClient, containerId);
    }

    private boolean containsChild(ManagementClient managementClient, String childType, String childName) throws Exception {
        ModelNode op = new ModelNode();
        op.get("operation").set("read-children-names");
        op.get("child-type").set(childType);
        ModelNode result = managementClient.getControllerClient().execute(op);
        if (!result.get("outcome").asString().equals("success")) {
            throw new IllegalStateException(result.asString());
        }
        List<ModelNode> names = result.get("result").asList();
        for (ModelNode name : names) {
            if (name.asString().equals(childName)) {
                return true;
            }
        }
        return false;
    }
}
