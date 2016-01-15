package com.dickthedeployer.dick.worker.util;

import org.junit.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ArgumentTokenizerTest {

    @Test
    public void shouldSplitCommand() {
        List<String> tokenize = ArgumentTokenizer.tokenize("bash -c \"\\\"git clone $REPOSITORY .\\\"\"");

        assertThat(tokenize).hasSize(3);
    }


}
