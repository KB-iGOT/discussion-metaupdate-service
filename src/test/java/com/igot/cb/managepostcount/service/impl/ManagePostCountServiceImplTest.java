package com.igot.cb.managepostcount.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.igot.cb.pores.cache.CacheService;
import com.igot.cb.pores.elasticsearch.dto.SearchCriteria;
import com.igot.cb.pores.elasticsearch.dto.SearchResult;
import com.igot.cb.pores.elasticsearch.service.EsUtilService;
import com.igot.cb.pores.util.ApiResponse;
import com.igot.cb.pores.util.CbServerProperties;
import com.igot.cb.pores.util.Constants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class ManagePostCountServiceImplTest {

    @Mock
    private CacheService cacheService;

    @Mock
    private CbServerProperties cbServerProperties;

    @Mock
    private EsUtilService esUtilService;

    @InjectMocks
    @Spy
    private ManagePostCountServiceImpl managePostCountService;

    @Mock
    private ObjectMapper objectMapper;

    /**
     * Test case for fetchPostCountFromElasticsearch method when an exception occurs during Elasticsearch search.
     * This test verifies that the method properly propagates exceptions thrown by the esUtilService.
     */
    @Test
    void test_fetchPostCountFromElasticsearch_elasticsearchException() throws Exception {
        // Arrange
        String userId = "testUser";
        when(objectMapper.readValue(any(String.class), any(Class.class))).thenReturn(new SearchCriteria());
        when(esUtilService.searchDocuments(any(), any())).thenThrow(new RuntimeException("Elasticsearch error"));

        // Act & Assert
        assertThrows(Exception.class, () -> managePostCountService.fetchPostCountFromElasticsearch(userId));
    }

    /**
     * Test case for fetchPostCountFromElasticsearch method
     * Verifies that the method correctly fetches the post count from Elasticsearch
     * and caches the result for future requests
     */
    @Test
    void fetchPostCountFromElasticsearch_success() throws Exception {
        String userId = "user123";
        String criteriaJson = "{\"filterCriteriaMap\":{}}";

        SearchCriteria mockCriteria = new SearchCriteria();
        mockCriteria.setFilterCriteriaMap(new java.util.HashMap<>());

        SearchResult mockSearchResult = new SearchResult();
        mockSearchResult.setTotalCount(5L);

        when(cbServerProperties.getUserPostCountSearchCriteria()).thenReturn(criteriaJson);
        when(objectMapper.readValue(criteriaJson, SearchCriteria.class)).thenReturn(mockCriteria);
        when(cbServerProperties.getDiscussionEntity()).thenReturn("discussion_entity");
        when(esUtilService.searchDocuments(eq("discussion_entity"), any(SearchCriteria.class)))
                .thenReturn(mockSearchResult);

        long result = managePostCountService.fetchPostCountFromElasticsearch(userId);

        assertEquals(5L, result);

        // Verify interactions
        verify(objectMapper).readValue(criteriaJson, SearchCriteria.class);
        verify(esUtilService).searchDocuments(eq("discussion_entity"), any(SearchCriteria.class));
        verify(cacheService).putCacheWithoutPrefix(eq("user:postCount_" + userId), eq(5L));

        // Verify that `createdBy` was set properly in criteria
        assertEquals(userId, mockCriteria.getFilterCriteriaMap().get("createdBy"));
    }

    /**
     * Tests the getPostCount method with a blank user ID.
     * This is an edge case explicitly handled in the method implementation.
     * Expected behavior: Returns a BAD_REQUEST response with FAILED status.
     */
    @Test
    void test_getPostCount_blankUserId() {
        ManagePostCountServiceImpl service = new ManagePostCountServiceImpl();
        ApiResponse response = service.getPostCount("");

        assertEquals(HttpStatus.BAD_REQUEST, response.getResponseCode());
        assertEquals(Constants.FAILED, response.getParams().getStatus());
    }

    /**
     * Tests the getPostCount method with a null user ID.
     * This is an edge case explicitly handled in the method implementation.
     * Expected behavior: Returns a BAD_REQUEST response with FAILED status.
     */
    @Test
    void test_getPostCount_nullUserId() {
        ManagePostCountServiceImpl service = new ManagePostCountServiceImpl();
        ApiResponse response = service.getPostCount(null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getResponseCode());
        assertEquals(Constants.FAILED, response.getParams().getStatus());
    }

    /**
     * Test case for getPostCount method when userId is not blank and post count data is available in cache.
     * This test verifies that the method returns the correct ApiResponse with post count fetched from cache.
     */
    @Test
    void test_getPostCount_whenUserIdIsValidAndPostCountInCache() {
        String userId = "testUser123";
        String cachedPostCount = "5";

        when(cacheService.getCacheWithoutPrefix("user:postCount_" + userId)).thenReturn(cachedPostCount);

        ApiResponse response = managePostCountService.getPostCount(userId);

        assertEquals(Integer.parseInt(cachedPostCount), response.getResult().get("postCount"));
    }

    /**
     * Test case for getPostCount method when userId is not blank and postCount is not cached.
     * This test verifies that the method fetches the post count from Elasticsearch when it's not in the cache.
     */
    @Test
    void test_getPostCount_whenUserIdValidAndPostCountNotCached() throws Exception {
        // Arrange
        String userId = "testUser";
        long expectedPostCount = 5L;

        when(cacheService.getCacheWithoutPrefix("user:postCount_" + userId)).thenReturn(null);
        doReturn(expectedPostCount).when(managePostCountService).fetchPostCountFromElasticsearch(userId);

        // Act
        ApiResponse response = managePostCountService.getPostCount(userId);

        // Assert
        assertEquals(HttpStatus.OK, response.getResponseCode());
        assertEquals(Constants.SUCCESS, response.getParams().getStatus());
        assertEquals(expectedPostCount, response.getResult().get("postCount"));
        verify(cacheService).getCacheWithoutPrefix("user:postCount_" + userId);
        verify(managePostCountService).fetchPostCountFromElasticsearch(userId);
    }

    @Test
    void getPostCount_whenFetchPostCountThrowsException_logsError() throws Exception {
        String userId = "user123";
        when(cacheService.getCacheWithoutPrefix("user:postCount_" + userId)).thenReturn(null);

        doThrow(new Exception("ES failure")).when(managePostCountService).fetchPostCountFromElasticsearch(userId);

        ApiResponse response = managePostCountService.getPostCount(userId);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getResponseCode());
        assertEquals(Constants.FAILED, response.getParams().getStatus());
    }

}
