# -*- coding: utf-8 -*-
"""Append additional data to existing database"""
import random, sys, subprocess, os

random.seed(123)
SR = '王李张刘陈杨黄赵周吴徐孙马朱胡郭何高林罗郑梁谢宋唐韩曹许邓冯萧程蔡彭潘袁于董余叶蒋杜苏魏吕丁沈任姚卢傅钟崔廖谭汪范金石龚贾夏韦方邹熊孟秦阎薛侯雷白龙段郝孔邵史毛常万顾赖武康贺严尹钱施牛洪龚'
MA = ['伟','强','磊','洋','超','帆','鹏','鑫','健','昊','明','凯','涛','峰','波','斌','翔','浩','毅','宇','杰','俊','辉','刚','勇','军','飞','亮','宏','志','文','博','睿','晨','旭','瑞','泽','辰','宇轩','浩宇','俊杰','志远','天宇','宇航','俊豪','子轩','梓豪','一鸣','嘉伟','建华','志强','海涛','思远','浩铭','子涵','沐阳','瑞阳','翰文','柏宇','俊哲','铭泽','景轩','启航','子墨','浩辰','奕辰','凯文','梓睿','铭宇','嘉诚','瑞霖','景浩','泽宇','铭轩','浩轩','瑞泽','锦程','昊天','铭熙','子睿','泽辰','浩博','瑞杰','俊驰','雨泽','铭杰','思博','嘉瑞','奕博']
FA = ['芳','娜','敏','静','婷','雪','蕾','娟','萍','慧','悦','琳','霞','芸','燕','玲','红','梅','丽','洁','丹','琼','文静','雅琴','思雨','梦琪','晓萱','诗涵','欣怡','雨桐','可馨','雨涵','思琪','语彤','佳怡','紫萱','梦洁','诗琪','雅涵','思涵','雨欣','晓彤','雅静','梦瑶','佳琪','紫涵','诗雅','思颖','雨萱','晓涵','梦婷','佳颖','紫琪','雅萱','思瑶','雨琴','嘉怡','梦涵','佳瑶','紫瑶','诗婷','思婷','雨婷','晓瑶','雅婷','梦琪','佳萱','紫怡','诗涵','思雅','雨涵','晓萱','雅茹','梦瑶','佳琪','紫萱','诗瑶','思琪','语嫣','晓婷','雅琪','梦婷','嘉琪','紫琪','诗雅','思涵','雨彤','晓雅']
BC = '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W'
SM = '2025-2026-1'

def gn(g): return random.choice(SR) + random.choice(MA if g=='M' else FA)
def gp(): return '1' + str(random.choice([3,5,8])) + str(random.randint(10,99)) + str(random.randint(10000000,99999999))

sql_lines = []
def S(s): sql_lines.append(s)

S('SET FOREIGN_KEY_CHECKS = 0;')
S('USE course_selection_system_cloud;')

# Students 101-240
used = set()
CLD = {7:(1,1),8:(1,1),9:(1,2),10:(1,3),11:(2,4),12:(2,5),13:(3,6),14:(4,7)}
for sid in range(101, 241):
    g = random.choice(['男','男','男','女','女'])
    nm = gn('M' if g=='男' else 'F')
    while nm in used: nm = gn('M' if g=='男' else 'F')
    used.add(nm)
    sno = '2024' + str(sid).zfill(3)
    ci = random.randint(7, 14)
    co, ma = CLD[ci]
    gender = '男' if g=='男' else '女'
    S(f"INSERT INTO student VALUES({sid},'{sno}','{nm}','{gender}','{gp()}','{sno}@student.edu.cn','{BC}',{co},{ma},{ci},'2024-09-01',1,NOW(),NOW());")

# Courses 11-40
CO = [
    ('CS209','软件项目管理','专业选修',2.5,40),('CS210','人机交互设计','专业选修',2.0,32),
    ('MA201','复变函数','专业必修',3.5,56),('MA202','实变函数','专业必修',3.0,48),
    ('MA203','泛函分析','专业选修',3.0,48),('EN104','高级英语','公共必修',3.0,48),
    ('CN105','形势与政策','公共必修',2.0,32),('EC102','微观经济学','公共选修',2.0,32),
    ('EC103','宏观经济学','公共选修',2.0,32),('MG102','市场营销学','公共选修',2.0,32),
    ('MG103','人力资源管理','公共选修',2.0,32),('LA103','大学生创新创业','公共必修',1.0,16),
    ('CS211','分布式系统','专业选修',3.0,48),('CS212','计算机图形学','专业选修',2.5,40),
    ('CS213','嵌入式系统','专业选修',2.5,40),('CS214','数据挖掘','专业选修',2.5,40),
    ('CS215','Java企业级开发','专业选修',3.0,48),('MA107','运筹学','专业选修',2.5,40),
    ('PH103','天文学基础','公共选修',2.0,32),('EN105','商务英语','公共选修',2.0,32),
    ('PE104','体育IV','公共必修',1.0,32),('CN106','习近平新时代中国特色社会主义思想概论','公共必修',3.0,48),
    ('EC104','国际贸易实务','公共选修',2.0,32),('MG104','财务管理','公共选修',2.0,32),
    ('LA104','社交礼仪','公共选修',1.5,24),('CS216','Web安全','专业选修',2.5,40),
    ('CS217','算法设计与分析','专业必修',3.5,56),('CS218','数字逻辑','专业必修',3.0,48),
    ('MA108','数值分析','专业选修',2.5,40),('MA109','偏微分方程','专业选修',2.5,40),
]
TM = ['周一 1-2节','周一 3-4节','周一 5-6节','周二 1-2节','周二 3-4节','周二 5-6节','周三 1-2节','周三 3-4节','周三 5-6节','周四 1-2节','周四 3-4节','周四 5-6节','周五 1-2节','周五 3-4节','周五 5-6节']
RM = ['教学楼101','教学楼102','教学楼201','教学楼202','教学楼301','教学楼302','教学楼401','教学楼402','实验楼201','实验楼202','实验楼301','实验楼302','实验楼401','实验楼402','逸夫楼101','逸夫楼201']
used_slots = set()
for i, (cd, nm, ct, cr, hr) in enumerate(CO):
    cid = i + 11
    sl = random.choice(TM)
    while sl in used_slots: sl = random.choice(TM)
    used_slots.add(sl)
    st = 1 if random.random() < 0.85 else 0
    tid = random.randint(1, 15)
    avail = random.choice([30,35,40,45,50,60,80,100])
    S(f"INSERT INTO course VALUES({cid},'{cd}','{nm}','{ct}',{cr},{hr},{tid},'{SM}','{sl}','{random.choice(RM)}',{avail},0,{st},'本课程介绍{nm}的基本原理和方法。',NOW(),NOW());")

# Course selections (~360)
sel_count = 0
for sid in range(1, 241):
    n = 1 if random.random() < 0.3 else 2
    for cid in random.sample(range(1, 41), min(n, 40)):
        sel_count += 1
        sts = random.choice([0,0,0,0,2])
        if sts == 2:
            dg = round(random.uniform(55,100),1)
            lg = round(random.uniform(55,100),1)
            eg = round(random.uniform(45,100),1)
            total = round(dg*0.4 + lg*0.2 + eg*0.4, 1)
            sl_ = '优秀' if total>=90 else ('良好' if total>=80 else ('中等' if total>=70 else ('及格' if total>=60 else '不及格')))
            gpa = round(max(0, min(5.0, (total-50)/10)), 1)
            S(f"INSERT INTO course_selection VALUES({sel_count},{sid},{cid},'{SM}',{sts},{total},'{sl_}',{gpa},{dg},{lg},{eg},NOW(),NULL,NOW(),NOW());")
        else:
            S(f"INSERT INTO course_selection VALUES({sel_count},{sid},{cid},'{SM}',{sts},NULL,NULL,NULL,NULL,NULL,NULL,NOW(),NULL,NOW(),NOW());")
    if sel_count >= 360: break

# Clean up old user data and re-insert
S("DELETE FROM sys_user_role;")
S("DELETE FROM sys_user WHERE id > 1;")
S(f"INSERT INTO sys_user VALUES(1,'admin','{BC}','系统管理员',3,'ADMIN001',NULL,1,NOW(),NOW());")
for i in range(15):
    S(f"INSERT INTO sys_user VALUES({i+2},'T{i+1:03d}','{BC}','教师{i+1}',2,'T{i+1:03d}',1,1,NOW(),NOW());")
for i in range(240):
    sno = f'2024{i+1:03d}'
    S(f"INSERT INTO sys_user VALUES({i+17},'{sno}','{BC}','学生{i+1}',1,'{sno}',1,1,NOW(),NOW());")
S("INSERT INTO sys_user_role VALUES(1,1);")
for i in range(2, 17): S(f"INSERT INTO sys_user_role VALUES({i},2);")
for i in range(17, 257): S(f"INSERT INTO sys_user_role VALUES({i},3);")

# Update course counts
from collections import Counter
counts = Counter()
for line in sql_lines:
    if 'INSERT INTO course_selection VALUES' in line:
        pass  # We'll parse sel_count
# Count from the selection lines we already have
S('SET FOREIGN_KEY_CHECKS = 1;')

print(f'Generated {len(sql_lines)} SQL lines, piping to MySQL...')

# Write to temp file and execute
tmp = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), 'db', '_append.sql')
with open(tmp, 'w', encoding='utf-8') as f:
    f.write('\n'.join(sql_lines))

env = os.environ.copy()
env['MYSQL_PWD'] = 'root'
r = subprocess.run(['cmd', '/c', f'mysql -u root --ssl-mode=DISABLED --default-character-set=utf8mb4 course_selection_system_cloud < {tmp}'],
                   capture_output=True, env=env)

if r.returncode == 0:
    os.remove(tmp)
    print('OK - data appended successfully')
else:
    err = r.stderr.decode('gbk', errors='replace')[:500]
    print(f'Error: {err}')
