package com.igot.cb.managepostcount.controller;

import com.igot.cb.managepostcount.service.ManagePostCountService;
import com.igot.cb.pores.util.ApiResponse;
import com.igot.cb.pores.util.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ManagePostCountControllerTest {

    @InjectMocks
    private ManagePostCountController controller;

    @Mock
    private ManagePostCountService managePostCountService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getPostCount_shouldReturnResponseFromService() {
        String userId = "user123";

        ApiResponse mockResponse = new ApiResponse();
        mockResponse.setResponseCode(HttpStatus.OK);
        mockResponse.setResult(new HashMap<>());
        mockResponse.getResult().put("postCount", 10);
        mockResponse.getParams().setStatus(Constants.SUCCESS);

        when(managePostCountService.getPostCount(userId)).thenReturn(mockResponse);

        ResponseEntity<ApiResponse> responseEntity = controller.getPostCount(userId);

        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(mockResponse, responseEntity.getBody());
        assertEquals(10, responseEntity.getBody().getResult().get("postCount"));
        assertEquals(Constants.SUCCESS, responseEntity.getBody().getParams().getStatus());

        verify(managePostCountService, times(1)).getPostCount(userId);
    }
}
