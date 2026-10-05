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

import cn.hutool.extra.spring.SpringUtil;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.context.support.StaticMessageSource;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class JarPreflightTest {
    @BeforeClass
    public static void initializeMessages() {
        GenericApplicationContext context = new GenericApplicationContext();
        StaticMessageSource messages = new StaticMessageSource();
        messages.setUseCodeAsDefaultMessage(true);
        context.getBeanFactory().registerSingleton("messageSource", messages);
        context.refresh();
        new SpringUtil().setApplicationContext(context);
    }

    private String check(Path jar) throws Exception {
        Method method = AbstractProjectCommander.class.getDeclaredMethod("checkJar", java.io.File.class);
        method.setAccessible(true);
        return (String) method.invoke(null, jar.toFile());
    }

    private byte[] mainClass(int major) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(buffer)) {
            out.writeInt(0xCAFEBABE);
            out.writeShort(0);
            out.writeShort(major);
            out.writeShort(5);
            out.writeByte(1); out.writeUTF("fixture/Main");
            out.writeByte(7); out.writeShort(1);
            out.writeByte(1); out.writeUTF("java/lang/Object");
            out.writeByte(7); out.writeShort(3);
            out.writeShort(0x0021); out.writeShort(2); out.writeShort(4);
            out.writeShort(0); out.writeShort(0); out.writeShort(0); out.writeShort(0);
        }
        return buffer.toByteArray();
    }

    private Path jar(String main, boolean includeClass, int major, boolean includeManifest) throws Exception {
        Path path = Files.createTempFile("jpom-jar-preflight-", ".jar");
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        if (main != null) manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, main);
        try (JarOutputStream out = includeManifest
            ? new JarOutputStream(Files.newOutputStream(path), manifest)
            : new JarOutputStream(Files.newOutputStream(path))) {
            if (includeClass) {
                out.putNextEntry(new JarEntry("fixture/Main.class"));
                out.write(mainClass(major));
                out.closeEntry();
            }
        }
        return path;
    }

    @Test
    public void newerProjectBytecodeDoesNotRequireNewerAgentJava() throws Exception {
        for (int major : new int[]{61, 65, 100}) {
            Path path = jar("fixture.Main", true, major, true);
            try { assertNull(check(path)); } finally { Files.delete(path); }
        }
    }

    @Test
    public void missingManifestMainAttributeOrClassAreRejected() throws Exception {
        Path[] paths = {jar(null, true, 61, false), jar(null, true, 61, true),
            jar(" ", true, 61, true), jar("fixture.Missing", true, 61, true),
            jar("fixture.Main", false, 61, true)};
        try { for (Path path : paths) assertNotNull(check(path)); }
        finally { for (Path path : paths) Files.delete(path); }
    }

    @Test
    public void corruptArchiveIsRejected() throws Exception {
        Path path = Files.createTempFile("jpom-invalid-jar-", ".jar");
        try { assertNotNull(check(path)); } finally { Files.delete(path); }
    }
}
