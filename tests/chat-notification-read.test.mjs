import test from 'node:test';
import assert from 'node:assert/strict';
import {chatReadRooms} from '../src/chat-notification-read.js';
const a='a'.repeat(32),b='b'.repeat(32);
test('viewing either chat route produces the same member/room read boundary',()=>{
  assert.deepEqual(chatReadRooms([{id:9},{id:10}],a,101),
    chatReadRooms([{id:9,target_uid:a,session_id:101},{id:10,target_uid:a,session_id:101}]));
  assert.deepEqual(chatReadRooms([{id:9},{id:10}],a,101),[{uid:a,session_id:'101',through_id:'10'}]);
});
test('boundaries distinguish other members, other rooms and large numeric IDs',()=>{
  assert.deepEqual(chatReadRooms([
    {id:'90071992547409930',target_uid:a,session_id:1},
    {id:'90071992547409931',target_uid:a,session_id:1},
    {id:'10',target_uid:b,session_id:1},{id:'20',target_uid:a,session_id:2}]),
    [{uid:a,session_id:'1',through_id:'90071992547409931'},
     {uid:b,session_id:'1',through_id:'10'},{uid:a,session_id:'2',through_id:'20'}]);
});
test('unknown room/message identifiers cannot clear unrelated notifications',()=>{
  assert.deepEqual(chatReadRooms([{id:1},{id:'0',target_uid:a,session_id:1},
    {id:'x:1',target_uid:a,session_id:1},{id:2,target_uid:'bad',session_id:1}]),[]);
});
