## 신규 API 스펙
### `POST /members/partners` — 상대방 프로필 최초 등록
**Request**
```ts
{
  personalityType: string,
  loveTypeCategory: 'STABLE_TYPE' | 'ANXIETY_TYPE' | 'AVOIDANCE_TYPE' | 'CONFUSION_TYPE' | null
  // null = "모르겠어요" 선택
}
```
**Response**
```ts
{
  personalityType: string,
  loveTypeCategory: 'STABLE_TYPE' | 'ANXIETY_TYPE' | 'AVOIDANCE_TYPE' | 'CONFUSION_TYPE' | 'UNKNOWN',
  description: string
}
```
---
### `PATCH /members/partners` — 상대방 프로필 수정
두 필드를 항상 함께 전송해야 하며, 전달된 값으로 기존 값을 덮어씁니다.

**Request**
```ts
{
  personalityType: string,   // 영문 4자리 (예: "INTJ")
  loveTypeCategory: 'STABLE_TYPE' | 'ANXIETY_TYPE' | 'AVOIDANCE_TYPE' | 'CONFUSION_TYPE' | null
  // null = "모르겠어요" 선택
}
```
**Response**
```ts
{
  personalityType: string,
  loveTypeCategory: 'STABLE_TYPE' | 'ANXIETY_TYPE' | 'AVOIDANCE_TYPE' | 'CONFUSION_TYPE' | 'UNKNOWN',
  description: string
}
```
---
### `PATCH /members` — 기존 수정
기존 필드 유지, 아래 필드 추가
```ts
{
  loveTypeCategory?: 'STABLE_TYPE' | 'ANXIETY_TYPE' | 'AVOIDANCE_TYPE' | 'CONFUSION_TYPE'
}
```
---
### `GET /members` 응답 필드 추가
기존 응답 유지, 아래 필드 추가
```ts
{
  personalityType: string,             // 내 MBTI
  loveTypeCategory: enum,              // 내 애착유형
  otherPersonalityType: string,        // 상대 MBTI
  partnerLoveTypeCategory: 'STABLE_TYPE' | 'ANXIETY_TYPE' | 'AVOIDANCE_TYPE' | 'CONFUSION_TYPE' | 'UNKNOWN'
  // undefined = 미입력 / UNKNOWN = "모르겠어요" 선택됨
  // 또는 1단계 완료 후 2단계 첫 분석 메시지 직후 내부 추론값이 저장될 수 있음
}
```

---

## 채팅 프롬프트 활용

사용자와 상대방 프로필의 `personalityType`, `loveTypeCategory`, `otherPersonalityType`, `partnerLoveTypeCategory`는 채팅 시스템 메시지의 메타데이터 구성에도 사용됩니다.

### 활용 규칙

- 사용자 본인
  - `personalityType`와 `loveTypeCategory`가 모두 있으면 `(personalityType, lovetype)` 조합으로 상세 프롬프트를 조회합니다.
  - 둘 중 하나라도 없거나 매칭 row가 없으면 `UNKNOWN, 사용자와의 대화로부터 유추할 것`을 사용합니다.
- 상대방
  - `otherPersonalityType`가 있을 때만 상대방 성향 프롬프트 항목이 추가됩니다.
  - `partnerLoveTypeCategory`가 `UNKNOWN` 또는 `null`이면 DB 조회 없이 `UNKNOWN, 사용자와의 대화로부터 유추할 것`을 사용합니다.
  - 이후 1단계 완료 후 생성되는 2단계 첫 분석 메시지 직후 내부 추론이 성공하면, 저장된 확정값으로 프롬프트를 조회합니다.
  - `otherPersonalityType`와 확정된 `partnerLoveTypeCategory`가 모두 있으면 `(otherPersonalityType, partnerLoveTypeCategory)` 조합으로 상세 프롬프트를 조회합니다.
  - 매칭 row가 없으면 채팅은 실패하지 않고 동일한 폴백 문구를 사용합니다.

### 조회 대상 테이블

- `love_type_personality_type_prompt`
  - `personality_type`: MBTI 4자리 문자열
  - `lovetype`: `STABLE_TYPE | ANXIETY_TYPE | AVOIDANCE_TYPE | CONFUSION_TYPE`
  - `prompts`: 실제 채팅 메타데이터에 삽입할 프롬프트 전문

상세 동작 예시는 `docs/API-CHANGES-CHAT-PROMPT-PERSONALITY-TYPE-LOVETYPE.md`, 실제 전달 예시는 `docs/CHAT-PROMPT-PERSONALITY-TYPE-LOVETYPE-EXAMPLE.md`,
영속화 동작은 `docs/API-CHANGES-PARTNER-LOVETYPE-INFERENCE-PERSISTENCE.md`를 참고합니다.
