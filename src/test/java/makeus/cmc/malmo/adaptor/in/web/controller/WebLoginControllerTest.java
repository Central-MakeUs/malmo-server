package makeus.cmc.malmo.adaptor.in.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import makeus.cmc.malmo.application.port.in.member.WebSignInUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.net.URI;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class WebLoginControllerTest {

    @Mock
    private WebSignInUseCase useCase;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new WebLoginController(useCase)).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void redirectsToKakaoAuthorizationPage() throws Exception {
        given(useCase.startAuthorization(
                "https://web.malmo.example/auth/callback", null
        )).willReturn(URI.create("https://kauth.kakao.com/oauth/authorize?state=test"));

        mockMvc.perform(get("/login/web/kakao/authorize")
                        .param("returnUrl", "https://web.malmo.example/auth/callback"))
                .andExpect(status().isFound())
                .andExpect(header().string(
                        "Location", "https://kauth.kakao.com/oauth/authorize?state=test"
                ))
                .andExpect(header().string("Cache-Control", "no-store"));
    }

    @Test
    void doesNotExposeAppleWebLoginEndpoint() throws Exception {
        mockMvc.perform(get("/login/web/apple/authorize")
                        .param("returnUrl", "https://web.malmo.example/auth/callback"))
                .andExpect(status().isNotFound());
    }

    @Test
    void exchangesOneTimeTicketForWebTokens() throws Exception {
        given(useCase.exchangeTicket("ticket")).willReturn(
                new WebSignInUseCase.SignInResponse(
                        "ALIVE", "Bearer", "access-token", "refresh-token"
                )
        );

        mockMvc.perform(post("/login/web/exchange")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new WebLoginController.TicketRequest("ticket")
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.memberState").value("ALIVE"))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh-token"));
    }

    @Test
    void webLogoutUsesWebRefreshTokenOnly() throws Exception {
        mockMvc.perform(post("/login/web/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new WebLoginController.RefreshTokenRequest("web-refresh-token")
                        )))
                .andExpect(status().isOk());

        verify(useCase).logout("web-refresh-token");
    }
}
