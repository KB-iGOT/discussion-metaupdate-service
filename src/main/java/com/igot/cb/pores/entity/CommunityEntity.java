package com.igot.cb.pores.entity;

import com.fasterxml.jackson.databind.JsonNode;

import java.sql.Timestamp;
import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "communities")
@JsonIgnoreProperties(ignoreUnknown = true)
@Entity
public class CommunityEntity {
    @Id
    private String communityId;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private JsonNode data;

    private Timestamp createdOn;

    private Timestamp updatedOn;

    private String created_by;

    @Column(name="is_active")
    private boolean isActive;
}
