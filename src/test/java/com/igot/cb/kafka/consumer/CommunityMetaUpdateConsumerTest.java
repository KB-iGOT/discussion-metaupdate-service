package com.igot.cb.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.igot.cb.pores.cache.CacheService;
import com.igot.cb.pores.elasticsearch.service.EsUtilService;
import com.igot.cb.pores.entity.CommunityEntity;
import com.igot.cb.pores.repository.CommunityEngagementRepository;
import com.igot.cb.pores.util.Constants;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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
    private CacheService cacheService;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    ConsumerRecord<String, String> consumerRecord;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(consumer, "communityIndex", "community_index");
    }

    @Test
    void testUpateUserCount_and_updateJoinedUserCount_success() {
        // Arrange
        String json = "{\"community\": \"COMMUNITY\", \"userId\": \"USER\"}";
        consumerRecord = new ConsumerRecord<>("topic", 0, 0L, "key", json);

        CommunityEntity entity = new CommunityEntity();
        entity.setCommunityId("COMMUNITY");

        ObjectNode dataNode = mock(ObjectNode.class);
        entity.setData(dataNode);
        // Act
        assertDoesNotThrow(()-> consumer.upateUserCount(consumerRecord));

    }

    @Test
    void testUpateUserCount_whenInvalidJson_thenException() {
        // Arrange
        String invalidJson = "{\"bad_json\": }";
        consumerRecord = new ConsumerRecord<>("topic", 0, 0L, "key", invalidJson);

        // Act
        assertDoesNotThrow(()-> consumer.upateUserCount(consumerRecord));
    }

    @Test
    void testUpdateJoinedUserCount_whenDependencyThrows_thenLogsError() {
        // Arrange
        Map<String, Object> userCountMap = new HashMap<>();
        userCountMap.put(Constants.COMMUNITY, "COMMUNITY");
        userCountMap.put(Constants.USER_ID, "USER");

        when(objectMapper.convertValue("COMMUNITY", CommunityEntity.class))
                .thenThrow(new RuntimeException("Failed conversion"));

        // Act
        assertDoesNotThrow(()-> consumer.updateJoinedUserCount(userCountMap));
    }

    @Test
    void testUpatePostCount_withTypeAnswerPost() {
        String json = "{\"communityId\":\"COMM2\",\"type\":\"ANSWER_POST\",\"status\":\"INCREMENT\"}";
        when(consumerRecord.value()).thenReturn(json);

        CommunityEntity entity = new CommunityEntity();
        entity.setCommunityId("COMM2");

        ObjectNode dataNode = mock(ObjectNode.class);
        entity.setData(dataNode);

        lenient().when(communityEngagementRepository.findByCommunityIdAndIsActive("COMM2", true))
                .thenReturn(Optional.of(entity));

        lenient().when(objectMapper.convertValue(dataNode, Map.class)).thenReturn(new HashMap<>());

        assertDoesNotThrow(()-> consumer.upatePostCount(consumerRecord));
     }

    @Test
    void testUpatePostCount_withException() {
        String badJson = "{bad json}";
        when(consumerRecord.value()).thenReturn(badJson);

        assertDoesNotThrow(()-> consumer.upatePostCount(consumerRecord));
    }

    @Test
    void testUpateLikeCount_increment() {
        String json = "{\"communityId\":\"COMM1\",\"status\":\"INCREMENT\"}";
        when(consumerRecord.value()).thenReturn(json);

        CommunityEntity entity = new CommunityEntity();
        entity.setCommunityId("COMM1");

        ObjectNode dataNode = mock(ObjectNode.class);
        entity.setData(dataNode);

        assertDoesNotThrow(()-> consumer.upateLikeCount(consumerRecord));
    }

    @Test
    void testUpateLikeCount_decrement() {
        String json = "{\"communityId\":\"COMM2\",\"status\":\"DECREMENT\"}";
        when(consumerRecord.value()).thenReturn(json);

        CommunityEntity entity = new CommunityEntity();
        entity.setCommunityId("COMM2");

        assertDoesNotThrow(()-> consumer.upateLikeCount(consumerRecord));
    }

    @Test
    void testUpateLikeCount_optionalEmpty() {
        String json = "{\"communityId\":\"COMM3\",\"status\":\"INCREMENT\"}";
        when(consumerRecord.value()).thenReturn(json);

        lenient().when(communityEngagementRepository.findByCommunityIdAndIsActive("COMM3", true))
                .thenReturn(Optional.empty());

        assertDoesNotThrow(()-> consumer.upateLikeCount(consumerRecord));
        verify(communityEngagementRepository, never()).save(any());
        verify(esUtilService, never()).updateDocument(any(), any(), any(), any());
        verify(cacheService, never()).putCache(any(), any());
    }

    @Test
    void testUpateLikeCount_jsonException() {
        String badJson = "{bad json}";
        when(consumerRecord.value()).thenReturn(badJson);

        assertDoesNotThrow(()-> consumer.upateLikeCount(consumerRecord));
    }
    @ParameterizedTest
    @MethodSource("provideUpdateUserPostCountTestCases")
    void test_updateUserPostCount(String userId, String status, String cacheValue) {
        String json = String.format("{\"userId\":\"%s\",\"status\":\"%s\"}", userId, status);
        when(consumerRecord.value()).thenReturn(json);

        lenient().when(cacheService.getCacheWithoutPrefix("user:postCount_" + userId)).thenReturn(cacheValue);

        assertDoesNotThrow(() -> consumer.updateUserPostCount(consumerRecord));
    }

    private static Stream<Arguments> provideUpdateUserPostCountTestCases() {
        return Stream.of(
                Arguments.of("user1", "INCREMENT", "5"),    // cache hit
                Arguments.of("user2", "INCREMENT", null),   // cache miss, ES success
                Arguments.of("user3", "INCREMENT", null)    // cache miss, ES exception
        );
    }



    @Test
    void test_updateUserPostCount_cacheHit_decrement() {
        String json = "{\"userId\":\"user1\",\"status\":\"DECREMENT\"}";
        when(consumerRecord.value()).thenReturn(json);

        assertDoesNotThrow(()-> consumer.updateUserPostCount(consumerRecord));
    }
    @Test
    void test_updateUserPostCount_outerException() throws Exception {
        String json = "{badJson}";
        when(consumerRecord.value()).thenReturn(json);

        lenient().when(objectMapper.readValue(json, Map.class)).thenThrow(new RuntimeException("bad json"));

        assertDoesNotThrow(()-> consumer.updateUserPostCount(consumerRecord));
    }
}
