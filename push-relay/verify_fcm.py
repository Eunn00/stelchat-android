"""Validate sender credentials without sending a notification."""
import firebase_admin
from firebase_admin import messaging

firebase_admin.initialize_app()
messaging.send(messaging.Message(topic='stelchat_setup_validation', data={'test': 'validation'}), dry_run=True)
print('FCM authentication/API validation passed; no notification sent')
