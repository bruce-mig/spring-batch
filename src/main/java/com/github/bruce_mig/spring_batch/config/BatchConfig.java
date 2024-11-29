package com.github.bruce_mig.spring_batch.config;

import com.github.bruce_mig.spring_batch.fault_tolerance.CustomSkipPolicy;
import com.github.bruce_mig.spring_batch.listeners.CustomJobExecutionListener;
import com.github.bruce_mig.spring_batch.listeners.CustomStepExecutionListener;
import com.github.bruce_mig.spring_batch.step.FileCollector;
import com.github.bruce_mig.spring_batch.student.Student;
import com.github.bruce_mig.spring_batch.student.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.integration.async.AsyncItemProcessor;
import org.springframework.batch.integration.async.AsyncItemWriter;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.data.RepositoryItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.LineMapper;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@RequiredArgsConstructor
public class BatchConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager platformTransactionManager;
    private final StudentRepository repository;
    private final StudentProcessor studentProcessor;
    private final CustomSkipPolicy customSkipPolicy;
    private final CustomStepExecutionListener customStepExecutionListener;
    private final CustomJobExecutionListener customJobExecutionListener;
    private final FileCollector fileCollector;

    @Bean
    @StepScope
    public FlatFileItemReader<Student> itemReader(@Value("#{jobParameters['input.file.name']}") String inputFileName) {
        FlatFileItemReader<Student> itemReader = new FlatFileItemReader<>();
        itemReader.setResource(new FileSystemResource(inputFileName));
        itemReader.setName("csvReader");
        itemReader.setLinesToSkip(1);
        itemReader.setLineMapper(lineMapper());
        return itemReader;
    }

   /* @Bean
    public StudentProcessor processor(){
        return new StudentProcessor();
    }
*/

    @Bean
    public RepositoryItemWriter<Student> writer(){
        RepositoryItemWriter<Student> writer = new RepositoryItemWriter<>();
        writer.setRepository(repository);
        writer.setMethodName("save");
        return  writer;
    }

    @Bean
    public Step importStep(ItemReader<Student> studentDTOItemReader){
        return new StepBuilder("csvImport", jobRepository)
                .<Student, Future<Student>>chunk(500, platformTransactionManager)
                .reader(studentDTOItemReader)
                .processor(asyncProcessor())
                .writer(asyncWriter())
                .faultTolerant()
                .skipPolicy(customSkipPolicy)
                .listener(customStepExecutionListener)
                .taskExecutor(taskExecutor())
                .build();
    }

    @Bean
    public Step fileCollectorTasklet(){
        return new StepBuilder("fileCollector", jobRepository).
                tasklet(fileCollector, platformTransactionManager)
                .build();
    }

    @Bean
    public Job runJob(Step importStep){
        return new JobBuilder("importStudents", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(importStep)
                .next(fileCollectorTasklet())
                .listener(customJobExecutionListener)
                .build();
    }

    /*@Bean
    public TaskExecutor taskExecutor(){
        try (SimpleAsyncTaskExecutor asyncTaskExecutor = new SimpleAsyncTaskExecutor()){
            asyncTaskExecutor.setConcurrencyLimit(30);  // throttle number of threads
            return asyncTaskExecutor;
        }
    }*/

    @Bean
    public TaskExecutor taskExecutor() {
        // Settings for a 1 core server running I/O workloads
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2); // Slightly more than 1 to allow for some concurrency
        executor.setMaxPoolSize(4); // Allow bursts of threads for high I/O activity
        executor.setQueueCapacity(50); // Accommodate more tasks while threads are busy
        executor.setThreadNamePrefix("Thread N-> : ");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    @Bean
    public AsyncItemProcessor<Student,Student> asyncProcessor(){
        var asyncItemProcessor = new AsyncItemProcessor<Student,Student>();
        asyncItemProcessor.setDelegate(studentProcessor);
        asyncItemProcessor.setTaskExecutor(taskExecutor());
        return asyncItemProcessor;
    }

    @Bean
    public AsyncItemWriter<Student> asyncWriter(){
        AsyncItemWriter<Student> asyncWriter = new AsyncItemWriter<>();
        asyncWriter.setDelegate(writer());
        return asyncWriter;
    }

    private LineMapper<Student> lineMapper(){
        DefaultLineMapper<Student> lineMapper = new DefaultLineMapper<>();
        DelimitedLineTokenizer lineTokenizer = new DelimitedLineTokenizer();
        lineTokenizer.setDelimiter(",");
        lineTokenizer.setStrict(false);
        lineTokenizer.setNames("id","firstname", "lastname", "age");

        BeanWrapperFieldSetMapper<Student> fieldSetMapper = new BeanWrapperFieldSetMapper<>();
        fieldSetMapper.setTargetType(Student.class);

        lineMapper.setLineTokenizer(lineTokenizer);
        lineMapper.setFieldSetMapper(fieldSetMapper);

        return lineMapper;
    }

}
