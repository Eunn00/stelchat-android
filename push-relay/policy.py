"""Public-event to FCM topic mapping. No user identifiers or device token storage."""
from datetime import datetime, timezone, timedelta
import time

def clipped(value, limit):
    return str(value or '').encode('utf-8')[:limit].decode('utf-8', errors='ignore')

def payload(event, members, now=None):
    now = time.time() if now is None else now
    if event.get('notify') is False:
        return None
    kind = 'chat' if event.get('type') == 'chat' else 'live' if event.get('type') == 'session' else None
    uid = event.get('target_uid') if kind == 'chat' else event.get('channel_id')
    if not kind or uid not in members:
        return None
    if kind == 'live' and (event.get('status') != 'OPEN' or event.get('previous_status') == 'OPEN' or event.get('superseded')):
        return None
    stamp = event.get('sent_at' if kind == 'chat' else 'opened_at')
    try:
        parsed = datetime.fromisoformat(stamp)
        if parsed.tzinfo is None:
            # CHZZK openDate is a Korean wall-clock time, unlike sent_at.
            if kind != 'live':
                return None
            parsed = parsed.replace(tzinfo=timezone(timedelta(hours=9)))
        occurred = parsed.timestamp()
        if not -30 <= now - occurred <= 60:
            return None
        event_id = int(event.get('id' if kind == 'chat' else 'session_id'))
        if event_id < 1:
            return None
    except (TypeError, ValueError):
        return None
    key = f'{kind}:{uid}:{event_id}'
    name = members[uid].get('short_name') or members[uid]['name']
    channel_member = members.get(event.get('channel_id'), {})
    channel_name = str(channel_member.get('short_name') or event.get('channel_name') or channel_member.get('name') or '').strip()
    chat_title = f'{name}의 채팅' + (f'·{channel_name}의 방송에서' if channel_name else '')
    return {'topic': f'stelchat_{kind}_{uid}', 'data': {
        'uid': uid, 'kind': kind, 'event_key': key,
        'avatar_path': clipped(members[uid].get('avatar_url', ''), 250),
        'session_id': str(event.get('session_id', '')),
        'sent_at_ms': str(int(occurred * 1000)),
        'title': clipped(chat_title if kind == 'chat' else f"{members[uid]['name']} 방송 시작", 200),
        'body': clipped(event.get('content', '') if kind == 'chat' else event.get('title', ''), 1500),
    }}
