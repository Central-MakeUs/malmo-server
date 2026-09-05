# Social login duplicate-member incident

## Summary

On 2026-09-05, Kakao login failed with an
`IncorrectResultSizeDataAccessException`. A repository method that returns an
`Optional<MemberEntity>` found multiple rows for one `(provider, provider_id)`
pair.

The affected rows were created within a sub-millisecond window. A separate social
identity had previously produced the same pattern. These timestamps establish a
concurrent check-then-insert race rather than sequential user registration.

The production duplicates were repaired manually after related-data inspection:
the member with completed onboarding data was retained, and one empty
pre-onboarding member was retained for the other group. Duplicate rows with no
related records were soft-deleted and assigned distinct provider ID tombstones.

This public incident record intentionally omits account identifiers, email
addresses, request and notification identifiers, monitoring links, host and
container identifiers, and credentials. Raw production query results and logs
must not be added to this document.

## Customer impact

- Affected users could not complete Kakao login.
- Once duplicate rows existed, every lookup through
  `findByProviderAndProviderId` failed before token issuance.
- Another active duplicate group was found proactively and repaired.
- No evidence showed that this exception read or returned another member's data;
  the query failed because it could not select a unique row.

## Root cause

The mobile and web sign-in flows used this sequence:

1. Query by `(provider, provider_id)`.
2. If absent, construct a new member.
3. Insert the member.

Each request ran in its own transaction. Multiple concurrent requests could all
observe "no member" before any insert committed. `@Transactional` does not
serialize independent requests, and production had no unique constraint on the
two identity columns, so every insert succeeded.

The exact source of the repeated HTTP requests is not proven by server data alone.
Likely sources include duplicate UI callbacks, repeated observers, concurrent
client tasks, or POST retry behavior. Client-side single-flight protection is a
useful follow-up but cannot replace the database invariant.

## Contributing data-model issue

Soft deletion previously changed a provider ID to `<providerId>_deleted`. A user
who deleted, rejoined, and deleted again therefore produced the same tombstone.
Several duplicate deleted identities confirmed this lifecycle. That format also
prevented adding a global unique constraint without first normalizing historical
rows.

The new tombstone is `<providerId>_deleted_member_<memberId>`, which is stable and
unique per historical member.

## Relationship to other deployment anomalies

Other deployment anomalies observed around the same period were investigated.
Application-level event attribution and an earlier occurrence of the same data
pattern showed that those anomalies were not required for this race and were not
its root cause. They remain a separate deployment-lifecycle hygiene issue.

## Remediation

### Database invariant

The production migration is in
`sqls/2026-09-05-member-provider-uniqueness.sql`. It:

1. Verifies that no active duplicate or invalid provider identity remains.
2. Creates a recoverable backup of only the identity columns being changed; it
   does not duplicate email or token columns.
3. Normalizes deleted provider IDs to member-specific tombstones.
4. Adds `UNIQUE (provider, provider_id)`.
5. Verifies the resulting index and data.

The SQL also includes an emergency rollback procedure that drops the new index
and restores the original provider IDs from the targeted backup.

Production has `spring.jpa.hibernate.ddl-auto=none`; the entity annotation alone
does not change production schema. The SQL migration is mandatory.

### Application behavior

`SaveMemberPort.saveMemberIfAbsent` now expresses create-or-load semantics for a
social identity. The persistence adapter:

1. Attempts the insert and flushes it in a `REQUIRES_NEW` transaction.
2. Lets the database unique constraint select the winning request.
3. Catches a constraint conflict outside the failed transaction.
4. Loads and returns the winning member in a fresh `REQUIRES_NEW` transaction.
5. Rethrows the original integrity error if no member exists, preserving unrelated
   constraint failures such as an invite-code collision.

Separate transactions are intentional. A transaction that has failed while
flushing must not be reused for the fallback query, and a fresh read must observe
the concurrently committed member under MySQL isolation.

Both mobile sign-in and web sign-in use this behavior.

### Entity and deletion behavior

- `MemberEntity` declares provider and provider ID as non-null.
- `MemberEntity` declares the composite unique constraint for generated test/dev
  schemas.
- `Member.delete()` includes the member ID in its provider ID tombstone.

## Deployment order

Safest order:

1. Pause social-login traffic or enter a short maintenance window.
2. Run SQL preflight and confirm empty/zero results.
3. Create and verify the backup table.
4. Normalize deleted identities and verify no duplicates.
5. Add the unique constraint.
6. Deploy the application change.
7. Run one existing-user and one new-user login smoke test for Kakao and Apple.
8. Resume traffic and monitor application errors plus duplicate-key logs.

If traffic cannot be paused, add the unique constraint first and deploy the code
immediately afterward. This can cause transient duplicate-key login failures on
the old application version, but it prevents new duplicate data.

## Verification and regression coverage

- Eight concurrent persistence calls for the same provider identity must all
  return one member ID.
- Exactly one database row must exist for that identity.
- Repeated account deletion/re-registration must produce unique tombstones.
- Existing and new Kakao/Apple login integration tests must remain green.

## Follow-up actions

- Add client-side single-flight protection around social-login calls.
- Confirm that the HTTP client does not automatically retry login POST requests.
- Add a dashboard/alert for duplicate-key conflicts on the member identity index.
- Audit deployment lifecycle and routing policies for obsolete runtime instances.
- Consider migrating production schema management to Flyway or Liquibase.
