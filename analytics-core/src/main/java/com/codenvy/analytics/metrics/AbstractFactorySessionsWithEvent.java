/*
 *
 * CODENVY CONFIDENTIAL
 * ________________
 *
 * [2012] - [2013] Codenvy, S.A.
 * All Rights Reserved.
 * NOTICE: All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any. The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
package com.codenvy.analytics.metrics;

import com.codenvy.analytics.storage.MongoDataLoader;

/** @author <a href="mailto:abazko@codenvy.com">Anatoliy Bazko</a> */
public abstract class AbstractFactorySessionsWithEvent extends AbstractLongValueResulted {

    public AbstractFactorySessionsWithEvent(MetricType metricType) {
        super(metricType);
    }

    public AbstractFactorySessionsWithEvent(String metricName) {
        super(metricName);
    }

    @Override
    public boolean isSupportMultipleTables() {
        return false;
    }

    @Override
    public String getStorageTableBaseName() {
        return MetricType.PRODUCT_USAGE_FACTORY_SESSIONS_LIST.name().toLowerCase() +
               MongoDataLoader.EXT_COLLECTION_NAME_SUFFIX;
    }

    @Override
    public abstract String[] getTrackedFields();
}
