# NotePad 增强版笔记应用

![License](https://img.shields.io/badge/license-Apache--2.0-blue.svg)
![Platform](https://img.shields.io/badge/platform-Android-green.svg)
![Language](https://img.shields.io/badge/language-Java-orange.svg)

这是一个在官方 NotePad 示例基础上进行全面功能增强和界面优化的 Android 笔记应用。

## 内容导航
- [项目概述](#项目概述)
- [功能详解](#功能详解)
  - [必备功能](#必备功能)
    - [笔记时间戳显示](#笔记时间戳显示)
    - [笔记搜索功能](#笔记搜索功能)
  - [拓展功能](#拓展功能)
    - [界面视觉优化](#界面视觉优化)
    - [待办事项管理](#待办事项管理)
    - [笔记分类整理](#笔记分类整理)
- [部署与使用](#部署与使用)
  - [环境依赖](#环境依赖)
  - [构建指南](#构建指南)
  - [操作说明](#操作说明)
- [工程架构](#工程架构)

## 项目概述

NotePad 增强版是在 Android 官方示例应用的基础上，针对现代用户使用习惯进行深度定制开发的笔记管理工具。该应用不仅保留了原有笔记创建、编辑、删除等核心功能，还引入了多项实用特性，包括时间戳记录、全文搜索、界面美化、待办清单以及笔记分类等，极大地提升了用户体验和实用性。

此项目不仅是 Android 开发学习的良好范例，同时也展现了如何在既有代码基础上进行功能扩展和用户体验优化的最佳实践。

## 功能详解

### 必备功能

#### 笔记时间戳显示

**功能描述**
- 自动追踪并显示每篇笔记的创建和最新修改时间
- 在笔记列表界面中以标准化格式 `yyyy-MM-dd HH:mm` 展示时间信息
- 帮助用户快速识别近期编辑的笔记内容

**技术实现**
该功能通过在数据表中维护时间戳字段并在界面中格式化显示来实现。每次保存笔记时，系统会自动更新修改时间字段。

在 [NoteEditor](app/src/main/java/com/example/android/notepad/NoteEditor.java) 的 [updateNote](app/src/main/java/com/example/android/notepad/NoteEditor.java) 方法中自动更新时间戳：
```java
private final void updateNote(String title, String text, String category) {
    ContentValues values = new ContentValues();
    // 保存时自动更新修改时间
    values.put(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE, System.currentTimeMillis());
    // 其他保存逻辑...
}
```

在 [NoteCategoryAdapter](app/src/main/java/com/example/android/notepad/NoteCategoryAdapter.java) 中格式化显示时间：
```java
void bind(NotesListFragment.NoteHolder note) {
    mNoteTitle.setText(note.title);
    
    // 将时间戳格式化为易读格式
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
    String formattedDate = sdf.format(new Date(note.modificationDate));
    mNoteTimestamp.setText(formattedDate);
}
```

<p align="center">
  <img src="picture/分类展示.png" alt="时间戳显示效果" width="300">
</p>
<p align="center">
  <em>笔记列表中显示的时间戳</em>
</p>

#### 笔记搜索功能

**功能描述**
- 支持对笔记标题和正文内容进行实时全文搜索
- 搜索结果动态更新，提供流畅的交互体验
- 与笔记分类功能无缝集成，可在分类内或跨分类搜索

**技术实现**
搜索功能在 [NotesListFragment](app/src/main/java/com/example/android/notepad/NotesListFragment.java) 中实现，利用 SearchView 组件监听用户输入并实时过滤笔记列表。

```java
// 设置搜索监听器
MenuItem searchItem = toolbarMenu.findItem(R.id.menu_search);
SearchView searchView = (SearchView) searchItem.getActionView();
if (searchView != null) {
    searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
        @Override
        public boolean onQueryTextSubmit(String query) {
            performSearch(query);
            return true;
        }

        @Override
        public boolean onQueryTextChange(String newText) {
            if (newText.isEmpty()) {
                performSearch(null);
            }
            return true;
        }
    });
}

// 在数据加载器中处理搜索逻辑
@NonNull
@Override
public Loader<Cursor> onCreateLoader(int id, @Nullable Bundle args) {
    Uri baseUri = NotePad.Notes.CONTENT_URI;
    String selection = null;
    String[] selectionArgs = null;

    if (args != null) {
        String query = args.getString("query");
        String category = args.getString("category");

        if (query != null && !query.isEmpty()) {
            selection = NotePad.Notes.COLUMN_NAME_TITLE + " LIKE ? OR " + NotePad.Notes.COLUMN_NAME_NOTE + " LIKE ?";
            selectionArgs = new String[]{"%" + query + "%", "%" + query + "%"};
        } else if (category != null && !category.isEmpty()) {
            selection = NotePad.Notes.COLUMN_NAME_CATEGORY + " = ?";
            selectionArgs = new String[]{category};
        }
    }

    return new CursorLoader(requireActivity(), baseUri,
            PROJECTION, selection, selectionArgs,
            NotePad.Notes.COLUMN_NAME_CATEGORY + " ASC, " + NotePad.Notes.DEFAULT_SORT_ORDER);
}
```

<p align="center">
  <img src="picture/搜索功能.png" alt="搜索功能演示" width="300">
</p>
<p align="center">
  <em>在笔记标题和内容中进行搜索</em>
</p>

### 拓展功能

#### 界面视觉优化

**功能描述**
- 采用现代化的 Material Design 设计语言重构界面
- 使用工具栏替代传统菜单，提升操作便捷性
- 引入卡片式布局和圆角元素，增强视觉美感
- 优化色彩搭配，提供更舒适的视觉体验

**技术实现**
界面优化主要通过使用 Material Components 组件库和自定义样式来实现。在 [note_editor.xml](app/src/main/res/layout/note_editor.xml) 中使用了现代化的布局组件：

```xml
<!-- 使用 Material Design 工具栏 -->
<com.google.android.material.appbar.AppBarLayout
    android:layout_width="match_parent"
    android:layout_height="wrap_content">

    <androidx.appcompat.widget.Toolbar
        android:id="@+id/toolbar"
        android:layout_width="match_parent"
        android:layout_height="?attr/actionBarSize"
        android:background="?attr/colorPrimary"
        android:elevation="4dp"
        android:theme="@style/ThemeOverlay.AppCompat.ActionBar"
        app:popupTheme="@style/ThemeOverlay.AppCompat.Light" />

</com.google.android.material.appbar.AppBarLayout>

<!-- 使用 Material 输入框组件 -->
<com.google.android.material.textfield.TextInputLayout
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_marginBottom="16dp"
    style="@style/Widget.MaterialComponents.TextInputLayout.OutlinedBox">

    <com.google.android.material.textfield.TextInputEditText
        android:id="@+id/title"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:inputType="textCapSentences"
        android:hint="@string/text_title" />

</com.google.android.material.textfield.TextInputLayout>
```

<p align="center">
  <img src="picture/待办展示.png" alt="待办事项展示" width="300">
</p>
<p align="center">
  <em>待办事项列表，支持滑动删除和颜色标记</em>
</p>

#### 待办事项管理

**功能描述**
- 提供独立的待办事项管理界面
- 支持为待办事项设置个性化颜色标签
- 实现直观的滑动删除操作
- 可视化区分已完成和未完成事项

**技术实现**
待办事项功能使用独立的数据模型和界面进行管理，在 [TodoFragment](app/src/main/java/com/example/android/notepad/TodoFragment.java) 中实现：

```java
// 实现滑动删除功能
new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        long id = (long) viewHolder.itemView.getTag();
        getContext().getContentResolver().delete(NotePad.Todos.CONTENT_URI, NotePad.Todos._ID + "=?", new String[]{String.valueOf(id)});
    }
}).attachToRecyclerView(recyclerView);

// 添加待办事项对话框
private void showAddTodoDialog() {
    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
    LayoutInflater inflater = requireActivity().getLayoutInflater();
    View dialogView = inflater.inflate(R.layout.dialog_add_todo, null);
    builder.setView(dialogView);

    final EditText todoText = dialogView.findViewById(R.id.edit_text_todo);
    final View colorPreview = dialogView.findViewById(R.id.color_preview);

    final String[] selectedColor = {"#FFFFFF"};

    // 多种颜色选项
    dialogView.findViewById(R.id.color_coral).setOnClickListener(v -> {
        selectedColor[0] = "#FF7F7F";
        colorPreview.setBackgroundColor(Color.parseColor(selectedColor[0]));
        dialogView.setBackgroundColor(Color.parseColor(selectedColor[0]));
    });
    
    // 更多颜色选项...

    builder.setPositiveButton("添加", (dialog, which) -> {
        String text = todoText.getText().toString();
        if (!text.isEmpty()) {
            ContentValues values = new ContentValues();
            values.put(NotePad.Todos.COLUMN_NAME_TEXT, text);
            values.put(NotePad.Todos.COLUMN_NAME_COLOR, selectedColor[0]);
            getContext().getContentResolver().insert(NotePad.Todos.CONTENT_URI, values);
        }
    });
    builder.setNegativeButton("取消", (dialog, which) -> dialog.cancel());

    builder.show();
}
```

<p align="center">
  <img src="picture/createTodo.png" alt="新建待办事项" width="300">
</p>
<p align="center">
  <em>创建新的待办事项界面</em>
</p>

#### 笔记分类整理

**功能描述**
- 支持为笔记添加自定义分类标签
- 自动按分类对笔记进行分组展示
- 提供分类筛选功能，便于快速查找
- 未分类笔记统一归入"未分类"组

**技术实现**
分类功能通过在 [NoteEditor](app/src/main/java/com/example/android/notepad/NoteEditor.java) 中使用 [AutoCompleteTextView](https://developer.android.com/reference/android/widget/AutoCompleteTextView) 实现分类名称的自动补全，并在 [NotesListFragment](app/src/main/java/com/example/android/notepad/NotesListFragment.java) 中按分类展示：

```java
// 实现分类自动补全
private void setupCategoryAutocomplete() {
    ArrayAdapter<String> adapter = new ArrayAdapter<>(this, 
            android.R.layout.simple_dropdown_item_1line, 
            getExistingCategories());
    mCategoryAutoComplete.setAdapter(adapter);
    mCategoryAutoComplete.setThreshold(0);
    
    // 点击时显示所有选项
    mCategoryAutoComplete.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mCategoryAutoComplete.showDropDown();
        }
    });
}

private List<String> getExistingCategories() {
    Set<String> categories = new HashSet<>();
    Cursor cursor = getContentResolver().query(NotePad.Notes.CONTENT_URI, 
            new String[]{NotePad.Notes.COLUMN_NAME_CATEGORY}, null, null, null);
    if (cursor != null) {
        while (cursor.moveToNext()) {
            String category = cursor.getString(0);
            if (category != null && !category.isEmpty()) {
                categories.add(category);
            }
        }
        cursor.close();
    }
    return new ArrayList<>(categories);
}

// 在笔记列表中按分类展示
@Override
public void onLoadFinished(@NonNull Loader<Cursor> loader, @Nullable Cursor data) {
    Map<String, List<NoteHolder>> categoryMap = new HashMap<>();

    if (data != null) {
        int idCol = data.getColumnIndexOrThrow(NotePad.Notes._ID);
        int titleCol = data.getColumnIndexOrThrow(NotePad.Notes.COLUMN_NAME_TITLE);
        int modDateCol = data.getColumnIndexOrThrow(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE);
        int categoryCol = data.getColumnIndexOrThrow(NotePad.Notes.COLUMN_NAME_CATEGORY);

        while (data.moveToNext()) {
            String category = data.getString(categoryCol);
            if (category == null || category.trim().isEmpty()) {
                category = "未分类";
            }

            if (!categoryMap.containsKey(category)) {
                categoryMap.put(category, new ArrayList<>());
            }

            NoteHolder note = new NoteHolder();
            note.id = data.getLong(idCol);
            note.title = data.getString(titleCol);
            note.modificationDate = data.getLong(modDateCol);

            categoryMap.get(category).add(note);
        }
    }

    List<String> sortedCategories = new ArrayList<>(categoryMap.keySet());
    Collections.sort(sortedCategories);

    List<Object> items = new ArrayList<>();
    for (String category : sortedCategories) {
        items.add(category);
        items.addAll(categoryMap.get(category));
    }

    mAdapter = new NoteCategoryAdapter(getContext(), items, this);
    mRecyclerView.setAdapter(mAdapter);
}
```

<p align="center">
  <img src="picture/分类展示.png" alt="笔记分类展示" width="300">
</p>
<p align="center">
  <em>按分类组织的笔记列表</em>
</p>

## 部署与使用

### 环境依赖
- Android 5.0 (API level 21) 或更高版本
- Android Studio 4.0 或更高版本
- JDK 8 或更高版本

### 构建指南
1. 克隆或下载项目源代码
2. 使用 Android Studio 打开项目
3. 等待 Gradle 同步完成
4. 连接 Android 设备或启动模拟器
5. 点击 Run 按钮编译并安装应用

### 操作说明
1. 启动应用后进入笔记浏览界面
2. 点击右下角浮动按钮创建新笔记或待办事项
3. 点击任一笔记条目进入编辑界面
4. 长按笔记可弹出操作菜单进行删除等操作
5. 使用顶部搜索框快速查找笔记
6. 切换至 "待办事项" 标签页管理任务清单

## 工程架构

```
app/
 src/
    main/
       java/com/example/android/notepad/
          NoteEditor.java           // 笔记编辑 Activity
          NotePad.java              // 数据契约定义
          NotePadProvider.java      // 内容提供者
          NotesListFragment.java    // 笔记列表 Fragment
          NotesList.java            // 主容器 Activity
          TodoFragment.java         // 待办事项 Fragment
          TodoAdapter.java          // 待办事项适配器
          NoteCategoryAdapter.java  // 分类笔记适配器
       res/
           layout/                   // 布局文件
           menu/                     // 菜单资源
           values/                   // 字符串、样式等资源
           drawable/                 // 图形资源
    AndroidManifest.xml               // 应用配置文件
 build.gradle                          // 构建配置
```

---

**NotePad 增强版** - 现代化、功能丰富的 Android 笔记应用解决方案