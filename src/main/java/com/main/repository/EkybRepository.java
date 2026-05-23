package com.main.repository;

import org.springframework.stereotype.Repository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;

import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;

@Repository
public class EkybRepository {

    @Value("${cms.db.schema-name}")
    private String dbSchema;

    @Value("${cms.db.catalog-name}")
    private String catalogName;

    private JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    private SimpleJdbcCall ekybCreateCall;

    public EkybRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = new ObjectMapper();
    }

    @PostConstruct
    public void init() {
        this.ekybCreateCall = new SimpleJdbcCall(jdbcTemplate)
                .withSchemaName(dbSchema)
                .withCatalogName(catalogName)
                .withProcedureName("EKYB_CREATE")
                .declareParameters(
                        new SqlParameter("IN_SINGLE_ID", Types.VARCHAR),
                        new SqlParameter("IN_TIN", Types.VARCHAR),
                        new SqlParameter("IN_COMPANY_NAME_KH", Types.NVARCHAR),
                        new SqlParameter("IN_COMPANY_NAME_EN", Types.VARCHAR),
                        new SqlParameter("IN_DIR_LIST_JSON", Types.VARCHAR),
                        new SqlParameter("IN_TYPE", Types.NVARCHAR),
                        new SqlParameter("IN_NOTE", Types.NVARCHAR),
                        new SqlParameter("IN_ERROR_DETAIL", Types.VARCHAR),
                        new SqlOutParameter("P_RESULT", Types.VARCHAR),
                        new SqlOutParameter("OUT_ID", Types.VARCHAR));
    }

    public String createEkyb(String singleId, String tin, String nameKH, String nameEn, String dirListJson,
            String type, String note, String errorDetail) {
        MapSqlParameterSource in = new MapSqlParameterSource()
                .addValue("IN_SINGLE_ID", singleId)
                .addValue("IN_TIN", tin)
                .addValue("IN_COMPANY_NAME_KH", nameKH)
                .addValue("IN_COMPANY_NAME_EN", nameEn)
                .addValue("IN_DIR_LIST_JSON", dirListJson)
                .addValue("IN_TYPE", type)
                .addValue("IN_NOTE", note)
                .addValue("IN_ERROR_DETAIL", errorDetail);

        Map<String, Object> responseMap = new HashMap<>();
        try {
            Map<String, Object> out = ekybCreateCall.execute(in);

            responseMap.put("p_result", out.get("P_RESULT"));
            responseMap.put("out_id", out.get("OUT_ID"));

            // Return as JSON string
            return objectMapper.writeValueAsString(responseMap);
        } catch (Exception e) {
            // Build error JSON manually to avoid further exceptions
            return String.format("{\"p_result\": \"exception: %s\", \"out_entries_id\": null}",
                    e.getMessage().replace("\"", "\\\""));
        }
    }

}
