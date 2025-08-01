package makeus.cmc.malmo.application.service.helper.question;

import lombok.RequiredArgsConstructor;
import makeus.cmc.malmo.application.port.out.SaveCoupleQuestionPort;
import makeus.cmc.malmo.application.port.out.SaveMemberAnswerPort;
import makeus.cmc.malmo.application.port.out.SaveTempCoupleQuestionPort;
import makeus.cmc.malmo.domain.model.question.CoupleQuestion;
import makeus.cmc.malmo.domain.model.question.MemberAnswer;
import makeus.cmc.malmo.domain.model.question.TempCoupleQuestion;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CoupleQuestionCommandHelper {

    private final SaveCoupleQuestionPort saveCoupleQuestionPort;
    private final SaveTempCoupleQuestionPort saveTempCoupleQuestionPort;
    private final SaveMemberAnswerPort saveMemberAnswerPort;

    public TempCoupleQuestion saveTempCoupleQuestion(TempCoupleQuestion tempCoupleQuestion) {
        return saveTempCoupleQuestionPort.saveTempCoupleQuestion(tempCoupleQuestion);
    }

    public CoupleQuestion saveCoupleQuestion(CoupleQuestion coupleQuestion) {
        return saveCoupleQuestionPort.saveCoupleQuestion(coupleQuestion);
    }

    public MemberAnswer saveMemberAnswer(MemberAnswer memberAnswer) {
        return saveMemberAnswerPort.saveMemberAnswer(memberAnswer);
    }
}
