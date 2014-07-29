/*******************************************************************************
 * Copyright (c) 2012-2014 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package com.codenvy.cli.command.builtin;

import com.codenvy.cli.command.builtin.model.DefaultUserBuilderStatus;
import com.codenvy.cli.command.builtin.model.DefaultUserRunnerStatus;
import com.codenvy.cli.command.builtin.model.UserBuilderStatus;
import com.codenvy.cli.command.builtin.model.UserProject;
import com.codenvy.cli.command.builtin.model.UserRunnerStatus;
import com.codenvy.client.Codenvy;
import com.codenvy.client.model.BuilderStatus;
import com.codenvy.client.model.Project;
import com.codenvy.client.model.RunnerStatus;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.fusesource.jansi.Ansi;

import static org.fusesource.jansi.Ansi.Attribute.INTENSITY_BOLD;
import static org.fusesource.jansi.Ansi.Attribute.INTENSITY_BOLD_OFF;
import static org.fusesource.jansi.Ansi.Color.RED;

/**
 * Allows to build a given project
 * @author Florent Benoit
 */
@Command(scope = "codenvy", name = "build", description = "Build a project in Codenvy System")
public class BuildCommand extends AbsCommand {

    @Argument(name = "project-id", description = "Specify the project ID to use", required = true, multiValued = false)
    private String projectID;

    /**
     * Execute the command
     */
    @Override
    protected Object doExecute() throws Exception {
        init();

        // not logged in
        if (!checkifEnvironments()) {
            return null;
        }

        // do we have the projectID ?
        if (projectID == null) {
            Ansi buffer = Ansi.ansi();
            buffer.fg(RED);
            buffer.a("No projectID has been set");
            buffer.reset();
            System.out.println(buffer.toString());
            return null;
        }

        // get project for the given shortID
        UserProject project = getMultiEnvCodenvy().getProject(projectID);

        if (project == null) {
            Ansi buffer = Ansi.ansi();
            buffer.fg(RED);
            buffer.a("No matching project for identifier '").a(projectID).a("'.");
            buffer.reset();
            System.out.println(buffer.toString());
            return null;
        }


        final Project projectToBuild = project.getInnerProject();

        // Ok now we have the project
        final BuilderStatus builderStatus = project.getCodenvy().builder().build(projectToBuild).execute();

        UserBuilderStatus userBuilderStatus = new DefaultUserBuilderStatus(builderStatus, project);

        Ansi buffer = Ansi.ansi();
        buffer.a("Build task for project ").a(INTENSITY_BOLD).a(project.name()).a(INTENSITY_BOLD_OFF)
              .a("' has been submitted with builder ID ").a(INTENSITY_BOLD).a(userBuilderStatus.shortId()).a(INTENSITY_BOLD_OFF)
              .a(System.lineSeparator());
        buffer.a("To get more details on builder ID, use the 'info ").a(INTENSITY_BOLD).a(userBuilderStatus.shortId()).a(INTENSITY_BOLD_OFF).a(
                "' command");
        System.out.println(buffer.toString());

        return null;
    }
}

