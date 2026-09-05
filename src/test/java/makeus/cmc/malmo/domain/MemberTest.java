package makeus.cmc.malmo.domain;

import makeus.cmc.malmo.domain.model.member.Member;
import makeus.cmc.malmo.domain.value.state.MemberState;
import makeus.cmc.malmo.domain.value.type.MemberRole;
import makeus.cmc.malmo.domain.value.type.Provider;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MemberTest {

    @Test
    void deleteUsesMemberSpecificProviderIdTombstone() {
        Member member = Member.from(
                42L, Provider.KAKAO, "provider-id", MemberRole.MEMBER, MemberState.ALIVE,
                false, null, null, null, 0, 0,
                null, "member@example.com", null, null, null, null, null,
                null, null, null, null, null, null, null
        );

        member.delete();

        assertThat(member.getMemberState()).isEqualTo(MemberState.DELETED);
        assertThat(member.getProviderId()).isEqualTo("provider-id_deleted_member_42");
        assertThat(member.getDeletedAt()).isNotNull();
    }
}
