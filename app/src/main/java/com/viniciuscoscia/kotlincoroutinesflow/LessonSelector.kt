package com.viniciuscoscia.kotlincoroutinesflow

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.viniciuscoscia.kotlincoroutinesflow._1coroutinebasics.CoroutineBasicsHomework
import com.viniciuscoscia.kotlincoroutinesflow._1coroutinebasics.CounterScreen
import com.viniciuscoscia.kotlincoroutinesflow._1coroutinebasics.ProfileScreen
import com.viniciuscoscia.kotlincoroutinesflow._2coroutinecontext.CoroutineContextsHomework1
import com.viniciuscoscia.kotlincoroutinesflow.commons.RotatingBox

private enum class Lesson(@StringRes val title: Int, @StringRes val description: Int) {
    Birds(R.string.lesson_birds, R.string.lesson_birds_description),
    Counter(R.string.lesson_counter, R.string.lesson_counter_description),
    Profile(R.string.lesson_profile, R.string.lesson_profile_description),
    Rotation(R.string.lesson_rotation, R.string.lesson_rotation_description),
    CoroutineContextsHomework(R.string.coroutine_contexts_homework_1, R.string.coroutine_contexts_homework_1)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonSelector(modifier: Modifier = Modifier) {
    var selectedLesson by rememberSaveable { mutableStateOf<Lesson?>(null) }

    BackHandler(enabled = selectedLesson != null) {
        selectedLesson = null
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(selectedLesson?.title ?: R.string.lesson_selector_title))
                },
                navigationIcon = {
                    if (selectedLesson != null) {
                        TextButton(onClick = { selectedLesson = null }) {
                            Text(stringResource(R.string.back_to_lessons))
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        // Removing a lesson from the composition also cancels its effects and scopes.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
        ) {
            when (selectedLesson) {
                Lesson.Birds -> CoroutineBasicsHomework.Assignment3()
                Lesson.Counter -> CounterScreen()
                Lesson.Profile -> ProfileScreen()
                Lesson.Rotation -> RotatingBox()
                null -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Lesson.entries.forEach { lesson ->
                        Card(
                            onClick = { selectedLesson = lesson },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = stringResource(lesson.title),
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = stringResource(lesson.description),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }

	            Lesson.CoroutineContextsHomework -> CoroutineContextsHomework1.Assignment()
            }
        }
    }
}
