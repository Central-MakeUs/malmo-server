package makeus.cmc.malmo.application.port.out.member;

public interface WebLoginTicketPort {

    String issue(Ticket ticket);

    Ticket consume(String ticket);

    record Ticket(Long memberId, String memberState) {
    }
}
