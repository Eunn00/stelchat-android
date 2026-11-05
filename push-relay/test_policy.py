import unittest
from policy import payload

UID = 'a' * 32
MEMBERS = {UID: {'name': '테스트'}}
CHAT = {'type':'chat','target_uid':UID,'id':10,'session_id':20,'content':'안녕','sent_at':'2026-10-28T00:00:00+00:00'}
NOW = 1793145600

class PolicyTests(unittest.TestCase):
    def test_chat_title_contains_channel_body_is_content_only(self):
        from datetime import datetime
        now=datetime.fromisoformat(CHAT['sent_at']).timestamp()
        result=payload({**CHAT,'channel_name':'채널'},MEMBERS,now)['data']
        self.assertEqual(result['title'],'테스트의 채팅·채널의 방송에서')
        self.assertEqual(result['body'],'안녕')
        self.assertEqual(payload(CHAT,MEMBERS,now)['data']['title'],'테스트의 채팅')
    def test_short_names_for_sender_member_channel_and_live(self):
        from datetime import datetime
        now=datetime.fromisoformat(CHAT['sent_at']).timestamp()
        other='b'*32
        members={UID:{'name':'아야츠노 유니','short_name':'유니'},other:{'name':'아라하시 타비','short_name':'타비'}}
        event={**CHAT,'channel_id':other,'channel_name':'아라하시 타비'}
        self.assertEqual(payload(event,members,now)['data']['title'],'유니의 채팅·타비의 방송에서')
        self.assertEqual(payload({**event,'channel_id':'c'*32,'channel_name':'외부채널'},members,now)['data']['title'],'유니의 채팅·외부채널의 방송에서')
        live={'type':'session','channel_id':UID,'session_id':42,'status':'OPEN','previous_status':'CLOSE','opened_at':CHAT['sent_at']}
        self.assertEqual(payload(live,members,now)['data']['title'],'아야츠노 유니 방송 시작')
    def test_chzzk_live_korean_wall_clock(self):
        from datetime import datetime
        now=datetime.fromisoformat('2026-10-29T18:00:30+09:00').timestamp()
        event={'type':'session','channel_id':UID,'session_id':42,'status':'OPEN','previous_status':'CLOSE','opened_at':'2026-10-29 18:00:00'}
        self.assertEqual(payload(event,MEMBERS,now)['data']['kind'],'live')
        self.assertIsNone(payload(event,MEMBERS,now+61))
        self.assertIsNone(payload({**event,'previous_status':'OPEN'},MEMBERS,now))
        self.assertIsNone(payload({**CHAT,'sent_at':'2026-10-29 18:00:00'},MEMBERS,now))
    def test_live_only_fresh_open_transition(self):
        from datetime import datetime
        now=datetime.fromisoformat(CHAT['sent_at']).timestamp()
        event={'type':'session','channel_id':UID,'session_id':42,'status':'OPEN','previous_status':'CLOSE','opened_at':CHAT['sent_at']}
        self.assertEqual(payload(event,MEMBERS,now)['data']['kind'],'live')
        for changes in ({'previous_status':'OPEN'},{'superseded':True},{'notify':False},{'status':'CLOSE'}):
            self.assertIsNone(payload({**event,**changes},MEMBERS,now))
    def test_live_chat_and_byte_budget(self):
        from datetime import datetime
        now=datetime.fromisoformat(CHAT['sent_at']).timestamp()
        result=payload({**CHAT,'content':'😀'*2000},MEMBERS,now)
        self.assertEqual(result['topic'],'stelchat_chat_'+UID)
        self.assertLessEqual(len(result['data']['body'].encode()),1500)
        avatar='/api/profile-images/'+UID+'?v=123456abcdef'
        self.assertEqual(payload(CHAT,{UID:{'name':'테스트','avatar_url':avatar}},now)['data']['avatar_path'],avatar)
    def test_silent_stale_unknown_and_offline_never_notify(self):
        from datetime import datetime
        now=datetime.fromisoformat(CHAT['sent_at']).timestamp()
        self.assertIsNone(payload({**CHAT,'notify':False},MEMBERS,now))
        self.assertIsNone(payload(CHAT,MEMBERS,now+61))
        self.assertIsNone(payload(CHAT,{},now))
        self.assertIsNone(payload({'type':'silent-resync'},MEMBERS,now))
        self.assertIsNone(payload({'type':'session','channel_id':UID,'status':'CLOSE'},MEMBERS,now))

if __name__=='__main__':unittest.main()
