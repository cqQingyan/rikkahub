package me.rerere.rikkahub.ui.pages.setting

import android.content.ActivityNotFoundException
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.composables.icons.lucide.BadgeInfo
import com.composables.icons.lucide.BookOpen
import com.composables.icons.lucide.Boxes
import com.composables.icons.lucide.Database
import com.composables.icons.lucide.Drama
import com.composables.icons.lucide.Earth
import com.composables.icons.lucide.Hammer
import com.composables.icons.lucide.HardDrive
import com.composables.icons.lucide.Heart
import com.composables.icons.lucide.Library
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.MessageCircleWarning
import com.composables.icons.lucide.Monitor
import com.composables.icons.lucide.Palette
import com.composables.icons.lucide.ScrollText
import com.composables.icons.lucide.Share2
import com.composables.icons.lucide.SunMoon
import com.composables.icons.lucide.Terminal
import com.composables.icons.lucide.Volume2
import me.rerere.rikkahub.R
import me.rerere.rikkahub.Screen
import me.rerere.rikkahub.data.datastore.isNotConfigured
import me.rerere.rikkahub.ui.components.nav.BackButton
import me.rerere.rikkahub.ui.components.ui.Select
import me.rerere.rikkahub.ui.context.LocalNavController
import me.rerere.rikkahub.ui.hooks.rememberColorMode
import me.rerere.rikkahub.ui.pages.setting.components.PresetThemeButtonGroup
import me.rerere.rikkahub.ui.theme.ColorMode
import me.rerere.rikkahub.utils.countChatFiles
import me.rerere.rikkahub.utils.openUrl
import me.rerere.rikkahub.utils.plus
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingPage(vm: SettingVM = koinViewModel()) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val navController = LocalNavController.current
    val settings by vm.settings.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(text = stringResource(R.string.settings))
                },
                navigationIcon = {
                    BackButton()
                },
                scrollBehavior = scrollBehavior,
                actions = {
                    if(settings.developerMode) {
                        IconButton(
                            onClick = {
                                navController.navigate(Screen.Developer)
                            }
                        ) {
                            Icon(Lucide.Hammer, "Developer")
                        }
                    }
                }
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = innerPadding + PaddingValues(16.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)
        ) {
            if (settings.isNotConfigured()) {
                item {
                    ProviderConfigWarningCard(navController)
                }
            }

            item {
                SettingGroup(
                    title = stringResource(R.string.setting_page_general_settings),
                ) {
                    var colorMode by rememberColorMode()
                    ListItem(
                        headlineContent = {
                            Text(stringResource(R.string.setting_page_color_mode))
                        },
                        leadingContent = {
                            Icon(Lucide.SunMoon, null)
                        },
                        trailingContent = {
                            Select(
                                options = ColorMode.entries,
                                selectedOption = colorMode,
                                onOptionSelected = {
                                    colorMode = it
                                    navController.navigate(Screen.Setting) {
                                        launchSingleTop = true
                                        popUpTo(Screen.Setting) {
                                            inclusive = true
                                        }
                                    }
                                },
                                optionToString = {
                                    when (it) {
                                        ColorMode.SYSTEM -> stringResource(R.string.setting_page_color_mode_system)
                                        ColorMode.LIGHT -> stringResource(R.string.setting_page_color_mode_light)
                                        ColorMode.DARK -> stringResource(R.string.setting_page_color_mode_dark)
                                    }
                                },
                                modifier = Modifier.width(150.dp)
                            )
                        }
                    )

                    SettingItem(
                        navController = navController,
                        title = { Text(stringResource(R.string.setting_page_display_setting)) },
                        description = { Text(stringResource(R.string.setting_page_display_setting_desc)) },
                        icon = { Icon(Lucide.Monitor, "Display Setting") },
                        link = Screen.SettingDisplay
                    )

                    SettingItem(
                        navController = navController,
                        title = { Text(stringResource(R.string.setting_page_assistant)) },
                        description = { Text(stringResource(R.string.setting_page_assistant_desc)) },
                        icon = { Icon(Lucide.Drama, "Assistant") },
                        link = Screen.Assistant
                    )

                    SettingItem(
                        navController = navController,
                        title = { Text(stringResource(R.string.setting_page_prompts_title)) },
                        description = { Text(stringResource(R.string.setting_page_prompts_desc)) },
                        icon = { Icon(Lucide.BookOpen, "Prompts") },
                        link = Screen.Prompts
                    )
                }
            }

            item {
                SettingGroup(
                    title = stringResource(R.string.setting_page_model_and_services),
                ) {
                    SettingItem(
                        navController = navController,
                        title = { Text(stringResource(R.string.setting_page_default_model)) },
                        description = { Text(stringResource(R.string.setting_page_default_model_desc)) },
                        icon = { Icon(Lucide.Hammer, stringResource(R.string.setting_page_default_model)) },
                        link = Screen.SettingModels
                    )

                    SettingItem(
                        navController = navController,
                        title = { Text(stringResource(R.string.setting_page_providers)) },
                        description = { Text(stringResource(R.string.setting_page_providers_desc)) },
                        icon = { Icon(Lucide.Boxes, "Models") },
                        link = Screen.SettingProvider
                    )

                    SettingItem(
                        navController = navController,
                        title = { Text(stringResource(R.string.setting_page_search_service)) },
                        description = { Text(stringResource(R.string.setting_page_search_service_desc)) },
                        icon = { Icon(Lucide.Earth, "Search") },
                        link = Screen.SettingSearch
                    )

                    SettingItem(
                        navController = navController,
                        title = { Text(stringResource(R.string.setting_page_tts_service)) },
                        description = { Text(stringResource(R.string.setting_page_tts_service_desc)) },
                        icon = { Icon(Lucide.Volume2, "TTS") },
                        link = Screen.SettingTTS
                    )
                }
            }

            item {
                SettingGroup(
                    title = stringResource(R.string.setting_page_data_settings),
                ) {
                    SettingItem(
                        navController = navController,
                        title = { Text(stringResource(R.string.setting_page_data_backup)) },
                        description = { Text(stringResource(R.string.setting_page_data_backup_desc)) },
                        icon = { Icon(Lucide.Database, "Backup") },
                        link = Screen.Backup
                    )

                    val context = LocalContext.current
                    val storageState by produceState(-1 to 0L) {
                        value = context.countChatFiles()
                    }
                    SettingItem(
                        navController = navController,
                        title = { Text(stringResource(R.string.setting_page_chat_storage)) },
                        description = {
                            if (storageState.first == -1) {
                                Text(stringResource(R.string.calculating))
                            } else {
                                Text(
                                    stringResource(
                                        R.string.setting_page_chat_storage_desc,
                                        storageState.first,
                                        storageState.second / 1024 / 1024.0
                                    )
                                )
                            }
                        },
                        icon = {
                            Icon(Lucide.HardDrive, "Storage")
                        },
                    )

                    SettingItem(
                        navController = navController,
                        title = { Text(stringResource(R.string.setting_page_request_logs)) },
                        description = { Text(stringResource(R.string.setting_page_request_logs_desc)) },
                        icon = { Icon(Lucide.ScrollText, stringResource(R.string.setting_page_request_logs)) },
                        link = Screen.Log
                    )
                }
            }
        }
    }
}

@Composable
private fun ProviderConfigWarningCard(navController: NavHostController) {
    Card(
        modifier = Modifier.padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.End
        ) {
            ListItem(
                headlineContent = {
                    Text(stringResource(R.string.setting_page_config_api_title))
                },
                supportingContent = {
                    Text(stringResource(R.string.setting_page_config_api_desc))
                },
                leadingContent = {
                    Icon(Lucide.MessageCircleWarning, null)
                },
                colors = ListItemDefaults.colors(
                    containerColor = Color.Transparent
                )
            )

            TextButton(
                onClick = {
                    navController.navigate(Screen.SettingProvider)
                }
            ) {
                Text(stringResource(R.string.setting_page_config))
            }
        }
    }
}

@Composable
fun SettingItem(
    navController: NavHostController,
    title: @Composable () -> Unit,
    description: @Composable () -> Unit,
    icon: @Composable () -> Unit,
    link: Screen? = null,
    onClick: () -> Unit = {}
) {
    Surface(
        onClick = {
            if (link != null) navController.navigate(link)
            onClick()
        },
        color = Color.Transparent
    ) {
        ListItem(
            headlineContent = {
                title()
            },
            supportingContent = {
                description()
            },
            leadingContent = {
                icon()
            },
            colors = ListItemDefaults.colors(
                containerColor = Color.Transparent
            )
        )
    }
}

@Composable
fun SettingGroup(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = title,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            Column {
                content()
            }
        }
    }
}
