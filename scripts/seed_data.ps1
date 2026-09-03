#Requires -Version 5.1
# 生成真实感数据并直接写入 MySQL

$ErrorActionPreference = 'Stop'
$env:MYSQL_PWD = 'root'
$MYSQL = 'mysql -u root --ssl-mode=DISABLED --default-character-set=utf8mb4 course_selection_system_cloud'
$BCRYPT = '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W'
$SEM = '2025-2026-1'

$SUR = '王李张刘陈杨黄赵周吴徐孙马朱胡郭何高林罗郑梁谢宋唐韩曹许邓冯萧程蔡彭潘袁于董余叶蒋杜苏魏吕丁沈任姚卢傅钟崔廖谭汪范金石龚贾夏韦方邹熊孟秦阎薛侯雷白龙段郝孔邵史毛常万顾赖武康贺严尹钱施牛洪龚'
$MG = @('伟','强','磊','洋','超','帆','鹏','鑫','健','昊','明','凯','涛','峰','波','斌','翔','浩','毅','宇','杰','俊','辉','刚','勇','军','飞','亮','宏','志','文','博','睿','晨','旭','瑞','泽','辰','宇轩','浩宇','俊杰','志远','天宇','宇航','俊豪','子轩','梓豪','一鸣','嘉伟','建华','志强','海涛','思远','浩铭','子涵','沐阳','瑞阳','翰文','柏宇','俊哲','铭泽','景轩','启航','子墨','浩辰','奕辰','凯文','梓睿','铭宇','嘉诚','瑞霖','景浩','泽宇','铭轩','浩轩','瑞泽','锦程','昊天','铭熙','子睿','泽辰','浩博','瑞杰','俊驰','雨泽','铭杰','思博','嘉瑞','奕博')
$FG = @('芳','娜','敏','静','婷','雪','蕾','娟','萍','慧','悦','琳','霞','芸','燕','玲','红','梅','丽','洁','丹','琼','文静','雅琴','思雨','梦琪','晓萱','诗涵','欣怡','雨桐','可馨','雨涵','思琪','语彤','佳怡','紫萱','梦洁','诗琪','雅涵','思涵','雨欣','晓彤','雅静','梦瑶','佳琪','紫涵','诗雅','思颖','雨萱','晓涵','梦婷','佳颖','紫琪','雅萱','思瑶','雨琴','嘉怡','梦涵','佳瑶','紫瑶','诗婷','思婷','雨婷','晓瑶','雅婷','梦琪','佳萱','紫怡','诗涵','思雅','雨涵','晓萱','雅茹','梦瑶','佳琪','紫萱','诗瑶','思琪','语嫣','晓婷','雅琪','梦婷','嘉琪','紫琪','诗雅','思涵','雨彤','晓雅')
$TITLES = @('教授','副教授','讲师','助教','高级讲师','特聘教授')
$HP = @('老师讲解清晰，课程内容丰富，收获很大。','课程安排合理，理论与实践结合得很好。','教学态度认真，案例生动有趣。','非常好的课程，对后续学习帮助很大。','老师经验丰富，课堂气氛活跃。','课程难度适中，适合本阶段学习。','实验环节设计得很好，动手能力得到提升。','课程内容前沿，紧跟行业发展趋势。','教学方式新颖，深受学生欢迎。','老师认真负责，答疑及时。','课程实用性强，对就业有帮助。','考核方式合理，能真实反映学习水平。')
$TIMES = @('周一 1-2节','周一 3-4节','周一 5-6节','周二 1-2节','周二 3-4节','周二 5-6节','周三 1-2节','周三 3-4节','周三 5-6节','周四 1-2节','周四 3-4节','周四 5-6节','周五 1-2节','周五 3-4节','周五 5-6节')
$ROOMS = @('教学楼101','教学楼102','教学楼201','教学楼202','教学楼301','教学楼302','教学楼401','教学楼402','实验楼201','实验楼202','实验楼301','实验楼302','实验楼401','实验楼402','逸夫楼101','逸夫楼201')

$r = [Random]::new(42)

function GenName([string]$gender) {
    $s = $SUR[$r.Next(0, $SUR.Length)]
    if (-not $gender) { $gender = if ($r.Next(0,2) -eq 0) { 'M' } else { 'F' } }
    $g = if ($gender -eq 'M') { $MG } else { $FG }
    return $s + $g[$r.Next(0, $g.Length)]
}

function GenPhone {
    return '1' + @(3,5,8)[$r.Next(0,3)] + $r.Next(10,99).ToString('00') + $r.Next(10000000,99999999).ToString('00000000')
}

# ========== Step 1: Reset DB with init script ==========
Write-Host '[1/3] 重置数据库...' -ForegroundColor Cyan
$initBytes = [System.IO.File]::ReadAllBytes("$PSScriptRoot\..\db\init-microservice.sql")
$utf8Bytes = [System.Text.Encoding]::Convert([System.Text.Encoding]::UTF8, [System.Text.Encoding]::UTF8, $initBytes)
[System.IO.File]::WriteAllBytes("$PSScriptRoot\..\db\_init_utf8.sql", $utf8Bytes)
cmd /c "$env:MYSQL_PWD='root' 2>nul && mysql -u root --ssl-mode=DISABLED --default-character-set=utf8mb4 course_selection_system_cloud < $PSScriptRoot\..\db\_init_utf8.sql" 2>$null
Remove-Item "$PSScriptRoot\..\db\_init_utf8.sql" -Force -ErrorAction SilentlyContinue
Write-Host '  [OK] 数据库已重置' -ForegroundColor Green

# ========== Step 2: Build INSERT SQL ==========
Write-Host '[2/3] 生成真实数据...' -ForegroundColor Cyan
$sql = New-Object System.Text.StringBuilder
$sql.AppendLine('SET NAMES utf8mb4;')
$sql.AppendLine('SET FOREIGN_KEY_CHECKS = 0;')
$sql.AppendLine('USE course_selection_system_cloud;')

# Truncate
foreach ($t in @('course_evaluation','course_selection','announcement','course','class_info','major','department','college','teacher','student','admin','sys_role_permission','sys_user_role','sys_permission','sys_role','sys_user','system_config','system_log','semester')) {
    $sql.AppendLine("TRUNCATE TABLE $t;")
}

# Static data
$sql.AppendLine("INSERT INTO college VALUES(1,'计算机与信息技术学院','CS','',1,NOW(),NOW()),(2,'数学与统计学院','MATH','',1,NOW(),NOW()),(3,'外国语学院','FL','',1,NOW(),NOW()),(4,'经济管理学院','EM','',1,NOW(),NOW());")
$sql.AppendLine("INSERT INTO department VALUES(1,'CS01','计算机科学系',1,'',1,NOW(),NOW()),(2,'CS02','软件工程系',1,'',1,NOW(),NOW()),(3,'CS03','网络工程系',1,'',1,NOW(),NOW()),(4,'MATH01','数学系',2,'',1,NOW(),NOW()),(5,'MATH02','统计系',2,'',1,NOW(),NOW()),(6,'FL01','英语系',3,'',1,NOW(),NOW()),(7,'EM01','管理系',4,'',1,NOW(),NOW());")
$sql.AppendLine("INSERT INTO major VALUES(1,'CS0101','计算机科学与技术',1,'',1,NOW(),NOW()),(2,'CS0201','软件工程',2,'',1,NOW(),NOW()),(3,'CS0301','网络工程',3,'',1,NOW(),NOW()),(4,'MATH0101','数学与应用数学',4,'',1,NOW(),NOW()),(5,'MATH0201','统计学',5,'',1,NOW(),NOW()),(6,'FL0101','英语',6,'',1,NOW(),NOW()),(7,'EM0101','工商管理',7,'',1,NOW(),NOW());")
$sql.AppendLine("INSERT INTO sys_role VALUES(1,'管理员','ROLE_ADMIN','系统管理员',1,NOW(),NOW()),(2,'教师','ROLE_TEACHER','教师用户',1,NOW(),NOW()),(3,'学生','ROLE_STUDENT','学生用户',1,NOW(),NOW());")
$sql.AppendLine("INSERT INTO sys_permission VALUES(1,'用户管理','user:manage','/api/v1/users/**',NULL,3,NULL,NULL,0,1,NOW(),NOW()),(2,'课程管理','course:manage','/api/v1/courses/**',NULL,3,NULL,NULL,0,1,NOW(),NOW()),(3,'选课管理','selection:manage','/api/v1/selections/**',NULL,3,NULL,NULL,0,1,NOW(),NOW()),(4,'成绩管理','grade:manage','/api/v1/grades/**',NULL,3,NULL,NULL,0,1,NOW(),NOW()),(5,'系统管理','system:manage','/api/v1/system/**',NULL,3,NULL,NULL,0,1,NOW(),NOW());")
$sql.AppendLine("INSERT INTO sys_role_permission VALUES(1,1),(1,2),(1,3),(1,4),(1,5),(2,2),(2,4),(3,3);")
$sql.AppendLine("INSERT INTO semester VALUES(1,'2024-2025-1','2024-2025学年第一学期','2024-09-01','2025-01-15',0,1,NOW(),NOW()),(2,'2024-2025-2','2024-2025学年第二学期','2025-02-17','2025-07-05',0,1,NOW(),NOW()),(3,'2025-2026-1','2025-2026学年第一学期','2025-09-01','2026-01-15',1,1,NOW(),NOW());")
$sql.AppendLine("INSERT INTO system_config VALUES(1,'system.name','网上选课系统','系统名称',NOW(),NOW()),(2,'system.version','2.0.0','系统版本',NOW(),NOW()),(3,'contact.email','admin@example.com','联系邮箱',NOW(),NOW()),(4,'login.timeout','60','登录超时(分钟)',NOW(),NOW()),(5,'max.upload.size','50','最大上传大小(MB)',NOW(),NOW()),(6,'system.status','open','选课状态',NOW(),NOW()),(7,'grade.entry.status','open','成绩录入状态',NOW(),NOW()),(8,'max.course.selection','10','最大选课数量',NOW(),NOW()),(9,'system.notice','欢迎使用网上选课系统！','系统通知',NOW(),NOW());")
$sql.AppendLine("INSERT INTO admin VALUES(1,'admin','$BCRYPT','系统管理员','','admin',1,NOW(),NOW());")

# Classes (17)
$used = @{}
$clRows = @()
$classTpl = @(
    @('CS2101','计科2101班',1,1),@('CS2102','计科2102班',1,1),@('CS2103','计科2103班',1,1),@('CS2104','计科2104班',1,1),@('CS2105','计科2105班',1,1),
    @('SE2101','软工2101班',1,2),@('SE2102','软工2102班',1,2),@('SE2103','软工2103班',1,2),
    @('NW2101','网工2101班',1,3),@('NW2102','网工2102班',1,3),
    @('MA2101','数学2101班',2,4),@('MA2102','数学2102班',2,4),@('ST2101','统计2101班',2,5),
    @('EN2101','英语2101班',3,6),@('EN2102','英语2102班',3,6),
    @('EM2101','工商2101班',4,7),@('EM2102','工商2102班',4,7)
)
for ($i = 0; $i -lt $classTpl.Length; $i++) {
    $ht = GenName 'M'; while ($used.ContainsKey($ht)) { $ht = GenName 'M' }; $used[$ht] = $true
    $clRows += "($($i+1),'$($classTpl[$i][0])','$($classTpl[$i][1])',$($classTpl[$i][2]),$($classTpl[$i][3]),'2021级','$ht',18,'normal',NOW(),NOW())"
}
$sql.AppendLine("INSERT INTO class_info VALUES`n$($clRows -join ",`n");")

# Teachers (18)
$used.Clear()
$teRows = @()
$tid = 1
foreach ($did in 1..7) {
    $cnt = if ($did -le 4) { 3 } else { 2 }
    for ($j = 0; $j -lt $cnt; $j++) {
        $g = @('男','男','男','女','女')[$r.Next(0,5)]
        $nm = GenName $(if ($g -eq '男') { 'M' } else { 'F' })
        while ($used.ContainsKey($nm)) { $nm = GenName $(if ($g -eq '男') { 'M' } else { 'F' }) }
        $used[$nm] = $true
        $cid = @{1=1;2=1;3=1;4=2;5=2;6=3;7=4}[$did]
        $hy = $r.Next(2015,2025)
        $teRows += "($tid,'T$($tid.ToString('000'))','$nm','$g','$(GenPhone)','t$($tid.ToString('000'))@edu.cn','$BCRYPT','$($TITLES[$r.Next(0,$TITLES.Length)])',$cid,$did,'$hy-$($r.Next(1,13).ToString('00'))-$($r.Next(1,29).ToString('00'))',1,NOW(),NOW())"
        $tid++
    }
}
$sql.AppendLine("INSERT INTO teacher VALUES`n$($teRows -join ",`n");")

# Students (240)
$used.Clear()
$stRows = @()
$sid = 1
$cnts = @(15,15,15,15,15,14,14,14,14,14,14,14,14,14,14,14,11)
for ($ci = 0; $ci -lt $classTpl.Length; $ci++) {
    for ($j = 0; $j -lt $cnts[$ci]; $j++) {
        $g = @('男','男','男','女','女')[$r.Next(0,5)]
        $nm = GenName $(if ($g -eq '男') { 'M' } else { 'F' })
        while ($used.ContainsKey($nm)) { $nm = GenName $(if ($g -eq '男') { 'M' } else { 'F' }) }
        $used[$nm] = $true
        $sno = "2024$($sid.ToString('000'))"
        $stRows += "($sid,'$sno','$nm','$(if ($g -eq '男') { '男' } else { '女' })','$(GenPhone)','$sno@student.edu.cn','$BCRYPT',$($classTpl[$ci][2]),$($classTpl[$ci][3]),$($ci+1),'2024-09-01',1,NOW(),NOW())"
        $sid++
    }
}
$sql.AppendLine("INSERT INTO student VALUES`n$($stRows -join ",`n");")

# Courses (40)
$CO = @(
    @('CS101','数据结构与算法','专业必修',4.0,64),@('CS102','操作系统原理','专业必修',3.5,56),@('CS103','计算机组成原理','专业必修',4.0,64),@('CS104','编译原理','专业必修',3.5,56),@('CS105','计算机网络','专业必修',3.5,56),
    @('CS106','数据库系统概论','专业必修',3.5,56),@('CS107','软件工程导论','专业必修',3.0,48),@('CS108','面向对象程序设计','专业必修',3.0,48),@('CS109','Web前端开发技术','专业选修',2.5,40),@('CS110','Python程序设计','专业选修',2.5,40),
    @('CS201','人工智能导论','专业选修',2.5,40),@('CS202','机器学习基础','专业选修',2.5,40),@('CS203','深度学习','专业选修',3.0,48),@('CS204','大数据技术','专业选修',3.0,48),@('CS205','云计算技术','专业选修',2.5,40),
    @('CS206','网络安全技术','专业选修',2.5,40),@('CS207','移动应用开发','专业选修',2.5,40),@('CS208','软件测试技术','专业选修',2.0,32),
    @('MA101','高等数学A','公共必修',5.0,80),@('MA102','高等数学B','公共必修',4.0,64),@('MA103','线性代数','公共必修',3.0,48),@('MA104','概率论与数理统计','公共必修',3.5,56),@('MA105','离散数学','专业必修',3.0,48),@('MA106','数学建模','专业选修',2.5,40),
    @('PH101','大学物理A','公共必修',4.0,64),@('PH102','大学物理实验','公共必修',1.5,32),
    @('EN101','大学英语I','公共必修',4.0,64),@('EN102','大学英语II','公共必修',4.0,64),@('EN103','大学英语III','公共必修',3.0,48),
    @('PE101','体育I','公共必修',1.0,32),@('PE102','体育II','公共必修',1.0,32),@('PE103','体育III','公共必修',1.0,32),
    @('CN101','思想道德与法治','公共必修',3.0,48),@('CN102','中国近现代史纲要','公共必修',3.0,48),@('CN103','马克思主义基本原理','公共必修',3.0,48),@('CN104','毛泽东思想概论','公共必修',3.0,48),
    @('EC101','经济学原理','公共选修',2.0,32),@('MG101','管理学原理','公共选修',2.0,32),@('LA101','大学生心理健康教育','公共必修',2.0,32),@('LA102','职业生涯规划','公共必修',1.0,16)
)
$usedSlots = @{}
$coRows = @()
for ($i = 0; $i -lt $CO.Length; $i++) {
    $cid = $i + 1
    $code = $CO[$i][0]
    $nm = $CO[$i][1]
    $tidC = if ($code -match '^(CS|MA|PH)') { $r.Next(1,13) } elseif ($code -match '^EN') { $r.Next(13,15) } else { $r.Next(15,19) }
    $slot = $TIMES[$r.Next(0,$TIMES.Length)]
    while ($usedSlots.ContainsKey($slot)) { $slot = $TIMES[$r.Next(0,$TIMES.Length)] }
    $usedSlots[$slot] = $true
    $st = if ($r.NextDouble() -lt 0.85) { 1 } else { 0 }
    $coRows += "($cid,'$code','$nm','$($CO[$i][2])',$($CO[$i][3]),$($CO[$i][4]),$tidC,'$SEM','$slot','$($ROOMS[$r.Next(0,$ROOMS.Length)])',$($r.Next(30,101,35,40,45,50,60,80,100)),0,$st,'本课程介绍$nm的基本原理和方法。',NOW(),NOW())"
}
$sql.AppendLine("INSERT INTO course VALUES`n$($coRows -join ",`n");")

# Selections (~240)
$selRows = @()
$completed = @()
$sid = 1
$courseIds = 1..40
:outer while ($sid -le 240) {
    $n = if ($r.NextDouble() -lt 0.3) { 1 } else { 2 }
    $sampled = @()
    $temp = $courseIds.Clone()
    for ($k = 0; $k -lt $n; $k++) {
        $idx = $r.Next(0, $temp.Length)
        $sampled += $temp[$idx]
        $temp = $temp | Where-Object { $_ -ne $temp[$idx] }
    }
    foreach ($cid in $sampled) {
        $idx = $selRows.Length + 1
        $sts = @(0,0,0,0,2)[$r.Next(0,5)]
        if ($sts -eq 2) {
            $dg = [Math]::Round($r.NextDouble()*45+55,1)
            $lg = [Math]::Round($r.NextDouble()*45+55,1)
            $eg = [Math]::Round($r.NextDouble()*55+45,1)
            $total = [Math]::Round($dg*0.4+$lg*0.2+$eg*0.4,1)
            $sl = if ($total -ge 90) { '优秀' } elseif ($total -ge 80) { '良好' } elseif ($total -ge 70) { '中等' } elseif ($total -ge 60) { '及格' } else { '不及格' }
            $gpa = [Math]::Round([Math]::Max(0,[Math]::Min(5.0,($total-50)/10)),1)
            $selRows += "($idx,$sid,$cid,'$SEM',$sts,$total,'$sl',$gpa,$dg,$lg,$eg,NOW(),NULL,NOW(),NOW())"
            $completed += ,@($sid, $cid)
        } else {
            $selRows += "($idx,$sid,$cid,'$SEM',$sts,NULL,NULL,NULL,NULL,NULL,NULL,NOW(),NULL,NOW(),NOW())"
        }
        if ($selRows.Length -ge 240) { break outer }
    }
    $sid++
}
$sql.AppendLine("INSERT INTO course_selection VALUES`n$($selRows -join ",`n");")

# Evaluations
$cmtCount = [Math]::Min(80, $completed.Length)
$completed = $completed | Sort-Object { $r.Next() }
$evRows = @()
for ($i = 0; $i -lt $cmtCount; $i++) {
    $evRows += "($($i+1),$($completed[$i][0]),$($completed[$i][1]),'$SEM',$($r.Next(3,6)),'$($HP[$r.Next(0,$HP.Length)])',NOW(),NOW(),NOW())"
}
$sql.AppendLine("INSERT INTO course_evaluation VALUES`n$($evRows -join ",`n");")

# Update counts
$counts = @{}
foreach ($s in $selRows) { $c = ($s -split ',')[2]; $counts[$c] = ($counts[$c] ?? 0) + 1 }
foreach ($c in $counts.Keys | Sort-Object) { $sql.AppendLine("UPDATE course SET selected_count = $($counts[$c]) WHERE id = $c;") }

# sys_user
$uRows = @("(1,'admin','$BCRYPT','系统管理员',3,'ADMIN001',NULL,1,NOW(),NOW())")
for ($i = 0; $i -lt 18; $i++) {
    $parts = $teRows[$i] -split "','"
    $nm = $parts[2]; $gc = if ($parts[3] -eq '男') { '1' } else { '2' }
    $uRows += "($($i+2),'T$($i.ToString('000'))','$BCRYPT','$nm',2,'T$($i.ToString('000'))',$gc,1,NOW(),NOW())"
}
for ($i = 0; $i -lt 240; $i++) {
    $parts = $stRows[$i] -split "','"
    $sno = $parts[1]; $nm = $parts[2]; $gc = if ($parts[3] -eq '男') { '1' } else { '2' }
    $uRows += "($($i+22),'$sno','$BCRYPT','$nm',1,'$sno',$gc,1,NOW(),NOW())"
}
$sql.AppendLine("INSERT INTO sys_user VALUES`n$($uRows -join ",`n");")

# user_role
$urRows = @('(1,1)')
for ($i = 2; $i -lt 22; $i++) { $urRows += "($i,2)" }
for ($i = 22; $i -lt 262; $i++) { $urRows += "($i,3)" }
$sql.AppendLine("INSERT INTO sys_user_role VALUES`n$($urRows -join ",`n");")
$sql.AppendLine("SET FOREIGN_KEY_CHECKS = 1;")

$total = $clRows.Length + $teRows.Length + $stRows.Length + $coRows.Length + $selRows.Length + $evRows.Length
$sql.AppendLine("-- 数据统计: 班级$($clRows.Length) 教师$($teRows.Length) 学生$($stRows.Length) 课程$($coRows.Length) 选课$($selRows.Length) 评价$($evRows.Length)")

# ========== Step 3: Execute via MySQL ==========
Write-Host '[3/3] 写入数据库...' -ForegroundColor Cyan
$tmpPath = "$PSScriptRoot\..\db\_seed_data.sql"
[System.IO.File]::WriteAllText($tmpPath, $sql.ToString(), [System.Text.Encoding]::UTF8)
cmd /c "mysql -u root --ssl-mode=DISABLED --default-character-set=utf8mb4 course_selection_system_cloud < $tmpPath" 2>&1 | Out-Null
if ($LASTEXITCODE -eq 0) {
    Remove-Item $tmpPath -Force
    Write-Host "✅ 成功! 总计约 $total 条记录" -ForegroundColor Green
    Write-Host "📊 班级:$($clRows.Length) 教师:$($teRows.Length) 学生:$($stRows.Length) 课程:$($coRows.Length) 选课:$($selRows.Length) 评价:$($evRows.Length)" -ForegroundColor Cyan
} else {
    Write-Host "❌ 失败!" -ForegroundColor Red
}
