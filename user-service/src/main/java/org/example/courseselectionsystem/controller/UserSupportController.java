package org.example.courseselectionsystem.controller;

import org.example.courseselectionsystem.common.Result;
import org.example.courseselectionsystem.exception.BusinessException;
import org.example.courseselectionsystem.vo.PageResult;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1")
public class UserSupportController {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public UserSupportController(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping({"/messages", "/messages/list"})
    public Result<PageResult<Map<String, Object>>> messages(@RequestParam Map<String, String> params) {
        int pageNum = positiveInt(params.get("pageNum"), 1);
        int pageSize = Math.min(positiveInt(params.get("pageSize"), 10), 100);
        MapSqlParameterSource source = pageSource(pageNum, pageSize);
        String where = messageWhere(params, source);
        long total = jdbcTemplate.queryForObject("select count(*) from message_notification" + where, source, Long.class);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                select id,
                       recipient_id recipientId,
                       recipient_type recipientType,
                       title,
                       content,
                       message_type messageType,
                       is_read isRead,
                       created_at createdAt,
                       updated_at updatedAt
                  from message_notification
                """ + where + " order by created_at desc limit :pageSize offset :offset", source);
        return Result.success(new PageResult<>(pageNum, pageSize, total, rows));
    }

    @GetMapping("/messages/{id}")
    public Result<Map<String, Object>> message(@PathVariable Long id) {
        return Result.success(requireMessage(id));
    }

    @PostMapping("/messages")
    public Result<Map<String, Object>> createMessage(@RequestBody Map<String, Object> body) {
        MapSqlParameterSource source = new MapSqlParameterSource()
                .addValue("recipientId", requiredLong(body.get("recipientId"), "接收人不能为空"))
                .addValue("recipientType", requiredInt(body.get("recipientType"), "接收人类型不能为空"))
                .addValue("title", requiredText(body.get("title"), "消息标题不能为空"))
                .addValue("content", requiredText(body.get("content"), "消息内容不能为空"))
                .addValue("messageType", text(body.get("messageType")) == null ? "system" : text(body.get("messageType")))
                .addValue("isRead", intValue(body.get("isRead"), 0));
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update("""
                insert into message_notification
                    (recipient_id, recipient_type, title, content, message_type, is_read)
                values
                    (:recipientId, :recipientType, :title, :content, :messageType, :isRead)
                """, source, keyHolder, new String[]{"id"});
        Number key = Objects.requireNonNull(keyHolder.getKey(), "message id");
        return Result.success("消息创建成功", requireMessage(key.longValue()));
    }

    @PutMapping("/messages/{id}")
    public Result<Map<String, Object>> updateMessage(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        requireMessage(id);
        MapSqlParameterSource source = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("title", requiredText(body.get("title"), "消息标题不能为空"))
                .addValue("content", requiredText(body.get("content"), "消息内容不能为空"))
                .addValue("messageType", text(body.get("messageType")) == null ? "system" : text(body.get("messageType")))
                .addValue("isRead", intValue(body.get("isRead"), 0));
        jdbcTemplate.update("""
                update message_notification
                   set title = :title,
                       content = :content,
                       message_type = :messageType,
                       is_read = :isRead
                 where id = :id
                """, source);
        return Result.success("消息更新成功", requireMessage(id));
    }

    @PutMapping("/messages/{id}/read")
    public Result<Map<String, Object>> readMessage(@PathVariable Long id) {
        requireMessage(id);
        jdbcTemplate.update("update message_notification set is_read = 1 where id = :id", new MapSqlParameterSource("id", id));
        return Result.success("消息已读", requireMessage(id));
    }

    @DeleteMapping("/messages/{id}")
    public Result<Boolean> deleteMessage(@PathVariable Long id) {
        int affected = jdbcTemplate.update("delete from message_notification where id = :id", new MapSqlParameterSource("id", id));
        return affected > 0 ? Result.success(true) : Result.notFound("消息不存在");
    }

    @GetMapping({"/operation-logs", "/operation-logs/list"})
    public Result<PageResult<Map<String, Object>>> operationLogs(@RequestParam Map<String, String> params) {
        int pageNum = positiveInt(params.get("pageNum"), 1);
        int pageSize = Math.min(positiveInt(params.get("pageSize"), 10), 100);
        MapSqlParameterSource source = pageSource(pageNum, pageSize);
        String where = logWhere(params, source);
        long total = jdbcTemplate.queryForObject("select count(*) from operation_log" + where, source, Long.class);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                select id,
                       operator_type operatorType,
                       operator_id operatorId,
                       operator_name operatorName,
                       operation_type operationType,
                       operation_desc operationDesc,
                       operation_time operationTime,
                       ip_address ipAddress,
                       status
                  from operation_log
                """ + where + " order by operation_time desc limit :pageSize offset :offset", source);
        return Result.success(new PageResult<>(pageNum, pageSize, total, rows));
    }

    @GetMapping("/operation-logs/{id}")
    public Result<Map<String, Object>> operationLog(@PathVariable Long id) {
        return Result.success(requireOperationLog(id));
    }

    @PostMapping("/operation-logs")
    public Result<Map<String, Object>> createOperationLog(@RequestBody Map<String, Object> body) {
        MapSqlParameterSource source = new MapSqlParameterSource()
                .addValue("operatorType", requiredInt(body.get("operatorType"), "操作人类型不能为空"))
                .addValue("operatorId", requiredLong(body.get("operatorId"), "操作人不能为空"))
                .addValue("operatorName", requiredText(body.get("operatorName"), "操作人姓名不能为空"))
                .addValue("operationType", requiredText(body.get("operationType"), "操作类型不能为空"))
                .addValue("operationDesc", requiredText(body.get("operationDesc"), "操作描述不能为空"))
                .addValue("ipAddress", text(body.get("ipAddress")))
                .addValue("status", intValue(body.get("status"), 1));
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update("""
                insert into operation_log
                    (operator_type, operator_id, operator_name, operation_type, operation_desc, ip_address, status)
                values
                    (:operatorType, :operatorId, :operatorName, :operationType, :operationDesc, :ipAddress, :status)
                """, source, keyHolder, new String[]{"id"});
        Number key = Objects.requireNonNull(keyHolder.getKey(), "operation log id");
        return Result.success("日志创建成功", requireOperationLog(key.longValue()));
    }

    @DeleteMapping("/operation-logs/{id}")
    public Result<Boolean> deleteOperationLog(@PathVariable Long id) {
        int affected = jdbcTemplate.update("delete from operation_log where id = :id", new MapSqlParameterSource("id", id));
        return affected > 0 ? Result.success(true) : Result.notFound("日志不存在");
    }

    @GetMapping({"/system-settings", "/system-settings/list"})
    public Result<List<Map<String, Object>>> systemSettings() {
        return Result.success(jdbcTemplate.queryForList("""
                select id,
                       setting_key settingKey,
                       setting_value settingValue,
                       description,
                       created_at createdAt,
                       updated_at updatedAt
                  from system_setting
                 order by setting_key
                """, new MapSqlParameterSource()));
    }

    @GetMapping("/system-settings/{id}")
    public Result<Map<String, Object>> systemSetting(@PathVariable Long id) {
        return Result.success(requireSystemSetting(id));
    }

    @GetMapping("/system-settings/key/{key}")
    public Result<Map<String, Object>> systemSettingByKey(@PathVariable String key) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                select id,
                       setting_key settingKey,
                       setting_value settingValue,
                       description,
                       created_at createdAt,
                       updated_at updatedAt
                  from system_setting
                 where setting_key = :key
                """, new MapSqlParameterSource("key", key));
        return rows.isEmpty() ? Result.notFound("系统设置不存在") : Result.success(rows.get(0));
    }

    @PostMapping("/system-settings")
    public Result<Map<String, Object>> createSystemSetting(@RequestBody Map<String, Object> body) {
        MapSqlParameterSource source = settingParams(body);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update("""
                insert into system_setting (setting_key, setting_value, description)
                values (:settingKey, :settingValue, :description)
                """, source, keyHolder, new String[]{"id"});
        Number key = Objects.requireNonNull(keyHolder.getKey(), "setting id");
        return Result.success("系统设置创建成功", requireSystemSetting(key.longValue()));
    }

    @PutMapping("/system-settings/{id}")
    public Result<Map<String, Object>> updateSystemSetting(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        requireSystemSetting(id);
        MapSqlParameterSource source = settingParams(body).addValue("id", id);
        jdbcTemplate.update("""
                update system_setting
                   set setting_key = :settingKey,
                       setting_value = :settingValue,
                       description = :description
                 where id = :id
                """, source);
        return Result.success("系统设置更新成功", requireSystemSetting(id));
    }

    @PutMapping("/system-settings/key/{key}")
    public Result<Map<String, Object>> upsertSystemSetting(@PathVariable String key, @RequestBody Map<String, Object> body) {
        String value = requiredText(body.get("settingValue"), "设置值不能为空");
        String description = text(body.get("description"));
        List<Long> existingIds = jdbcTemplate.queryForList("select id from system_setting where setting_key = :key",
                new MapSqlParameterSource("key", key), Long.class);
        if (existingIds.isEmpty()) {
            return createSystemSetting(Map.of("settingKey", key, "settingValue", value, "description", description == null ? "" : description));
        }
        Long id = existingIds.get(0);
        jdbcTemplate.update("""
                update system_setting
                   set setting_value = :value,
                       description = :description
                 where id = :id
                """, new MapSqlParameterSource().addValue("id", id).addValue("value", value).addValue("description", description));
        return Result.success("系统设置更新成功", requireSystemSetting(id));
    }

    @DeleteMapping("/system-settings/{id}")
    public Result<Boolean> deleteSystemSetting(@PathVariable Long id) {
        int affected = jdbcTemplate.update("delete from system_setting where id = :id", new MapSqlParameterSource("id", id));
        return affected > 0 ? Result.success(true) : Result.notFound("系统设置不存在");
    }

    private Map<String, Object> requireMessage(Long id) {
        return requireOne("""
                select id,
                       recipient_id recipientId,
                       recipient_type recipientType,
                       title,
                       content,
                       message_type messageType,
                       is_read isRead,
                       created_at createdAt,
                       updated_at updatedAt
                  from message_notification
                 where id = :id
                """, id, "消息不存在");
    }

    private Map<String, Object> requireOperationLog(Long id) {
        return requireOne("""
                select id,
                       operator_type operatorType,
                       operator_id operatorId,
                       operator_name operatorName,
                       operation_type operationType,
                       operation_desc operationDesc,
                       operation_time operationTime,
                       ip_address ipAddress,
                       status
                  from operation_log
                 where id = :id
                """, id, "日志不存在");
    }

    private Map<String, Object> requireSystemSetting(Long id) {
        return requireOne("""
                select id,
                       setting_key settingKey,
                       setting_value settingValue,
                       description,
                       created_at createdAt,
                       updated_at updatedAt
                  from system_setting
                 where id = :id
                """, id, "系统设置不存在");
    }

    private Map<String, Object> requireOne(String sql, Long id, String message) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, new MapSqlParameterSource("id", id));
        if (rows.isEmpty()) {
            throw new BusinessException(Result.NOT_FOUND, message);
        }
        return rows.get(0);
    }

    private String messageWhere(Map<String, String> params, MapSqlParameterSource source) {
        List<String> filters = new ArrayList<>();
        addLongFilter(filters, source, "recipientId", "recipient_id", params.get("recipientId"));
        addIntFilter(filters, source, "recipientType", "recipient_type", params.get("recipientType"));
        addIntFilter(filters, source, "isRead", "is_read", params.get("isRead"));
        String type = text(params.get("messageType"));
        if (type != null && !"all".equalsIgnoreCase(type)) {
            filters.add("message_type = :messageType");
            source.addValue("messageType", type);
        }
        String keyword = text(params.get("keyword"));
        if (keyword != null) {
            filters.add("(title like :keyword or content like :keyword)");
            source.addValue("keyword", "%" + keyword + "%");
        }
        return filters.isEmpty() ? "" : " where " + String.join(" and ", filters);
    }

    private String logWhere(Map<String, String> params, MapSqlParameterSource source) {
        List<String> filters = new ArrayList<>();
        addIntFilter(filters, source, "operatorType", "operator_type", params.get("operatorType"));
        addIntFilter(filters, source, "status", "status", params.get("status"));
        String type = text(params.get("operationType"));
        if (type != null && !"all".equalsIgnoreCase(type)) {
            filters.add("operation_type = :operationType");
            source.addValue("operationType", type);
        }
        String keyword = text(params.get("keyword"));
        if (keyword != null) {
            filters.add("(operator_name like :keyword or operation_type like :keyword or operation_desc like :keyword)");
            source.addValue("keyword", "%" + keyword + "%");
        }
        String startDate = text(params.get("startDate"));
        if (startDate != null) {
            filters.add("operation_time >= :startDate");
            source.addValue("startDate", startDate);
        }
        String endDate = text(params.get("endDate"));
        if (endDate != null) {
            filters.add("operation_time < date_add(:endDate, interval 1 day)");
            source.addValue("endDate", endDate);
        }
        return filters.isEmpty() ? "" : " where " + String.join(" and ", filters);
    }

    private MapSqlParameterSource settingParams(Map<String, Object> body) {
        return new MapSqlParameterSource()
                .addValue("settingKey", requiredText(body.get("settingKey"), "设置键不能为空"))
                .addValue("settingValue", requiredText(body.get("settingValue"), "设置值不能为空"))
                .addValue("description", text(body.get("description")));
    }

    private MapSqlParameterSource pageSource(int pageNum, int pageSize) {
        return new MapSqlParameterSource()
                .addValue("offset", (pageNum - 1) * pageSize)
                .addValue("pageSize", pageSize);
    }

    private void addLongFilter(List<String> filters, MapSqlParameterSource source, String name, String column, String value) {
        Long parsed = longValue(value);
        if (parsed != null) {
            filters.add(column + " = :" + name);
            source.addValue(name, parsed);
        }
    }

    private void addIntFilter(List<String> filters, MapSqlParameterSource source, String name, String column, String value) {
        Integer parsed = intValue(value, null);
        if (parsed != null) {
            filters.add(column + " = :" + name);
            source.addValue(name, parsed);
        }
    }

    private int positiveInt(String value, int defaultValue) {
        Integer parsed = intValue(value, defaultValue);
        return parsed == null || parsed < 1 ? defaultValue : parsed;
    }

    private Integer intValue(Object value, Integer defaultValue) {
        String text = text(value);
        if (text == null || "all".equalsIgnoreCase(text)) {
            return defaultValue;
        }
        return Integer.parseInt(text);
    }

    private Integer requiredInt(Object value, String message) {
        Integer parsed = intValue(value, null);
        if (parsed == null) {
            throw new BusinessException(Result.PARAM_ERROR, message);
        }
        return parsed;
    }

    private Long longValue(Object value) {
        String text = text(value);
        if (text == null || "all".equalsIgnoreCase(text)) {
            return null;
        }
        return Long.parseLong(text);
    }

    private Long requiredLong(Object value, String message) {
        Long parsed = longValue(value);
        if (parsed == null) {
            throw new BusinessException(Result.PARAM_ERROR, message);
        }
        return parsed;
    }

    private String requiredText(Object value, String message) {
        String text = text(value);
        if (text == null) {
            throw new BusinessException(Result.PARAM_ERROR, message);
        }
        return text;
    }

    private String text(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return StringUtils.hasText(text) ? text : null;
    }
}
