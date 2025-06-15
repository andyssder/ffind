# Find in Copy - IntelliJ IDEA Plugin

 #### 🔍 扩展IDEA的引用查找功能，检测隐藏在以下方法中的属性引用：

- Spring的BeanUtil.copyProperties(source, target) (默认支持)
- 自定义copy方法 (如 MyCopier.copyData(source, target))

#### 💡 示例: 可检测以下代码中的user.Name引用:
- BeanUtil.copyProperties(user, dto)
- CustomMapper.copy(user, target)

#### 📚 安装指南
IDEA 插件市场安装

---

#### 🔍 Extends IntelliJ's Find Usages to detect property references inside:

- BeanUtil.copyProperties(source, target) (Spring supported by default)
- Any custom copy method (e.g. MyCopier.copyData(source, target))

#### 💡 Example: Finds usage of user.Name in:
- BeanUtil.copyProperties(user, dto)
- CustomMapper.copy(user, target)

#### 📚 Installation
Via IDEA Plugin Marketplace



