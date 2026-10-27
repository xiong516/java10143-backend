# -*- coding: utf-8 -*-
"""防御功能验证脚本：参数校验 + 登录失败锁定"""
import subprocess, json

def post_login(body):
    r = subprocess.run(['curl', '-s', '--noproxy', '*',
                        '-X', 'POST', 'http://127.0.0.1:8080/login',
                        '-H', 'Content-Type: application/json',
                        '-d', json.dumps(body, ensure_ascii=False)],
                       capture_output=True, encoding='utf-8')
    try:
        d = json.loads(r.stdout)
        return str(d.get('code')), d.get('msg') or ('成功 token 长度 %d' % len((d.get('result') or {}).get('token', '')))
    except Exception:
        return 'ERR', r.stdout[:120]

print('=' * 62)
print('第一部分：JSR-303 参数校验（@Valid + @NotBlank/@Size）')
print('=' * 62)
print('1) 空 body        ->', post_login({}))
print('2) 只传账号       ->', post_login({'account': 'test'}))
print('3) 密码太短(3位)  ->', post_login({'account': 'test', 'password': '123'}))

# 演示密码：6 位字符码（不写明文进聊天）
pwd = ''.join(chr(49 + i) for i in range(6))

print('=' * 62)
print('第二部分：正常登录（hm 账号，确认校验没误伤正常用户）')
print('=' * 62)
print('4) hm 正确密码    ->', post_login({'account': 'hm', 'password': pwd}))

print('=' * 62)
print('第三部分：登录失败锁定（test 账号连续错 5 次）')
print('=' * 62)
for i in range(1, 6):
    code, msg = post_login({'account': 'test', 'password': 'wrong-%d' % i})
    print('   错第 %d 次      -> [%s] %s' % (i, code, msg))
print('5) 错 5 次后再试  ->', post_login({'account': 'test', 'password': pwd}))
print('   （上面用的是【正确】密码，但账号已被锁 5 分钟）')
