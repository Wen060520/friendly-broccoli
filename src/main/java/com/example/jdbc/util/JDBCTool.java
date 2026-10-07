package com.example.jdbc.util;

import com.example.jdbc.annotation.Column;
import com.example.jdbc.annotation.Id;
import com.example.jdbc.annotation.Table;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A small reflection-based ORM built on top of JDBC.
 */
public final class JDBCTool {
    private static final Map<Class<?>, EntityMeta> META_CACHE = new ConcurrentHashMap<>();

    private JDBCTool() {
    }

    /**
     * Converts every row in a ResultSet into an instance of clazz.
     */
    public static <T> List<T> resultSetToList(ResultSet rs, Class<T> clazz) throws SQLException {
        EntityMeta meta = resolveMeta(clazz);
        List<T> result = new ArrayList<>();
        ResultSetMetaData resultSetMetaData = rs.getMetaData();
        Map<String, FieldMapping> mappingsByColumn = meta.mappingsByColumn();

        while (rs.next()) {
            T instance = newInstance(clazz);
            for (int index = 1; index <= resultSetMetaData.getColumnCount(); index++) {
                String columnName = resultSetMetaData.getColumnLabel(index);
                FieldMapping mapping = mappingsByColumn.get(normalize(columnName));
                if (mapping != null) {
                    Object value = rs.getObject(index);
                    Object converted = convertValue(value, mapping.field());
                    writeField(instance, mapping.field(), converted);
                }
            }
            result.add(instance);
        }
        return result;
    }

    /**
     * Inserts an entity and assigns the generated primary key back to the object.
     */
    public static <T> int save(T obj, Connection connection) throws SQLException {
        Objects.requireNonNull(obj, "obj must not be null");
        Objects.requireNonNull(connection, "connection must not be null");

        EntityMeta meta = resolveMeta(obj.getClass());
        Object idValue = readField(obj, meta.idField());
        boolean includeId = idValue != null;
        List<FieldMapping> insertMappings = meta.mappings().stream()
                .filter(mapping -> includeId || !mapping.field().equals(meta.idField()))
                .toList();

        String columns = insertMappings.stream()
                .map(FieldMapping::columnName)
                .reduce((left, right) -> left + ", " + right)
                .orElseThrow(() -> new IllegalStateException("No insertable fields found"));
        String placeholders = String.join(", ", java.util.Collections.nCopies(insertMappings.size(), "?"));
        String sql = "INSERT INTO " + meta.tableName() + " (" + columns + ") VALUES (" + placeholders + ")";

        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindValues(statement, insertMappings, obj);
            int affectedRows = statement.executeUpdate();
            if (!includeId && affectedRows > 0) {
                try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        writeField(obj, meta.idField(),
                                convertValue(generatedKeys.getObject(1), meta.idField()));
                    }
                }
            }
            return affectedRows;
        }
    }

    /**
     * Updates an entity by its primary key.
     */
    public static <T> int update(T obj, Connection connection) throws SQLException {
        Objects.requireNonNull(obj, "obj must not be null");
        Objects.requireNonNull(connection, "connection must not be null");

        EntityMeta meta = resolveMeta(obj.getClass());
        Object idValue = readRequiredId(obj, meta);
        List<FieldMapping> updateMappings = meta.mappings().stream()
                .filter(mapping -> !mapping.field().equals(meta.idField()))
                .toList();

        String assignments = updateMappings.stream()
                .map(mapping -> mapping.columnName() + " = ?")
                .reduce((left, right) -> left + ", " + right)
                .orElseThrow(() -> new IllegalStateException("No updatable fields found"));
        String sql = "UPDATE " + meta.tableName() + " SET " + assignments
                + " WHERE " + meta.idColumn() + " = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bindValues(statement, updateMappings, obj);
            statement.setObject(updateMappings.size() + 1, idValue);
            return statement.executeUpdate();
        }
    }

    /**
     * Deletes an entity by its primary key.
     */
    public static <T> int delete(T obj, Connection connection) throws SQLException {
        Objects.requireNonNull(obj, "obj must not be null");
        Objects.requireNonNull(connection, "connection must not be null");

        EntityMeta meta = resolveMeta(obj.getClass());
        Object idValue = readRequiredId(obj, meta);
        String sql = "DELETE FROM " + meta.tableName() + " WHERE " + meta.idColumn() + " = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, idValue);
            return statement.executeUpdate();
        }
    }

    /**
     * Finds one entity by its primary key.
     */
    public static <T> T getOneById(String id, Class<T> clazz, Connection connection) throws SQLException {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(clazz, "clazz must not be null");
        Objects.requireNonNull(connection, "connection must not be null");

        EntityMeta meta = resolveMeta(clazz);
        String sql = "SELECT * FROM " + meta.tableName() + " WHERE " + meta.idColumn() + " = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, convertValue(id, meta.idField()));
            try (ResultSet resultSet = statement.executeQuery()) {
                List<T> result = resultSetToList(resultSet, clazz);
                return result.isEmpty() ? null : result.get(0);
            }
        }
    }

    private static EntityMeta resolveMeta(Class<?> clazz) {
        return META_CACHE.computeIfAbsent(clazz, JDBCTool::createMeta);
    }

    private static EntityMeta createMeta(Class<?> clazz) {
        Table table = clazz.getAnnotation(Table.class);
        String tableName = table == null ? camelToSnake(clazz.getSimpleName()) : table.value();

        Field idField = null;
        List<FieldMapping> mappings = new ArrayList<>();
        List<Field> fields = Arrays.stream(clazz.getDeclaredFields())
                .filter(field -> !field.isSynthetic())
                .sorted(Comparator.comparing(Field::getName))
                .toList();

        for (Field field : fields) {
            Column column = field.getAnnotation(Column.class);
            Id id = field.getAnnotation(Id.class);
            if (column == null && id == null) {
                continue;
            }

            String columnName;
            if (id != null && !id.value().isBlank()) {
                columnName = id.value();
            } else if (column != null && !column.value().isBlank()) {
                columnName = column.value();
            } else {
                columnName = camelToSnake(field.getName());
            }

            field.setAccessible(true);
            FieldMapping mapping = new FieldMapping(field, columnName);
            mappings.add(mapping);
            if (id != null) {
                if (idField != null) {
                    throw new IllegalArgumentException("Multiple @Id fields in " + clazz.getName());
                }
                idField = field;
            }
        }

        if (idField == null) {
            throw new IllegalArgumentException("No @Id field in " + clazz.getName());
        }

        Field resolvedIdField = idField;
        FieldMapping idMapping = mappings.stream()
                .filter(mapping -> mapping.field().equals(resolvedIdField))
                .findFirst()
                .orElseThrow();
        return new EntityMeta(tableName, idMapping.columnName(), resolvedIdField, List.copyOf(mappings));
    }

    private static <T> T newInstance(Class<T> clazz) throws SQLException {
        try {
            Constructor<T> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException exception) {
            throw new SQLException("Cannot create " + clazz.getName()
                    + "; a no-argument constructor is required", exception);
        }
    }

    private static Object readField(Object target, Field field) throws SQLException {
        try {
            return field.get(target);
        } catch (IllegalAccessException exception) {
            throw new SQLException("Cannot read field " + field.getName(), exception);
        }
    }

    private static Object readRequiredId(Object target, EntityMeta meta) throws SQLException {
        Object value = readField(target, meta.idField());
        if (value == null || (value instanceof Number number && number.longValue() == 0)) {
            throw new IllegalArgumentException(meta.idField().getName() + " is required");
        }
        return value;
    }

    private static void writeField(Object target, Field field, Object value) throws SQLException {
        try {
            field.set(target, value);
        } catch (IllegalAccessException exception) {
            throw new SQLException("Cannot write field " + field.getName(), exception);
        }
    }

    private static void bindValues(PreparedStatement statement,
                                   List<FieldMapping> mappings,
                                   Object target) throws SQLException {
        for (int index = 0; index < mappings.size(); index++) {
            Field field = mappings.get(index).field();
            statement.setObject(index + 1, readField(target, field));
        }
    }

    private static Object convertValue(Object value, Field targetField) {
        if (value == null) {
            return null;
        }

        Class<?> targetType = wrapPrimitive(targetField.getType());
        if (targetType.isInstance(value)) {
            return value;
        }
        if (targetType == String.class) {
            return value.toString();
        }
        if (targetType == Boolean.class) {
            if (value instanceof Number number) {
                return number.intValue() != 0;
            }
            return Boolean.parseBoolean(value.toString());
        }
        if (Number.class.isAssignableFrom(targetType)) {
            return convertNumber(value, targetType);
        }
        if (targetType == LocalDate.class) {
            if (value instanceof Date date) {
                return date.toLocalDate();
            }
            if (value instanceof Timestamp timestamp) {
                return timestamp.toLocalDateTime().toLocalDate();
            }
            return LocalDate.parse(value.toString());
        }
        if (targetType == LocalDateTime.class) {
            if (value instanceof Timestamp timestamp) {
                return timestamp.toLocalDateTime();
            }
            return LocalDateTime.parse(value.toString().replace(' ', 'T'));
        }
        if (targetType == LocalTime.class) {
            if (value instanceof Time time) {
                return time.toLocalTime();
            }
            return LocalTime.parse(value.toString());
        }
        if (targetType == java.util.Date.class && value instanceof Date date) {
            return new java.util.Date(date.getTime());
        }
        if (targetType.isEnum()) {
            @SuppressWarnings({"rawtypes", "unchecked"})
            Object enumValue = Enum.valueOf((Class<? extends Enum>) targetType, value.toString());
            return enumValue;
        }
        return value;
    }

    private static Object convertNumber(Object value, Class<?> targetType) {
        String text = value.toString();
        if (targetType == Byte.class) {
            return Byte.valueOf(text);
        }
        if (targetType == Short.class) {
            return Short.valueOf(text);
        }
        if (targetType == Integer.class) {
            return new BigDecimal(text).intValue();
        }
        if (targetType == Long.class) {
            return new BigDecimal(text).longValue();
        }
        if (targetType == Float.class) {
            return new BigDecimal(text).floatValue();
        }
        if (targetType == Double.class) {
            return new BigDecimal(text).doubleValue();
        }
        if (targetType == BigDecimal.class) {
            return new BigDecimal(text);
        }
        throw new IllegalArgumentException("Unsupported number type: " + targetType.getName());
    }

    private static Class<?> wrapPrimitive(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }
        if (type == int.class) {
            return Integer.class;
        }
        if (type == long.class) {
            return Long.class;
        }
        if (type == boolean.class) {
            return Boolean.class;
        }
        if (type == double.class) {
            return Double.class;
        }
        if (type == float.class) {
            return Float.class;
        }
        if (type == short.class) {
            return Short.class;
        }
        if (type == byte.class) {
            return Byte.class;
        }
        if (type == char.class) {
            return Character.class;
        }
        return type;
    }

    private static String camelToSnake(String value) {
        return value.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
    }

    private static String normalize(String value) {
        return value.replace("\"", "")
                .replace("`", "")
                .toLowerCase(Locale.ROOT);
    }

    private record FieldMapping(Field field, String columnName) {
    }

    private record EntityMeta(String tableName,
                              String idColumn,
                              Field idField,
                              List<FieldMapping> mappings) {
        private Map<String, FieldMapping> mappingsByColumn() {
            Map<String, FieldMapping> result = new HashMap<>();
            for (FieldMapping mapping : mappings) {
                result.put(normalize(mapping.columnName()), mapping);
            }
            return result;
        }
    }
}
