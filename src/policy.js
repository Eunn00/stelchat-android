export const defaults = () => ({darkMode:false, notificationVolume:0.7, opacity:1, selectedMemberUid:'', notificationPreferences:{}, mutedChatRooms:{}});
export const preference = value => ({desktop:{live:Boolean(value?.desktop?.live),chat:Boolean(value?.desktop?.chat)},sound:{live:Boolean(value?.sound?.live),chat:Boolean(value?.sound?.chat)}});
export function mobileSettings(value) {
  const next={...defaults(),...value,notificationPreferences:{...value?.notificationPreferences}};
  if(next.mobileNotificationVersion!==2){
    let audible=false;
    for(const [uid,old] of Object.entries(next.notificationPreferences)){
      const p=preference(old);
      audible ||= p.sound.live||p.sound.chat;
      next.notificationPreferences[uid]={desktop:{live:p.desktop.live||p.sound.live,chat:p.desktop.chat||p.sound.chat},sound:{live:false,chat:false}};
    }
    next.notificationMode=audible?'sound':'silent';
    if(!Object.keys(next.notificationPreferences).length)next.notificationMode='sound';
    next.mobileNotificationVersion=2;
  }
  if(!['sound','vibrate','silent','none'].includes(next.notificationMode))next.notificationMode='sound';
  return next;
}
export function roomKey(id,uid) {
  if(!Number.isSafeInteger(Number(id)) || Number(id)<1 || !/^[a-f0-9]{32}$/.test(uid)) throw Error('Invalid chat room');
  return `${Number(id)}:${uid}`;
}
export function externalUrl(value) {
  try { const u=new URL(value); return u.protocol==='https:' && !u.username && !u.password && !u.port && ['stelchat.xyz','chzzk.naver.com'].includes(u.hostname) ? u.href : null; } catch {return null;}
}
export function parseEvent(block) {
  if(block.length>262144) throw Error('Oversized event');
  let eventName='message';const data=[];
  for(const line of block.split(/\r?\n/)) {
    if(line.startsWith('event:')) eventName=line.slice(6).trim();
    if(line.startsWith('data:')) data.push(line.slice(5).trimStart());
  }
  if(!data.length) return null;
  return {eventName,payload:JSON.parse(data.join('\n'))};
}
export class Deduper {
  constructor(){this.keys=new Set();}
  accept(event){
    if(event.eventName!=='chat')return true;
    const key=String(event.payload.id);
    if(!/^\d+$/.test(key)||this.keys.has(key))return false;
    this.keys.add(key); if(this.keys.size>1000)this.keys.delete(this.keys.values().next().value);
    return true;
  }
}
