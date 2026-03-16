# 채팅 프롬프트 변경 사항 - MBTI + 애착유형 조합 프롬프트 주입

## 개요

채팅 시스템 메시지의 `[사용자 메타데이터]` 블록에 사용자와 상대방의 `MBTI + 애착유형` 조합별 프롬프트를 함께 주입합니다.

이 변경의 목적은 다음과 같습니다.

1. 상담 모델이 사용자 성향과 상대방 성향을 더 구체적으로 이해하도록 돕기
2. `UNKNOWN` 상태를 명시적으로 전달해 대화 문맥으로 추론하게 만들기
3. 조합 데이터가 일부 비어 있어도 채팅을 실패시키지 않고 안전하게 계속 진행하기

---

## 데이터 소스

### 신규 테이블

`love_type_mbti_prompt`

| column | type | description |
| --- | --- | --- |
| `mbti` | `VARCHAR(4)` | MBTI 4자리 문자열 |
| `lovetype` | `VARCHAR(255)` | `LoveTypeCategory` enum 문자열 |
| `prompts` | `TEXT` | 채팅 메타데이터에 삽입할 프롬프트 전문 |

### 키 규칙

- `(mbti, lovetype)` 복합 PK
- 애플리케이션은 MBTI를 대문자로 정규화해 조회합니다.
- `UNKNOWN` row는 저장하지 않습니다.

### DDL

```sql
CREATE TABLE love_type_mbti_prompt (
    mbti VARCHAR(4) NOT NULL,
    lovetype VARCHAR(255) NOT NULL,
    prompts TEXT,
    PRIMARY KEY (mbti, lovetype)
);
```

실제 배포용 SQL은 `sqls/MM-181.sql`에 있습니다.

---

## 채팅 메타데이터 주입 규칙

프롬프트는 `ChatPromptBuilder`에서 `[사용자 메타데이터]` 문자열을 만들 때 삽입됩니다.

### 추가되는 항목

```text
- 사용자 성향 프롬프트:
{사용자 조합 프롬프트 또는 폴백 문구}

- 상대방 성향 프롬프트:
{상대방 조합 프롬프트 또는 폴백 문구}
```

### 사용자 본인

- `member.mbti`와 `member.loveTypeCategory`가 모두 있으면 `love_type_mbti_prompt`를 조회합니다.
- 조합 row가 존재하면 `prompts` 컬럼 값을 그대로 삽입합니다.
- 둘 중 하나라도 없거나 조합 row가 없으면 아래 폴백 문구를 삽입합니다.

```text
UNKNOWN, 사용자와의 대화로부터 유추할 것
```

### 상대방

- `partnerMbti`가 있는 경우에만 `- 상대방 성향 프롬프트:` 항목을 추가합니다.
- `partnerLoveTypeCategory`가 `UNKNOWN` 또는 `null`이면 DB 조회 없이 바로 폴백 문구를 삽입합니다.
- `partnerMbti`와 확정된 `partnerLoveTypeCategory`가 모두 있으면 `(partnerMbti, partnerLoveTypeCategory)` 조합으로 조회합니다.
- 조합 row가 없으면 채팅은 실패시키지 않고 동일한 폴백 문구를 삽입합니다.

---

## 런타임 동작

### 성공 케이스

- 사용자 조합 row가 있으면 사용자 성향 프롬프트에 해당 전문이 들어갑니다.
- 상대방 조합 row가 있으면 상대방 성향 프롬프트에 해당 전문이 들어갑니다.

### 폴백 케이스

아래 경우에는 모두 동일한 폴백 문구를 사용합니다.

- 사용자 MBTI 없음
- 사용자 애착유형 없음
- 상대방 MBTI 없음
- 상대방 애착유형이 `UNKNOWN`
- 상대방 애착유형이 `null`
- 조합 row 없음
- `prompts` 값이 비어 있음

### 로깅

- MBTI/애착유형 값은 존재하지만 조합 row가 없는 경우 `warn` 로그를 남깁니다.
- 이 경우에도 채팅 요청은 실패하지 않습니다.

---

## 외부 API 영향

외부 HTTP API 스펙 변경은 없습니다.

다만 아래 프로필 데이터가 채팅 프롬프트 생성에 직접 활용됩니다.

- `GET /members`의 `mbti`
- `GET /members`의 `loveTypeCategory`
- `GET /members`의 `partnerMbti`
- `GET /members`의 `partnerLoveTypeCategory`

---

## 테스트

검증한 항목:

- MBTI 대소문자 무관 조회
- 사용자 조합 프롬프트 삽입
- 사용자 정보 없음 시 폴백 문구 삽입
- 상대방 조합 프롬프트 삽입
- 상대방 애착유형 `UNKNOWN` 시 폴백 문구 삽입
- 상대방 프로필 미등록 시 상대방 성향 프롬프트 항목 생략
- 조합 row 누락 시 예외 없이 폴백 처리

관련 테스트:

- `src/test/java/makeus/cmc/malmo/application/service/chat/ChatPromptBuilderTest.java`
- `src/test/java/makeus/cmc/malmo/integration_test/LoveTypeMbtiPromptPersistenceAdapterTest.java`

실제 시스템 메시지 예시는 `docs/CHAT-PROMPT-MBTI-LOVETYPE-EXAMPLE.md`를 참고합니다.
