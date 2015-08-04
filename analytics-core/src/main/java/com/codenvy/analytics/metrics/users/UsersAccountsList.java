/*
 * CODENVY CONFIDENTIAL
 * __________________
 *
 *  [2012] - [2015] Codenvy, S.A.
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
package com.codenvy.analytics.metrics.users;

import com.codenvy.analytics.metrics.AbstractListValueResulted;
import com.codenvy.analytics.metrics.MetricType;

import javax.annotation.security.RolesAllowed;

/**
 * @author Alexander Reshetnyak
 */
@RolesAllowed({"system/admin", "system/manager"})
public class UsersAccountsList extends AbstractListValueResulted {

    public UsersAccountsList() {
        super(MetricType.USERS_ACCOUNTS_LIST);
    }

    /** {@inheritDoc} */
    @Override
    public String getDescription() {
        return "Users' roles in accounts";
    }

    /** {@inheritDoc} */
    @Override
    public String getStorageCollectionName() {
        return getStorageCollectionName(MetricType.USERS_ACCOUNTS);
    }

    /** {@inheritDoc} */
    @Override
    public String[] getTrackedFields() {
        return new String[]{ID,
                            USER,
                            ACCOUNT,
                            ROLES,
                            REMOVED,
                            REMOVED_DATE};
    }
}