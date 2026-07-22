# Kakao web login

The web flow is additive. Existing mobile endpoints (`/login/kakao`, `/login/apple`,
`/refresh`, and `/logout`) and the `Member.refreshToken`/`Member.oauthToken` fields are
not used by web sessions.

## API flow

1. Redirect the browser to the backend endpoint:
   - `GET /login/web/kakao/authorize?returnUrl={frontendCallback}`
2. The backend creates one-time `state`, `nonce`, and PKCE values and redirects to the provider.
3. The provider calls the backend callback.
4. The backend redirects to `{frontendCallback}?ticket={oneTimeTicket}`.
5. Exchange the ticket once:

```http
POST /login/web/exchange
Content-Type: application/json

{"ticket":"..."}
```

6. Refresh and revoke the web session only through:

```http
POST /login/web/refresh
Content-Type: application/json

{"refreshToken":"..."}
```

```http
POST /login/web/logout
Content-Type: application/json

{"refreshToken":"..."}
```

Do not call the mobile `/refresh` or `/logout` endpoints from the web client.

## Required environment variables

```text
WEB_SOCIAL_LOGIN_ALLOWED_RETURN_ORIGINS=https://web.example.com

KAKAO_WEB_REST_API_KEY=
KAKAO_WEB_CLIENT_SECRET=
KAKAO_WEB_REDIRECT_URI=https://api.example.com/login/web/kakao/callback
```

`WEB_SOCIAL_LOGIN_ALLOWED_RETURN_ORIGINS` accepts a comma-separated list. Production origins
must use HTTPS. Web login is always active, so all required variables must be present before
the application starts. `KAKAO_WEB_CLIENT_SECRET` may be empty when the Kakao app does not use it.

## Provider console setup

- Register the web domain and exact backend redirect URI.
- Enable OpenID Connect.
- Use the REST API key and configure the client secret.

## Deployment order

1. Apply `docs/sql/web-social-login.sql`.
2. Configure the Kakao console and all required environment variables.
3. Deploy the application and run the existing mobile sign-in regression tests.
4. Verify Kakao web login in QA.
5. Deploy production after checking mobile `/refresh` failure rates and web callback errors.
