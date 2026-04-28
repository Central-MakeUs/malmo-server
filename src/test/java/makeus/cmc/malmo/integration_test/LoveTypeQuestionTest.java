package makeus.cmc.malmo.integration_test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import makeus.cmc.malmo.adaptor.out.persistence.entity.LoveTypePersonalityTypeFeatureEntity;
import makeus.cmc.malmo.adaptor.out.persistence.entity.TempLoveTypeEntity;
import makeus.cmc.malmo.domain.value.type.LoveTypeCategory;
import makeus.cmc.malmo.integration_test.dto_factory.LoveTypeQuestionRequestDtoFactory;
import makeus.cmc.malmo.integration_test.dto_factory.MemberRequestDtoFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static makeus.cmc.malmo.adaptor.in.exception.ErrorCode.*;
import static makeus.cmc.malmo.adaptor.in.exception.ErrorCode.NO_SUCH_LOVE_TYPE_QUESTION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
public class LoveTypeQuestionTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EntityManager em;

    @Nested
    @DisplayName("애착 유형 등록 테스트")
    class RegisterLoveTypeTest {
        @Test
        @DisplayName("애착 유형 등록 성공 - 회피형")
        void 애착_유형_등록_성공_회피형() throws Exception {
            // given
            int[] scores = {5, 1, 1, 5, 1, 1, 1, 5, 1, 1, 5, 1, 1, 1, 1, 5, 1, 1, 5, 1, 1, 1, 5, 1, 1, 1, 1, 5, 1, 1, 1, 1, 1, 1, 5, 1};

            // when
            MvcResult mvcResult = mockMvc.perform(post("/love-types/result")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    LoveTypeQuestionRequestDtoFactory.createRegisterLoveTypeRequestDto(scores)
                            )))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("data.loveTypeCategory").value(LoveTypeCategory.AVOIDANCE_TYPE.name()))
                    .andExpect(jsonPath("data.avoidanceRate").value(5.00f))
                    .andExpect(jsonPath("data.anxietyRate").value(1.00f))
                    .andReturn();
            String content = mvcResult.getResponse().getContentAsString();
            Integer loveTypeId = JsonPath.read(content, "$.data.loveTypeId");


            // then
            TempLoveTypeEntity tempLoveTypeEntity = em.find(TempLoveTypeEntity.class, loveTypeId);
            Assertions.assertThat(tempLoveTypeEntity.getCategory()).isEqualTo(LoveTypeCategory.AVOIDANCE_TYPE);
            Assertions.assertThat(tempLoveTypeEntity.getAvoidanceRate()).isEqualTo(5.00f);
            Assertions.assertThat(tempLoveTypeEntity.getAnxietyRate()).isEqualTo(1.00f);
        }

        @Test
        @DisplayName("애착 유형 등록 성공 - 불안형")
        void 애착_유형_등록_성공_불안형() throws Exception {
            // given
            int[] scores = {1, 5, 5, 1, 5, 5, 5, 1, 5, 5, 1, 5, 5, 5, 5, 1, 5, 5, 1, 5, 5, 5, 1, 5, 5, 5, 5, 1, 5, 5, 5, 5, 5, 5, 1, 5};

            // when
            MvcResult mvcResult = mockMvc.perform(post("/love-types/result")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    LoveTypeQuestionRequestDtoFactory.createRegisterLoveTypeRequestDto(scores)
                            )))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("data.loveTypeCategory").value(LoveTypeCategory.ANXIETY_TYPE.name()))
                    .andExpect(jsonPath("data.avoidanceRate").value(1.00f))
                    .andExpect(jsonPath("data.anxietyRate").value(5.00f))
                    .andReturn();
            String content = mvcResult.getResponse().getContentAsString();
            Integer loveTypeId = JsonPath.read(content, "$.data.loveTypeId");


            // then
            TempLoveTypeEntity tempLoveTypeEntity = em.find(TempLoveTypeEntity.class, loveTypeId);
            Assertions.assertThat(tempLoveTypeEntity.getCategory()).isEqualTo(LoveTypeCategory.ANXIETY_TYPE);
            Assertions.assertThat(tempLoveTypeEntity.getAvoidanceRate()).isEqualTo(1.00f);
            Assertions.assertThat(tempLoveTypeEntity.getAnxietyRate()).isEqualTo(5.00f);
        }

        @Test
        @DisplayName("애착 유형 등록 성공 - 혼란형")
        void 애착_유형_등록_성공_혼란형() throws Exception {
            // given
            int[] scores = {5, 5, 1, 5, 1, 5, 1, 1, 1, 5, 5, 5, 5, 5, 5, 5, 5, 1, 5, 5, 5, 5, 1, 5, 5, 5, 1, 5, 1, 1, 5, 1, 1, 1, 5, 5};

            // when
            MvcResult mvcResult = mockMvc.perform(post("/love-types/result")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    LoveTypeQuestionRequestDtoFactory.createRegisterLoveTypeRequestDto(scores)
                            )))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("data.loveTypeCategory").value(LoveTypeCategory.CONFUSION_TYPE.name()))
                    .andExpect(jsonPath("data.avoidanceRate").value(5.00f))
                    .andExpect(jsonPath("data.anxietyRate").value(5.00f))
                    .andReturn();
            String content = mvcResult.getResponse().getContentAsString();
            Integer loveTypeId = JsonPath.read(content, "$.data.loveTypeId");


            // then
            TempLoveTypeEntity tempLoveTypeEntity = em.find(TempLoveTypeEntity.class, loveTypeId);
            Assertions.assertThat(tempLoveTypeEntity.getCategory()).isEqualTo(LoveTypeCategory.CONFUSION_TYPE);
            Assertions.assertThat(tempLoveTypeEntity.getAvoidanceRate()).isEqualTo(5.00f);
            Assertions.assertThat(tempLoveTypeEntity.getAnxietyRate()).isEqualTo(5.00f);
        }

        @Test
        @DisplayName("애착 유형 등록 성공 - 안정형")
        void 애착_유형_등록_성공_안정형() throws Exception {
            // given
            int[] scores = {1, 1, 5, 1, 5, 1, 5, 5, 5, 1, 1, 1, 1, 1, 1, 1, 1, 5, 1, 1, 1, 1, 5, 1, 1, 1, 5, 1, 5, 5, 1, 5, 5, 5, 1, 1};

            // when
            MvcResult mvcResult = mockMvc.perform(post("/love-types/result")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    LoveTypeQuestionRequestDtoFactory.createRegisterLoveTypeRequestDto(scores)
                            )))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("data.loveTypeCategory").value(LoveTypeCategory.STABLE_TYPE.name()))
                    .andExpect(jsonPath("data.avoidanceRate").value(1.00f))
                    .andExpect(jsonPath("data.anxietyRate").value(1.00f))
                    .andReturn();
            String content = mvcResult.getResponse().getContentAsString();
            Integer loveTypeId = JsonPath.read(content, "$.data.loveTypeId");

            // then
            TempLoveTypeEntity tempLoveTypeEntity = em.find(TempLoveTypeEntity.class, loveTypeId);
            Assertions.assertThat(tempLoveTypeEntity.getCategory()).isEqualTo(LoveTypeCategory.STABLE_TYPE);
            Assertions.assertThat(tempLoveTypeEntity.getAvoidanceRate()).isEqualTo(1.00f);
            Assertions.assertThat(tempLoveTypeEntity.getAnxietyRate()).isEqualTo(1.00f);
        }

        @Test
        @DisplayName("애착 유형 등록 실패 - 점수가 0점인 경우")
        void 애착_유형_등록_실패_점수가_0점인_경우() throws Exception {
            // given
            int[] scores = {1, 1, 5, 1, 0, 1, 5, 5, 5, 1, 1, 1, 1, 1, 1, 1, 1, 5, 1, 1, 1, 1, 5, 1, 1, 1, 5, 1, 5, 5, 1, 5, 5, 5, 1, 1};
            // when & then
            mockMvc.perform(post("/love-types/result")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    MemberRequestDtoFactory.createRegisterLoveTypeRequestDto(scores)
                            )))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("message").value(BAD_REQUEST.getMessage()))
                    .andExpect(jsonPath("code").value(BAD_REQUEST.getCode()));
        }

        @Test
        @DisplayName("애착 유형 등록 실패 - 점수가 6점인 경우")
        void 애착_유형_등록_실패_점수가_6점인_경우() throws Exception {
            // given
            int[] scores = {1, 1, 5, 1, 6, 1, 5, 5, 5, 1, 1, 1, 1, 1, 1, 1, 1, 5, 1, 1, 1, 1, 5, 1, 1, 1, 5, 1, 5, 5, 1, 5, 5, 5, 1, 1};
            // when & then
            mockMvc.perform(post("/love-types/result")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    MemberRequestDtoFactory.createRegisterLoveTypeRequestDto(scores)
                            )))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("message").value(BAD_REQUEST.getMessage()))
                    .andExpect(jsonPath("code").value(BAD_REQUEST.getCode()));
        }

        @Test
        @DisplayName("애착 유형 등록 실패 - 존재하지 않는 질문인 경우")
        void 애착_유형_등록_실패_존재하지_않는_질문인_경우() throws Exception {
            // given
            // 36개의 질문에 대한 점수 배열이 필요하지만, 37번에 대한 답변을 했다고 가정
            int[] scores = {1, 1, 5, 1, 1, 1, 5, 5, 5, 1, 1, 1, 1, 1, 1, 1, 1, 5, 1, 1, 1, 1, 5, 1, 1, 1, 5, 1, 5, 5, 1, 5, 5, 5, 1, 1, 4};
            // when & then
            mockMvc.perform(post("/love-types/result")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    MemberRequestDtoFactory.createRegisterLoveTypeRequestDto(scores)
                            )))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("message").value(NO_SUCH_LOVE_TYPE_QUESTION.getMessage()))
                    .andExpect(jsonPath("code").value(NO_SUCH_LOVE_TYPE_QUESTION.getCode()));
        }
    }

    @Nested
    @DisplayName("등록된 애착 유형 결과 조회 테스트")
    class GetLoveTypeResultTest {
        @Test
        @DisplayName("애착 유형 조회 성공")
        void 애착_유형_조회_성공() throws Exception {
            // given
            int[] scores = {1, 1, 5, 1, 5, 1, 5, 5, 5, 1, 1, 1, 1, 1, 1, 1, 1, 5, 1, 1, 1, 1, 5, 1, 1, 1, 5, 1, 5, 5, 1, 5, 5, 5, 1, 1};

            MvcResult mvcResult = mockMvc.perform(post("/love-types/result")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    LoveTypeQuestionRequestDtoFactory.createRegisterLoveTypeRequestDto(scores)
                            )))
                    .andExpect(status().isOk())
                    .andReturn();

            String content = mvcResult.getResponse().getContentAsString();
            Integer loveTypeId = JsonPath.read(content, "$.data.loveTypeId");

            // when
            mockMvc.perform(get("/love-types/result/" + loveTypeId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("data.loveTypeId").value(loveTypeId))
                    .andExpect(jsonPath("data.loveTypeCategory").value(LoveTypeCategory.STABLE_TYPE.name()))
                    .andExpect(jsonPath("data.avoidanceRate").value(1.00f))
                    .andExpect(jsonPath("data.anxietyRate").value(1.00f));
        }

        @Test
        @DisplayName("애착 유형 조회 실패 - 존재하지 않는 애착 유형 ID인 경우")
        void 애착_유형_조회_실패_존재하지_않는_애착_유형_ID인_경우() throws Exception {
            // given
            long loveTypeId = 999L; // 존재하지 않는 애착 유형 ID

            // when
            mockMvc.perform(get("/love-types/result/" + loveTypeId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("message").value(NO_SUCH_TEMP_LOVE_TYPE.getMessage()))
                    .andExpect(jsonPath("code").value(NO_SUCH_TEMP_LOVE_TYPE.getCode()));
        }

    }

    @Nested
    @DisplayName("MBTI + 애착 유형 상세 결과 조회 테스트")
    class GetLoveTypePersonalityTypeResultTest {
        @Test
        @DisplayName("MBTI와 애착 유형 상세 결과 조회 성공 - 소문자 쿼리와 빈 항목 제외")
        void personalityType과_애착유형_상세_결과_조회_성공() throws Exception {
            // given
            em.persist(LoveTypePersonalityTypeFeatureEntity.builder()
                    .personalityType("ENFP")
                    .loveTypeCategory(LoveTypeCategory.STABLE_TYPE)
                    .summary("풍부한 상상력과 사랑으로, 함께하는 일상을 즐겁게 만들어 가는 유형")
                    .keyword1("열정적")
                    .keyword2("자유로움")
                    .keyword3("")
                    .strength1("Ne")
                    .strengthDesc1("흩어진 정보 속에서 하나의 핵심 맥락과 미래를 읽어내요")
                    .strength2("")
                    .strengthDesc2("")
                    .strength3(null)
                    .strengthDesc3(null)
                    .weakness("Si")
                    .weaknessDesc("반복되는 루틴에 답답함을 느껴요")
                    .patternTitle1("나다운 기준을 지켜요")
                    .pattern1("여러 가능성 속에서 무엇이 나에게 의미 있는지 먼저 생각해요")
                    .patternTitle2("")
                    .pattern2("")
                    .patternTitle3(null)
                    .pattern3(null)
                    .patternTitle4(null)
                    .pattern4(null)
                    .loveTypeFeatureTitle1("미래의 가능성을 자주 상상해요")
                    .loveTypeFeature1("연인과 함께할 미래의 가능성을 상상하며 창의적인 질문으로 관계를 만들어요")
                    .loveTypeFeatureTitle2("")
                    .loveTypeFeature2("")
                    .loveTypeFeatureTitle3(null)
                    .loveTypeFeature3(null)
                    .loveTypeFeatureTitle4(null)
                    .loveTypeFeature4(null)
                    .datingGuide1("감정을 정리해 표현해요 : 솔직하게 말하면 관계가 깊어져요")
                    .datingGuide2("")
                    .datingGuide3(null)
                    .bestPersonalityType1("infj")
                    .bestDesc1("속마음을 깊이 이해해주며 안정적인 감정을 공유하는 궁합")
                    .bestPersonalityType2("")
                    .bestDesc2("")
                    .worstPersonalityType1("istp")
                    .worstDesc1("자유로운 감정선과 솔직한 피드백이 부딪히는 궁합")
                    .worstPersonalityType2(null)
                    .worstDesc2(null)
                    .build());
            em.flush();
            em.clear();

            // when & then
            mockMvc.perform(get("/love-types/result")
                            .param("personalityType", "enfp")
                            .param("lovetype", "stable_type")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("data.personalityType").value("ENFP"))
                    .andExpect(jsonPath("data.loveTypeCategory").value(LoveTypeCategory.STABLE_TYPE.name()))
                    .andExpect(jsonPath("data.summary").value("풍부한 상상력과 사랑으로, 함께하는 일상을 즐겁게 만들어 가는 유형"))
                    .andExpect(jsonPath("data.keywords.length()").value(2))
                    .andExpect(jsonPath("data.keywords[0]").value("열정적"))
                    .andExpect(jsonPath("data.strengths.length()").value(1))
                    .andExpect(jsonPath("data.strengths[0].title").value("Ne"))
                    .andExpect(jsonPath("data.weaknesses.length()").value(1))
                    .andExpect(jsonPath("data.weaknesses[0].title").value("Si"))
                    .andExpect(jsonPath("data.patterns.length()").value(1))
                    .andExpect(jsonPath("data.loveTypeFeatures.length()").value(1))
                    .andExpect(jsonPath("data.datingGuides.length()").value(1))
                    .andExpect(jsonPath("data.datingGuides[0].title").value("감정을 정리해 표현해요"))
                    .andExpect(jsonPath("data.datingGuides[0].description").value("솔직하게 말하면 관계가 깊어져요"))
                    .andExpect(jsonPath("data.bestMatches.length()").value(1))
                    .andExpect(jsonPath("data.bestMatches[0].personalityType").value("INFJ"))
                    .andExpect(jsonPath("data.worstMatches.length()").value(1))
                    .andExpect(jsonPath("data.worstMatches[0].personalityType").value("ISTP"));
        }

        @Test
        @DisplayName("MBTI와 애착 유형 상세 결과 조회 실패 - MBTI 형식 오류")
        void personalityType과_애착유형_상세_결과_조회_실패_personalityType형식오류() throws Exception {
            mockMvc.perform(get("/love-types/result")
                            .param("personalityType", "ENF")
                            .param("lovetype", "STABLE_TYPE")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("message").value(BAD_REQUEST.getMessage()))
                    .andExpect(jsonPath("code").value(BAD_REQUEST.getCode()));
        }

        @Test
        @DisplayName("MBTI와 애착 유형 상세 결과 조회 실패 - 애착 유형 값 오류")
        void personalityType과_애착유형_상세_결과_조회_실패_애착유형값오류() throws Exception {
            mockMvc.perform(get("/love-types/result")
                            .param("personalityType", "ENFP")
                            .param("lovetype", "WRONG_TYPE")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("message").value(BAD_REQUEST.getMessage()))
                    .andExpect(jsonPath("code").value(BAD_REQUEST.getCode()));
        }

        @Test
        @DisplayName("MBTI와 애착 유형 상세 결과 조회 실패 - 매칭되는 결과 없음")
        void personalityType과_애착유형_상세_결과_조회_실패_결과없음() throws Exception {
            mockMvc.perform(get("/love-types/result")
                            .param("personalityType", "ENFP")
                            .param("lovetype", "STABLE_TYPE")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("message").value(NO_SUCH_LOVE_TYPE_PERSONALITY_TYPE_RESULT.getMessage()))
                    .andExpect(jsonPath("code").value(NO_SUCH_LOVE_TYPE_PERSONALITY_TYPE_RESULT.getCode()));
        }
    }
}
