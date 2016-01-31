package com.dickthedeployer.dick.worker.command;

import com.dickthedeployer.dick.worker.facade.model.BuildOrder;
import com.dickthedeployer.dick.worker.service.CommandService;
import com.google.common.base.Throwables;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
public class CommandChainFactory {

    @Autowired
    CommandService commandService;

    public List<Command> produceCommands(BuildOrder buildOrder) {
        Path temp = getTempDirectory(buildOrder);
        List<Command> commands = new ArrayList<>();
        if (buildOrder.isRequireRepository()) {
            commands.add(CheckoutRepository.builder()
                    .location(temp)
                    .commandService(commandService)
                    .ref(buildOrder.getRef())
                    .repository(buildOrder.getRepository())
                    .sha(buildOrder.getSha())
                    .build()
            );
        }
        commands.add(LocalExecutor.builder()
                .commands(buildOrder.getCommands())
                .environment(buildOrder.getEnvironment())
                .commandService(commandService)
                .location(temp)
                .build()
        );
        return commands;
    }

    private Path getTempDirectory(BuildOrder buildOrder) {
        try {
            return Files.createTempDirectory("build-" + buildOrder.getBuildId());
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }
}
