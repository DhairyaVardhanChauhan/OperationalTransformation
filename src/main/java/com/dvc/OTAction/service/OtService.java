package com.dvc.OTAction.service;

import com.dvc.OTAction.dto.TextOperation;
import com.dvc.OTAction.utils.OTUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class OtService {

    private static final Logger logger = Logger.getLogger(OtService.class.getName());

    private final OTUtils otUtils;
    private final ObjectMapper objectMapper;

    // In-memory doc & history for demo — replace with Redis later
    private final Map<String, String> docContentMap = new HashMap<>();
    private final Map<String, List<String>> historyMap = new HashMap<>();
    private final ReentrantLock reentrantLock = new ReentrantLock();

    public OtService(OTUtils otUtils, ObjectMapper objectMapper) {
        this.otUtils = otUtils;
        this.objectMapper = objectMapper;
    }

    public TextOperation receiveOperation(String sessionId, String documentId, int clientRevision, TextOperation operation, String clientId) {
        try {
            reentrantLock.lock();

            String currentContent = getContent(sessionId, documentId);
            int serverRevision = getServerRevision(sessionId, documentId);

            if (clientRevision < 0 || clientRevision > serverRevision) {
                throw new IllegalArgumentException(
                        String.format("[Session: %s, Doc: %s] Invalid client revision: %d. Server revision is: %d.",
                                sessionId, documentId, clientRevision, serverRevision)
                );
            }

            List<TextOperation> concurrentOps = new ArrayList<>();

            if (clientRevision < serverRevision) {
                try {
                    String hKey = historyKey(sessionId, documentId);
                    List<String> allRawOperations = historyMap.get(hKey);

                    if (allRawOperations != null && !allRawOperations.isEmpty()) {
                        List<String> rawOperations = allRawOperations.subList(clientRevision, serverRevision);

                        for (String opJson : rawOperations) {
                            try {
                                List<Object> opsList = objectMapper.readValue(opJson, new TypeReference<List<Object>>() {});
                                concurrentOps.add(new TextOperation(opsList));
                            } catch (JsonProcessingException e) {
                                logger.warning(String.format(
                                        "[Session: %s, Doc: %s] Failed to parse operation JSON from history: %s. JSON: %s",
                                        sessionId, documentId, e.getMessage(), opJson));
                                throw new IllegalStateException("Invalid operation format found in history for key: " + hKey, e);
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.log(Level.SEVERE, String.format(
                            "[Session: %s, Doc: %s] Error getting concurrent ops (rev %d to %d): %s",
                            sessionId, documentId, clientRevision, serverRevision, e.getMessage()), e);
                    throw new RuntimeException(e);
                }
            }

            logger.fine(String.format("[Session: %s, Doc: %s] Found %d concurrent operations to transform against.",
                    sessionId, documentId, concurrentOps.size()));

            TextOperation transformedOperation = operation;
            for (TextOperation concurrentOp : concurrentOps) {
                logger.fine(String.format("[Session: %s, Doc: %s] Transforming against concurrent op: %s",
                        sessionId, documentId, concurrentOp));
                List<TextOperation> result = OTUtils.transform(transformedOperation, concurrentOp);
                transformedOperation = result.get(0);
                logger.fine(String.format("[Session: %s, Doc: %s] Result after transform: %s",
                        sessionId, documentId, transformedOperation));
            }

            String newContent = OTUtils.apply(currentContent, transformedOperation);
            saveContent(sessionId, documentId, newContent);
            saveHistoryOp(sessionId, documentId, transformedOperation);

            return transformedOperation;

        } catch (Exception e) {;
            logger.warning(e.getMessage());
            throw new RuntimeException(("Failed to process operation for client " + clientId+clientRevision + " " + getServerRevision(sessionId,documentId)),e);
        } finally {
            reentrantLock.unlock();
        }
    }

    public String getContent(String sessionId, String documentId) {
        return docContentMap.getOrDefault(contentKey(sessionId, documentId), "");
    }

    private void saveContent(String sessionId, String documentId, String content) {
        docContentMap.put(contentKey(sessionId, documentId), content);
    }

    private String contentKey(String sessionId, String documentId) {
        return sessionId + ":" + documentId + ":content";
    }

    private String historyKey(String sessionId, String documentId) {
        return sessionId + ":" + documentId + ":history";
    }

    public int getServerRevision(String sessionId, String documentId) {
        List<String> history = historyMap.get(historyKey(sessionId, documentId));
        return history == null ? 0 : history.size();
    }

    private void saveHistoryOp(String sessionId, String documentId, TextOperation op) {
        String hKey = historyKey(sessionId, documentId);
        historyMap.computeIfAbsent(hKey, k -> new ArrayList<>());

        try {
            String serialized = objectMapper.writeValueAsString(op.getOps());
            historyMap.get(hKey).add(serialized);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize operation", e);
        }
    }
}