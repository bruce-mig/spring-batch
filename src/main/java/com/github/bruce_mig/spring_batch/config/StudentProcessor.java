package com.github.bruce_mig.spring_batch.config;

import com.github.bruce_mig.spring_batch.student.Student;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class StudentProcessor implements ItemProcessor<Student,Student> {

    @Override
    public Student process(Student student) {
        // all the business logic goes here
        student.setId(null);
//        log.info("processing the item: {}", student);
        return student;
    }
}
