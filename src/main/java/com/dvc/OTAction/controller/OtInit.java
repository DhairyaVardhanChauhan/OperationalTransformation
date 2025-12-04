package com.dvc.OTAction.controller;

import com.dvc.OTAction.service.OtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
public class OtInit {
    @Autowired
    private OtService otService;

    @GetMapping("/ot/init")
    public Map<String, Object> init(@RequestParam String sessionId,@RequestParam String documentId){
        Map<String, Object> mp = new HashMap<>();
        String currentContent = otService.getContent(sessionId, documentId);
        int revision = otService.getServerRevision(sessionId,documentId);
        mp.put("content",currentContent);
        mp.put("revision",revision);
        return mp;
    }
}
