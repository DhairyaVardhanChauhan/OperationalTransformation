package com.dvc.OTAction.controller;
import com.dvc.OTAction.dto.TextOperation;
import com.dvc.OTAction.service.OtService;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import com.codecafe.backend.dto.IncomingOperationPayload;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

@Controller
public class OtController {
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(OtController.class);
    private final OtService otService;
    private final SimpMessagingTemplate messagingTemplate;
    private static final Logger logger = Logger.getLogger(OtController.class.getName());
    public OtController(OtService otService, SimpMessagingTemplate messagingTemplate) {
        this.otService = otService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/operation")
    public void handleOperation(@Payload IncomingOperationPayload payload, SimpMessageHeaderAccessor headerAccessor, Principal principal){
        String clientId = payload.getClientId();
        String documentId = payload.getDocumentId();
        String sessionId = payload.getSessionId();

        if(clientId == null || documentId == null || sessionId ==null){
            logger.warning("Received payload with no clientId,documentId,sessionId");
        }
        logger.info(String.format("OtController received operation payload from client [%s] for session [%s], doc [%s]: %s",
                clientId, sessionId, documentId, payload.toString()));

        try{
            TextOperation operation = new TextOperation(payload.getOperation());
            TextOperation transformedOp = otService.receiveOperation(sessionId,documentId,payload.getRevision(),operation);
            // broadCastMessage
            Map<String,Object> broadcastPayLoad = new HashMap<>();
            broadcastPayLoad.put("documentId",documentId);
            broadcastPayLoad.put("clientId",clientId);
            broadcastPayLoad.put("operation",transformedOp.getOps());
            broadcastPayLoad.put("sessionId",sessionId);
            if (payload.getCursorPosition() != null) {
                broadcastPayLoad.put("cursorPosition", payload.getCursorPosition());
            }
            String allDestinations = "/topic/sessions";
            messagingTemplate.convertAndSend(allDestinations,broadcastPayLoad);
            String ackDestination = "/topic/ack/" + clientId;
            messagingTemplate.convertAndSend(ackDestination, "ack");
            logger.fine("Sent ACK to client [" + clientId + "] at " + ackDestination);
        }catch (Exception e){
            logger.warning(e.getMessage());
        }

    }

}
