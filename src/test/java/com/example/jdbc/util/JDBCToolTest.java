package com.example.jdbc.util;

import com.example.jdbc.entity.College;
import com.example.jdbc.entity.Student;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JDBCToolTest {
    private Connection connection;

    @BeforeEach
    void setUp() throws SQLException {
        connection = DriverManager.getConnection(
                "jdbc:h2:mem:DateTest;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
                "sa", "");
        DatabaseInitializer.initialize(connection);
        DatabaseInitializer.clearData(connection);
    }

    @Test
    void savesAndReadsCollegeByReflection() throws SQLException {
        College college = new College(null, "School of Computer Science", "CS");

        assertEquals(1, JDBCTool.save(college, connection));
        assertNotNull(college.getId());

        College loaded = JDBCTool.getOneById(String.valueOf(college.getId()), College.class, connection);
        assertNotNull(loaded);
        assertEquals(college, loaded);
    }

    @Test
    void updatesAndDeletesStudent() throws SQLException {
        Student student = new Student(null, "Alice", "Computer Science", 20,
                LocalDate.of(2024, 9, 1), false, new BigDecimal("6800.00"));
        assertEquals(1, JDBCTool.save(student, connection));

        student.setAge(21);
        student.setMajor("Software Engineering");
        student.setGraduated(true);
        assertEquals(1, JDBCTool.update(student, connection));

        Student updated = JDBCTool.getOneById(String.valueOf(student.getId()), Student.class, connection);
        assertEquals(21, updated.getAge());
        assertEquals("Software Engineering", updated.getMajor());
        assertTrue(updated.getGraduated());

        assertEquals(1, JDBCTool.delete(student, connection));
        assertNull(JDBCTool.getOneById(String.valueOf(student.getId()), Student.class, connection));
    }

    @Test
    void mapsResultSetToListForBothEntities() throws SQLException {
        JDBCTool.save(new College(null, "School of Mathematics", "MATH"), connection);
        JDBCTool.save(new College(null, "School of Foreign Languages", "FL"), connection);
        JDBCTool.save(new Student(null, "Bob", "Mathematics", 22,
                LocalDate.of(2022, 9, 1), true, new BigDecimal("6200.50")), connection);

        List<College> colleges = JDBCTool.resultSetToList(
                connection.createStatement().executeQuery("SELECT * FROM colleges ORDER BY id"),
                College.class);
        List<Student> students = JDBCTool.resultSetToList(
                connection.createStatement().executeQuery("SELECT * FROM students ORDER BY id"),
                Student.class);

        assertEquals(2, colleges.size());
        assertEquals("MATH", colleges.get(0).getCode());
        assertEquals(1, students.size());
        assertEquals(LocalDate.of(2022, 9, 1), students.get(0).getEnrollmentDate());
        assertFalse(students.get(0).getTuition().compareTo(new BigDecimal("6200.50")) != 0);
    }
}
