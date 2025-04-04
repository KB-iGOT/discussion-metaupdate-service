package com.igot.cb.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.igot.cb.pores.cache.CacheService;
import com.igot.cb.pores.elasticsearch.service.EsUtilService;
import com.igot.cb.pores.entity.CommunityEntity;
import com.igot.cb.pores.repository.CommunityEngagementRepository;
import com.igot.cb.pores.util.CbServerProperties;
import com.igot.cb.pores.util.Constants;
import com.igot.cb.transactional.cassandrautils.CassandraOperation;
import java.sql.Timestamp;
import java.util.HashMap;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
public class CommunityMetaUpdateConsumer {
    private ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private CommunityEngagementRepository communityEngagementRepository;

    @Autowired
    private EsUtilService esUtilService;

    @Autowired
    private CbServerProperties cbServerProperties;

    @Autowired
    private CacheService cacheService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    CassandraOperation cassandraOperation;

    @Value("${community.index}")
    private String communityIndex;

    @KafkaListener(groupId = "${kafka.topic.community.user.count.group}", topics = "${kafka.topic.community.user.count}")
    public void upateUserCount(ConsumerRecord<String, String> data) {
        try {
            Map<String, Object> updateUserCount = mapper.readValue(data.value(), Map.class);
            updateJoinedUserCount(updateUserCount);
        } catch (Exception e) {
            log.error("Failed to update the userCount" + data.value(), e);
        }
    }

    @KafkaListener(groupId = "${kafka.topic.community.discusion.post.count.group}", topics = "${kafka.topic.community.discusion.post.count}")
    public void upatePostCount(ConsumerRecord<String, String> data) {
        log.info("Received post updation topic msg");
        try {
            Map<String, Object> updateUserCount = mapper.readValue(data.value(), Map.class);
            updatePostCount(updateUserCount);
        } catch (Exception e) {
            log.error("Failed to update the userCount" + data.value(), e);
        }
    }

    private void updatePostCount(Map<String, Object> updatePostAndAnswerPostCount) {
        try {
            log.info("Received post updation topic msg::inside updatePostCount");
            String communityId = (String) updatePostAndAnswerPostCount.get(Constants.COMMUNITY_ID);
            Optional<CommunityEntity> communityEntityOptional = communityEngagementRepository.findByCommunityIdAndIsActive(
                communityId, true);
            if (communityEntityOptional.isPresent()) {
                ObjectNode dataNode = (ObjectNode) communityEntityOptional.get().getData();
                if (Constants.POST.equalsIgnoreCase(
                    (String) updatePostAndAnswerPostCount.get(Constants.TYPE))) {
                    updateCount(dataNode, updatePostAndAnswerPostCount, Constants.COUNT_OF_POST_CREATED);
                } else if (Constants.ANSWER_POST.equalsIgnoreCase(
                    (String) updatePostAndAnswerPostCount.get(Constants.TYPE))) {
                    updateCount(dataNode, updatePostAndAnswerPostCount, Constants.COUNT_OF_ANSWER_POST_CREATED);
                }
                communityEntityOptional.get().setData(dataNode);
                communityEngagementRepository.save(communityEntityOptional.get());
                Map<String, Object> map = objectMapper.convertValue(dataNode, Map.class);
                esUtilService.updateDocument(communityIndex,
                    communityEntityOptional.get().getCommunityId(), map,
                    cbServerProperties.getElasticCommunityJsonPath());
                cacheService.putCache(communityId, communityEntityOptional.get().getData());
            }
        } catch (Exception e) {
            log.error("Failed to update the post count for community: " + updatePostAndAnswerPostCount, e);
        }
    }

    private void updateCount(ObjectNode dataNode, Map<String, Object> updatePostAndAnswerPostCount,
        String countField) {
        long currentCount = 0L;
        if (dataNode.has(countField)) {
            currentCount = dataNode.get(countField).asLong();
        }
        if (updatePostAndAnswerPostCount.get(Constants.STATUS).equals(Constants.INCREMENT)) {
            dataNode.put(countField, currentCount + 1);
        } else if (updatePostAndAnswerPostCount.get(Constants.STATUS).equals(Constants.DECREMENT)) {
            dataNode.put(countField, currentCount - 1);
        }
    }

    public void updateJoinedUserCount(Map<String, Object> updateUserCount) {
        try {
            log.info("Received user count updation topic msg::inside updateJoinedUserCount");
            Map<String, Object> propertyMap = new HashMap<>();
            CommunityEntity communityEntity = objectMapper.convertValue(updateUserCount.get(Constants.COMMUNITY), CommunityEntity.class);
            String userId = (String) updateUserCount.get(Constants.USER_ID);
            propertyMap.put(Constants.USER_ID, userId);
            propertyMap.put(Constants.CommunityId, communityEntity.getCommunityId());
            propertyMap.put(Constants.STATUS, true);
            cassandraOperation.insertRecord(Constants.KEYSPACE_SUNBIRD, Constants.USER_COMMUNITY_LOOK_UP_TABLE, propertyMap);
            ObjectNode dataNode = (ObjectNode) communityEntity.getData();

            // Perform the update
            long currentCount = dataNode.get(Constants.COUNT_OF_PEOPLE_JOINED).asLong();
            dataNode.put(Constants.COUNT_OF_PEOPLE_JOINED, currentCount + 1);
            Timestamp currentTime = new Timestamp(System.currentTimeMillis());
            communityEntity.setUpdatedOn(currentTime);
            dataNode.put(Constants.UPDATED_ON, String.valueOf(currentTime));
            dataNode.put(Constants.UPDATED_BY, userId);
            dataNode.put(Constants.STATUS, Constants.ACTIVE);
            dataNode.put(Constants.COMMUNITY_ID, communityEntity.getCommunityId());
            communityEngagementRepository.save(communityEntity);
            Map<String, Object> map = objectMapper.convertValue(dataNode, Map.class);
            esUtilService.updateDocument(communityIndex,
                communityEntity.getCommunityId(), map,
                cbServerProperties.getElasticCommunityJsonPath());
            cacheService.putCache(communityEntity.getCommunityId(), communityEntity.getData());
            cacheService.upsertUserToHash(Constants.CMMUNITY_USER_REDIS_PREFIX + communityEntity.getCommunityId(), Constants.USER_PREFIX + userId, Constants.USER_PREFIX + userId);
        } catch (Exception e) {
            log.error("Failed to update the joined user count: "+ updateUserCount, e);
        }
    }

    @KafkaListener(groupId = "${kafka.topic.community.discusion.like.count.group}", topics = "${kafka.topic.community.discusion.like.count}")
    public void upateLikeCount(ConsumerRecord<String, String> data) {
        try {
            Map<String, Object> updateLikeCount = mapper.readValue(data.value(), Map.class);
            updateLikeCount(updateLikeCount);
        } catch (Exception e) {
            log.error("Failed to update the userCount" + data.value(), e);
        }
    }

    private void updateLikeCount(Map<String, Object> updateLikeCount) {
        try {
            log.info("Received like updation topic msg::inside updatePostCount");
            String communityId = (String) updateLikeCount.get(Constants.COMMUNITY_ID);
            Optional<CommunityEntity> communityEntityOptional = communityEngagementRepository.findByCommunityIdAndIsActive(
                communityId, true);
            if (communityEntityOptional.isPresent()) {
                ObjectNode dataNode = (ObjectNode) communityEntityOptional.get().getData();
                Long currentlike = 0L;
                if (dataNode.has(Constants.COUNT_OF_PEOPLE_LIKED)) {
                    currentlike = dataNode.get(Constants.COUNT_OF_PEOPLE_LIKED).asLong();
                }
                dataNode.put(Constants.COUNT_OF_PEOPLE_LIKED, currentlike + 1);
                communityEntityOptional.get().setData(dataNode);
                communityEngagementRepository.save(communityEntityOptional.get());
                Map<String, Object> map = objectMapper.convertValue(dataNode, Map.class);
                esUtilService.updateDocument(communityIndex,
                    communityEntityOptional.get().getCommunityId(), map,
                    cbServerProperties.getElasticCommunityJsonPath());
                cacheService.putCache(communityId, communityEntityOptional.get().getData());
            }
        } catch (Exception e) {
            log.error("Failed to update the like count for community: "+updateLikeCount, e);
        }
    }
}
