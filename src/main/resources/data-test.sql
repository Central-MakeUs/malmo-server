INSERT INTO terms_entity (title, content, version, is_required, terms_type)
VALUES ('만 14세 이상입니다.',null,1.0,true,'AGE_VERIFICATION'),
       ('[필수] 서비스 이용약관', '본 약관은 말모 서비스 이용에 관한 내용을 담고 있습니다...',1.0,true,'SERVICE_USAGE'),
       ('[필수] 개인정보 처리방침','당신의 개인 정보를 가져가겠습니다. 감사합니다.',1.0,true,'PRIVACY_POLICY'),
       ('[선택] 마케팅 정보 수신 동의','마케팅을 많이 해서 돈을 많이 벌 거에요.',1.0,false,'MARKETING');

INSERT INTO question_entity (title, content, level)
VALUES ('지금 연애를 시작하게 된 계기는 무엇인가요?', '지금 연애를 시작하게 된 계기는 무엇인가요?', 1),
       ('내가 가장 사랑받는다고 느끼는 순간은 언제였나요?', '내가 가장 사랑받는다고 느끼는 순간은 언제였나요?', 2),
       ('사랑을 표현하는 데 있어서 나만의 방식이 있다면?', '사랑을 표현하는 데 있어서 나만의 방식이 있다면?', 3),
       ('연애 중 가장 고마웠던 순간은 어떤 상황이었나요?', '연애 중 가장 고마웠던 순간은 어떤 상황이었나요?', 4),
       ('연인이 서운한 마음을 표현할 때, 나는 어떤 마음이 드나요?', '연인이 서운한 마음을 표현할 때, 나는 어떤 마음이 드나요?', 5);

INSERT INTO prompt_entity (level, content, is_for_answer_metadata, is_for_completed_response, is_for_guideline, is_for_summary, is_for_system, is_for_total_summary, is_for_title_generation, is_for_weekly_report)
VALUES
    (-4, '주간 리포트용 프롬프트', false, false, false, false, false, false, false, true),
    (-3, '요약용 프롬프트', true, false, false, true, false, true, false, false),
    (-2, '시스템 프롬프트' , false, false, false, false, true, false, false, false),
    (-1, '중간 요약용 프롬프트', true, false, false, true, false, false, false, false),
    (0, '다음 대화 내용을 바탕으로 20자 이내의 간결한 제목을 생성해주세요.', false, false, false, false, false, false, true, false),
    (1, '1단계 프롬프트', false, false, true, false, false, false, false, false),
    (2, '2단계 프롬프트', false, false, true, false, false, false, false, false),
    (3, '3단계 프롬프트', false, false, true, false, false, false, false, false);
