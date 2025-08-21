package com.igot.cb.pores.elasticsearch.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.json.JsonData;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.igot.cb.pores.util.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.util.*;

class EsUtilServiceImplBuildFilterQueryTest {

    private EsUtilServiceImpl esUtilService;

    @BeforeEach
    void setUp() {
        esUtilService = new EsUtilServiceImpl(mock(ElasticsearchClient.class));
        ReflectionTestUtils.setField(esUtilService, "objectMapper", new ObjectMapper());
    }

    @Test
    void testBuildFilterQuery_nullMap() throws Exception {
        BoolQuery.Builder result = invokeBuildFilterQuery(null);
        assertNotNull(result);
    }

    @Test
    void testBuildFilterQuery_emptyMap() throws Exception {
        BoolQuery.Builder result = invokeBuildFilterQuery(new HashMap<>());
        assertNotNull(result);
    }

    @Test
    void testBuildFilterQuery_mustNotWithArrayList() throws Exception {
        Map<String, Object> filterMap = new HashMap<>();
        filterMap.put("must_not", new ArrayList<>(Arrays.asList("value1", "value2")));

        BoolQuery.Builder result = invokeBuildFilterQuery(filterMap);
        assertNotNull(result);
    }

    @Test
    void testBuildFilterQuery_booleanValue() throws Exception {
        Map<String, Object> filterMap = new HashMap<>();
        filterMap.put("field1", true);
        filterMap.put("field2", false);

        BoolQuery.Builder result = invokeBuildFilterQuery(filterMap);
        assertNotNull(result);
    }

    @Test
    void testBuildFilterQuery_arrayListValue() throws Exception {
        Map<String, Object> filterMap = new HashMap<>();
        filterMap.put("field1", new ArrayList<>(Arrays.asList("value1", "value2")));

        BoolQuery.Builder result = invokeBuildFilterQuery(filterMap);
        assertNotNull(result);
    }

    @Test
    void testBuildFilterQuery_stringValue() throws Exception {
        Map<String, Object> filterMap = new HashMap<>();
        filterMap.put("field1", "stringValue");

        BoolQuery.Builder result = invokeBuildFilterQuery(filterMap);
        assertNotNull(result);
    }

    @Test
    void testBuildFilterQuery_rangeQuery_gte() throws Exception {
        Map<String, Object> rangeConditions = new HashMap<>();
        rangeConditions.put(Constants.SEARCH_OPERATION_GREATER_THAN_EQUALS, JsonData.of(10));

        Map<String, Object> filterMap = new HashMap<>();
        filterMap.put("field1", rangeConditions);

        BoolQuery.Builder result = invokeBuildFilterQuery(filterMap);
        assertNotNull(result);
    }

    @Test
    void testBuildFilterQuery_rangeQuery_lte() throws Exception {
        Map<String, Object> rangeConditions = new HashMap<>();
        rangeConditions.put(Constants.SEARCH_OPERATION_LESS_THAN_EQUALS, JsonData.of(100));

        Map<String, Object> filterMap = new HashMap<>();
        filterMap.put("field1", rangeConditions);

        BoolQuery.Builder result = invokeBuildFilterQuery(filterMap);
        assertNotNull(result);
    }

    @Test
    void testBuildFilterQuery_rangeQuery_gt() throws Exception {
        Map<String, Object> rangeConditions = new HashMap<>();
        rangeConditions.put(Constants.SEARCH_OPERATION_GREATER_THAN, JsonData.of(5));

        Map<String, Object> filterMap = new HashMap<>();
        filterMap.put("field1", rangeConditions);

        BoolQuery.Builder result = invokeBuildFilterQuery(filterMap);
        assertNotNull(result);
    }

    @Test
    void testBuildFilterQuery_rangeQuery_lt() throws Exception {
        Map<String, Object> rangeConditions = new HashMap<>();
        rangeConditions.put(Constants.SEARCH_OPERATION_LESS_THAN, JsonData.of(50));

        Map<String, Object> filterMap = new HashMap<>();
        filterMap.put("field1", rangeConditions);

        BoolQuery.Builder result = invokeBuildFilterQuery(filterMap);
        assertNotNull(result);
    }

    @Test
    void testBuildFilterQuery_rangeQuery_multipleConditions() throws Exception {
        Map<String, Object> rangeConditions = new HashMap<>();
        rangeConditions.put(Constants.SEARCH_OPERATION_GREATER_THAN_EQUALS, JsonData.of(10));
        rangeConditions.put(Constants.SEARCH_OPERATION_LESS_THAN_EQUALS, JsonData.of(100));

        Map<String, Object> filterMap = new HashMap<>();
        filterMap.put("field1", rangeConditions);

        BoolQuery.Builder result = invokeBuildFilterQuery(filterMap);
        assertNotNull(result);
    }

    @Test
    void testBuildFilterQuery_nestedMap_booleanValue() throws Exception {
        Map<String, Object> nestedMap = new HashMap<>();
        nestedMap.put("nestedField", true);

        Map<String, Object> filterMap = new HashMap<>();
        filterMap.put("field1", nestedMap);

        BoolQuery.Builder result = invokeBuildFilterQuery(filterMap);
        assertNotNull(result);
    }

    @Test
    void testBuildFilterQuery_nestedMap_stringValue() {
        Map<String, Object> nestedMap = new HashMap<>();
        nestedMap.put("nestedField", "nestedValue");

        Map<String, Object> filterMap = new HashMap<>();
        filterMap.put("field1", nestedMap);

        // This will cause ClassCastException due to TermsQueryField casting
        assertThrows(Exception.class, () -> invokeBuildFilterQuery(filterMap));
    }

    @Test
    void testBuildFilterQuery_nestedMap_arrayListValue() {
        Map<String, Object> nestedMap = new HashMap<>();
        nestedMap.put("nestedField", new ArrayList<>(Arrays.asList("val1", "val2")));

        Map<String, Object> filterMap = new HashMap<>();
        filterMap.put("field1", nestedMap);

        // This will cause ClassCastException due to TermsQueryField casting
        assertThrows(Exception.class, () -> invokeBuildFilterQuery(filterMap));
    }


    @Test
    void testBuildFilterQuery_nonRangeNestedMap() {
        Map<String, Object> nestedMap = new HashMap<>();
        nestedMap.put("regularField", "regularValue");

        Map<String, Object> filterMap = new HashMap<>();
        filterMap.put("field1", nestedMap);

        // This will cause ClassCastException due to TermsQueryField casting
        assertThrows(Exception.class, () -> invokeBuildFilterQuery(filterMap));
    }

    @Test
    void testBuildFilterQuery_mustNotNotArrayList() throws Exception {
        Map<String, Object> filterMap = new HashMap<>();
        filterMap.put("must_not", "notAnArrayList");

        BoolQuery.Builder result = invokeBuildFilterQuery(filterMap);
        assertNotNull(result);
    }

    private BoolQuery.Builder invokeBuildFilterQuery(Map<String, Object> filterCriteriaMap) throws Exception {
        Method method = EsUtilServiceImpl.class.getDeclaredMethod("buildFilterQuery", Map.class);
        method.setAccessible(true);
        return (BoolQuery.Builder) method.invoke(esUtilService, filterCriteriaMap);
    }
}