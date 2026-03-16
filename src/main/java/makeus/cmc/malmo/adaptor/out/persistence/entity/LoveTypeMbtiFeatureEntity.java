package makeus.cmc.malmo.adaptor.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import makeus.cmc.malmo.adaptor.out.persistence.entity.value.LoveTypeMbtiFeatureEntityId;
import makeus.cmc.malmo.domain.value.type.LoveTypeCategory;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@IdClass(LoveTypeMbtiFeatureEntityId.class)
@Table(name = "love_type_mbti_feature")
public class LoveTypeMbtiFeatureEntity {

    @Id
    @Column(name = "mbti")
    private String mbti;

    @Id
    @Column(name = "lovetype")
    @Enumerated(EnumType.STRING)
    private LoveTypeCategory loveTypeCategory;

    @Column(name = "summary")
    private String summary;

    @Column(name = "keyword1")
    private String keyword1;

    @Column(name = "keyword2")
    private String keyword2;

    @Column(name = "keyword3")
    private String keyword3;

    @Column(name = "strength1")
    private String strength1;

    @Column(name = "strength2")
    private String strength2;

    @Column(name = "strength3")
    private String strength3;

    @Column(name = "weakness")
    private String weakness;

    @Column(name = "strength_desc1")
    private String strengthDesc1;

    @Column(name = "strength_desc2")
    private String strengthDesc2;

    @Column(name = "strength_desc3")
    private String strengthDesc3;

    @Column(name = "weakness_desc")
    private String weaknessDesc;

    @Column(name = "pattern_title1")
    private String patternTitle1;

    @Column(name = "pattern_title2")
    private String patternTitle2;

    @Column(name = "pattern_title3")
    private String patternTitle3;

    @Column(name = "pattern_title4")
    private String patternTitle4;

    @Column(name = "pattern1")
    private String pattern1;

    @Column(name = "pattern2")
    private String pattern2;

    @Column(name = "pattern3")
    private String pattern3;

    @Column(name = "pattern4")
    private String pattern4;

    @Column(name = "lovetype_feature_title1")
    private String loveTypeFeatureTitle1;

    @Column(name = "lovetype_feature_title2")
    private String loveTypeFeatureTitle2;

    @Column(name = "lovetype_feature_title3")
    private String loveTypeFeatureTitle3;

    @Column(name = "lovetype_feature_title4")
    private String loveTypeFeatureTitle4;

    @Column(name = "lovetype_feature1")
    private String loveTypeFeature1;

    @Column(name = "lovetype_feature2")
    private String loveTypeFeature2;

    @Column(name = "lovetype_feature3")
    private String loveTypeFeature3;

    @Column(name = "lovetype_feature4")
    private String loveTypeFeature4;

    @Column(name = "dating_guide1")
    private String datingGuide1;

    @Column(name = "dating_guide2")
    private String datingGuide2;

    @Column(name = "dating_guide3")
    private String datingGuide3;

    @Column(name = "best_mbti1")
    private String bestMbti1;

    @Column(name = "best_desc1")
    private String bestDesc1;

    @Column(name = "best_mbti2")
    private String bestMbti2;

    @Column(name = "best_desc2")
    private String bestDesc2;

    @Column(name = "worst_mbti1")
    private String worstMbti1;

    @Column(name = "worst_desc1")
    private String worstDesc1;

    @Column(name = "worst_mbti2")
    private String worstMbti2;

    @Column(name = "worst_desc2")
    private String worstDesc2;
}
