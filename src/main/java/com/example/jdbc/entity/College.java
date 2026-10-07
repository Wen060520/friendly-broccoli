package com.example.jdbc.entity;

import com.example.jdbc.annotation.Column;
import com.example.jdbc.annotation.Id;
import com.example.jdbc.annotation.Table;

import java.util.Objects;

@Table("colleges")
public class College {
    @Id("id")
    private Integer id;

    @Column("name")
    private String name;

    @Column("code")
    private String code;

    public College() {
    }

    public College(Integer id, String name, String code) {
        this.id = id;
        this.name = name;
        this.code = code;
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

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    @Override
    public String toString() {
        return "College{id=%d, name='%s', code='%s'}".formatted(id, name, code);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof College college)) {
            return false;
        }
        return Objects.equals(id, college.id)
                && Objects.equals(name, college.name)
                && Objects.equals(code, college.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, code);
    }
}
