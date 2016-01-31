package com.dickthedeployer.dick.worker.command;

import com.dickthedeployer.dick.worker.service.CommandService;
import com.dickthedeployer.dick.worker.util.ArgumentTokenizer;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import rx.Observable;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@Slf4j
@Builder
public class LocalExecutor implements Command {

    private CommandService commandService;
    private Path location;
    private List<String> commands;
    private Map<String, String> environment;

    @Override
    public Observable<String> invoke() {
        return Observable.just(commands)
                .concatMap(Observable::from)
                .map(command -> split(command))
                .concatMap(commandArray
                        -> commandService.invokeWithEnvironment(location, environment, commandArray)
                );
    }

    private String[] split(String command) {
        log.info("Splitting command: {}", command);
        return ArgumentTokenizer.tokenize(command).toArray(new String[0]);
    }
}
