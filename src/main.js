import { Capacitor, CapacitorHttp, registerPlugin } from '@capacitor/core';
import { App } from '@capacitor/app';
import { Preferences } from '@capacitor/preferences';
import { defaults, preference, mobileSettings, roomKey, externalUrl, parseEvent, Deduper } from './policy.js';
import { installMobileNotifications } from './mobile-notifications.js';
import { installMemberPicker } from './member-picker.js';
import {normalizeMember} from './member-names.js';
import {installTouchFeedback} from './touch-feedback.js';
const native=Capacitor.isNativePlatform(), Native=registerPlugin('StelChatNative');
const listeners=new Map(), deduper=new Deduper();
const on=(type,callback)=>{if(!listeners.has(type))listeners.set(type,new Set());listeners.get(type).add(callback);return ()=>listeners.get(type).delete(callback);};
const emit=(type,value)=>{for(const callback of listeners.get(type)||[])callback(value);};
let settings=defaults(), members=[], seedPromise, active=true, stream, reconnect, attempt=0, connecting=false, snapshotPromise;
let connection={connected:false,state:'connecting'};
function status(value){connection=value;emit('connection',value);}
function notice(text){const el=document.querySelector('#mobile-notice');if(el){el.textContent=text;el.hidden=!text;}}
async function showPushStatus(){
  if(!native||!active)return;
  try{const push=await Native.pushStatus();
    notice(!push.configured?'알림 서비스를 사용할 수 없습니다.':!push.permission?'휴대폰 설정에서 StelChat 알림을 허용해 주세요.':push.synced?'':`설정은 저장됐지만 알림 연결이 아직 완료되지 않았습니다. (${push.confirmedTopics}/${push.desiredTopics}) 네트워크 연결 후 자동으로 재시도합니다.`);
  }catch{notice('알림 연결 상태를 확인하지 못했습니다. 잠시 후 다시 확인합니다.');}
}
const stored=await Preferences.get({key:'settings'});
try{settings={...defaults(),...JSON.parse(stored.value||'{}')};}catch{}
if(native){const storedNative=await Native.readSettings();settings={...defaults(),...storedNative.settings};}
settings=mobileSettings(settings);
if(native)await Native.savePushSettings({settings});
await Preferences.set({key:'settings',value:JSON.stringify(settings)});
async function api(path){
  if(!active)throw Error('App is in background');
  if(!path.startsWith('/api/'))throw Error('Invalid API path');
  if(native){const result=await CapacitorHttp.get({url:'https://stelchat.xyz'+path,connectTimeout:10000,readTimeout:15000});if(result.status!==200)throw Error('API '+result.status);return result.data;}
  const result=await fetch(path,{signal:AbortSignal.timeout(15000)});if(!result.ok)throw Error('API '+result.status);return result.json();
}
async function snapshot(){
  if(!snapshotPromise)snapshotPromise=(async()=>{
    const [streamers,recent]=await Promise.all([api('/api/streamers'),api('/api/recent?limit=20')]);
    members=streamers.map(normalizeMember);document.dispatchEvent(new Event('stelchat-members-updated'));
    for(const message of recent)deduper.accept({eventName:'chat',payload:message});
    return {streamers:members,recent,settings,connection};
  })().finally(()=>{snapshotPromise=null;});
  return snapshotPromise;
}
let saving=Promise.resolve();
function mutate(change,needsPush=false){
  const operation=saving.catch(()=>{}).then(async()=>{
    const next=structuredClone(settings);change(next);
    if(needsPush){
      if(!native)throw Error('Android 기기에서만 시스템 알림을 켤 수 있습니다.');
      const permission=await Native.enablePush();
      if(!permission.enabled)throw Error('휴대폰 설정에서 StelChat 알림을 허용해 주세요.');
    }
    // Commit native preferences first: background filtering must match the UI.
    if(native)await Native.savePushSettings({settings:next});
    await Preferences.set({key:'settings',value:JSON.stringify(next)});
    settings=next;emit('settings',settings);void showPushStatus();return settings;
  }).catch(error=>{notice(error.message);throw error;});
  saving=operation;return operation;
}
const noop=()=>{};
window.stelchat={
  memberProfiles:()=>members,
  acknowledgeNotifications:keys=>native?Native.acknowledgeNotifications({keys:[...new Set(keys)].slice(0,500)}):Promise.resolve(),
  emojiCacheUrl:file=>native?`https://localhost/emoji-cache/${file}`:`https://stelchat.xyz/emoji-assets/${file}`,
  emojiCacheStats:()=>native?Native.emojiCacheStats():Promise.resolve({bytes:0,limit:50*1048576,count:0}),
  emojiCacheClear:()=>native?Native.emojiCacheClear():Promise.resolve(),
  emojiCacheHas:async files=>native?(await Native.emojiCacheHas({files})).files:[],
  snapshot,refresh:snapshot,
  emojiSeed:()=>seedPromise||=(api('/api/emoji-seed').catch(e=>{seedPromise=null;throw e;})),
  emojiReady:files=>api('/api/emoji-ready?files='+encodeURIComponent(files.join(','))),
  memberSessions:uid=>api(`/api/streamers/${encodeURIComponent(uid)}/sessions?limit=50`),
  sessionPreview:(id,uid)=>api(`/api/sessions/${Number(id)}?target_uid=${encodeURIComponent(uid)}&limit=20`),
  openUrl:async value=>{const url=externalUrl(value);if(!url)throw Error('Blocked link');if(native)await Native.openExternal({url});else window.open(url,'_blank','noopener,noreferrer');},
  appVersionStatus:async()=>({current:native?(await App.getInfo()).version:'0.1.1',latest:null,updateAvailable:false}),
  setSetting:(key,value)=>mutate(next=>{if(key==='darkMode')next.darkMode=Boolean(value);else if(key==='notificationMode'&&['sound','vibrate','silent','none'].includes(value))next.notificationMode=value;}),
  openNotificationSettings:()=>native?Native.openNotificationSettings():Promise.resolve(),
  retryPush:async()=>{if(native){await Native.retryPush();await showPushStatus();}},
  setSelectedMember:uid=>mutate(next=>{if(!members.some(m=>m.uid===uid))throw Error('Unknown member');next.selectedMemberUid=uid;}),
  setMemberNotification:(uid,channel,type,value)=>mutate(next=>{
    if(!members.some(m=>m.uid===uid)||!['desktop','sound'].includes(channel)||!['live','chat'].includes(type))throw Error('Invalid preference');
    next.notificationPreferences[uid]=preference(next.notificationPreferences[uid]);next.notificationPreferences[uid].desktop[type]=Boolean(value);next.notificationPreferences[uid].sound[type]=false;
  },channel==='desktop'&&value),
  setAllMemberNotifications:(channel,type,value)=>mutate(next=>{
    for(const member of members){const p=preference(next.notificationPreferences[member.uid]);for(const t of ['live','chat'])if(!type||type===t){p.desktop[t]=Boolean(value);p.sound[t]=false;}next.notificationPreferences[member.uid]=p;}
  },(!channel||channel==='desktop')&&value),
  setChatRoomMuted:(id,uid,value)=>mutate(next=>{const key=roomKey(id,uid);if(value)next.mutedChatRooms[key]=true;else delete next.mutedChatRooms[key];}),
  hideWindow:async()=>{if(native)await App.minimizeApp();},
  onEvent:cb=>on('event',cb),onConnection:cb=>on('connection',cb),onSettings:cb=>on('settings',cb),onNotificationSound:cb=>on('sound',cb),onVersionStatus:()=>noop,
};
function receive(event){
  if(!event||!deduper.accept(event))return;
  if(event.eventName==='silent-resync')event={...event,eventName:'resync'};
  const incoming=event.payload;
  let liveSound=false;
  if(event.eventName==='session'){
    const index=members.findIndex(m=>m.uid===incoming.channel_id);
    if(index>=0){const transition=window.stelchatRendererState.applySessionEventToStreamer(incoming,members[index]);
      if(transition.accepted){members[index]=transition.streamer;liveSound=transition.notify;}}
  }
  emit('event',event);
  const p=event.payload;
  // FCM is the sole notification source, foreground and background alike.
  // SSE updates content only, avoiding duplicate sound/notification delivery.
}
function retry(){
  if(!active)return;
  status({connected:false,state:'reconnecting'});
  clearTimeout(reconnect);reconnect=setTimeout(connect,Math.min(30000,1000*2**Math.min(attempt++,5)));
}
async function connect(){
  if(!active||connecting)return;connecting=true;clearTimeout(reconnect);
  try{
    if(native)await Native.startEvents();
    else{
      stream?.close();stream=new EventSource('/api/events');
      stream.onopen=()=>{attempt=0;status({connected:true,state:'connected'});};
      for(const type of ['ready','resync','silent-resync','chat','session','registry'])stream.addEventListener(type,e=>{try{receive({eventName:type,payload:JSON.parse(e.data)});}catch{}});
      stream.onerror=()=>{stream.close();retry();};
    }
  }catch{retry();}finally{connecting=false;}
}
async function lifecycle(isActive){
  active=isActive;clearTimeout(reconnect);
  if(!active){stream?.close();if(native)await Native.stopEvents();status({connected:false,state:'background'});return;}
  // No startup notification: snapshots are data only.
  emit('event',{eventName:'resync',payload:{}});await connect();if(native){await Native.retryPush();await showPushStatus();}
}
const banner=document.createElement('div');banner.id='mobile-notice';banner.className='mobile-notice';document.querySelector('#settings-panel .settings-head').after(banner);
notice(native?'':'브라우저 미리보기에서는 휴대폰 알림을 받을 수 없습니다.');
installMobileNotifications();
installTouchFeedback();
for(const name of ['realtime-merge-policy','renderer-state-policy','emoji-renderer','renderer'])await new Promise((resolve,reject)=>{const s=document.createElement('script');s.src=`/desktop/${name}.js`;s.onload=resolve;s.onerror=reject;document.body.append(s);});
installMemberPicker();
async function openNotification(){
 const route=await Native.consumeNotificationOpen();if(!route.uid)return;
 for(const panel of document.querySelectorAll('.settings-panel.open')){panel.classList.remove('open');panel.setAttribute('aria-hidden','true');}
 if(route.kind==='chat')await window.openChatNotification?.(route);
 else if(route.kind==='live'){document.querySelector('[data-tab="live"]').click();await window.stelchat.acknowledgeNotifications([route.event_key]);}
}
if(native){
  await Native.addListener('notificationOpen',()=>{void openNotification();});
  void openNotification();
  await Native.addListener('sse',event=>{try{receive(parseEvent(event.block));}catch{}});
  await Native.addListener('streamState',event=>{if(!active)return;if(event.connected){attempt=0;status({connected:true,state:'connected'});}else retry();});
  await App.addListener('appStateChange',({isActive})=>lifecycle(isActive));
  await App.addListener('backButton',()=>{if(window.closeMobileMemberPicker?.())return;const open=[...document.querySelectorAll('.settings-panel.open')].pop();if(open){open.classList.remove('open');open.setAttribute('aria-hidden','true');}else App.minimizeApp();});
  await showPushStatus();
  setInterval(showPushStatus,5000);
}else document.addEventListener('visibilitychange',()=>lifecycle(!document.hidden));
await connect();
