# 第 2、3 题参考答案（配合 RenderLab.docx）

## 第 2 题：老师真拿去上课会遇到什么问题

这题不是让你泛泛说"网络不好"，而是要你说出**这个具体实现**（一个静态 `HashMap` + 免费
Render 实例 + 无登录）在真实课堂里的失效方式。挑 4–6 条，每条说清"为什么"就行。

1. **冷启动 / 可用性**：Render 免费实例闲置约 15 分钟就休眠，下一位访问者要等 30–60 秒
   才唤醒，浏览器多半已经超时；上课铃响时全班同时打开，第一波请求全部卡在冷启动上。
2. **并发能力**：免费实例只有 0.1 CPU / 512 MB，而且 `handle()` 里对 `sid2seat` 加了
   `synchronized`，所有提交串行化。几十个人同时点提交，响应会明显变慢甚至 502/503。
3. **身份无法验证（代签）**：没有登录，链接公开，`studentId` 是学生自己填的字符串。
   别人可以替缺勤的同学提交；人也可以在宿舍、在被窝里提交。`seatId` 同样只是手填的
   文本，没有任何东西证明"这个人真的坐在这个座位上"（没有二维码、没有定位、没有
   校园网校验、没有设备绑定）。
4. **数据不持久**：`sid2seat` 是 `static` 内存字段，服务重启、重新部署、休眠后冷启动
   都会清空；没有数据库、没有导出，下课就没了，事后无法核对。
5. **没有时间信息**：记录里根本没有时间戳，分不清谁迟到、谁早退；也没法"关闭签到"，
   一节课和下一节课的记录混在同一个 map 里。
6. **没有纠错能力**：学号打错就多一条假记录，没法删；同一个人重复提交只会覆盖座位号；
   没有"未签到名单"，老师还得自己比对。
7. **隐私**：`Attendee List` 页面把所有人的学号和座位号明文展示，任何拿到链接的人
   （包括外网）都能看。
8. **网络与设备**：依赖学生自带设备 + 校园网；Render 节点在境外，国内访问可能很慢或
   间歇性不通；教室 Wi-Fi 的 NAT / 认证页也会挡掉一部分人。

可直接交的英文版（改一改措辞即可）：

> In a real classroom this service has several practical problems. First, it runs on Render's
> free tier, which sleeps after about 15 minutes of inactivity, so the first request of the
> class can take 30–60 seconds and many students will see a timeout. Second, the free instance
> has very little CPU and memory, and the servlet synchronizes every access to the map, so
> dozens of simultaneous submissions become slow or return 502/503 errors. Third, there is no
> authentication at all: the URL is public and the student id is simply typed by the student,
> so anyone can sign in for an absent friend or submit from a dormitory, and the seat id is
> also just free text, so nothing proves that the student is really sitting in that seat.
> Fourth, attendance is stored only in a static in-memory HashMap, so all records are lost
> whenever the service restarts or is redeployed, and there is no database, no export and no
> audit trail. Fifth, no timestamp is recorded, so the instructor cannot tell who was late,
> cannot close the sign-in, and cannot separate one class session from another. Finally, the
> attendee list shows every student id and seat id to anyone who has the link, which is a
> privacy problem, and the page also cannot correct a mistyped id or remove a fake entry.

## 第 3 题：改 `Attend.java` 的哪一部分才能让名单按学号排序

**答案：改 Attendee List 分支里遍历 `sid2seat` 的那个 for 循环。**

具体位置是 `handle()` 方法中 `if (studentId == null)` 分支：

```java
if (studentId == null) {
    // ----- Attendee List: dump the map -----
    synchronized (sid2seat) {
        if (sid2seat.isEmpty()) {
            out.println("No sign-in record yet.<br><br>");
        } else {
            for (Map.Entry<String, String> e : sid2seat.entrySet()) {   // ← 就是这里
                out.println(e.getKey() + " : " + e.getValue() + "<br>");
            }
        }
    }
    ...
```

原因：`sid2seat` 声明为 `HashMap`，`HashMap` 的迭代顺序由哈希桶决定，既不是插入顺序，
也不是学号顺序。所以即使你按顺序提交，名单看起来也是乱的。

两种改法（任选一种，考试答第二种最省事）：

**改法 A：遍历前排序（只动这一小段）**

```java
import java.util.TreeMap;
...
synchronized (sid2seat) {
    if (sid2seat.isEmpty()) {
        out.println("No sign-in record yet.<br><br>");
    } else {
        for (Map.Entry<String, String> e : new TreeMap<>(sid2seat).entrySet()) {
            out.println(e.getKey() + " : " + e.getValue() + "<br>");
        }
    }
}
```

**改法 B：换掉 map 的实现（一行）**

```java
// 原：private static final Map<String, String> sid2seat = new HashMap<String, String>();
private static final Map<String, String> sid2seat = new TreeMap<String, String>();
```

`TreeMap` 按 key 的自然顺序（`String` 的字典序）排列，遍历时天然有序，`for` 循环一个字都不用改。
写入仍是 `put`，重复学号仍然覆盖，行为完全不变。

**答题时补一句会加分：** key 是 `String`，所以排的是字典序——学号定长（比如都是 11 位）时
字典序等价于数值序；如果学号长度不定（`"9"` 和 `"10"`），要把比较器换成
`Comparator.comparingLong(Long::parseLong)`（11 位学号超过 `int` 范围，别用 `parseInt`）。

如果要让**页面和以后可能的导出都统一有序**，改法 B 更彻底；只想动一处、不影响其他逻辑，
就用改法 A。

> 实测证据（可以直接写进答案里）：同一份代码，按 25406040236 → 217 → 230 → 227 → 263 的
> 顺序提交，`HashMap` 版名单输出是 230、263、227、217、236，既不按学号也不按提交顺序；
> 换成 `TreeMap` 后输出 217、227、230、236、263。

## 附：完整排序版代码

见 `docs/AttendSorted.java.txt`（改法 A），可直接覆盖 `src/java/attendance/Attend.java` 后重新部署。
