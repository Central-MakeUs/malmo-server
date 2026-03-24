# 상대방 애착유형 내부 추론 영속화 변경 사항

## 개요

사용자가 상대방 애착유형(`partnerLoveTypeCategory`)을 직접 입력하지 않았거나 `UNKNOWN`으로 남겨둔 경우,
1단계 상황 수집이 완료된 뒤 생성되는 **2단계 첫 분석 메시지 직후**에 챗봇이 상대방 애착유형을 내부적으로 추론하고 `member.partnerLoveTypeCategory`에 저장합니다.

이번 변경의 목적은 다음과 같습니다.

1. 1단계에서 수집된 갈등 상황 정보를 바탕으로 더 이른 시점에 상대방 애착유형을 확정하기
2. 2단계 분석 메시지와 동일한 맥락을 사용해 추론 결과의 일관성을 높이기
3. 3단계 이후 프롬프트에서 확정된 상대방 애착유형을 재사용해 상담 품질을 높이기

---

## 동작 시점

추론은 다음 조건을 모두 만족할 때 실행됩니다.

- 현재 완료된 단계가 `1단계`의 마지막 세부 단계임
- 1단계 완료 후 시스템이 `2단계`의 첫 분석 메시지를 생성함
- `partnerLoveTypeCategory`가 `null` 또는 `UNKNOWN`임

즉, **2단계 종료 시점이 아니라 2단계 첫 분석 메시지가 생성된 직후**에 실행됩니다.

---

## 추론 규칙

추론은 별도 내부 프롬프트(`GlobalConstants.PARTNER_LOVE_TYPE_INFERENCE_PROMPT`)를 사용하며,
입력 컨텍스트에는 아래 내용이 포함됩니다.

- 사용자 메타데이터
- 이전 단계 요약 메타데이터
- 2단계 레벨의 대화 메시지들
  - 여기에는 방금 생성된 챗봇의 2단계 첫 분석 메시지가 포함됩니다.

반환값은 아래 4개 enum 중 하나만 허용됩니다.

- `STABLE_TYPE`
- `ANXIETY_TYPE`
- `AVOIDANCE_TYPE`
- `CONFUSION_TYPE`

`UNKNOWN`은 추론 결과로 허용하지 않습니다.

응답 형식은 JSON 객체로 고정됩니다.

```json
{
  "partnerLoveTypeCategory": "STABLE_TYPE"
}
```

---

## 저장 규칙

- 저장 대상 필드: `member.partnerLoveTypeCategory`
- 저장 조건:
  - 현재 DB 값이 `null` 또는 `UNKNOWN`일 때만 반영
  - 이미 사용자가 직접 입력한 확정값이 있으면 덮어쓰지 않음
- 저장 직전에는 `Member`를 다시 조회해 최신 DB 상태를 확인합니다.

### 실패 처리

아래 경우에는 저장하지 않고 상담 흐름만 계속 진행합니다.

- JSON 파싱 실패
- enum 값 검증 실패
- `UNKNOWN` 반환
- 저장 직전 재조회 결과 이미 확정값이 존재함
- 영속화 과정 예외 발생

---

## 외부 API 영향

외부 요청/응답 스펙 변경은 없습니다.

다만 `GET /members`의 `partnerLoveTypeCategory`는 다음과 같은 값이 들어올 수 있습니다.

- 사용자가 직접 입력한 값
- 사용자가 `UNKNOWN`으로 설정한 값
- 내부 2단계 분석 직후 추론되어 저장된 값

즉, 클라이언트는 `partnerLoveTypeCategory`가 이전보다 더 이른 시점에 확정될 수 있음을 고려해야 합니다.

---

## 테스트

검증한 항목:

- `AUXILIARY_EXTRACTION` 시나리오로 JSON 추론 호출
- `UNKNOWN` 및 비정상 enum 값 거부
- 1단계 완료 후 2단계 첫 분석 메시지 직후 추론 및 저장
- 이미 확정된 상대방 애착유형이 있으면 추론/저장 생략
- 추론 실패 시 저장 없이 다음 단계 진행
- 2단계 종료 시점에는 더 이상 추론하지 않음

관련 테스트:

- `src/test/java/makeus/cmc/malmo/application/service/chat/ChatProcessorTest.java`
- `src/test/java/makeus/cmc/malmo/application/service/chat/ChatMessageServiceTest.java`
