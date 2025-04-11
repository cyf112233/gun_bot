# gun_bot
一个很简易但是很有效的防假人攻击插件<br>
***通过识别1分钟内的连接数进行阻止假人攻击服务器***
<br>
以下为配置文件
```
# 防机器人攻击配置
anti-bot:
  # 1分钟内允许的最大连接次数
  max-connections-per-minute: 10
  # 触发限制后的封禁时间（分钟）
  ban-duration: 5
  # 是否启用防机器人攻击
  enabled: true
  # 是否在触发限制时发送消息到控制台
  debug: true
  # 玩家消息配置
  messages:
    # 当服务器被攻击时的踢出消息
    kick-message: "§c服务器正在遭受攻击，请稍后再试！"
    # 当服务器被攻击时的控制台消息
    console-attack-message: "检测到可能的机器人攻击，来自IP: {ip}"
    # 当服务器被攻击时的封禁时间消息
    console-ban-duration-message: "服务器将在 {time} 后拒绝新的连接"
    # 当服务器被攻击时显示的 MOTD 消息
    motd-ban-message: "§c服务器正在遭受攻击，将在 {time} 后恢复" 
```
