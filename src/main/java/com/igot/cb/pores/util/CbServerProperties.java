package com.igot.cb.pores.util;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@Getter
@Setter
public class CbServerProperties {

      @Value("${elastic.required.field.community.json.path}")
      private String elasticCommunityJsonPath;

      @Value("${discussion.entity}")
      private String discussionEntity;

      @Value("${search.criteria.user.user.post.count}")
      private String userPostCountSearchCriteria;

      @Value("${kafka.topic.user.post.count}")
      private String kafkaUserPostCountTopic;
}
