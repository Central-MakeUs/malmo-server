# 주간 분석 리포트 Backend SPEC

## 1. 문서 목적

본 문서는 신규 기능 `분석 리포트`의 백엔드 요구사항과 구현 기준을 정의한다.

- 대상 기능: 매주 월요일 00시 전주 상담 데이터를 기반으로 주간 분석 리포트 자동 생성
- 대상 사용자: 인증된 회원
- 문서 범위: 배치 생성, 데이터 모델, API, 상태 정의, 예외 처리, 운영 고려사항

본 기능은 기존 `ChatRoom`의 상담 요약 기능과 목적이 다르므로, `채팅방 단위 요약`과 `주간 리포트 스냅샷`을 분리하여 설계한다.

## 2. 배경 및 목표

- 상담은 사건 단위 이해를 제공하고, 분석 리포트는 한 주 동안 누적된 상담을 바탕으로 `나의 패턴`을 이해시키는 기능이다.
- 리포트는 매주 재생성되는 가변 데이터가 아니라, 발행 시점의 입력과 결과가 고정된 `불변 스냅샷`이어야 한다.
- 사용자는 주차 단위로 과거 리포트를 조회할 수 있어야 한다.

## 3. 핵심 요구사항

### 3.1 기능 요구사항

1. 매주 월요일 `00:00`에 직전 1주 상담 데이터를 기준으로 주간 리포트를 자동 생성한다.
2. 리포트는 `주차별`로 조회할 수 있어야 한다.
3. 리포트는 생성 후 수정되지 않는다.
4. 전주 데이터 중 `MemberChatRoomMetadata` 기준으로 `level=1`의 마지막 `detailedLevel`까지 완료된 상담 채팅방이 1개 이상인 경우에만 리포트를 발행한다.
5. 리포트가 발행되면 미조회 알림을 통해 홈에서 도착 배너를 노출할 수 있어야 한다.

### 3.2 비기능 요구사항

- 주간 배치는 재실행되어도 중복 발행되지 않아야 한다.
- 다중 인스턴스 환경에서도 동일 사용자/주차 조합에 대해 1건만 생성되어야 한다.
- 리포트 조회는 생성 시점 결과를 그대로 반환해야 한다.

## 4. 용어 정의

### 4.1 주간 범위

- 타임존: `Asia/Seoul`
- 주간 범위는 `[월요일 00:00:00, 다음 월요일 00:00:00)`의 half-open interval 로 정의한다.
- API와 리포트 본문의 `weekEndDate`는 사용자가 인지하는 `일요일 날짜`를 의미한다.
- 내부 집계 기준 시각은 `weekEndAtExclusive = 다음 월요일 00:00:00` 으로 사용한다.
- 예시
  - 리포트 대상 기간: `2026-03-23 00:00:00 KST ~ 2026-03-30 00:00:00 KST`
  - 생성 시점: `2026-03-30 00:00:00 KST`

즉, `2026-03-29 23:59:59.999...`까지의 데이터는 해당 주간 리포트에 포함되고, `2026-03-30 00:00:00` 이후 데이터는 다음 주간 리포트에 포함된다.

### 4.2 1단계 완료 상담

현재 코드 기준으로 상담은 `ChatRoom.level`과 `detailedLevel`을 가지며, `level=1`의 마지막 충분성 조건을 통과하면 다음 단계로 승급된다.

주간 리포트에서 `분석 대상 채팅방의 존재 여부`는 `ChatRoom` 상태가 아니라 `MemberChatRoomMetadata`로 판단한다.

판정 기준:

- 해당 채팅방에 대해 `MemberChatRoomMetadata(level=1, detailedLevel=마지막)` 레코드가 존재해야 한다.
- 여기서 `마지막 detailedLevel`은 `DetailedPrompt(level=1)` 중 `isLastDetailedPrompt=true` 인 레코드의 `detailedLevel` 값으로 판단한다.
- `마지막 detailedLevel` 판정은 리포트 생성 시점의 `DetailedPrompt` 기준을 따른다. 운영 중 단계 수가 변경되더라도 기존에 생성된 리포트에는 영향이 없다.

즉, 주간 리포트 자격은 `level 1 마지막 메타데이터의 존재`로 판별하며, 별도의 `chat_room.level1_completed_at` 컬럼을 기준으로 삼지 않는다.

### 4.3 주간 리포트

회원 1명과 특정 주간 범위에 대해 1건만 존재하는 불변 스냅샷이다.

주의:

- 본 기능은 `커플 단위`가 아니라 `회원 단위`로 생성한다.
- 커플이 연결되어 있어도 각 회원의 상담 데이터 기준으로 별도 생성한다.
- 커플 해제/재연결 여부는 리포트 생성에 영향을 주지 않는다. 해당 회원의 상담 데이터가 존재하면 커플 상태와 무관하게 리포트를 생성한다.

## 5. 범위

### 5.1 In Scope

- 주간 리포트 생성 스케줄러
- 리포트 상태 저장
- 리포트 상세/목록 조회 API
- 리포트 도착 알림 생성
- 주간 집계용 원천 데이터 추출
- 리포트 JSON 스키마 정의

### 5.2 Out of Scope

- 리포트 UI 구현
- 배너 문구/디자인 상세
- 프롬프트 문구의 세부 카피라이팅
- 리포트 수정 기능
- 기존 채팅방 요약 API 대체

## 6. 아키텍처 결정

### 6.1 기존 `ChatRoom` 요약과 분리

기존 `ChatRoom.totalSummary`, `MemberChatRoomMetadata`는 채팅방/단계 단위 데이터이며, 주간 리포트는 `회원 + 주차` 단위 데이터다.

따라서 다음 원칙을 따른다.

- `ChatRoom` 엔티티에 주간 리포트 본문을 직접 저장하지 않는다.
- 신규 집계 결과는 별도 테이블 `weekly_analysis_report`에 저장한다.
- 기존 채팅 요약 데이터는 리포트 생성을 위한 입력 데이터로만 활용한다.
- 기존 `ChatRoomState.COMPLETED` 의미를 주간 리포트 상태로 재사용하지 않는다.

### 6.2 스냅샷 저장 방식

리포트는 프론트 렌더링 친화적인 `구조화 JSON`으로 저장한다.

- 장점
  - UI 섹션별 독립 렌더링 가능
  - 문구 일부 변경 없이도 숫자/리스트/카드형 UI 확장 가능
  - `schemaVersion` 필드로 하위 호환 관리 가능

## 7. 데이터 소스 정의

### 7.1 원천 데이터

- `chat_room`
- `chat_message`
- `member_chat_room_metadata`
- `member`
- `prompt`
- `detailed_prompt`

### 7.2 집계 대상 데이터

주간 리포트 생성 시 입력 데이터는 아래 규칙을 따른다.

1. 대상 회원의 채팅방 중 `DELETED`가 아닌 채팅방만 사용한다.
2. `분석 대상 채팅방`은 아래 두 조건을 모두 만족하는 채팅방으로 정의한다.
   - 해당 채팅방에 `MemberChatRoomMetadata(level=1, detailedLevel=마지막)` 이 존재한다.
   - 아래 둘 중 하나를 만족한다.
     - `MemberChatRoomMetadata(level=1, detailedLevel=마지막)` 의 `createdAt` 이 현재 주차 범위 안에 있다.
     - `MemberChatRoomMetadata(level>=2)` 중 하나 이상의 `createdAt` 이 현재 주차 범위 안에 있다.
3. 즉, `level 1` 완료는 이전 주차에 발생했더라도, 같은 채팅방의 `level 2 이상 metadata` 가 이번 주차에 생성되었다면 이번 주 리포트 분석 대상에 포함한다.
4. 분석 대상 채팅방의 리포트 입력에는 해당 채팅방의 **모든 `MemberChatRoomMetadata`** 를 포함한다. 이전 주차에 생성된 level 1 metadata도 전체 맥락 파악을 위해 포함한다.
5. 주간 범위 내 원문 메시지는 리포트 본문 생성을 위한 기본 입력으로 사용하지 않는다.
6. 원문 메시지는 아래 조건 중 하나를 만족할 때만 fallback 입력으로 제한적으로 사용한다.
   - 해당 채팅방의 metadata 수가 너무 적어 리포트 섹션 필수값 생성이 어려운 경우
   - metadata만으로는 시간 순서나 반복 트리거 판단이 부족하다고 검증된 경우
7. fallback으로 원문 메시지를 사용할 때에도 `주간 종료 시각 이전 메시지`만 잘라서 사용한다.
8. 시간대 집계는 `USER` 메시지만 사용한다.
9. 서술형 분석용 입력 우선순위는 아래와 같다.
   - 1순위: 분석 대상 채팅방의 `MemberChatRoomMetadata`
   - 2순위: 제한적으로 발췌한 주간 범위 내 원문 메시지

### 7.2.1 토큰 비용 최적화 원칙

- 리포트 분석용 LLM 입력은 `metadata-first` 원칙을 따른다.
- 채팅방별 전체 메시지 전문을 그대로 프롬프트에 넣지 않는다.
- 주간 리포트 입력은 `채팅방 단위 metadata 묶음`을 먼저 만들고, 이를 회원 기준으로 합쳐 1회 요청한다.
- 원문 메시지 fallback은 예외 경로이며, 기본 경로가 되어서는 안 된다.
- fallback 원문은 채팅방별 소량 발췌만 허용한다.

권장 제한:

- 채팅방별 metadata는 해당 주차 관련 레코드만 사용
- 채팅방별 원문 발췌는 최대 6개 메시지
- 회원 1명의 주간 리포트 전체 원문 발췌 합계는 최대 20개 메시지

권장 발췌 기준:

- 가장 최근 메시지
- 감정 강도가 높은 메시지
- 반복 트리거를 드러내는 메시지
- metadata 요약과 직접 연결되는 메시지

### 7.3 발행 자격

해당 회원의 대상 주간 데이터에 대해 아래 두 조건을 모두 만족해야 한다.

1. 주간 범위 내 사용자 메시지가 1건 이상 존재한다.
2. `분석 대상 채팅방`이 1개 이상 존재한다.

발행 자격이 없는 후보 회원은 `weekly_analysis_report` row를 생성하지 않는다.

## 8. 리포트 상태 모델

`weekly_analysis_report.status`

- `PENDING`: 생성 대상 확정, 아직 생성 전
- `GENERATING`: LLM 생성 중
- `PUBLISHED`: 생성 완료, 조회 가능
- `FAILED`: 생성 실패

정책:

- 동일 회원/동일 `week_start_date`에는 하나의 최종 상태만 존재한다.
- `PUBLISHED` 이후에는 본문 수정 금지
- `FAILED`는 재처리 가능하되, 성공 시에도 동일 row를 갱신하고 새 row를 만들지 않는다.
- 발행 자격 미달인 회원은 row 자체를 생성하지 않으므로 `SKIPPED` 상태는 사용하지 않는다.

## 9. DB 스키마 제안

### 9.1 `prompt` 변경

```sql
ALTER TABLE prompt
ADD COLUMN is_for_weekly_report BOOLEAN NOT NULL DEFAULT FALSE;
```

용도:

- 주간 분석 리포트 전용 프롬프트를 `Prompt` 테이블에서 1건 조회하기 위한 식별자
- `is_for_weekly_report=true`인 row는 `level`과 무관하게 단 1건만 존재한다.
- `주로 상담하는 주제`, `충돌 점수`, `자주 나타나는 행동 패턴`, `바로 할 수 있는 연애 솔루션`을 한 번의 프롬프트 호출로 생성하기 위한 전용 prompt 지정

### 9.2 `weekly_analysis_report` 신설

```sql
CREATE TABLE weekly_analysis_report (
    weekly_analysis_report_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    week_start_date DATE NOT NULL,
    week_end_date DATE NOT NULL,
    status VARCHAR(64) NOT NULL,
    source_chat_room_count INT NOT NULL DEFAULT 0,
    eligible_chat_room_count INT NOT NULL DEFAULT 0,
    source_user_message_count INT NOT NULL DEFAULT 0,
    schema_version VARCHAR(32) NULL,
    timezone VARCHAR(64) NULL,
    overview_title TEXT NULL,
    overview_summary TEXT NULL,
    mood_dominant_period VARCHAR(32) NULL,
    mood_ratio_morning DOUBLE NULL,
    mood_ratio_afternoon DOUBLE NULL,
    mood_ratio_evening DOUBLE NULL,
    mood_ratio_late_night DOUBLE NULL,
    mood_description TEXT NULL,
    conflict_score INT NULL,
    conflict_description TEXT NULL,
    behavior_pattern_one_line_summary TEXT NULL,
    behavior_pattern_trigger_situation TEXT NULL,
    behavior_pattern_belief TEXT NULL,
    behavior_pattern_response_type TEXT NULL,
    solution_title TEXT NULL,
    solution_content TEXT NULL,
    generated_at DATETIME NULL,
    failed_reason VARCHAR(255) NULL,
    created_at DATETIME NOT NULL,
    modified_at DATETIME NOT NULL,
    deleted_at DATETIME NULL,
    CONSTRAINT uq_weekly_analysis_report_member_week UNIQUE (member_id, week_start_date)
);
```

```sql
CREATE TABLE weekly_analysis_report_top_topic (
    weekly_analysis_report_id BIGINT NOT NULL,
    topic_rank INT NOT NULL,
    keyword TEXT NOT NULL,
    weight DOUBLE NOT NULL,
    description TEXT NOT NULL,
    FOREIGN KEY (weekly_analysis_report_id) REFERENCES weekly_analysis_report (weekly_analysis_report_id)
);
```

설명:

- `member_id + week_start_date` 를 유니크 키로 사용해 멱등성을 보장한다.
- 단일 섹션은 `weekly_analysis_report` 컬럼에 저장한다.
- 반복 섹션인 `topTopics`는 `weekly_analysis_report_top_topic` child table에 저장한다.
- 민감한 원문 대화는 별도 보관하지 않는다.
- `week_end_date`는 해당 주간의 일요일 날짜를 저장하며, 처리된 기간을 명시적으로 표기하기 위해 유지한다.

## 10. 리포트 본문 스키마

리포트 본문은 아래 구조를 기본으로 하며, API 응답에서는 `payload` 래퍼 없이 동일 필드명을 직접 반환한다.

```json
{
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
      "MORNING": 0.10,
      "AFTERNOON": 0.18,
      "EVENING": 0.29,
      "LATE_NIGHT": 0.43
    },
    "description": "주로 밤 시간대에 고민이 커지는 흐름이 보여요."
  },
  "conflict": {
    "score": 72,
    "description": "관계의 긴장은 반복되고 있지만, 해결을 시도하려는 흐름도 함께 보여요."
  },
  "behaviorPattern": {
    "oneLineSummary": "상대의 반응이 늦어질 때 불안을 빠르게 확신으로 연결하는 패턴이 보여요.",
    "triggerSituation": "상대의 연락이 예상보다 늦어질 때",
    "belief": "답이 늦으면 마음이 식은 것일 수 있다는 믿음",
    "responseType": "확인을 서두르거나 감정을 앞당겨 표현함"
  },
  "solution": {
    "title": "바로 할 수 있는 연애 솔루션",
    "content": "연락이 늦을 때 바로 의미를 확정하지 않고 30분만 해석을 미루어 보세요. 확인을 요청할 때는 사실과 감정을 분리해서 말하는 연습을 해보면 좋겠어요."
  }
}
```

정책:

- 서버는 markdown/html이 아닌 구조화 JSON을 저장/응답한다.
- 각 섹션은 nullable 하지 않도록 생성 프롬프트와 후처리 검증을 둔다.
- 일부 섹션 생성 실패 시 전체 발행 대신 `FAILED` 처리한다.
- `topTopics`는 최대 3개로 제한한다. LLM 프롬프트에서 개수를 명시하고, 후처리에서 3개 초과 시 상위 3개만 유지한다.
- `conflict.score`는 0~100 사이의 정수로, LLM이 생성한다. 프롬프트에서 범위를 명시하고, 후처리에서 범위를 검증한다.
- `solution.content`는 평문 텍스트로, 리스트가 아닌 서술형으로 제공한다.

## 11. 리포트 생성 규칙

### 11.1 시간대별 기분

- 기준 데이터: 주간 범위 내 `USER` 메시지의 `createdAt`
- 구간
  - `MORNING`: 06:00 이상 12:00 미만
  - `AFTERNOON`: 12:00 이상 18:00 미만
  - `EVENING`: 18:00 이상 24:00 미만
  - `LATE_NIGHT`: 00:00 이상 06:00 미만
- 계산 방식
  - 각 구간 메시지 수 집계
  - 전체 합 기준 비율 계산
- 동률 우선순위
  - `LATE_NIGHT > EVENING > MORNING > AFTERNOON`

### 11.2 LLM 분석 섹션 생성 방식

아래 4개 섹션은 각각 별도 호출하지 않고, `주간 리포트 분석용 Prompt` 1회 호출로 한 번에 생성한다.

- `주로 상담하는 주제` (`topTopics`)
- `충돌 점수` (`conflict`)
- `자주 나타나는 행동 패턴` (`behaviorPattern`)
- `바로 할 수 있는 연애 솔루션` (`solution`)

입력:

- 회원 기준으로 집계한 분석 대상 채팅방의 `MemberChatRoomMetadata` (해당 채팅방의 모든 metadata 포함)
- 필요한 경우에만 추가한 소량의 원문 메시지 발췌
- 사용자 기본 프로필 정보

입력 구성 방식:

1. 각 분석 대상 채팅방에 대해 해당 채팅방의 모든 `MemberChatRoomMetadata` 를 수집한다.
2. metadata를 채팅방별 요약 블록으로 정규화한다.
3. 회원 단위로 채팅방별 요약 블록을 합쳐 단일 프롬프트 입력을 만든다.
4. metadata만으로 필수 섹션 생성이 어렵다고 판단될 때만 원문 메시지 발췌를 덧붙인다.

채팅방별 요약 블록 예시:

```text
[채팅방 123]
- level1-final-metadata: 상대의 연락 공백에서 불안이 커지고 확인 욕구가 반복됨
- level2-metadata: 같은 상황에서 빠르게 결론을 내리는 패턴이 관찰됨
- createdAt-range: 2026-03-25 ~ 2026-03-28
```

프롬프트 조회:

- `Prompt` 테이블에서 `is_for_weekly_report=true` 인 레코드 1건 조회 (level과 무관)
- 해당 Prompt의 `content`를 리포트 분석 전용 지시문으로 사용
- 리포트 생성 시 위 4개 섹션은 동일 Prompt 응답에서 구조화 JSON으로 함께 반환되어야 한다

토큰 최적화 정책:

- metadata만으로 생성 가능한 경우 raw message는 프롬프트에 포함하지 않는다.
- 원문 메시지를 포함하더라도 채팅방 단위 상한과 회원 단위 상한을 넘지 않는다.
- 토큰 초과가 예상되면 오래된 raw message보다 metadata를 우선 보존한다.
- 입력이 너무 길면 raw message를 먼저 줄이고, 그 다음 metadata를 최근 주차 관련 순으로 줄인다.

## 12. 배치 처리 설계

### 12.1 처리 방식

월요일 00시에 전체 회원 리포트를 동기 생성하지 않고, 아래 2단계로 분리한다.

1. `WeeklyAnalysisReportScheduler`
   - 대상 주간 계산
   - 후보 회원 조회
   - 발행 자격 검증 후, 자격을 충족하는 회원에 대해서만 `weekly_analysis_report` row 생성 (`PENDING`)
   - 생성된 row에 대해 Redis Stream 이벤트 발행
2. `WeeklyAnalysisReportGenerator`
   - Redis Stream 이벤트를 수신하여 실시간 처리
   - `MemberChatRoomMetadata` 기준으로 분석 대상 채팅방 선별
   - metadata 중심 데이터 집계
   - 필요 시에만 제한적 raw message fallback 집계
   - 주간 리포트 분석 Prompt 1회 요청
   - 결과 검증 후 `PUBLISHED` 저장

동시 처리 제어:

- Redis Stream consumer group을 활용하여 동시 병렬 처리 수를 제한한다.
- 다수의 이벤트가 동시에 발행되더라도 과도한 LLM API 호출이 발생하지 않도록 조절한다.

### 12.2 후보 회원 선정

후보 회원은 아래 조건 중 하나를 만족하는 회원으로 한정한다.

- 대상 주간에 `USER` 메시지가 1건 이상 있는 회원
- 대상 주간에 `MemberChatRoomMetadata(level=1, 마지막)` 또는 `MemberChatRoomMetadata(level>=2)` 가 생성된 회원

이유:

- 완전 무활동 회원까지 배치 대상에 포함할 필요가 없다.

후보 회원 중 발행 자격(7.3)을 충족하는 회원에 대해서만 row를 생성한다. 자격 미달 회원은 row를 생성하지 않는다.

### 12.3 멱등성

- `member_id + week_start_date` 유니크 키로 row 중복 생성 방지
- 스케줄러 재실행 시 이미 존재하는 row는 재삽입하지 않는다.
- `PUBLISHED` row는 덮어쓰지 않는다.

### 12.4 분산 환경 고려

운영 환경이 다중 인스턴스가 될 수 있으므로 아래 중 하나를 반드시 적용한다.

- Redis 분산 락
- DB 기반 배치 락
- 유니크 키 + 상태 전이 조건 기반 낙관적 제어

권장안은 `Redis` 사용이다. 현재 프로젝트는 Redis 의존성과 스케줄링 패턴을 이미 사용 중이므로 적용 비용이 낮다.

## 13. API SPEC

응답 envelope은 기존 `BaseResponse` 규약을 따른다.

### 13.1 주간 리포트 상세 조회

`GET /reports/weekly/{weekStartDate}`

예시:

- `GET /reports/weekly/2026-03-23`

응답:

- `PUBLISHED`: 구조화된 리포트 필드 전체 반환
- row가 존재하지 않음: 404

응답 예시 (PUBLISHED):

```json
{
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
  "overview": { ... },
  "topTopics": [ ... ],
  "moodByTime": { ... },
  "conflict": { ... },
  "behaviorPattern": { ... },
  "solution": { ... }
}
```

정책:

- `PENDING`, `GENERATING` 상태의 row는 사용자에게 노출하지 않는다 (존재하지 않는 것과 동일하게 처리).
- `FAILED` 상태 역시 사용자에게 노출하지 않는다.

## 14. 알림 정책

리포트가 `PUBLISHED` 되면 기존 알림 체계를 재사용해 미조회 알림을 저장한다.

### 14.1 NotificationType 확장

`NotificationType`

- 기존: `COUPLE_CONNECTED`, `COUPLE_DISCONNECTED`
- 추가: `WEEKLY_ANALYSIS_REPORT_PUBLISHED`

### 14.2 payload 예시

```json
{
  "weekStartDate": "2026-03-23",
  "weekEndDate": "2026-03-29"
}
```

정책:

- 홈의 주간 리포트 배너 노출 여부는 `GET /members/notifications/pending` 에서 `WEEKLY_ANALYSIS_REPORT_PUBLISHED` 타입 존재 여부로 판단한다.
- 같은 회원에게 여러 주차의 주간 리포트가 발행되면 주차별로 알림을 각각 생성한다.
- 알림 읽음 여부와 리포트 자체 조회 가능 여부는 분리

## 15. 실패 및 예외 처리

### 15.1 생성 실패

- LLM 응답 실패
- JSON 파싱 실패
- 필수 필드 누락
- 입력 데이터 집계 오류

처리:

- 상태를 `FAILED`로 기록
- `failed_reason` 저장
- 재시도 정책 적용

### 15.2 재시도 정책

- 생성 실패 시 Redis Stream으로 이벤트를 최대 2회 재발행한다.
- 최종 실패 시 (총 3회 시도 후) 운영자 확인 대상으로 남긴다.

### 15.3 관리자 수동 재처리

운영 편의를 위해 내부 관리자용 API 또는 배치 명령을 제공하는 것을 권장한다.

- `POST /admin/reports/weekly/{weekStartDate}/retry`

본 API는 사용자 공개 API 범위에는 포함하지 않는다.

## 16. 보안 및 개인정보

- 리포트 본문에는 원문 전체 대화를 저장하지 않는다.
- 프롬프트 입력 로그가 필요하다면 별도 보안 저장소 또는 단기 보관 정책을 둔다.
- 사용자 간 리포트 접근은 기존 member ownership 검증과 동일 수준으로 차단한다.
- 삭제된 채팅방의 메시지는 신규 리포트 생성 입력에서 제외한다.

## 17. 성능 및 운영 고려사항

- 월요일 00시에 부하가 집중될 수 있으므로 `생성 예약`과 `실제 생성`을 분리한다.
- 상세 조회는 `member_id + week_start_date` 인덱스가 필요하다.
- 후보 회원 조회를 위해 `chat_message.created_at`, `chat_message.sender_type`, `chat_room.member_id` 인덱스를 점검한다.
- 리포트 생성 소요 시간, 실패율, 주간 발행 수를 메트릭으로 수집한다.
- 토큰 비용 절감을 위해 `metadata-only 생성 비율`과 `raw fallback 사용 비율`을 함께 추적한다.

권장 메트릭:

- `weekly_report_generation_requested_count`
- `weekly_report_generation_success_count`
- `weekly_report_generation_failed_count`
- `weekly_report_generation_duration_ms`
- `weekly_report_metadata_only_count`
- `weekly_report_raw_fallback_count`

## 18. 테스트 전략

### 18.1 단위 테스트

- 주간 범위 계산
- 시간대 비율 계산
- 동률 우선순위 계산
- 발행 자격 판정
- 상태 전이 검증

### 18.2 통합 테스트

- 월요일 00시 스케줄러 실행 시 자격 충족 회원에 대해서만 row 생성
- 동일 주차 중복 실행 시 row 1건 유지
- `MemberChatRoomMetadata(level=1, 마지막)` 존재/부재 분기 검증
- `level 1 metadata는 이전 주차, level 2+ metadata는 현재 주차` 인 장기 상담 포함 검증
- 장기 상담 포함 시 이전 주차의 level 1 metadata도 입력에 포함되는지 검증
- 단일 주간 리포트 Prompt 1회 호출 검증
- metadata만으로 리포트 생성 가능한 경우 raw message를 조회하지 않는지 검증
- raw fallback 사용 시 메시지 상한이 지켜지는지 검증
- 상세 조회 권한 검증
- 홈 상태 조회 검증
- Redis Stream 이벤트 발행/소비 검증
- 실패 시 최대 2회 재발행 검증

### 18.3 회귀 테스트 포인트

- 기존 `ChatRoom` 요약 조회 기능 영향 없음
- 기존 알림 조회 API 호환성 유지
- 삭제된 채팅방이 목록/집계에 섞이지 않음

## 19. 구현 권장 순서

1. `prompt.is_for_weekly_report` 추가 및 조회 로직 정의
2. `weekly_analysis_report` 테이블 및 엔티티 추가
3. 주간 범위 계산기/상태 enum/도메인 모델 추가
4. `MemberChatRoomMetadata(level=1, 마지막)` 및 `level>=2` 기반 분석 대상 채팅방 조회 쿼리 추가
5. metadata 기반 입력 정규화기와 raw fallback 제한 정책 추가
6. 후보 회원 조회, 발행 자격 검증 및 스케줄러 추가
7. Redis Stream 이벤트 발행/소비 구조 추가
8. 생성기와 단일 Prompt 기반 LLM 응답 스키마 검증 추가
9. 상세/홈 상태 API 추가
10. 알림 타입 확장
11. 운영용 재처리 도구 추가

## 20. 최종 결정사항 요약

- 주간 분석 리포트는 `회원 + 주차` 단위의 별도 스냅샷으로 저장한다.
- 기준 타임존은 `Asia/Seoul`, 주간 범위는 `[월요일 00:00, 다음 월요일 00:00)` 이다.
- 리포트 생성 자격은 `주간 메시지 존재 + MemberChatRoomMetadata 기준 분석 대상 채팅방 1개 이상`이다.
- 발행 자격 미달 회원은 row를 생성하지 않는다.
- `주로 상담하는 주제`, `충돌 점수`, `자주 나타나는 행동 패턴`, `바로 할 수 있는 연애 솔루션`은 Prompt 테이블의 전용 프롬프트 1회 호출로 함께 생성한다.
- `topTopics`는 최대 3개, `conflict.score`는 0~100 정수, `solution`은 평문 텍스트로 제공한다.
- 리포트 입력은 `MemberChatRoomMetadata` 중심으로 구성하고, 원문 메시지는 예외적으로만 소량 발췌한다.
- 분석 대상 채팅방의 모든 metadata를 입력에 포함한다 (이전 주차 생성분 포함).
- 리포트는 생성 후 수정하지 않는다.
- 홈 노출을 위해 `리포트 상태`와 `리포트 도착 알림`을 함께 관리한다.
- Generator는 Redis Stream 이벤트로 트리거되며, 실패 시 최대 2회 재발행한다.
