# API 변경 사항 - Weekly Report / Notification

## 문서 목적

최근 5개 커밋을 기준으로 `Weekly Report` 신기능과 `Notification` 연동에 대해 프론트엔드 개발자가 바로 붙일 수 있도록 HTTP API 계약을 정리한 문서입니다.

기준 커밋:

- `e644af2` (2026-04-02): `feat: 주간 리포트 생성 및 조회 기능 개발 (#125)`
- `4da791f` (2026-04-02): `fix: audit column becomes null when report updated`
- `810d4ce` (2026-04-02): `fix: 기존 리포트 FAILED로 바꿔 저장 시 createdAt 불일치`
- `5eda183` (2026-04-02): `fix: createdAt should not be updated`
- `11fa197` (2026-04-02): `fix: failed test`

핵심 정리:

- 신규 사용자 공개 API가 추가되었습니다.
  - `GET /reports/weekly`
  - `GET /reports/weekly/{weekStartDate}`
- 기존 알림 API 응답에 새 알림 타입이 추가되었습니다.
  - `WEEKLY_ANALYSIS_REPORT_PUBLISHED`
- 기존 알림 읽음 처리 API로 주간 리포트 알림도 읽음 처리할 수 있습니다.
  - `PATCH /members/notifications/pending`
- 관리자용 수동 트리거 API도 함께 추가되었습니다.
  - `POST /admin/reports/weekly/{weekStartDate}/trigger`
- 뒤의 4개 fix 커밋은 주로 DB 저장 시각/감사 컬럼 보정입니다.
  - 프론트가 사용하는 공개 응답 JSON 스키마는 바뀌지 않았습니다.

---

## 프론트에서 꼭 알아야 하는 변경점

### 1. 주간 리포트는 "조회 API"만 공개됨

- 앱 클라이언트가 직접 리포트를 생성하는 API는 없습니다.
- 리포트는 서버 스케줄러 또는 관리자 수동 트리거로 생성됩니다.
- 프론트는 발행된(`PUBLISHED`) 리포트만 조회할 수 있습니다.

### 2. 주간 리포트 발행 알림은 기존 Notification API에 섞여 내려옴

- 별도의 신규 알림 조회 API는 없습니다.
- 기존 `GET /members/notifications/pending` 응답에서 `type = WEEKLY_ANALYSIS_REPORT_PUBLISHED` 를 처리하면 됩니다.
- 현재 커밋 기준으로 주간 리포트 발행 시 별도 SSE 이벤트 전송 코드는 추가되지 않았습니다.
  - 즉, 실시간 push/SSE 계약 변경이 아니라 "pending notification 저장" 방식입니다.

### 3. 리포트 상세 응답은 `content` 래퍼 없이 바로 펼쳐짐

- 응답 `data` 내부에 `overview`, `topTopics`, `conflict` 등이 바로 있습니다.
- `data.content.overview` 구조가 아닙니다.

### 4. 미발행 리포트는 존재해도 숨김 처리됨

- `PENDING`, `GENERATING`, `FAILED` 상태는 사용자 공개 API에서 숨겨집니다.
- 상세 조회 시 이 상태들은 모두 `404`로 내려옵니다.
- 목록 조회에도 포함되지 않습니다.

---

## 공통 응답 형식

성공 응답 기본 구조:

```json
{
  "requestId": "e762d840-9565-4612-b308-42d1a50dc0c2",
  "success": true,
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {}
}
```

리스트 응답 기본 구조:

```json
{
  "requestId": "e762d840-9565-4612-b308-42d1a50dc0c2",
  "success": true,
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "size": 0,
    "page": null,
    "list": [],
    "totalCount": 0
  }
}
```

에러 응답 기본 구조:

```json
{
  "requestId": "e762d840-9565-4612-b308-42d1a50dc0c2",
  "code": 40401,
  "success": false,
  "message": "주간 분석 리포트가 존재하지 않습니다."
}
```

공통 헤더:

- `Authorization: Bearer {accessToken}`

---

## 신규/변경 API 상세

## 1. GET /reports/weekly

주간 리포트 목록 조회 API입니다.

### 용도

- 현재 로그인한 사용자의 발행 완료된 주간 리포트 목록 조회
- 최신 주차부터 내려옴

### 요청

- Method: `GET`
- Path: `/reports/weekly`
- Request Body: 없음

### 응답 필드

최상위 `data`:

| 필드 | 타입 | 의미 |
|---|---|---|
| `size` | Number | 현재 응답에 담긴 리스트 길이 |
| `page` | Number \| null | 페이징 미사용이므로 항상 `null` |
| `list` | Array | 리포트 목록 |
| `totalCount` | Number | 전체 리포트 개수. 현재는 `list.length`와 동일 |

`data.list[]` 각 아이템:

| 필드 | 타입 | 의미 |
|---|---|---|
| `weekStartDate` | `yyyy-MM-dd` String | 해당 주차의 시작일(월요일) |
| `weekEndDate` | `yyyy-MM-dd` String | 해당 주차의 종료일(일요일) |
| `status` | String | 현재 공개 API 기준 항상 `PUBLISHED` |
| `generatedAt` | ISO DateTime String | 리포트가 발행된 시각 |

### 성공 예시

```json
{
  "requestId": "req-1",
  "success": true,
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "size": 2,
    "page": null,
    "list": [
      {
        "weekStartDate": "2026-03-30",
        "weekEndDate": "2026-04-05",
        "status": "PUBLISHED",
        "generatedAt": "2026-04-06T00:02:14"
      },
      {
        "weekStartDate": "2026-03-23",
        "weekEndDate": "2026-03-29",
        "status": "PUBLISHED",
        "generatedAt": "2026-03-30T00:02:14"
      }
    ],
    "totalCount": 2
  }
}
```

### 빈 목록 예시

```json
{
  "requestId": "req-2",
  "success": true,
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "size": 0,
    "page": null,
    "list": [],
    "totalCount": 0
  }
}
```

### 응답/처리 규칙

- 목록에는 `PUBLISHED` 리포트만 포함됩니다.
- 정렬 기준은 `weekStartDate DESC` 입니다.
- `PENDING`, `GENERATING`, `FAILED` 상태 리포트는 존재해도 내려오지 않습니다.

### 가능한 응답 케이스

| HTTP | code | 의미 |
|---|---|---|
| `200` | 없음 | 성공 |
| `401` | `40100` | 인증 실패 |
| `400` | `40001` | 토큰의 회원이 실제로 존재하지 않음 |
| `500` | `50000` | 서버 내부 오류 |

---

## 2. GET /reports/weekly/{weekStartDate}

특정 주차의 주간 리포트 상세 조회 API입니다.

### 용도

- 특정 주간 리포트 본문 조회
- 상세 화면 진입 API

### 요청

- Method: `GET`
- Path: `/reports/weekly/{weekStartDate}`
- Path Variable:

| 이름 | 타입 | 의미 |
|---|---|---|
| `weekStartDate` | `yyyy-MM-dd` String | 조회할 주차의 시작일(월요일) |

### 프론트 사용 규칙

- 직접 날짜 계산해서 넣기보다 아래 값 중 하나를 그대로 사용하는 것을 권장합니다.
  - `GET /reports/weekly` 의 `weekStartDate`
  - `GET /members/notifications/pending` 의 `payload.weekStartDate`
- 서버 설명상 월요일 날짜를 넣는 계약입니다.
- 날짜 형식이 틀리면 `400`, 형식은 맞지만 해당 주차 리포트가 없거나 미발행이면 `404` 입니다.

### 응답 필드

상위 필드:

| 필드 | 타입 | 의미 |
|---|---|---|
| `weekStartDate` | `yyyy-MM-dd` String | 리포트 주 시작일 |
| `weekEndDate` | `yyyy-MM-dd` String | 리포트 주 종료일 |
| `status` | String | 현재 공개 API 기준 항상 `PUBLISHED` |
| `generatedAt` | ISO DateTime String | 리포트 발행 시각 |
| `schemaVersion` | String | 리포트 스키마 버전. 현재 `v1` |
| `period` | Object | 기간 메타 정보 |
| `overview` | Object | 주간 요약 |
| `topTopics` | Array | 주요 주제 목록 |
| `moodByTime` | Object | 시간대별 기분/고민 흐름 |
| `conflict` | Object | 갈등 지표 |
| `behaviorPattern` | Object | 행동 패턴 분석 |
| `solution` | Object | 바로 실천할 솔루션 |

`period`:

| 필드 | 타입 | 의미 |
|---|---|---|
| `weekStartDate` | `yyyy-MM-dd` String | 기간 시작일 |
| `weekEndDate` | `yyyy-MM-dd` String | 기간 종료일 |
| `timezone` | String | 기준 타임존. 현재 `Asia/Seoul` |

`overview`:

| 필드 | 타입 | 의미 |
|---|---|---|
| `title` | String | 리포트 상단 제목 |
| `summary` | String | 주간 전체 요약 |

`topTopics[]`:

| 필드 | 타입 | 의미 |
|---|---|---|
| `keyword` | String | 핵심 주제 키워드 |
| `rank` | Number | 주제 우선순위 |
| `weight` | Number | 상대 비중 |
| `description` | String | 해당 주제를 풀어쓴 설명 |

추가 규칙:

- 서버는 `topTopics` 를 최대 3개까지만 반환합니다.
- 배열 순서를 그대로 쓰되, 정렬이 필요하면 `rank` 를 기준으로 사용하는 것이 안전합니다.

`moodByTime`:

| 필드 | 타입 | 의미 |
|---|---|---|
| `dominantPeriod` | String | 가장 두드러진 시간대. `MORNING`, `AFTERNOON`, `EVENING`, `LATE_NIGHT` 중 하나 |
| `ratios` | Object | 시간대별 비율 |
| `description` | String | 시간대 흐름을 설명하는 문장 |

`moodByTime.ratios`:

| 필드 | 타입 | 의미 |
|---|---|---|
| `morning` | Number | 오전 비율 |
| `afternoon` | Number | 오후 비율 |
| `evening` | Number | 저녁 비율 |
| `lateNight` | Number | 심야 비율 |

설명:

- `moodByTime` 은 공개 응답상 "감정/고민이 집중되는 시간대" 정보입니다.
- 내부 구현상 사용자 메시지 작성 시간으로 계산됩니다.

`conflict`:

| 필드 | 타입 | 의미 |
|---|---|---|
| `score` | Number | 갈등 강도 점수 |
| `description` | String | 갈등 해석 문장 |

추가 규칙:

- `score` 는 서버에서 `0 ~ 100` 범위로 정규화됩니다.

`behaviorPattern`:

| 필드 | 타입 | 의미 |
|---|---|---|
| `oneLineSummary` | String | 행동 패턴 한 줄 요약 |
| `triggerSituation` | String | 패턴이 주로 나타나는 상황 |
| `belief` | String | 그 상황에서 깔린 해석/믿음 |
| `responseType` | String | 주된 반응 방식 |

`solution`:

| 필드 | 타입 | 의미 |
|---|---|---|
| `title` | String | 솔루션 제목 |
| `content` | String | 솔루션 본문 |

### 성공 예시

```json
{
  "requestId": "req-3",
  "success": true,
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "weekStartDate": "2026-03-23",
    "weekEndDate": "2026-03-29",
    "status": "PUBLISHED",
    "generatedAt": "2026-03-30T00:02:14",
    "schemaVersion": "v1",
    "period": {
      "weekStartDate": "2026-03-23",
      "weekEndDate": "2026-03-29",
      "timezone": "Asia/Seoul"
    },
    "overview": {
      "title": "이번 주, 연락 공백이 감정의 불안을 키웠어요",
      "summary": "이번 주 상담에서는 연락 텀과 감정 확인 욕구가 반복적으로 나타났어요."
    },
    "topTopics": [
      {
        "keyword": "연락",
        "rank": 1,
        "weight": 0.44,
        "description": "상대의 반응 속도와 연락 패턴이 가장 큰 고민 주제로 나타났어요."
      }
    ],
    "moodByTime": {
      "dominantPeriod": "LATE_NIGHT",
      "ratios": {
        "morning": 0.1,
        "afternoon": 0.18,
        "evening": 0.29,
        "lateNight": 0.43
      },
      "description": "주로 늦은 밤 시간대에 고민이 커지는 흐름이 보여요."
    },
    "conflict": {
      "score": 72,
      "description": "관계의 긴장은 반복되고 있지만, 해결을 시도하려는 흐름도 함께 보여요."
    },
    "behaviorPattern": {
      "oneLineSummary": "상대의 반응이 늦어질 때 불안을 빠르게 확신으로 연결하는 패턴이 보여요.",
      "triggerSituation": "답장이 늦거나 애매한 표현을 들었을 때",
      "belief": "상대가 나를 덜 중요하게 여긴다고 느끼기 쉬움",
      "responseType": "확인 요구 또는 감정 거리두기"
    },
    "solution": {
      "title": "바로 할 수 있는 연애 솔루션",
      "content": "연락이 늦을 때 바로 의미를 확정하지 않고 30분만 해석을 미루어 보세요."
    }
  }
}
```

### 가능한 응답 케이스

| HTTP | code | 의미 |
|---|---|---|
| `200` | 없음 | 성공 |
| `400` | `40000` | 날짜 형식이 잘못됨 |
| `400` | `40001` | 토큰의 회원이 실제로 존재하지 않음 |
| `401` | `40100` | 인증 실패 |
| `404` | `40401` | 해당 주차 리포트가 없거나, 있어도 `PENDING`/`GENERATING`/`FAILED` 상태라 사용자에게 숨김 처리됨 |
| `500` | `50000` | 서버 내부 오류 |

---

## 3. GET /members/notifications/pending

기존 알림 조회 API입니다. 이번 기능으로 새 알림 타입이 추가되었습니다.

### 변경 포인트

- 새 타입 추가: `WEEKLY_ANALYSIS_REPORT_PUBLISHED`
- 같은 API에서 주간 리포트 발행 알림도 함께 조회됩니다.

### 요청

- Method: `GET`
- Path: `/members/notifications/pending`
- Request Body: 없음

### 응답 필드

최상위 `data`:

| 필드 | 타입 | 의미 |
|---|---|---|
| `size` | Number | 현재 응답에 담긴 알림 수 |
| `page` | Number \| null | 페이징 미사용이므로 항상 `null` |
| `list` | Array | pending 알림 목록 |
| `totalCount` | Number | 현재 pending 알림 수 |

`data.list[]`:

| 필드 | 타입 | 의미 |
|---|---|---|
| `id` | Number | 알림 ID. 읽음 처리 요청에 사용 |
| `type` | String | 알림 종류 |
| `state` | String | 현재 응답에서는 항상 `PENDING` |
| `payload` | Object \| null | 알림별 부가 데이터 |
| `createdAt` | ISO DateTime String | 알림 생성 시각 |

### 알림 타입별 해석

| `type` | `payload` 구조 | 프론트 의미 |
|---|---|---|
| `COUPLE_CONNECTED` | `null` | 커플 연결 완료 알림 |
| `COUPLE_DISCONNECTED` | `null` | 커플 연결 해제 알림 |
| `WEEKLY_ANALYSIS_REPORT_PUBLISHED` | `{ "weekStartDate": "yyyy-MM-dd", "weekEndDate": "yyyy-MM-dd" }` | 해당 주차 리포트가 발행되었음을 의미 |

`WEEKLY_ANALYSIS_REPORT_PUBLISHED.payload`:

| 필드 | 타입 | 의미 |
|---|---|---|
| `weekStartDate` | `yyyy-MM-dd` String | 리포트 상세 조회 키 값. `GET /reports/weekly/{weekStartDate}` 에 그대로 사용 |
| `weekEndDate` | `yyyy-MM-dd` String | 배너/카드 UI에 표시할 종료일 |

### 성공 예시

```json
{
  "requestId": "req-4",
  "success": true,
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "size": 1,
    "page": null,
    "list": [
      {
        "id": 10,
        "type": "WEEKLY_ANALYSIS_REPORT_PUBLISHED",
        "state": "PENDING",
        "payload": {
          "weekStartDate": "2026-03-23",
          "weekEndDate": "2026-03-29"
        },
        "createdAt": "2026-03-30T00:02:14"
      }
    ],
    "totalCount": 1
  }
}
```

### 빈 목록 예시

```json
{
  "requestId": "req-5",
  "success": true,
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "size": 0,
    "page": null,
    "list": [],
    "totalCount": 0
  }
}
```

### 응답/처리 규칙

- 주간 리포트 홈 배너 노출 여부는 이 API에서 `WEEKLY_ANALYSIS_REPORT_PUBLISHED` 존재 여부로 판단하면 됩니다.
- 주차가 다르면 알림도 별개로 여러 건 생성될 수 있습니다.
- 알림 읽음 여부와 리포트 조회 가능 여부는 별개입니다.
  - 알림을 읽음 처리해도 리포트는 계속 조회 가능합니다.
- 현재 서버 코드상 별도 `ORDER BY` 가 없어 알림 배열 순서는 보장하지 않는 것이 안전합니다.
  - 필요하면 프론트에서 `createdAt DESC` 정렬을 적용하는 편이 안전합니다.

### 가능한 응답 케이스

| HTTP | code | 의미 |
|---|---|---|
| `200` | 없음 | 성공 |
| `401` | `40100` | 인증 실패 |
| `400` | `40001` | 토큰의 회원이 실제로 존재하지 않음 |
| `500` | `50000` | 서버 내부 오류 |

---

## 4. PATCH /members/notifications/pending

기존 알림 읽음 처리 API입니다. 이번 기능으로 주간 리포트 알림 ID도 읽음 처리 대상이 됩니다.

### 요청

- Method: `PATCH`
- Path: `/members/notifications/pending`
- Content-Type: `application/json`

Request Body:

```json
{
  "pendingNotifications": [10, 11]
}
```

요청 필드:

| 필드 | 타입 | 의미 |
|---|---|---|
| `pendingNotifications` | Number Array | 읽음 처리할 알림 ID 목록 |

### 처리 규칙

- 전달한 알림들만 `READ` 로 바뀝니다.
- 주간 리포트 알림도 다른 알림과 동일하게 개별 읽음 처리 가능합니다.
- 요청한 ID 중 하나라도:
  - 존재하지 않거나
  - 내 알림이 아니면
  - 전체 요청이 `403` 으로 실패합니다.
- 즉, 부분 성공이 아니라 "전부 유효해야 성공" 계약입니다.

### 성공 응답 예시

```json
{
  "requestId": "req-6",
  "success": true,
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": null
}
```

### 가능한 응답 케이스

| HTTP | code | 의미 |
|---|---|---|
| `200` | 없음 | 성공 |
| `400` | `40000` | JSON 형식 오류 등 잘못된 요청 |
| `400` | `40001` | 토큰의 회원이 실제로 존재하지 않음 |
| `401` | `40100` | 인증 실패 |
| `403` | `40302` | 요청한 알림 중 하나 이상이 내 것이 아니거나 존재하지 않음 |
| `500` | `50000` | 서버 내부 오류 |

---

## 5. POST /admin/reports/weekly/{weekStartDate}/trigger

최근 5개 커밋에 포함된 관리자용 신규 API입니다. 일반 앱 프론트에서 직접 사용할 가능성은 낮지만, 이번 기능 범위 안에 포함되어 있어 참고용으로 정리합니다.

### 용도

- 특정 주차의 주간 리포트 예약 배치를 관리자 권한으로 재실행

### 요청

- Method: `POST`
- Path: `/admin/reports/weekly/{weekStartDate}/trigger`
- Path Variable:

| 이름 | 타입 | 의미 |
|---|---|---|
| `weekStartDate` | `yyyy-MM-dd` String | 대상 주차 시작일. 반드시 월요일 |

### 응답 필드

| 필드 | 타입 | 의미 |
|---|---|---|
| `weekStartDate` | `yyyy-MM-dd` String | 대상 주차 시작일 |
| `weekEndDate` | `yyyy-MM-dd` String | 대상 주차 종료일 |
| `candidateMemberCount` | Number | 검사 대상 회원 수 |
| `reservedCount` | Number | 신규 예약된 리포트 수 |
| `republishedFailedCount` | Number | 기존 `FAILED` 리포트를 재발행 대상으로 다시 큐잉한 수 |
| `skippedExistingCount` | Number | 기존 리포트가 이미 있어서 건너뛴 수 |
| `skippedIneligibleCount` | Number | 생성 조건 미달로 건너뛴 수 |

### 가능한 응답 케이스

| HTTP | code | 의미 |
|---|---|---|
| `200` | 없음 | 성공 |
| `400` | `40000` | `weekStartDate` 가 월요일이 아니거나 형식이 잘못됨 |
| `401` | `40100` | 인증 실패 |
| `403` | `40300` | 관리자 권한 없음 |
| `500` | `50000` | 서버 내부 오류 |

---

## 프론트 구현 가이드

### 권장 플로우

1. 홈 진입 시 `GET /members/notifications/pending` 호출
2. `type = WEEKLY_ANALYSIS_REPORT_PUBLISHED` 존재 여부로 주간 리포트 배너 노출
3. 배너 클릭 시 `payload.weekStartDate` 로 `GET /reports/weekly/{weekStartDate}` 호출
4. 확인 완료 후 해당 알림 ID만 `PATCH /members/notifications/pending` 로 읽음 처리

### 주의 포인트

- 리포트 상세는 발행 전 상태를 보여주지 않습니다.
  - 생성 중 화면이 필요해도 현재 공개 API만으로는 표현 불가
- 알림을 읽어도 리포트 자체는 계속 남아 있습니다.
- 리포트 상세 응답은 `data.content` 구조가 아니라 `data.overview`, `data.topTopics` 구조입니다.
- `schemaVersion` 은 현재 `v1` 이므로, 이후 버전 확장을 대비해 switch 지점을 만들어 두는 것이 좋습니다.
- 이번 fix 커밋들은 `createdAt`/audit 보정이라 프론트 JSON 계약은 변경하지 않습니다.
  - 따라서 앱 클라이언트는 새 필드 대응보다 "신규 엔드포인트 + 신규 알림 타입" 대응에 집중하면 됩니다.

---

## 이번 변경으로 프론트에서 실제로 해야 할 일

- 신규 화면/라우팅에서 `GET /reports/weekly` 목록 조회 연결
- 주간 리포트 상세 화면에서 `GET /reports/weekly/{weekStartDate}` 연결
- Notification 타입 분기문에 `WEEKLY_ANALYSIS_REPORT_PUBLISHED` 추가
- 주간 리포트 배너/카드 클릭 시 `payload.weekStartDate` 를 상세 조회 키로 사용
- 읽음 처리 시 주간 리포트 알림 ID도 기존 `PATCH /members/notifications/pending` 로 보내기
- 알림 배열 순서를 서버가 보장한다고 가정하지 않기
