package com.github.bruce_mig.spring_batch.listeners;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Component
public class CustomJobExecutionListener implements JobExecutionListener {

    public static final String INPUT_FILE_NAME = "input.file.name";

    @Override
    public void beforeJob(JobExecution jobExecution) {
        log.info("-------> Job execution started");
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        log.info("------> After job computing the business logic");
        JobParameters jobParameters = jobExecution.getJobParameters();
        Map<String, JobParameter<?>> parameters = jobParameters.getParameters();
        if (parameters.containsKey(INPUT_FILE_NAME)) {
            compute(jobExecution, parameters);
        }

    }

    private void compute(final JobExecution jobExecution, Map<String, JobParameter<?>> parameters) {
        String sourceDirectoryAbsolutePath = (String) parameters.get(INPUT_FILE_NAME).getValue();
        Path inputDirectoryAbsolutePath = Path.of(sourceDirectoryAbsolutePath);
        Path inputDirectoryParent = inputDirectoryAbsolutePath.getParent();

        Path processedPath = Paths.get(inputDirectoryParent + File.separator + "processed");
        Path failedPath = Paths.get(inputDirectoryParent + File.separator + "failed");

        if (ExitStatus.COMPLETED.getExitCode().equalsIgnoreCase(jobExecution.getExitStatus().getExitCode())) {
            // then create dir if not exist and send the file into the directory
            createDirectoryIfAbsent(processedPath);
            computeFileMove(inputDirectoryAbsolutePath, processedPath);
        } else if (ExitStatus.STOPPED.getExitCode().equalsIgnoreCase(jobExecution.getExitStatus().getExitCode()) ||
                ExitStatus.FAILED.getExitCode().equalsIgnoreCase(jobExecution.getExitStatus().getExitCode())) {
            // send to failed directory
            createDirectoryIfAbsent(failedPath);
            computeFileMove(inputDirectoryAbsolutePath, failedPath);
        }
    }

    @SneakyThrows
    void computeFileMove(final Path inputDirectoryAbsolutePath, final Path targetDirectory) {
        Path destination = targetDirectory.resolve(inputDirectoryAbsolutePath.getFileName());
        Files.move(inputDirectoryAbsolutePath, destination, StandardCopyOption.ATOMIC_MOVE);
    }

    @SneakyThrows
    void createDirectoryIfAbsent(final Path directoryPath) {
        Objects.requireNonNull(directoryPath, "The directory path must not be null");
        if (Files.notExists(directoryPath)) {
            log.info("--------> Creating directory {}", directoryPath.toAbsolutePath());
            Files.createDirectory(directoryPath);
        }
    }
}
