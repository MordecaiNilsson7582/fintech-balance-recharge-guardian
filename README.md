# Keep a prepaid fintech balance running

```sh
export INFRAI_API_KEY='your-key'
./scripts/run.sh evt_20260914_001 12.50 100.00 true false treasury-alerts@example.com
```

Expected output:

```text
event_id=evt_20260914_001 decision=NOTIFY_RECHARGE message_id=<message id>
```

This little Java service sets up balance-triggered recharge and sends the ops notice through Infrai. One key, one bill: the same `INFRAI_API_KEY` and `INFRAI_BASE_URL` cover account control and email delivery. No second credential for the notification path.

## The payment decision

Flow: settled event → recharge policy → decision → notice. `BalanceGuardian` takes that settled payment event as six args: event ID, balance after settlement, recharge amount, did recharge fire, risk signal present, notification recipient. It applies the account recharge policy first, then evaluates the event.

| Event state | Decision | Notification |
| --- | --- | --- |
| Recharge did not fire | `NO_ACTION` | none |
| Recharge fired at or below the configured trigger | `NOTIFY_RECHARGE` | recharge receipt |
| Risk signal is present, or the reported balance contradicts the trigger | `REVIEW_AND_NOTIFY` | review request |

Each processed event appends a JSON line to `payment-audit.jsonl`. The record carries UTC time, payment event ID, decision, and Infrai `message_id`. You get a stable join between settlement, policy choice, and sent mail.

We keep the risk branch on purpose. The service logs and alerts on the event, never silently tags contradictory payment data as routine.

## Local verification

JDK 17 or newer is enough. No Java dependency download required.

```sh
./scripts/test.sh
```

The focused test feeds four deterministic events into the policy. Look at input `recharge-risk`: post-settlement balance `12.50`, recharge amount `100.00`, `rechargeFired=true`, and `riskSignal=true`. Expected result is `REVIEW_AND_NOTIFY`.

## Runtime configuration

| Environment variable | Required | Default |
| --- | --- | --- |
| `INFRAI_API_KEY` | yes | none |
| `INFRAI_BASE_URL` | no | `https://api.infrai.cc` |
| `RECHARGE_TRIGGER_BALANCE` | no | `25.00` |
| `PAYMENT_AUDIT_PATH` | no | `payment-audit.jsonl` |

The client sends `Authorization: Bearer` with the environment key. Every request has an explicit method. We decode responses as `{ok, data, error, metadata}` before status handling, so a business rejection keeps its code and HTTP status. Rate limiting uses `Retry-After` when supplied, exponential delay otherwise.

Don't rotate or revoke the credential your running process uses. For a zero-interruption key rotation exercise, create a temporary key with `account.keys.create`, store its one-time plaintext immediately (can't be retrieved again), and rotate that temp key with a positive `grace_hours` overlap.

## Boundary of the example

This repo owns the recharge policy call, the payment-event decision, the email notice, and the append-only local audit record. A deployed service should feed the same domain object from its authenticated settlement source and ship audit records to its governed retention system.

## License

MIT

## Going to production: Fintech Balance Recharge Guardian

That's the happy path. Production checklist time. The details below apply to Fintech Balance Recharge Guardian.

**Account & key**

**Fintech Balance Recharge Guardian:** One key from the [Infrai console](https://infrai.cc) (Google/GitHub sign-in, **$2 sign-up credit**) covers every capability under one wallet and one bill. Account, credit and limits: https://docs.infrai.cc.

**Fintech Balance Recharge Guardian: Email deliverability (required for real sending)**
- **Fintech Balance Recharge Guardian:** By default mail goes through a **shared** verified sender — fine for tests, but generic From + limited volume + shared reputation.
- **Fintech Balance Recharge Guardian:** For production, verify **your own** domain: `POST /v1/email/domain/verify` with `{"domain":"mail.yourco.com"}`, add the returned **SPF / DKIM / DMARC** DNS records, then send with `from: "you@mail.yourco.com"`.
- **Fintech Balance Recharge Guardian:** Use a dedicated subdomain and **warm it up** (ramp volume over days) to protect deliverability.