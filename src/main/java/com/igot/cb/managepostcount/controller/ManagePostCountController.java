package com.igot.cb.managepostcount.controller;

import com.igot.cb.managepostcount.service.ManagePostCountService;
import com.igot.cb.pores.util.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ManagePostCountController {

    @Autowired
    private ManagePostCountService managePostCountService;

    @GetMapping("/v1/postcount/{userId}")
    public ResponseEntity <ApiResponse> getPostCount(@PathVariable String userId ){
        ApiResponse response = managePostCountService.getPostCount(userId);
        return new ResponseEntity<>(response,response.getResponseCode());
    }
}
