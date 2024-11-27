package com.github.bruce_mig.spring_batch.integration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.integration.launch.JobLaunchingGateway;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.file.DefaultFileNameGenerator;
import org.springframework.integration.file.FileReadingMessageSource;
import org.springframework.integration.file.FileWritingMessageHandler;
import org.springframework.integration.file.filters.SimplePatternFileListFilter;
import org.springframework.integration.file.support.FileExistsMode;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.Duration;

@Component
@EnableIntegration
@IntegrationComponentScan
@Slf4j
public class StudentIntegrationConfig {

    private final Job runJob;
    private final JobRepository jobRepository;
    private final BeanFactory beanFactory;

    @Value("${student.directory}")
    private String studentDirectory;

    public StudentIntegrationConfig(Job runJob, JobRepository jobRepository, BeanFactory beanFactory) {
        this.runJob = runJob;
        this.jobRepository = jobRepository;
        this.beanFactory = beanFactory;
    }

    @Bean
    public IntegrationFlow integrationFlow() {
        return IntegrationFlow.from(fileReadingMessageSource(),
                sourcePolling -> sourcePolling.poller(Pollers.fixedDelay(Duration.ofSeconds(5)).maxMessagesPerPoll(1)))
                .channel(fileIn())
                .handle(fileRenameProcessingHandler())
                .transform(fileMessageToJobRequest())
                .handle(jobLaunchingGateway())
                .log()
                .bridge(e -> e.id("bridgeToConsumeOutput"))
                .channel(outputChannel())
                .get();
    }

    public FileReadingMessageSource fileReadingMessageSource(){
        FileReadingMessageSource messageSource = new FileReadingMessageSource();
        messageSource.setDirectory(new File(studentDirectory));
        messageSource.setFilter(new SimplePatternFileListFilter("*.csv"));
        return messageSource;
    }

    public DirectChannel fileIn(){
        return new DirectChannel();
    }

    public DirectChannel outputChannel(){
        DirectChannel directChannel = new DirectChannel();
        directChannel.subscribe(message -> log.info("Message received in outputChannel: {}", message));
        return directChannel;
    }

    public FileWritingMessageHandler fileRenameProcessingHandler(){
        FileWritingMessageHandler fileWritingMessage = new FileWritingMessageHandler(new File(studentDirectory));
        fileWritingMessage.setFileExistsMode(FileExistsMode.REPLACE);
        fileWritingMessage.setDeleteSourceFiles(Boolean.TRUE);
        fileWritingMessage.setFileNameGenerator(fileNameGenerator());
        fileWritingMessage.setRequiresReply(false);
        return fileWritingMessage;
    }

    public DefaultFileNameGenerator fileNameGenerator(){
        DefaultFileNameGenerator fileNameGenerator = new DefaultFileNameGenerator();
        fileNameGenerator.setExpression("payload.name + '.processing'");
        fileNameGenerator.setBeanFactory(beanFactory);
        return fileNameGenerator;
    }

    public FileMessageToJobRequest fileMessageToJobRequest(){
        var transformer = new FileMessageToJobRequest();
        transformer.setJob(runJob);
        return transformer;
    }

    public JobLaunchingGateway jobLaunchingGateway(){
        var jobLauncher = new TaskExecutorJobLauncher();
        jobLauncher.setJobRepository(jobRepository);
        jobLauncher.setTaskExecutor(new SyncTaskExecutor());
        var gateway = new JobLaunchingGateway(jobLauncher);
        gateway.setOutputChannel(outputChannel());
        return gateway;
    }

    @Bean
    public IntegrationFlow outputConsumerFlow() {
        return IntegrationFlow.from(outputChannel())
                .handle(message -> log.info("Message received in outputChannel: {}", message))
                .get();
    }
}
