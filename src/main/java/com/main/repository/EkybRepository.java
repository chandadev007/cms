package com.main.repository;

import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.main.model.Ekyb;
import com.main.model.History;
import com.main.model.HistoryAction;
import com.main.utilities.StatusClassification;

import jakarta.annotation.PostConstruct;
import java.sql.Types;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

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

        private SimpleJdbcCall ekybCreateCall, ekybProcessingCall, ekybFinalStatusCall, ekybFinalDirectorCall;

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
                                                new SqlParameter("IN_APP_CHANNEL", Types.VARCHAR),
                                                new SqlOutParameter("P_RESULT", Types.VARCHAR),
                                                new SqlOutParameter("OUT_ID", Types.VARCHAR));

                this.ekybProcessingCall = new SimpleJdbcCall(jdbcTemplate)
                                .withSchemaName(dbSchema)
                                .withCatalogName(catalogName)
                                .withProcedureName("EKYB_PROCESSING")
                                .declareParameters(
                                                new SqlParameter("IN_ID", Types.VARCHAR),
                                                new SqlOutParameter("P_RESULT", Types.VARCHAR));

                this.ekybFinalStatusCall = new SimpleJdbcCall(jdbcTemplate)
                                .withSchemaName(dbSchema)
                                .withCatalogName(catalogName)
                                .withProcedureName("EKYB_FINAL_STATUS")
                                .declareParameters(
                                                new SqlParameter("IN_ID", Types.VARCHAR),
                                                new SqlParameter("IN_STATUS", Types.VARCHAR),
                                                new SqlParameter("IN_SCORE", Types.VARCHAR),
                                                new SqlParameter("IN_ERROR_DETAIL", Types.VARCHAR),
                                                new SqlOutParameter("P_RESULT", Types.VARCHAR));

                this.ekybFinalDirectorCall = new SimpleJdbcCall(jdbcTemplate)
                                .withSchemaName(dbSchema)
                                .withCatalogName(catalogName)
                                .withProcedureName("EKYB_FINAL_DIRECTOR")
                                .declareParameters(
                                                new SqlParameter("IN_ID", Types.VARCHAR),
                                                new SqlParameter("IN_STATUS", Types.VARCHAR),
                                                new SqlParameter("IN_SCORE", Types.VARCHAR),
                                                new SqlParameter("IN_ERROR_DETAIL", Types.VARCHAR),
                                                new SqlParameter("IN_RES_DIR_LIST_JSON", Types.VARCHAR),
                                                new SqlOutParameter("P_RESULT", Types.VARCHAR));
        }

        public String createEkyb(String singleId, String tin, String nameKH, String nameEn, String dirListJson,
                        String type, String note, String errorDetail, String appChannel) {
                MapSqlParameterSource in = new MapSqlParameterSource()
                                .addValue("IN_SINGLE_ID", singleId)
                                .addValue("IN_TIN", tin)
                                .addValue("IN_COMPANY_NAME_KH", nameKH)
                                .addValue("IN_COMPANY_NAME_EN", nameEn)
                                .addValue("IN_DIR_LIST_JSON", dirListJson)
                                .addValue("IN_TYPE", type)
                                .addValue("IN_NOTE", note)
                                .addValue("IN_ERROR_DETAIL", errorDetail)
                                .addValue("IN_APP_CHANNEL", appChannel);

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

        public String updateToProcessing(String id) {
                MapSqlParameterSource in = new MapSqlParameterSource()
                                .addValue("IN_ID", id);

                Map<String, Object> responseMap = new HashMap<>();
                try {
                        Map<String, Object> out = ekybProcessingCall.execute(in);
                        responseMap.put("p_result", out.get("P_RESULT"));
                        // Return as JSON string
                        return objectMapper.writeValueAsString(responseMap);
                } catch (Exception e) {
                        // Build error JSON manually to avoid further exceptions
                        return String.format("{\"p_result\": \"exception: %s\", \"out_entries_id\": null}",
                                        e.getMessage().replace("\"", "\\\""));
                }
        }

        public String updateFinalStatus(String id, String status, String score, String errorDetail) {
                MapSqlParameterSource in = new MapSqlParameterSource()
                                .addValue("IN_ID", id)
                                .addValue("IN_STATUS", status)
                                .addValue("IN_SCORE", score)
                                .addValue("IN_ERROR_DETAIL", errorDetail);

                Map<String, Object> responseMap = new HashMap<>();
                try {
                        Map<String, Object> out = ekybFinalStatusCall.execute(in);
                        responseMap.put("p_result", out.get("P_RESULT"));
                        // Return as JSON string
                        return objectMapper.writeValueAsString(responseMap);
                } catch (Exception e) {
                        // Build error JSON manually to avoid further exceptions
                        return String.format("{\"p_result\": \"exception: %s\", \"out_entries_id\": null}",
                                        e.getMessage().replace("\"", "\\\""));
                }
        }

        public String updateFinalDirector(String id, String status, String score, String errorDetail,
                        String resDirListJson) {
                MapSqlParameterSource in = new MapSqlParameterSource()
                                .addValue("IN_ID", id)
                                .addValue("IN_STATUS", status)
                                .addValue("IN_SCORE", score)
                                .addValue("IN_ERROR_DETAIL", errorDetail)
                                .addValue(("IN_RES_DIR_LIST_JSON"), resDirListJson);

                Map<String, Object> responseMap = new HashMap<>();
                try {
                        Map<String, Object> out = ekybFinalDirectorCall.execute(in);
                        responseMap.put("p_result", out.get("P_RESULT"));
                        // Return as JSON string
                        return objectMapper.writeValueAsString(responseMap);
                } catch (Exception e) {
                        // Build error JSON manually to avoid further exceptions
                        return String.format("{\"p_result\": \"exception: %s\", \"out_entries_id\": null}",
                                        e.getMessage().replace("\"", "\\\""));
                }
        }

        public List<Ekyb> findPendingRecords() {
                String sql = "SELECT ID, APP_CODE, APP_CHANNEL, SINGLE_ID, TIN, COMPANY_NAME_KH, COMPANY_NAME_EN, \r\n"
                                + //
                                "       DIR_LIST_JSON, STATUS, SCORE, TYPE, ERROR_DETAIL, NOTE, \r\n" + //
                                "       RES_DIR_LIST_JSON\r\n" + //
                                "FROM EKYB_PROFILE \r\n" + //
                                "WHERE STATUS = 0\r\n" + //
                                "ORDER BY ID ASC ";

                return jdbcTemplate.query(sql, (rs, rowNum) -> {
                        Ekyb ekyb = new Ekyb();

                        ekyb.setId(rs.getString("ID"));
                        ekyb.setAppCode(rs.getString("APP_CODE"));
                        ekyb.setAppChannel(rs.getString("APP_CHANNEL"));

                        ekyb.setSingleId(rs.getString("SINGLE_ID"));
                        ekyb.setTin(rs.getString("TIN"));
                        ekyb.setCompanyNameKh(rs.getString("COMPANY_NAME_KH"));
                        ekyb.setCompanyNameEn(rs.getString("COMPANY_NAME_EN"));

                        // director json list
                        String dirList = rs.getString("DIR_LIST_JSON");
                        JsonNode dirListNode;
                        try {
                                dirListNode = objectMapper.readTree(dirList);
                                ekyb.setDirList(dirListNode);
                        } catch (Exception e) {
                        }

                        // response director json list
                        String resDirList = rs.getString("RES_DIR_LIST_JSON");
                        JsonNode resDirListNode;
                        try {
                                resDirListNode = objectMapper.readTree(resDirList);
                                if (resDirListNode != null) {
                                        List<Map<String, Object>> list = StreamSupport
                                                        .stream(resDirListNode.spliterator(), false)
                                                        .map(node -> {
                                                                Map<String, Object> map = new HashMap<>();
                                                                map.put("exist", node.path("exist").asText());
                                                                map.put("incorrect_fields",
                                                                                node.path("incorrect_fields"));
                                                                map.put("identification_number",
                                                                                node.path("identification_number")
                                                                                                .asText());

                                                                String scoreString = node.path("score").asText() == null
                                                                                ? "0"
                                                                                : node.path("score").asText("")
                                                                                                .isEmpty() ? "0"
                                                                                                                : node.path("score")
                                                                                                                                .asText("");
                                                                double score = Double.parseDouble(scoreString) * 100;
                                                                map.put("score", String.valueOf(score));

                                                                return map;
                                                        })
                                                        .collect(Collectors.toList());
                                        ekyb.setResDirList(objectMapper.valueToTree(list));
                                }
                        } catch (Exception e) {
                        }

                        String scoreString = rs.getString("SCORE") == null ? "0"
                                        : rs.getString("SCORE").isEmpty() ? "0" : rs.getString("SCORE");
                        double score = Double.parseDouble(scoreString) * 100;
                        ekyb.setScore(String.valueOf(score));

                        ekyb.setStatus(rs.getString("STATUS"));
                        ekyb.setStatusDesc(StatusClassification.statusConvertor(score, rs.getString("STATUS")));

                        ekyb.setType(rs.getString("TYPE"));
                        ekyb.setNote(rs.getString("NOTE"));
                        ekyb.setErrorDetail(rs.getString("ERROR_DETAIL"));

                        return ekyb;
                });
        }

        public Ekyb getEkybById(String id) {
                String sql = "SELECT ID, APP_CODE, APP_CHANNEL, SINGLE_ID, TIN, COMPANY_NAME_KH, COMPANY_NAME_EN, \r\n"
                                + //
                                "       DIR_LIST_JSON, STATUS, SCORE, TYPE, ERROR_DETAIL, NOTE, RES_DIR_LIST_JSON \r\n"
                                + //
                                "FROM EKYB_PROFILE \r\n" + //
                                "WHERE ID = ?";

                try {
                        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
                                Ekyb ekyb = new Ekyb();

                                ekyb.setId(rs.getString("ID"));
                                ekyb.setAppCode(rs.getString("APP_CODE"));
                                ekyb.setAppChannel(rs.getString("APP_CHANNEL"));

                                ekyb.setSingleId(rs.getString("SINGLE_ID"));
                                ekyb.setTin(rs.getString("TIN"));
                                ekyb.setCompanyNameKh(rs.getString("COMPANY_NAME_KH"));
                                ekyb.setCompanyNameEn(rs.getString("COMPANY_NAME_EN"));

                                // director json list
                                String dirList = rs.getString("DIR_LIST_JSON");
                                JsonNode dirListNode;
                                try {
                                        dirListNode = objectMapper.readTree(dirList);
                                        ekyb.setDirList(dirListNode);
                                } catch (Exception e) {
                                }

                                // response director json list
                                String resDirList = rs.getString("RES_DIR_LIST_JSON");
                                JsonNode resDirListNode;
                                try {
                                        resDirListNode = objectMapper.readTree(resDirList);
                                        if (resDirListNode != null) {
                                                List<Map<String, Object>> list = StreamSupport
                                                                .stream(resDirListNode.spliterator(), false)
                                                                .map(node -> {
                                                                        Map<String, Object> map = new HashMap<>();
                                                                        map.put("exist", node.path("exist").asText());
                                                                        map.put("incorrect_fields",
                                                                                        node.path("incorrect_fields"));
                                                                        map.put("identification_number",
                                                                                        node.path("identification_number")
                                                                                                        .asText());

                                                                        String scoreString = node.path("score")
                                                                                        .asText() == null
                                                                                                        ? "0"
                                                                                                        : node.path("score")
                                                                                                                        .asText("")
                                                                                                                        .isEmpty() ? "0"
                                                                                                                                        : node.path("score")
                                                                                                                                                        .asText("");
                                                                        double score = Double.parseDouble(scoreString)
                                                                                        * 100;
                                                                        map.put("score", String.valueOf(score));

                                                                        return map;
                                                                })
                                                                .collect(Collectors.toList());
                                                ekyb.setResDirList(objectMapper.valueToTree(list));
                                        }
                                } catch (Exception e) {
                                }

                                String scoreString = rs.getString("SCORE") == null ? "0"
                                                : rs.getString("SCORE").isEmpty() ? "0" : rs.getString("SCORE");

                                double score = Double.parseDouble(scoreString) * 100;
                                ekyb.setScore(String.valueOf(score));

                                ekyb.setStatus(rs.getString("STATUS"));
                                ekyb.setStatusDesc(StatusClassification.statusConvertor(score, rs.getString("STATUS")));

                                ekyb.setType(rs.getString("TYPE"));
                                ekyb.setNote(rs.getString("NOTE"));
                                ekyb.setErrorDetail(rs.getString("ERROR_DETAIL"));

                                return ekyb;
                        }, id);
                } catch (Exception e) {
                        return null;
                }
        }

        public List<Ekyb> getEkybPage(int size, int page, String searchString) {

                int offSet = (page - 1) * size;
                String sql = "SELECT ID, APP_CODE, APP_CHANNEL, SINGLE_ID, TIN, COMPANY_NAME_KH, COMPANY_NAME_EN, \r\n"
                                + //
                                "       DIR_LIST_JSON, STATUS, SCORE, TYPE, ERROR_DETAIL, NOTE, RES_DIR_LIST_JSON \r\n"
                                + //
                                "FROM EKYB_PROFILE \r\n" + //
                                "WHERE 1 = 1 \r\n" + //
                                "      AND ((SINGLE_ID LIKE '%' || ? || '%') \r\n" + //
                                "           OR (TIN LIKE '%' || ? || '%')\r\n" + //
                                "           OR (COMPANY_NAME_KH LIKE '%' || TO_NCHAR(?) || '%')\r\n" + //
                                "           OR (UPPER(COMPANY_NAME_EN) LIKE '%' || UPPER(?) || '%'))\r\n" + //
                                "ORDER BY ID DESC \r\n" + //
                                "OFFSET ? ROWS FETCH NEXT ? ROWS ONLY  ";

                return jdbcTemplate.query(sql, (rs, rowNum) -> {
                        Ekyb ekyb = new Ekyb();

                        ekyb.setId(rs.getString("ID"));
                        ekyb.setAppCode(rs.getString("APP_CODE"));
                        ekyb.setAppChannel(rs.getString("APP_CHANNEL"));

                        ekyb.setSingleId(rs.getString("SINGLE_ID"));
                        ekyb.setTin(rs.getString("TIN"));
                        ekyb.setCompanyNameKh(rs.getString("COMPANY_NAME_KH"));
                        ekyb.setCompanyNameEn(rs.getString("COMPANY_NAME_EN"));

                        // director json list
                        String dirList = rs.getString("DIR_LIST_JSON");
                        JsonNode dirListNode;
                        try {
                                dirListNode = objectMapper.readTree(dirList);
                                ekyb.setDirList(dirListNode);
                        } catch (Exception e) {
                        }

                        // response director json list
                        String resDirList = rs.getString("RES_DIR_LIST_JSON");
                        JsonNode resDirListNode;
                        try {
                                resDirListNode = objectMapper.readTree(resDirList);
                                if (resDirListNode != null) {
                                        List<Map<String, Object>> list = StreamSupport
                                                        .stream(resDirListNode.spliterator(), false)
                                                        .map(node -> {
                                                                Map<String, Object> map = new HashMap<>();
                                                                map.put("exist", node.path("exist").asText());
                                                                map.put("incorrect_fields",
                                                                                node.path("incorrect_fields"));
                                                                map.put("identification_number",
                                                                                node.path("identification_number")
                                                                                                .asText());

                                                                String scoreString = node.path("score").asText() == null
                                                                                ? "0"
                                                                                : node.path("score").asText("")
                                                                                                .isEmpty() ? "0"
                                                                                                                : node.path("score")
                                                                                                                                .asText("");
                                                                double score = Double.parseDouble(scoreString) * 100;
                                                                map.put("score", String.valueOf(score));

                                                                return map;
                                                        })
                                                        .collect(Collectors.toList());
                                        ekyb.setResDirList(objectMapper.valueToTree(list));
                                }
                        } catch (Exception e) {
                        }

                        String scoreString = rs.getString("SCORE") == null ? "0"
                                        : rs.getString("SCORE").isEmpty() ? "0" : rs.getString("SCORE");
                        double score = Double.parseDouble(scoreString) * 100;
                        ekyb.setScore(String.valueOf(score));

                        ekyb.setStatus(rs.getString("STATUS"));
                        ekyb.setStatusDesc(StatusClassification.statusConvertor(score, rs.getString("STATUS")));

                        ekyb.setType(rs.getString("TYPE"));
                        ekyb.setNote(rs.getString("NOTE"));
                        ekyb.setErrorDetail(rs.getString("ERROR_DETAIL"));

                        return ekyb;
                }, searchString, searchString, searchString, searchString, offSet, size);
        }

        @SuppressWarnings("null")
        public int getEkybPageCount(String searchValue) {
                String sql = "SELECT COUNT(*) COUNT_VALUE FROM (  \r\n" + //
                                "  SELECT ID, APP_CODE, APP_CHANNEL, SINGLE_ID, TIN, COMPANY_NAME_KH, COMPANY_NAME_EN, \r\n"
                                + //
                                "         DIR_LIST_JSON, STATUS, SCORE, TYPE, ERROR_DETAIL, NOTE\r\n" + //
                                "  FROM EKYB_PROFILE \r\n" + //
                                "  WHERE 1=1 \r\n" + //
                                "        AND ((SINGLE_ID LIKE '%' || ? || '%') \r\n" + //
                                "             OR (TIN LIKE '%' || ? || '%')\r\n" + //
                                "             OR (COMPANY_NAME_KH LIKE '%' || TO_NCHAR(?) || '%')\r\n" + //
                                "             OR (UPPER(COMPANY_NAME_EN) LIKE '%' || UPPER(?) || '%'))\r\n" + //
                                "\r\n" + //
                                ") ";
                try {
                        return jdbcTemplate.queryForObject(sql, Integer.class, searchValue, searchValue, searchValue,
                                        searchValue);
                } catch (Exception e) {
                        return 0;
                }
        }

        public List<Ekyb> getHistoryById(String id) {

                String sql = "SELECT X.* FROM (\r\n" + //
                                "  SELECT A.ID, A.APP_CODE, A.APP_CHANNEL, A.SINGLE_ID, A.TIN, \r\n" + //
                                "         A.COMPANY_NAME_KH, A.COMPANY_NAME_EN, A.TYPE, \r\n" + //
                                "         A.SCORE, A.NOTE, A.ERROR_DETAIL, A.DIR_LIST_JSON, A.STATUS, \r\n" + //
                                "         B.USER_ID,\r\n" + //
                                "         CASE WHEN B.ACTION_TYPE = 'USER' THEN B.USER_NAME\r\n" + //
                                "              ELSE 'SYSTEM'\r\n" + //
                                "         END AS USER_NAME,\r\n" + //
                                "         A.RES_DIR_LIST_JSON, B.CREATED_TIME, \r\n" + //
                                "         TO_CHAR(TO_DATE(B.CREATED_TIME, 'YYYYMMDDHH24MI'), 'YYYY-MM-DD HH24:MI') AS CREATE_TIME2\r\n"
                                + //
                                "              \r\n" + //
                                "  FROM EKYB_PROFILE A, CAMDX_LOG B \r\n" + //
                                "  WHERE A.ID = B.TABLE_ID\r\n" + //
                                "        AND B.TABLE_NAME = 'EKYB_PROFILE' \r\n" + //
                                "        AND A.ID = ?\r\n" + //
                                "  ORDER BY B.ID ASC\r\n" + //
                                ")X ";

                return jdbcTemplate.query(sql, (rs, rowNum) -> {
                        Ekyb ekyb = new Ekyb();

                        ekyb.setId(rs.getString("ID"));
                        ekyb.setAppCode(rs.getString("APP_CODE"));
                        ekyb.setAppChannel(rs.getString("APP_CHANNEL"));

                        ekyb.setSingleId(rs.getString("SINGLE_ID"));
                        ekyb.setTin(rs.getString("TIN"));
                        ekyb.setCompanyNameKh(rs.getString("COMPANY_NAME_KH"));
                        ekyb.setCompanyNameEn(rs.getString("COMPANY_NAME_EN"));

                        // director json list
                        String dirList = rs.getString("DIR_LIST_JSON");
                        JsonNode dirListNode;
                        try {
                                dirListNode = objectMapper.readTree(dirList);
                                ekyb.setDirList(dirListNode);
                        } catch (Exception e) {
                        }

                        // response director json list
                        String resDirList = rs.getString("RES_DIR_LIST_JSON");
                        JsonNode resDirListNode;
                        try {
                                resDirListNode = objectMapper.readTree(resDirList);
                                if (resDirListNode != null) {
                                        List<Map<String, Object>> list = StreamSupport
                                                        .stream(resDirListNode.spliterator(), false)
                                                        .map(node -> {
                                                                Map<String, Object> map = new HashMap<>();
                                                                map.put("exist", node.path("exist").asText());
                                                                map.put("incorrect_fields",
                                                                                node.path("incorrect_fields"));
                                                                map.put("identification_number",
                                                                                node.path("identification_number")
                                                                                                .asText());

                                                                String scoreString = node.path("score").asText() == null
                                                                                ? "0"
                                                                                : node.path("score").asText("")
                                                                                                .isEmpty() ? "0"
                                                                                                                : node.path("score")
                                                                                                                                .asText("");
                                                                double score = Double.parseDouble(scoreString) * 100;
                                                                map.put("score", String.valueOf(score));

                                                                return map;
                                                        })
                                                        .collect(Collectors.toList());
                                        ekyb.setResDirList(objectMapper.valueToTree(list));
                                }
                        } catch (Exception e) {
                        }

                        String scoreString = rs.getString("SCORE") == null ? "0"
                                        : rs.getString("SCORE").isEmpty() ? "0" : rs.getString("SCORE");
                        double score = Double.parseDouble(scoreString) * 100;

                        ekyb.setScore(String.valueOf(score));
                        ekyb.setStatus(rs.getString("STATUS"));

                        String statusDesc = StatusClassification.statusConvertor(score, rs.getString("STATUS"));
                        ekyb.setStatusDesc(statusDesc);

                        ekyb.setType(rs.getString("TYPE"));
                        ekyb.setNote(rs.getString("NOTE"));
                        ekyb.setErrorDetail(rs.getString("ERROR_DETAIL"));

                        if (rowNum == 0) {
                                HistoryAction step1 = new HistoryAction();

                                String desc = ((rs.getString("TIN") != null && !rs.getString("TIN").isEmpty())
                                                && (rs.getString("SINGLE_ID") != null
                                                                && !rs.getString("SINGLE_ID").isEmpty()))
                                                                                ? ("TIN " + rs.getString("TIN") + " - "
                                                                                                + rs.getString("SINGLE_ID")
                                                                                                + " - New request submitted")
                                                                                : (rs.getString("TIN") != null && !rs
                                                                                                .getString("TIN")
                                                                                                .isEmpty())
                                                                                                                ? ("TIN " + rs.getString(
                                                                                                                                "TIN")
                                                                                                                                + " - New request submitted")
                                                                                                                : (rs.getString("SINGLE_ID") != null
                                                                                                                                && !rs.getString(
                                                                                                                                                "SINGLE_ID")
                                                                                                                                                .isEmpty())
                                                                                                                                                                ? ("Register ID "
                                                                                                                                                                                + rs.getString("SINGLE_ID")
                                                                                                                                                                                + " - New request submitted")
                                                                                                                                                                : "";

                                step1.setDescription(desc);
                                step1.setUserId(rs.getString("USER_ID"));
                                step1.setUserName(rs.getString("USER_NAME"));
                                step1.setActionDate(rs.getString("CREATE_TIME2"));

                                ekyb.setStep1(step1);
                        }
                        if (rowNum == 1) {
                                HistoryAction step2 = new HistoryAction();
                                step2.setDescription("Request Dispached automatically to CamDx");
                                step2.setUserId(rs.getString("USER_ID"));
                                step2.setUserName(rs.getString("USER_NAME"));
                                step2.setActionDate(rs.getString("CREATE_TIME2"));

                                ekyb.setStep2(step2);
                        }
                        if (rowNum > 1) {
                                HistoryAction step3 = new HistoryAction();
                                step3.setDescription(
                                                "CamDx score: " + new DecimalFormat("0.##").format(score) + "/100 - "
                                                                + statusDesc);
                                step3.setUserId(rs.getString("USER_ID"));
                                step3.setUserName(rs.getString("USER_NAME"));
                                step3.setActionDate(rs.getString("CREATE_TIME2"));

                                ekyb.setStep3(step3);
                        }
                        return ekyb;
                }, id);
        }

        public List<History> getListByAppChannel(int size, int page, String searchValue, String appChannel,
                        String requestType, String statusDesc, String fromDate, String toDate) {

                int offSet = (page - 1) * size;
                String sql = "SELECT X.* \r\n" + //
                                "FROM(\r\n" + //
                                "  WITH LOG_DATE AS (SELECT TABLE_NAME, TABLE_ID, MIN(CREATED_TIME) AS CREATED_TIME\r\n"
                                + //
                                "                    FROM CAMDX_LOG T\r\n" + //
                                "                    GROUP BY TABLE_NAME, TABLE_ID)\r\n" + //
                                "  SELECT ID, 'eKYC' AS REQUEST_TYPE, APP_CODE, APP_CHANNEL, FIRST_NAME_KH, lAST_NAME_KH, \r\n"
                                + //
                                "         FIRST_NAME_EN, LAST_NAME_EN, N'' AS COMPANY_NAME_KH, '' AS COMPANY_NAME_EN, \r\n"
                                + //
                                "         SCORE, FACE_MOI_SCORE, STATUS,\r\n" + //
                                "         PKG_CAMDX.STATUS_CLASSIFICATION(SCORE, STATUS) AS STATUS_DESC, \r\n" + //
                                "         TO_CHAR(TO_DATE(CREATED_TIME, 'YYYYMMDDHH24MI'), 'YYYY-MM-DD HH24:MI') AS CREATE_TIME2,\r\n" + 
                                "         CREATED_TIME, TYPE\r\n" + //
                                "  FROM EKYC_PROFILE A, LOG_DATE B\r\n" + //
                                "  WHERE A.ID = B.TABLE_ID\r\n" + //
                                "        AND B.TABLE_NAME = 'EKYC_PROFILE'\r\n" + //
                                "        AND ((ID_NUMBER LIKE '%' || ? || '%') \r\n" + //
                                "            OR ((LAST_NAME_KH || ' ' || FIRST_NAME_KH) LIKE '%' || TO_NCHAR(?) || '%')\r\n"
                                + //
                                "            OR (UPPER(LAST_NAME_EN) || ' ' || UPPER(FIRST_NAME_EN) LIKE UPPER('%' || ? || '%'))\r\n"
                                + //
                                "            OR (FIRST_NAME_KH LIKE '%' || TO_NCHAR(?) || '%')\r\n" + //
                                "            OR (LAST_NAME_KH LIKE '%' || TO_NCHAR(?) || '%')\r\n" + //
                                "            OR (UPPER(FIRST_NAME_EN) LIKE UPPER('%' || ? || '%'))\r\n" + //
                                "            OR (UPPER(LAST_NAME_EN) LIKE UPPER('%' || ? || '%')))\r\n" + //
                                "  UNION ALL\r\n" + //
                                "  SELECT ID, 'eKYB' AS REQUEST_TYPE, APP_CODE, APP_CHANNEL, N'' AS FIRST_NAME_KH, N'' AS lAST_NAME_KH, \r\n"
                                + //
                                "         '' AS FIRST_NAME_EN, '' AS LAST_NAME_EN, COMPANY_NAME_KH, COMPANY_NAME_EN, \r\n"
                                + //
                                "         SCORE, 0 AS FACE_MOI_SCORE, STATUS, \r\n" + //
                                "         PKG_CAMDX.STATUS_CLASSIFICATION(SCORE, STATUS) AS STATUS_DESC, \r\n" + //
                                "         TO_CHAR(TO_DATE(CREATED_TIME, 'YYYYMMDDHH24MI'), 'YYYY-MM-DD HH24:MI') AS CREATE_TIME2,\r\n" + 
                                "         CREATED_TIME, TYPE\r\n" + //
                                "  FROM EKYB_PROFILE C, LOG_DATE D\r\n" + //
                                "  WHERE C.ID = D.TABLE_ID\r\n" + //
                                "        AND D.TABLE_NAME = 'EKYB_PROFILE'\r\n" + //
                                "        AND ((SINGLE_ID LIKE '%' || ? || '%')\r\n" + //
                                "            OR (TIN LIKE '%' || ? || '%')\r\n" + //
                                "            OR (COMPANY_NAME_KH LIKE '%' || TO_NCHAR(?) || '%')\r\n" + //
                                "            OR (UPPER(COMPANY_NAME_EN) LIKE '%' || UPPER(?) || '%'))\r\n" + //
                                ")X\r\n" + //
                                "WHERE 1 = 1\r\n" + //
                                "      AND ((TRIM(?) IS NULL) OR (X.APP_CHANNEL = ?))             --APP_CHANNEL\r\n" + //
                                "      AND ((TRIM(?) IS NULL) OR (X.REQUEST_TYPE = ?))            --REQUEST_TYPE\r\n" + //
                                "      AND ((TRIM(?) IS NULL) OR (X.STATUS_DESC = ?))             --STATUS_DESC\r\n" + //
                                "      AND ((TRIM(?) IS NULL) OR (X.CREATED_TIME >= ?))           --FROM_DATE\r\n" + //
                                "      AND ((TRIM(?) IS NULL) OR (X.CREATED_TIME <= ?))           --TO_DATE\r\n" + //
                                "      \r\n" + //
                                "ORDER BY X.CREATED_TIME DESC \r\n" + //
                                "OFFSET ? ROWS FETCH NEXT ? ROWS ONLY ";

                return jdbcTemplate.query(sql, (rs, rowNum) -> {
                        History history = new History();

                        history.setId(rs.getString("ID"));
                        history.setAppCode(rs.getString("APP_CODE"));
                        history.setAppChannel(rs.getString("APP_CHANNEL"));
                        history.setRequestType(rs.getString("REQUEST_TYPE"));

                        if ("eKYB".equalsIgnoreCase(rs.getString("REQUEST_TYPE")))
                                history.setEkybType(rs.getString("TYPE"));
                        else
                                history.setEkycType(rs.getString("TYPE"));

                        history.setFirstNameKh(rs.getString("FIRST_NAME_KH"));
                        history.setLastNameKh(rs.getString("LAST_NAME_KH"));
                        history.setFirstNameEn(rs.getString("FIRST_NAME_EN"));
                        history.setLastNameEn(rs.getString("LAST_NAME_EN"));
                        history.setCompanyNameKh(rs.getString("COMPANY_NAME_KH"));
                        history.setCompanyNameEn(rs.getString("COMPANY_NAME_EN"));

                        history.setStatus(rs.getString("STATUS"));
                        history.setStatusDesc(rs.getString("STATUS_DESC"));

                        String scoreString = rs.getString("SCORE") == null ? "0"
                                        : rs.getString("SCORE").isEmpty() ? "0" : rs.getString("SCORE");
                        double score = Double.parseDouble(scoreString) * 100;
                        history.setScore(String.valueOf(score));

                        String faceScoreString = rs.getString("FACE_MOI_SCORE") == null ? "0"
                                        : rs.getString("FACE_MOI_SCORE").isEmpty() ? "0"
                                                        : rs.getString("FACE_MOI_SCORE");
                        double faceScore = Double.parseDouble(faceScoreString) * 100;
                        history.setFaceScore(String.valueOf(faceScore));

                        history.setCreateTime(rs.getString("CREATE_TIME2"));

                        return history;
                }, searchValue, searchValue, searchValue, searchValue, searchValue, searchValue, searchValue,
                                searchValue, searchValue, searchValue, searchValue, appChannel, appChannel, requestType,
                                requestType, statusDesc,
                                statusDesc,
                                fromDate, fromDate, toDate, toDate, offSet, size);
        }

        public int getListByAppChannelCount(String searchValue, String appChannel,
                        String requestType, String statusDesc, String fromDate, String toDate) {
                String sql = "\r\n" + //
                                "SELECT COUNT(*) AS COUNT_VALUE  \r\n" + //
                                "FROM(\r\n" + //
                                "  WITH LOG_DATE AS (SELECT TABLE_NAME, TABLE_ID, MIN(CREATED_TIME) AS CREATED_TIME\r\n"
                                + //
                                "                    FROM CAMDX_LOG T\r\n" + //
                                "                    GROUP BY TABLE_NAME, TABLE_ID)\r\n" + //
                                "  SELECT ID, 'eKYC' AS REQUEST_TYPE, APP_CODE, APP_CHANNEL, FIRST_NAME_KH, lAST_NAME_KH, \r\n"
                                + //
                                "         FIRST_NAME_EN, LAST_NAME_EN, N'' AS COMPANY_NAME_KH, '' AS COMPANY_NAME_EN, \r\n"
                                + //
                                "         SCORE, FACE_MOI_SCORE, STATUS,\r\n" + //
                                "         PKG_CAMDX.STATUS_CLASSIFICATION(SCORE, STATUS) AS STATUS_DESC, \r\n" + //
                                "         CREATED_TIME\r\n" + //
                                "  FROM EKYC_PROFILE A, LOG_DATE B\r\n" + //
                                "  WHERE A.ID = B.TABLE_ID\r\n" + //
                                "        AND B.TABLE_NAME = 'EKYC_PROFILE'\r\n" + //
                                "        AND ((ID_NUMBER LIKE '%' || ? || '%') \r\n" + //
                                "            OR ((LAST_NAME_KH || ' ' || FIRST_NAME_KH) LIKE '%' || TO_NCHAR(?) || '%')\r\n"
                                + //
                                "            OR (UPPER(LAST_NAME_EN) || ' ' || UPPER(FIRST_NAME_EN) LIKE UPPER('%' || ? || '%'))\r\n"
                                + //
                                "            OR (FIRST_NAME_KH LIKE '%' || TO_NCHAR(?) || '%')\r\n" + //
                                "            OR (LAST_NAME_KH LIKE '%' || TO_NCHAR(?) || '%')\r\n" + //
                                "            OR (UPPER(FIRST_NAME_EN) LIKE UPPER('%' || ? || '%'))\r\n" + //
                                "            OR (UPPER(LAST_NAME_EN) LIKE UPPER('%' || ? || '%')))\r\n" + //
                                "  UNION ALL\r\n" + //
                                "  SELECT ID, 'eKYB' AS REQUEST_TYPE, APP_CODE, APP_CHANNEL, N'' AS FIRST_NAME_KH, N'' AS lAST_NAME_KH, \r\n"
                                + //
                                "         '' AS FIRST_NAME_EN, '' AS LAST_NAME_EN, COMPANY_NAME_KH, COMPANY_NAME_EN, \r\n"
                                + //
                                "         SCORE, 0 AS FACE_MOI_SCORE, STATUS, \r\n" + //
                                "         PKG_CAMDX.STATUS_CLASSIFICATION(SCORE, STATUS) AS STATUS_DESC, \r\n" + //
                                "         CREATED_TIME\r\n" + //
                                "  FROM EKYB_PROFILE C, LOG_DATE D\r\n" + //
                                "  WHERE C.ID = D.TABLE_ID\r\n" + //
                                "        AND D.TABLE_NAME = 'EKYB_PROFILE'\r\n" + //
                                "        AND ((SINGLE_ID LIKE '%' || ? || '%')\r\n" + //
                                "            OR (TIN LIKE '%' || ? || '%')\r\n" + //
                                "            OR (COMPANY_NAME_KH LIKE '%' || TO_NCHAR(?) || '%')\r\n" + //
                                "            OR (UPPER(COMPANY_NAME_EN) LIKE '%' || UPPER(?) || '%'))\r\n" + //
                                ")X\r\n" + //
                                "WHERE 1 = 1\r\n" + //
                                "      AND ((TRIM(?) IS NULL) OR (X.APP_CHANNEL = ?))             --APP_CHANNEL\r\n" + //
                                "      AND ((TRIM(?) IS NULL) OR (X.REQUEST_TYPE = ?))            --REQUEST_TYPE\r\n" + //
                                "      AND ((TRIM(?) IS NULL) OR (X.STATUS_DESC = ?))             --STATUS_DESC\r\n" + //
                                "      AND ((TRIM(?) IS NULL) OR (X.CREATED_TIME >= ?))           --FROM_DATE\r\n" + //
                                "      AND ((TRIM(?) IS NULL) OR (X.CREATED_TIME <= ?))           --TO_DATE";

                try {
                        return jdbcTemplate.queryForObject(sql, Integer.class, searchValue, searchValue, searchValue,
                                        searchValue,
                                        searchValue, searchValue, searchValue,
                                        searchValue, searchValue, appChannel, appChannel, requestType, requestType,
                                        statusDesc,
                                        statusDesc,
                                        fromDate, fromDate, toDate, toDate);
                } catch (Exception e) {
                        return 0;
                }
        }

        public Map<String, String> getSummaryStatusByAppChannel(String searchValue, String appChannel,
                        String requestType, String statusDesc, String fromDate, String toDate) {

                String sql = "SELECT X.STATUS, COUNT(*) AS COUNT_VALUE \r\n" + //
                                "FROM(\r\n" + //
                                "  WITH LOG_DATE AS (SELECT TABLE_NAME, TABLE_ID, MIN(CREATED_TIME) AS CREATED_TIME\r\n"
                                + //
                                "                    FROM CAMDX_LOG T \r\n" + //
                                "                    GROUP BY TABLE_NAME, TABLE_ID\r\n" + //
                                "                    )      \r\n" + //
                                "\r\n" + //
                                "  SELECT A.ID, 'eKYC' REQUEST_TYPE, A.STATUS, B.CREATED_TIME, A.APP_CHANNEL, \r\n" + //
                                "         PKG_CAMDX.STATUS_CLASSIFICATION(SCORE, STATUS) AS STATUS_DESC\r\n" + //
                                "  FROM EKYC_PROFILE A, LOG_DATE B\r\n" + //
                                "  WHERE A.ID = B.TABLE_ID \r\n" + //
                                "        AND B.TABLE_NAME = 'EKYC_PROFILE' \r\n" + //
                                "        AND ((ID_NUMBER LIKE '%' || ? || '%')\r\n" + //
                                "            OR (FIRST_NAME_KH LIKE '%' || TO_NCHAR(?) || '%')\r\n" + //
                                "            OR (LAST_NAME_KH LIKE '%' || TO_NCHAR(?) || '%')\r\n" + //
                                "            OR (UPPER(FIRST_NAME_EN) LIKE UPPER('%' || ? || '%'))\r\n" + //
                                "            OR (LAST_NAME_EN LIKE UPPER('%' || ? || '%')))\r\n" + //
                                "  UNION ALL\r\n" + //
                                "  SELECT C.ID, 'eKYB' REQUEST_TYPE, C.STATUS, D.CREATED_TIME, C.APP_CHANNEL, \r\n" + //
                                "         PKG_CAMDX.STATUS_CLASSIFICATION(SCORE, STATUS) AS STATUS_DESC\r\n" + //
                                "  FROM EKYB_PROFILE C, LOG_DATE D\r\n" + //
                                "  WHERE C.ID = D.TABLE_ID \r\n" + //
                                "        AND D.TABLE_NAME = 'EKYB_PROFILE'\r\n" + //
                                "        AND ((SINGLE_ID LIKE '%' || ? || '%')\r\n" + //
                                "            OR (TIN LIKE '%' || ? || '%')\r\n" + //
                                "            OR (COMPANY_NAME_KH LIKE '%' || TO_NCHAR(?) || '%')\r\n" + //
                                "            OR (UPPER(COMPANY_NAME_EN) LIKE '%' || UPPER(?) || '%'))   \r\n" + //
                                ")X \r\n" + //
                                "WHERE 1 = 1\r\n" + //
                                "      AND ((TRIM(?) IS NULL) OR (X.APP_CHANNEL = ?))            --APP_CHANNEL\r\n" + //
                                "      AND ((TRIM(?) IS NULL) OR (X.REQUEST_TYPE = ?))           --REQUEST_TYPE\r\n" + //
                                "      AND ((TRIM(?) IS NULL) OR (X.STATUS_DESC = ?))            --STATUS_DESC\r\n" + //
                                "      AND ((TRIM(?) IS NULL) OR (X.CREATED_TIME >= ?))          --FROM_DATE\r\n" + //
                                "      AND ((TRIM(?) IS NULL) OR (X.CREATED_TIME <= ?))          --TO_DATE\r\n" + //
                                "GROUP BY X.STATUS  ";

                Map<String, String> map = new HashMap<>();
                jdbcTemplate.query(sql, (rs, rowNum) -> {

                        map.put(rs.getString("STATUS"), rs.getString("COUNT_VALUE"));
                        return null;
                }, searchValue, searchValue, searchValue, searchValue,
                                searchValue, searchValue, searchValue,
                                searchValue, searchValue, appChannel, appChannel, requestType, requestType, statusDesc,
                                statusDesc,
                                fromDate, fromDate, toDate, toDate);
                return map;
        }

        @SuppressWarnings("null")
        public int checkEkybExisting(String type, String singleId, String tin, String companyNameEn,
                        String companyNameKh) {

                String sql = "SELECT COUNT(*) AS COUNT_VALUE \r\n" + //
                                "FROM EKYB_PROFILE \r\n" + //
                                "WHERE STATUS = 3 \r\n" + //
                                "      AND ((TRIM(?) IS NULL) OR (TYPE = ?))                 --TYPE\r\n" + //
                                "      AND ((TRIM(?) IS NULL) OR (SINGLE_ID = ?))            --SINGLE_ID\r\n" + //
                                "      AND ((TRIM(?) IS NULL) OR (TIN = ?))                  --TIN\r\n" + //
                                "      AND ((TRIM(?) IS NULL) OR (COMPANY_NAME_EN = ?))      --COMPANY_NAME_EN \r\n" + //
                                "      AND ((TRIM(?) IS NULL) OR (COMPANY_NAME_KH = TO_NCHAR(?)))  --COMPANY_NAME_KH ";

                try {
                        return jdbcTemplate.queryForObject(sql, Integer.class, type, type, singleId, singleId, tin, tin,
                                        companyNameEn,
                                        companyNameEn, companyNameKh, companyNameKh);
                } catch (Exception e) {
                        return 0;
                }
        }

}
