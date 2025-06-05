package com.igot.cb.managepostcount.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.igot.cb.managepostcount.service.ManagePostCountService;
import com.igot.cb.pores.cache.CacheService;
import com.igot.cb.pores.elasticsearch.dto.SearchCriteria;
import com.igot.cb.pores.elasticsearch.dto.SearchResult;
import com.igot.cb.pores.elasticsearch.service.EsUtilService;
import com.igot.cb.pores.util.ApiResponse;
import com.igot.cb.pores.util.CbServerProperties;
import com.igot.cb.pores.util.Constants;
import com.igot.cb.pores.util.ProjectUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ManagePostCountServiceImpl implements ManagePostCountService {
    @Autowired
    private CacheService cacheService;
    @Autowired
    private EsUtilService esUtilService;
    @Autowired
    private CbServerProperties cbServerProperties;
    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public ApiResponse getPostCount(String userId) {
        ApiResponse response = ProjectUtil.createDefaultResponse("api.v1.postcount");
        if (StringUtils.isBlank(userId)) {
            log.error("User ID is blank or null");
            response.setResponseCode(HttpStatus.BAD_REQUEST);
            response.getParams().setStatus(Constants.FAILED);
            return response;
        }

        String postCountData = cacheService.getCacheWithoutPrefix("user:postCount_" + userId);
        if (postCountData != null) {
            log.info("Post count for user {} fetched from cache: {}", userId, postCountData);
            response.getResult().put("postCount", Integer.parseInt(postCountData));
            return response;
        }

        try {
            long postCount = fetchPostCountFromElasticsearch(userId);
            response.getResult().put("postCount", postCount);
        } catch (Exception e) {
            log.error("Error occurred while getting post count for user {}", userId, e);
            response.getParams().setStatus(Constants.FAILED);
            response.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR);
            return response;
        }
        return response;
    }

    public long fetchPostCountFromElasticsearch(String userId) throws Exception {
        SearchCriteria searchCriteria = objectMapper.readValue(cbServerProperties.getUserPostCountSearchCriteria(), SearchCriteria.class);
        searchCriteria.getFilterCriteriaMap().put("createdBy", userId);

        SearchResult searchResult = esUtilService.searchDocuments(cbServerProperties.getDiscussionEntity(), searchCriteria);
        long postCount = searchResult.getTotalCount();

        // Cache the result for future requests
        cacheService.putCacheWithoutPrefix("user:postCount_" + userId, postCount);
        return postCount;
    }
}
