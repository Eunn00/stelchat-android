import test from 'node:test';
import assert from 'node:assert/strict';
import {defaults,preference,mobileSettings,roomKey,externalUrl,parseEvent,Deduper} from '../src/policy.js';
test('mobile migration preserves enabled events and is idempotent',()=>{
 const value={notificationPreferences:{a:{desktop:{live:true},sound:{chat:true}}}};
 const migrated=mobileSettings(value);
 assert.deepEqual(migrated.notificationPreferences.a,{desktop:{live:true,chat:true},sound:{live:false,chat:false}});
 assert.equal(migrated.notificationMode,'sound');
 assert.equal(mobileSettings({notificationPreferences:{a:{desktop:{chat:true}}}}).notificationMode,'silent');
 assert.deepEqual(mobileSettings(migrated),migrated);
 for(const mode of ['sound','vibrate','silent','none'])assert.equal(mobileSettings({...migrated,notificationMode:mode}).notificationMode,mode);
 assert.deepEqual(mobileSettings({...migrated,notificationMode:'none'}).notificationPreferences,migrated.notificationPreferences);
 assert.equal(mobileSettings({}).notificationMode,'sound');
 assert.deepEqual(mobileSettings({}).notificationPreferences,{});
});
test('notifications are off by default and independent by event',()=>{
 assert.deepEqual(defaults().notificationPreferences,{});
 assert.deepEqual(preference({desktop:{chat:true}}),{desktop:{chat:true,live:false},sound:{chat:false,live:false}});
});
test('room mute key includes member and session',()=>{
 assert.notEqual(roomKey(1,'a'.repeat(32)),roomKey(1,'b'.repeat(32)));
 assert.throws(()=>roomKey(-1,'a'.repeat(32)));
});
test('external link allowlist prevents arbitrary schemes or hosts',()=>{
 for(const value of ['javascript:alert(1)','https://evil.test','https://user@stelchat.xyz','http://stelchat.xyz','https://stelchat.xyz:999'])assert.equal(externalUrl(value),null);
 assert.equal(externalUrl('https://chzzk.naver.com/live/abc'),'https://chzzk.naver.com/live/abc');
});
test('SSE supports comments, CRLF and multiline JSON',()=>{
 assert.equal(parseEvent(': keepalive\n\n'),null);
 assert.deepEqual(parseEvent('event: chat\r\ndata: {"id":1,\r\ndata: "content":"hello"}\r\n'),{eventName:'chat',payload:{id:1,content:'hello'}});
 assert.throws(()=>parseEvent('a'.repeat(262145)));
});
test('duplicate chats suppressed and cache bounded',()=>{
 const d=new Deduper(); const e={eventName:'chat',payload:{id:1}};
 assert.equal(d.accept(e),true);assert.equal(d.accept(e),false);
 for(let id=2;id<1500;id++)d.accept({eventName:'chat',payload:{id}});
 assert.equal(d.keys.size,1000);
 assert.equal(d.accept({eventName:'session',payload:{}}),true);
});
