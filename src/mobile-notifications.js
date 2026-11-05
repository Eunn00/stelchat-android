// Android-only presentation; shared desktop rendering remains unchanged.
export function installMobileNotifications(){
  const $=s=>document.querySelector(s);
  const escape=s=>String(s??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
  const panel=$('#notification-settings-panel');
  panel.querySelector('.notification-settings-head small').textContent='멤버별 방송 시작과 새 채팅 알림을 선택하세요.';
  $('#notification-settings-open small').textContent='방송 시작·새 채팅 알림과 소리·진동을 설정해요.';
  panel.querySelector('.notification-volume-setting').hidden=true;
  panel.querySelectorAll('[data-notification-channel="sound"]').forEach(el=>el.hidden=true);
  panel.querySelectorAll('[data-notification-channel="desktop"] span').forEach(el=>el.textContent=el.textContent.replace('시스템 · ',''));
  const section=document.createElement('div');section.className='mobile-alert-mode';
  section.innerHTML='<strong>알림 방식</strong><div id="mobile-alert-mode" class="mobile-mode-buttons" role="group" aria-label="알림 방식"><button type="button" data-mode="sound">소리</button><button type="button" data-mode="vibrate">진동</button><button type="button" data-mode="silent">무음</button><button type="button" data-mode="none">없음</button></div><p>무음은 알림센터에 표시되고, 없음은 모든 알림을 끕니다. 멤버별 설정은 유지돼요.</p><button type="button" id="mobile-system-settings">휴대폰 알림 설정 ↗</button>';
  panel.querySelector('.notification-settings-head').after(section);
  const update=async action=>{try{await action();}catch{const e=$('#settings-error');e.textContent='설정을 저장하지 못했습니다. 알림 권한과 네트워크를 확인해 주세요.';e.hidden=false;}};
  section.querySelectorAll('[data-mode]').forEach(button=>button.addEventListener('click',()=>update(()=>window.stelchat.setSetting('notificationMode',button.dataset.mode))));
  $('#mobile-system-settings').addEventListener('click',()=>update(()=>window.stelchat.openNotificationSettings()));
  const retry=document.createElement('button');retry.type='button';retry.id='mobile-push-retry';retry.textContent='알림 연결 다시 확인';
  retry.addEventListener('click',()=>update(()=>window.stelchat.retryPush()));section.append(retry);
  window.renderMobileNotifications=(members,settings)=>{
    section.querySelectorAll('[data-mode]').forEach(button=>{const selected=button.dataset.mode===(settings.notificationMode||'sound');button.classList.toggle('selected',selected);button.setAttribute('aria-pressed',String(selected));});
    const enabled=(uid,type)=>Boolean(settings.notificationPreferences?.[uid]?.desktop?.[type]);
    $('#notification-summary').textContent=settings.notificationMode==='none'?'모든 알림 꺼짐':`방송 ${members.filter(m=>enabled(m.uid,'live')).length}명 · 채팅 ${members.filter(m=>enabled(m.uid,'chat')).length}명`;
    $('#notification-member-list').innerHTML=members.map(m=>{
      const portrait=String(m.avatar_url||'');
      const safePortrait=/^\/api\/profile-images\/[a-f0-9]{32}\?v=[a-f0-9]{12}$/.test(portrait)?'https://stelchat.xyz'+portrait:'';
      return `<article class="notification-member mobile-member"><header class="notification-member-head"><span class="notification-member-copy"><span class="avatar" style="--avatar-color:${/^#[a-f0-9]{6}$/i.test(m.color)?m.color:'#788ce2'}">${safePortrait?`<img src="${escape(safePortrait)}" alt="" loading="lazy">`:escape(m.initials||m.name.slice(0,1))}</span><strong>${escape(m.name)}</strong></span><b>알림</b></header><div class="notification-matrix">${[['live','방송 시작'],['chat','새 채팅']].map(([kind,label])=>`<span>${label}</span><input type="checkbox" data-mobile-uid="${escape(m.uid)}" data-kind="${kind}" aria-label="${escape(m.name)} ${label} 알림" ${enabled(m.uid,kind)?'checked':''}>`).join('')}</div></article>`;
    }).join('');
    panel.querySelectorAll('[data-mobile-uid]').forEach(input=>input.addEventListener('change',()=>update(()=>window.stelchat.setMemberNotification(input.dataset.mobileUid,'desktop',input.dataset.kind,input.checked))));
    panel.querySelectorAll('[data-notification-bulk]').forEach(button=>{
      const all=members.length>0&&members.every(m=>enabled(m.uid,button.dataset.notificationEvent));
      button.classList.toggle('enabled',all);button.querySelector('small').textContent=all?'모두 끄기':'모두 켜기';
    });
  };
}
