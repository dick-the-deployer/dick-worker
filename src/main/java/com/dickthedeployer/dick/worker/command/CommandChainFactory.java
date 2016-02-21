package com.dickthedeployer.dick.worker.command;

import com.dickthedeployer.dick.worker.facade.model.BuildOrder;
import com.dickthedeployer.dick.worker.service.CommandService;
import com.google.common.base.Throwables;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${dick.worker.runner}")
    RunnerType runnerType;

    @Value("${dick.worker.docker.params:}")
    String additionalDockerParams;

    @Value("${dick.worker.docker.runner:bash -c}")
    String runner;

    public List<Command> produceCommands(BuildOrder buildOrder) {
        Path temp = getTempDirectory(buildOrder);
        Path code = getCodeDirectory(temp);
        List<Command> commands = new ArrayList<>();
        if (buildOrder.isRequireRepository()) {
            commands.add(CheckoutRepository.builder()
                    .location(code)
                    .commandService(commandService)
                    .ref(buildOrder.getRef())
                    .repository(buildOrder.getRepository())
                    .sha(buildOrder.getSha())
                    .build()
            );
        }
        if (runnerType.equals(RunnerType.docker)) {
            commands.add(DockerCommandExecutor.builder()
                    .codeLocation(code)
                    .commands(buildOrder.getCommands())
                    .commandService(commandService)
                    .environment(buildOrder.getEnvironment())
                    .location(temp)
                    .image(buildOrder.getDockerImage())
                    .runner(runner)
                    .additionalDockerParams(additionalDockerParams)
                    .build()
            );
        } else {
            commands.add(LocalExecutor.builder()
                    .commands(buildOrder.getCommands())
                    .environment(buildOrder.getEnvironment())
                    .commandService(commandService)
                    .location(code)
                    .build()
            );
        }
        return commands;
    }

    private Path getCodeDirectory(Path temp) {
        try {
            return Files.createDirectory(temp.resolve("code"));
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    private Path getTempDirectory(BuildOrder buildOrder) {
        try {
            return Files.createTempDirectory("build-" + buildOrder.getBuildId());
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }
}
