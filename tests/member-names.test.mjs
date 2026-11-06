import test from 'node:test';import assert from 'node:assert/strict';import {normalizeMember} from '../src/member-names.js';
test('known member names use consistent spacing and preserve identity',()=>{
 const source={uid:'example',name:'토키노메리'};assert.deepEqual(normalizeMember(source),{...source,name:'토키노 메리'});assert.equal(source.name,'토키노메리');
 assert.equal(normalizeMember({name:'네네코마시로'}).name,'네네코 마시로');assert.equal(normalizeMember({name:'새 멤버'}).name,'새 멤버');
});
