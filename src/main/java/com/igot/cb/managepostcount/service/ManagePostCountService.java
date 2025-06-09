package com.igot.cb.managepostcount.service;

import com.igot.cb.pores.util.ApiResponse;
import org.springframework.stereotype.Service;

@Service
public interface ManagePostCountService {

    ApiResponse getPostCount(String userId);

}
