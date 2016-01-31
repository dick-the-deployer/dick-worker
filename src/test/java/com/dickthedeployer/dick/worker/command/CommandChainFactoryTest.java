package com.dickthedeployer.dick.worker.command;

import com.dickthedeployer.dick.worker.ContextTestBase;
import com.dickthedeployer.dick.worker.facade.model.BuildOrder;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class CommandChainFactoryTest extends ContextTestBase {

    @Autowired
    CommandChainFactory commandChainFactory;

    @Test
    public void shouldCreateCheckoutCommand() {
        List<Command> commands = commandChainFactory.produceCommands(BuildOrder.builder()
                .requireRepository(true)
                .build());

        assertThat(commands).hasSize(2);
        assertThat(commands.get(0)).isInstanceOf(CheckoutRepository.class);
    }
}
