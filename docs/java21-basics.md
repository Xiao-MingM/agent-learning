# 从 Java 8 过渡到 Java 21：结合本项目理解语法

这份笔记不追求罗列 Java 9 到 Java 21 的所有功能，只解释本项目实际出现的写法，以及阅读现代 Java 代码时最常见的变化。

## 1. 先看结论：项目中哪些是新语法

本项目真正超出 Java 8 的主要写法只有：

- `record`：Java 16 正式提供的数据载体类型。
- `Map.of(...)`：Java 9 提供的不可变集合工厂方法。

下面这些不是 Java 21 新语法：

- `@Tool`、`@ToolParam`：普通 Java 注解。
- `chatClient.prompt().user(...).tools(...)`：普通方法的链式调用。
- 构造器注入：Java 8 就支持，只是一种 Spring 编码习惯。

Java 21 是 LTS 版本。人们通常会把 Java 9 到 Java 21 之间加入的特性都笼统地称为“Java 21 写法”。

## 2. record：最需要理解的变化

项目中的订单：

```java
public record Order(String id, String productName, String status) {
}
```

它可以理解为“只用来保存数据的类”。编译器会自动生成：

- 三个 `private final` 字段
- 包含全部字段的构造方法
- `id()`、`productName()`、`status()` 访问方法
- `equals()`、`hashCode()`、`toString()`

大致等价于下面的 Java 8 代码：

```java
public final class Order {
    private final String id;
    private final String productName;
    private final String status;

    public Order(String id, String productName, String status) {
        this.id = id;
        this.productName = productName;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public String getProductName() {
        return productName;
    }

    public String getStatus() {
        return status;
    }

    // 还需要手写 equals、hashCode 和 toString
}
```

### record 的访问方法没有 get 前缀

Java 8 Bean 常写：

```java
order.getStatus();
```

record 写：

```java
order.status();
```

因此测试中的这行：

```java
result.order().status()
```

可以拆成：

```java
Order order = result.order();
String status = order.status();
```

如果换成 Java 8 Bean 风格，就接近：

```java
result.getOrder().getStatus();
```

### record 默认适合不可变数据

record 的字段不能在创建后重新赋值：

```java
Order order = new Order("1001", "机械键盘", "已发货");
// order.status = "已签收"; // 编译错误
```

如果状态需要变化，应创建新对象，或者使用适合可变业务实体的普通 class。这个项目只查询演示数据，所以 record 很合适。

## 3. 定义在类内部的 record

Controller 中有：

```java
public record ChatRequest(String question) {
}

public record ChatResponse(String answer) {
}
```

它们定义在 `AgentController` 内部，相当于两个静态嵌套类型。完整类型名是：

```java
AgentController.ChatRequest
AgentController.ChatResponse
```

这样做只是因为它们非常小，并且只服务于这个 Controller。不是必须这么写，也可以各自放在独立文件中。

下面这行容易因为嵌套较多而看不懂：

```java
return new ChatResponse(agentService.chat(request.question()));
```

拆开后就是：

```java
String question = request.question();
String answer = agentService.chat(question);
ChatResponse response = new ChatResponse(answer);
return response;
```

## 4. Map.of：快速创建不可变 Map

项目中的数据：

```java
private final Map<String, Order> orders = Map.of(
        "1001", new Order("1001", "机械键盘", "已发货"),
        "1002", new Order("1002", "显示器", "处理中")
);
```

它大致替代下面的 Java 8 写法：

```java
private final Map<String, Order> orders;

public OrderService() {
    Map<String, Order> data = new HashMap<>();
    data.put("1001", new Order("1001", "机械键盘", "已发货"));
    data.put("1002", new Order("1002", "显示器", "处理中"));
    orders = Collections.unmodifiableMap(data);
}
```

`Map.of` 创建的 Map 不能修改：

```java
orders.put("1003", new Order(...)); // 运行时抛出 UnsupportedOperationException
```

这对固定的学习数据很合适。如果以后需要新增、修改订单，就应换成数据库或可变 Map。

类似方法还有：

```java
List.of("A", "B", "C");
Set.of("A", "B", "C");
```

## 5. 链式调用不是新语法

AgentService 中：

```java
return chatClient.prompt()
        .user(question)
        .tools(orderTools)
        .call()
        .content();
```

每个方法都会返回一个对象，然后继续调用下一个方法。Java 8 完全支持这种写法。

为了理解执行顺序，可以暂时把它想象成：

```java
var prompt = chatClient.prompt();
var promptWithUser = prompt.user(question);
var promptWithTools = promptWithUser.tools(orderTools);
var callResponse = promptWithTools.call();
String content = callResponse.content();
return content;
```

这里使用 `var` 只是为了避免写很长的 Spring AI 类型名。原项目没有使用 `var`。

## 6. var：局部变量类型推断

Java 10 开始可以写：

```java
var order = new Order("1001", "机械键盘", "已发货");
```

编译器仍然知道它是 `Order`，Java 并没有变成动态类型语言。它等价于：

```java
Order order = new Order("1001", "机械键盘", "已发货");
```

限制：

- 只能用于局部变量。
- 必须在声明时赋值，让编译器能推断类型。
- 不能代替字段、方法参数和返回类型。

```java
var name = "订单";       // 可以
var value;               // 不可以，无法推断类型
private var order;       // 不可以，字段不能使用 var
```

学习阶段不必刻意使用 `var`。显式类型能让代码更容易阅读时，就继续写显式类型。

## 7. switch 表达式

Java 8 的 switch 通常需要 `break`：

```java
String text;
switch (status) {
    case "SHIPPED":
        text = "已发货";
        break;
    case "DONE":
        text = "已完成";
        break;
    default:
        text = "处理中";
}
```

现代 Java 可以直接返回值：

```java
String text = switch (status) {
    case "SHIPPED" -> "已发货";
    case "DONE" -> "已完成";
    default -> "处理中";
};
```

箭头分支不需要 `break`，也不会意外进入下一个 case。

## 8. instanceof 模式匹配

Java 8：

```java
if (value instanceof Order) {
    Order order = (Order) value;
    System.out.println(order.status());
}
```

现代 Java：

```java
if (value instanceof Order order) {
    System.out.println(order.status());
}
```

判断类型成功后，变量 `order` 已经完成类型转换。

## 9. 文本块

Java 8 中较长字符串经常需要拼接和转义：

```java
String json = "{\n" +
        "  \"question\": \"查询订单\"\n" +
        "}";
```

现代 Java 可以使用三个双引号：

```java
String json = """
        {
          "question": "查询订单"
        }
        """;
```

文本块适合 JSON、SQL 和多行提示词。

## 10. sealed：限制谁能实现或继承

如果只允许固定的几种结果类型，可以写：

```java
public sealed interface QueryResult permits Found, NotFound {
}

public record Found(Order order) implements QueryResult {
}

public record NotFound(String message) implements QueryResult {
}
```

这表示 `QueryResult` 只能由列出的类型实现。它对复杂领域模型有价值，但本项目只有一个简单工具结果，因此没有必要引入。

## 11. 阅读本项目时的翻译方法

看到现代 Java 代码时，可以先在脑中做下面的替换：

| 现代写法 | 按 Java 8 理解 |
| --- | --- |
| `record Order(...)` | final class + final 字段 + 构造器 + getter + equals/hashCode/toString |
| `order.status()` | `order.getStatus()` |
| `result.order().status()` | `result.getOrder().getStatus()` |
| `Map.of(k, v)` | 创建 Map、put 数据、再变成不可修改 Map |
| `var order = ...` | 编译器帮你填写左侧类型 |
| `case X -> value` | 不需要 break 的 switch 分支 |
| `value instanceof Order order` | instanceof 判断 + 强制类型转换 |
| `"""..."""` | 多行字符串 |

## 12. 建议的学习顺序

先掌握与当前项目直接相关的内容：

1. record 及其无 `get` 前缀的访问方法
2. `Map.of`、`List.of`
3. 链式方法调用
4. `var`
5. switch 表达式
6. instanceof 模式匹配
7. 文本块

虚拟线程、sealed class、switch 模式匹配可以等到实际需要并发或复杂领域建模时再学习，不需要为了“使用 Java 21”强行加入当前项目。
