# -*- coding: utf-8 -*-
"""Minimum viable seed - generate & execute SQL directly"""
import subprocess, random, os
random.seed(42)

SR = '王李张刘陈杨黄赵周吴徐孙马朱胡郭何高林罗郑梁谢宋唐韩曹许邓冯萧程蔡彭潘袁于董余叶蒋杜苏魏吕丁沈任姚卢傅钟崔廖谭汪范金石龚贾夏韦方邹熊孟秦阎薛侯雷白龙段郝孔邵史毛常万顾赖武康贺严尹钱施牛洪龚'
MA = '伟强磊洋超帆鹏鑫健昊明凯涛峰波斌翔浩毅宇杰俊辉刚勇军飞亮宏志文博睿晨旭瑞泽辰'
FA = '芳娜敏静婷雪蕾娟萍慧悦琳霞芸燕玲红梅丽洁丹琼文静雅琴思雨梦琪晓萱诗涵欣怡'
BC = '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W'
SM = '2025-2026-1'

def gn(g): return random.choice(SR) + random.choice(MA if g=='M' else FA)
def gp(): return f'1{random.choice([3,5,8])}{random.randint(10,99):02d}{random.randint(10000000,99999999):08d}'

def mysql(sql):
    env = os.environ.copy(); env['MYSQL_PWD'] = 'root'
    r = subprocess.run(['mysql','-u','root','--ssl-mode=DISABLED','--default-character-set=utf8mb4','course_selection_system_cloud'],
                      input=sql.encode('utf-8'), capture_output=True, env=env, timeout=30)
    if r.returncode != 0:
        e = r.stderr.decode('utf-8','replace')[:300]
        if 'Duplicate' not in e and 'already exists' not in e:
            print(f'  ⚠ {e}')

# Reset database first
with open(os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), 'db', 'init-microservice.sql'), 'r', encoding='utf-8') as f:
    mysql(f.read())

# Now truncate and insert our data
def exec_phase(name, sql_func):
    print(f'  [{name}]...', end='', flush=True)
    mysql(sql_func())
    print('OK')

exec_phase('清理', lambda: ';'.join(f'TRUNCATE TABLE {t}' for t in [
    'course_evaluation','course_selection','announcement','course','class_info',
    'major','department','college','teacher','student','admin',
    'sys_role_permission','sys_user_role','sys_permission','sys_role',
    'sys_user','system_config','system_log','semester']) + ';')

# Static data
static = (
    "INSERT INTO college VALUES(1,'计算机与信息技术学院','CS','',1,NOW(),NOW()),(2,'数学与统计学院','MATH','',1,NOW(),NOW()),(3,'外国语学院','FL','',1,NOW(),NOW()),(4,'经济管理学院','EM','',1,NOW(),NOW());"
    "INSERT INTO department VALUES(1,'CS01','计算机科学系',1,'',1,NOW(),NOW()),(2,'CS02','软件工程系',1,'',1,NOW(),NOW()),(3,'CS03','网络工程系',1,'',1,NOW(),NOW()),(4,'MATH01','数学系',2,'',1,NOW(),NOW()),(5,'MATH02','统计系',2,'',1,NOW(),NOW()),(6,'FL01','英语系',3,'',1,NOW(),NOW()),(7,'EM01','管理系',4,'',1,NOW(),NOW());"
    "INSERT INTO major VALUES(1,'CS0101','计算机科学与技术',1,'',1,NOW(),NOW()),(2,'CS0201','软件工程',2,'',1,NOW(),NOW()),(3,'CS0301','网络工程',3,'',1,NOW(),NOW()),(4,'MATH0101','数学与应用数学',4,'',1,NOW(),NOW()),(5,'MATH0201','统计学',5,'',1,NOW(),NOW()),(6,'FL0101','英语',6,'',1,NOW(),NOW()),(7,'EM0101','工商管理',7,'',1,NOW(),NOW());"
    "INSERT INTO sys_role VALUES(1,'管理员','ROLE_ADMIN','系统管理员',1,NOW(),NOW()),(2,'教师','ROLE_TEACHER','教师用户',1,NOW(),NOW()),(3,'学生','ROLE_STUDENT','学生用户',1,NOW(),NOW());"
    "INSERT INTO sys_permission VALUES(1,'用户管理','user:manage','/api/v1/users/**',NULL,3,NULL,NULL,0,1,NOW(),NOW()),(2,'课程管理','course:manage','/api/v1/courses/**',NULL,3,NULL,NULL,0,1,NOW(),NOW()),(3,'选课管理','selection:manage','/api/v1/selections/**',NULL,3,NULL,NULL,0,1,NOW(),NOW()),(4,'成绩管理','grade:manage','/api/v1/grades/**',NULL,3,NULL,NULL,0,1,NOW(),NOW()),(5,'系统管理','system:manage','/api/v1/system/**',NULL,3,NULL,NULL,0,1,NOW(),NOW());"
    "INSERT INTO sys_role_permission VALUES(1,1),(1,2),(1,3),(1,4),(1,5),(2,2),(2,4),(3,3);"
    "INSERT INTO semester VALUES(1,'2024-2025-1','2024-2025学年第一学期','2024-09-01','2025-01-15',0,1,NOW(),NOW()),(2,'2024-2025-2','2024-2025学年第二学期','2025-02-17','2025-07-05',0,1,NOW(),NOW()),(3,'2025-2026-1','2025-2026学年第一学期','2025-09-01','2026-01-15',1,1,NOW(),NOW());"
    "INSERT INTO system_config VALUES(1,'system.name','网上选课系统','系统名称',NOW(),NOW()),(2,'system.version','2.0.0','系统版本',NOW(),NOW()),(3,'contact.email','admin@example.com','联系邮箱',NOW(),NOW()),(4,'login.timeout','60','登录超时(分钟)',NOW(),NOW()),(5,'max.upload.size','50','最大上传大小(MB)',NOW(),NOW()),(6,'system.status','open','选课状态',NOW(),NOW()),(7,'grade.entry.status','open','成绩录入状态',NOW(),NOW()),(8,'max.course.selection','10','最大选课数量',NOW(),NOW()),(9,'system.notice','欢迎使用网上选课系统！','系统通知',NOW(),NOW());"
    f"INSERT INTO admin VALUES(1,'admin','{BC}','系统管理员','','admin',1,NOW(),NOW());"
)
mysql(static)

# --- Generate data in small batches ---
CL = [('CS2101','计科2101班',1,1),('CS2102','计科2102班',1,1),('CS2103','计科2103班',1,1),('CS2104','计科2104班',1,1),('CS2105','计科2105班',1,1),('SE2101','软工2101班',1,2),('SE2102','软工2102班',1,2),('SE2103','软工2103班',1,2),('NW2101','网工2101班',1,3),('NW2102','网工2102班',1,3),('MA2101','数学2101班',2,4),('MA2102','数学2102班',2,4),('ST2101','统计2101班',2,5),('EN2101','英语2101班',3,6),('EN2102','英语2102班',3,6),('EM2101','工商2101班',4,7),('EM2102','工商2102班',4,7)]

# Batch 1: Classes + Teachers
print('写入班级...', end=' ', flush=True)
used = set()
for i,(cd_,nm_,co,ma) in enumerate(CL):
    ht = gn('M')
    while ht in used: ht = gn('M')
    used.add(ht)
    mysql(f"INSERT INTO class_info VALUES({i+1},'{cd_}','{nm_}',{co},{ma},'2021级','{ht}',18,'normal',NOW(),NOW());")
print(f'{len(CL)}个')

print('写入教师...', end=' ', flush=True)
used.clear()
for did in range(1,8):
    for _ in range(3 if did<=4 else 2):
        t = 1 + sum(1 for _1 in range(1,did) for _2 in range(3 if _1<=4 else 2)) + _
        # simpler:
        # just use running count
pass

# Actually let me just do simple sequential
used.clear()
t = 0
for did in range(1,8):
    cnt = 3 if did<=4 else 2
    for _ in range(cnt):
        t += 1
        g = random.choice(['男','男','男','女','女'])
        n_ = gn('M' if g=='男' else 'F')
        while n_ in used: n_ = gn('M' if g=='男' else 'F')
        used.add(n_)
        col = {1:1,2:1,3:1,4:2,5:2,6:3,7:4}[did]
        mysql(f"INSERT INTO teacher VALUES({t},'T{t:03d}','{n_}','{g}','{gp()}','t{t:03d}@edu.cn','{BC}','{random.choice(['教授','副教授','讲师','助教'])}',{col},{did},'2024-09-01',1,NOW(),NOW());")
print(f'{t}个')

# Batch 2: Students (240)
print('写入学生...', flush=True)
used.clear()
s = 0
cnts = [15,15,15,15,15,14,14,14,14,14,14,14,14,14,14,14,11]
for ci,(_,_,co,ma) in enumerate(CL):
    for _ in range(cnts[ci]):
        s += 1
        g = random.choice(['男','男','男','女','女'])
        n_ = gn('M' if g=='男' else 'F')
        while n_ in used: n_ = gn('M' if g=='男' else 'F')
        used.add(n_)
        sn = f'2024{s:03d}'
        mysql(f"INSERT INTO student VALUES({s},'{sn}','{n_}','{'男' if g=='男' else '女'}','{gp()}','{sn}@student.edu.cn','{BC}',{co},{ma},{ci+1},'2024-09-01',1,NOW(),NOW());")
        if s % 30 == 0: print(f'  {s}/240', flush=True)
print(f'  学生: {s}')

# Batch 3: Courses (40)
print('写入课程...', end=' ', flush=True)
used_slots = set()
CO = [('CS101','数据结构与算法','专业必修',4.0,64),('CS102','操作系统原理','专业必修',3.5,56),('CS103','计算机组成原理','专业必修',4.0,64),('CS104','编译原理','专业必修',3.5,56),('CS105','计算机网络','专业必修',3.5,56),('CS106','数据库系统概论','专业必修',3.5,56),('CS107','软件工程导论','专业必修',3.0,48),('CS108','面向对象程序设计','专业必修',3.0,48),('CS109','Web前端开发技术','专业选修',2.5,40),('CS110','Python程序设计','专业选修',2.5,40),('CS201','人工智能导论','专业选修',2.5,40),('CS202','机器学习基础','专业选修',2.5,40),('CS203','深度学习','专业选修',3.0,48),('CS204','大数据技术','专业选修',3.0,48),('CS205','云计算技术','专业选修',2.5,40),('CS206','网络安全技术','专业选修',2.5,40),('CS207','移动应用开发','专业选修',2.5,40),('CS208','软件测试技术','专业选修',2.0,32),('MA101','高等数学A','公共必修',5.0,80),('MA102','高等数学B','公共必修',4.0,64),('MA103','线性代数','公共必修',3.0,48),('MA104','概率论与数理统计','公共必修',3.5,56),('MA105','离散数学','专业必修',3.0,48),('MA106','数学建模','专业选修',2.5,40),('PH101','大学物理A','公共必修',4.0,64),('PH102','大学物理实验','公共必修',1.5,32),('EN101','大学英语I','公共必修',4.0,64),('EN102','大学英语II','公共必修',4.0,64),('EN103','大学英语III','公共必修',3.0,48),('PE101','体育I','公共必修',1.0,32),('PE102','体育II','公共必修',1.0,32),('PE103','体育III','公共必修',1.0,32),('CN101','思想道德与法治','公共必修',3.0,48),('CN102','中国近现代史纲要','公共必修',3.0,48),('CN103','马克思主义基本原理','公共必修',3.0,48),('CN104','毛泽东思想概论','公共必修',3.0,48),('EC101','经济学原理','公共选修',2.0,32),('MG101','管理学原理','公共选修',2.0,32),('LA101','大学生心理健康教育','公共必修',2.0,32),('LA102','职业生涯规划','公共必修',1.0,16)]
TM = ['周一 1-2节','周一 3-4节','周一 5-6节','周二 1-2节','周二 3-4节','周二 5-6节','周三 1-2节','周三 3-4节','周三 5-6节','周四 1-2节','周四 3-4节','周四 5-6节','周五 1-2节','周五 3-4节','周五 5-6节']
RM = ['教学楼101','教学楼102','教学楼201','教学楼202','教学楼301','教学楼302','教学楼401','教学楼402','实验楼201','实验楼202','实验楼301','实验楼302','实验楼401','实验楼402','逸夫楼101','逸夫楼201']

for i,(cd_,nm_,ct,cr,hr) in enumerate(CO):
    c = i+1
    t = random.randint(1,12) if cd_[:2] in('CS','MA','PH') else (random.randint(13,14) if cd_[:2]=='EN' else random.randint(15,18))
    sl = random.choice(TM)
    while sl in used_slots: sl = random.choice(TM)
    used_slots.add(sl)
    sts = 1 if random.random()<0.85 else 0
    mysql(f"INSERT INTO course VALUES({c},'{cd_}','{nm_}','{ct}',{cr},{hr},{t},'{SM}','{sl}','{random.choice(RM)}',{random.choice([30,35,40,45,50,60,80,100])},0,{sts},'本课程介绍{nm_}的基本原理和方法。',NOW(),NOW());")
print(f'{len(CO)}门')

# Batch 4: Selections
print('写入选课...', flush=True)
sel_count = 0
for sid in range(1, 241):
    for cid in random.sample(range(1,41), 1 if random.random()<0.3 else 2):
        sel_count += 1
        sts = random.choice([0,0,0,0,2])
        if sts == 2:
            dg = round(random.uniform(55,100),1); lg = round(random.uniform(55,100),1); eg = round(random.uniform(45,100),1)
            tt = round(dg*0.4+lg*0.2+eg*0.4, 1)
            sl_ = '优秀' if tt>=90 else ('良好' if tt>=80 else ('中等' if tt>=70 else ('及格' if tt>=60 else '不及格')))
            gpa = round(max(0,min(5.0,(tt-50)/10)),1)
            mysql(f"INSERT INTO course_selection VALUES({sel_count},{sid},{cid},'{SM}',{sts},{tt},'{sl_}',{gpa},{dg},{lg},{eg},NOW(),NULL,NOW(),NOW());")
        else:
            mysql(f"INSERT INTO course_selection VALUES({sel_count},{sid},{cid},'{SM}',{sts},NULL,NULL,NULL,NULL,NULL,NULL,NOW(),NULL,NOW(),NOW());")
    if sel_count % 60 == 0: print(f'  {sel_count}条', flush=True)
print(f'  选课: {sel_count}')

# Batch 5: Simple user data
print('写入用户数据...', flush=True)
for i in range(18):
    mysql(f"INSERT INTO sys_user VALUES({i+2},'T{i+1:03d}','{BC}','教师{i+1}',2,'T{i+1:03d}',1,1,NOW(),NOW());")
for i in range(240):
    sn = f'2024{i+1:03d}'
    mysql(f"INSERT INTO sys_user VALUES({i+22},'{sn}','{BC}','学生{i+1}',1,'{sn}',1,1,NOW(),NOW());")
# admin already inserted
mysql("INSERT INTO sys_user VALUES(1,'admin','{BC}','系统管理员',3,'ADMIN001',NULL,1,NOW(),NOW());")
mysql("INSERT INTO sys_user_role VALUES(1,1);")
for i in range(2,22): mysql(f"INSERT INTO sys_user_role VALUES({i},2);")
for i in range(22,262): mysql(f"INSERT INTO sys_user_role VALUES({i},3);")
mysql('SET FOREIGN_KEY_CHECKS = 1;')

print(f'\n✅ 完成! 班级:17 教师:18 学生:240 课程:40 选课:{sel_count}')
