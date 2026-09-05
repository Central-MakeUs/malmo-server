package makeus.cmc.malmo.adaptor.out.persistence;

import makeus.cmc.malmo.adaptor.out.persistence.adapter.MemberPersistenceAdapter;
import makeus.cmc.malmo.adaptor.out.persistence.mapper.MemberMapper;
import makeus.cmc.malmo.adaptor.out.persistence.repository.member.MemberRepository;
import makeus.cmc.malmo.application.port.out.member.SaveMemberPort;
import makeus.cmc.malmo.config.QueryDslConfig;
import makeus.cmc.malmo.domain.model.member.Member;
import makeus.cmc.malmo.domain.value.id.InviteCodeValue;
import makeus.cmc.malmo.domain.value.state.MemberState;
import makeus.cmc.malmo.domain.value.type.MemberRole;
import makeus.cmc.malmo.domain.value.type.Provider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({MemberPersistenceAdapter.class, MemberMapper.class, QueryDslConfig.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class MemberPersistenceAdapterConcurrencyTest {

    private static final int CONCURRENT_REQUEST_COUNT = 8;

    @Autowired
    private SaveMemberPort saveMemberPort;

    @Autowired
    private MemberRepository memberRepository;

    private String providerId;

    @AfterEach
    void cleanUp() {
        if (providerId == null) {
            return;
        }

        List<Long> memberIds = memberRepository.findAll().stream()
                .filter(member -> providerId.equals(member.getProviderId()))
                .map(member -> member.getId())
                .toList();
        memberRepository.deleteAllById(memberIds);
    }

    @Test
    void concurrentMemberCreationReturnsSingleMember() throws Exception {
        providerId = "concurrent-provider-" + UUID.randomUUID();
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_REQUEST_COUNT);
        CountDownLatch ready = new CountDownLatch(CONCURRENT_REQUEST_COUNT);
        CountDownLatch start = new CountDownLatch(1);

        try {
            List<Future<Member>> futures = new ArrayList<>();
            for (int request = 0; request < CONCURRENT_REQUEST_COUNT; request++) {
                int requestNumber = request;
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    return saveMemberPort.saveMemberIfAbsent(newMember(requestNumber));
                }));
            }

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            Set<Long> returnedMemberIds = new java.util.HashSet<>();
            for (Future<Member> future : futures) {
                returnedMemberIds.add(future.get(10, TimeUnit.SECONDS).getId());
            }

            long savedMemberCount = memberRepository.findAll().stream()
                    .filter(member -> providerId.equals(member.getProviderId()))
                    .count();

            assertThat(returnedMemberIds).hasSize(1);
            assertThat(savedMemberCount).isOne();
        } finally {
            executor.shutdownNow();
        }
    }

    private Member newMember(int requestNumber) {
        return Member.createMember(
                Provider.KAKAO,
                providerId,
                MemberRole.MEMBER,
                MemberState.BEFORE_ONBOARDING,
                "concurrent@example.com",
                InviteCodeValue.of("concurrent-invite-" + requestNumber + "-" + UUID.randomUUID()),
                null
        );
    }
}
