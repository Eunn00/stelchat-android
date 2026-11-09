const uidPattern=/^[a-f0-9]{32}$/;
const idPattern=/^[1-9][0-9]{0,19}$/;

export function chatReadRooms(messages,uid='',sessionId=null){
  const marks=new Map();
  for(const message of messages||[]){
    const target=message.target_uid||uid,sid=String(message.session_id??sessionId??''),id=String(message.id??'');
    if(!uidPattern.test(target)||!idPattern.test(sid)||!idPattern.test(id))continue;
    const key=sid+':'+target,old=marks.get(key);
    if(!old||BigInt(id)>BigInt(old.through_id))marks.set(key,{uid:target,session_id:sid,through_id:id});
  }
  return [...marks.values()];
}
