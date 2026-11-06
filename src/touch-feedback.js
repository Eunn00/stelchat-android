// Android WebView may retain :hover/:active after a tap. Keep feedback bounded.
export function installTouchFeedback(){
  let target=null,timer;
  const clear=()=>{clearTimeout(timer);target?.classList.remove('touch-feedback');target=null;};
  document.addEventListener('pointerdown',event=>{
    if(event.pointerType!=='touch')return;clear();
    target=event.target.closest('.icon-button');if(!target)return;
    target.classList.add('touch-feedback');timer=setTimeout(clear,500);
  },{passive:true});
  document.addEventListener('pointerup',()=>{if(target){clearTimeout(timer);timer=setTimeout(clear,100);}},{passive:true});
  document.addEventListener('pointercancel',clear,{passive:true});
  document.addEventListener('visibilitychange',()=>{if(document.hidden)clear();});
}
