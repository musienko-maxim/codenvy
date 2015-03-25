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
package com.codenvy.im.cli.command;

import com.codenvy.im.service.InstallationManagerService;
import com.codenvy.im.service.UserCredentials;
import org.apache.felix.service.command.CommandSession;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.restlet.resource.ResourceException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.testng.Assert.assertEquals;

/** @author Dmytro Nochevnov */
public class TestAddNodeCommand extends AbstractTestCommand {
    private AbstractIMCommand spyCommand;

    @Mock
    private InstallationManagerService mockInstallationManagerProxy;
    @Mock
    private CommandSession             commandSession;

    private UserCredentials credentials;

    @BeforeMethod
    public void initMocks() throws IOException {
        MockitoAnnotations.initMocks(this);

        spyCommand = spy(new AddNodeCommand());
        spyCommand.service = mockInstallationManagerProxy;

        performBaseMocks(spyCommand, true);

        credentials = new UserCredentials("token", "accountId");
        doReturn(credentials).when(spyCommand).getCredentials();
    }

    @Test
    public void testAddNode() throws Exception {
        String okServiceResponse = "{\n"
                                   + "  \"status\" : \"OK\"\n"
                                   + "}";
        String TEST_DNS_NAME = "some";
        doReturn(okServiceResponse).when(mockInstallationManagerProxy).addNode(TEST_DNS_NAME);

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);
        commandInvoker.argument("dns", TEST_DNS_NAME);

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.disableAnsi().getOutputStream();
        assertEquals(output, okServiceResponse + "\n");
    }

    @Test
    public void testAddNodeThrowsError() throws Exception {
        String expectedOutput = "{\n"
                                + "  \"message\" : \"Server Error Exception\",\n"
                                + "  \"status\" : \"ERROR\"\n"
                                + "}";
        String TEST_DNS_NAME = "some";
        doThrow(new ResourceException(500, "Server Error Exception", "Description", "localhost"))
            .when(mockInstallationManagerProxy).addNode(TEST_DNS_NAME);

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);
        commandInvoker.argument("dns", TEST_DNS_NAME);

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.disableAnsi().getOutputStream();
        assertEquals(output, expectedOutput + "\n");
    }
}