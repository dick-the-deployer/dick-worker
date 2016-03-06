package com.dickthedeployer.dick.worker.command;

import com.dickthedeployer.dick.worker.facade.model.EnvironmentVariable;
import com.dickthedeployer.dick.worker.service.CommandService;
import com.google.common.base.Throwables;
import org.junit.Ignore;
import org.junit.Test;
import rx.Observable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;

public class DockerCommandExecutorTest {

    @Test
    @Ignore("Won't work on travis, no docker")
    public void shouldCreateScript() {
        List<EnvironmentVariable> environment = new ArrayList<>();
        environment.add(EnvironmentVariable.builder()
                .name("FOO")
                .value("foovalue")
                .build());
        // If running on docker machine
        environment.add(EnvironmentVariable.builder()
                .name("DOCKER_TLS_VERIFY")
                .value("1")
                .build());
        environment.add(EnvironmentVariable.builder()
                .name("DOCKER_HOST")
                .value("tcp://192.168.99.100:2376")
                .build());
        environment.add(EnvironmentVariable.builder()
                .name("DOCKER_CERT_PATH")
                .value("/Users/mariusz.luciow/.docker/machine/machines/default")
                .build());
        environment.add(EnvironmentVariable.builder()
                .name("DOCKER_MACHINE_NAME")
                .value("default")
                .build());
        Path tempDirectory = getTempDirectory();
        Path codeDir = getCodeDirectory(tempDirectory);
        DockerCommandExecutor command = DockerCommandExecutor.builder()
                .codeLocation(codeDir)
                .commands(asList("echo $FOO", "echo bar"))
                .commandService(new CommandService())
                .environment(environment)
                .location(tempDirectory)
                .image("centos")
                .runner("bash -c")
                .additionalDockerParams("")
                .build();

        Observable<String> invoke = command.invoke();
        invoke.subscribe();
    }


    // https://github.com/docker/machine/issues/13
    // Util then when running on docker-machine (not in docker natively)
    // catalogues other than users are not mounted correctly, so this instead of temp
    private Path getTempDirectory() {
        try {
            return Files.createDirectory(Paths.get("target/foo")).toAbsolutePath();
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    private Path getCodeDirectory(Path temp) {
        try {
            return Files.createDirectory(temp.resolve("code"));
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }
}
