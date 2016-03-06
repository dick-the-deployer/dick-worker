package com.dickthedeployer.dick.worker.command;

import com.dickthedeployer.dick.worker.service.CommandService;
import lombok.Builder;
import rx.Observable;

import java.nio.file.Path;
import java.util.Collections;

import static java.util.Arrays.asList;

@Builder
public class CheckoutRepository implements Command {

    private CommandService commandService;
    private String repository;
    private String ref;
    private String sha;
    private Path location;

    @Override
    public Observable<String> invoke() {
        return LocalExecutor.builder()
                .commands(
                        asList("git clone " + repository + " .",
                                "git fetch origin",
                                "git checkout " + ref,
                                "git checkout " + sha)
                ).commandService(commandService)
                .location(location)
                .environment(Collections.emptyList())
                .build()
                .invoke();
    }
}
