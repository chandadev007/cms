package com.main.queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.main.model.Ekyb;
import com.main.model.Ekyc;
import com.main.service.DBLogService;
import com.main.service.EkybService;
import com.main.service.EkycService;
import java.util.List;

@Component
public class CamdxBatchScheduler {

    private static final Logger logger = LoggerFactory.getLogger(CamdxBatchScheduler.class);
    private final EkycService ekycService;
    private final EkybService ekybService;
    private final DBLogService dbLogService;

    // Injected separate workers
    private final EkycGatewayWorker ekycGatewayWorker;
    private final EkybGatewayWorker ekybGatewayWorker;

    public CamdxBatchScheduler(EkycService ekycService, EkybService ekybService, DBLogService dbLogService,
            EkycGatewayWorker ekycGatewayWorker, EkybGatewayWorker ekybGatewayWorker) {
        this.ekycService = ekycService;
        this.ekybService = ekybService;
        this.dbLogService = dbLogService;
        this.ekycGatewayWorker = ekycGatewayWorker;
        this.ekybGatewayWorker = ekybGatewayWorker;
    }

    // Checks the Database pool every 10 seconds for Status 1 records
    @Scheduled(fixedDelayString = "${cms.camdx.scheduler.interval:10000}")
    public void runDatabaseToQueueFlow() {

        // -----------------------------------------------------------------
        // 1. EKYC PROCESSING BLOCK
        // -----------------------------------------------------------------
        try {
            List<Ekyc> ekycPendingRecords = ekycService.findPendingRecords();
            if (ekycPendingRecords != null && ekycPendingRecords.size() > 0) {
                for (Ekyc ekyc : ekycPendingRecords) {
                    try {
                        // Route via dedicated Ekyc Worker
                        if ("1".equalsIgnoreCase(ekyc.getType())) {
                            // Update status in DB
                            ekycService.updateToProcessing(ekyc.getId());
                            dbLogService.createEkycLog("update status to processing by worker", "SYSTEM", ekyc.getId(),
                                    null, null, null);

                            ekycGatewayWorker.sendToEkycVerifyInfo(ekyc);
                        }

                        // verify with face
                        else if ("2".equalsIgnoreCase(ekyc.getType())) {
                            // Update status in DB
                            ekycService.updateToProcessing(ekyc.getId());
                            dbLogService.createEkycLog("update status to processing by worker", "SYSTEM",
                                    ekyc.getId(),
                                    null, null, null);

                            ekycGatewayWorker.sendToEkycVerifyFace(ekyc);
                        }
                    } catch (Exception itemEx) {
                        logger.error("Failed processing individual Ekyc record ID: " + ekyc.getId(), itemEx);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Critical error during Ekyc queue extraction branch: ", e);
        }

        // -----------------------------------------------------------------
        // 2. EKYB PROCESSING BLOCK
        // -----------------------------------------------------------------
        try {
            List<Ekyb> ekybPendingRecords = ekybService.findPendingRecords();
            if (ekybPendingRecords != null && ekybPendingRecords.size() > 0) {
                for (Ekyb ekyb : ekybPendingRecords) {
                    try {
                        // Route via dedicated Ekyb Worker
                        if ("1".equalsIgnoreCase(ekyb.getType())) {
                            // Update status in DB
                            ekybService.updateToProcessing(ekyb.getId());
                            dbLogService.createEkybLog("update status to processing by worker", "SYSTEM", ekyb.getId(),
                                    null, null, null);

                            ekybGatewayWorker.sendToEkybVerifyBasicInfo(ekyb);
                        }
                        // verify with TIN
                        else if ("2".equalsIgnoreCase(ekyb.getType())) {
                            // Update status in DB
                            ekybService.updateToProcessing(ekyb.getId());
                            dbLogService.createEkybLog("update status to processing by worker", "SYSTEM", ekyb.getId(),
                                    null, null, null);

                            ekybGatewayWorker.sendToEkybVerifyWithTIN(ekyb);
                        }
                    } catch (Exception itemEx) {
                        logger.error("Failed processing individual Ekyb record ID: " + ekyb.getId(), itemEx);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Critical error during Ekyb queue extraction branch: ", e);
        }
    }
}