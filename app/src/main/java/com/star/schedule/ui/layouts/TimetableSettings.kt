package com.star.schedule.ui.layouts

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.star.schedule.db.CourseEntity
import com.star.schedule.db.LessonTimeEntity
import com.star.schedule.db.ScheduleDao
import com.star.schedule.db.TimetableEntity
import com.star.schedule.ui.components.OptimizedBottomSheet
import com.star.schedule.utils.ValidationUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableSettings(dao: ScheduleDao) {
    val scope = rememberCoroutineScope()
    val timetables by dao.getAllTimetables().collectAsState(initial = emptyList())
    val haptic = LocalHapticFeedback.current

    // BottomSheet 状态管理
    var showAddLessonSheet by remember { mutableStateOf(false) }
    var showAddCourseSheet by remember { mutableStateOf(false) }
    var showEditLessonSheet by remember { mutableStateOf<LessonTimeEntity?>(null) }
    var showEditCourseSheet by remember { mutableStateOf<CourseEntity?>(null) }
    var showTimetableDetailSheet by remember { mutableStateOf<TimetableEntity?>(null) }
    var showImportOptionsSheet by remember { mutableStateOf(false) }
    var showWakeUpImportSheet by remember { mutableStateOf(false) }
    var currentTimetableId by remember { mutableStateOf<Long?>(null) }
    
    // BottomSheet状态
    val addLessonSheetState = rememberModalBottomSheetState()
    val addCourseSheetState = rememberModalBottomSheetState()
    val editLessonSheetState = rememberModalBottomSheetState()
    val editCourseSheetState = rememberModalBottomSheetState()
    val timetableDetailSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val importOptionsSheetState = rememberModalBottomSheetState()
    val wakeUpImportSheetState = rememberModalBottomSheetState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "课表管理",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        // 新建和导入按钮
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    shape = RoundedCornerShape(topStart = 50.dp, topEnd = 8.dp, bottomEnd = 8.dp, bottomStart = 50.dp),
                    onClick = {
                        scope.launch {
                            dao.insertTimetableWithReminders(
                                TimetableEntity(
                                    name = "新建课表",
                                    showWeekend = true,
                                    startDate = LocalDate.now().toString()
                                )
                            )
                        }
                    },
                    modifier = Modifier.weight(0.5f)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("新建")
                    }
                }

                Spacer(Modifier.width(2.dp))
                
                Button(
                    shape = RoundedCornerShape(topStart = 8.dp, topEnd = 50.dp, bottomEnd = 50.dp, bottomStart = 8.dp),
                    onClick = { showImportOptionsSheet = true },
                    modifier = Modifier.weight(0.5f)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.CalendarMonth, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("导入")
                    }
                }
            }
        }
        items(timetables) { timetable ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                    showTimetableDetailSheet = timetable
                }
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(timetable.name, style = MaterialTheme.typography.titleMedium)
                        IconButton(onClick = {
                            scope.launch {
                                dao.deleteTimetableWithReminders(
                                    timetable
                                )
                            }
                        }) {
                            Icon(Icons.Rounded.Delete, contentDescription = "删除课表")
                        }
                    }
                }
            }
        }


    }

    // 课表详情 BottomSheet
    showTimetableDetailSheet?.let { timetable ->
        TimetableDetailSheet(
            timetable = timetable,
            onDismiss = { 
                scope.launch { timetableDetailSheetState.hide() }.invokeOnCompletion {
                    if (!timetableDetailSheetState.isVisible) {
                        showTimetableDetailSheet = null
                    }
                }
            },
            dao = dao,
            sheetState = timetableDetailSheetState
        )
    }

    // 新增课程时间 BottomSheet
    if (showAddLessonSheet && currentTimetableId != null) {
        AddLessonTimeSheet(
            timetableId = currentTimetableId!!,
            onDismiss = {
                scope.launch { addLessonSheetState.hide() }.invokeOnCompletion {
                    if (!addLessonSheetState.isVisible) {
                        showAddLessonSheet = false
                        currentTimetableId = null
                    }
                }
            },
            dao = dao,
            sheetState = addLessonSheetState
        )
    }

    // 新增课程 BottomSheet
    if (showAddCourseSheet && currentTimetableId != null) {
        AddCourseSheet(
            timetableId = currentTimetableId!!,
            onDismiss = {
                scope.launch { addCourseSheetState.hide() }.invokeOnCompletion {
                    if (!addCourseSheetState.isVisible) {
                        showAddCourseSheet = false
                        currentTimetableId = null
                    }
                }
            },
            dao = dao,
            sheetState = addCourseSheetState
        )
    }

    // 编辑课程时间 BottomSheet
    showEditLessonSheet?.let { lesson ->
        EditLessonTimeSheet(
            lesson = lesson,
            onDismiss = {
                scope.launch { editLessonSheetState.hide() }.invokeOnCompletion {
                    if (!editLessonSheetState.isVisible) {
                        showEditLessonSheet = null
                    }
                }
            },
            dao = dao,
            sheetState = editLessonSheetState
        )
    }

    // 编辑课程 BottomSheet
    showEditCourseSheet?.let { course ->
        EditCourseSheet(
            course = course,
            onDismiss = {
                scope.launch { editCourseSheetState.hide() }.invokeOnCompletion {
                    if (!editCourseSheetState.isVisible) {
                        showEditCourseSheet = null
                    }
                }
            },
            dao = dao,
            sheetState = editCourseSheetState
        )
    }

    // 导入选项 BottomSheet
    if (showImportOptionsSheet) {
        ImportOptionsSheet(
            onDismiss = {
                scope.launch { importOptionsSheetState.hide() }.invokeOnCompletion {
                    if (!importOptionsSheetState.isVisible) {
                        showImportOptionsSheet = false
                    }
                }
            },
            onWakeUpImport = {
                scope.launch { importOptionsSheetState.hide() }.invokeOnCompletion {
                    if (!importOptionsSheetState.isVisible) {
                        showImportOptionsSheet = false
                        showWakeUpImportSheet = true
                    }
                }
            },
            sheetState = importOptionsSheetState
        )
    }

    // WakeUp导入 BottomSheet
    if (showWakeUpImportSheet) {
        WakeUpImportSheet(
            onDismiss = {
                scope.launch { wakeUpImportSheetState.hide() }.invokeOnCompletion {
                    if (!wakeUpImportSheetState.isVisible) {
                        showWakeUpImportSheet = false
                    }
                }
            },
            dao = dao,
            sheetState = wakeUpImportSheetState
        )
    }
}

// ---------- 编辑课程时间弹窗 ----------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditLessonTimeSheet(lesson: LessonTimeEntity, onDismiss: () -> Unit, dao: ScheduleDao, sheetState: androidx.compose.material3.SheetState) {
    // 获取当前课表的所有课程时间，用于重叠检测
    val lessonTimes by dao.getLessonTimesFlow(lesson.timetableId)
        .collectAsState(initial = emptyList())
    val sortedLessonTimes =
        lessonTimes.filter { it.id != lesson.id }.sortedBy { it.period } // 排除当前正在编辑的课程时间

    var startTime by remember { mutableStateOf(lesson.startTime) }
    var endTime by remember { mutableStateOf(lesson.endTime) }
    var errorMessage by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    // 时间选择器状态
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    OptimizedBottomSheet(
        onDismiss = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "编辑课程时间",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // 错误信息显示
            if (errorMessage.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(8.dp)
                    )
                }
                Spacer(Modifier.height(8.dp))
            }

            OutlinedTextField(
                value = startTime,
                onValueChange = {
                    startTime = it
                    errorMessage = "" // 清除错误信息
                },
                label = { Text("开始时间") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { showStartTimePicker = true }) {
                        Icon(Icons.Rounded.AccessTime, contentDescription = "选择时间")
                    }
                },
                isError = errorMessage.contains("开始时间") || errorMessage.contains("重叠"),
                supportingText = if (errorMessage.contains("开始时间")) {
                    { Text("格式如：08:00") }
                } else if (errorMessage.contains("重叠")) {
                    { Text(errorMessage) }
                } else {
                    { Text("点击选择课程开始时间") }
                }
            )

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = endTime,
                onValueChange = {
                    endTime = it
                    errorMessage = "" // 清除错误信息
                },
                label = { Text("结束时间") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { showEndTimePicker = true }) {
                        Icon(Icons.Rounded.AccessTime, contentDescription = "选择时间")
                    }
                },
                isError = errorMessage.contains("结束时间") || errorMessage.contains("重叠"),
                supportingText = if (errorMessage.contains("结束时间")) {
                    { Text("格式如：08:45，且必须晚于开始时间") }
                } else if (errorMessage.contains("重叠")) {
                    { Text(errorMessage) }
                } else {
                    { Text("点击选择课程结束时间") }
                }
            )

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) { Text("取消") }
                Spacer(Modifier.width(8.dp))
                Button(onClick = {
                    // 数据验证
                    val validationResult = ValidationUtils.LessonTimeValidation.validateTimeFormat(
                        startTime,
                        "开始时间"
                    )
                    if (!validationResult.isValid) {
                        errorMessage = validationResult.errorMessage
                        return@Button
                    }

                    val validationResult2 =
                        ValidationUtils.LessonTimeValidation.validateTimeFormat(endTime, "结束时间")
                    if (!validationResult2.isValid) {
                        errorMessage = validationResult2.errorMessage
                        return@Button
                    }

                    val timeRangeResult =
                        ValidationUtils.LessonTimeValidation.validateTimeRange(startTime, endTime)
                    if (!timeRangeResult.isValid) {
                        errorMessage = timeRangeResult.errorMessage
                        return@Button
                    }

                    // 检查时间重叠
                    val newStartTime = startTime
                    val newEndTime = endTime
                    val hasOverlap = sortedLessonTimes.any { l ->
                        // 检查时间是否重叠
                        (newStartTime < l.endTime && newEndTime > l.startTime)
                    }

                    if (hasOverlap) {
                        errorMessage = "时间重叠：与现有课程时间冲突"
                        return@Button
                    }

                    // 验证通过，保存数据（节次将自动分配）
                    scope.launch {
                        try {
                            val result = dao.insertOrUpdateLessonTimeAutoSort(
                                lesson.copy(
                                    startTime = startTime,
                                    endTime = endTime
                                ),
                                isInsert = false
                            )
                            Log.d("EditLessonTimeSheet", "更新课程时间成功，ID: $result")
                            onDismiss()
                        } catch (e: Exception) {
                            Log.e("EditLessonTimeSheet", "更新课程时间失败", e)
                            errorMessage = "保存失败: ${e.message}"
                        }
                    }
                }) { Text("保存") }
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    // 时间选择器
    if (showStartTimePicker) {
        TimePickerDialog(
            onDismiss = { showStartTimePicker = false },
            onConfirm = { hour, minute ->
                startTime =
                    java.text.MessageFormat.format("{0,number,00}:{1,number,00}", hour, minute)
                showStartTimePicker = false
            },
            initialHour = startTime.split(":")[0].toIntOrNull() ?: 8,
            initialMinute = startTime.split(":")[1].toIntOrNull() ?: 0
        )
    }

    if (showEndTimePicker) {
        TimePickerDialog(
            onDismiss = { showEndTimePicker = false },
            onConfirm = { hour, minute ->
                endTime =
                    java.text.MessageFormat.format("{0,number,00}:{1,number,00}", hour, minute)
                showEndTimePicker = false
            },
            initialHour = endTime.split(":")[0].toIntOrNull() ?: 8,
            initialMinute = endTime.split(":")[1].toIntOrNull() ?: 45
        )
    }
}

// ---------- 编辑课程弹窗 ----------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCourseSheet(course: CourseEntity, onDismiss: () -> Unit, dao: ScheduleDao, sheetState: androidx.compose.material3.SheetState) {
    // 获取当前课表的课程和课程时间，用于重叠检测
    val courses by dao.getCoursesFlow(course.timetableId).collectAsState(initial = emptyList())
    val filteredCourses = courses.filter { it.id != course.id } // 排除当前正在编辑的课程

    var name by remember { mutableStateOf(course.name) }
    var location by remember { mutableStateOf(course.location) }
    var dayOfWeek by remember { mutableStateOf(course.dayOfWeek.toString()) }
    var periods by remember { mutableStateOf(course.periods.joinToString(",")) }
    var weeks by remember { mutableStateOf(course.weeks.joinToString(",")) }
    var errorMessage by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    OptimizedBottomSheet(
        onDismiss = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "编辑课程",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // 错误信息显示
            if (errorMessage.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(8.dp)
                    )
                }
                Spacer(Modifier.height(8.dp))
            }

            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    errorMessage = "" // 清除错误信息
                },
                label = { Text("课程名称") },
                modifier = Modifier.fillMaxWidth(),
                isError = errorMessage.contains("课程名称"),
                supportingText = if (errorMessage.contains("课程名称")) {
                    { Text("课程名称不能为空，且不超过50个字符") }
                } else null
            )

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = location,
                onValueChange = {
                    location = it
                    errorMessage = "" // 清除错误信息
                },
                label = { Text("地点") },
                modifier = Modifier.fillMaxWidth(),
                isError = errorMessage.contains("上课地点"),
                supportingText = if (errorMessage.contains("上课地点")) {
                    { Text("上课地点不能超过100个字符") }
                } else null
            )

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = dayOfWeek,
                onValueChange = {
                    dayOfWeek = it.filter { c -> c.isDigit() }
                    errorMessage = "" // 清除错误信息
                },
                label = { Text("星期 (1-7)") },
                modifier = Modifier.fillMaxWidth(),
                isError = errorMessage.contains("星期"),
                supportingText = if (errorMessage.contains("星期")) {
                    { Text("1=周一，2=周二，...，7=周日") }
                } else null
            )

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = periods,
                onValueChange = {
                    periods = it
                    errorMessage = "" // 清除错误信息
                },
                label = { Text("节次 (如 1,2,3)") },
                modifier = Modifier.fillMaxWidth(),
                isError = errorMessage.contains("节次") || errorMessage.contains("重叠"),
                supportingText = if (errorMessage.contains("节次")) {
                    { Text("用逗号分隔，如：1,2,3") }
                } else if (errorMessage.contains("重叠")) {
                    { Text(errorMessage) }
                } else {
                    { Text("请输入课程节次，多个节次用逗号分隔") }
                }
            )

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = weeks,
                onValueChange = {
                    weeks = it
                    errorMessage = "" // 清除错误信息
                },
                label = { Text("周次 (如 1,2,3)") },
                modifier = Modifier.fillMaxWidth(),
                isError = errorMessage.contains("周次"),
                supportingText = if (errorMessage.contains("周次")) {
                    { Text("用逗号分隔，如：1,2,3，表示第几周上课") }
                } else null
            )

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) { Text("取消") }
                Spacer(Modifier.width(8.dp))
                Button(onClick = {
                    // 数据验证
                    val validationResult = ValidationUtils.CourseValidation.validateCourseData(
                        name = name,
                        location = location,
                        dayOfWeek = dayOfWeek,
                        periods = periods,
                        weeks = weeks
                    )

                    if (!validationResult.isValid) {
                        errorMessage = validationResult.errorMessage
                        return@Button
                    }

                    // 检查课程时间重叠
                    val day = dayOfWeek.toInt()
                    val periodList = periods.split(",").map { it.trim().toInt() }
                    val weekList = weeks.split(",").map { it.trim().toInt() }

                    // 检查是否有时间冲突
                    val hasTimeOverlap = filteredCourses.any { existingCourse ->
                        // 检查是否同一天
                        if (existingCourse.dayOfWeek != day) return@any false

                        // 检查是否同一周
                        val weekOverlap = existingCourse.weeks.any { w -> weekList.contains(w) }
                        if (!weekOverlap) return@any false

                        // 检查是否同一节次
                        val periodOverlap =
                            existingCourse.periods.any { p -> periodList.contains(p) }
                        periodOverlap
                    }

                    if (hasTimeOverlap) {
                        errorMessage = "时间重叠：与现有课程在同一时间"
                        return@Button
                    }

                    // 验证通过，保存数据
                    scope.launch {
                        try {
                            dao.updateCourseWithReminders(
                                course.copy(
                                    name = name,
                                    location = location,
                                    dayOfWeek = day,
                                    periods = periodList,
                                    weeks = weekList
                                )
                            )
                            Log.d("EditCourseSheet", "更新课程成功，ID: ${course.id}")
                            onDismiss()
                        } catch (e: Exception) {
                            Log.e("EditCourseSheet", "更新课程失败", e)
                            errorMessage = "保存失败: ${e.message}"
                        }
                    }
                }) {
                    Text("保存")
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ------------------ 新增课程时间弹窗 ------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLessonTimeSheet(
    timetableId: Long,
    onDismiss: () -> Unit,
    dao: ScheduleDao,
    sheetState: androidx.compose.material3.SheetState
) {
    // 获取当前课表的所有课程时间，用于重叠检测
    val lessonTimes by dao.getLessonTimesFlow(timetableId).collectAsState(initial = emptyList())

    var startTime by remember { mutableStateOf("08:00") }
    var endTime by remember { mutableStateOf("08:45") }
    var errorMessage by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    // 时间选择器状态
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    OptimizedBottomSheet(
        onDismiss = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "新增课程时间",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // 错误信息显示
            if (errorMessage.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(8.dp)
                    )
                }
                Spacer(Modifier.height(8.dp))
            }

            OutlinedTextField(
                value = startTime,
                onValueChange = {
                    startTime = it
                    errorMessage = "" // 清除错误信息
                },
                label = { Text("开始时间") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { showStartTimePicker = true }) {
                        Icon(Icons.Rounded.AccessTime, contentDescription = "选择时间")
                    }
                },
                isError = errorMessage.contains("开始时间") || errorMessage.contains("重叠"),
                supportingText = if (errorMessage.contains("开始时间")) {
                    { Text("格式如：08:00") }
                } else if (errorMessage.contains("重叠")) {
                    { Text(errorMessage) }
                } else {
                    { Text("点击选择课程开始时间") }
                }
            )

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = endTime,
                onValueChange = {
                    endTime = it
                    errorMessage = "" // 清除错误信息
                },
                label = { Text("结束时间") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { showEndTimePicker = true }) {
                        Icon(Icons.Rounded.AccessTime, contentDescription = "选择时间")
                    }
                },
                isError = errorMessage.contains("结束时间") || errorMessage.contains("重叠"),
                supportingText = if (errorMessage.contains("结束时间")) {
                    { Text("格式如：08:45，且必须晚于开始时间") }
                } else if (errorMessage.contains("重叠")) {
                    { Text(errorMessage) }
                } else {
                    { Text("点击选择课程结束时间") }
                }
            )

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) { Text("取消") }
                Spacer(Modifier.width(8.dp))
                Button(onClick = {
                    // 数据验证
                    val validationResult = ValidationUtils.LessonTimeValidation.validateTimeFormat(
                        startTime,
                        "开始时间"
                    )
                    if (!validationResult.isValid) {
                        errorMessage = validationResult.errorMessage
                        return@Button
                    }

                    val validationResult2 =
                        ValidationUtils.LessonTimeValidation.validateTimeFormat(endTime, "结束时间")
                    if (!validationResult2.isValid) {
                        errorMessage = validationResult2.errorMessage
                        return@Button
                    }

                    val timeRangeResult =
                        ValidationUtils.LessonTimeValidation.validateTimeRange(startTime, endTime)
                    if (!timeRangeResult.isValid) {
                        errorMessage = timeRangeResult.errorMessage
                        return@Button
                    }

                    // 检查时间重叠
                    val newStartTime = startTime
                    val newEndTime = endTime
                    val hasOverlap = lessonTimes.any { lesson ->
                        // 检查时间是否重叠
                        (newStartTime < lesson.endTime && newEndTime > lesson.startTime)
                    }

                    if (hasOverlap) {
                        errorMessage = "时间重叠：与现有课程时间冲突"
                        return@Button
                    }

                    // 验证通过，保存数据（节次将自动分配）
                    scope.launch {
                        try {
                            val result = dao.insertOrUpdateLessonTimeAutoSort(
                                LessonTimeEntity(
                                    timetableId = timetableId,
                                    period = 1, // 临时值，会被自动排序方法覆盖
                                    startTime = startTime,
                                    endTime = endTime
                                )
                            )
                            Log.d("AddLessonTimeSheet", "新增课程时间成功，ID: $result")
                            onDismiss()
                        } catch (e: Exception) {
                            Log.e("AddLessonTimeSheet", "新增课程时间失败", e)
                            errorMessage = "保存失败: ${e.message}"
                        }
                    }
                }) {
                    Text("保存")
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    // 时间选择器
    if (showStartTimePicker) {
        TimePickerDialog(
            onDismiss = { showStartTimePicker = false },
            onConfirm = { hour, minute ->
                startTime =
                    java.text.MessageFormat.format("{0,number,00}:{1,number,00}", hour, minute)
                showStartTimePicker = false
            },
            initialHour = startTime.split(":")[0].toIntOrNull() ?: 8,
            initialMinute = startTime.split(":")[1].toIntOrNull() ?: 0
        )
    }

    if (showEndTimePicker) {
        TimePickerDialog(
            onDismiss = { showEndTimePicker = false },
            onConfirm = { hour, minute ->
                endTime =
                    java.text.MessageFormat.format("{0,number,00}:{1,number,00}", hour, minute)
                showEndTimePicker = false
            },
            initialHour = endTime.split(":")[0].toIntOrNull() ?: 8,
            initialMinute = endTime.split(":")[1].toIntOrNull() ?: 45
        )
    }
}

// ------------------ 新增课程弹窗 ------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCourseSheet(
    timetableId: Long,
    onDismiss: () -> Unit,
    dao: ScheduleDao,
    sheetState: androidx.compose.material3.SheetState
) {
    // 获取当前课表的课程和课程时间，用于重叠检测
    val courses by dao.getCoursesFlow(timetableId).collectAsState(initial = emptyList())

    var name by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var dayOfWeek by remember { mutableStateOf("1") }
    var periods by remember { mutableStateOf("1") }
    var weeks by remember { mutableStateOf("1") }
    var errorMessage by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    OptimizedBottomSheet(
        onDismiss = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "新增课程",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // 错误信息显示
            if (errorMessage.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(8.dp)
                    )
                }
                Spacer(Modifier.height(8.dp))
            }

            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    errorMessage = "" // 清除错误信息
                },
                label = { Text("课程名称") },
                modifier = Modifier.fillMaxWidth(),
                isError = errorMessage.contains("课程名称"),
                supportingText = if (errorMessage.contains("课程名称")) {
                    { Text("课程名称不能为空，且不超过50个字符") }
                } else null
            )

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = location,
                onValueChange = {
                    location = it
                    errorMessage = "" // 清除错误信息
                },
                label = { Text("地点") },
                modifier = Modifier.fillMaxWidth(),
                isError = errorMessage.contains("上课地点"),
                supportingText = if (errorMessage.contains("上课地点")) {
                    { Text("上课地点不能超过100个字符") }
                } else null
            )

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = dayOfWeek,
                onValueChange = {
                    dayOfWeek = it.filter { c -> c.isDigit() }
                    errorMessage = "" // 清除错误信息
                },
                label = { Text("星期 (1-7)") },
                modifier = Modifier.fillMaxWidth(),
                isError = errorMessage.contains("星期"),
                supportingText = if (errorMessage.contains("星期")) {
                    { Text("1=周一，2=周二，...，7=周日") }
                } else null
            )

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = periods,
                onValueChange = {
                    periods = it
                    errorMessage = "" // 清除错误信息
                },
                label = { Text("节次 (如 1,2,3)") },
                modifier = Modifier.fillMaxWidth(),
                isError = errorMessage.contains("节次") || errorMessage.contains("重叠"),
                supportingText = if (errorMessage.contains("节次")) {
                    { Text("用逗号分隔，如：1,2,3") }
                } else if (errorMessage.contains("重叠")) {
                    { Text(errorMessage) }
                } else {
                    { Text("请输入课程节次，多个节次用逗号分隔") }
                }
            )

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = weeks,
                onValueChange = {
                    weeks = it
                    errorMessage = "" // 清除错误信息
                },
                label = { Text("周次 (如 1,2,3)") },
                modifier = Modifier.fillMaxWidth(),
                isError = errorMessage.contains("周次"),
                supportingText = if (errorMessage.contains("周次")) {
                    { Text("用逗号分隔，如：1,2,3，表示第几周上课") }
                } else null
            )

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) { Text("取消") }
                Spacer(Modifier.width(8.dp))
                Button(onClick = {
                    // 数据验证
                    val validationResult = ValidationUtils.CourseValidation.validateCourseData(
                        name = name,
                        location = location,
                        dayOfWeek = dayOfWeek,
                        periods = periods,
                        weeks = weeks
                    )

                    if (!validationResult.isValid) {
                        errorMessage = validationResult.errorMessage
                        return@Button
                    }

                    // 检查课程时间重叠
                    val day = dayOfWeek.toIntOrNull() ?: 1
                    val periodList = periods.split(",").map { it.trim().toInt() }
                    val weekList = weeks.split(",").map { it.trim().toInt() }

                    // 检查是否有时间冲突
                    val hasTimeOverlap = courses.any { existingCourse ->
                        // 检查是否同一天
                        if (existingCourse.dayOfWeek != day) return@any false

                        // 检查是否同一周
                        val weekOverlap = existingCourse.weeks.any { w -> weekList.contains(w) }
                        if (!weekOverlap) return@any false

                        // 检查是否同一节次
                        val periodOverlap =
                            existingCourse.periods.any { p -> periodList.contains(p) }
                        periodOverlap
                    }

                    if (hasTimeOverlap) {
                        errorMessage = "时间重叠：与现有课程在同一时间"
                        return@Button
                    }

                    // 验证通过，保存数据
                    scope.launch {
                        try {
                            val result = dao.insertCourseWithReminders(
                                CourseEntity(
                                    timetableId = timetableId,
                                    name = name,
                                    location = location,
                                    dayOfWeek = day,
                                    periods = periodList,
                                    weeks = weekList
                                )
                            )
                            Log.d("AddCourseSheet", "新增课程成功，ID: $result")
                            onDismiss()
                        } catch (e: Exception) {
                            Log.e("AddCourseSheet", "新增课程失败", e)
                            errorMessage = "保存失败: ${e.message}"
                        }
                    }
                }) {
                    Text("保存")
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableDetailSheet(timetable: TimetableEntity, onDismiss: () -> Unit, dao: ScheduleDao, sheetState: androidx.compose.material3.SheetState) {
    val scope = rememberCoroutineScope()

    // 课表信息状态
    var name by remember { mutableStateOf(timetable.name) }
    var startDate by remember { mutableStateOf(timetable.startDate) }
    var showWeekend by remember { mutableStateOf(timetable.showWeekend) }
    var errorMessage by remember { mutableStateOf("") }

    // 显示非本周课程开关
    val perTableKey = "timetable_${timetable.id}_show_non_current_week"
    val showNonCurrentPerTable by dao.getPreferenceFlow(perTableKey)
        .collectAsState(initial = null)
    val showNonCurrentGlobal by dao.getPreferenceFlow("show_non_current_week")
        .collectAsState(initial = null)
    val effectiveShowNonCurrent = when (showNonCurrentPerTable) {
        "true" -> true
        "false" -> false
        else -> showNonCurrentGlobal == "true"
    }
    var showNonCurrent by remember(showNonCurrentPerTable, showNonCurrentGlobal) {
        mutableStateOf(effectiveShowNonCurrent)
    }

    // 日期选择器状态
    var showDatePicker by remember { mutableStateOf(false) }

    // 课程时间管理 (按节次排序)
    val lessonTimes by dao.getLessonTimesFlow(timetable.id).collectAsState(initial = emptyList())
    val sortedLessonTimes = lessonTimes.sortedBy { it.period }

    // 课程管理
    val courses by dao.getCoursesFlow(timetable.id).collectAsState(initial = emptyList())

    // 子 BottomSheet 状态
    var showAddLessonSheet by remember { mutableStateOf(false) }
    var showAddCourseSheet by remember { mutableStateOf(false) }
    var showEditLessonSheet by remember { mutableStateOf<LessonTimeEntity?>(null) }
    var showEditCourseSheet by remember { mutableStateOf<CourseEntity?>(null) }
    
    // 子 BottomSheet 状态
    val addLessonSheetState = rememberModalBottomSheetState()
    val addCourseSheetState = rememberModalBottomSheetState()
    val editLessonSheetState = rememberModalBottomSheetState()
    val editCourseSheetState = rememberModalBottomSheetState()

    OptimizedBottomSheet(
        onDismiss = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = timetable.name,
                    style = MaterialTheme.typography.headlineSmall
                )
//                IconButton(onClick = onDismiss) {
//                    Icon(Icons.Rounded.Close, contentDescription = "关闭")
//                }
            }

            Spacer(Modifier.height(16.dp))

            // 错误信息显示
            if (errorMessage.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(8.dp)
                    )
                }
                Spacer(Modifier.height(8.dp))
            }

            // 课表信息编辑
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            errorMessage = "" // 清除错误信息
                        },
                        label = { Text("课表名称") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = errorMessage.contains("课程表名称"),
                        supportingText = if (errorMessage.contains("课程表名称")) {
                            { Text("课程表名称不能为空，且不超过100个字符") }
                        } else null
                    )

                    OutlinedTextField(
                        value = startDate,
                        onValueChange = {
                            startDate = it
                            errorMessage = "" // 清除错误信息
                        },
                        label = { Text("开学日期") },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { showDatePicker = true }) {
                                Icon(Icons.Rounded.CalendarMonth, contentDescription = "选择日期")
                            }
                        },
                        isError = errorMessage.contains("日期") || errorMessage.contains("学期"),
                        supportingText = if (errorMessage.contains("日期") || errorMessage.contains(
                                "学期"
                            )
                        ) {
                            { Text("请使用有效的日期格式") }
                        } else null
                    )

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("显示周末")
                        Switch(
                            checked = showWeekend,
                            onCheckedChange = { showWeekend = it }
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("显示非本周课程")
                        Switch(
                            checked = showNonCurrent,
                            onCheckedChange = { enabled ->
                                showNonCurrent = enabled
                                scope.launch {
                                    dao.setPreference(perTableKey, enabled.toString())
                                }
                            }
                        )
                    }

                    Button(
                        onClick = {
                            // 数据验证
                            val validationResult =
                                ValidationUtils.TimetableValidation.validateTimetableData(
                                    name = name,
                                    startDate = startDate
                                )

                            if (!validationResult.isValid) {
                                errorMessage = validationResult.errorMessage
                                return@Button
                            }

                            // 验证通过，保存数据
                            scope.launch {
                                dao.updateTimetableWithReminders(
                                    timetable.copy(
                                        name = name,
                                        startDate = startDate,
                                        showWeekend = showWeekend
                                    )
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("保存修改")
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // 课程时间管理
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("课程时间管理", style = MaterialTheme.typography.titleSmall)
                        IconButton(onClick = { showAddLessonSheet = true }) {
                            Icon(Icons.Rounded.Add, contentDescription = "新增课程时间")
                        }
                    }

                    if (sortedLessonTimes.isEmpty()) {
                        Text(
                            text = "暂无课程时间",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(8.dp)
                        )
                    } else {
                        sortedLessonTimes.forEach { lesson ->
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Text(
                                    text = "第${lesson.period}节 ${lesson.startTime}-${lesson.endTime}",
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.width(IntrinsicSize.Min)
                                ) {
                                    IconButton(onClick = { showEditLessonSheet = lesson }) {
                                        Icon(
                                            Icons.Rounded.Edit,
                                            contentDescription = "编辑课程时间"
                                        )
                                    }
                                    IconButton(onClick = {
                                        scope.launch {
                                            dao.deleteLessonTimeAutoSort(
                                                lesson
                                            )
                                        }
                                    }) {
                                        Icon(
                                            Icons.Rounded.Delete,
                                            contentDescription = "删除课程时间"
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // 课程管理
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("课程管理", style = MaterialTheme.typography.titleSmall)
                        IconButton(onClick = { showAddCourseSheet = true }) {
                            Icon(Icons.Rounded.Add, contentDescription = "新增课程")
                        }
                    }

                    if (courses.isEmpty()) {
                        Text(
                            text = "暂无课程",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(8.dp)
                        )
                    } else {
                        courses.forEach { course ->
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${course.name} (周${course.dayOfWeek} 节次:${
                                        course.periods.joinToString(
                                            ","
                                        )
                                    })",
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.width(IntrinsicSize.Min)
                                ) {
                                    IconButton(onClick = { showEditCourseSheet = course }) {
                                        Icon(Icons.Rounded.Edit, contentDescription = "编辑课程")
                                    }
                                    IconButton(onClick = {
                                        scope.launch {
                                            dao.deleteCourseWithReminders(
                                                course
                                            )
                                        }
                                    }) {
                                        Icon(Icons.Rounded.Delete, contentDescription = "删除课程")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    // 日期选择器对话框
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = try {
                LocalDate.parse(startDate).atStartOfDay(ZoneId.systemDefault()).toInstant()
                    .toEpochMilli()
            } catch (_: Exception) {
                System.currentTimeMillis()
            }
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            val date = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault())
                                .toLocalDate()
                            startDate = date.toString()
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDatePicker = false
                    }
                ) {
                    Text("取消")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // 子 BottomSheet
    if (showAddLessonSheet) {
        AddLessonTimeSheet(
            timetableId = timetable.id,
            onDismiss = {
                scope.launch { addLessonSheetState.hide() }.invokeOnCompletion {
                    if (!addLessonSheetState.isVisible) {
                        showAddLessonSheet = false
                    }
                }
            },
            dao = dao,
            sheetState = addLessonSheetState
        )
    }

    if (showAddCourseSheet) {
        AddCourseSheet(
            timetableId = timetable.id,
            onDismiss = {
                scope.launch { addCourseSheetState.hide() }.invokeOnCompletion {
                    if (!addCourseSheetState.isVisible) {
                        showAddCourseSheet = false
                    }
                }
            },
            dao = dao,
            sheetState = addCourseSheetState
        )
    }

    showEditLessonSheet?.let { lesson ->
        EditLessonTimeSheet(
            lesson = lesson,
            onDismiss = {
                scope.launch { editLessonSheetState.hide() }.invokeOnCompletion {
                    if (!editLessonSheetState.isVisible) {
                        showEditLessonSheet = null
                    }
                }
            },
            dao = dao,
            sheetState = editLessonSheetState
        )
    }

    showEditCourseSheet?.let { course ->
        EditCourseSheet(
            course = course,
            onDismiss = {
                scope.launch { editCourseSheetState.hide() }.invokeOnCompletion {
                    if (!editCourseSheetState.isVisible) {
                        showEditCourseSheet = null
                    }
                }
            },
            dao = dao,
            sheetState = editCourseSheetState
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit,
    initialHour: Int,
    initialMinute: Int
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onConfirm(timePickerState.hour, timePickerState.minute)
            }) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
        text = {
            TimePicker(state = timePickerState)
        }
    )
}

// ---------- 导入选项弹窗 ----------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportOptionsSheet(
    onDismiss: () -> Unit,
    onWakeUpImport: () -> Unit,
    sheetState: androidx.compose.material3.SheetState
) {
    OptimizedBottomSheet(
        onDismiss = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .navigationBarsPadding()
        ) {
            Text(
                text = "选择导入方式",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // WakeUp课程表导入选项
            Card(
                modifier = Modifier
                    .fillMaxWidth(),
                onClick = { onWakeUpImport() },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.CalendarMonth, 
                        contentDescription = "WakeUp课程表",
                        modifier = Modifier.padding(end = 12.dp)
                    )
                    Column {
                        Text(
                            text = "WakeUp课程表",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "从WakeUp课程表在线导入",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

//            Spacer(Modifier.height(8.dp))
//
//            Card(
//                modifier = Modifier
//                    .fillMaxWidth(),
//                onClick = {
//                    //跳转activity
//                    val intent = Intent(context, WebActivity::class.java)
//                    context.startActivity(intent)
//                },
//                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
//            ) {
//                Row(
//                    modifier = Modifier.padding(16.dp),
//                    verticalAlignment = Alignment.CenterVertically
//                ) {
//                    Icon(
//                        Icons.Rounded.CalendarMonth,
//                        contentDescription = "从教务系统导入",
//                        modifier = Modifier.padding(end = 12.dp)
//                    )
//                    Column {
//                        Text(
//                            text = "教务系统",
//                            style = MaterialTheme.typography.titleMedium
//                        )
//                        Text(
//                            text = "从教务系统导入",
//                            style = MaterialTheme.typography.bodySmall,
//                            color = MaterialTheme.colorScheme.onSurfaceVariant
//                        )
//                    }
//                }
//            }
            
            Spacer(Modifier.height(16.dp))
            
            Text(
                text = "更多导入方式即将推出...",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

// ---------- WakeUp导入弹窗 ----------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WakeUpImportSheet(
    onDismiss: () -> Unit,
    dao: ScheduleDao,
    sheetState: androidx.compose.material3.SheetState
) {
    var shareText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    OptimizedBottomSheet(
        onDismiss = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "从WakeUp课程表在线导入",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Text(
                text = "请完整复制分享口令",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // 示例分享口令
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "[示例]这是来自「WakeUp课程表」的课表分享，30分钟内有效哦，如果失效请朋友再分享一遍叭。为了保护隐私我们选择不监听你的剪贴板，请复制这条消息后，打开App的主界面，右上角第二个按钮 -> 从分享口令导入，按操作提示即可完成导入~分享口令为「0000000000000000」",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(12.dp)
                )
            }
            
            Spacer(Modifier.height(16.dp))
            
            OutlinedTextField(
                value = shareText,
                onValueChange = {
                    shareText = it
                    errorMessage = ""
                },
                label = { Text("分享口令或口令内容") },
                modifier = Modifier.fillMaxWidth(),
                isError = errorMessage.isNotEmpty(),
                supportingText = {
                    if (errorMessage.isNotEmpty()) {
                        Text(errorMessage)
                    } else {
                        Text("可粘贴完整分享口令或直接输入口令内容")
                    }
                },
                placeholder = { Text("粘贴分享口令或输入口令内容") }
            )
            
            Spacer(Modifier.height(16.dp))
            
            if (isLoading) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text("正在导入...")
                }
            }
            
            Button(
                onClick = {
                    if (shareText.isBlank()) {
                        errorMessage = "请输入分享口令"
                        return@Button
                    }
                    
                    // 提取口令内容
                    val key = extractKeyFromShareText(shareText)
                    if (key.isBlank()) {
                        errorMessage = "未找到有效的分享口令"
                        return@Button
                    }
                    
                    isLoading = true
                    scope.launch {
                        try {
                            // 调用WakeUp API导入课表
                            val result = importFromWakeUp(key, dao)
                            if (result) {
                                onDismiss()
                            } else {
                                errorMessage = "导入失败，请检查分享口令是否有效"
                            }
                        } catch (e: Exception) {
                            errorMessage = "导入失败: ${e.message}"
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                Text(if (isLoading) "导入中..." else "导入")
            }
        }
    }
}

// 从分享文本中提取口令
fun extractKeyFromShareText(text: String): String {
    val pattern = "分享口令为「([a-f0-9]+)」".toRegex()
    val match = pattern.find(text)
    val key = match?.groupValues?.get(1) ?: ""
    return key
}

// WakeUp导入函数
suspend fun importFromWakeUp(key: String, dao: ScheduleDao): Boolean = withContext(Dispatchers.IO) {
    try {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("https://i.wakeup.fun/share_schedule/get?key=$key")
            .get()
            .build()
        val response = client.newCall(request).execute()
        val body = response.body.string()

        if (response.code != 200) return@withContext false

        val rootJson = Json.parseToJsonElement(body).jsonObject
        if (rootJson["status"]?.jsonPrimitive?.int != 1) return@withContext false

        val dataStr = rootJson["data"]?.jsonPrimitive?.content ?: return@withContext false
        val segments = dataStr.split("\n")
        if (segments.size < 4) return@withContext false

        val timetableInfo = Json.decodeFromString<JsonObject>(segments[0])
        val lessonTimes = Json.decodeFromString<JsonArray>(segments[1])
        val configInfo = Json.decodeFromString<JsonObject>(segments[2])
        val courses = Json.decodeFromString<JsonArray>(segments[3])
        val courseInfo = Json.decodeFromString<JsonArray>(segments[4])

        Log.d("WakeUp", "timetableInfo: $timetableInfo")
        Log.d("WakeUp", "lessonTimes: $lessonTimes")
        Log.d("WakeUp", "configInfo: $configInfo")
        Log.d("WakeUp", "courses: $courses")
        Log.d("WakeUp", "courseInfo: $courseInfo")


        val timetableId = dao.insertTimetableWithReminders(
            TimetableEntity(
                name = configInfo["tableName"]?.jsonPrimitive?.content ?: "未命名WakeUp课程表",
                showWeekend = configInfo["showSun"]?.jsonPrimitive?.boolean ?: true,
                startDate = configInfo["startDate"]?.jsonPrimitive?.content?.let { parseDateAutoFix(it) }
                    ?: LocalDate.now().toString()
            )
        )

        lessonTimes.forEach { jsonElement ->
            val lessonObject = jsonElement.jsonObject
            val period = lessonObject["node"]?.jsonPrimitive?.int ?: 1
            val startTime = lessonObject["startTime"]?.jsonPrimitive?.content ?: return@forEach
            val endTime = lessonObject["endTime"]?.jsonPrimitive?.content ?: return@forEach

            if (startTime == endTime) {
                return@forEach
            }

            dao.insertOrUpdateLessonTimeAutoSort(
                LessonTimeEntity(
                    timetableId = timetableId,
                    period = period,
                    startTime = startTime,
                    endTime = endTime
                )
            )
        }

        courseInfo.forEach { jsonElement ->
            val courseInfoObject = jsonElement.jsonObject
            val startWeek = courseInfoObject["startWeek"]?.jsonPrimitive?.int ?: return@forEach
            val endWeek = courseInfoObject["endWeek"]?.jsonPrimitive?.int ?: return@forEach
            val weeks = (startWeek..endWeek).toList()
            val startPeriod = courseInfoObject["startNode"]?.jsonPrimitive?.int ?: return@forEach
            val endPeriod = startPeriod + (courseInfoObject["step"]?.jsonPrimitive?.int ?: return@forEach)-1
            val periods = (startPeriod..endPeriod).toList()
            val location = courseInfoObject["room"]?.jsonPrimitive?.content ?: return@forEach
            val courseId = courseInfoObject["id"]?.jsonPrimitive?.int ?: return@forEach
            val courseInfo = courses.firstOrNull { course ->
                course.jsonObject["id"]?.jsonPrimitive?.int == courseId
            }
            if (courseInfo == null) return@withContext false
            val courseName = courseInfo.jsonObject["courseName"]?.jsonPrimitive?.content ?: return@forEach

            val dayOfWeek = courseInfoObject["day"]?.jsonPrimitive?.int ?: return@forEach

            dao.insertCourseWithReminders(
                CourseEntity(
                    timetableId = timetableId,
                    name = courseName,
                    location = location,
                    dayOfWeek = dayOfWeek,
                    periods = periods,
                    weeks = weeks
                )
            )
        }
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}
fun parseDateAutoFix(dateStr: String): String {
    val parts = dateStr.split("-")
    if (parts.size != 3) throw IllegalArgumentException("Invalid date format: $dateStr")
    val year = parts[0].padStart(4, '0')
    val month = parts[1].padStart(2, '0')
    val day = parts[2].padStart(2, '0')
    val fixedDateStr = "$year-$month-$day"
    return LocalDate.parse(fixedDateStr, DateTimeFormatter.ISO_LOCAL_DATE).toString()
}
