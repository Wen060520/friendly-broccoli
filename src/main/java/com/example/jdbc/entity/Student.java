package com.example.jdbc.entity;

import com.example.jdbc.annotation.Column;
import com.example.jdbc.annotation.Id;
import com.example.jdbc.annotation.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

@Table("students")
public class Student {
    @Id("id")
    private Integer id;

    @Column("name")
    private String name;

    @Column("major")
    private String major;

    @Column("age")
    private Integer age;

    @Column("enrollment_date")
    private LocalDate enrollmentDate;

    @Column("graduated")
    private Boolean graduated;

    @Column("tuition")
    private BigDecimal tuition;

    public Student() {
    }

    public Student(Integer id, String name, String major, Integer age,
                   LocalDate enrollmentDate, Boolean graduated, BigDecimal tuition) {
        this.id = id;
        this.name = name;
        this.major = major;
        this.age = age;
        this.enrollmentDate = enrollmentDate;
        this.graduated = graduated;
        this.tuition = tuition;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMajor() {
        return major;
    }

    public void setMajor(String major) {
        this.major = major;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public LocalDate getEnrollmentDate() {
        return enrollmentDate;
    }

    public void setEnrollmentDate(LocalDate enrollmentDate) {
        this.enrollmentDate = enrollmentDate;
    }

    public Boolean getGraduated() {
        return graduated;
    }

    public void setGraduated(Boolean graduated) {
        this.graduated = graduated;
    }

    public BigDecimal getTuition() {
        return tuition;
    }

    public void setTuition(BigDecimal tuition) {
        this.tuition = tuition;
    }

    @Override
    public String toString() {
        return "Student{id=%d, name='%s', major='%s', age=%d, enrollmentDate=%s, graduated=%s, tuition=%s}"
                .formatted(id, name, major, age, enrollmentDate, graduated, tuition);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Student student)) {
            return false;
        }
        return Objects.equals(id, student.id)
                && Objects.equals(name, student.name)
                && Objects.equals(major, student.major)
                && Objects.equals(age, student.age)
                && Objects.equals(enrollmentDate, student.enrollmentDate)
                && Objects.equals(graduated, student.graduated)
                && Objects.equals(tuition, student.tuition);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, major, age, enrollmentDate, graduated, tuition);
    }
}
