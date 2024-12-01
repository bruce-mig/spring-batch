package com.github.bruce_mig.spring_batch.config;

import com.github.bruce_mig.spring_batch.AbstractContainerProviderConfig;
import com.github.bruce_mig.spring_batch.SpringBatchApplication;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@SpringBatchTest
@SpringJUnitConfig({SpringBatchApplication.class, BatchConfig.class})
//@ImportAutoConfiguration(BatchAutoConfiguration.class)
@ActiveProfiles("test")
@Slf4j
class BatchConfigTest extends AbstractContainerProviderConfig {

    @TempDir
    Path tempDir;

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private JobRepositoryTestUtils jobRepositoryTestUtils;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    Path INPUT_DIRECTORY;
    Path EXPECTED_COMPLETED_DIRECTORY;
    Path EXPECTED_FAILED_DIRECTORY;

    @BeforeEach
    @SneakyThrows
    void setUp() {
        INPUT_DIRECTORY = tempDir.resolve("data");
        EXPECTED_COMPLETED_DIRECTORY = INPUT_DIRECTORY.resolve("processed");
        EXPECTED_FAILED_DIRECTORY = INPUT_DIRECTORY.resolve("failed");

        if (Files.notExists(INPUT_DIRECTORY)) {
            Files.createDirectory(INPUT_DIRECTORY);
        }

        jobRepositoryTestUtils.removeJobExecutions();
    }

    @AfterEach
    @SneakyThrows
    void tearDown() {
        try (Stream<Path> fileTree = Files.walk(INPUT_DIRECTORY)) {
            fileTree.filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .peek(fileToDelete -> log.warn("Deleting the file: {}", fileToDelete.getName()))
                    .forEach(File::delete);
        }
    }

    @Test
    @DisplayName("GIVEN a directory with invalid files WHEN jobLaunched THEN exit status is FAILED and file is moved into failed directory")
    @SneakyThrows
    void shouldFailWhenInputFileContainsInvalidData() {
        // GIVEN
        Path studentsFilePath = Path.of(INPUT_DIRECTORY + File.separator + "students.csv");
        Path inputFile = Files.createFile(studentsFilePath);

        Files.writeString(inputFile, StudentsTestDataProviderUtils.supplyInvalidContent());

        // WHEN
        var jobParameters = new JobParametersBuilder()
                .addString("input.file.name", inputFile.toString())
                .addDate("uniqueness", new Date())
                .toJobParameters();

        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        // THEN
        boolean failedReaderStep = jobExecution.getStepExecutions()
                .stream()
                .filter(stepExecution -> "csvImport".equals(stepExecution.getStepName()))
                .anyMatch(stepExecution -> ExitStatus.FAILED.getExitCode().equalsIgnoreCase(stepExecution.getExitStatus().getExitCode()));

        assertTrue(failedReaderStep);
        assertEquals(ExitStatus.FAILED, jobExecution.getExitStatus());

/*        Integer totalRowsInserted = jdbcTemplate.queryForObject("select count(*) from student", Integer.class);
        assertEquals(0,totalRowsInserted);*/

        assertTrue(Files.exists(EXPECTED_FAILED_DIRECTORY));

        try (Stream<Path> fileStream = Files.list(EXPECTED_FAILED_DIRECTORY)) {
            boolean containsAnyFile = fileStream
                    .findAny()
                    .isPresent();

            assertTrue(containsAnyFile);
        }
    }

    @Test
    @DisplayName("GIVEN a directory with valid files WHEN jobLaunched THEN records persisted into DB and the input file is moved to processed directory")
    @SneakyThrows
    void shouldReadFromFileAndPersistIntoDatabaseAndMoveToProcessedDirectory(){
        // GIVEN
        Path studentsFilePath = Path.of(INPUT_DIRECTORY + File.separator + "students.csv");
        Path inputFile = Files.createFile(studentsFilePath);

        Files.writeString(inputFile, StudentsTestDataProviderUtils.supplyValidContent());

        // WHEN
        var jobParameters = new JobParametersBuilder()
                .addString("input.file.name", inputFile.toString())
                .addDate("uniqueness", new Date())
                .toJobParameters();

        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);


        // THEN
        assertEquals(ExitStatus.COMPLETED, jobExecution.getExitStatus());

        Integer totalRowsInserted = jdbcTemplate.queryForObject("select count(*) from student", Integer.class);
        assertEquals(1000,totalRowsInserted);
        assertTrue(Files.exists(EXPECTED_COMPLETED_DIRECTORY));

        try (Stream<Path> fileStream = Files.list(EXPECTED_COMPLETED_DIRECTORY)) {
            boolean containsAnyFile = fileStream
                    .findAny()
                    .isPresent();

            assertTrue(containsAnyFile);
        }
    }



}
