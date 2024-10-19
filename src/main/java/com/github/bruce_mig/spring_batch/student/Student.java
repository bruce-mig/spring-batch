package com.github.bruce_mig.spring_batch.student;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class Student {

    @Id
    @GeneratedValue
    private Integer id;
    private String fistName;
    private String lastName;
    private int age;
}
