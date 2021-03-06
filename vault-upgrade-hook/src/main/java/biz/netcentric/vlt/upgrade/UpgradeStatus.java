/*
 * (C) Copyright 2016 Netcentric AG.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package biz.netcentric.vlt.upgrade;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.jackrabbit.vault.packaging.InstallContext;
import org.apache.jackrabbit.vault.packaging.Version;

import biz.netcentric.vlt.upgrade.util.PackageInstallLogger;

/**
 * This class represents a previous upgrade execution and has methods to compare
 * the current execution with the former.
 */
public class UpgradeStatus {

    private static final PackageInstallLogger LOG = PackageInstallLogger.create(UpgradeStatus.class);

    public static final String PN_UPGRADE_TIME = "time";
    public static final String PN_VERSION = "version";
    public static final String PN_ACTIONS = "actions";

    private final Node node;
    private final Version version;

    public UpgradeStatus(InstallContext ctx, String path) throws RepositoryException {
        LOG.debug(ctx, "loading status [{}]", path);
        node = JcrUtils.getOrCreateByPath(path, JcrConstants.NT_UNSTRUCTURED, ctx.getSession());
        version = createVersion(getNode());
        LOG.info(ctx, "loaded status [{}]", this);
    }

    protected static Version createVersion(Node node) throws RepositoryException {
        if (node.hasProperty(PN_VERSION)) {
            return Version.create(node.getProperty(PN_VERSION).getString());
        } else {
            return null;
        }
    }

    public boolean isExecuted(InstallContext ctx, UpgradeInfo info, UpgradeAction action)
            throws RepositoryException {
        Node infoStatus = getInfoStatus(info);
        String actionId = buildActionId(action);
        if (infoStatus.hasProperty(PN_ACTIONS)) {
            for (Value executedAction : infoStatus.getProperty(PN_ACTIONS).getValues()) {
                if (executedAction.getString().equals(actionId)) {
                    LOG.debug(ctx, "action [{}] already exected: [{}]", actionId, infoStatus);
                    return true;
                }
            }
        }
        LOG.debug(ctx, "action [{}] not exected yet: [{}]", actionId, infoStatus);
        return false;
    }

    protected String buildActionId(UpgradeAction action) {
        return action.getName() + "_" + action.getContentHash();
    }

    protected Node getInfoStatus(UpgradeInfo info) throws RepositoryException {
        String packagePath = getNode().getPath() + "/" + info.getNode().getName();
        return JcrUtils.getOrCreateByPath(packagePath, JcrConstants.NT_UNSTRUCTURED, getNode().getSession());
    }

    /**
     * Stores the general status.
     * 
     * @param ctx
     * @throws RepositoryException
     */
    public void update(InstallContext ctx) throws RepositoryException {
        getNode().setProperty(PN_UPGRADE_TIME, Calendar.getInstance());
        String versionString = ctx.getPackage().getId().getVersionString();
        getNode().setProperty(PN_VERSION, versionString);
        LOG.info(ctx, "stored new status [{}]: [{}]", getNode(), versionString);
    }

    /**
     * Stores the info specific status.
     * 
     * @param ctx
     * @param info
     * @param executedActions
     * @throws RepositoryException
     */
    public void update(InstallContext ctx, UpgradeInfo info, List<UpgradeAction> executedActions) throws RepositoryException {
        Node infoStatus = getInfoStatus(info);
        String[] actions = getActionStringArray(infoStatus, executedActions);
        infoStatus.setProperty(PN_ACTIONS, actions);
        LOG.info(ctx, "stored status to [{}] actions: [{}]", infoStatus, actions);
    }

    protected String[] getActionStringArray(Node infoStatus, List<UpgradeAction> executedActions) throws RepositoryException {
        List<String> actions = new ArrayList<>();
        if (infoStatus.hasProperty(PN_ACTIONS)) {
            for (Value action : infoStatus.getProperty(PN_ACTIONS).getValues()) {
                actions.add(action.getString());
            }
        }
        for (UpgradeAction action : executedActions) {
            actions.add(buildActionId(action));
        }
        return actions.toArray(new String[actions.size()]);
    }

    @Override
    public String toString() {
        return super.toString() + " [node=" + getNode() + ", version=" + getVersion() + "]";
    }

    public Node getNode() {
        return node;
    }

    public Version getVersion() {
        return version;
    }

}
