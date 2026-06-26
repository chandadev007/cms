package com.main.repository;

import org.springframework.stereotype.Repository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.main.model.Ekyc;
import com.main.model.History;
import com.main.model.HistoryAction;
import com.main.model.ReportFull;

import jakarta.annotation.PostConstruct;
import java.sql.Types;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.support.DataAccessUtils;
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

        private SimpleJdbcCall ekycCreateCall, ekycProcessingCall, ekycFinalStatusCall, ekycFinalFaceCall;

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
                                                new SqlParameter("IN_APP_CHANNEL", Types.VARCHAR),
                                                new SqlOutParameter("P_RESULT", Types.VARCHAR),
                                                new SqlOutParameter("OUT_ID", Types.VARCHAR));

                this.ekycProcessingCall = new SimpleJdbcCall(jdbcTemplate)
                                .withSchemaName(dbSchema)
                                .withCatalogName(catalogName)
                                .withProcedureName("EKYC_PROCESSING")
                                .declareParameters(
                                                new SqlParameter("IN_ID", Types.VARCHAR),
                                                new SqlOutParameter("P_RESULT", Types.VARCHAR));

                this.ekycFinalStatusCall = new SimpleJdbcCall(jdbcTemplate)
                                .withSchemaName(dbSchema)
                                .withCatalogName(catalogName)
                                .withProcedureName("EKYC_FINAL_STATUS")
                                .declareParameters(
                                                new SqlParameter("IN_ID", Types.VARCHAR),
                                                new SqlParameter("IN_ID_NUMBER", Types.VARCHAR),
                                                new SqlParameter("IN_STATUS", Types.VARCHAR),
                                                new SqlParameter("IN_SCORE", Types.VARCHAR),
                                                new SqlParameter("IN_ERROR_DETAIL", Types.VARCHAR),
                                                new SqlOutParameter("P_RESULT", Types.VARCHAR));

                this.ekycFinalFaceCall = new SimpleJdbcCall(jdbcTemplate)
                                .withSchemaName(dbSchema)
                                .withCatalogName(catalogName)
                                .withProcedureName("EKYC_FINAL_FACE")
                                .declareParameters(
                                                new SqlParameter("IN_ID", Types.VARCHAR),
                                                new SqlParameter("IN_ID_NUMBER", Types.VARCHAR),
                                                new SqlParameter("IN_STATUS", Types.VARCHAR),
                                                new SqlParameter("IN_SCORE", Types.VARCHAR),
                                                new SqlParameter("IN_FACE_SCORE", Types.VARCHAR),
                                                new SqlParameter("IN_ERROR_DETAIL", Types.VARCHAR),
                                                new SqlOutParameter("P_RESULT", Types.VARCHAR));

        }

        public String createEkyc(String idNumber, String firstNameKh, String lastNameKh, String firstNameEn,
                        String lastNameEn, String gender, String dob, String issuedDae, String expiredDate, String note,
                        String type, String selfiePath, String errorDetail, String appChannel) {
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
                                .addValue("IN_ERROR_DETAIL", errorDetail)
                                .addValue("IN_APP_CHANNEL", appChannel);

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

        public String updateToProcessing(String id) {
                MapSqlParameterSource in = new MapSqlParameterSource()
                                .addValue("IN_ID", id);

                Map<String, Object> responseMap = new HashMap<>();
                try {
                        Map<String, Object> out = ekycProcessingCall.execute(in);

                        responseMap.put("p_result", out.get("P_RESULT"));

                        // Return as JSON string
                        return objectMapper.writeValueAsString(responseMap);
                } catch (Exception e) {
                        // Build error JSON manually to avoid further exceptions
                        return String.format("{\"p_result\": \"exception: %s\", \"out_entries_id\": null}",
                                        e.getMessage().replace("\"", "\\\""));
                }
        }

        public String updateFinalStatus(String id, String idNumber, String status, String score,
                        String errorDetail) {
                MapSqlParameterSource in = new MapSqlParameterSource()
                                .addValue("IN_ID", id)
                                .addValue("IN_ID_NUMBER", idNumber)
                                .addValue("IN_STATUS", status)
                                .addValue("IN_SCORE", score)
                                .addValue("IN_ERROR_DETAIL", errorDetail);

                Map<String, Object> responseMap = new HashMap<>();
                try {
                        Map<String, Object> out = ekycFinalStatusCall.execute(in);

                        responseMap.put("p_result", out.get("P_RESULT"));

                        // Return as JSON string
                        return objectMapper.writeValueAsString(responseMap);
                } catch (Exception e) {
                        // Build error JSON manually to avoid further exceptions
                        return String.format("{\"p_result\": \"exception: %s\", \"out_entries_id\": null}",
                                        e.getMessage().replace("\"", "\\\""));
                }
        }

        public String updateFinalFace(String id, String idNumber, String status, String score, String faceScore,
                        String errorDetail) {
                MapSqlParameterSource in = new MapSqlParameterSource()
                                .addValue("IN_ID", id)
                                .addValue("IN_ID_NUMBER", idNumber)
                                .addValue("IN_STATUS", status)
                                .addValue("IN_SCORE", score)
                                .addValue("IN_FACE_SCORE", faceScore)
                                .addValue("IN_ERROR_DETAIL", errorDetail);

                Map<String, Object> responseMap = new HashMap<>();
                try {
                        Map<String, Object> out = ekycFinalFaceCall.execute(in);

                        responseMap.put("p_result", out.get("P_RESULT"));

                        // Return as JSON string
                        return objectMapper.writeValueAsString(responseMap);
                } catch (Exception e) {
                        // Build error JSON manually to avoid further exceptions
                        return String.format("{\"p_result\": \"exception: %s\", \"out_entries_id\": null}",
                                        e.getMessage().replace("\"", "\\\""));
                }
        }

        public List<Ekyc> findPendingRecords() {
                String sql = "SELECT ID, APP_CODE, APP_CHANNEL, STATUS, SCORE, FACE_MOI_SCORE, TYPE, ERROR_DETAIL, ID_NUMBER, \r\n"
                                + //
                                "       FIRST_NAME_KH, LAST_NAME_KH, FIRST_NAME_EN, LAST_NAME_EN, GENDER,\r\n" + //
                                "       TO_CHAR(DOB, 'YYYY-MM-DD') DOB, \r\n" + //
                                "       TO_CHAR(ISSUED_DATE, 'YYYY-MM-DD') ISSUED_DATE, TO_CHAR(EXPIRED_DATE, 'YYYY-MM-DD') EXPIRED_DATE, \r\n"
                                + //
                                "       NOTE, SELFIE_PATH, \r\n" + //
                                "       PKG_CAMDX.STATUS_CLASSIFICATION(SCORE, STATUS) AS STATUS_DESC \r\n" + //
                                "FROM EKYC_PROFILE \r\n" + //
                                "WHERE STATUS = 0  \r\n" +
                                "ORDER BY ID ASC";

                return jdbcTemplate.query(sql, (rs, rowNum) -> {
                        Ekyc ekyc = new Ekyc();

                        ekyc.setId(rs.getString("ID"));
                        ekyc.setAppCode(rs.getString("APP_CODE"));
                        ekyc.setAppChannel(rs.getString("APP_CHANNEL"));

                        ekyc.setIdNumber(rs.getString("ID_NUMBER"));
                        ekyc.setFirstNameKh(rs.getString("FIRST_NAME_KH"));
                        ekyc.setLastNameKh(rs.getString("LAST_NAME_KH"));
                        ekyc.setFirstNameEn(rs.getString("FIRST_NAME_EN"));
                        ekyc.setLastNameEn(rs.getString("LAST_NAME_EN"));
                        ekyc.setGender(rs.getString("GENDER"));
                        ekyc.setDob(rs.getString("DOB"));
                        ekyc.setIssuedDate(rs.getString("ISSUED_DATE"));
                        ekyc.setExpiredDate(rs.getString("EXPIRED_DATE"));

                        String scoreString = rs.getString("SCORE") == null ? "0"
                                        : rs.getString("SCORE").isEmpty() ? "0" : rs.getString("SCORE");
                        double score = Double.parseDouble(scoreString) * 100;
                        ekyc.setScore(String.format("%.2f", score));

                        ekyc.setType(rs.getString("TYPE"));
                        ekyc.setNote(rs.getString("NOTE"));
                        ekyc.setSelfiePath(rs.getString("SELFIE_PATH"));
                        ekyc.setErrorDetail(rs.getString("ERROR_DETAIL"));
                        ekyc.setStatus(rs.getString("STATUS"));
                        ekyc.setStatusDesc(rs.getString("STATUS_DESC"));

                        String faceScoreString = rs.getString("FACE_MOI_SCORE") == null ? "0"
                                        : rs.getString("FACE_MOI_SCORE").isEmpty() ? "0"
                                                        : rs.getString("FACE_MOI_SCORE");
                        double faceScore = Double.parseDouble(faceScoreString) * 100;
                        ekyc.setFaceScore(String.format("%.2f", faceScore));

                        return ekyc;
                });
        }

        public Ekyc getEkycById(String id) {
                String sql = "SELECT ID, APP_CODE, APP_CHANNEL, STATUS, SCORE, FACE_MOI_SCORE, TYPE, ERROR_DETAIL, ID_NUMBER, \r\n"
                                + //
                                "       FIRST_NAME_KH, LAST_NAME_KH, FIRST_NAME_EN, LAST_NAME_EN, GENDER,\r\n" + //
                                "       TO_CHAR(DOB, 'YYYY-MM-DD') DOB, \r\n" + //
                                "       TO_CHAR(ISSUED_DATE, 'YYYY-MM-DD') ISSUED_DATE, TO_CHAR(EXPIRED_DATE, 'YYYY-MM-DD') EXPIRED_DATE, \r\n"
                                + //
                                "       NOTE, SELFIE_PATH, \r\n" + //
                                "       PKG_CAMDX.STATUS_CLASSIFICATION(SCORE, STATUS) AS STATUS_DESC \r\n" + //
                                "FROM EKYC_PROFILE \r\n" + //
                                "WHERE ID = ? ";

                List<Ekyc> results = jdbcTemplate.query(sql, (rs, rowNum) -> {
                        Ekyc ekyc = new Ekyc();

                        ekyc.setId(rs.getString("ID"));
                        ekyc.setAppCode(rs.getString("APP_CODE"));
                        ekyc.setAppChannel(rs.getString("APP_CHANNEL"));

                        ekyc.setIdNumber(rs.getString("ID_NUMBER"));
                        ekyc.setFirstNameKh(rs.getString("FIRST_NAME_KH"));
                        ekyc.setLastNameKh(rs.getString("LAST_NAME_KH"));
                        ekyc.setFirstNameEn(rs.getString("FIRST_NAME_EN"));
                        ekyc.setLastNameEn(rs.getString("LAST_NAME_EN"));
                        ekyc.setGender(rs.getString("GENDER"));
                        ekyc.setDob(rs.getString("DOB"));
                        ekyc.setIssuedDate(rs.getString("ISSUED_DATE"));
                        ekyc.setExpiredDate(rs.getString("EXPIRED_DATE"));

                        String scoreString = rs.getString("SCORE") == null ? "0"
                                        : rs.getString("SCORE").isEmpty() ? "0" : rs.getString("SCORE");
                        double score = Double.parseDouble(scoreString) * 100;

                        ekyc.setScore(String.format("%.2f", score));
                        ekyc.setType(rs.getString("TYPE"));
                        ekyc.setNote(rs.getString("NOTE"));
                        ekyc.setSelfiePath(rs.getString("SELFIE_PATH"));
                        ekyc.setErrorDetail(rs.getString("ERROR_DETAIL"));
                        ekyc.setStatus(rs.getString("STATUS"));
                        ekyc.setStatusDesc(rs.getString("STATUS_DESC"));
                        String faceScoreString = rs.getString("FACE_MOI_SCORE") == null ? "0"
                                        : rs.getString("FACE_MOI_SCORE").isEmpty() ? "0"
                                                        : rs.getString("FACE_MOI_SCORE");
                        double faceScore = Double.parseDouble(faceScoreString) * 100;
                        ekyc.setFaceScore(String.format("%.2f", faceScore));

                        return ekyc;
                }, id);

                return DataAccessUtils.uniqueResult(results);
        }

        public List<Ekyc> getEkycPage(int size, int page, String searchValue) {

                int offSet = (page - 1) * size;
                String sql = "SELECT ID, APP_CODE, APP_CHANNEL, STATUS, SCORE, FACE_MOI_SCORE, TYPE, ERROR_DETAIL, ID_NUMBER, \r\n"
                                + //
                                "       FIRST_NAME_KH, LAST_NAME_KH, FIRST_NAME_EN, LAST_NAME_EN, GENDER,\r\n" + //
                                "       TO_CHAR(DOB, 'YYYY-MM-DD') DOB, \r\n" + //
                                "       TO_CHAR(ISSUED_DATE, 'YYYY-MM-DD') ISSUED_DATE, TO_CHAR(EXPIRED_DATE, 'YYYY-MM-DD') EXPIRED_DATE, \r\n"
                                + //
                                "       NOTE, SELFIE_PATH, \r\n" + //
                                "       PKG_CAMDX.STATUS_CLASSIFICATION(SCORE, STATUS) AS STATUS_DESC \r\n" + //
                                "FROM EKYC_PROFILE \r\n" + //
                                "WHERE 1=1\r\n" + //
                                "      AND ((ID_NUMBER LIKE '%' || ? || '%') \r\n" + //
                                "          OR ((LAST_NAME_KH || ' ' || FIRST_NAME_KH) LIKE '%' || TO_NCHAR(?) || '%') \r\n"
                                + //
                                "          OR (UPPER(LAST_NAME_EN || ' ' || FIRST_NAME_EN) LIKE '%' || UPPER(?) || '%') \r\n"
                                + //
                                "          OR (FIRST_NAME_KH LIKE '%' || TO_NCHAR(?) || '%') \r\n" + //
                                "          OR (LAST_NAME_KH LIKE '%' || TO_NCHAR(?) || '%') \r\n" + //
                                "          OR (UPPER(FIRST_NAME_EN) LIKE '%' || UPPER(?) || '%') \r\n" + //
                                "          OR (UPPER(LAST_NAME_EN) LIKE '%' || UPPER(?) || '%'))\r\n" + //
                                "ORDER BY SCORE DESC \r\n" + //
                                "OFFSET ? ROWS FETCH NEXT ? ROWS ONLY  ";

                return jdbcTemplate.query(sql, (rs, rownum) -> {
                        Ekyc ekyc = new Ekyc();

                        ekyc.setId(rs.getString("ID"));
                        ekyc.setAppCode(rs.getString("APP_CODE"));
                        ekyc.setAppChannel(rs.getString("APP_CHANNEL"));

                        ekyc.setIdNumber(rs.getString("ID_NUMBER"));
                        ekyc.setFirstNameKh(rs.getString("FIRST_NAME_KH"));
                        ekyc.setLastNameKh(rs.getString("LAST_NAME_KH"));
                        ekyc.setFirstNameEn(rs.getString("FIRST_NAME_EN"));
                        ekyc.setLastNameEn(rs.getString("LAST_NAME_EN"));
                        ekyc.setGender(rs.getString("GENDER"));
                        ekyc.setDob(rs.getString("DOB"));
                        ekyc.setIssuedDate(rs.getString("ISSUED_DATE"));
                        ekyc.setExpiredDate(rs.getString("EXPIRED_DATE"));

                        String scoreString = rs.getString("SCORE") == null ? "0"
                                        : rs.getString("SCORE").isEmpty() ? "0" : rs.getString("SCORE");
                        double score = Double.parseDouble(scoreString) * 100;
                        ekyc.setScore(String.format("%.2f", score));
                        ekyc.setType(rs.getString("TYPE"));
                        ekyc.setNote(rs.getString("NOTE"));
                        ekyc.setSelfiePath(rs.getString("SELFIE_PATH"));
                        ekyc.setErrorDetail(rs.getString("ERROR_DETAIL"));
                        ekyc.setStatus(rs.getString("STATUS"));
                        ekyc.setStatusDesc(rs.getString("STATUS_DESC"));

                        String faceScoreString = rs.getString("FACE_MOI_SCORE") == null ? "0"
                                        : rs.getString("FACE_MOI_SCORE").isEmpty() ? "0"
                                                        : rs.getString("FACE_MOI_SCORE");
                        double faceScore = Double.parseDouble(faceScoreString) * 100;
                        ekyc.setFaceScore(String.format("%.2f", faceScore));

                        return ekyc;
                }, searchValue, searchValue, searchValue, searchValue, searchValue,
                                searchValue, searchValue,
                                offSet, size);
        }

        @SuppressWarnings("null")
        public int getEkycPageCount(String searchValue) {
                String sql = "SELECT COUNT(*) AS COUNT_VALUE FROM(  \r\n" + //
                                "  SELECT ID, APP_CODE, APP_CHANNEL, STATUS, SCORE, TYPE, ERROR_DETAIL, ID_NUMBER, \r\n"
                                + //
                                "         FIRST_NAME_KH, LAST_NAME_KH, FIRST_NAME_EN, LAST_NAME_EN, GENDER,\r\n" + //
                                "         TO_CHAR(DOB, 'YYYY-MM-DD') DOB, \r\n" + //
                                "         TO_CHAR(ISSUED_DATE, 'YYYY-MM-DD') ISSUED_DATE, TO_CHAR(EXPIRED_DATE, 'YYYY-MM-DD') EXPIRED_DATE, \r\n"
                                + //
                                "         NOTE, SELFIE_PATH\r\n" + //
                                "  FROM EKYC_PROFILE \r\n" + //
                                "  WHERE 1=1\r\n" + //
                                "        AND ((ID_NUMBER LIKE '%' || ? || '%') \r\n" + //
                                "            OR ((LAST_NAME_KH || ' ' || FIRST_NAME_KH) LIKE '%' || TO_NCHAR(?) || '%') \r\n"
                                + //
                                "            OR (UPPER(LAST_NAME_EN || ' ' || FIRST_NAME_EN) LIKE '%' || UPPER(?) || '%') \r\n"
                                + //
                                "            OR (FIRST_NAME_KH LIKE '%' || TO_NCHAR(?) || '%') \r\n" + //
                                "            OR (LAST_NAME_KH LIKE '%' || TO_NCHAR(?) || '%') \r\n" + //
                                "            OR (UPPER(FIRST_NAME_EN) LIKE '%' || UPPER(?) || '%') \r\n" + //
                                "            OR (UPPER(LAST_NAME_EN) LIKE '%' || UPPER(?) || '%'))\r\n" + //
                                ")";

                try {
                        return jdbcTemplate.queryForObject(sql, Integer.class, searchValue, searchValue, searchValue,
                                        searchValue, searchValue, searchValue,
                                        searchValue);
                } catch (Exception e) {
                        return 0;
                }
        }

        public List<History> getHistories(int size, int page, String searchValue, String requestType, String statusDesc,
                        String fromDate, String toDate, String userId) {

                int offSet = (page - 1) * size;
                String sql = "SELECT X.ID, X.APP_CODE, X.APP_CHANNEL, X.REQUEST_TYPE, X.FIRST_NAME_KH, X.LAST_NAME_KH, \r\n"
                                + //
                                "       X.FIRST_NAME_EN, X.LAST_NAME_EN, X.COMPANY_NAME_KH, X.COMPANY_NAME_EN, \r\n" + //
                                "       X.STATUS, X.STATUS_DESC, \r\n" + //
                                "       X.USER_ID, X.USER_NAME,\r\n" + //
                                "       x.TYPE, X.CREATED_TIME,\r\n" + //
                                "       TO_CHAR(TO_DATE(X.CREATED_TIME, 'YYYYMMDDHH24MI'), 'YYYY-MM-DD HH24:MI') AS CREATE_TIME2\r\n"
                                + //
                                "FROM(\r\n" + //
                                "  WITH LOG_DATE AS (SELECT TABLE_NAME, TABLE_ID, ACTION_TYPE, UNIT_ID, USER_ID, USER_NAME, CREATED_TIME \r\n"
                                + //
                                "                    FROM CAMDX_LOG T WHERE T.ACTION_TYPE = 'USER')\r\n" + //
                                "  SELECT ID, 'eKYC' AS REQUEST_TYPE, APP_CODE, APP_CHANNEL, FIRST_NAME_KH, lAST_NAME_KH, FIRST_NAME_EN, LAST_NAME_EN, \r\n"
                                + //
                                "         N'' AS COMPANY_NAME_KH, '' AS COMPANY_NAME_EN, STATUS, \r\n" + //
                                "         PKG_CAMDX.STATUS_CLASSIFICATION(SCORE, STATUS) AS STATUS_DESC, \r\n" + //
                                "         B.USER_ID, \r\n" + //
                                "         B.USER_NAME, \r\n" + //
                                "         B.CREATED_TIME, TYPE\r\n" + //
                                "  FROM EKYC_PROFILE A, LOG_DATE B\r\n" + //
                                "  WHERE A.ID = B.TABLE_ID\r\n" + //
                                "        AND B.TABLE_NAME = 'EKYC_PROFILE'\r\n" + //
                                "        AND ((ID_NUMBER LIKE '%' || ? || '%') \r\n" + //
                                "            OR ((LAST_NAME_KH || ' ' || FIRST_NAME_KH) LIKE '%' || TO_NCHAR(?) || '%') \r\n"
                                + //
                                "            OR (UPPER(LAST_NAME_EN || ' ' || FIRST_NAME_EN) LIKE UPPER('%' || ? || '%')) \r\n"
                                + //
                                "            OR (FIRST_NAME_KH LIKE '%' || TO_NCHAR(?) || '%') \r\n" + //
                                "            OR (LAST_NAME_KH LIKE '%' || TO_NCHAR(?) || '%') \r\n" + //
                                "            OR (UPPER(FIRST_NAME_EN) LIKE UPPER('%' || ? || '%')) \r\n" + //
                                "            OR (UPPER(LAST_NAME_EN) LIKE UPPER('%' || ? || '%')))\r\n" + //
                                "  UNION ALL\r\n" + //
                                "  SELECT ID, 'eKYB' AS REQUEST_TYPE, APP_CODE, APP_CHANNEL,N'' AS FIRST_NAME_KH,N'' AS lAST_NAME_KH,'' AS FIRST_NAME_EN, '' AS LAST_NAME_EN, \r\n"
                                + //
                                "         COMPANY_NAME_KH, COMPANY_NAME_EN, STATUS, \r\n" + //
                                "         PKG_CAMDX.STATUS_CLASSIFICATION(SCORE, STATUS) AS STATUS_DESC, \r\n" + //
                                "         D.USER_ID, \r\n" + //
                                "         D.USER_NAME, \r\n" + //
                                "         D.CREATED_TIME, TYPE\r\n" + //
                                "  FROM EKYB_PROFILE C, LOG_DATE D\r\n" + //
                                "  WHERE C.ID = D.TABLE_ID\r\n" + //
                                "        AND D.TABLE_NAME = 'EKYB_PROFILE'\r\n" + //
                                "        AND ((SINGLE_ID LIKE '%' || ? || '%') \r\n" + //
                                "             OR (TIN LIKE '%' || ? || '%')\r\n" + //
                                "             OR (COMPANY_NAME_KH LIKE '%' || TO_NCHAR(?) || '%')\r\n" + //
                                "             OR (UPPER(COMPANY_NAME_EN) LIKE '%' || UPPER(?) || '%'))\r\n" + //
                                ")X \r\n" + //
                                "WHERE 1 = 1\r\n" + //
                                "      AND ((TRIM(?) IS NULL) OR (X.REQUEST_TYPE = ?))            --REQUEST_TYPE\r\n" + //
                                "      AND ((TRIM(?) IS NULL) OR (X.STATUS_DESC = ?))             --STATUS_DESC\r\n" + //
                                "      AND ((TRIM(?) IS NULL) OR (X.CREATED_TIME >= ?))           --FROM_DATE\r\n" + //
                                "      AND ((TRIM(?) IS NULL) OR (X.CREATED_TIME <= ?))           --TO_DATE\r\n" + //
                                "      AND ((TRIM(?) IS NULL) OR (X.USER_ID = ?)) \r\n" + //
                                "ORDER BY CREATED_TIME DESC\r\n" + //
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
                        history.setUserId(rs.getString("USER_ID"));
                        history.setUserName(rs.getString("USER_NAME"));
                        history.setCreateTime(rs.getString("CREATE_TIME2"));

                        return history;
                }, searchValue, searchValue, searchValue, searchValue, searchValue, searchValue, searchValue,
                                searchValue, searchValue, searchValue,
                                searchValue, requestType, requestType, statusDesc, statusDesc, fromDate, fromDate,
                                toDate, toDate,
                                userId, userId, offSet, size);
        }

        @SuppressWarnings("null")
        public int getHistoriesCount(String searchValue, String requestType, String statusDesc,
                        String fromDate, String toDate, String userId) {
                String sql = "SELECT COUNT(*) AS COUNT_VALUE FROM(  \r\n" + //
                                "  SELECT X.ID, X.APP_CODE, X.APP_CHANNEL, X.REQUEST_TYPE, X.FIRST_NAME_KH, X.LAST_NAME_KH, \r\n"
                                + //
                                "         X.FIRST_NAME_EN, X.LAST_NAME_EN, X.COMPANY_NAME_KH, X.COMPANY_NAME_EN, \r\n" + //
                                "         X.STATUS, X.STATUS_DESC, \r\n" + //
                                "         X.USER_ID, X.USER_NAME,\r\n" + //
                                "         X.CREATED_TIME,\r\n" + //
                                "         TO_CHAR(TO_DATE(X.CREATED_TIME, 'YYYYMMDDHH24MI'), 'YYYY-MM-DD HH24:MI') AS CREATE_TIME2\r\n"
                                + //
                                "  FROM(\r\n" + //
                                "    WITH LOG_DATE AS (SELECT TABLE_NAME, TABLE_ID, ACTION_TYPE, UNIT_ID, USER_ID, USER_NAME, CREATED_TIME \r\n"
                                + //
                                "                      FROM CAMDX_LOG T WHERE T.ACTION_TYPE = 'USER')\r\n" + //
                                "    SELECT ID, 'eKYC' AS REQUEST_TYPE, APP_CODE, APP_CHANNEL, FIRST_NAME_KH, lAST_NAME_KH, FIRST_NAME_EN, LAST_NAME_EN, \r\n"
                                + //
                                "           N'' AS COMPANY_NAME_KH, '' AS COMPANY_NAME_EN, STATUS, \r\n" + //
                                "           PKG_CAMDX.STATUS_CLASSIFICATION(SCORE, STATUS) AS STATUS_DESC, \r\n" + //
                                "           B.USER_ID, \r\n" + //
                                "           B.USER_NAME, \r\n" + //
                                "           B.CREATED_TIME\r\n" + //
                                "    FROM EKYC_PROFILE A, LOG_DATE B\r\n" + //
                                "    WHERE A.ID = B.TABLE_ID\r\n" + //
                                "          AND B.TABLE_NAME = 'EKYC_PROFILE'\r\n" + //
                                "          AND ((ID_NUMBER LIKE '%' || ? || '%') \r\n" + //
                                "              OR ((LAST_NAME_KH || ' ' || FIRST_NAME_KH) LIKE '%' || TO_NCHAR(?) || '%') \r\n"
                                + //
                                "              OR (UPPER(LAST_NAME_EN || ' ' || FIRST_NAME_EN) LIKE UPPER('%' || ? || '%')) \r\n"
                                + //
                                "              OR (FIRST_NAME_KH LIKE '%' || TO_NCHAR(?) || '%') \r\n" + //
                                "              OR (LAST_NAME_KH LIKE '%' || TO_NCHAR(?) || '%') \r\n" + //
                                "              OR (UPPER(FIRST_NAME_EN) LIKE UPPER('%' || ? || '%')) \r\n" + //
                                "              OR (UPPER(LAST_NAME_EN) LIKE UPPER('%' || ? || '%')))\r\n" + //
                                "    UNION ALL\r\n" + //
                                "    SELECT ID, 'eKYB' AS REQUEST_TYPE, APP_CODE, APP_CHANNEL,N'' AS FIRST_NAME_KH,N'' AS lAST_NAME_KH,'' AS FIRST_NAME_EN, '' AS LAST_NAME_EN, \r\n"
                                + //
                                "           COMPANY_NAME_KH, COMPANY_NAME_EN, STATUS, \r\n" + //
                                "           PKG_CAMDX.STATUS_CLASSIFICATION(SCORE, STATUS) AS STATUS_DESC, \r\n" + //
                                "           D.USER_ID, \r\n" + //
                                "           D.USER_NAME, \r\n" + //
                                "           D.CREATED_TIME\r\n" + //
                                "    FROM EKYB_PROFILE C, LOG_DATE D\r\n" + //
                                "    WHERE C.ID = D.TABLE_ID\r\n" + //
                                "          AND D.TABLE_NAME = 'EKYB_PROFILE'\r\n" + //
                                "          AND ((SINGLE_ID LIKE '%' || ? || '%') \r\n" + //
                                "               OR (TIN LIKE '%' || ? || '%')\r\n" + //
                                "               OR (COMPANY_NAME_KH LIKE '%' || TO_NCHAR(?) || '%')\r\n" + //
                                "               OR (UPPER(COMPANY_NAME_EN) LIKE '%' || UPPER(?) || '%'))\r\n" + //
                                "  )X \r\n" + //
                                "  WHERE 1 = 1\r\n" + //
                                "        AND ((TRIM(?) IS NULL) OR (X.REQUEST_TYPE = ?))            --REQUEST_TYPE\r\n"
                                + //
                                "        AND ((TRIM(?) IS NULL) OR (X.STATUS_DESC = ?))             --STATUS_DESC\r\n" + //
                                "        AND ((TRIM(?) IS NULL) OR (X.CREATED_TIME >= ?))           --FROM_DATE\r\n" + //
                                "        AND ((TRIM(?) IS NULL) OR (X.CREATED_TIME <= ?))           --TO_DATE\r\n" + //
                                "        AND ((TRIM(?) IS NULL) OR (X.USER_ID = ?)) \r\n" + //
                                ")";

                try {
                        return jdbcTemplate.queryForObject(sql, Integer.class, searchValue, searchValue, searchValue,
                                        searchValue, searchValue, searchValue,
                                        searchValue, searchValue, searchValue, searchValue,
                                        searchValue, requestType, requestType, statusDesc, statusDesc, fromDate,
                                        fromDate,
                                        toDate, toDate,
                                        userId, userId);
                } catch (Exception e) {
                        return 0;
                }
        }

        public List<Ekyc> getHistoryById(String id) {

                String sql = "SELECT X.* FROM (\r\n" + //
                                "  SELECT A.ID, A.APP_CODE, A.APP_CHANNEL, A.STATUS, A.SCORE, A.FACE_MOI_SCORE, A.TYPE, A.ERROR_DETAIL, A.ID_NUMBER, \r\n"
                                + //
                                "         A.FIRST_NAME_KH, A.LAST_NAME_KH, A.FIRST_NAME_EN, A.LAST_NAME_EN, A.GENDER,\r\n"
                                + //
                                "         TO_CHAR(A.DOB, 'YYYY-MM-DD') DOB, \r\n" + //
                                "         TO_CHAR(A.ISSUED_DATE, 'YYYY-MM-DD') ISSUED_DATE, TO_CHAR(A.EXPIRED_DATE, 'YYYY-MM-DD') EXPIRED_DATE, \r\n"
                                + //
                                "         A.NOTE, A.SELFIE_PATH, \r\n" + //
                                "         B.USER_ID,\r\n" + //
                                "         CASE WHEN B.ACTION_TYPE = 'USER' THEN B.USER_NAME\r\n" + //
                                "              ELSE 'SYSTEM'\r\n" + //
                                "         END AS USER_NAME,\r\n" + //
                                "         B.CREATED_TIME, \r\n" + //
                                "         TO_CHAR(TO_DATE(B.CREATED_TIME, 'YYYYMMDDHH24MI'), 'YYYY-MM-DD HH24:MI') AS CREATE_TIME2, \r\n"
                                + //
                                "              \r\n" + //
                                "       PKG_CAMDX.STATUS_CLASSIFICATION(A.SCORE, A.STATUS) AS STATUS_DESC \r\n" + //
                                "  FROM EKYC_PROFILE A, CAMDX_LOG B \r\n" + //
                                "  WHERE A.ID = B.TABLE_ID\r\n" + //
                                "        AND B.TABLE_NAME = 'EKYC_PROFILE' \r\n" + //
                                "        AND A.ID = ?\r\n" + //
                                "  ORDER BY B.ID ASC\r\n" + //
                                ")X ";

                return jdbcTemplate.query(sql, (rs, rowNum) -> {
                        Ekyc ekyc = new Ekyc();

                        ekyc.setId(rs.getString("ID"));
                        ekyc.setAppCode(rs.getString("APP_CODE"));
                        ekyc.setAppChannel(rs.getString("APP_CHANNEL"));

                        ekyc.setIdNumber(rs.getString("ID_NUMBER"));
                        ekyc.setFirstNameKh(rs.getString("FIRST_NAME_KH"));
                        ekyc.setLastNameKh(rs.getString("LAST_NAME_KH"));
                        ekyc.setFirstNameEn(rs.getString("FIRST_NAME_EN"));
                        ekyc.setLastNameEn(rs.getString("LAST_NAME_EN"));
                        ekyc.setGender(rs.getString("GENDER"));
                        ekyc.setDob(rs.getString("DOB"));
                        ekyc.setIssuedDate(rs.getString("ISSUED_DATE"));
                        ekyc.setExpiredDate(rs.getString("EXPIRED_DATE"));

                        String scoreString = rs.getString("SCORE") == null ? "0"
                                        : rs.getString("SCORE").isEmpty() ? "0" : rs.getString("SCORE");
                        double score = Double.parseDouble(scoreString) * 100;

                        ekyc.setScore(String.format("%.2f", score));
                        ekyc.setType(rs.getString("TYPE"));
                        ekyc.setNote(rs.getString("NOTE"));
                        ekyc.setSelfiePath(rs.getString("SELFIE_PATH"));
                        ekyc.setErrorDetail(rs.getString("ERROR_DETAIL"));
                        ekyc.setStatus(rs.getString("STATUS"));

                        String faceScoreString = rs.getString("FACE_MOI_SCORE") == null ? "0"
                                        : rs.getString("FACE_MOI_SCORE").isEmpty() ? "0"
                                                        : rs.getString("FACE_MOI_SCORE");
                        double faceScore = Double.parseDouble(faceScoreString) * 100;
                        ekyc.setFaceScore(String.format("%.2f", faceScore));
                        ekyc.setStatusDesc(rs.getString("STATUS_DESC"));

                        if (rowNum == 0) {
                                HistoryAction step1 = new HistoryAction();
                                step1.setDescription("ID " + rs.getString("ID_NUMBER") + " - New request submitted");
                                step1.setUserId(rs.getString("USER_ID"));
                                step1.setUserName(rs.getString("USER_NAME"));
                                step1.setActionDate(rs.getString("CREATE_TIME2"));

                                ekyc.setStep1(step1);
                        }
                        if (rowNum == 1) {
                                HistoryAction step2 = new HistoryAction();
                                step2.setDescription("Request Dispached automatically to CamDx");
                                step2.setUserId(rs.getString("USER_ID"));
                                step2.setUserName(rs.getString("USER_NAME"));
                                step2.setActionDate(rs.getString("CREATE_TIME2"));

                                ekyc.setStep2(step2);
                        }
                        if (rowNum > 1) {
                                HistoryAction step3 = new HistoryAction();
                                step3.setDescription("CamDx score: " + new DecimalFormat("0.##").format(score)
                                                + "/100 - " + rs.getString("STATUS_DESC"));
                                step3.setUserId(rs.getString("USER_ID"));
                                step3.setUserName(rs.getString("USER_NAME"));
                                step3.setActionDate(rs.getString("CREATE_TIME2"));

                                ekyc.setStep3(step3);
                        }
                        return ekyc;
                }, id);
        }

        // Dashboard Report
        public Map<String, String> getSummaryStatus(String fromDate, String toDate) {
                String sql = "SELECT X.STATUS, COUNT(*) AS COUNT_VALUE \r\n" + //
                                "FROM(\r\n" + //
                                "  WITH LOG_DATE AS (SELECT TABLE_NAME, TABLE_ID, MIN(CREATED_TIME) AS CREATED_TIME\r\n"
                                + //
                                "                    FROM CAMDX_LOG T \r\n" + //
                                "                    GROUP BY TABLE_NAME, TABLE_ID\r\n" + //
                                "                    )      \r\n" + //
                                "\r\n" + //
                                "  SELECT A.ID, 'eKYC' REQUEST_TYPE, A.STATUS, B.CREATED_TIME\r\n" + //
                                "  FROM EKYC_PROFILE A, LOG_DATE B\r\n" + //
                                "  WHERE A.ID = B.TABLE_ID \r\n" + //
                                "        AND B.TABLE_NAME = 'EKYC_PROFILE'\r\n" + //
                                "        AND A.STATUS IN (0, 1, 2, 3, 4)\r\n" + //
                                "  UNION ALL\r\n" + //
                                "  SELECT C.ID, 'eKYB' REQUEST_TYPE, C.STATUS, D.CREATED_TIME\r\n" + //
                                "  FROM EKYB_PROFILE C, LOG_DATE D\r\n" + //
                                "  WHERE C.ID = D.TABLE_ID \r\n" + //
                                "        AND D.TABLE_NAME = 'EKYB_PROFILE'\r\n" + //
                                "        AND C.STATUS IN (0, 1, 2, 3, 4)\r\n" + //
                                ")X \r\n" + //
                                "WHERE 1 = 1\r\n" + //
                                "      AND ((TRIM(?) IS NULL OR X.CREATED_TIME >= ?))          --FROM_DATE\r\n" + //
                                "      AND ((TRIM(?) IS NULL OR X.CREATED_TIME <= ?))          --TO_DATE\r\n" + //
                                "GROUP BY X.STATUS  ";

                Map<String, String> map = new HashMap<>();
                jdbcTemplate.query(sql, (rs, rowNum) -> {

                        map.put(rs.getString("STATUS"), rs.getString("COUNT_VALUE"));
                        return null;
                }, fromDate, fromDate, toDate, toDate);
                return map;
        }

        public Map<String, String> getRequestbyInputer(String fromDate, String toDate) {
                String sql = "SELECT USER_NAME, COUNT(*) AS COUNT_VALUE\r\n" + //
                                "FROM CAMDX_LOG \r\n" + //
                                "WHERE ACTION_TYPE = 'USER' \r\n" + //
                                "      AND ((TRIM(?) IS NULL) OR (CREATED_TIME >= ?))     --FROM_DATE\r\n" + //
                                "      AND ((TRIM(?) IS NULL) OR (CREATED_TIME <= ?))     --TO_DATE\r\n" + //
                                "GROUP BY USER_NAME ";

                Map<String, String> map = new HashMap<>();
                jdbcTemplate.query(sql, (rs, rowNum) -> {

                        map.put(rs.getString("USER_NAME"), rs.getString("COUNT_VALUE"));
                        return null;
                }, fromDate, fromDate, toDate, toDate);
                return map;
        }

        public Map<String, String> getSummaryByChannel(String fromDate, String toDate) {
                String sql = "SELECT X.APP_CHANNEL, COUNT(*) AS COUNT_VALUE \r\n" + //
                                "FROM(\r\n" + //
                                "  WITH LOG_DATE AS (SELECT TABLE_NAME, TABLE_ID, MIN(CREATED_TIME) AS CREATED_TIME\r\n"
                                + //
                                "                    FROM CAMDX_LOG T \r\n" + //
                                "                    GROUP BY TABLE_NAME, TABLE_ID\r\n" + //
                                "                    )      \r\n" + //
                                "\r\n" + //
                                "  SELECT A.ID, 'eKYC' REQUEST_TYPE, A.STATUS, B.CREATED_TIME, A.APP_CHANNEL\r\n" + //
                                "  FROM EKYC_PROFILE A, LOG_DATE B\r\n" + //
                                "  WHERE A.ID = B.TABLE_ID \r\n" + //
                                "        AND B.TABLE_NAME = 'EKYC_PROFILE'\r\n" + //
                                "  UNION ALL\r\n" + //
                                "  SELECT C.ID, 'eKYB' REQUEST_TYPE, C.STATUS, D.CREATED_TIME, C.APP_CHANNEL\r\n" + //
                                "  FROM EKYB_PROFILE C, LOG_DATE D\r\n" + //
                                "  WHERE C.ID = D.TABLE_ID \r\n" + //
                                "        AND D.TABLE_NAME = 'EKYB_PROFILE'\r\n" + //
                                ")X \r\n" + //
                                "WHERE 1 = 1\r\n" + //
                                "      AND ((TRIM(?) IS NULL OR X.CREATED_TIME >= ?))          --FROM_DATE\r\n" + //
                                "      AND ((TRIM(?) IS NULL OR X.CREATED_TIME <= ?))          --TO_DATE\r\n" + //
                                "GROUP BY X.APP_CHANNEL";

                Map<String, String> map = new HashMap<>();
                jdbcTemplate.query(sql, (rs, rowNum) -> {

                        map.put(rs.getString("APP_CHANNEL"), rs.getString("COUNT_VALUE"));
                        return null;
                }, fromDate, fromDate, toDate, toDate);
                return map;
        }

        @SuppressWarnings("null")
        public int checkEkycExisting(String idNumber, String firstNameKh, String lastNameKh, String firstNameEn,
                        String lastNameEn, String gender, String dob, String issuedDate, String expiredDate,
                        String type) {

                String sql = "SELECT COUNT(*) AS COUNT_VALUE \r\n" + //
                                "FROM EKYC_PROFILE \r\n" + //
                                "WHERE STATUS = 3 \r\n" + //
                                "      AND ((TRIM(?) IS NULL) OR (ID_NUMBER = ?))                            --ID_NUMBER\r\n"
                                + //
                                "      AND ((TRIM(?) IS NULL) OR (FIRST_NAME_KH = TO_NCHAR(?)))              --FIRST_NAME_KH\r\n"
                                + //
                                "      AND ((TRIM(?) IS NULL) OR (LAST_NAME_KH = TO_NCHAR(?)))               --LAST_NAME_KH\r\n"
                                + //
                                "      AND ((TRIM(?) IS NULL) OR (FIRST_NAME_EN = ?))                        --FIRST_NAME_EN\r\n"
                                + //
                                "      AND ((TRIM(?) IS NULL) OR (LAST_NAME_EN = ?))                         --LAST_NAME_EN\r\n"
                                + //
                                "      AND ((TRIM(?) IS NULL) OR (GENDER = ?))                               --GENDER\r\n"
                                + //
                                "      AND ((TRIM(?) IS NULL) OR (TO_CHAR(DOB, 'YYYY-MM-DD') = ?))           --DOB\r\n"
                                + //
                                "      AND ((TRIM(?) IS NULL) OR (TO_CHAR(ISSUED_DATE, 'YYYY-MM-DD') = ?))   --ISSUED_DATE\r\n"
                                + //
                                "      AND ((TRIM(?) IS NULL) OR (TO_CHAR(EXPIRED_DATE, 'YYYY-MM-DD') = ?))  --EXPIRED_DATE\r\n"
                                + //
                                "      AND (('1' = ?) OR (TYPE = ?))     ";

                try {
                        return jdbcTemplate.queryForObject(sql, Integer.class, idNumber, idNumber, firstNameKh,
                                        firstNameKh,
                                        lastNameKh, lastNameKh, firstNameEn, firstNameEn, lastNameEn, lastNameEn,
                                        gender,
                                        gender, dob, dob,
                                        issuedDate, issuedDate, expiredDate, expiredDate, type, type);
                } catch (Exception e) {
                        return 0;
                }
        }

        public List<ReportFull> getFullReport(String fromDate, String toDate, String channel, String requestType) {
                String sql = "SELECT X.* \r\n" + //
                                "FROM(\r\n" + //
                                "  WITH LOG_DATE AS (SELECT TABLE_NAME, TABLE_ID, MIN(CREATED_TIME) AS CREATED_TIME,  MAX(UNIT_ID) AS UNIT_ID\r\n"
                                + //
                                "                    FROM CAMDX_LOG T \r\n" + //
                                "                    GROUP BY TABLE_NAME, TABLE_ID\r\n" + //
                                "                    )\r\n" + //
                                "  SELECT A.ID_NUMBER, (A.LAST_NAME_EN || ' ' || A.FIRST_NAME_EN) AS CUSTOMER_NAME_EN, \r\n"
                                + //
                                "         TO_CHAR(TO_DATE(B.CREATED_TIME, 'YYYYMMDDHH24MI'), 'DD/MM/YYYY') AS DATE_ASSESSMENT, \r\n"
                                + //
                                "         PKG_CAMDX.report_assessment_status(A.SCORE, A.STATUS) AS CURREENT_ASSESSMENT, \r\n"
                                + //
                                "         A.SCORE AS CURRENT_SCORE, PKG_CAMDX.report_status(A.STATUS) AS STATUS, A.GENDER, \r\n"
                                + //
                                "         TO_CHAR(A.DOB, 'DD/MM/YYYY') AS DOB, A.ID_NUMBER AS NATIONAL_ID, \r\n" + //
                                "         (A.LAST_NAME_KH || ' ' || A.FIRST_NAME_KH) AS CUSTOMER_NAME_KH, 'ID NUMBER' AS LEGAL_DOC_NAME, \r\n"
                                + //
                                "         'CAMBODIA' AS NATIONALITY, 'KH' AS RESIDENCE, 'INDIVIDUAL'AS SECTOR, \r\n" + //
                                "         PKG_CAMDX.get_branch_id(B.UNIT_ID) AS BRANCH_CODE, \r\n" + //
                                "         TO_CHAR(A.ISSUED_DATE, 'DD/MM/YYYY') AS ISSUED_DATE, TO_CHAR(A.EXPIRED_DATE, 'DD/MM/YYYY') AS EXPIRED_DATE, \r\n"
                                + //
                                "         'eKYC' AS REQUEST_TYPE, A.APP_CHANNEL, B.CREATED_TIME\r\n" + //
                                "  FROM EKYC_PROFILE A, LOG_DATE B\r\n" + //
                                "  WHERE A.ID = B.TABLE_ID\r\n" + //
                                "        AND B.TABLE_NAME = 'EKYC_PROFILE'\r\n" + //
                                "\r\n" + //
                                "  UNION ALL \r\n" + //
                                "  SELECT CASE WHEN C.TYPE IN (1, 3) THEN C.SINGLE_ID \r\n" + //
                                "              ELSE C.TIN \r\n" + //
                                "         END AS ID_NUMBER, C.COMPANY_NAME_EN AS CUSTOMER_NAME_EN, \r\n" + //
                                "         TO_CHAR(TO_DATE(D.CREATED_TIME, 'YYYYMMDDHH24MI'), 'DD/MM/YYYY') AS DATE_ASSESSMENT, \r\n"
                                + //
                                "         PKG_CAMDX.report_assessment_status(C.SCORE, C.STATUS) AS CURREENT_ASSESSMENT, \r\n"
                                + //
                                "         C.SCORE AS CURRENT_SCORE, PKG_CAMDX.report_status(C.STATUS) AS STATUS, '' AS GENDER, \r\n"
                                + //
                                "         '' AS DOB, \r\n" + //
                                "         CASE WHEN C.TYPE IN (1, 3) THEN C.SINGLE_ID \r\n" + //
                                "              ELSE C.TIN \r\n" + //
                                "         END AS NATIONAL_ID, \r\n" + //
                                "         C.COMPANY_NAME_KH AS CUSTOMER_NAME_KH, \r\n" + //
                                "         CASE WHEN C.TYPE IN (1, 3) THEN 'Single ID' \r\n" + //
                                "              ELSE 'Tax identification number (TIN)'\r\n" + //
                                "         END AS LEGAL_DOC_NAME, \r\n" + //
                                "         'CAMBODIA' AS NATIONALITY, 'KH' AS RESIDENCE, 'COMPANY'AS SECTOR, \r\n" + //
                                "         PKG_CAMDX.get_branch_id(D.UNIT_ID) AS BRANCH_CODE, \r\n" + //
                                "         '' AS ISSUED_DATE, '' AS EXPIRED_DATE, \r\n" + //
                                "         'eKYB' AS REQUEST_TYPE, C.APP_CHANNEL, D.CREATED_TIME\r\n" + //
                                "  FROM EKYB_PROFILE C, LOG_DATE D\r\n" + //
                                "  WHERE C.ID = D.TABLE_ID \r\n" + //
                                "        AND D.TABLE_NAME = 'EKYB_PROFILE'\r\n" + //
                                ")X\r\n" + //
                                "WHERE 1 = 1\r\n" + //
                                "      AND ((TRIM(?) IS NULL) OR (X.CREATED_TIME >= ?))           --FROM_DATE\r\n" + //
                                "      AND ((TRIM(?) IS NULL) OR (X.CREATED_TIME <= ?))           --TO_DATE\r\n" + //
                                "      AND ((TRIM(?) IS NULL) OR (X.APP_CHANNEL = ?))             --APP_CHANNEL\r\n" + //
                                "      AND ((TRIM(?) IS NULL) OR (X.REQUEST_TYPE = ?))            --REQUEST_TYPE\r\n" + //
                                "ORDER BY X.CREATED_TIME ASC ";

                return jdbcTemplate.query(sql, (rs, rowNum) -> {
                        ReportFull reportFull = new ReportFull();

                        reportFull.setCustomerNameEn(rs.getString("CUSTOMER_NAME_EN"));
                        reportFull.setDateAssessment(rs.getString("DATE_ASSESSMENT"));
                        reportFull.setCurrentAssessment(rs.getString("CURREENT_ASSESSMENT"));

                        String scoreString = rs.getString("CURRENT_SCORE") == null ? "0"
                                        : rs.getString("CURRENT_SCORE").isEmpty() ? "0" : rs.getString("CURRENT_SCORE");
                        double score = Double.parseDouble(scoreString) * 100;
                        reportFull.setCurrentScore(String.format("%.2f", score));

                        reportFull.setStatus(rs.getString("STATUS"));
                        reportFull.setGender(rs.getString("GENDER"));
                        reportFull.setBirthDate(rs.getString("DOB"));
                        reportFull.setNationalId(rs.getString("NATIONAL_ID"));
                        reportFull.setCustomerNameKh(rs.getString("CUSTOMER_NAME_KH"));
                        reportFull.setLegalDocName(rs.getString("LEGAL_DOC_NAME"));
                        reportFull.setNationality(rs.getString("NATIONALITY"));
                        reportFull.setResidence(rs.getString("RESIDENCE"));
                        reportFull.setSector(rs.getString("SECTOR"));
                        reportFull.setBranchCode(rs.getString("BRANCH_CODE"));
                        reportFull.setIssuedDate(rs.getString("ISSUED_DATE"));
                        reportFull.setExpiredDate(rs.getString("EXPIRED_DATE"));

                        reportFull.setRequestType(rs.getString("REQUEST_TYPE"));
                        reportFull.setChannel(rs.getString("APP_CHANNEL"));

                        return reportFull;
                }, fromDate, fromDate, toDate, toDate, channel, channel, requestType, requestType);
        }

}
