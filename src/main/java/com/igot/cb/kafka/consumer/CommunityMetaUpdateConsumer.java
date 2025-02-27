package com.igot.cb.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.igot.cb.pores.cache.CacheService;
import com.igot.cb.pores.elasticsearch.service.EsUtilService;
import com.igot.cb.pores.entity.CommunityEntity;
import com.igot.cb.pores.repository.CommunityEngagementRepository;
import com.igot.cb.pores.util.CbServerProperties;
import com.igot.cb.pores.util.Constants;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
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

    @KafkaListener(groupId = "${kafka.topic.community.discussion.post.count.group}", topics = "${kafka.topic.community.discussion.post.count}")
    public void demandContentConsumer(ConsumerRecord<String, String> data) {
        log.info("Received post updation topic msg");
        try {
            Map<String, Object> updateUserCount = mapper.readValue(data.value(), Map.class);
            updatePostCount(updateUserCount);
        } catch (Exception e) {
            log.error("Failed to update the userCount" + data.value(), e);
        }
    }

    private void updatePostCount(Map<String, Object> updateUserCount) {
        log.info("Received post updation topic msg::inside updatePostCount");
        String communityId = (String) updateUserCount.get(Constants.COMMUNITY_ID);
        Optional<CommunityEntity> communityEntityOptional= communityEngagementRepository.findByCommunityIdAndIsActive(communityId, true);
        if (communityEntityOptional.isPresent()){
            ObjectNode dataNode = (ObjectNode) communityEntityOptional.get().getData();
            long currentCount = 0L;
            if (dataNode.has(Constants.COUNT_OF_POST_CREATED)) {
                currentCount = dataNode.get(Constants.COUNT_OF_POST_CREATED).asLong();
            }
            if (updateUserCount.get(Constants.STATUS).equals(Constants.INCREMENT)){
                dataNode.put(Constants.COUNT_OF_POST_CREATED,currentCount+1);
            } if (updateUserCount.get(Constants.STATUS).equals(Constants.DECREMENT)){
                dataNode.put(Constants.COUNT_OF_POST_CREATED,currentCount-1);
            }
            communityEntityOptional.get().setData(dataNode);
            communityEngagementRepository.save(communityEntityOptional.get());
            Map<String, Object> map = mapper.convertValue(dataNode, Map.class);
            esUtilService.updateDocument(Constants.INDEX_NAME,
                    communityEntityOptional.get().getCommunityId(), map,
                    cbServerProperties.getElasticCommunityJsonPath());
            cacheService.putCache(Constants.REDIS_KEY_PREFIX+"community:"+communityEntityOptional.get().getCommunityId(), communityEntityOptional.get().getData());
            cacheService.deleteCache(Constants.CATEGORY_LIST_ALL_REDIS_KEY_PREFIX);
        }
    }
}
