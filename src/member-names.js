const names=['강지','아야츠노 유니','사키하네 후야','시라유키 히나','네네코 마시로','아카네 리제','아라하시 타비','텐코 시부키','아오쿠모 린','하나코 나나','유즈하 리코','시즈키 토아','코가네 랑','하나요 히요리','토키노 메리'];
export function normalizeMember(member){
 const key=String(member.name||'').replace(/\s/g,'');
 const name=names.find(n=>n.replace(/\s/g,'')===key);
 return name?{...member,name}:member;
}
