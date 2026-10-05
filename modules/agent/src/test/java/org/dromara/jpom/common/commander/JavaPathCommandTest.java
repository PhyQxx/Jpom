/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.common.commander;

import org.dromara.jpom.common.commander.impl.LinuxProjectCommander;
import org.dromara.jpom.common.commander.impl.WindowsProjectCommander;
import org.dromara.jpom.configuration.AgentConfig;
import org.dromara.jpom.configuration.ProjectConfig;
import org.dromara.jpom.configuration.ProjectLogConfig;
import org.dromara.jpom.model.RunMode;
import org.dromara.jpom.model.data.NodeProjectInfoModel;
import org.dromara.jpom.service.manage.ProjectInfoService;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertTrue;

public class JavaPathCommandTest {
    @Test
    public void customJavaMustBeUsedInRealCommandWithLegacyJarDiscovery() throws Exception {
        Path directory = Files.createTempDirectory("jpom-java-command-");
        Path jar = Files.createFile(directory.resolve("app.jar"));
        try {
            AgentConfig config = new AgentConfig(null);
            ProjectConfig project = new ProjectConfig();
            project.setLog(new ProjectLogConfig());
            config.setProject(project);
            ProjectInfoService service = new ProjectInfoService(null, config) {
                @Override
                public String resolveLibPath(NodeProjectInfoModel model) {
                    return directory.toString();
                }

                @Override
                public String resolveAbsoluteLog(NodeProjectInfoModel model, NodeProjectInfoModel original) {
                    return "/tmp/app.log";
                }
            };
            LinuxProjectCommander linux = new LinuxProjectCommander(config, null, null, service);
            WindowsProjectCommander windows = new WindowsProjectCommander(config, null, null, service);
            NodeProjectInfoModel model = new NodeProjectInfoModel();
            model.setId("TESTJAVA");
            model.setRunMode(RunMode.Jar);
            model.setJavaPath(" /usr/local/java/jdk1.8.0_202/bin/java ");
            String command = linux.buildRunCommand(model, model);
            assertTrue(command, command.startsWith("nohup /usr/local/java/jdk1.8.0_202/bin/java "));
            assertTrue(command, command.contains("-jar " + jar.toAbsolutePath()));
            assertTrue(command, command.endsWith(">> /tmp/app.log 2>&1 &"));
            model.setJavaPath(null);
            assertTrue(linux.buildRunCommand(model, model).startsWith("nohup java "));
            assertTrue(windows.buildRunCommand(model, model).startsWith("javaw "));
            model.setJavaPath("/opt/Java SDK/bin/java");
            assertTrue(linux.buildRunCommand(model, model).startsWith("nohup \"/opt/Java SDK/bin/java\" "));
            model.setJavaPath("C:\\Java SDK\\bin\\java.exe");
            assertTrue(windows.buildRunCommand(model, model).startsWith("\"C:\\Java SDK\\bin\\java.exe\" "));
        } finally {
            Files.delete(jar);
            Files.delete(directory);
        }
    }
}
