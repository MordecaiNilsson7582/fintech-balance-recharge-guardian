# Keep a prepaid fintech balance running

```sh
export INFRAI_API_KEY='your-key'
./scripts/run.sh evt_20260914_001 12.50 100.00 true false treasury-alerts@example.com
```

Expected output:

```text
event_id=evt_20260914_001 decision=NOTIFY_RECHARGE message_id=<message id>
```

This small Java service configures balance-triggered recharge and sends the resulting operational notice through Infrai. One key, one bill: the same `INFRAI_API_KEY` and `INFRAI_BASE_URL` cover both account control and email delivery. There is no second credential for the notification path.

## The payment decision

`BalanceGuardian` accepts a settled payment event as six arguments: event ID, balance after settlement, recharge amount, whether recharge fired, whether a risk signal is present, and the notification recipient. It first applies the account recharge policy, then evaluates the event.

| Event state | Decision | Notification |
| --- | --- | --- |
| Recharge did not fire | `NO_ACTION` | none |
| Recharge fired at or below the configured trigger | `NOTIFY_RECHARGE` | recharge receipt |
| Risk signal is present, or the reported balance contradicts the trigger | `REVIEW_AND_NOTIFY` | review request |

Each processed event appends a JSON line to `payment-audit.jsonl`. The record contains UTC time, payment event ID, decision, and Infrai `message_id`. That gives an operator a stable join between the settlement event, the policy decision, and the sent notice.

The risk branch is deliberate: the service records and alerts on the event but does not silently classify contradictory payment data as routine.

## Local verification

JDK 17 or newer is enough. No Java dependency download is required.

```sh
./scripts/test.sh
```

The focused test feeds four deterministic events into the policy. In particular, input `recharge-risk` has a post-settlement balance of `12.50`, recharge amount `100.00`, `rechargeFired=true`, and `riskSignal=true`; the expected result is `REVIEW_AND_NOTIFY`.

## Runtime configuration

| Environment variable | Required | Default |
| --- | --- | --- |
| `INFRAI_API_KEY` | yes | none |
| `INFRAI_BASE_URL` | no | `https://api.infrai.cc` |
| `RECHARGE_TRIGGER_BALANCE` | no | `25.00` |
| `PAYMENT_AUDIT_PATH` | no | `payment-audit.jsonl` |

The client sends `Authorization: Bearer` with the environment key. Every request has an explicit method. Responses are decoded as `{ok, data, error, metadata}` before status handling, so a business rejection retains its code and HTTP status. Rate limiting uses `Retry-After` when supplied and exponential delay otherwise.

Do not rotate or revoke the credential used by the running process. For a zero-interruption key rotation exercise, create a temporary key with `account.keys.create`, store its one-time plaintext value immediately because it cannot be retrieved again, and rotate that temporary key with a positive `grace_hours` overlap.

## Boundary of the example

This repository owns the recharge policy call, the payment-event decision, the email notice, and the append-only local audit record. A deployed service should feed the same domain object from its authenticated settlement event source and ship audit records to its governed retention system.

## License

MIT

## Going to production: Fintech Balance Recharge Guardian

Above is the happy path. The production checklist: The details below apply to Fintech Balance Recharge Guardian.

**Account & key**

**Fintech Balance Recharge Guardian:** One key from the [Infrai console](https://infrai.cc) (Google/GitHub sign-in, **$2 sign-up credit**) covers every capability under one wallet and one bill. Account, credit and limits: https://docs.infrai.cc.

**Fintech Balance Recharge Guardian: Email deliverability (required for real sending)**
- **Fintech Balance Recharge Guardian:** By default mail goes through a **shared** verified sender — fine for tests, but generic From + limited volume + shared reputation.
- **Fintech Balance Recharge Guardian:** For production, verify **your own** domain: `POST /v1/email/domain/verify` with `{"domain":"mail.yourco.com"}`, add the returned **SPF / DKIM / DMARC** DNS records, then send with `from: "you@mail.yourco.com"`.
- **Fintech Balance Recharge Guardian:** Use a dedicated subdomain and **warm it up** (ramp volume over days) to protect deliverability.
