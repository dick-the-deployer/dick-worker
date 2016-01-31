package com.dickthedeployer.dick.worker.command;

import rx.Observable;

public interface Command {

    Observable<String> invoke();
}
