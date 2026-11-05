"""Optional single-instance FCM relay. Not installed on production.

SSE is not durable: reconnect gaps cannot guarantee notifications. Chat history
is recovered by the app's normal snapshot API, not by replaying old pushes.
"""
import json
import logging
import os
import sqlite3
import time
import fcntl
from datetime import timedelta
import requests
import firebase_admin
from firebase_admin import messaging
from policy import payload

logging.basicConfig(level=logging.INFO)
firebase_admin.initialize_app()  # GOOGLE_APPLICATION_CREDENTIALS: server-side only
state_path = os.getenv('PUSH_STATE_DB', 'push-state.sqlite')
lock = open(state_path + '.lock', 'a')
fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
database = sqlite3.connect(state_path)
database.execute('CREATE TABLE IF NOT EXISTS sent (event_key TEXT PRIMARY KEY, at REAL)')
session = requests.Session()
session.trust_env = False
base = 'https://stelchat.xyz'
while True:
    try:
        response = session.get(base + '/api/streamers', timeout=15)
        response.raise_for_status()
        members = {m['uid']: m for m in response.json()}
        with session.get(base + '/api/events', stream=True, timeout=(15, 55)) as stream:
            stream.raise_for_status()
            logging.info('SSE connected; listening for fresh public events')
            block = []
            for line in stream.iter_lines(chunk_size=1, decode_unicode=True):
                if line:
                    block.append(line)
                    if sum(len(s) for s in block) > 262144:
                        raise ValueError('Oversized SSE')
                    continue
                raw = '\n'.join(s[5:].lstrip() for s in block if s.startswith('data:'))
                block = []
                if not raw:
                    continue
                event = json.loads(raw)
                if event.get('type') == 'registry':
                    break
                data = payload(event, members)
                if not data:
                    continue
                key = data['data']['event_key']
                if database.execute('SELECT 1 FROM sent WHERE event_key=?', (key,)).fetchone():
                    continue
                message = messaging.Message(topic=data['topic'], data=data['data'], android=messaging.AndroidConfig(priority='high', ttl=timedelta(minutes=10)))
                messaging.send(message)
                database.execute('INSERT OR IGNORE INTO sent VALUES (?,?)', (key,time.time()))
                database.execute('DELETE FROM sent WHERE at<?', (time.time()-86400*7,))
                database.commit()
                logging.info('Push accepted: %s', key)
    except Exception as error:
        logging.warning('Relay disconnected: %s', type(error).__name__)
        time.sleep(5)
