package com.github.bruce_mig.spring_batch.tasklets;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SendEmail implements Tasklet {

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        // send email

        log.warn("---> Sending email on COMPLETED_WITH_SKIPS");
        long readSkipCount = contribution.getReadSkipCount();
        log.info("The job was completed but {} lines were skipped", readSkipCount);
        return RepeatStatus.FINISHED;
    }
}
