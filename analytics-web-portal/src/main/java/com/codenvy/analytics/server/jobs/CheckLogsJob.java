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
package com.codenvy.analytics.server.jobs;

import com.codenvy.analytics.metrics.MetricParameter;
import com.codenvy.analytics.metrics.TimeUnit;
import com.codenvy.analytics.metrics.Utils;
import com.codenvy.analytics.metrics.value.ListListStringValueData;
import com.codenvy.analytics.metrics.value.ListStringValueData;
import com.codenvy.analytics.metrics.value.ValueData;
import com.codenvy.analytics.scripts.ScriptType;
import com.codenvy.analytics.scripts.executor.ScriptExecutor;
import com.codenvy.analytics.server.service.MailService;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

/** @author <a href="mailto:abazko@codenvy.com">Anatoliy Bazko</a> */
public class CheckLogsJob implements Job, ForceableRunJob {

    private static final Logger LOGGER                        = LoggerFactory.getLogger(CheckLogsJob.class);
    private static final String CHECKLOGS_PROPERTIES_RESOURCE =
            System.getProperty("analytics.job.checklogs.properties");

    private final Properties checkLogsProperties;

    public CheckLogsJob() throws IOException {
        checkLogsProperties = Utils.readProperties(CHECKLOGS_PROPERTIES_RESOURCE);
    }

    /** {@inheritDoc} */
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            Map<String, String> executionContext = Utils.initializeContext(TimeUnit.DAY);
            run(executionContext);
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
            throw new JobExecutionException(e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void forceRun(Map<String, String> context) throws Exception {
        run(context);
    }

    private void run(Map<String, String> context) throws IOException {
        LOGGER.info("CheckLogsJob is started");
        long start = System.currentTimeMillis();

        try {
            String date = MetricParameter.TO_DATE.get(context) ;

            ValueData valueData = ScriptExecutor.INSTANCE.executeAndReturn(ScriptType.CHECK_LOGS_1, context);
            valueData = valueData.union(ScriptExecutor.INSTANCE.executeAndReturn(ScriptType.CHECK_LOGS_2, context));

            sendMail((ListListStringValueData)valueData, date);
        } finally {
            LOGGER.info("CheckLogsJob is finished in " + (System.currentTimeMillis() - start) / 1000 + " sec.");
        }
    }

    private void sendMail(ListListStringValueData valueData, String date) throws IOException {
        MailService mailService = new MailService(checkLogsProperties);

        StringBuilder builder = new StringBuilder();
        for (ListStringValueData item : valueData.getAll()) {
            builder.append(item.getAsString());
            builder.append('\n');
        }

        mailService.setSubject("Log checking for " + date);
        mailService.setText(builder.toString());

        mailService.send();
    }
}
