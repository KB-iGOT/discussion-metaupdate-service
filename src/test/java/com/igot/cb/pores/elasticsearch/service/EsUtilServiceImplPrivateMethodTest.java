package com.igot.cb.pores.elasticsearch.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.Buckets;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQueryField;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.json.JsonData;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.igot.cb.pores.elasticsearch.dto.FacetDTO;
import com.igot.cb.pores.elasticsearch.dto.SearchCriteria;
import com.igot.cb.pores.util.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.util.*;

class EsUtilServiceImplPrivateMethodTest {

    private EsUtilServiceImpl esUtilService;

    @BeforeEach
    void setUp() {
        esUtilService = new EsUtilServiceImpl(mock(ElasticsearchClient.class));
        ReflectionTestUtils.setField(esUtilService, "objectMapper", new ObjectMapper());
    }

    @Test
    void testBuildQueryPart_NullOrEmptyMap() throws Exception {
        Method method = EsUtilServiceImpl.class.getDeclaredMethod("buildQueryPart", Map.class);
        method.setAccessible(true);

        Query resultNull = (Query) method.invoke(esUtilService, (Object) null);
        assertNotNull(resultNull);

        Query resultEmpty = (Query) method.invoke(esUtilService, new HashMap<>());
        assertNotNull(resultEmpty);
    }

    @Test
    void testBuildQueryPart_Term() throws Exception {
        Method method = EsUtilServiceImpl.class.getDeclaredMethod("buildQueryPart", Map.class);
        method.setAccessible(true);

        Map<String, Object> termMap = Map.of(Constants.TERM, Map.of("field", FieldValue.of("value")));
        Query result = (Query) method.invoke(esUtilService, termMap);
        assertNotNull(result);
    }

    @Test
    void testBuildQueryPart_Terms() throws Exception {
        Method method = EsUtilServiceImpl.class.getDeclaredMethod("buildQueryPart", Map.class);
        method.setAccessible(true);

        TermsQueryField termsField = TermsQueryField.of(t -> t.value(List.of(FieldValue.of("v1"), FieldValue.of("v2"))));
        Map<String, Object> termsMap = Map.of(Constants.TERMS, Map.of("field", termsField));
        Query result = (Query) method.invoke(esUtilService, termsMap);
        assertNotNull(result);
    }

    @Test
    void testBuildQueryPart_Match() throws Exception {
        Method method = EsUtilServiceImpl.class.getDeclaredMethod("buildQueryPart", Map.class);
        method.setAccessible(true);

        Map<String, Object> matchMap = Map.of(Constants.MATCH, Map.of("field", FieldValue.of("value")));
        Query result = (Query) method.invoke(esUtilService, matchMap);
        assertNotNull(result);
    }

    @Test
    void testBuildQueryPart_Range() throws Exception {
        Method method = EsUtilServiceImpl.class.getDeclaredMethod("buildQueryPart", Map.class);
        method.setAccessible(true);

        Map<String, Object> rangeConditions = Map.of("gte", JsonData.of(1), "lte", JsonData.of(10));
        Map<String, Object> rangeMap = Map.of(Constants.RANGE, Map.of("field", rangeConditions));
        Query result = (Query) method.invoke(esUtilService, rangeMap);
        assertNotNull(result);
    }

    @Test
    void testBuildQueryPart_Bool() throws Exception {
        Method method = EsUtilServiceImpl.class.getDeclaredMethod("buildQueryPart", Map.class);
        method.setAccessible(true);

        Map<String, Object> mustClause = Map.of(Constants.MATCH, Map.of("field1", FieldValue.of("value1")));
        Map<String, Object> boolMap = Map.of("must", List.of(mustClause));
        Map<String, Object> queryMap = Map.of(Constants.BOOL, boolMap);

        Query result = (Query) method.invoke(esUtilService, queryMap);
        assertNotNull(result);
    }

    @Test
    void testBuildBoolQuery() throws Exception {
        Method method = EsUtilServiceImpl.class.getDeclaredMethod("buildBoolQuery", Map.class);
        method.setAccessible(true);

        Map<String, Object> mustClause = Map.of(Constants.MATCH, Map.of("field1", FieldValue.of("value1")));
        Map<String, Object> boolMap = Map.of("must", List.of(mustClause));

        BoolQuery result = (BoolQuery) method.invoke(esUtilService, boolMap);
        assertNotNull(result);
    }

    @Test
    void testBuildBoolQuery_filter() throws Exception {
        Method method = EsUtilServiceImpl.class.getDeclaredMethod("buildBoolQuery", Map.class);
        method.setAccessible(true);

        Map<String, Object> filter = Map.of(Constants.MATCH, Map.of("field1", FieldValue.of("value1")));
        Map<String, Object> boolMap = Map.of(Constants.FILTER, List.of(filter));

        BoolQuery result = (BoolQuery) method.invoke(esUtilService, boolMap);
        assertNotNull(result);
    }

    @Test
    void testBuildBoolQuery_should() throws Exception {
        Method method = EsUtilServiceImpl.class.getDeclaredMethod("buildBoolQuery", Map.class);
        method.setAccessible(true);

        Map<String, Object> filter = Map.of(Constants.MATCH, Map.of("field1", FieldValue.of("value1")));
        Map<String, Object> boolMap = Map.of(Constants.SHOULD, List.of(filter));

        BoolQuery result = (BoolQuery) method.invoke(esUtilService, boolMap);
        assertNotNull(result);
    }

    @Test
    void testBuildBoolQuery_must_not() throws Exception {
        Method method = EsUtilServiceImpl.class.getDeclaredMethod("buildBoolQuery", Map.class);
        method.setAccessible(true);

        Map<String, Object> filter = Map.of(Constants.MATCH, Map.of("field1", FieldValue.of("value1")));
        Map<String, Object> boolMap = Map.of(Constants.MUST_NOT, List.of(filter));

        BoolQuery result = (BoolQuery) method.invoke(esUtilService, boolMap);
        assertNotNull(result);
    }

    @Test
    void testExtractFacetData_nullFacets() throws Exception {
        SearchCriteria criteria = mock(SearchCriteria.class);
        when(criteria.getFacets()).thenReturn(null);

        SearchResponse<Object> response = mock(SearchResponse.class);

        Map<String, List<FacetDTO>> result = invokeExtractFacetData(response, criteria);
        assertTrue(result.isEmpty());
    }

    @Test
    void testExtractFacetData_emptyFacets() throws Exception {
        SearchCriteria criteria = mock(SearchCriteria.class);
        when(criteria.getFacets()).thenReturn(Collections.emptyList());

        SearchResponse<Object> response = mock(SearchResponse.class);

        Map<String, List<FacetDTO>> result = invokeExtractFacetData(response, criteria);
        assertTrue(result.isEmpty());
    }

    @Test
    void testExtractFacetData_withValidBuckets() throws Exception {
        SearchCriteria criteria = mock(SearchCriteria.class);
        when(criteria.getFacets()).thenReturn(List.of("field1"));

        StringTermsBucket bucket1 = mock(StringTermsBucket.class);
        when(bucket1.key()).thenReturn(FieldValue.of("val1"));
        when(bucket1.docCount()).thenReturn(5L);

        StringTermsBucket bucket2 = mock(StringTermsBucket.class);
        when(bucket2.key()).thenReturn(FieldValue.of(""));
        when(bucket2.docCount()).thenReturn(2L);

        co.elastic.clients.elasticsearch._types.aggregations.StringTermsAggregate sterms = mock(co.elastic.clients.elasticsearch._types.aggregations.StringTermsAggregate.class);
        when(sterms.buckets()).thenReturn(Buckets.of(b -> b.array(Arrays.asList(bucket1, bucket2))));

        Aggregate aggregate = mock(Aggregate.class);
        when(aggregate.isSterms()).thenReturn(true);
        when(aggregate.sterms()).thenReturn(sterms);

        Map<String, Aggregate> aggMap = new HashMap<>();
        aggMap.put("field1_agg", aggregate);

        SearchResponse<Object> response = mock(SearchResponse.class);
        when(response.aggregations()).thenReturn(aggMap);

        Map<String, List<FacetDTO>> result = invokeExtractFacetData(response, criteria);

        assertEquals(1, result.size());
        assertEquals(1, result.get("field1").size());
        assertEquals(5L, result.get("field1").get(0).getCount());
    }

    @Test
    void testExtractFacetData_isStermsFalse() throws Exception {
        SearchCriteria criteria = mock(SearchCriteria.class);
        when(criteria.getFacets()).thenReturn(List.of("field1"));

        Aggregate aggregate = mock(Aggregate.class);
        when(aggregate.isSterms()).thenReturn(false);

        Map<String, Aggregate> aggMap = new HashMap<>();
        aggMap.put("field1_agg", aggregate);

        SearchResponse<Object> response = mock(SearchResponse.class);
        when(response.aggregations()).thenReturn(aggMap);

        Map<String, List<FacetDTO>> result = invokeExtractFacetData(response, criteria);
        assertTrue(result.isEmpty());
    }

    @Test
    void testAddQueryStringToFilter_withValidString() throws Exception {
        // Arrange
        String searchString = "TestSearch";
        BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();

        // Act - use reflection to call private method
        Method method = EsUtilServiceImpl.class.getDeclaredMethod(
                "addQueryStringToFilter", String.class, BoolQuery.Builder.class);
        method.setAccessible(true);
        method.invoke(esUtilService, searchString, boolQueryBuilder);

        assertNotNull(boolQueryBuilder.build().must());
    }

    @Test
    void testAddQueryStringToFilter_withBlankString() throws Exception {
        // Arrange
        String searchString = "   "; // blank
        BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();

        // Act
        Method method = EsUtilServiceImpl.class.getDeclaredMethod(
                "addQueryStringToFilter", String.class, BoolQuery.Builder.class);
        method.setAccessible(true);
        method.invoke(esUtilService, searchString, boolQueryBuilder);

        assertTrue(boolQueryBuilder.build().must().isEmpty());
    }

    private Map<String, List<FacetDTO>> invokeExtractFacetData(SearchResponse<Object> response, SearchCriteria criteria) throws Exception {
        Method method = EsUtilServiceImpl.class.getDeclaredMethod("extractFacetData", SearchResponse.class, SearchCriteria.class);
        method.setAccessible(true);
        return (Map<String, List<FacetDTO>>) method.invoke(esUtilService, response, criteria);
    }
}