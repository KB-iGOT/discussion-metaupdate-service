package com.igot.cb.pores.elasticsearch.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQueryField;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import co.elastic.clients.elasticsearch.core.search.TotalHits;
import co.elastic.clients.elasticsearch.core.search.TotalHitsRelation;
import co.elastic.clients.elasticsearch.indices.GetIndexRequest;
import co.elastic.clients.elasticsearch.indices.GetIndexResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.igot.cb.pores.elasticsearch.dto.SearchCriteria;
import com.igot.cb.pores.elasticsearch.dto.SearchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EsUtilServiceImplMethodTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ElasticsearchClient elasticsearchClient;

    @Mock
    private GetIndexResponse getIndexResponse;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private EsUtilServiceImpl esUtilService;

    private SearchCriteria searchCriteria;

    @BeforeEach
    void setUp() {
        searchCriteria = new SearchCriteria();
        searchCriteria.setPageNumber(0);
        searchCriteria.setPageSize(10);
        searchCriteria.setRequestedFields(List.of("field1", "field2"));
        HashMap<String, Object> filterCriteriaMap = new HashMap<>();
        searchCriteria.setFilterCriteriaMap(filterCriteriaMap);
    }

    @Test
    void testSearchDocuments_success() throws IOException {
        String index = "test_index";

        // Mock hit
        Hit<Object> hit1 = new Hit.Builder<>().id("doc1").index("index").source(Map.of("field", "value")).build();
        List<Hit<Object>> hitList = Arrays.asList(hit1);

        // Mock total hits
        TotalHits totalHits = new TotalHits.Builder().value(1L).relation(TotalHitsRelation.Eq).build();

        // Mock hits metadata
        HitsMetadata<Object> mockHitsMetadata = Mockito.mock(HitsMetadata.class);
        when(mockHitsMetadata.total()).thenReturn(totalHits);
        when(mockHitsMetadata.hits()).thenReturn(hitList);

        // Mock response
        SearchResponse<Object> mockResponse = Mockito.mock(SearchResponse.class);
        when(mockResponse.hits()).thenReturn(mockHitsMetadata);

        // Mock client
        when(elasticsearchClient.search(any(SearchRequest.class), eq(Object.class))).thenReturn(mockResponse);

        // Build searchCriteria
        searchCriteria.setRequestedFields(List.of("field"));
        searchCriteria.setFilterCriteriaMap(new HashMap<>());

        SearchResult result = esUtilService.searchDocuments(index, searchCriteria);

        assertNotNull(result);
        assertEquals(1, result.getTotalCount());
        assertFalse(result.getData().isEmpty());
    }

    @Test
    void testAddRequestedFieldsToSearchSourceBuilder_emptyFields() throws Exception {
        SearchRequest.Builder builder = new SearchRequest.Builder();
        searchCriteria.setRequestedFields(new ArrayList<>());

        Method method = EsUtilServiceImpl.class.getDeclaredMethod("addRequestedFieldsToSearchSourceBuilder", SearchCriteria.class, SearchRequest.Builder.class);
        method.setAccessible(true);
        method.invoke(esUtilService, searchCriteria, builder);
    }

    @Test
    void testAddFacetsToSearchSourceBuilder() throws Exception {
        SearchRequest.Builder builder = new SearchRequest.Builder();
        List<String> facets = List.of("communityId");

        Method method = EsUtilServiceImpl.class.getDeclaredMethod("addFacetsToSearchSourceBuilder", List.class, SearchRequest.Builder.class);
        method.setAccessible(true);
        method.invoke(esUtilService, facets, builder);
    }

    @Test
    void testIsIndexPresent_IndexExists() throws Exception {
        when(elasticsearchClient.indices().get(any(GetIndexRequest.class)))
                .thenReturn(getIndexResponse);

        boolean result = esUtilService.isIndexPresent("test-index");

        assertTrue(result);
        verify(elasticsearchClient.indices()).get(any(GetIndexRequest.class));
    }

    @Test
    void testIsIndexPresent_IndexThrowsIOException() throws Exception {
        when(elasticsearchClient.indices().get(any(GetIndexRequest.class)))
                .thenThrow(new IOException("Simulated failure"));

        boolean result = esUtilService.isIndexPresent("test-index");

        assertFalse(result);
        verify(elasticsearchClient.indices()).get(any(GetIndexRequest.class));
    }

    @Test
    void testSearchRequestBuild_allFieldsPresent() throws Exception {
        ReflectionTestUtils.setField(esUtilService, "objectMapper", objectMapper);
        // Arrange
        SearchRequest originalRequest = SearchRequest.of(s -> s
                .query(q -> q.matchAll(m -> m))
                .aggregations("agg", a -> a.terms(t -> t.field("field")))
                .source(a -> a.filter(f -> f.includes("field1", "field2")))
                .from(5)
                .size(10)
        );

        String indexName = "test-index";

        // Act
        Method method = EsUtilServiceImpl.class
                .getDeclaredMethod("searchRequestBuild", SearchRequest.class, String.class);
        method.setAccessible(true);

        SearchRequest.Builder builder =
                (SearchRequest.Builder) method.invoke(esUtilService, originalRequest, indexName);

        SearchRequest resultRequest = builder.build();

        // Assert
        assertEquals(indexName, resultRequest.index().get(0));
        assertNotNull(resultRequest.query());
        assertNotNull(resultRequest.aggregations());
        assertNotNull(resultRequest.source());
        assertEquals(5, resultRequest.from());
        assertEquals(10, resultRequest.size());
    }

    @Test
    void testSearchRequestBuild_someFieldsNull() throws Exception {
        ReflectionTestUtils.setField(esUtilService, "objectMapper", objectMapper);
        // Arrange
        SearchRequest originalRequest = SearchRequest.of(s -> s
                        .query(q -> q.matchAll(m -> m))
                // no aggs
                // no source
                // no from
                // no size
        );

        String indexName = "test-index";

        // Act
        Method method = EsUtilServiceImpl.class
                .getDeclaredMethod("searchRequestBuild", SearchRequest.class, String.class);
        method.setAccessible(true);

        SearchRequest.Builder builder =
                (SearchRequest.Builder) method.invoke(esUtilService, originalRequest, indexName);

        SearchRequest resultRequest = builder.build();

        // Assert
        assertEquals(indexName, resultRequest.index().get(0));
        assertNotNull(resultRequest.query());
        assertTrue(resultRequest.aggregations().isEmpty());
        assertNull(resultRequest.source());
        assertNull(resultRequest.from());
        assertNull(resultRequest.size());
    }

    @Test
    void testBuildRangeQuery_allValidOperators() throws Exception {
        // Arrange
        Map<String, Object> rangeMap = new HashMap<>();

        Map<String, Object> field1Conditions = new HashMap<>();
        field1Conditions.put("gt", 10);
        field1Conditions.put("gte", 15);
        field1Conditions.put("lt", 50);
        field1Conditions.put("lte", 60);
        rangeMap.put("field1", field1Conditions);

        // Act
        Method method = EsUtilServiceImpl.class.getDeclaredMethod("buildRangeQuery", Map.class);
        method.setAccessible(true);

        Query query = (Query) method.invoke(esUtilService, rangeMap);

        // Assert
        assertNotNull(query);
        assertTrue(query.isBool());
        assertFalse(query.bool().must().isEmpty());

        // Check that it contains at least one range query for our field
        boolean hasRangeQuery = query.bool().must().stream()
                .anyMatch(q -> q.isRange() && q.range().field().equals("field1"));
        assertTrue(hasRangeQuery);
    }

    @Test
    void testBuildRangeQuery_withUnsupportedOperator() throws Exception {
        // Arrange
        Map<String, Object> rangeMap = new HashMap<>();

        Map<String, Object> field1Conditions = new HashMap<>();
        field1Conditions.put("invalidOperator", 10);
        rangeMap.put("field1", field1Conditions);

        // Act & Assert
        Method method = EsUtilServiceImpl.class.getDeclaredMethod("buildRangeQuery", Map.class);
        method.setAccessible(true);

        Exception exception = assertThrows(Exception.class, () -> {
            method.invoke(esUtilService, rangeMap);
        });

        Throwable cause = exception.getCause();
        assertTrue(cause instanceof IllegalArgumentException);
    }

    @Test
    void testBuildTermsQuery_nonEmptyMap() throws Exception {
        Map<String, Object> termsMap = new HashMap<>();

        // Example TermsQueryField
        TermsQueryField termsQueryField = TermsQueryField.of(t -> t.value(List.of(FieldValue.of("val1"), FieldValue.of("val2"))));
        termsMap.put("field1", termsQueryField);

        Method method = EsUtilServiceImpl.class.getDeclaredMethod("buildTermsQuery", Map.class);
        method.setAccessible(true);

        Query query = (Query) method.invoke(esUtilService, termsMap);

        assertNotNull(query);
        assertTrue(query.isBool());
        assertFalse(query.bool().must().isEmpty());

        boolean hasTermsQuery = query.bool().must().stream()
                .anyMatch(q -> q.isTerms() && q.terms().field().equals("field1"));
        assertTrue(hasTermsQuery);
    }

    @Test
    void testBuildTermsQuery_emptyMap() throws Exception {
        Map<String, Object> termsMap = new HashMap<>();

        Method method = EsUtilServiceImpl.class.getDeclaredMethod("buildTermsQuery", Map.class);
        method.setAccessible(true);

        Query query = (Query) method.invoke(esUtilService, termsMap);

        assertNotNull(query);
        assertTrue(query.isBool());
        assertTrue(query.bool().must().isEmpty());
    }

    @Test
    void testBuildMatchQuery_nonEmptyMap() throws Exception {
        Map<String, Object> matchMap = new HashMap<>();

        FieldValue fieldValue = FieldValue.of("value1");
        matchMap.put("field2", fieldValue);

        Method method = EsUtilServiceImpl.class.getDeclaredMethod("buildMatchQuery", Map.class);
        method.setAccessible(true);

        Query query = (Query) method.invoke(esUtilService, matchMap);

        assertNotNull(query);
        assertTrue(query.isBool());
        assertFalse(query.bool().must().isEmpty());

        boolean hasMatchQuery = query.bool().must().stream()
                .anyMatch(q -> q.isMatch() && q.match().field().equals("field2"));
        assertTrue(hasMatchQuery);
    }

    @Test
    void testBuildMatchQuery_emptyMap() throws Exception {
        Map<String, Object> matchMap = new HashMap<>();

        Method method = EsUtilServiceImpl.class.getDeclaredMethod("buildMatchQuery", Map.class);
        method.setAccessible(true);

        Query query = (Query) method.invoke(esUtilService, matchMap);

        assertNotNull(query);
        assertTrue(query.isBool());
        assertTrue(query.bool().must().isEmpty());
    }
}