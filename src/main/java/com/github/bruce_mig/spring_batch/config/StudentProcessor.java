package com.github.bruce_mig.spring_batch.config;

import com.github.bruce_mig.spring_batch.student.Student;
import org.springframework.batch.item.ItemProcessor;

public class StudentProcessor implements ItemProcessor<Student,Student> {

    @Override
    public Student process(Student student) throws Exception {
        // all the business logic goes here
        student.setId(null);
        return student;
    }
}
