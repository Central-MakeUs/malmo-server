## 신규 API 스펙

### `GET /love-types/result` - MBTI + 애착유형 상세 결과 조회

`mbti`와 `lovetype` 조합으로 MBTI별 애착유형 상세 결과를 조회합니다.

기존 API와의 차이:
- `POST /love-types/result`: 검사 답변을 제출하고 임시 결과를 생성
- `GET /love-types/result/{loveTypeId}`: 생성된 임시 검사 결과를 조회
- `GET /love-types/result`: `(mbti, lovetype)` 조합에 대응하는 고정 상세 콘텐츠를 조회

---

## Request

### Query Parameters

| name | type | required | description |
| --- | --- | --- | --- |
| `mbti` | String | Y | 영문 4자리 MBTI. 대소문자 무관, 내부적으로 대문자로 정규화 |
| `lovetype` | String | Y | `STABLE_TYPE`, `ANXIETY_TYPE`, `AVOIDANCE_TYPE`, `CONFUSION_TYPE` 중 하나. 대소문자 무관 |

### Example

```http
GET /love-types/result?mbti=enfp&lovetype=stable_type
```

---

## Response

```ts
{
  mbti: string,
  loveTypeCategory: 'STABLE_TYPE' | 'ANXIETY_TYPE' | 'AVOIDANCE_TYPE' | 'CONFUSION_TYPE',
  summary: string,
  keywords: string[],
  strengths: Array<{
    title: string | null,
    description: string | null
  }>,
  weaknesses: Array<{
    title: string | null,
    description: string | null
  }>,
  patterns: Array<{
    title: string | null,
    description: string | null
  }>,
  loveTypeFeatures: Array<{
    title: string | null,
    description: string | null
  }>,
  datingGuides: string[],
  bestMatches: Array<{
    mbti: string | null,
    description: string | null
  }>,
  worstMatches: Array<{
    mbti: string | null,
    description: string | null
  }>
}
```

### Example Response

```json
{
  "mbti": "ENFP",
  "loveTypeCategory": "STABLE_TYPE",
  "summary": "풍부한 상상력과 사랑으로, 함께하는 일상을 즐겁게 만들어 가는 유형",
  "keywords": ["열정적", "자유로움", "생기발랄"],
  "strengths": [
    {
      "title": "Ne",
      "description": "흩어진 정보 속에서 하나의 핵심 맥락과 미래를 읽어내요"
    },
    {
      "title": "Fi",
      "description": "나의 진솔한 감정을 공유하는 것을 최고의 가치로 여겨요"
    }
  ],
  "weaknesses": [
    {
      "title": "Si",
      "description": "반복되는 루틴에 답답함을 느껴요"
    }
  ],
  "patterns": [
    {
      "title": "나다운 기준을 지켜요",
      "description": "여러 가능성 속에서 무엇이 나에게 의미 있는지 먼저 생각해요"
    }
  ],
  "loveTypeFeatures": [
    {
      "title": "미래의 가능성을 자주 상상해요",
      "description": "연인과 함께할 미래의 가능성을 상상하며 창의적인 질문으로 관계를 만들어요"
    }
  ],
  "datingGuides": ["감정을 정리해 표현해요"],
  "bestMatches": [
    {
      "mbti": "INFJ",
      "description": "속마음을 깊이 이해해주며 안정적인 감정을 공유하는 궁합"
    }
  ],
  "worstMatches": [
    {
      "mbti": "ISTP",
      "description": "자유로운 감정선과 솔직한 피드백이 부딪히는 궁합"
    }
  ]
}
```

---

## DB 매핑 기준

조회 대상 테이블은 `love_type_mbti_feature`입니다.

테이블 컬럼은 `docs/mbti-lovetype-feature.md` 헤더를 기준으로 그대로 맞춥니다.

| column | description |
| --- | --- |
| `lovetype` | 애착유형 enum 문자열 |
| `mbti` | MBTI 4자리 문자열 |
| `summary` | 요약 문구 |
| `keyword1` ~ `keyword3` | 키워드 |
| `strength1` ~ `strength3` | 강점 제목 |
| `strength_desc1` ~ `strength_desc3` | 강점 설명 |
| `weakness` | 약점 제목 |
| `weakness_desc` | 약점 설명 |
| `pattern_title1` ~ `pattern_title4` | 관계 패턴 제목 |
| `pattern1` ~ `pattern4` | 관계 패턴 설명 |
| `lovetype_feature_title1` ~ `lovetype_feature_title4` | 애착유형 특징 제목 |
| `lovetype_feature1` ~ `lovetype_feature4` | 애착유형 특징 설명 |
| `dating_guide1` ~ `dating_guide3` | 연애 가이드 |
| `best_mbti1` ~ `best_mbti2` | 잘 맞는 MBTI |
| `best_desc1` ~ `best_desc2` | 잘 맞는 MBTI 설명 |
| `worst_mbti1` ~ `worst_mbti2` | 부딪히기 쉬운 MBTI |
| `worst_desc1` ~ `worst_desc2` | 부딪히기 쉬운 MBTI 설명 |

주의:
- 애플리케이션은 별도 audit 컬럼 없이 위 문서 컬럼만 기준으로 조회합니다.
- `(mbti, lovetype)` 조합은 유니크하다고 가정합니다.

---

## 응답 가공 규칙

문서의 번호형 컬럼은 응답에서 묶어서 반환합니다.

- `keyword1` ~ `keyword3` -> `keywords[]`
- `strengthN + strength_descN` -> `strengths[]`
- `weakness + weakness_desc` -> `weaknesses[]`
- `pattern_titleN + patternN` -> `patterns[]`
- `lovetype_feature_titleN + lovetype_featureN` -> `loveTypeFeatures[]`
- `dating_guideN` -> `datingGuides[]`
- `best_mbtiN + best_descN` -> `bestMatches[]`
- `worst_mbtiN + worst_descN` -> `worstMatches[]`

빈 문자열 또는 null은 응답 배열에서 제외합니다.

예:
- `strength2=""`, `strength_desc2=""` 이면 `strengths` 배열에 두 번째 항목은 포함되지 않습니다.
- `best_mbti2`만 비어 있어도 `bestMatches` 두 번째 항목은 제외됩니다.

---

## 에러 응답

### 잘못된 요청
- `mbti`가 영문 4자리가 아닌 경우
- `lovetype`이 허용 enum 값이 아닌 경우
- 필수 query parameter가 누락된 경우

```json
{
  "success": false,
  "message": "잘못된 요청입니다.",
  "code": 40000
}
```

### 데이터 없음
- `mbti + lovetype` 조합에 해당하는 row가 없는 경우

```json
{
  "success": false,
  "message": "해당 MBTI와 애착 유형 결과가 존재하지 않습니다.",
  "code": 40019
}
```
