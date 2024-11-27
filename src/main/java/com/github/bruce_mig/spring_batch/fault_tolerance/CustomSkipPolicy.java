package com.github.bruce_mig.spring_batch.fault_tolerance;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.step.skip.SkipLimitExceededException;
import org.springframework.batch.core.step.skip.SkipPolicy;
import org.springframework.batch.item.file.FlatFileParseException;
import org.springframework.lang.NonNullApi;
import org.springframework.stereotype.Component;

import java.io.FileNotFoundException;

@Component
@Slf4j
public class CustomSkipPolicy implements SkipPolicy {

    private final Integer skipLimit = 1;

    @Override
    public boolean shouldSkip(Throwable exception, long skipCount) throws SkipLimitExceededException {
        if (exception instanceof FileNotFoundException){
            return Boolean.FALSE;
        } else if ((exception instanceof FlatFileParseException fileParseException) && (skipCount <= skipLimit)){
            String input = fileParseException.getInput();
            int lineNumber = fileParseException.getLineNumber();

            log.warn("Skipping line {}: {}", lineNumber, input);
            // write into a file
            // send into kafka topic or message broker
            return Boolean.TRUE;
        } else if ((exception instanceof IllegalArgumentException illegalArgumentException) && (skipCount <= skipLimit)){
            log.warn("An error occurred.");
            return Boolean.TRUE;
        }
        return false;
    }
}
