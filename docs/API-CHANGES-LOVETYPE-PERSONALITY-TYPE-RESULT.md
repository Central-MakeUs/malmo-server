## 신규 API 스펙

### `GET /love-types/result` - personalityType + 애착유형 상세 결과 조회

`personalityType`와 `lovetype` 조합으로 상세 결과를 조회합니다.

기존 API와의 차이:
- `POST /love-types/result`: 검사 답변을 제출하고 임시 결과를 생성
- `GET /love-types/result/{loveTypeId}`: 생성된 임시 검사 결과를 조회
- `GET /love-types/result`: `(personalityType, lovetype)` 조합에 대응하는 고정 상세 콘텐츠를 조회

## Request

| name | type | required | description |
| --- | --- | --- | --- |
| `personalityType` | String | Y | 영문 4자리 MBTI. 대소문자 무관, 내부적으로 대문자로 정규화 |
| `lovetype` | String | Y | `STABLE_TYPE`, `ANXIETY_TYPE`, `AVOIDANCE_TYPE`, `CONFUSION_TYPE` 중 하나 |

```http
GET /love-types/result?personalityType=enfp&lovetype=stable_type
```

## Response

```ts
{
  personalityType: string,
  loveTypeCategory: 'STABLE_TYPE' | 'ANXIETY_TYPE' | 'AVOIDANCE_TYPE' | 'CONFUSION_TYPE',
  summary: string,
  keywords: string[],
  strengths: Array<{ title: string | null, description: string | null }>,
  weaknesses: Array<{ title: string | null, description: string | null }>,
  patterns: Array<{ title: string | null, description: string | null }>,
  loveTypeFeatures: Array<{ title: string | null, description: string | null }>,
  datingGuides: Array<{ title: string, description: string | null }>,
  bestMatches: Array<{ personalityType: string | null, description: string | null }>,
  worstMatches: Array<{ personalityType: string | null, description: string | null }>
}
```

## DB 매핑 기준

- 조회 대상 테이블은 `love_type_personality_type_feature`입니다.
- 복합키는 `(personality_type, lovetype)`입니다.
- 궁합 컬럼은 `best_personality_type1/2`, `worst_personality_type1/2`를 사용합니다.

## 에러 응답

- `personalityType`가 영문 4자리가 아닌 경우
- `lovetype`이 허용 enum 값이 아닌 경우
- `personalityType + lovetype` 조합에 해당하는 row가 없는 경우
