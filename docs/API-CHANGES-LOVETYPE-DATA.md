## 신규 API 스펙
### `POST /members/partners` — 상대방 프로필 최초 등록
**Request**
```ts
{
  mbti: string,
  loveTypeCategory: 'STABLE_TYPE' | 'ANXIETY_TYPE' | 'AVOIDANCE_TYPE' | 'CONFUSION_TYPE' | null
  // null = "모르겠어요" 선택
}
```
**Response**
```ts
{
  mbti: string,
  loveTypeCategory: 'STABLE_TYPE' | 'ANXIETY_TYPE' | 'AVOIDANCE_TYPE' | 'CONFUSION_TYPE' | 'UNKNOWN',
  description: string
}
```
---
### `PATCH /members/partners` — 상대방 프로필 수정
**Request**
```ts
{
  mbti?: string,
  loveTypeCategory?: 'STABLE_TYPE' | 'ANXIETY_TYPE' | 'AVOIDANCE_TYPE' | 'CONFUSION_TYPE' | null
}
```
**Response**
```ts
{
  mbti: string,
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
  mbti: string,                        // 내 MBTI
  loveTypeCategory: enum,              // 내 애착유형
  partnerMbti: string,                 // 상대 MBTI
  partnerLoveTypeCategory: 'STABLE_TYPE' | 'ANXIETY_TYPE' | 'AVOIDANCE_TYPE' | 'CONFUSION_TYPE' | 'UNKNOWN'
  // undefined = 미입력 / UNKNOWN = "모르겠어요" 선택됨
}
```

---

## 채팅 프롬프트 활용

사용자와 상대방 프로필의 `mbti`, `loveTypeCategory`, `partnerMbti`, `partnerLoveTypeCategory`는 채팅 시스템 메시지의 메타데이터 구성에도 사용됩니다.

### 활용 규칙

- 사용자 본인
  - `mbti`와 `loveTypeCategory`가 모두 있으면 `(mbti, lovetype)` 조합으로 상세 프롬프트를 조회합니다.
  - 둘 중 하나라도 없거나 매칭 row가 없으면 `UNKNOWN, 사용자와의 대화로부터 유추할 것`을 사용합니다.
- 상대방
  - `partnerMbti`가 있을 때만 상대방 성향 프롬프트 항목이 추가됩니다.
  - `partnerLoveTypeCategory`가 `UNKNOWN` 또는 `null`이면 DB 조회 없이 `UNKNOWN, 사용자와의 대화로부터 유추할 것`을 사용합니다.
  - `partnerMbti`와 확정된 `partnerLoveTypeCategory`가 모두 있으면 `(partnerMbti, partnerLoveTypeCategory)` 조합으로 상세 프롬프트를 조회합니다.
  - 매칭 row가 없으면 채팅은 실패하지 않고 동일한 폴백 문구를 사용합니다.

### 조회 대상 테이블

- `love_type_mbti_prompt`
  - `mbti`: MBTI 4자리 문자열
  - `lovetype`: `STABLE_TYPE | ANXIETY_TYPE | AVOIDANCE_TYPE | CONFUSION_TYPE`
  - `prompts`: 실제 채팅 메타데이터에 삽입할 프롬프트 전문

상세 동작 예시는 `docs/API-CHANGES-CHAT-PROMPT-MBTI-LOVETYPE.md`, 실제 전달 예시는 `docs/CHAT-PROMPT-MBTI-LOVETYPE-EXAMPLE.md`를 참고합니다.
