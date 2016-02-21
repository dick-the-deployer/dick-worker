package com.dickthedeployer.dick.worker.command;

import com.dickthedeployer.dick.worker.service.CommandService;
import com.dickthedeployer.dick.worker.util.ArgumentTokenizer;
import com.google.common.base.Throwables;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import rx.Observable;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Builder
public class DockerCommandExecutor implements Command {

    private CommandService commandService;
    private Path location;
    private Path codeLocation;
    private List<String> commands;
    private String runner;
    private String image;
    private String additionalDockerParams;
    private Map<String, String> environment;

    @Override
    public Observable<String> invoke() {
        createScript();
        String command = buildCommand();
        return commandService.invokeWithEnvironment(location, environment, split(command));
    }

    private String buildCommand() {
        String environment = getEnvironment();
        return "docker run --rm " +
                additionalDockerParams + " " +
                environment +
                " -v " + location + ":/dick " +
                "-w /dick/code " +
                image + " " +
                runner + " /dick/building-script";
    }

    private String getEnvironment() {
        return this.environment.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .map(variable -> "-e " + variable)
                .reduce("", (first, second) -> first + second + " ");
    }


    private String[] split(String command) {
        log.info("Splitting command: {}", command);
        return ArgumentTokenizer.tokenize(command).toArray(new String[0]);
    }

    private void createScript() {
        Set<OpenOption> options = new HashSet<>();
        options.add(StandardOpenOption.CREATE_NEW);
        options.add(StandardOpenOption.APPEND);

        Set<PosixFilePermission> perms =
                PosixFilePermissions.fromString("r-xr-xr-x");
        FileAttribute<Set<PosixFilePermission>> attr =
                PosixFilePermissions.asFileAttribute(perms);

        byte data[] = commands.stream()
                .reduce("", (first, second) -> first + second + "\n")
                .getBytes();
        ByteBuffer bb = ByteBuffer.wrap(data);

        Path file = location.resolve("building-script");

        try (SeekableByteChannel sbc =
                     Files.newByteChannel(file, options, attr)) {
            sbc.write(bb);
        } catch (IOException x) {
            Throwables.propagate(x);
        }
    }
}
