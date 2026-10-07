package com.example.jdbc;

import com.example.jdbc.entity.College;
import com.example.jdbc.entity.Student;
import com.example.jdbc.util.DatabaseConfig;
import com.example.jdbc.util.DatabaseInitializer;
import com.example.jdbc.util.JDBCTool;

import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;

public final class CrudDemo {
    private CrudDemo() {
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== JDBC + Reflection ORM Demo: DateTest ===");

        try (Connection connection = DatabaseConfig.getConnection()) {
            DatabaseInitializer.initialize(connection);
            DatabaseInitializer.clearData(connection);

            demonstrateCollegeCrud(connection);
            demonstrateStudentCrud(connection);
        }

        System.out.println();
        System.out.println("All CRUD operations completed successfully.");
    }

    private static void demonstrateCollegeCrud(Connection connection) throws Exception {
        section("1. College table");

        College computerCollege = new College(null, "School of Computer Science", "CS");
        College mathematicsCollege = new College(null, "School of Mathematics", "MATH");
        System.out.println("save college rows : "
                + JDBCTool.save(computerCollege, connection)
                + ", " + JDBCTool.save(mathematicsCollege, connection));
        System.out.println("generated ids      : " + computerCollege.getId()
                + ", " + mathematicsCollege.getId());

        printColleges("after insert", JDBCTool.getOneById("1", College.class, connection),
                queryAllColleges(connection));

        computerCollege.setName("School of Computing");
        System.out.println("update rows        : " + JDBCTool.update(computerCollege, connection));
        printColleges("after update", JDBCTool.getOneById("1", College.class, connection),
                queryAllColleges(connection));

        System.out.println("delete rows        : " + JDBCTool.delete(mathematicsCollege, connection));
        printColleges("after delete", JDBCTool.getOneById("1", College.class, connection),
                queryAllColleges(connection));
    }

    private static void demonstrateStudentCrud(Connection connection) throws Exception {
        section("2. Student table");

        Student alice = new Student(null, "Alice", "Computer Science", 20,
                LocalDate.of(2024, 9, 1), false, new BigDecimal("6800.00"));
        Student bob = new Student(null, "Bob", "Mathematics", 22,
                LocalDate.of(2022, 9, 1), true, new BigDecimal("6200.50"));
        System.out.println("save student rows : "
                + JDBCTool.save(alice, connection)
                + ", " + JDBCTool.save(bob, connection));
        System.out.println("generated ids     : " + alice.getId() + ", " + bob.getId());

        printStudents("after insert", JDBCTool.getOneById("1", Student.class, connection),
                queryAllStudents(connection));

        alice.setAge(21);
        alice.setMajor("Software Engineering");
        alice.setTuition(new BigDecimal("7000.00"));
        System.out.println("update rows       : " + JDBCTool.update(alice, connection));
        printStudents("after update", JDBCTool.getOneById("1", Student.class, connection),
                queryAllStudents(connection));

        System.out.println("delete rows       : " + JDBCTool.delete(bob, connection));
        printStudents("after delete", JDBCTool.getOneById("1", Student.class, connection),
                queryAllStudents(connection));
    }

    private static List<College> queryAllColleges(Connection connection) throws Exception {
        try (var statement = connection.createStatement();
             var resultSet = statement.executeQuery("SELECT * FROM colleges ORDER BY id")) {
            return JDBCTool.resultSetToList(resultSet, College.class);
        }
    }

    private static List<Student> queryAllStudents(Connection connection) throws Exception {
        try (var statement = connection.createStatement();
             var resultSet = statement.executeQuery("SELECT * FROM students ORDER BY id")) {
            return JDBCTool.resultSetToList(resultSet, Student.class);
        }
    }

    private static void printColleges(String label, College one, List<College> all) {
        System.out.println("-- " + label);
        System.out.println("getOneById(1)     : " + one);
        System.out.println("resultSetToList() : " + all);
    }

    private static void printStudents(String label, Student one, List<Student> all) {
        System.out.println("-- " + label);
        System.out.println("getOneById(1)     : " + one);
        System.out.println("resultSetToList() : " + all);
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("------------------------------------------------------------");
        System.out.println(title);
        System.out.println("------------------------------------------------------------");
    }
}
