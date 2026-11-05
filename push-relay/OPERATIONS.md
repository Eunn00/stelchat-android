# Private Android push relay

Independent systemd service: `stelchat-push`. It does not restart the web API or collector.

- Code and virtual environment: `/opt/stelchat-push`
- Credential: `/etc/stelchat-push/service-account.json` (root:stelchat-push, 0640)
- Persistent deduplication: `/var/lib/stelchat-push/state.sqlite`
- Read status: `sudo systemctl status stelchat-push --no-pager`
- Read logs: `sudo journalctl -u stelchat-push --since '10 minutes ago' --no-pager`
- Stop pushes: `sudo systemctl stop stelchat-push`
- Disable startup: `sudo systemctl disable stelchat-push`
- Restart relay only: `sudo systemctl restart stelchat-push`

Only fresh chat and live-start events are sent. Manual/silent updates and snapshots
do not produce pushes. Each successful FCM submission is recorded by event key;
a process lock prevents duplicate instances sharing this state. A crash between
FCM acceptance and commit can resend a key; Android also deduplicates those keys.

This is not guaranteed delivery: SSE reconnect gaps, stale-event filtering, FCM
delivery delays, Android force-stop and notification settings may suppress alerts.
Normal app history refresh remains independent. FCM acceptance is not proof of
delivery to a device. Do not run a second local relay against the same project.

Private test use only. No app publication or privacy-policy changes are part of
this setup. Review disclosure, dependency notices and signing before public release.
