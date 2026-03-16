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