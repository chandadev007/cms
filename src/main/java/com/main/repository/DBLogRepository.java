package com.main.repository;

import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;

@Repository
public class DBLogRepository {
    @Value("${cms.db.schema-name}")
    private String dbSchema;

    @Value("${cms.db.catalog-name}")
    private String catalogName;

    private JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    private SimpleJdbcCall ekycLogCall, ekybLogCall;

    public DBLogRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = new ObjectMapper();
    }

    @PostConstruct
    public void init() {
        this.ekycLogCall = new SimpleJdbcCall(jdbcTemplate)
                .withSchemaName(dbSchema)
                .withCatalogName(catalogName)
                .withProcedureName("EKYC_LOG")
                .declareParameters(
                        new SqlParameter("IN_ACTION_NAME", Types.VARCHAR),
                        new SqlParameter("IN_ACTION_TYPE", Types.VARCHAR),
                        new SqlParameter("IN_TABLE_ID", Types.VARCHAR),
                        new SqlParameter("IN_UNIT_ID", Types.VARCHAR),
                        new SqlParameter("IN_USER_ID", Types.VARCHAR),
                        new SqlParameter("IN_USER_NAME", Types.VARCHAR),
                        new SqlOutParameter("P_RESULT", Types.VARCHAR));

        this.ekybLogCall = new SimpleJdbcCall(jdbcTemplate)
                .withSchemaName(dbSchema)
                .withCatalogName(catalogName)
                .withProcedureName("EKYB_LOG")
                .declareParameters(
                        new SqlParameter("IN_ACTION_NAME", Types.VARCHAR),
                        new SqlParameter("IN_ACTION_TYPE", Types.VARCHAR),
                        new SqlParameter("IN_TABLE_ID", Types.VARCHAR),
                        new SqlParameter("IN_UNIT_ID", Types.VARCHAR),
                        new SqlParameter("IN_USER_ID", Types.VARCHAR),
                        new SqlParameter("IN_USER_NAME", Types.VARCHAR),
                        new SqlOutParameter("P_RESULT", Types.VARCHAR));
    }

    public String createEkycLog(String actionName, String actionType, String tableId, String unitId, String userId,
            String userName) {
        MapSqlParameterSource in = new MapSqlParameterSource()
                .addValue("IN_ACTION_NAME", actionName)
                .addValue("IN_ACTION_TYPE", actionType)
                .addValue("IN_TABLE_ID", tableId)
                .addValue("IN_UNIT_ID", unitId)
                .addValue("IN_USER_ID", userId)
                .addValue("IN_USER_NAME", userName);

        Map<String, Object> responseMap = new HashMap<>();
        try {
            Map<String, Object> out = ekycLogCall.execute(in);

            responseMap.put("p_result", out.get("P_RESULT"));

            // Return as JSON string
            return objectMapper.writeValueAsString(responseMap);
        } catch (Exception e) {
            // Build error JSON manually to avoid further exceptions
            return String.format("{\"p_result\": \"exception: %s\", \"out_entries_id\": null}",
                    e.getMessage().replace("\"", "\\\""));
        }
    }

    public String createEkybLog(String actionName, String actionType, String tableId, String unitId, String userId,
            String userName) {
        MapSqlParameterSource in = new MapSqlParameterSource()
                .addValue("IN_ACTION_NAME", actionName)
                .addValue("IN_ACTION_TYPE", actionType)
                .addValue("IN_TABLE_ID", tableId)
                .addValue("IN_UNIT_ID", unitId)
                .addValue("IN_USER_ID", userId)
                .addValue("IN_USER_NAME", userName);

        Map<String, Object> responseMap = new HashMap<>();
        try {
            Map<String, Object> out = ekybLogCall.execute(in);

            responseMap.put("p_result", out.get("P_RESULT"));

            // Return as JSON string
            return objectMapper.writeValueAsString(responseMap);
        } catch (Exception e) {
            // Build error JSON manually to avoid further exceptions
            return String.format("{\"p_result\": \"exception: %s\", \"out_entries_id\": null}",
                    e.getMessage().replace("\"", "\\\""));
        }
    }

}
