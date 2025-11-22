package com.dvc.OTAction.service;
import com.dvc.OTAction.dto.TextOperation;
import com.dvc.OTAction.utils.OTUtils;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
@Service
public class OtService {
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(OtService.class);
    private final OTUtils otUtils;
    private static final Logger logger = Logger.getLogger(OtService.class.getName());
    Map<String,String> docContentMap = new HashMap<>();
    private final String contentKey = "CONTENT_KEY";
    public OtService(OTUtils otUtils) {
        this.otUtils = otUtils;
    }

    public TextOperation receiveOperation(String sessionId, String documentId, int revision, TextOperation operation) {

        try{
            String currentContent = getDocumentContent(sessionId,documentId);
            TextOperation transformedOperation= operation;
            String newContent = OTUtils.apply(currentContent,transformedOperation);
            docContentMap.putIfAbsent(contentKey,newContent);
            return transformedOperation;
        }catch (Exception e){
            logger.warning(e.getMessage());
            throw new RuntimeException("Failed!  ", e);
        }
    }


    private String getDocumentContent(String sessionId, String documentId) {

        String content = docContentMap.get(contentKey);
        return content == null? "":content;
    }
}
