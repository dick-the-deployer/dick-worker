package com.dickthedeployer.dick.worker.command;

import com.dickthedeployer.dick.worker.ContextTestBase;
import com.dickthedeployer.dick.worker.service.CommandService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

public class CheckoutRepositoryTest extends ContextTestBase {

    @Autowired
    CommandService commandService;

    @Test
    public void shouldCheckOutRepository() throws IOException {
        Path footest = Files.createTempDirectory("footest");

        CheckoutRepository.builder()
                .repository("https://github.com/dick-the-deployer/dick.git")
                .ref("master")
                .sha("74ab161c6b4df731ded57dd434c8df120d140832")
                .commandService(commandService)
                .location(footest)
                .build().invoke().subscribe();

        Path dickfile = footest.resolve(".dick.yml");
        assertThat(Files.exists(dickfile)).isTrue();
    }
}
