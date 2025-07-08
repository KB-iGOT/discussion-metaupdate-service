package com.igot.cb.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.igot.cb.managepostcount.service.impl.ManagePostCountServiceImpl;
import com.igot.cb.pores.cache.CacheService;
import com.igot.cb.pores.elasticsearch.service.EsUtilService;
import com.igot.cb.pores.entity.CommunityEntity;
import com.igot.cb.pores.repository.CommunityEngagementRepository;
import com.igot.cb.pores.util.CbServerProperties;
import com.igot.cb.pores.util.Constants;
import com.igot.cb.transactional.cassandrautils.CassandraOperation;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommunityMetaUpdateConsumerTest {

    @InjectMocks
    private CommunityMetaUpdateConsumer consumer;

    @Mock
    private CommunityEngagementRepository communityEngagementRepository;

    @Mock
    private EsUtilService esUtilService;

    @Mock
    private CbServerProperties cbServerProperties;

    @Mock
    private CacheService cacheService;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private CassandraOperation cassandraOperation;

    @Mock
    ConsumerRecord<String, String> consumerRecord;

    @Mock
    ManagePostCountServiceImpl managePostCountServiceImpl;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(consumer, "communityIndex", "community_index");
    }

    @Test
    void testUpateUserCount_and_updateJoinedUserCount_success() throws Exception {
        // Arrange
        String json = "{\"community\": \"COMMUNITY\", \"userId\": \"USER\"}";
        ConsumerRecord<String, String> consumerRecord = new ConsumerRecord<>("topic", 0, 0L, "key", json);

        Map<String, Object> userCountMap = new HashMap<>();
        userCountMap.put(Constants.COMMUNITY, "COMMUNITY");
        userCountMap.put(Constants.USER_ID, "USER");

        CommunityEntity entity = new CommunityEntity();
        entity.setCommunityId("COMMUNITY");

        ObjectNode dataNode = mock(ObjectNode.class);
        entity.setData(dataNode);

        // Mock the JsonNode returned from dataNode.get()
        var mockedCountNode = mock(com.fasterxml.jackson.databind.node.NumericNode.class);
        when(dataNode.get(Constants.COUNT_OF_PEOPLE_JOINED)).thenReturn(mockedCountNode);
        when(mockedCountNode.asLong()).thenReturn(1L);

        // Mock ObjectNode.put() to return itself
        when(dataNode.put(anyString(), anyLong())).thenReturn(dataNode);
        when(dataNode.put(anyString(), anyString())).thenReturn(dataNode);

        when(objectMapper.convertValue(eq("COMMUNITY"), eq(CommunityEntity.class))).thenReturn(entity);
        when(objectMapper.convertValue(eq(dataNode), eq(Map.class))).thenReturn(new HashMap<>());

        // Act
        consumer.upateUserCount(consumerRecord);

        // Let async task complete
        Thread.sleep(500);

        // Verify internal calls
        verify(cassandraOperation).insertRecord(anyString(), anyString(), anyMap());
        verify(communityEngagementRepository).save(entity);
        verify(cacheService).putCache(anyString(), any());
        verify(cacheService).upsertUserToHash(anyString(), anyString(), anyString());
    }

    @Test
    void testUpateUserCount_whenInvalidJson_thenException() {
        // Arrange
        String invalidJson = "{\"bad_json\": }";
        ConsumerRecord<String, String> consumerRecord = new ConsumerRecord<>("topic", 0, 0L, "key", invalidJson);

        // Act
        consumer.upateUserCount(consumerRecord);
    }

    @Test
    void testUpdateJoinedUserCount_whenDependencyThrows_thenLogsError() {
        // Arrange
        Map<String, Object> userCountMap = new HashMap<>();
        userCountMap.put(Constants.COMMUNITY, "COMMUNITY");
        userCountMap.put(Constants.USER_ID, "USER");

        when(objectMapper.convertValue(eq("COMMUNITY"), eq(CommunityEntity.class)))
                .thenThrow(new RuntimeException("Failed conversion"));

        // Act
        consumer.updateJoinedUserCount(userCountMap);
    }

    @Test
    void testUpatePostCount_withTypeAnswerPost() throws Exception {
        String json = "{\"communityId\":\"COMM2\",\"type\":\"ANSWER_POST\",\"status\":\"INCREMENT\"}";
        when(consumerRecord.value()).thenReturn(json);

        Map<String, Object> updateMap = new HashMap<>();
        updateMap.put(Constants.COMMUNITY_ID, "COMM2");
        updateMap.put(Constants.TYPE, Constants.ANSWER_POST);
        updateMap.put(Constants.STATUS, Constants.INCREMENT);

        CommunityEntity entity = new CommunityEntity();
        entity.setCommunityId("COMM2");

        ObjectNode dataNode = mock(ObjectNode.class);
        entity.setData(dataNode);

        when(communityEngagementRepository.findByCommunityIdAndIsActive("COMM2", true))
                .thenReturn(Optional.of(entity));

        when(objectMapper.convertValue(dataNode, Map.class)).thenReturn(new HashMap<>());
        when(cbServerProperties.getElasticCommunityJsonPath()).thenReturn("jsonPath");

        consumer.upatePostCount(consumerRecord);
        Thread.sleep(300);

        verify(communityEngagementRepository).save(entity);
        }

    @Test
    void testUpatePostCount_withException() throws Exception {
        String badJson = "{bad json}";
        when(consumerRecord.value()).thenReturn(badJson);

        consumer.upatePostCount(consumerRecord);
        Thread.sleep(300);

    }

    @Test
    void testUpateLikeCount_increment() throws Exception {
        String json = "{\"communityId\":\"COMM1\",\"status\":\"INCREMENT\"}";
        when(consumerRecord.value()).thenReturn(json);

        Map<String, Object> updateMap = new HashMap<>();
        updateMap.put(Constants.COMMUNITY_ID, "COMM1");
        updateMap.put(Constants.STATUS, Constants.INCREMENT);

        CommunityEntity entity = new CommunityEntity();
        entity.setCommunityId("COMM1");

        ObjectNode dataNode = mock(ObjectNode.class);
        when(dataNode.has(Constants.COUNT_OF_PEOPLE_LIKED)).thenReturn(true);
        entity.setData(dataNode);

        when(communityEngagementRepository.findByCommunityIdAndIsActive("COMM1", true))
                .thenReturn(Optional.of(entity));

        consumer.upateLikeCount(consumerRecord);

        Thread.sleep(300);
    }

    @Test
    void testUpateLikeCount_decrement() throws Exception {
        String json = "{\"communityId\":\"COMM2\",\"status\":\"DECREMENT\"}";
        when(consumerRecord.value()).thenReturn(json);

        Map<String, Object> updateMap = new HashMap<>();
        updateMap.put(Constants.COMMUNITY_ID, "COMM2");
        updateMap.put(Constants.STATUS, Constants.DECREMENT);

        CommunityEntity entity = new CommunityEntity();
        entity.setCommunityId("COMM2");

        ObjectNode dataNode = mock(ObjectNode.class);
        when(dataNode.has(Constants.COUNT_OF_PEOPLE_LIKED)).thenReturn(true);
        entity.setData(dataNode);

        when(communityEngagementRepository.findByCommunityIdAndIsActive("COMM2", true))
                .thenReturn(Optional.of(entity));

        consumer.upateLikeCount(consumerRecord);

        Thread.sleep(300);
    }

    @Test
    void testUpateLikeCount_optionalEmpty() throws Exception {
        String json = "{\"communityId\":\"COMM3\",\"status\":\"INCREMENT\"}";
        when(consumerRecord.value()).thenReturn(json);

        Map<String, Object> updateMap = new HashMap<>();
        updateMap.put(Constants.COMMUNITY_ID, "COMM3");
        updateMap.put(Constants.STATUS, Constants.INCREMENT);

        when(communityEngagementRepository.findByCommunityIdAndIsActive("COMM3", true))
                .thenReturn(Optional.empty());

        consumer.upateLikeCount(consumerRecord);

        Thread.sleep(300);

        verify(communityEngagementRepository, never()).save(any());
        verify(esUtilService, never()).updateDocument(any(), any(), any(), any());
        verify(cacheService, never()).putCache(any(), any());
    }

    @Test
    void testUpateLikeCount_jsonException() throws Exception {
        String badJson = "{bad json}";
        when(consumerRecord.value()).thenReturn(badJson);

        consumer.upateLikeCount(consumerRecord);

        Thread.sleep(300);
    }

    @Test
    void test_updateUserPostCount_cacheHit_increment() throws Exception {
        String json = "{\"userId\":\"user1\",\"status\":\"INCREMENT\"}";
        when(consumerRecord.value()).thenReturn(json);

        Map<String, String> map = new HashMap<>();
        map.put(Constants.USERID, "user1");
        map.put(Constants.STATUS, Constants.INCREMENT);

        lenient().when(cacheService.getCacheWithoutPrefix("user:postCount_user1")).thenReturn("5");

        consumer.updateUserPostCount(consumerRecord);
        Thread.sleep(200);
    }

    @Test
    void test_updateUserPostCount_cacheHit_decrement() throws Exception {
        String json = "{\"userId\":\"user1\",\"status\":\"DECREMENT\"}";
        when(consumerRecord.value()).thenReturn(json);

        Map<String, String> map = new HashMap<>();
        map.put(Constants.USERID, "user1");
        map.put(Constants.STATUS, Constants.DECREMENT);
        when(objectMapper.readValue(json, Map.class)).thenReturn(map);

        when(cacheService.getCacheWithoutPrefix("user:postCount_user1")).thenReturn("5");

        consumer.updateUserPostCount(consumerRecord);
        Thread.sleep(200);
    }

    @Test
    void test_updateUserPostCount_cacheMiss_elasticSearchSuccess() throws Exception {
        String json = "{\"userId\":\"user2\",\"status\":\"INCREMENT\"}";
        when(consumerRecord.value()).thenReturn(json);

        Map<String, String> map = new HashMap<>();
        map.put(Constants.USERID, "user2");
        map.put(Constants.STATUS, Constants.INCREMENT);
        when(objectMapper.readValue(json, Map.class)).thenReturn(map);

        lenient().when(cacheService.getCacheWithoutPrefix("user:postCount_user2")).thenReturn(null);
        when(managePostCountServiceImpl.fetchPostCountFromElasticsearch("user2")).thenReturn(10L);

        consumer.updateUserPostCount(consumerRecord);
        Thread.sleep(200);
    }

    @Test
    void test_updateUserPostCount_cacheMiss_elasticSearchException() throws Exception {
        String json = "{\"userId\":\"user3\",\"status\":\"INCREMENT\"}";
        when(consumerRecord.value()).thenReturn(json);

        Map<String, String> map = new HashMap<>();
        map.put(Constants.USERID, "user3");
        map.put(Constants.STATUS, Constants.INCREMENT);

        lenient().when(cacheService.getCacheWithoutPrefix("user:postCount_user3")).thenReturn(null);

        consumer.updateUserPostCount(consumerRecord);
    }

    @Test
    void test_updateUserPostCount_outerException() throws Exception {
        String json = "{badJson}";
        when(consumerRecord.value()).thenReturn(json);

        lenient().when(objectMapper.readValue(json, Map.class)).thenThrow(new RuntimeException("bad json"));

        consumer.updateUserPostCount(consumerRecord);
        Thread.sleep(100);
    }
}
