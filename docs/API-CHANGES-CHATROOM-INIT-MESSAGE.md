# API 변경 사항 - 채팅방 초기 메시지 개선

## 개요

채팅방 생성 시 표시되는 초기 메시지가 다음과 같이 개선되었습니다:

1. **초기 메시지 2개로 분리**: 기존 1개의 메시지에서 2개의 별도 메시지로 분리
2. **RelationshipStatus 기반 맞춤 메시지**: 사용자의 연애 상태에 따라 두 번째 메시지 내용 변경
3. **애착유형 프롬프트 시스템 메시지**: 애착유형 테스트를 하지 않은 사용자에게 안내 메시지 표시

---

## 1. 초기 메시지 분리

### 변경 전

채팅방 생성 시 1개의 메시지만 저장:
```
"{nickname}아 안녕! 나는 연애 고민 상담사 모모야..."
```

### 변경 후

채팅방 생성 시 2개의 메시지가 순차적으로 저장:

| 순서 | SenderType | 내용 |
|------|------------|------|
| 1 | `ASSISTANT` | `"{nickname}아 안녕!"` 또는 `"{nickname}야 안녕!"` (조사 자동 처리) |
| 2 | `ASSISTANT` | 연애 상태에 따른 맞춤 메시지 (아래 참조) |

---

## 2. RelationshipStatus 기반 맞춤 메시지

두 번째 초기 메시지는 사용자의 `RelationshipStatus`에 따라 다른 내용을 표시합니다.

### 공통 Prefix
모든 메시지는 다음 내용으로 시작합니다:
```
나는 연애 고민 상담사 모모야.
나와의 대화를 마무리하고 싶다면 종료하기 버튼을 눌러줘! 대화 종료 후에는 대화 요약 리포트를 보여줄게.
```

### RelationshipStatus별 메시지

| RelationshipStatus | 추가되는 메시지 |
|--------------------|----------------|
| `SEEING_SOMEONE` | "오늘은 어떤 고민 때문에 나를 찾아왔어? 마음에 두고 있는 상대와 있었던 상황을 이야기해 주면 내가 같이 고민해볼게!" |
| `IN_RELATIONSHIP` | "오늘은 어떤 고민 때문에 나를 찾아왔어? 먼저 연인과 있었던 갈등 상황을 이야기해 주면 내가 같이 고민해볼게!" |
| `BREAKUP` | "오늘은 어떤 고민 때문에 나를 찾아왔어? 이별 전후로 마음에 남아 있는 상황을 이야기해 주면 내가 같이 고민해볼게!" |
| `null` (미설정) | `SEEING_SOMEONE`과 동일한 메시지 |

### 예시 - IN_RELATIONSHIP 사용자

```json
{
  "list": [
    {
      "messageId": 1,
      "senderType": "ASSISTANT",
      "content": "민수야 안녕!",
      "createdAt": "2026-01-30T10:00:00"
    },
    {
      "messageId": 2,
      "senderType": "ASSISTANT",
      "content": "나는 연애 고민 상담사 모모야.\n나와의 대화를 마무리하고 싶다면 종료하기 버튼을 눌러줘! 대화 종료 후에는 대화 요약 리포트를 보여줄게.\n오늘은 어떤 고민 때문에 나를 찾아왔어? 먼저 연인과 있었던 갈등 상황을 이야기해 주면 내가 같이 고민해볼게!",
      "createdAt": "2026-01-30T10:00:01"
    }
  ]
}
```

---

## 3. 애착유형 프롬프트 시스템 메시지

### 조건
다음 조건을 **모두** 만족할 때 시스템 메시지가 표시됩니다:
- 사용자의 `loveTypeCategory`가 `null` (애착유형 테스트 미완료)
- 해당 채팅방에 `USER` 타입 메시지가 없음 (첫 메시지 전송 전)

### 동작 방식

#### GET /chatrooms/{chatRoomId}/messages 조회 시
- 조건 충족 시 마지막에 `SYSTEM` 메시지가 **동적으로** 추가됨
- DB에 저장되지 않음 (매 조회 시 동적 추가)
- `messageId`가 `null`로 반환됨

#### POST /chatrooms/{chatRoomId}/messages 첫 메시지 전송 시
- 조건 충족 시 사용자 메시지 전송 **직전**에 `SYSTEM` 메시지가 DB에 저장됨
- 이후 조회 시에는 저장된 `SYSTEM` 메시지가 반환됨

### 시스템 메시지 내용
```
잠깐! 애착유형 테스트를 하면, 더 정확한 상담이 가능해! 그대로 진행하면 바로 상담해줄게
```

### 예시 - 애착유형 미설정 사용자, 첫 메시지 전송 전

```json
{
  "totalCount": 3,
  "list": [
    {
      "messageId": 1,
      "senderType": "ASSISTANT",
      "content": "민수야 안녕!",
      "createdAt": "2026-01-30T10:00:00"
    },
    {
      "messageId": 2,
      "senderType": "ASSISTANT",
      "content": "나는 연애 고민 상담사 모모야...(생략)",
      "createdAt": "2026-01-30T10:00:01"
    },
    {
      "messageId": null,
      "senderType": "SYSTEM",
      "content": "잠깐! 애착유형 테스트를 하면, 더 정확한 상담이 가능해! 그대로 진행하면 바로 상담해줄게",
      "createdAt": "2026-01-30T10:00:02"
    }
  ]
}
```

> **Note:** `messageId`가 `null`인 경우 해당 메시지는 동적으로 추가된 것입니다.

---

## 클라이언트 구현 가이드

### 메시지 렌더링
1. `senderType`에 따라 메시지 스타일 구분:
   - `ASSISTANT`: AI 상담사 메시지 (채팅 버블 왼쪽)
   - `USER`: 사용자 메시지 (채팅 버블 오른쪽)
   - `SYSTEM`: 시스템 안내 메시지 (중앙 정렬, 다른 스타일)

2. `SYSTEM` 메시지 처리:
   - 애착유형 테스트 유도 문구이므로, 탭 시 애착유형 테스트 화면으로 이동하는 것을 권장

3. `messageId`가 `null`인 경우:
   - 해당 메시지는 서버에 저장되지 않은 동적 메시지
   - 북마크 등 messageId가 필요한 기능은 비활성화

### 온보딩 시 RelationshipStatus 설정 권장
- 사용자가 `RelationshipStatus`를 설정하지 않은 경우 `SEEING_SOMEONE` 기본 메시지 표시
- 더 개인화된 상담 경험을 위해 온보딩 시 연애 상태 설정을 권장

---

## 변경된 파일 목록

### Constants
- `GlobalConstants.java`
  - `INIT_CHAT_MESSAGE_FIRST`: `" 안녕!"` (조사와 결합)
  - `INIT_CHAT_MESSAGE_SECOND_PREFIX`: 공통 prefix
  - `INIT_CHAT_MESSAGE_SECOND_SEEING_SOMEONE`: 썸 상태 메시지
  - `INIT_CHAT_MESSAGE_SECOND_IN_RELATIONSHIP`: 연애 중 메시지
  - `INIT_CHAT_MESSAGE_SECOND_BREAKUP`: 이별 후 메시지
  - `ATTACHMENT_TYPE_PROMPT_MESSAGE`: 애착유형 안내 메시지

### Domain Layer
- `ChatMessage.java` - `createSystemTextMessage()` 팩토리 메서드 추가
- `ChatRoomDomainService.java` - `createSystemMessage()`, `createAiMessage()` 오버로드 추가

### Application Layer
- `ChatRoomManagementService.java` - 초기 메시지 2개 생성, `getSecondMessageByRelationshipStatus()` 메서드 추가
- `ChatRoomService.java` - GET 조회 시 동적 시스템 메시지 추가 로직
- `ChatService.java` - POST 전송 시 시스템 메시지 저장 로직
- `ChatRoomQueryHelper.java` - `hasUserMessages()` 메서드 추가
- `LoadMessagesPort.java` - `hasUserMessages()` 인터페이스 추가

### Adaptor Layer
- `ChatMessageRepository.java` - `existsByChatRoomIdAndSenderType()` 쿼리 추가
- `ChatRoomPersistenceAdapter.java` - 포트 구현

### Tests
- `ChatRoomManagementServiceTest.java` - 5개 단위 테스트 추가
- `ChatRoomIntegrationTest.java` - 통합 테스트 케이스 추가
