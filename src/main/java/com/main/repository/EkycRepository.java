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
public class EkycRepository {

    @Value("${cms.db.schema-name}")
    private String dbSchema;

    @Value("${cms.db.catalog-name}")
    private String catalogName;

    private JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    private SimpleJdbcCall ekycCreateCall;

    public EkycRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = new ObjectMapper();
    }

    @PostConstruct
    public void init() {
        this.ekycCreateCall = new SimpleJdbcCall(jdbcTemplate)
                .withSchemaName(dbSchema)
                .withCatalogName(catalogName)
                .withProcedureName("EKYC_CREATE")
                .declareParameters(
                        new SqlParameter("IN_ID_NUMBER", Types.VARCHAR),
                        new SqlParameter("IN_FIRST_NAME_KH", Types.NVARCHAR),
                        new SqlParameter("IN_LAST_NAME_KH", Types.NVARCHAR),
                        new SqlParameter("IN_FIRST_NAME_EN", Types.VARCHAR),
                        new SqlParameter("IN_LAST_NAME_EN", Types.VARCHAR),
                        new SqlParameter("IN_GENDER", Types.VARCHAR),
                        new SqlParameter("IN_DOB", Types.NVARCHAR),
                        new SqlParameter("IN_ISSUED_DATE", Types.NVARCHAR),
                        new SqlParameter("IN_EXPIRED_DATE", Types.VARCHAR),
                        new SqlParameter("IN_NOTE", Types.VARCHAR),
                        new SqlParameter("IN_TYPE", Types.VARCHAR),
                        new SqlParameter("IN_SELFIE_PATH", Types.NVARCHAR),
                        new SqlParameter("IN_ERROR_DETAIL", Types.VARCHAR),
                        new SqlOutParameter("P_RESULT", Types.VARCHAR),
                        new SqlOutParameter("OUT_ID", Types.VARCHAR));
    }

    public String createEkyc(String idNumber, String firstNameKh, String lastNameKh, String firstNameEn,
            String lastNameEn, String gender, String dob, String issuedDae, String expiredDate, String note,
            String type, String selfiePath, String errorDetail) {
        MapSqlParameterSource in = new MapSqlParameterSource()
                .addValue("IN_ID_NUMBER", idNumber)
                .addValue("IN_FIRST_NAME_KH", firstNameKh)
                .addValue("IN_LAST_NAME_KH", lastNameKh)
                .addValue("IN_FIRST_NAME_EN", firstNameEn)
                .addValue("IN_LAST_NAME_EN", lastNameEn)
                .addValue("IN_GENDER", gender)
                .addValue("IN_DOB", dob)
                .addValue("IN_ISSUED_DATE", issuedDae)
                .addValue("IN_EXPIRED_DATE", expiredDate)
                .addValue("IN_NOTE", note)
                .addValue("IN_TYPE", type)
                .addValue("IN_SELFIE_PATH", selfiePath)
                .addValue("IN_ERROR_DETAIL", errorDetail);

        Map<String, Object> responseMap = new HashMap<>();
        try {
            Map<String, Object> out = ekycCreateCall.execute(in);

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
